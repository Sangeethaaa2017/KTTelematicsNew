package com.example.samktt

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var mMap: GoogleMap
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var locationHistory = mutableListOf<LatLng>()
    private var playbackIndex = 0
    private var isPlaying = false
    private var markers = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Get latitude and longitude from intent extras
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)

        mapView = findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Retrieve location history from Firebase
        retrieveUserData()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add marker for the selected location
        val location = LatLng(latitude, longitude)
        val marker = mMap.addMarker(MarkerOptions().position(location).title("Selected Location"))
        if (marker != null) {
            markers.add(marker)
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))

        // Set a listener for marker click
        mMap.setOnMarkerClickListener { marker ->
            Toast.makeText(this, marker.title, Toast.LENGTH_SHORT).show()
            true
        }
    }

    // Function to handle playback button click
    fun startPlayback(view: View) {
        if (locationHistory.isNotEmpty()) {
            if (!isPlaying) {
                isPlaying = true
                playbackIndex = 0
                playNextLocation()
            } else {
                isPlaying = false
            }
        } else {
            Toast.makeText(this, "Location history is empty", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to play next location in location history
    // Function to play next location in location history with animation
    private fun playNextLocation() {
        if (playbackIndex < locationHistory.size) {
            val nextLocation = locationHistory[playbackIndex]
            val cameraPosition = CameraPosition.Builder()
                .target(nextLocation)
                .zoom(15f)
                .build()
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
            mMap.animateCamera(cameraUpdate, 1000, object : GoogleMap.CancelableCallback {
                override fun onFinish() {
                    if (isPlaying) {
                        playbackIndex++
                        playNextLocation()
                    } else {
                        // Playback finished, show success toast
                        Toast.makeText(this@MapsActivity, "Playback successful", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancel() {
                    // Handle cancellation if needed
                }
            })
        } else {
            isPlaying = false
        }
    }


    private fun retrieveUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("Users").child(userId.orEmpty())

        // Retrieve the last login timestamp
        userRef.child("lastLogin").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val lastLoginTimestamp = dataSnapshot.getValue(Long::class.java) ?: 0

                // Query the location data after the last login timestamp
                val locationRef = userRef.child("locations").orderByChild("timestamp").startAt(lastLoginTimestamp.toString())

                locationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (locationSnapshot in dataSnapshot.children) {
                            val latitude = locationSnapshot.child("latitude").getValue(Double::class.java)
                            val longitude = locationSnapshot.child("longitude").getValue(Double::class.java)
                            if (latitude != null && longitude != null) {
                                val location = LatLng(latitude, longitude)
                                locationHistory.add(location)
                                // Add marker for each location
                                val marker = mMap.addMarker(MarkerOptions().position(location).title("Location"))
                                if (marker != null) {
                                    markers.add(marker)
                                }
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle errors
                        Log.e("UserData", "Failed to retrieve location data: ${databaseError.message}")
                    }
                })
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
                Log.e("UserData", "Failed to retrieve last login timestamp: ${databaseError.message}")
            }
        })
    }



    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
