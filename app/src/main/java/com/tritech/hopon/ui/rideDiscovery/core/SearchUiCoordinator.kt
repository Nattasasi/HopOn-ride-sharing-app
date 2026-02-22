package com.tritech.hopon.ui.rideDiscovery.core

import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.tritech.hopon.R
import com.tritech.hopon.databinding.ActivityMapsBinding
import com.tritech.hopon.utils.ViewUtils
import kotlin.math.max

class SearchUiCoordinator(
    private val activity: AppCompatActivity,
    private val binding: ActivityMapsBinding
) {
    private var searchBarAtTop = false

    fun isSearchBarAtTop(): Boolean = searchBarAtTop

    fun moveSearchBarToTop() {
        if (searchBarAtTop) {
            return
        }

        val root = binding.root as ConstraintLayout
        val constraintSet = ConstraintSet()
        constraintSet.clone(root)
        constraintSet.clear(R.id.searchBarContainer, ConstraintSet.BOTTOM)
        constraintSet.connect(
            R.id.searchBarContainer,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            getSearchBarTopMargin()
        )
        TransitionManager.beginDelayedTransition(root, AutoTransition().setDuration(150))
        constraintSet.applyTo(root)
        searchBarAtTop = true
    }

    fun moveSearchBarToBottom() {
        if (!searchBarAtTop) {
            return
        }

        val root = binding.root as ConstraintLayout
        val constraintSet = ConstraintSet()
        constraintSet.clone(root)
        constraintSet.clear(R.id.searchBarContainer, ConstraintSet.TOP)
        constraintSet.connect(
            R.id.searchBarContainer,
            ConstraintSet.BOTTOM,
            R.id.bottomNav,
            ConstraintSet.TOP,
            ViewUtils.dpToPx(12f)
        )
        TransitionManager.beginDelayedTransition(root, AutoTransition().setDuration(150))
        constraintSet.applyTo(root)
        searchBarAtTop = false
    }

    fun activateInlineSearchMode(
        clearSelectedMeetupMarker: () -> Unit,
        hideRideResultsPanel: (Boolean) -> Unit,
        hideCreateRideButton: () -> Unit,
        requestSearchFocus: () -> Unit,
        showPredictionsCard: () -> Unit
    ) {
        clearSelectedMeetupMarker()
        hideRideResultsPanel(true)
        hideCreateRideButton()
        moveSearchBarToTop()
        requestSearchFocus()
        showPredictionsCard()
    }

    fun clearSearchFocus(
        clearSearchFocusRequest: () -> Unit,
        clearFocusSignal: Int
    ): Int {
        clearSearchFocusRequest()
        val imm = activity.getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.searchBarContainer.windowToken, 0)
        return clearFocusSignal + 1
    }

    private fun getSearchBarTopMargin(): Int {
        val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) {
            activity.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
        return max(ViewUtils.dpToPx(28f), statusBarHeight + ViewUtils.dpToPx(16f))
    }
}
