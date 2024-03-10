package com.example.samktt

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class LocationTrackingService : Service() {

    private lateinit var locationManager: LocationManager
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var locationListener: LocationListener

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val CHANNEL_ID = "LocationTrackingServiceChannel"
        private const val FOREGROUND_SERVICE_ID = 12345
        private const val LOCATION_UPDATE_INTERVAL = 15 * 60 * 1000L // 15 minutes in milliseconds
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startLocationUpdates()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        firebaseAuth = FirebaseAuth.getInstance()
        createNotificationChannel()
    }

    private fun startForegroundService() {
        // Build notification
        val notification = buildNotification()

        // Start service in the foreground
        startForeground(FOREGROUND_SERVICE_ID, notification)
    }

    private fun buildNotification(): Notification {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val notificationText = " $currentTime"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text) + " " + "...From:" + notificationText)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text) + notificationText)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_text),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val user = firebaseAuth.currentUser
                user?.let { saveLocationToFirebase(location, it) }
            }

            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Request location updates
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            LOCATION_UPDATE_INTERVAL,
            0f,
            locationListener
        )

        startForegroundService()
    }

    private fun saveLocationToFirebase(location: Location, user: FirebaseUser) {
        val database = FirebaseDatabase.getInstance()
        val userId = user.uid
        userId?.let {
            val userRef = database.getReference("Users").child(userId)
            val currentTime = Calendar.getInstance().time
            val locationData = LocationData(location.latitude, location.longitude, currentTime.toString())
            userRef.child("locations").push().setValue(locationData)
                .addOnSuccessListener {
                    println("Location data stored successfully for user: $userId")
                }
                .addOnFailureListener { e ->
                    println("Failed to store location data for user: $userId, error: ${e.message}")
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
    }
}
