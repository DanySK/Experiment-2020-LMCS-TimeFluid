package it.unibo.alchemist.model.implementations.conditions

import it.unibo.alchemist.model.interfaces.Context
import it.unibo.alchemist.model.interfaces.Node

abstract class ChangedRecently<T> @JvmOverloads constructor(
    node: Node<T>,
    val stepsRequiredForStability: Int = 1
) : AbstractCondition<T>(node) {

    private val previousState: Array<Any?> = arrayOfNulls(stepsRequiredForStability)
    private var index = stepsRequiredForStability - 1
    private var evaluatedAsValid: Boolean = false

    protected abstract val currentState: Any?

    final override fun getPropensityContribution() = if (isValid) 1.0 else 0.0

    final override fun isValid() =
        evaluatedAsValid || previousState.any { it != currentState }.also { evaluatedAsValid = it }

    final override fun reactionReady() {
        evaluatedAsValid = false
        index = (index + 1) % stepsRequiredForStability
        previousState[index] = currentState
    }

    override fun toString() = "${super.toString()}[$isValid]"
}
