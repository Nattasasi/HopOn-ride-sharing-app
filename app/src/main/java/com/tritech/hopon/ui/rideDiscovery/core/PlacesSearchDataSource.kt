package com.tritech.hopon.ui.rideDiscovery.core

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

class PlacesSearchDataSource(
    private val placesClient: PlacesClient
) {

    fun findPredictions(
        query: String,
        onSuccess: (List<AutocompletePrediction>) -> Unit,
        onFailure: () -> Unit
    ) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                onSuccess(response.autocompletePredictions)
            }
            .addOnFailureListener {
                onFailure()
            }
    }

    fun fetchPlaceDetails(
        prediction: AutocompletePrediction,
        onSuccess: (name: String, latLng: LatLng) -> Unit,
        onFailure: () -> Unit
    ) {
        val request = FetchPlaceRequest.builder(
            prediction.placeId,
            listOf(Place.Field.LAT_LNG, Place.Field.NAME)
        ).build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val latLng = response.place.latLng ?: run {
                    onFailure()
                    return@addOnSuccessListener
                }
                onSuccess(prediction.getFullText(null).toString(), latLng)
            }
            .addOnFailureListener {
                onFailure()
            }
    }
}
