@file:JvmName("DistanceOracle")

import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.implementations.nodes.ProtelisNode
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.protelis.AlchemistExecutionContext
import java.lang.IllegalStateException

private val source = SimpleMolecule("source")
private typealias NodeDistance = Pair<Node<Any>, Double>
fun distanceToSource(context: AlchemistExecutionContext<*>): Double {
    val environment = context.environmentAccess
    val node = context.deviceUID as? ProtelisNode<*> ?: TODO()
    fun Node<Any>.neighborhood() = environment.getNeighborhood(this )
    val distances = mutableMapOf<Node<Any>, Double>(node to 0.0)
    fun Node<Any>.shortestDistanceSoFar() = distances[this] ?: Double.POSITIVE_INFINITY
    val visited = mutableSetOf<Node<Any>>()
//    val closest = Comparator<Node<Any>> { a, b -> a.shortestDistanceSoFar().compareTo(b.shortestDistanceSoFar()) }
//    val toVisit = sortedSetOf<Node<Any>>(closest, node)
    val toVisit = sortedSetOf<NodeDistance>(
        Comparator { a, b -> a.second.compareTo(b.second) },
        node to 0.0
    )
    while (toVisit.isNotEmpty()) {
//        val target =  toVisit.minByOrNull { it.second } ?: throw IllegalStateException()
        val target =  toVisit.pollFirst() ?: throw IllegalStateException()
        val (targetNode, distance) = target
        toVisit.remove(target)
        if (visited.add(targetNode)) {
//            val distance = targetNode.shortestDistanceSoFar()
            if (targetNode.contains(source)) {
                return distance
            }
            targetNode.neighborhood().neighbors.asSequence()
                .filter { it !in visited }
                .forEach { neighbor ->
                    val distanceOnThisPath = distance + environment.getDistanceBetweenNodes(targetNode, neighbor)
                    val previousDistance = neighbor.shortestDistanceSoFar()
                    if (distanceOnThisPath < previousDistance) {
                        distances.put(neighbor, distanceOnThisPath)
                        if (previousDistance.isFinite()) {
                            toVisit.remove(neighbor to previousDistance) || throw IllegalStateException()
                        }
                        toVisit.add(neighbor to distanceOnThisPath)
                    }
                }
        }
    }
    return Double.POSITIVE_INFINITY
}

//class BinaryHeap<T> private constructor(
//    val comparator: Comparator<T> = Comparator { a, b, -> (a as Comparable<T>).compareTo(b) },
//    private val tree: MutableList<T> = mutableListOf()
//) : Collection<T> by tree {
//    val head: T = tree[0]
//    operator fun plus(element: T) {
//        tree.add(element)
//        sortAt(tree.size - 1)
//    }
//
//    private fun sortAt(index: Int) {
//
//    }
//    private val Int.parent get() = (this - 1) / 2
//    private val Int.left get() = 2 * this + 1
//    private val Int.right get() = 2 * this + 2
//}