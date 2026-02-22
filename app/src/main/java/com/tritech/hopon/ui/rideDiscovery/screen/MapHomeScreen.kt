package com.tritech.hopon.ui.rideDiscovery.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.libraries.places.api.model.AutocompletePrediction
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
    onJoinRideClick: () -> Unit
) {
    val isJoinRideMode = selectedRide != null
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp),
        contentAlignment = if (isJoinRideMode) Alignment.BottomCenter else Alignment.BottomEnd
    ) {
        hopOnButton(
            text = if (isJoinRideMode) "Join Ride" else "Create ride",
            onClick = if (isJoinRideMode) onJoinRideClick else onCreateRideClick,
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