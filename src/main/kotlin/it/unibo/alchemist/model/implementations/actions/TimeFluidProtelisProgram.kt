package it.unibo.alchemist.model.implementations.actions

import it.unibo.alchemist.model.implementations.nodes.ProtelisNode
import it.unibo.alchemist.model.interfaces.*
import org.apache.commons.math3.random.RandomGenerator
import org.jgrapht.graph.DirectedAcyclicGraph
import org.protelis.lang.datatype.DeviceUID
import org.protelis.vm.CodePath

typealias Spec = Map<String, Map<String, Any>>

sealed class Descriptor(name: String, backingMap: Map<String, Any>) {

    val program: String
    val retentionTime: Double by backingMap.withDefault { Double.NaN }
    val reactsToNewInformation: Boolean by backingMap.withDefault { true }
    val reactsToSelfState: Boolean by backingMap.withDefault { true }

    init {
        require("program" in backingMap.keys)
        program = backingMap["program"].toString().let {
            when {
                it.matches("\\w+(:\\w+)*".toRegex()) -> it
                else -> "module $name\n$it"
            }
        }
    }

    class Action internal constructor(
        name: String,
        backingMap: Map<String, Any>
    ) : Descriptor(name, backingMap)

    class Edge internal constructor(
        name: String,
        backingMap: Map<String, Any>
    ) : Descriptor(name, backingMap) {
        val from: String by backingMap
        val to: String by backingMap
    }

    companion object {
        private val arcRequirements = setOf("from", "to")

        fun fromMap(name: String, backingMap: Map<String, Any>): Descriptor =
            if (backingMap.keys.containsAll(arcRequirements)) Edge(name, backingMap) else Action(name, backingMap)
    }
}

private typealias Edge<P> = Pair<ProtelisWrappingAction, RunProtelisProgram<P>>

class TimeFluidProtelisProgram<P : Position<P>>(
    randomGenerator: RandomGenerator,
    environment: Environment<Any, P>,
    node: ProtelisNode<P>,
    reaction: Reaction<Any>,
    spec: Spec
) : AbstractAction<Any>(node) {

    private val dependencyGraph = DirectedAcyclicGraph<ProtelisWrappingAction, Edge<P>>(null, null, false)

    init {
        val (actions, edges) = spec.mapValues { (name, map) -> Descriptor.fromMap(name, map) }
            .entries
            .partition { it.value is Descriptor.Action }
            .toList()
            .map { entries: List<Map.Entry<String, Descriptor>> -> // transform to map
                entries.groupingBy { it.key }.aggregate { _, _: Descriptor?, entry, _ ->
                    entry.value
                }
            }
        val vertices = actions.mapValues { (_, action) ->
            val program = RunProtelisProgram(environment, node, reaction, randomGenerator, action.program, action.retentionTime)
            val sendToNeighbor = SendToNeighbor(node, reaction, program)
            val wrapper = ProtelisWrappingAction(program, sendToNeighbor, action.reactsToNewInformation, action.reactsToSelfState)
            dependencyGraph.addVertex(wrapper)
            wrapper
        }
        // Populate edges, use vertices to reference them
        edges.forEach { (_, guard) ->
            if (guard is Descriptor.Edge) {
                val program = RunProtelisProgram(environment, node, reaction, randomGenerator, guard.program, guard.retentionTime)
                val from = vertices[guard.from]
                require(from != null)
                val to = vertices[guard.to]
                require(to != null)
                dependencyGraph.addEdge(from, to, Edge(from, program))
            } else {
                throw IllegalStateException("Unexpected Action: $guard")
            }
        }
    }

    override fun cloneAction(node: Node<Any>?, reaction: Reaction<Any>?): Action<Any> {
        TODO("Not yet implemented")
    }

    private fun <K : Any, V : Any> MutableMap<K, V>.computeIfNotPresent(key: K, supplier: () -> V): V {
        val previous = this[key]
        return if (previous == null) {
            val new: V = supplier()
            this[key] = new
            new
        } else {
            previous
        }
    }

    private fun evaluate(visited: MutableMap<Any, Boolean>, element: Edge<P>): Boolean = visited.computeIfNotPresent(element) {
        evaluate(visited, element.first) && element.second.let {
            it.execute()
            when (val computed = node.getConcentration(it.asMolecule())) {
                is Boolean -> computed
                is String -> computed.toBoolean()
                else -> throw IllegalArgumentException(computed.toString())
            }
        }
    }

    private fun evaluate(visited: MutableMap<Any, Boolean>, element: ProtelisWrappingAction): Boolean = visited.computeIfNotPresent(element) {
        val incoming: Set<Edge<P>> = dependencyGraph.incomingEdgesOf(element)
        if (
            incoming.isEmpty()
            || incoming.map { evaluate(visited, it) }.any { it }
            || element.runsOnMessageChange && element.hasNewMessageStatus
            || element.runsOnStateChange && element.hasNewMessageStatus
        ) {
            element()
            true
        } else {
            false
        }
    }

    override fun execute() {
        /*
         * For each vertex
         *  run if it has no incoming edges
         *  otherwise compute all edges and run if at least one is enabled
         *      OR if a new message at this level has been received
         */
        val visited: MutableMap<Any, Boolean> = mutableMapOf()
        dependencyGraph.vertexSet().forEach { evaluate(visited, it) }
    }

    override fun getContext(): Context = Context.NEIGHBORHOOD
}

private typealias Message = Map<CodePath, Any>
private typealias NetworkMessages = Map<DeviceUID, Message>

private class ProtelisWrappingAction(
    val program: RunProtelisProgram<*>,
    val send: SendToNeighbor,
    val runsOnMessageChange: Boolean,
    val runsOnStateChange: Boolean,
) : () -> Unit {

    private var internalStateAtLastExecution: Message? = null
    private var networkStateAtLastExecution: NetworkMessages? = null
    private val currentInternalState get() = program.executionContext.storedState
    private val currentNetworkState get() = networkManager.neighborState

    val networkManager = program.node.getNetworkManager(program)

    val hasNewMessageStatus get() = networkStateAtLastExecution != currentNetworkState
    val hasNewSelfState get() = internalStateAtLastExecution != currentInternalState

    override fun invoke() {
        internalStateAtLastExecution = currentInternalState
        networkStateAtLastExecution = currentNetworkState
        program.execute()
        send.execute()
    }

    override fun toString(): String = program.toString()
}
