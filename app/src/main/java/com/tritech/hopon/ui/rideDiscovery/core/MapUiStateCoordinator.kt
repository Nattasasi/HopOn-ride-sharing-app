package com.tritech.hopon.ui.rideDiscovery.core

import android.os.SystemClock
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MapUiStateCoordinator(
    private val hasSelectedRide: () -> Boolean,
    private val clearRideDetailSelection: () -> Unit,
    private val isRidePanelVisible: () -> Boolean,
    private val isRidePanelExpanded: () -> Boolean,
    private val collapseRidePanel: () -> Unit,
    private val isSearchBarAtTop: () -> Boolean,
    private val isPredictionsVisible: () -> Boolean,
    private val clearPendingSearch: () -> Unit,
    private val clearPredictionsState: () -> Unit,
    private val hidePredictionsCard: () -> Unit,
    private val clearSearchQuery: () -> Unit,
    private val clearSearchFocus: () -> Unit,
    private val hideRideResultsPanel: (Boolean) -> Unit,
    private val hideCreateRideButton: () -> Unit,
    private val moveSearchBarToBottom: () -> Unit
) {
    private var lastBackHandledAtMs = 0L

    fun setUpBackPressHandler(activity: AppCompatActivity, onFinish: () -> Unit) {
        activity.onBackPressedDispatcher.addCallback(activity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lastBackHandledAtMs = SystemClock.elapsedRealtime()
                if (dismissTransientMapUi()) {
                    return
                }
                onFinish()
            }
        })
    }

    fun handleMapTap(onBeforeDismiss: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBackHandledAtMs < 250L) {
            return
        }
        onBeforeDismiss()
        dismissTransientMapUi()
    }

    fun dismissTransientMapUi(): Boolean {
        if (isRidePanelVisible() && isRidePanelExpanded()) {
            collapseRidePanel()
            return true
        }

        if (hasSelectedRide()) {
            clearRideDetailSelection()
            return true
        }

        return restoreDefaultMapUiIfNeeded()
    }

    fun restoreDefaultMapUiIfNeeded(): Boolean {
        if (!isRidePanelVisible() &&
            !isSearchBarAtTop() &&
            !isPredictionsVisible()
        ) {
            return false
        }

        clearPendingSearch()
        clearPredictionsState()
        hidePredictionsCard()
        clearSearchQuery()
        clearSearchFocus()

        hideRideResultsPanel(true)
        hideCreateRideButton()
        moveSearchBarToBottom()
        return true
    }
}
