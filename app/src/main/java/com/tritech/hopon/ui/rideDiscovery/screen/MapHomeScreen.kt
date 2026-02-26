package com.tritech.hopon.ui.rideDiscovery.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.hopOnButton
import com.tritech.hopon.ui.rideDiscovery.components.MapsBottomNavItem
import com.tritech.hopon.ui.rideDiscovery.components.mapSearchBar
import com.tritech.hopon.ui.rideDiscovery.components.mapsBottomNavigation
import com.tritech.hopon.ui.rideDiscovery.components.placePredictionsPanel
import com.tritech.hopon.ui.rideDiscovery.components.rideResultsBottomSheetPanel
import com.tritech.hopon.ui.rideDiscovery.core.RideListItem

@Composable
fun mapHomeRideResultsScreen(
    visible: Boolean,
    expanded: Boolean,
    rides: List<RideListItem>,
    selectedRide: RideListItem?,
    currentUserId: String?,
    currentUserName: String,
    onExpandChange: (Boolean) -> Unit,
    onRideClick: (RideListItem) -> Unit
) {
    rideResultsBottomSheetPanel(
        visible = visible,
        expanded = expanded,
        onExpandChange = onExpandChange,
        rides = rides,
        selectedRide = selectedRide,
        currentUserId = currentUserId,
        currentUserName = currentUserName,
        onRideClick = onRideClick
    )
}

@Composable
fun mapHomePredictionsScreen(
    predictions: List<AutocompletePrediction>,
    showEmptyState: Boolean,
    onPredictionClick: (AutocompletePrediction) -> Unit
) {
    placePredictionsPanel(
        predictions = predictions,
        showEmptyState = showEmptyState,
        onPredictionClick = onPredictionClick
    )
}

@Composable
fun mapHomeSearchBarScreen(
    query: String,
    requestFocus: Boolean,
    clearFocusSignal: Int,
    onFocusHandled: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    onClick: () -> Unit
) {
    mapSearchBar(
        query = query,
        requestFocus = requestFocus,
        onFocusHandled = onFocusHandled,
        clearFocusSignal = clearFocusSignal,
        onFocusChanged = onFocusChanged,
        onQueryChange = onQueryChange,
        onSearchAction = onSearchAction,
        onClick = onClick
    )
}

@Composable
fun mapHomePrimaryActionButton(
    selectedRide: RideListItem?,
    onCreateRideClick: () -> Unit,
    onJoinRideClick: () -> Unit,
    /** Active booking status for the selected ride ("pending"|"confirmed"|"rejected"|"cancelled"|null). */
    bookingStatus: String? = null,
    /** True while a booking network call is in-flight. */
    isBookingLoading: Boolean = false,
    /** Called when the passenger taps "Cancel Request". */
    onCancelBookingClick: () -> Unit = {},
    /** Driver only – called when the driver taps "View Requests". */
    onViewRequestsClick: () -> Unit = {},
    /** Number of pending booking requests (for driver badge). */
    pendingRequestCount: Int = 0,
    /** True when the selected ride is the current user's own (driver view). */
    isOwnRide: Boolean = false
) {
    val isJoinRideMode = selectedRide != null

    // Determine label and action based on booking state
    val (label, action, enabled) = when {
        !isJoinRideMode -> Triple(stringResource(R.string.create_ride), onCreateRideClick, true)

        isBookingLoading -> Triple("…", {}, false)

        isOwnRide -> {
            val requestLabel = if (pendingRequestCount > 0) {
                stringResource(R.string.view_requests_label, pendingRequestCount)
            } else {
                stringResource(R.string.view_requests_label_none)
            }
            Triple(requestLabel, onViewRequestsClick, true)
        }

        bookingStatus == "pending" ->
            Triple(stringResource(R.string.booking_status_pending), onCancelBookingClick, true)

        bookingStatus == "confirmed" ->
            Triple(stringResource(R.string.booking_status_confirmed), onCancelBookingClick, true)

        else ->
            Triple(stringResource(R.string.join_ride_label), onJoinRideClick, true)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp),
        contentAlignment = if (isJoinRideMode) Alignment.BottomCenter else Alignment.BottomEnd
    ) {
        hopOnButton(
            text     = label,
            onClick  = action,
            enabled  = enabled,
            modifier = if (isJoinRideMode) {
                Modifier.fillMaxWidth(0.8f)
            } else {
                Modifier.padding(end = 15.dp)
            }
        )
    }
}

@Composable
fun mapHomeBottomNavigationScreen(
    selectedItem: MapsBottomNavItem,
    onItemSelected: (MapsBottomNavItem) -> Unit
) {
    mapsBottomNavigation(
        selectedItem = selectedItem,
        onItemSelected = onItemSelected
    )
}