package com.tritech.hopon.ui.rideDiscovery.core

import android.view.View

class RidePanelCoordinator(
    private val ridesBottomSheetCard: View,
    private val mapTouchOverlay: View,
    private val clearMeetupLocationMarkers: () -> Unit,
    private val clearRideDetailSelection: () -> Unit,
    private val setRidePanelVisible: (Boolean) -> Unit,
    private val setRidePanelExpanded: (Boolean) -> Unit,
    private val setRidePanelItems: (List<RideListItem>) -> Unit,
    private val isRidePanelVisible: () -> Boolean
) {

    fun showRideResultsPanel() {
        ridesBottomSheetCard.visibility = View.VISIBLE
        mapTouchOverlay.visibility = View.GONE
        setRidePanelVisible(true)
    }

    fun hideRideResultsPanel(clearData: Boolean) {
        setRidePanelVisible(false)
        setRidePanelExpanded(false)
        ridesBottomSheetCard.visibility = View.GONE
        mapTouchOverlay.visibility = View.GONE
        clearMeetupLocationMarkers()
        clearRideDetailSelection()
        if (clearData) {
            setRidePanelItems(emptyList())
        }
    }

    fun expandRidePanel() {
        if (!isRidePanelVisible()) {
            showRideResultsPanel()
        }
        setRidePanelExpanded(true)
    }

    fun collapseRidePanel() {
        setRidePanelExpanded(false)
    }
}
