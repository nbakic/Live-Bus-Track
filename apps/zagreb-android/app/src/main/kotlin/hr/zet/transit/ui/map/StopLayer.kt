package hr.zet.transit.ui.map

import hr.zet.transit.domain.model.Stop
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Iscrtava stajališta kao GeoJSON sloj. Svaki feature nosi `stopId` —
 * tap na karti vraća feature, iz njega se čita ID i navigira na detalje.
 *
 * Stajališta su statična (rijetko se mijenjaju), pa se sloj puni jednom.
 * Sloj se dodaje ispod sloja vozila — vozila su uvijek iznad stajališta.
 */
class StopLayer {

    fun attach(style: Style) {
        if (style.getSource(SOURCE_ID) != null) return
        style.addSource(GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(emptyList())))
        style.addLayer(
            CircleLayer(LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(4f),
                PropertyFactory.circleColor("#444444"),
                PropertyFactory.circleStrokeWidth(1f),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
            ),
        )
    }

    fun setStops(map: MapLibreMap, stops: List<Stop>) {
        val source = map.style?.getSourceAs<GeoJsonSource>(SOURCE_ID) ?: return
        val features = stops.map { stop ->
            Feature.fromGeometry(
                Point.fromLngLat(stop.position.lng, stop.position.lat),
            ).apply {
                addStringProperty(PROP_STOP_ID, stop.id)
                addStringProperty(PROP_STOP_NAME, stop.name)
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    companion object {
        const val LAYER_ID = "stops-layer"
        const val PROP_STOP_ID = "stopId"
        const val PROP_STOP_NAME = "stopName"
        private const val SOURCE_ID = "stops-source"
    }
}
