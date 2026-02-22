package com.tritech.hopon.ui.rideDiscovery.core

import android.animation.ValueAnimator
import android.content.Context
import android.view.animation.DecelerateInterpolator
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.tritech.hopon.R
import com.tritech.hopon.utils.MapUtils

class MeetupMarkerController(
    private val context: Context,
    private val googleMap: GoogleMap,
    private val normalPinSizePx: Int,
    private val selectedPinSizePx: Int
) {
    private val meetupLocationMarkers: MutableList<Marker> = mutableListOf()
    private var meetupMarkerIcon: BitmapDescriptor? = null
    private var meetupMarkerIconSelected: BitmapDescriptor? = null
    private var selectedMeetupMarker: Marker? = null

    fun addMeetupLocationMarkers(rides: List<RideListItem>) {
        clearMeetupLocationMarkers()
        if (meetupMarkerIcon == null) {
            meetupMarkerIcon = BitmapDescriptorFactory.fromBitmap(
                MapUtils.getLocationIconBitmap(
                    context,
                    R.drawable.location_on_24,
                    R.color.colorPrimaryDark,
                    sizePx = normalPinSizePx
                )
            )
        }
        if (meetupMarkerIconSelected == null) {
            meetupMarkerIconSelected = BitmapDescriptorFactory.fromBitmap(
                MapUtils.getLocationIconBitmap(
                    context,
                    R.drawable.location_on_24,
                    R.color.colorPrimary,
                    sizePx = selectedPinSizePx
                )
            )
        }

        for (ride in rides) {
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(ride.meetupLatLng)
                    .icon(meetupMarkerIcon)
                    .flat(false)
                    .title(ride.meetupLabel)
            )
            if (marker != null) {
                marker.tag = ride
                meetupLocationMarkers.add(marker)
            }
        }
    }

    fun clearMeetupLocationMarkers() {
        for (marker in meetupLocationMarkers) {
            marker.remove()
        }
        meetupLocationMarkers.clear()
        selectedMeetupMarker = null
    }

    fun findMarkerForRide(ride: RideListItem): Marker? {
        return meetupLocationMarkers.find { it.tag == ride }
    }

    fun setSelectedMarker(marker: Marker) {
        if (selectedMeetupMarker == marker) {
            return
        }
        selectedMeetupMarker?.let { previous ->
            animateMeetupMarkerIcon(previous, selectedPinSizePx, normalPinSizePx, R.color.colorPrimaryDark)
        }
        selectedMeetupMarker = marker
        animateMeetupMarkerIcon(marker, normalPinSizePx, selectedPinSizePx, R.color.colorPrimary)
    }

    fun clearSelectedMarker() {
        selectedMeetupMarker?.let { marker ->
            animateMeetupMarkerIcon(marker, selectedPinSizePx, normalPinSizePx, R.color.colorPrimaryDark)
        }
        selectedMeetupMarker = null
    }

    private fun animateMeetupMarkerIcon(
        marker: Marker,
        fromSizePx: Int,
        toSizePx: Int,
        colorResId: Int
    ) {
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180L
            interpolator = DecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                val fraction = valueAnimator.animatedValue as Float
                val size = (fromSizePx + (toSizePx - fromSizePx) * fraction).toInt()
                val icon = BitmapDescriptorFactory.fromBitmap(
                    MapUtils.getLocationIconBitmap(
                        context,
                        R.drawable.location_on_24,
                        colorResId,
                        sizePx = size
                    )
                )
                marker.setIcon(icon)
            }
        }
        animator.start()
    }
}
