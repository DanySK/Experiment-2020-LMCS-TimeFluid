package it.unibo.alchemist.model.implementations.conditions

import it.unibo.alchemist.model.implementations.actions.RunProtelisProgram
import it.unibo.alchemist.model.implementations.actions.TimeFluidProtelisProgram
import it.unibo.alchemist.model.implementations.nodes.ProtelisNode
import it.unibo.alchemist.model.interfaces.*
import it.unibo.alchemist.protelis.AlchemistNetworkManager

class OnAnyLocalChange<P : Position<P>> @JvmOverloads constructor(
    val environment: Environment<Any, P>,
    node: Node<Any>,
    stepsRequiredForStability: Int = 1
) : AnythingChangedRecently<Any>(node, stepsRequiredForStability) {

    override fun getContext(): Context = Context.LOCAL
    override val currentState: Any get() = listOf(
        environment.getPosition(node),
        node.contents,
        ((node as? ProtelisNode<P>)?.networkManagers ?: emptyMap<RunProtelisProgram<*>, AlchemistNetworkManager>())
            .map { (protelisProgram, networkmanager) ->
                protelisProgram.getExecutionContext().storedState to networkmanager.neighborState
            }
    )
}
