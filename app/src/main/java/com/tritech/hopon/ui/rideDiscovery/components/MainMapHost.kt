package com.tritech.hopon.ui.rideDiscovery.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi

@Composable
@OptIn(MapsComposeExperimentalApi::class)
fun mainMapHost(
    onMapReady: (GoogleMap) -> Unit,
    modifier: Modifier = Modifier
) {
    var mapReadyDispatched by remember { mutableStateOf(false) }

    GoogleMap(modifier = modifier.fillMaxSize()) {
        MapEffect(Unit) { googleMap ->
            if (!mapReadyDispatched) {
                mapReadyDispatched = true
                onMapReady(googleMap)
            }
        }
    }
}
