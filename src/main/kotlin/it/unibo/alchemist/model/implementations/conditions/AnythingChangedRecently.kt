package it.unibo.alchemist.model.implementations.conditions

import it.unibo.alchemist.model.interfaces.Dependency
import it.unibo.alchemist.model.interfaces.Node

abstract class AnythingChangedRecently<T> @JvmOverloads constructor(
    node: Node<T>,
    stepsRequiredForStability: Int = 1
) : ChangedRecently<T>(node, stepsRequiredForStability) {
    init {
        declareDependencyOn(Dependency.EVERYTHING)
    }
}
