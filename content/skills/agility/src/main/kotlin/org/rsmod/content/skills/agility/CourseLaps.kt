package org.rsmod.content.skills.agility

/**
 * Lap order across every course. Courses that open with the same obstacles (Shayzien's and the
 * Colossal Wyrm's basic and advanced routes) count as one lap until the player takes an obstacle
 * of only one of them, so a lap begun on one carries over to the other at the split. The Agility
 * Pyramid's floors may be done in any order between its first and last obstacles.
 */
internal class CourseLaps(obstacles: List<AgilityObstacle>) {
    private val lastSteps: Map<Int, Int>
    private val stepLocs: Map<Int, Map<Int, Set<Int>>>

    init {
        val lapObstacles = obstacles.filterNot { it.isRecovery }.groupBy { it.course.rowId }
        lastSteps = lapObstacles.mapValues { (_, course) -> course.maxOf { it.step } }
        stepLocs =
            lapObstacles.mapValues { (_, course) ->
                course.associate { obstacle ->
                    obstacle.step to obstacle.locs.map { it.id }.toSet()
                }
            }
    }

    fun lastStep(course: Int): Int = lastSteps.getValue(course)

    fun continues(course: Int, step: Int, obstacle: AgilityObstacle): Boolean {
        if (obstacle.isRecovery) {
            return false
        }
        val other = obstacle.course.rowId
        if (obstacle.course.anyOrder == true) {
            return course == other && obstacle.step > 0
        }
        if (obstacle.step != step) {
            return false
        }
        return course == other || sharesStart(course, other, step)
    }

    private fun sharesStart(course: Int, other: Int, steps: Int): Boolean {
        val first = stepLocs[course] ?: return false
        val second = stepLocs[other] ?: return false
        return (0 until steps).all { step -> first[step] != null && first[step] == second[step] }
    }
}
