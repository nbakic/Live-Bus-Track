package hr.zet.transit.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import hr.zet.transit.data.local.db.RoutineEntity
import hr.zet.transit.data.local.db.TransitDatabase
import hr.zet.transit.domain.model.Routine
import hr.zet.transit.domain.model.RoutineKind
import hr.zet.transit.domain.repository.RoutineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Rutine Dom/Posao/Škola — lokalno u SQLDelight bazi, bez backenda (A1.5).
 */
class RoutineRepositoryImpl(
    private val db: TransitDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RoutineRepository {

    private val queries get() = db.transitQueries

    override fun observeRoutines(): Flow<List<Routine>> =
        queries.selectRoutines()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun setRoutine(routine: Routine) = withContext(ioDispatcher) {
        queries.upsertRoutine(
            kind = routine.kind.name,
            stopId = routine.stopId,
            stopName = routine.stopName,
        )
    }

    override suspend fun clearRoutine(kind: RoutineKind) = withContext(ioDispatcher) {
        queries.deleteRoutine(kind.name)
    }
}

/** Nepoznat `kind` u bazi se preskače umjesto da ruši cijeli upit. */
private fun RoutineEntity.toDomain(): Routine? {
    val parsedKind = runCatching { RoutineKind.valueOf(kind) }.getOrNull() ?: return null
    return Routine(kind = parsedKind, stopId = stopId, stopName = stopName)
}
