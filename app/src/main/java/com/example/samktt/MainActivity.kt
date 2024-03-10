package com.example.samktt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var locationAdapter: LocationAdapter
    private lateinit var locationManager: LocationManager
    private val locationList = mutableListOf<LocationData>()
    private lateinit var toolbar: LinearLayout
    private lateinit var backArrowImageView: ImageView

    private val PERMISSION_REQUEST_CODE = 1001
    private val LOCATION_UPDATE_INTERVAL = 15 * 1000L // 15 seconds in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startLocationService()

        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar)
        backArrowImageView = findViewById(R.id.backArrowImageView)

        // Set click listener for the back arrow ImageView
        backArrowImageView.setOnClickListener {
            onBackPressed()
        }
        val logoutImageView: ImageView = findViewById(R.id.logoutImageView)
        logoutImageView.setOnClickListener {
            logoutUser()
        }


        // Set toolbar color
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow))

        firebaseAuth = FirebaseAuth.getInstance()
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize location adapter and handle item click
        locationAdapter = LocationAdapter(locationList) { locationData ->
            // Handle item click here
            val intent = Intent(this@MainActivity, MapsActivity::class.java).apply {
                putExtra("latitude", locationData.latitude)
                putExtra("longitude", locationData.longitude)
            }
            startActivity(intent)
        }

        recyclerView.adapter = locationAdapter

        // Initialize LocationManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Start listening for location updates
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        // Immediate location retrieval and storage upon login
        getLocationAndSaveToFirebase()

        // Schedule location updates every 15 seconds
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                getLocationAndSaveToFirebase()
                handler.postDelayed(this, LOCATION_UPDATE_INTERVAL)
            }
        }, LOCATION_UPDATE_INTERVAL)
    }

    private fun getLocationAndSaveToFirebase() {
        try {
            // Get last known location
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocation?.let {
                // Extract latitude and longitude
                val latitude = it.latitude
                val longitude = it.longitude
                // Create LocationData object

                val currentTimeMillis = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedTime = dateFormat.format(Date(currentTimeMillis))
                val currentTime = System.currentTimeMillis().toString()
                val locationData = LocationData(latitude, longitude, formattedTime)
                // Add to locationList
                locationList.add(locationData)
                // Notify adapter of data change
                locationAdapter.notifyDataSetChanged()

                // Save location data to Firebase
                saveLocationToFirebase(latitude, longitude, currentTime)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLocationToFirebase(latitude: Double, longitude: Double, timestamp: String) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        user?.let {
            val database = FirebaseDatabase.getInstance()
            val userId = user.uid
            val userRef = database.getReference("Users").child(userId)
            val locationData = LocationData(latitude, longitude, timestamp)
            userRef.child("locations").push().setValue(locationData)
                .addOnSuccessListener {
                    println("Location data stored successfully for user: $userId")
                }
                .addOnFailureListener { e ->
                    println("Failed to store location data for user: $userId, error: ${e.message}")
                }
        }
    }

    // Function to handle logout button click
    private fun logoutUser() {
        firebaseAuth.signOut()
        // Redirect to SignInActivity or any other appropriate action
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish() // Finish MainActivity to prevent going back to it with back button
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        startService(serviceIntent)
    }
    override fun onBackPressed() {
        // Handle back button press here
        super.onBackPressed()
        finish()
    }
}
