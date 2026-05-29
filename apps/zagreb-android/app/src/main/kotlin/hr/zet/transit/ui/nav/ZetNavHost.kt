package hr.zet.transit.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import hr.zet.transit.ui.alerts.AlertsScreen
import hr.zet.transit.ui.common.BackTopBar
import hr.zet.transit.ui.favorites.FavoritesScreen
import hr.zet.transit.ui.map.MapScreen
import hr.zet.transit.ui.nearby.NearbyScreen
import hr.zet.transit.ui.plan.PlanScreen
import hr.zet.transit.ui.routes.RouteDetailScreen
import hr.zet.transit.ui.routes.RoutesScreen
import hr.zet.transit.ui.routines.RoutinesScreen
import hr.zet.transit.ui.search.SearchScreen
import hr.zet.transit.ui.stop.StopDetailScreen

/**
 * Navigacijski graf aplikacije. Karta je početni ekran; iz nje se dolazi do
 * stajalište-detalja (tap na karti), pregleda linija, omiljenih i pretrage.
 *
 * Svi detail ekrani imaju TopAppBar s back-arrow gumbom (BackTopBar).
 * MapScreen nema jer je root i ima vlastiti UI s FAB-ovima.
 */
@Composable
fun ZetNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.MAP,
        modifier = modifier,
    ) {
        composable(Destinations.MAP) {
            MapScreen(
                onStopClick = { navController.navigateToStop(it) },
                onRoutesClick = { navController.navigate(Destinations.ROUTES) },
                onFavoritesClick = { navController.navigate(Destinations.FAVORITES) },
                onSearchClick = { navController.navigate(Destinations.SEARCH) },
                onAlertsClick = { navController.navigate(Destinations.ALERTS) },
                onRoutinesClick = { navController.navigate(Destinations.ROUTINES) },
                onNearbyClick = { navController.navigate(Destinations.NEARBY) },
                onPlanClick = { navController.navigate(Destinations.PLAN) },
            )
        }

        composable(
            route = Destinations.STOP_DETAIL,
            arguments = listOf(
                navArgument(Destinations.ARG_STOP_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            ScreenWithBack(title = "Stajalište", onBack = { navController.popBackStack() }) { pad ->
                StopDetailScreen(
                    stopId = entry.arguments?.getString(Destinations.ARG_STOP_ID).orEmpty(),
                    modifier = Modifier.padding(pad),
                )
            }
        }

        composable(
            route = Destinations.ROUTE_DETAIL,
            arguments = listOf(
                navArgument(Destinations.ARG_ROUTE_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            ScreenWithBack(title = "Linija", onBack = { navController.popBackStack() }) { pad ->
                RouteDetailScreen(
                    routeId = entry.arguments?.getString(Destinations.ARG_ROUTE_ID).orEmpty(),
                    modifier = Modifier.padding(pad),
                )
            }
        }

        composable(Destinations.ROUTES) {
            ScreenWithBack(title = "Linije", onBack = { navController.popBackStack() }) { pad ->
                RoutesScreen(
                    onRouteClick = { navController.navigateToRoute(it) },
                    modifier = Modifier.padding(pad),
                )
            }
        }

        composable(Destinations.FAVORITES) {
            ScreenWithBack(title = "Omiljeni", onBack = { navController.popBackStack() }) { pad ->
                FavoritesScreen(
                    onStopClick = { navController.navigateToStop(it) },
                    modifier = Modifier.padding(pad),
                )
            }
        }

        composable(Destinations.SEARCH) {
            ScreenWithBack(title = "Pretraga", onBack = { navController.popBackStack() }) { pad ->
                SearchScreen(
                    onStopClick = { navController.navigateToStop(it) },
                    onRouteClick = { navController.navigateToRoute(it) },
                    modifier = Modifier.padding(pad),
                )
            }
        }

        composable(Destinations.ALERTS) {
            ScreenWithBack(title = "Obavijesti", onBack = { navController.popBackStack() }) { pad ->
                AlertsScreen(modifier = Modifier.padding(pad))
            }
        }

        composable(Destinations.ROUTINES) {
            ScreenWithBack(title = "Rutine", onBack = { navController.popBackStack() }) { pad ->
                RoutinesScreen(
                    onStopClick = { navController.navigateToStop(it) },
                    modifier = Modifier.padding(pad),
                )
            }
        }

        composable(Destinations.NEARBY) {
            ScreenWithBack(title = "U blizini", onBack = { navController.popBackStack() }) { pad ->
                NearbyScreen(
                    onStopClick = { navController.navigateToStop(it) },
                    modifier = Modifier.padding(pad),
                )
            }
        }

        composable(Destinations.PLAN) {
            ScreenWithBack(title = "Planiranje rute", onBack = { navController.popBackStack() }) { pad ->
                PlanScreen(modifier = Modifier.padding(pad))
            }
        }
    }
}

@Composable
private fun ScreenWithBack(
    title: String,
    onBack: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(topBar = { BackTopBar(title = title, onBack = onBack) }) { pad ->
        content(pad)
    }
}

private fun NavController.navigateToStop(stopId: String) =
    navigate(Destinations.stopDetail(stopId))

private fun NavController.navigateToRoute(routeId: String) =
    navigate(Destinations.routeDetail(routeId))
