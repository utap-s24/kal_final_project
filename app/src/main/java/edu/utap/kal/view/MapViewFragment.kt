package edu.utap.kal.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.GeoPoint
import edu.utap.kal.MainViewModel
import edu.utap.kal.R
import edu.utap.kal.databinding.MapViewBinding

class MapViewFragment : Fragment (R.layout.map_view), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private var locationPermissionGranted = false
    private val viewModel: MainViewModel by activityViewModels()
    private val nearUTtower = LatLng(30.28621, -97.73943)
    var locationMarkers = mutableListOf<GeoPoint>()

    private fun requestPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    locationPermissionGranted = true
                } else -> {
                Toast.makeText(context,
                    "Unable to show location - permission required",
                    Toast.LENGTH_LONG).show()
            }
            }
        }
        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if( locationPermissionGranted ) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // We put this here to satisfy the compiler, which wants to know we checked
                // permissions before setting isMyLocationEnabled
                return
            }
            // XXX Write me.
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        }

        // Start the map at UT tower if there are no locations.
        // If there are, set it so that it encompasses all of the marked locations.
        if (locationMarkers.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(nearUTtower, 15.0f))
        } else {
            // Determine bounds for camera position
            val builder = LatLngBounds.Builder()
            for (markerPosition in locationMarkers) {
                val latLng = LatLng(markerPosition.latitude, markerPosition.longitude)
                builder.include(latLng)
            }
            val bounds = builder.build()
            // Move the camera
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = MapViewBinding.bind(view)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFrag) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestPermission()
        viewModel.fetchLocations()
        viewModel.observeLocations().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                locationMarkers = it.toMutableList()
                Log.d("XXX", "the markers include: ${locationMarkers}")

                // If the map has already been created, only THEN do the markers logic
                if (this::map.isInitialized) {
                    map.clear()
                    for (markerPosition in locationMarkers) {
                        val latLng = LatLng(markerPosition.latitude, markerPosition.longitude)
                        map.addMarker(MarkerOptions().position(latLng))
                    }
                }
            }
        }

    }

}