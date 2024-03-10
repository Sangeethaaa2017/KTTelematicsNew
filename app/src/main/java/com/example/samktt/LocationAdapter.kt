package com.example.samktt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.samktt.LocationData
import com.example.samktt.R

class LocationAdapter(private val locationList: List<LocationData>, private val onItemClick: (LocationData) -> Unit) :
    RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {

    inner class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val latitudeTextView: TextView = itemView.findViewById(R.id.latitudeTextView)
        private val longitudeTextView: TextView = itemView.findViewById(R.id.longitudeTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timeTextView)

        fun bind(locationData: LocationData) {
            latitudeTextView.text = "Latitude: ${locationData.latitude}"
            longitudeTextView.text = "Longitude: ${locationData.longitude}"
            timestampTextView.text = "Timestamp: ${locationData.timestamp}"

            // Set click listener
            itemView.setOnClickListener {
                onItemClick(locationData)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.location_item, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(locationList[position])
    }

    override fun getItemCount() = locationList.size
}
