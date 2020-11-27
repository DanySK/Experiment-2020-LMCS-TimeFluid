package it.unibo.alchemist.model.implementations.actions

import it.unibo.alchemist.core.implementations.JGraphTDependencyGraph
import it.unibo.alchemist.model.implementations.nodes.ProtelisNode
import it.unibo.alchemist.model.interfaces.*
import org.apache.commons.math3.random.RandomGenerator
import org.danilopianini.util.ListSet
import org.jgrapht.graph.DirectedAcyclicGraph
import java.lang.IllegalStateException

typealias Spec = Map<String, Map<String, Any>>

sealed class Descriptor(private val backingMap: Map<String, Any>) {

    init {
        require("program" in backingMap.keys)
    }

    val program: String by backingMap
    val retentionTime: Double by backingMap.withDefault { 1.0 }

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

class TimeFluidProtelisProgram<P : Position<P>>(
    val incarnation: Incarnation<Any, P>,
    randomGenerator: RandomGenerator,
    environment: Environment<Any, P>,
    node: ProtelisNode<P>,
    timeDistribution: TimeDistribution<Any>,
    reaction: Reaction<Any>,
    spec: Spec
) : AbstractAction<Any>(node) {

    private val dependencyGraph = DirectedAcyclicGraph<ProtelisWrappingAction, RunProtelisProgram<P>>(null, null, false)

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
            val wrapper = ProtelisWrappingAction(program, sendToNeighbor)
            dependencyGraph.addVertex(wrapper)
            wrapper
        }
        // Populate edges, use vertices to reference them
    }

    override fun cloneAction(node: Node<Any>?, reaction: Reaction<Any>?): Action<Any> {
        TODO("Not yet implemented")
    }
    override fun execute() {
        TODO("Not yet implemented")
    }

    override fun getContext(): Context = Context.NEIGHBORHOOD

    private class ProtelisWrappingAction(val program: RunProtelisProgram<*>, val send: SendToNeighbor) : () -> Unit {
        override fun invoke() {
            program.execute()
            send.execute()
        }
    }
}