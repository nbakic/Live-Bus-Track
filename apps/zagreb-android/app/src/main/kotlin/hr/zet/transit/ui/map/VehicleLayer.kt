package hr.zet.transit.ui.map

import hr.zet.transit.domain.model.TransitMode
import hr.zet.transit.domain.model.Vehicle
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Iscrtava živa vozila (A0.1) kao GeoJSON sloj na MapLibre karti.
 *
 * GeoJSON source + CircleLayer skalira na stotine vozila bez per-marker
 * objekata — bitno za metriku "60 fps @ 200 vozila" (plan sekcija 7).
 * Ažuriranje je samo zamjena podataka u sourceu, bez rekreiranja sloja.
 *
 * Sljedeći korak: zamijeniti CircleLayer SymbolLayerom s ikonom vozila i
 * smjer-strelicom (`iconRotate` ← `bearing`) — traži registriranu ikonu u
 * stilu, dolazi uz vlastiti tile stil u Fazi 0.
 */
class VehicleLayer {

    /** Poziva se jednom kad je stil učitan — registrira source i sloj. */
    fun attach(style: Style) {
        if (style.getSource(SOURCE_ID) != null) return
        style.addSource(GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(emptyList())))
        style.addLayer(
            CircleLayer(LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(6f),
                PropertyFactory.circleStrokeWidth(1.5f),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
                // Tramvaj plav, autobus narančast — privremena paleta do design tokena.
                PropertyFactory.circleColor(
                    Expression.match(
                        Expression.get(PROP_MODE),
                        Expression.literal("#FF8C00"),                          // default: bus
                        Expression.stop(TransitMode.TRAM.name, "#0066CC"),
                    ),
                ),
            ),
        )
    }

    /** Ažurira pozicije vozila — samo zamjena GeoJSON podataka. */
    fun update(map: MapLibreMap, vehicles: List<Vehicle>) {
        val source = map.style?.getSourceAs<GeoJsonSource>(SOURCE_ID) ?: return
        val features = vehicles.map { vehicle ->
            Feature.fromGeometry(
                Point.fromLngLat(vehicle.position.lng, vehicle.position.lat),
            ).apply {
                addStringProperty(PROP_ID, vehicle.id)
                addStringProperty(PROP_MODE, vehicle.mode.name)
                addNumberProperty(PROP_BEARING, vehicle.heading?.degrees ?: 0f)
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private companion object {
        const val SOURCE_ID = "vehicles-source"
        const val LAYER_ID = "vehicles-layer"
        const val PROP_ID = "id"
        const val PROP_MODE = "mode"
        const val PROP_BEARING = "bearing"
    }
}
