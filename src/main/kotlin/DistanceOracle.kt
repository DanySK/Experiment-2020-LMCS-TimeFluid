@file:JvmName("DistanceOracle")

import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.implementations.nodes.ProtelisNode
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Time
import it.unibo.alchemist.protelis.AlchemistExecutionContext
import java.lang.IllegalStateException

private val source = SimpleMolecule("source")
private val oracle = SimpleMolecule("oracle")
private val error = SimpleMolecule("error")
private val coverage = SimpleMolecule("coverage")
private fun Node<Any>.injectInformation() {

}
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
        Comparator { a, b -> a.second.compareTo(b.second).takeUnless { it == 0 } ?: a.first.compareTo(b.first) },
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

const val timeId = "oracle-last-time"

@JvmOverloads fun injectDistanceEverywhere(
    context: AlchemistExecutionContext<*>,
    sourceName: String = "source"
) {
    val timeId = "$timeId-$sourceName"
    val lastTime = when (val registeredTime = context.executionEnvironment[timeId]) {
        null -> Double.NEGATIVE_INFINITY
        is Number -> registeredTime.toDouble()
        is Time -> registeredTime.toDouble()
        is String -> registeredTime.toDouble()
        else -> throw IllegalStateException("$registeredTime is not a time")
    }
    val now = context.currentTime.toDouble()
    if (now <= lastTime) {
        return
    }
    val environment = context.environmentAccess
    val nodes = environment.nodes
    val sourceMolecule = SimpleMolecule(sourceName)
    val sources: List<NodeDistance> = nodes.filter { it.contains(sourceMolecule) }.map { it to 0.0 }
    fun Node<Any>.neighborhood() = environment.getNeighborhood(this )
    val distances = sources.toMap(mutableMapOf())
    fun Node<Any>.shortestDistanceSoFar() = distances[this] ?: Double.POSITIVE_INFINITY
    val visited = mutableSetOf<Node<Any>>()
    val toVisit = sortedSetOf<NodeDistance>(
        Comparator { a, b -> a.second.compareTo(b.second).takeUnless { it == 0 } ?: a.first.compareTo(b.first) },
        *sources.toTypedArray()
    )
    while (toVisit.isNotEmpty()) {
        val target =  toVisit.pollFirst() ?: throw IllegalStateException()
        val (targetNode, distance) = target
        toVisit.remove(target)
        if (visited.add(targetNode)) {
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
    val oracleVariableName = "oracle-$source"
    nodes.asSequence()
        .filterIsInstance<ProtelisNode<*>>()
        .forEach {
            it.put(oracleVariableName, it.shortestDistanceSoFar())
            it.put(timeId, now)
        }
}
