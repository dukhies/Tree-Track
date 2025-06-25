package com.example.treetrack

import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.EditText
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.*

class TrackTreesActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var fabCenterLocation: FloatingActionButton
    private val REQUEST_PERM = 1
    private var userLocationMarker: Marker? = null

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        setContentView(R.layout.activity_track_trees)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        findViewById<FloatingActionButton>(R.id.fabAddTree).setOnClickListener {
            addCurrentTree()
        }

        fabCenterLocation = findViewById(R.id.fabCenterLocation)
        fabCenterLocation.setOnClickListener {
            moveToUserLocation()
        }

        TreeStorage.loadTrees {
            Log.d("TrackTreesActivity", "✅ Trees loaded: ${TreeStorage.getTrees().size}")
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERM
            )
        } else {
            moveToUserLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERM && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            moveToUserLocation()
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        fusedClient.lastLocation
            .addOnSuccessListener { loc ->
                loc?.let {
                    val pt = GeoPoint(it.latitude, it.longitude)
                    map.controller.setZoom(16.0)
                    map.controller.setCenter(pt)
                    showUserLocationMarker(pt)
                    renderAllTreeMarkers()
                }
            }
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(5000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val point = GeoPoint(location.latitude, location.longitude)
                showUserLocationMarker(point)
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun showUserLocationMarker(geoPoint: GeoPoint) {
        if (userLocationMarker == null) {
            userLocationMarker = Marker(map).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "You are here"
                icon = getDrawable(R.drawable.ic_my_location_dot)
            }
            map.overlays.add(userLocationMarker)
        }
        userLocationMarker?.position = geoPoint
        map.invalidate()
    }

    private fun addCurrentTree() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        // Show input dialog first
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_tree, null)
        val inputName = dialogView.findViewById<EditText>(R.id.editTreeName)
        val inputDesc = dialogView.findViewById<EditText>(R.id.editTreeDesc)

        AlertDialog.Builder(this)
            .setTitle("Add Tree Info")
            .setView(dialogView)
            .setPositiveButton("Pin Tree") { _, _ ->
                val name = inputName.text.toString().ifBlank { "Unnamed Tree" }
                val desc = inputDesc.text.toString()

                fusedClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if (loc == null) {
                            Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val tree = TreeData(
                            name = name,
                            description = desc,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            datePlanted = System.currentTimeMillis(),
                            health = 100
                        )

                        TreeStorage.addTree(context = this@TrackTreesActivity, tree = tree) {
                            runOnUiThread {
                                Toast.makeText(this, "Tree pinned!", Toast.LENGTH_SHORT).show()
                                renderAllTreeMarkers()
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addMarker(tree: TreeData) {
        val geoPoint = GeoPoint(tree.latitude, tree.longitude)
        val marker = Marker(map)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = tree.name
        marker.subDescription = "Planted: ${
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(tree.datePlanted))
        }"
        marker.setOnMarkerClickListener { m, _ ->
            m.showInfoWindow()
            true
        }
        map.overlays.add(marker)
    }

    private fun renderAllTreeMarkers() {
        map.overlays.removeAll { it is Marker && it != userLocationMarker }
        TreeStorage.getTrees().forEach { addMarker(it) }
        map.invalidate()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        stopLocationUpdates()
    }
}