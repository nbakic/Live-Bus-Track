package hr.zet.transit.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import hr.zet.transit.ui.map.MapScreen
import hr.zet.transit.ui.routes.RoutesScreen
import hr.zet.transit.ui.stop.StopDetailScreen

/**
 * Navigacijski graf aplikacije. Karta je početni ekran; iz nje se ide na
 * stajalište-detalje (tap na karti) i na pregled linija.
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
                onStopClick = { stopId ->
                    navController.navigate(Destinations.stopDetail(stopId))
                },
                onRoutesClick = {
                    navController.navigate(Destinations.ROUTES)
                },
            )
        }

        composable(
            route = Destinations.STOP_DETAIL,
            arguments = listOf(
                navArgument(Destinations.ARG_STOP_ID) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val stopId = backStackEntry.arguments
                ?.getString(Destinations.ARG_STOP_ID)
                .orEmpty()
            StopDetailScreen(stopId = stopId)
        }

        composable(Destinations.ROUTES) {
            RoutesScreen()
        }
    }
}
