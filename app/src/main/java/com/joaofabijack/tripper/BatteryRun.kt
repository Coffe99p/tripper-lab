package com.joaofabijack.tripper

/** Whole-device observations, never an attribution to one application. */
class BatteryRun(val mode: Mode, val start: Sample) {
    enum class Mode { MAPS_ONLY, MAPS_TRIPPER }
    data class Sample(val elapsedMs: Long, val percent: Int?, val chargeUah: Long?,
        val temperatureC: Double?, val plugged: Boolean?, val screenOn: Boolean)
    data class Result(val mode: Mode, val start: Sample, val end: Sample, val durationMs: Long,
        val screenOnMs: Long, val percentDrop: Int?, val chargeMah: Double?, val issues: List<String>) {
        val comparable get() = issues.isEmpty()
        val mahPerHour get() = if (comparable && chargeMah != null && chargeMah > 0)
            chargeMah * 3_600_000 / durationMs else null
    }
    companion object {
        const val MIN_MS = 20 * 60 * 1000L
        const val MAX_MS = 60 * 60 * 1000L
    }
    private var last = start
    private var screenOnMs = 0L
    private val issues = linkedSetOf<String>()
    init { inspect(start) }
    fun invalidate(reason: String) { issues += reason }
    private fun inspect(sample: Sample) {
        if (sample.plugged != false) issues += "Carregador conectado ou estado de alimentação indisponível."
        if (sample.percent == null) issues += "Porcentagem da bateria indisponível."
    }
    fun observe(sample: Sample) {
        if (sample.elapsedMs < last.elapsedMs) issues += "Relógio da medição interrompido."
        else if (last.screenOn) screenOnMs += sample.elapsedMs - last.elapsedMs
        if (sample.chargeUah != null && last.chargeUah != null && sample.chargeUah > last.chargeUah!!)
            issues += "O contador de carga aumentou durante a medição."
        if (sample.percent != null && last.percent != null && sample.percent > last.percent!!)
            issues += "A porcentagem da bateria aumentou durante a medição."
        inspect(sample)
        last = sample
    }
    fun finish(end: Sample): Result {
        observe(end)
        val duration = end.elapsedMs - start.elapsedMs
        if (duration < MIN_MS) issues += "Amostra curta: use pelo menos 20 minutos para comparar."
        if (duration > MAX_MS) issues += "Amostra excedeu 60 minutos."
        val percent = if (start.percent != null && end.percent != null) start.percent - end.percent else null
        val charge = if (start.chargeUah != null && end.chargeUah != null)
            (start.chargeUah - end.chargeUah) / 1000.0 else null
        if (percent == 0 && (charge == null || charge == 0.0))
            issues += "Variação abaixo da resolução disponível; não significa consumo zero."
        return Result(mode, start, end, duration, screenOnMs, percent, charge, issues.toList())
    }
}
