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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.rideInProcessCard
import com.tritech.hopon.ui.components.hopOnButton
import com.tritech.hopon.ui.rideDiscovery.core.ApiBooking
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
    vehiclePlate: String? = null,
    currentLocationLatLng: LatLng?,
    meetupLatLng: LatLng?,
    destinationLatLng: LatLng?,
    pickupRoutePoints: List<LatLng>,
    rideRoutePoints: List<LatLng>,
    showReportDriverAction: Boolean = false,
    onReportDriverClick: () -> Unit = {},
    onGroupChatClick: () -> Unit = {},
    isDriverView: Boolean = false,
    driverPassengerBookings: List<ApiBooking> = emptyList(),
    currentUserBooking: ApiBooking? = null,
    waitTimerLabel: String? = null,
    showStartRideAction: Boolean = false,
    isStartRideEnabled: Boolean = false,
    startRideChecklistSummary: String? = null,
    startRideChecklistBlockers: List<String> = emptyList(),
    isRideStartChecklistReady: Boolean = false,
    showCompleteRideAction: Boolean = false,
    isActionLoading: Boolean = false,
    onPassengerArriveClick: () -> Unit = {},
    onDriverConfirmBoardedClick: (bookingId: String) -> Unit = {},
    onDriverStartRideClick: () -> Unit = {},
    onDriverCompleteRideClick: () -> Unit = {},
    showEmergencyAction: Boolean = false,
    onEmergencyClick: () -> Unit = {},
    cancelWindowInfo: String? = null,
    isCancelRideEnabled: Boolean = true,
    onCancelRideClick: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val mapMinHeight = screenHeight * RideMapMinHeightRatio

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
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
                if (!vehiclePlate.isNullOrBlank()) {
                    Text(
                        text = stringResource(id = R.string.vehicle_plate_format, vehiclePlate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                    )
                }
            } else {
                Text(
                    text = stringResource(id = R.string.ride_in_process_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (isDriverView) {
                waitTimerLabel?.takeIf { it.isNotBlank() }?.let { info ->
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                    )
                }
            }

            if (isDriverView && driverPassengerBookings.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.ride_in_process_passenger_states_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                )
                driverPassengerBookings.forEach { booking ->
                    val passengerName = booking.passenger_id?.fullName
                        ?.takeIf { it.isNotBlank() }
                        ?: stringResource(id = R.string.booking_unknown_passenger)
                    val pickupLabel = when (booking.pickup_status) {
                        "arrived" -> stringResource(id = R.string.pickup_status_arrived)
                        "boarded" -> stringResource(id = R.string.pickup_status_boarded)
                        "left_behind" -> stringResource(id = R.string.pickup_status_left_behind)
                        else -> stringResource(id = R.string.pickup_status_not_arrived)
                    }

                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = passengerName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Black
                            )
                            Text(
                                text = pickupLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                        }
                        if (booking.pickup_status == "arrived") {
                            hopOnButton(
                                text = stringResource(id = R.string.pickup_confirm_boarded_action),
                                onClick = { onDriverConfirmBoardedClick(booking.id) },
                                enabled = !isActionLoading,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            if (isDriverView && showStartRideAction) {
                rideStartChecklistCard(
                    summary = startRideChecklistSummary,
                    blockers = startRideChecklistBlockers,
                    isReady = isRideStartChecklistReady,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(200.dp))
        }

        // Action buttons overlay at bottom, transparent background
        Column(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onGroupChatClick,
                    modifier = Modifier
                        .size(58.dp)
                        .background(
                            color = colorResource(id = R.color.colorPrimary),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = stringResource(id = R.string.group_chat),
                        tint = Color.White
                    )
                }

                if (showEmergencyAction) {
                    IconButton(
                        onClick = onEmergencyClick,
                        enabled = !isActionLoading,
                        modifier = Modifier
                            .size(58.dp)
                            .background(
                                color = colorResource(id = R.color.cancelRideRed),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(id = R.string.emergency_action),
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(58.dp))
                }
            }

            if (showReportDriverAction) {
                hopOnButton(
                    text = stringResource(id = R.string.report_driver),
                    onClick = onReportDriverClick,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = colorResource(id = R.color.colorAccent)
                )
            }
            if (!isDriverView) {
                val pickupStatus = currentUserBooking?.pickup_status
                val arriveLabel = when (pickupStatus) {
                    "arrived" -> stringResource(id = R.string.pickup_status_waiting_driver)
                    "boarded" -> stringResource(id = R.string.pickup_status_boarded)
                    "left_behind" -> stringResource(id = R.string.pickup_status_left_behind)
                    else -> stringResource(id = R.string.pickup_arrive_action)
                }
                val canArrive = pickupStatus == null || pickupStatus == "not_arrived"
                hopOnButton(
                    text = arriveLabel,
                    onClick = onPassengerArriveClick,
                    enabled = canArrive && !isActionLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                if (pickupStatus == "arrived") {
                    currentUserBooking?.pickup_code?.takeIf { it.isNotBlank() }?.let { code ->
                        Text(
                            text = stringResource(id = R.string.pickup_code_share_label, code),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
            if (isDriverView && showStartRideAction) {
                hopOnButton(
                    text = stringResource(id = R.string.ride_start_action),
                    onClick = onDriverStartRideClick,
                    enabled = isStartRideEnabled && !isActionLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isDriverView && showCompleteRideAction) {
                hopOnButton(
                    text = stringResource(id = R.string.ride_complete_action),
                    onClick = onDriverCompleteRideClick,
                    enabled = !isActionLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            hopOnButton(
                text = stringResource(id = R.string.cancel_ride),
                onClick = onCancelRideClick,
                modifier = Modifier.fillMaxWidth(),
                containerColor = colorResource(id = R.color.cancelRideRed),
                enabled = isCancelRideEnabled
            )
        }
    }
}

@Composable
private fun rideStartChecklistCard(
    summary: String?,
    blockers: List<String>,
    isReady: Boolean,
    modifier: Modifier = Modifier
) {
    if (summary.isNullOrBlank() && blockers.isEmpty()) return

    val accentColor = if (isReady) {
        colorResource(id = R.color.badgeGreenText)
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.ride_start_checklist_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            blockers.firstOrNull()?.let { blocker ->
                Text(
                    text = blocker,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
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
