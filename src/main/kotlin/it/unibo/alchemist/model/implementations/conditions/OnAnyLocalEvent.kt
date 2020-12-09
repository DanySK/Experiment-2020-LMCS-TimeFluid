package it.unibo.alchemist.model.implementations.conditions

import it.unibo.alchemist.model.interfaces.Context
import it.unibo.alchemist.model.interfaces.Dependency
import it.unibo.alchemist.model.interfaces.Node

class OnAnyLocalEvent<T>(node: Node<T>) : AbstractCondition<T>(node) {

    init {
        declareDependencyOn(Dependency.EVERYTHING)
    }

    override fun getContext(): Context = Context.LOCAL

    override fun getPropensityContribution(): Double = 1.0

    override fun isValid() = true
}