package com.tritech.hopon.ui.maps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tritech.hopon.R
import java.util.Locale

class RideListAdapter(
    private val onRideClicked: (RideListItem) -> Unit
) : RecyclerView.Adapter<RideListAdapter.RideViewHolder>() {

    private val items = mutableListOf<RideListItem>()

    fun submitList(newItems: List<RideListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ride, parent, false)
        return RideViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        holder.bind(items[position], onRideClicked)
    }

    override fun getItemCount(): Int = items.size

    class RideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val meetupTextView: TextView = itemView.findViewById(R.id.meetupTextView)
        private val destinationTextView: TextView = itemView.findViewById(R.id.destinationTextView)
        private val distanceTextView: TextView = itemView.findViewById(R.id.distanceTextView)

        fun bind(item: RideListItem, onRideClicked: (RideListItem) -> Unit) {
            meetupTextView.text = itemView.context.getString(R.string.meetup_format, item.meetupLabel)
            destinationTextView.text = itemView.context.getString(R.string.destination_format, item.destinationLabel)
            distanceTextView.text = itemView.context.getString(
                R.string.pickup_distance_format,
                String.format(Locale.US, "%.2f", item.pickupDistanceMeters / 1000f)
            )
            itemView.setOnClickListener { onRideClicked(item) }
        }
    }
}
