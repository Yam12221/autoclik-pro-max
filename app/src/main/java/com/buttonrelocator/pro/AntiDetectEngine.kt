package com.buttonrelocator.pro

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Engine responsible for humanizing touch interactions and avoiding static pattern detection by game anti-cheat systems.
 */
object AntiDetectEngine {

    enum class SecurityLevel {
        STEALTH,   // Maximum protection: Gaussian coordinate jitter + humanized stroke duration + timing variance
        BALANCED,  // Medium protection: Small coordinate jitter + standard duration
        DIRECT     // Direct instant mode (No jitter)
    }

    data class HumanizedClick(
        val x: Int,
        val y: Int,
        val durationMs: Long,
        val preDelayMs: Long = 0L
    )

    private val javaRandom = java.util.Random()

    fun calculateClick(
        baseX: Int,
        baseY: Int,
        level: SecurityLevel = SecurityLevel.STEALTH,
        jitterRadius: Int = 4
    ): HumanizedClick {
        return when (level) {
            SecurityLevel.STEALTH -> {
                val dx = (javaRandom.nextGaussian() * (jitterRadius / 2.0)).roundToInt().coerceIn(-jitterRadius, jitterRadius)
                val dy = (javaRandom.nextGaussian() * (jitterRadius / 2.0)).roundToInt().coerceIn(-jitterRadius, jitterRadius)
                val duration = Random.nextLong(18, 46)
                val preDelay = Random.nextLong(1, 7)

                HumanizedClick(
                    x = baseX + dx,
                    y = baseY + dy,
                    durationMs = duration,
                    preDelayMs = preDelay
                )
            }
            SecurityLevel.BALANCED -> {
                val dx = Random.nextInt(-jitterRadius / 2, (jitterRadius / 2) + 1)
                val dy = Random.nextInt(-jitterRadius / 2, (jitterRadius / 2) + 1)
                val duration = Random.nextLong(10, 25)

                HumanizedClick(
                    x = baseX + dx,
                    y = baseY + dy,
                    durationMs = duration,
                    preDelayMs = 0L
                )
            }
            SecurityLevel.DIRECT -> {
                HumanizedClick(
                    x = baseX,
                    y = baseY,
                    durationMs = 1L,
                    preDelayMs = 0L
                )
            }
        }
    }
}
