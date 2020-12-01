package it.unibo.alchemist.model.implementations.actions

import it.unibo.alchemist.model.implementations.nodes.ProtelisNode
import it.unibo.alchemist.model.interfaces.*
import org.apache.commons.math3.random.RandomGenerator
import org.jgrapht.graph.DirectedAcyclicGraph
import org.protelis.lang.datatype.DeviceUID
import org.protelis.vm.CodePath

typealias Spec = Map<String, Map<String, Any>>

sealed class Descriptor(backingMap: Map<String, Any>) {

    init {
        require("program" in backingMap.keys)
    }

    val program: String by backingMap
    val retentionTime: Double by backingMap.withDefault { 60.0 }
    val reactsToNewInformation: Boolean by backingMap.withDefault { true }

    class Action internal constructor(backingMap: Map<String, Any>) : Descriptor(backingMap)

    class Edge internal constructor(backingMap: Map<String, Any>) : Descriptor(backingMap) {
        val from: String by backingMap
        val to: String by backingMap
    }

    companion object {
        private val arcRequirements = setOf("from", "to")

        fun fromMap(backingMap: Map<String, Any>): Descriptor =
            if (backingMap.keys.containsAll(arcRequirements)) Edge(backingMap) else Action(backingMap)
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
        val (actions, edges) = spec.mapValues { Descriptor.fromMap(it.value) }
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
            val wrapper = ProtelisWrappingAction(program, sendToNeighbor, action.reactsToNewInformation)
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

    private fun evaluate(visited: MutableMap<Any, Boolean>, element: Edge<P>): Boolean = visited.computeIfAbsent(element) {
        evaluate(visited, element.first) && element.second.let {
            it.execute()
            when (val computed = node.getConcentration(it.asMolecule())) {
                is Boolean -> computed
                is String -> computed.toBoolean()
                else -> throw IllegalArgumentException(computed.toString())
            }
        }
    }

    private fun evaluate(visited: MutableMap<Any, Boolean>, element: ProtelisWrappingAction): Boolean = visited.computeIfAbsent(element) {
        val incoming: Set<Edge<P>> = dependencyGraph.incomingEdgesOf(element)
        if (
            incoming.isEmpty()
            || incoming.map { evaluate(visited, it) }.any()
            || element.runsOnMessageChange && element.hasNewMessageStatus
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

private class ProtelisWrappingAction(
        val program: RunProtelisProgram<*>,
        val send: SendToNeighbor,
        val runsOnMessageChange: Boolean
) : () -> Unit {

    private var stateAtLastExecution = emptyMap<DeviceUID, Map<CodePath, Any>>()

    val networkManager = program.node.getNetworkManager(program)

    val hasNewMessageStatus = stateAtLastExecution != networkManager.neighborState

    override fun invoke() {
        stateAtLastExecution = networkManager.neighborState
        program.execute()
        send.execute()
    }
}
