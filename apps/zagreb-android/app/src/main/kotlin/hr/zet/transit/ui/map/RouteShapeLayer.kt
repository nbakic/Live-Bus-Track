package hr.zet.transit.ui.map

import hr.zet.transit.domain.model.RouteShape
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Crta geometriju rute (A1.2) kao LineLayer na MapLibre karti.
 *
 * Geometrija dolazi iz GTFS `shapes.txt` preko backenda. Sloj se dodaje
 * ispod slojeva vozila/stajališta — linija je pozadina, markeri su iznad.
 */
class RouteShapeLayer {

    fun attach(style: Style) {
        if (style.getSource(SOURCE_ID) != null) return
        style.addSource(GeoJsonSource(SOURCE_ID))
        style.addLayer(
            LineLayer(LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.lineColor("#E30613"),
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineOpacity(0.85f),
            ),
        )
    }

    /** Postavlja geometriju rute; prazan shape obriše liniju. */
    fun setShape(map: MapLibreMap, shape: RouteShape?) {
        val source = map.style?.getSourceAs<GeoJsonSource>(SOURCE_ID) ?: return
        val points = shape?.points?.map { Point.fromLngLat(it.lng, it.lat) }
        if (points.isNullOrEmpty()) {
            source.setGeoJson(LineString.fromLngLats(emptyList()))
        } else {
            source.setGeoJson(LineString.fromLngLats(points))
        }
    }

    private companion object {
        const val SOURCE_ID = "route-shape-source"
        const val LAYER_ID = "route-shape-layer"
    }
}
