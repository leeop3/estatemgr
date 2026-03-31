package com.estate.manager.ui.map

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.estate.manager.R
import com.estate.manager.data.models.BunchRecord
import com.estate.manager.data.models.GangTrack
import com.estate.manager.data.models.TractorLocation
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

object MapManager {

    /**
     * Initialise an OSMDroid MapView configured for fully offline operation.
     * Tile cache directory is the app's internal cache — no SD card permission needed.
     */
    fun init(context: Context): MapView {
        Configuration.getInstance().apply {
            userAgentValue    = context.packageName
            osmdroidBasePath  = context.cacheDir
            osmdroidTileCache = File(context.cacheDir, "osm_tiles")
        }
        return MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(false)       // offline-only
            controller.setZoom(16.0)
            // Centre on a default estate coordinate — override from Settings later
            controller.setCenter(GeoPoint(3.14, 101.69))
            minZoomLevel = 10.0
            maxZoomLevel = 20.0
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Bunch markers  (coloured by status)
    // ─────────────────────────────────────────────────────────────
    fun renderBunchMarkers(map: MapView, records: List<BunchRecord>) {
        map.overlays.removeAll { it is Marker && (it as Marker).id?.startsWith("b_") == true }
        val ctx = map.context
        records.forEach { r ->
            Marker(map).apply {
                id       = "b_${r.id}"
                position = GeoPoint(r.lat, r.lon)
                icon     = ContextCompat.getDrawable(ctx, when {
                    r.unripe  > 0 -> R.drawable.ic_bunch_unripe
                    r.rotten  > 0 -> R.drawable.ic_bunch_rotten
                    r.damaged > 0 -> R.drawable.ic_bunch_damaged
                    else          -> R.drawable.ic_bunch_ripe
                })
                title   = "Block ${r.blockId}"
                snippet = "R:${r.ripe} U:${r.unripe} E:${r.empty} Ro:${r.rotten} D:${r.damaged}"
                setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                map.overlays.add(this)
            }
        }
        map.invalidate()
    }

    // ─────────────────────────────────────────────────────────────
    // Tractor live markers
    // ─────────────────────────────────────────────────────────────
    fun renderTractorMarkers(map: MapView, tractors: List<TractorLocation>) {
        map.overlays.removeAll { it is Marker && (it as Marker).id?.startsWith("t_") == true }
        val ctx = map.context
        tractors.forEach { t ->
            Marker(map).apply {
                id       = "t_${t.tractorId}"
                position = GeoPoint(t.lat, t.lon)
                icon     = ContextCompat.getDrawable(ctx, R.drawable.ic_tractor)
                title   = "Tractor ${t.tractorId}"
                snippet = "Driver: ${t.driverId}"
                setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                map.overlays.add(this)
            }
        }
        map.invalidate()
    }

    // ─────────────────────────────────────────────────────────────
    // Gang path polylines
    // Red = Pest & Disease, Green = Fertilizing
    // ─────────────────────────────────────────────────────────────
    fun renderGangPath(map: MapView, track: GangTrack) {
        val color = if (track.gangType == "PEST") Color.RED else Color.rgb(0, 160, 0)
        val line  = Polyline(map).apply {
            id = "g_${track.gangId}_${track.timestamp}"
            outlinePaint.apply {
                this.color       = color
                strokeWidth      = 6f
                strokeCap        = Paint.Cap.ROUND
                strokeJoin       = Paint.Join.ROUND
            }
            val pts = JSONArray(track.pathJson)
            setPoints((0 until pts.length()).map { i ->
                val p = pts.getJSONObject(i)
                GeoPoint(p.getDouble("la"), p.getDouble("lo"))
            })
        }
        map.overlays.add(line)
        map.invalidate()
    }

    /** Remove all gang polylines of a given type before re-rendering. */
    fun clearGangPaths(map: MapView, gangType: String) {
        val prefix = if (gangType == "PEST") "g_PEST" else "g_FERT"
        map.overlays.removeAll { it is Polyline && (it as Polyline).id?.contains(prefix) == true }
    }
}
