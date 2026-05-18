package hr.zet.transit.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import hr.zet.transit.ui.alerts.AlertsScreen
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
            StopDetailScreen(
                stopId = entry.arguments?.getString(Destinations.ARG_STOP_ID).orEmpty(),
            )
        }

        composable(
            route = Destinations.ROUTE_DETAIL,
            arguments = listOf(
                navArgument(Destinations.ARG_ROUTE_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            RouteDetailScreen(
                routeId = entry.arguments?.getString(Destinations.ARG_ROUTE_ID).orEmpty(),
            )
        }

        composable(Destinations.ROUTES) {
            RoutesScreen(onRouteClick = { navController.navigateToRoute(it) })
        }

        composable(Destinations.FAVORITES) {
            FavoritesScreen(onStopClick = { navController.navigateToStop(it) })
        }

        composable(Destinations.SEARCH) {
            SearchScreen(
                onStopClick = { navController.navigateToStop(it) },
                onRouteClick = { navController.navigateToRoute(it) },
            )
        }

        composable(Destinations.ALERTS) {
            AlertsScreen()
        }

        composable(Destinations.ROUTINES) {
            RoutinesScreen(onStopClick = { navController.navigateToStop(it) })
        }

        composable(Destinations.NEARBY) {
            NearbyScreen(onStopClick = { navController.navigateToStop(it) })
        }

        composable(Destinations.PLAN) {
            PlanScreen()
        }
    }
}

private fun NavController.navigateToStop(stopId: String) =
    navigate(Destinations.stopDetail(stopId))

private fun NavController.navigateToRoute(routeId: String) =
    navigate(Destinations.routeDetail(routeId))
