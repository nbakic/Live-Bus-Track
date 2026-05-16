package hr.zet.transit.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import hr.zet.transit.data.local.db.TransitDatabase
import hr.zet.transit.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Omiljena stajališta — lokalno u SQLDelight bazi, bez backenda (A0.3 plana).
 */
class FavoritesRepositoryImpl(
    private val db: TransitDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : FavoritesRepository {

    private val queries get() = db.transitQueries

    override fun observeFavoriteStopIds(): Flow<Set<String>> =
        queries.selectFavorites()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { it.toSet() }

    override suspend fun addFavorite(stopId: String) = withContext(ioDispatcher) {
        queries.insertFavorite(stopId)
    }

    override suspend fun removeFavorite(stopId: String) = withContext(ioDispatcher) {
        queries.deleteFavorite(stopId)
    }

    override suspend fun isFavorite(stopId: String): Boolean = withContext(ioDispatcher) {
        queries.selectFavorites().executeAsList().contains(stopId)
    }
}
