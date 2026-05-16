package hr.zet.transit.ui.stop

/**
 * Formatira predviđeno vrijeme dolaska u kratak ETA string ("za 3 min").
 *
 * Čista funkcija — `now` se ubacuje da bude testabilna bez stvarnog sata.
 */
internal fun formatEta(
    predictedEpochSeconds: Long,
    nowEpochSeconds: Long = System.currentTimeMillis() / 1000,
): String {
    val deltaSeconds = predictedEpochSeconds - nowEpochSeconds
    return when {
        deltaSeconds <= 30 -> "stiže"
        deltaSeconds < 60 -> "<1 min"
        else -> "za ${deltaSeconds / 60} min"
    }
}
