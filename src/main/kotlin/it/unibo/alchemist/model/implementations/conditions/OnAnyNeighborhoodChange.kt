package it.unibo.alchemist.model.implementations.conditions

import it.unibo.alchemist.model.implementations.nodes.ProtelisNode
import it.unibo.alchemist.model.interfaces.*
import org.protelis.vm.CodePath

class OnAnyNeighborhoodChange<P : Position<P>>(
    private val environment: Environment<Any, P>,
    node: Node<Any>,
    stepsRequiredForStability: Int
) : AnythingChangedRecently<Any>(node, stepsRequiredForStability) {

    override fun getContext(): Context = Context.LOCAL

    override val currentState: Any get() = node.state to environment.getNeighborhood(node).map { it.state }

    override fun toString() = "${super.toString()}[$isValid]"

    private val Node<Any>.position get() = environment.getPosition(this)
    private val Node<Any>.state get() = listOf(
        position,
        contents,
        when (this) {
            is ProtelisNode<*> -> networkManagers.map { (protelisProgram, networkmanager) ->
                protelisProgram.executionContext.storedState to networkmanager.neighborState
            }
            else -> emptyList<Any>()
        }
    )
}
