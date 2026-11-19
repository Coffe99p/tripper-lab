package com.joaofabijack.tripper

/** A fresh notification replaces the entire instruction. Never reuse a distance or
 * resurrect another notification when the selected source disappears. Monotonic time. */
class MapsRouteState(private val startedAt: Long) {
    data class Guidance(
        val maneuver: TripperProtocol.Maneuver? = null,
        val meters: Int? = null,
        val exit: Int? = null,
        val status: String,
        val stop: Boolean = false,
    )
    companion object { const val FRESH_MS = 30_000L }
    private var selectedKey: String? = null
    private var updatedAt = startedAt
    private var ended = false
    private var guidance = Guidance(status = "Aguardando uma instrução atual do Maps.")

    fun posted(key: String, result: MapsInstructionParser.Result, now: Long, sourceAgeMs: Long = 0): Guidance {
        if (ended) return guidance
        if (result.state == MapsInstructionParser.State.NOT_NAVIGATION) {
            if (key == selectedKey) {
                selectedKey = null
                guidance = Guidance(status = "O Maps deixou de informar navegação. Aguardando dados.")
            }
            return current(now)
        }
        // An old snapshot is not made fresh merely by enabling the bridge.
        if (sourceAgeMs < 0 || sourceAgeMs >= FRESH_MS) return current(now)
        selectedKey = key
        updatedAt = now - sourceAgeMs
        guidance = when (result.state) {
            MapsInstructionParser.State.ARRIVED -> Guidance(status = "Maps informou chegada. Encerrando a sessão.", stop = true)
            MapsInstructionParser.State.APPROACHING_ARRIVAL -> Guidance(status = "Aproximando-se do destino. Aguardando o Maps encerrar.")
            MapsInstructionParser.State.RECALCULATING -> Guidance(status = "Maps recalculando a rota.")
            MapsInstructionParser.State.INCOMPLETE -> Guidance(status = "Instrução incompleta: aguardando manobra e distância.")
            MapsInstructionParser.State.MANEUVER -> translate(result)
            else -> Guidance(status = "Instrução não reconhecida. Aguardando dados do Maps.")
        }
        ended = guidance.stop
        return guidance
    }

    fun removed(key: String, now: Long): Guidance {
        if (!ended && key == selectedKey) {
            ended = true
            guidance = Guidance(status = "A notificação da rota foi removida. Sessão encerrada.", stop = true)
        }
        return current(now)
    }

    fun current(now: Long): Guidance {
        if (!ended && now - updatedAt >= FRESH_MS) {
            guidance = Guidance(status = "Maps sem atualização há 30 segundos. Aguardando dados atuais.")
        }
        return guidance
    }

    private fun translate(result: MapsInstructionParser.Result): Guidance {
        val meters = result.meters ?: return Guidance(status = "Distância ausente. Aguardando dados do Maps.")
        if (meters !in 0..6_553_500) return Guidance(status = "Distância fora do intervalo. Aguardando dados.")
        val maneuver = when (result.direction) {
            MapsInstructionParser.Direction.LEFT -> TripperProtocol.Maneuver.LEFT
            MapsInstructionParser.Direction.RIGHT -> TripperProtocol.Maneuver.RIGHT
            MapsInstructionParser.Direction.SLIGHT_LEFT -> TripperProtocol.Maneuver.SLIGHT_LEFT
            MapsInstructionParser.Direction.SLIGHT_RIGHT -> TripperProtocol.Maneuver.SLIGHT_RIGHT
            MapsInstructionParser.Direction.STRAIGHT -> TripperProtocol.Maneuver.STRAIGHT
            MapsInstructionParser.Direction.KEEP_LEFT -> TripperProtocol.Maneuver.KEEP_LEFT
            MapsInstructionParser.Direction.KEEP_RIGHT -> TripperProtocol.Maneuver.KEEP_RIGHT
            MapsInstructionParser.Direction.MERGE -> TripperProtocol.Maneuver.MERGE
            MapsInstructionParser.Direction.ROUNDABOUT -> {
                if (result.exit !in 1..9) return Guidance(status = "Saída da rotatória ausente ou não suportada.")
                TripperProtocol.Maneuver.ROUNDABOUT
            }
            // Neither the exit number nor the geometry can be inferred from these titles.
            else -> return Guidance(status = "Manobra ainda sem correspondência no Tripper. Aguardando dados.")
        }
        val description = when (maneuver) {
            TripperProtocol.Maneuver.LEFT -> "Esquerda"
            TripperProtocol.Maneuver.RIGHT -> "Direita"
            TripperProtocol.Maneuver.SLIGHT_LEFT -> "Curva suave à esquerda"
            TripperProtocol.Maneuver.SLIGHT_RIGHT -> "Curva suave à direita"
            TripperProtocol.Maneuver.STRAIGHT -> "Em frente"
            TripperProtocol.Maneuver.KEEP_LEFT -> "Manter à esquerda"
            TripperProtocol.Maneuver.KEEP_RIGHT -> "Manter à direita"
            TripperProtocol.Maneuver.MERGE -> "Entrar na via"
            else -> "Rotatória, saída ${result.exit}"
        }
        return Guidance(maneuver, meters, result.exit, "$description • $meters m")
    }
}
