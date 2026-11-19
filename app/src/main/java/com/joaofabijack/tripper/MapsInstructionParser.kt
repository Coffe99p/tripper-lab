package com.joaofabijack.tripper

import java.text.Normalizer
import java.math.BigDecimal
import java.util.Locale

/** Experimental text inspection, not a navigation feed. Only exact, anchored forms
 * are recognized. Subtext (e.g. "Arrive 16:02") is deliberately never parsed. */
object MapsInstructionParser {
    enum class State { MANEUVER, APPROACHING_ARRIVAL, ARRIVED, RECALCULATING, INCOMPLETE, UNKNOWN, NOT_NAVIGATION }
    enum class Direction {
        LEFT, RIGHT, SLIGHT_LEFT, SLIGHT_RIGHT, STRAIGHT, KEEP_LEFT, KEEP_RIGHT, MERGE,
        ROUNDABOUT, ROUNDABOUT_RIGHT, EXIT_ROUNDABOUT,
    }
    data class Result(val state: State, val direction: Direction? = null, val meters: Int? = null, val exit: Int? = null)
    private val distancePrefix = Regex("^([0-9]+(?:[.,][0-9]{1,2})?)\\s+(km|m)\\s*[·•–-]\\s*(.+)$")

    fun parse(title: String?, category: String?, ongoing: Boolean, groupSummary: Boolean = false): Result {
        if (category != "navigation" || groupSummary) return Result(State.NOT_NAVIGATION)
        val text = Normalizer.normalize(title.orEmpty(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace('\u00a0', ' ').replace('\u202f', ' ')
            .trim().lowercase(Locale.ROOT)
        if (Regex("^(you have arrived|you've arrived|voce chegou)(?:[.!…]| at\\b.*| ao\\b.*| no\\b.*| na\\b.*)?$").matches(text)) {
            return Result(State.ARRIVED)
        }
        if (!ongoing) return Result(State.NOT_NAVIGATION)
        // "Arriving" was followed by removal on a completed real trip, but it is
        // an approach message, not a universal proof that arrival is complete.
        if (text == "arriving") return Result(State.APPROACHING_ARRIVAL)
        if (Regex("^(rerouting|recalculating|recalculando)(?:[.!…]|\\s)*$").matches(text)) return Result(State.RECALCULATING)
        val match = distancePrefix.matchEntire(text)
        val meters = if (match != null) {
            val value = runCatching {
                BigDecimal(match.groupValues[1].replace(',', '.'))
                    .multiply(if (match.groupValues[2] == "km") BigDecimal(1000) else BigDecimal.ONE)
                    .intValueExact()
            }.getOrNull() ?: return Result(State.UNKNOWN)
            if (value !in 0..6_553_500) return Result(State.UNKNOWN)
            value
        } else null
        val instruction = match?.groupValues?.get(3) ?: text
        val recognizedState = if (meters == null) State.INCOMPLETE else State.MANEUVER
        if (Regex("\\b(then|depois|em seguida)\\b").containsMatchIn(instruction)) return Result(State.UNKNOWN)
        val roundabout = Regex("^(?:at the roundabout, take the ([1-9])(?:st|nd|rd|th) exit|na rotatoria, (?:pegue|tome) a ([1-9])[ªa]? saida)(?:$|\\s+(?:onto|and stay on|para|em)\\b.*)")
            .matchEntire(instruction)
        if (roundabout != null) return Result(recognizedState, Direction.ROUNDABOUT, meters,
            roundabout.groupValues.drop(1).first { it.isNotEmpty() }.toInt())
        val patterns = listOf(
            "(?:turn right|vire a direita|vire para a direita)" to Direction.RIGHT,
            "(?:turn left|vire a esquerda|vire para a esquerda)" to Direction.LEFT,
            "slight right" to Direction.SLIGHT_RIGHT,
            "slight left" to Direction.SLIGHT_LEFT,
            "(?:continue straight|siga em frente|continue em frente)" to Direction.STRAIGHT,
            "(?:keep right|mantenha-se a direita)" to Direction.KEEP_RIGHT,
            "(?:keep left|mantenha-se a esquerda)" to Direction.KEEP_LEFT,
            "merge" to Direction.MERGE,
            "at the roundabout, turn right" to Direction.ROUNDABOUT_RIGHT,
            "exit the roundabout" to Direction.EXIT_ROUNDABOUT,
        )
        val direction = patterns.firstOrNull { (pattern, _) ->
            Regex("^$pattern(?:$|[.!]|\\s+(?:onto|on|at|after|to stay on|toward|towards|na|no|em|para)\\b.*)").matches(instruction)
        }?.second ?: return Result(if (meters == null) State.INCOMPLETE else State.UNKNOWN)
        return Result(recognizedState, direction, meters)
    }
}
