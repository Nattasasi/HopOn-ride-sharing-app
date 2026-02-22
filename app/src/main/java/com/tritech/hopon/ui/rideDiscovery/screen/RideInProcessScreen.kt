package com.tritech.hopon.ui.rideDiscovery.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.rideInProcessCard
import com.tritech.hopon.ui.components.hopOnButton
import com.tritech.hopon.utils.MapUtils
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.ui.platform.LocalContext

private val HeaderLeadingIconSpace = 36.dp
private val HeaderTitleStartGap = 6.dp
private const val RideMapMinHeightRatio = 0.4f
private val RideMapCornerRadius = 12.dp
private const val RideMapCameraPaddingPx = 120
private const val RideMapPolylineWidth = 8f

@Composable
fun rideInProcessScreen(
    destinationLabel: String?,
    meetupLabel: String?,
    meetupDateTimeLabel: String?,
    waitTimeMinutes: Int?,
    peopleCount: Int?,
    maxPeopleCount: Int?,
    hostName: String?,
    currentLocationLatLng: LatLng?,
    meetupLatLng: LatLng?,
    destinationLatLng: LatLng?,
    pickupRoutePoints: List<LatLng>,
    rideRoutePoints: List<LatLng>,
    onGroupChatClick: () -> Unit = {},
    onCancelRideClick: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val mapMinHeight = screenHeight * RideMapMinHeightRatio

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.statusBarsPadding()
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(HeaderLeadingIconSpace)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(id = R.string.ride_detail_back),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.size(HeaderTitleStartGap))

                val destinationTitle = destinationLabel?.trim().orEmpty().ifBlank {
                    stringResource(id = R.string.selected_place)
                }

                Text(
                    text = "To $destinationTitle",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            rideRoutePreviewMap(
                currentLocationLatLng = currentLocationLatLng,
                meetupLatLng = meetupLatLng,
                destinationLatLng = destinationLatLng,
                pickupRoutePoints = pickupRoutePoints,
                rideRoutePoints = rideRoutePoints,
                minHeight = mapMinHeight,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            )

            val hasRide = !meetupLabel.isNullOrBlank() &&
                !meetupDateTimeLabel.isNullOrBlank() &&
                waitTimeMinutes != null &&
                peopleCount != null &&
                maxPeopleCount != null &&
                !hostName.isNullOrBlank()

            if (hasRide) {
                rideInProcessCard(
                    meetupLabel = meetupLabel,
                    meetupDateTimeLabel = meetupDateTimeLabel,
                    waitTimeMinutes = waitTimeMinutes,
                    peopleCount = peopleCount,
                    maxPeopleCount = maxPeopleCount,
                    hostName = hostName,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                )
            } else {
                Text(
                    text = stringResource(id = R.string.ride_in_process_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            hopOnButton(
                text = stringResource(id = R.string.group_chat),
                onClick = onGroupChatClick,
                modifier = Modifier.fillMaxWidth()
            )
            hopOnButton(
                text = stringResource(id = R.string.cancel_ride),
                onClick = onCancelRideClick,
                modifier = Modifier.fillMaxWidth(),
                containerColor = colorResource(id = R.color.cancelRideRed)
            )
        }
    }
}

@Composable
private fun rideRoutePreviewMap(
    currentLocationLatLng: LatLng?,
    meetupLatLng: LatLng?,
    destinationLatLng: LatLng?,
    pickupRoutePoints: List<LatLng>,
    rideRoutePoints: List<LatLng>,
    minHeight: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapStyle = remember(context) {
        runCatching {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_minimal)
        }.getOrNull()
    }
    val cameraPositionState = rememberCameraPositionState()
    val pickupPathPoints = remember(currentLocationLatLng, meetupLatLng, pickupRoutePoints) {
        if (pickupRoutePoints.size >= 2) {
            pickupRoutePoints
        } else {
            listOfNotNull(currentLocationLatLng, meetupLatLng)
        }
    }
    val ridePathPoints = remember(meetupLatLng, destinationLatLng, rideRoutePoints) {
        if (rideRoutePoints.size >= 2) {
            rideRoutePoints
        } else {
            listOfNotNull(meetupLatLng, destinationLatLng)
        }
    }
    val currentLocationIcon = remember(context) {
        BitmapDescriptorFactory.fromBitmap(
            MapUtils.getLocationIconBitmap(
                context,
                R.drawable.ic_target,
                R.color.colorPrimaryDark,
                sizePx = 72
            )
        )
    }
    val meetupIcon = remember(context) {
        BitmapDescriptorFactory.fromBitmap(
            MapUtils.getLocationIconBitmap(
                context,
                R.drawable.location_on_24,
                R.color.colorPrimary,
                sizePx = 94
            )
        )
    }
    val destinationIcon = remember(context) {
        BitmapDescriptorFactory.fromBitmap(
            MapUtils.getLocationIconBitmap(
                context,
                R.drawable.ic_target,
                R.color.colorAccent,
                sizePx = 72
            )
        )
    }
    val allMapPoints = buildList {
        addAll(pickupPathPoints)
        addAll(ridePathPoints)
        currentLocationLatLng?.let(::add)
        meetupLatLng?.let(::add)
        destinationLatLng?.let(::add)
    }
    val hasMapContent = allMapPoints.isNotEmpty()
    var isMapLoaded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.height(minHeight),
        shape = RoundedCornerShape(RideMapCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (hasMapContent) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false,
                    mapStyleOptions = mapStyle
                ),
                onMapLoaded = { isMapLoaded = true }
            ) {
                currentLocationLatLng?.let {
                    mapMarker(
                        position = it,
                        title = stringResource(id = R.string.current_location),
                        icon = currentLocationIcon
                    )
                }
                meetupLatLng?.let {
                    mapMarker(
                        position = it,
                        title = stringResource(id = R.string.meetup_location_label),
                        icon = meetupIcon
                    )
                }
                destinationLatLng?.let {
                    mapMarker(
                        position = it,
                        title = stringResource(id = R.string.destination_label),
                        icon = destinationIcon
                    )
                }
                if (pickupPathPoints.size >= 2) {
                    Polyline(
                        points = pickupPathPoints,
                        width = RideMapPolylineWidth,
                        color = colorResource(id = R.color.colorPrimary)
                    )
                }
                if (ridePathPoints.size >= 2) {
                    Polyline(
                        points = ridePathPoints,
                        width = RideMapPolylineWidth,
                        color = colorResource(id = R.color.colorAccent)
                    )
                }
            }

            LaunchedEffect(isMapLoaded, allMapPoints) {
                if (!isMapLoaded || allMapPoints.isEmpty()) return@LaunchedEffect

                if (allMapPoints.size == 1) {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(allMapPoints.first(), 14f)
                    )
                    return@LaunchedEffect
                }

                val boundsBuilder = LatLngBounds.Builder()
                allMapPoints.forEach(boundsBuilder::include)

                runCatching {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngBounds(
                            boundsBuilder.build(),
                            RideMapCameraPaddingPx
                        )
                    )
                }.onFailure {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(allMapPoints.first(), 13f)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun mapMarker(
    position: LatLng,
    title: String,
    icon: BitmapDescriptor
) {
    Marker(
        state = MarkerState(position = position),
        title = title,
        icon = icon
    )
}