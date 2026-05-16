package hr.zet.transit.domain.usecase

import hr.zet.transit.domain.TextNormalizer
import hr.zet.transit.domain.model.Route
import hr.zet.transit.domain.model.Stop
import hr.zet.transit.domain.repository.StaticRepository

/**
 * Pretraga stajališta i linija (A1.3 plana) — dijakritika-neutralna.
 *
 * Linije se podudaraju po oznaci ili imenu; stajališta po imenu.
 * Prazan upit vraća prazan rezultat (UI ne prikazuje cijelu bazu).
 */
class SearchUseCase(
    private val staticRepository: StaticRepository,
) {
    /** Rezultat pretrage — linije i stajališta odvojeni za grupiran prikaz. */
    data class Results(
        val routes: List<Route>,
        val stops: List<Stop>,
    ) {
        val isEmpty: Boolean get() = routes.isEmpty() && stops.isEmpty()
    }

    suspend operator fun invoke(query: String): Results {
        if (query.isBlank()) return Results(emptyList(), emptyList())

        val routes = staticRepository.getRoutes().filter { route ->
            TextNormalizer.matches(route.shortName, query) ||
                TextNormalizer.matches(route.longName, query)
        }
        val stops = staticRepository.getStops().filter { stop ->
            TextNormalizer.matches(stop.name, query)
        }
        return Results(
            routes = routes.take(MAX_RESULTS_PER_TYPE),
            stops = stops.take(MAX_RESULTS_PER_TYPE),
        )
    }

    private companion object {
        /** Ograniči rezultate — duga lista nije korisna ni brza za render. */
        const val MAX_RESULTS_PER_TYPE = 30
    }
}
