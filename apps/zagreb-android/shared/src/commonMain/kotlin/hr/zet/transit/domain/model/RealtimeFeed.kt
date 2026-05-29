package hr.zet.transit.domain.model

/**
 * Snapshot živog feeda — wrappa podatke i znak je li RT veza s backendom živa.
 *
 * Bez ovoga UI ne razlikuje "prazna lista jer nema vozila" od "prazna lista
 * jer backend ne odgovara" — drugo treba pokazati kao degradirano stanje.
 */
data class RealtimeFeed<T>(
    val data: List<T>,
    val isLive: Boolean,
)
