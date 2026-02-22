package com.tritech.hopon.ui.rideDiscovery.screen

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.tritech.hopon.BuildConfig
import com.tritech.hopon.R
import com.tritech.hopon.data.network.NetworkService
import com.tritech.hopon.databinding.ActivityMapsBinding
import com.tritech.hopon.ui.auth.LoginActivity
import com.tritech.hopon.ui.rideDiscovery.components.MapsBottomNavAction
import com.tritech.hopon.ui.rideDiscovery.components.MapsBottomNavItem
import com.tritech.hopon.ui.rideDiscovery.components.mainMapHost
import com.tritech.hopon.ui.rideDiscovery.components.mapBottomNavItemToAction
import com.tritech.hopon.ui.rideDiscovery.core.MapUiStateCoordinator
import com.tritech.hopon.ui.rideDiscovery.core.RidePanelCoordinator
import com.tritech.hopon.ui.rideDiscovery.core.SearchUiCoordinator
import com.tritech.hopon.ui.rideDiscovery.core.MeetupMarkerController
import com.tritech.hopon.ui.rideDiscovery.core.PlacesSearchDataSource
import com.tritech.hopon.ui.rideDiscovery.core.RoutesApiClient
import com.tritech.hopon.ui.rideDiscovery.core.CreateRideSubmission
import com.tritech.hopon.ui.rideDiscovery.core.MockData
import com.tritech.hopon.ui.rideDiscovery.core.RideDateTimeFormatter
import com.tritech.hopon.ui.rideDiscovery.core.RideListItem
import com.tritech.hopon.ui.rideDiscovery.core.RideLifecycleStatus
import com.tritech.hopon.ui.rideDiscovery.core.MapsPresenter
import com.tritech.hopon.ui.rideDiscovery.core.MapsView
import com.tritech.hopon.ui.rides.core.RideHistoryProvider
import com.tritech.hopon.utils.AnimationUtils
import com.tritech.hopon.utils.MapUtils
import com.tritech.hopon.utils.PermissionUtils
import com.tritech.hopon.utils.SessionManager
import com.tritech.hopon.utils.ViewUtils
import java.util.Locale

class RootHostActivity : AppCompatActivity(), MapsView, OnMapReadyCallback {

    private enum class CreateRideLocationField {
        MEETUP,
        DESTINATION
    }

    companion object {
        private const val TAG = "RootHostActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 999
        private const val PLACE_REQUEST_CODE = 2001
        private const val EXTRA_OPEN_RIDE_IN_PROCESS = "extra_open_ride_in_process"
        private const val RIDE_ONGOING_CHANNEL_ID = "ride_ongoing_channel"
        private const val RIDE_ONGOING_NOTIFICATION_ID = 9001
    }

    private lateinit var binding: ActivityMapsBinding
    private lateinit var searchUiCoordinator: SearchUiCoordinator
    private lateinit var mapUiStateCoordinator: MapUiStateCoordinator
    private lateinit var ridePanelCoordinator: RidePanelCoordinator
    private lateinit var googleMap: GoogleMap
    private var isMapReady = false
    private var presenter: MapsPresenter? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private var placesSearchDataSource: PlacesSearchDataSource? = null
    private val routesApiClient = RoutesApiClient()

    private var currentLatLng: LatLng? = null
    private var pickUpLatLng: LatLng? = null
    private var dropLatLng: LatLng? = null

    // Mock current location: Assumption University Thailand (13.7370, 100.6270)
    private val mockCurrentLocation = LatLng(13.7370, 100.6270)

    private var destinationMarker: Marker? = null
    private var originMarker: Marker? = null
    private var greyPolyLine: Polyline? = null
    private var blackPolyline: Polyline? = null
    private var previousLatLngFromServer: LatLng? = null
    private var currentLatLngFromServer: LatLng? = null
    private var movingCabMarker: Marker? = null
    private var meetupMarkerController: MeetupMarkerController? = null
    private val searchDebounceHandler = Handler(Looper.getMainLooper())
    private var pendingSearchRunnable: Runnable? = null
    private var latestRequestToken = 0L
    private var latestPredictions by mutableStateOf<List<AutocompletePrediction>>(emptyList())
    private var showEmptyPredictions by mutableStateOf(false)
    private var searchQuery by mutableStateOf("")
    private var shouldRequestSearchFocus by mutableStateOf(false)
    private var clearSearchFocusSignal by mutableStateOf(0)
    private var isRidePanelVisible by mutableStateOf(false)
    private var ridePanelItems by mutableStateOf<List<RideListItem>>(emptyList())
    private var allRidePanelItems by mutableStateOf<List<RideListItem>>(emptyList())
    private var isRidePanelExpanded by mutableStateOf(false)
    private var selectedRide by mutableStateOf<RideListItem?>(null)
    private var selectedBottomNavItem by mutableStateOf(MapsBottomNavItem.HOME)
    private var isHistoryVisible by mutableStateOf(false)
    private var isRideDetailVisible by mutableStateOf(false)
    private var isProfileVisible by mutableStateOf(false)
    private var isRideInProcessVisible by mutableStateOf(false)
    private var isCreateRideVisible by mutableStateOf(false)
    private var createRideDestination by mutableStateOf("")
    private var createRideDestinationLatLng: LatLng? = null
    private var createRideInitialMeetupLocation by mutableStateOf("")
    private var createRideInitialMeetupLatLng: LatLng? = null
    private var createRideInitialDestination by mutableStateOf("")
    private var createRideInitialDestinationLatLng: LatLng? = null
    private var createRideMeetupLocation by mutableStateOf("")
    private var createRideMeetupLatLng: LatLng? = null
    private var createRideDestinationInput by mutableStateOf("")
    private var createRideDestinationInputLatLng: LatLng? = null
    private var createRidePredictions by mutableStateOf<List<AutocompletePrediction>>(emptyList())
    private var showCreateRideEmptyPredictions by mutableStateOf(false)
    private var activeCreateRideField: CreateRideLocationField? = null
    private var pendingCreateRideSearchRunnable: Runnable? = null
    private var createRideRequestToken = 0L
    private var historyRideItems by mutableStateOf<List<RideListItem>>(emptyList())
    private var selectedHistoryRide by mutableStateOf<RideListItem?>(null)
    private var rideRoutePolyline: Polyline? = null
    private var pickupRoutePolyline: Polyline? = null
    private var rideRoutePoints by mutableStateOf<List<LatLng>>(emptyList())
    private var pickupRoutePoints by mutableStateOf<List<LatLng>>(emptyList())
    private val meetupPinSizePx = 94
    private val meetupPinSelectedSizePx = 112

    private var placeholderPlaceIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Require authenticated session before entering map flow.
        if (!SessionManager.isLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        searchUiCoordinator = SearchUiCoordinator(this, binding)
        ridePanelCoordinator = RidePanelCoordinator(
            ridesBottomSheetCard = binding.ridesBottomSheetCard,
            mapTouchOverlay = binding.mapTouchOverlay,
            clearMeetupLocationMarkers = ::clearMeetupLocationMarkers,
            clearRideDetailSelection = { clearRideDetailSelection() },
            setRidePanelVisible = { isRidePanelVisible = it },
            setRidePanelExpanded = { isRidePanelExpanded = it },
            setRidePanelItems = { ridePanelItems = it },
            isRidePanelVisible = { isRidePanelVisible }
        )
        mapUiStateCoordinator = MapUiStateCoordinator(
            hasSelectedRide = { selectedRide != null },
            clearRideDetailSelection = {
                clearRideDetailSelection(restoreCreateRideButton = true)
            },
            isRidePanelVisible = { isRidePanelVisible },
            isRidePanelExpanded = { isRidePanelExpanded },
            collapseRidePanel = ::collapseRidePanel,
            isSearchBarAtTop = { searchUiCoordinator.isSearchBarAtTop() },
            isPredictionsVisible = { binding.predictionsCard.visibility == View.VISIBLE },
            clearPendingSearch = {
                pendingSearchRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
                pendingSearchRunnable = null
            },
            clearPredictionsState = {
                latestPredictions = emptyList()
                showEmptyPredictions = false
            },
            hidePredictionsCard = { binding.predictionsCard.visibility = View.GONE },
            clearSearchQuery = { searchQuery = "" },
            clearSearchFocus = ::clearSearchFocus,
            hideRideResultsPanel = ::hideRideResultsPanel,
            hideCreateRideButton = { binding.createRideButton.visibility = View.GONE },
            moveSearchBarToBottom = ::moveSearchBarToBottom
        )
        ViewUtils.enableTransparentStatusBar(window)
        setUpImeSpacing()
        setUpRideResultsPanelCompose()
        setUpHistoryCompose()
        setUpRideDetailCompose()
        setUpProfileCompose()
        setUpRideInProcessCompose()
        setUpCreateRideCompose()
        setUpPredictionsCompose()
        setUpSearchBarCompose()
        setUpBackPressHandler()
        ensureRideOngoingNotificationChannel()
        setUpMainMapCompose()

        // Live mode initializes Places API client + websocket presenter.
        if (!BuildConfig.USE_MOCK_DATA) {
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, getString(R.string.google_maps_key))
            }
            placesSearchDataSource = PlacesSearchDataSource(Places.createClient(this))
            presenter = MapsPresenter(NetworkService())
            presenter?.onAttach(this)
        }

        // Initialize to Assumption University as default for testing on emulator without GPS.
        // Real location provider will override this if a valid location is obtained.
        currentLatLng = mockCurrentLocation
        pickUpLatLng = mockCurrentLocation

        setUpClickListener()
        handleNavigationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun setUpBackPressHandler() {
        mapUiStateCoordinator.setUpBackPressHandler(this) {
            if (isRideDetailVisible) {
                showHistoryContent()
            } else if (
                isHistoryVisible ||
                isProfileVisible ||
                isRideInProcessVisible ||
                isCreateRideVisible
            ) {
                selectedBottomNavItem = MapsBottomNavItem.HOME
                showMapContent()
            } else {
                finish()
            }
        }
    }

    private fun setUpMainMapCompose() {
        binding.mapCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.mapCompose.setContent {
            mainMapHost(onMapReady = ::onMapReady)
        }
    }

    private fun dismissTransientMapUi(): Boolean {
        return mapUiStateCoordinator.dismissTransientMapUi()
    }

    private fun restoreDefaultMapUiIfNeeded(): Boolean {
        return mapUiStateCoordinator.restoreDefaultMapUiIfNeeded()
    }

    private fun setUpRideResultsPanelCompose() {
        binding.ridesBottomSheetCard.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.ridesBottomSheetCard.setContent {
            mapHomeRideResultsScreen(
                visible = isRidePanelVisible,
                expanded = isRidePanelExpanded,
                currentUserName = resolveCurrentUserDisplayName(),
                onExpandChange = { expanded ->
                    if (expanded) expandRidePanel() else collapseRidePanel()
                },
                rides = ridePanelItems,
                selectedRide = selectedRide,
                onRideClick = { ride ->
                    selectRideForDetail(ride)
                }
            )
        }
    }

    private fun setUpPredictionsCompose() {
        binding.predictionsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.predictionsCompose.setContent {
            mapHomePredictionsScreen(
                predictions = latestPredictions,
                showEmptyState = showEmptyPredictions,
                onPredictionClick = ::fetchPlaceDetailsAndApplySelection
            )
        }
    }

    private fun setUpHistoryCompose() {
        binding.historyContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.historyContent.setContent {
            historyRidesScreen(
                rides = historyRideItems,
                currentUserName = resolveCurrentUserDisplayName(),
                onRideClick = ::showRideDetailForHistoryRide
            )
        }
    }

    private fun setUpRideDetailCompose() {
        binding.rideDetailContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.rideDetailContent.setContent {
            val ride = selectedHistoryRide
            if (ride != null) {
                rideDetailScreen(
                    onBackClick = ::showHistoryContent,
                    meetup = ride.meetupLabel,
                    destination = ride.destinationLabel,
                    pickupDistanceKm = String.format(
                        Locale.US,
                        "%.2f",
                        ride.pickupDistanceMeters / 1000f
                    ),
                    meetupDateTime = ride.meetupDateTimeLabel,
                    waitTimeMinutes = ride.waitTimeMinutes,
                    hostName = ride.hostName,
                    currentUserName = resolveCurrentUserDisplayName(),
                    hostRating = ride.hostRating,
                    hostVehicleType = ride.hostVehicleType,
                    peopleCount = ride.peopleCount
                )
            }
        }
    }

    private fun setUpProfileCompose() {
        binding.profileContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.profileContent.setContent {
            profileScreen(
                userName = getString(R.string.me_label),
                onLogoutClick = ::logoutAndNavigateToLogin
            )
        }
    }

    private fun resolveCurrentUserDisplayName(): String {
        val currentUserId = SessionManager.getCurrentUserId(this)
        return MockData.userNameForId(currentUserId) ?: currentUserId.orEmpty()
    }

    private fun setUpRideInProcessCompose() {
        binding.rideInProcessContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.rideInProcessContent.setContent {
            val currentUserId = SessionManager.getCurrentUserId(this)
            val ongoingRide = MockData.ongoingRideForUser(currentUserId)
            rideInProcessScreen(
                destinationLabel = ongoingRide?.destinationLabel,
                meetupLabel = ongoingRide?.meetupLabel,
                meetupDateTimeLabel = ongoingRide?.meetupDateTimeLabel,
                waitTimeMinutes = ongoingRide?.waitTimeMinutes,
                peopleCount = ongoingRide?.peopleCount,
                maxPeopleCount = ongoingRide?.maxPeopleCount,
                hostName = ongoingRide?.host?.name,
                currentLocationLatLng = currentLatLng,
                meetupLatLng = ongoingRide?.meetupLatLng,
                destinationLatLng = ongoingRide?.destinationLatLng,
                pickupRoutePoints = pickupRoutePoints,
                rideRoutePoints = rideRoutePoints,
                onBackClick = ::showHistoryContent
            )
        }
    }

    private fun setUpCreateRideCompose() {
        binding.createRideContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.createRideContent.setContent {
            createRideScreen(
                initialMeetupLocation = createRideInitialMeetupLocation,
                initialMeetupLatLng = createRideInitialMeetupLatLng,
                initialDestination = createRideInitialDestination,
                initialDestinationLatLng = createRideInitialDestinationLatLng,
                meetupLocation = createRideMeetupLocation,
                meetupLatLng = createRideMeetupLatLng,
                destination = createRideDestinationInput,
                destinationLatLng = createRideDestinationInputLatLng,
                locationPredictions = createRidePredictions,
                showLocationPredictions = activeCreateRideField != null &&
                    (createRidePredictions.isNotEmpty() || showCreateRideEmptyPredictions),
                onMeetupLocationChange = { query -> handleCreateRideQueryChange(CreateRideLocationField.MEETUP, query) },
                onDestinationChange = { query -> handleCreateRideQueryChange(CreateRideLocationField.DESTINATION, query) },
                onMeetupFocusChanged = { hasFocus -> handleCreateRideFocusChanged(CreateRideLocationField.MEETUP, hasFocus) },
                onDestinationFocusChanged = { hasFocus -> handleCreateRideFocusChanged(CreateRideLocationField.DESTINATION, hasFocus) },
                onMeetupSearchAction = { handleCreateRideSearchAction(CreateRideLocationField.MEETUP) },
                onDestinationSearchAction = { handleCreateRideSearchAction(CreateRideLocationField.DESTINATION) },
                onMeetupClearClick = { clearCreateRideField(CreateRideLocationField.MEETUP) },
                onDestinationClearClick = { clearCreateRideField(CreateRideLocationField.DESTINATION) },
                onPredictionClick = ::fetchCreateRidePlaceDetailsAndApplySelection,
                onDismissLocationOverlay = ::dismissCreateRideEditingUi,
                onBackClick = ::showMapContent,
                onCreateRideClick = ::handleCreateRideSubmit
            )
        }
    }

    private fun setUpSearchBarCompose() {
        binding.searchBarContainer.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.searchBarContainer.setContent {
            mapHomeSearchBarScreen(
                query = searchQuery,
                requestFocus = shouldRequestSearchFocus,
                onFocusHandled = { shouldRequestSearchFocus = false },
                clearFocusSignal = clearSearchFocusSignal,
                onFocusChanged = { hasFocus ->
                    if (hasFocus && !BuildConfig.USE_MOCK_DATA) {
                        activateInlineSearchMode()
                    }
                },
                onQueryChange = { newValue ->
                    searchQuery = newValue
                    handleSearchQueryChange(newValue)
                },
                onSearchAction = {
                    handleSearchAction()
                },
                onClick = {
                    if (BuildConfig.USE_MOCK_DATA) {
                        applyPlaceholderPlaceSelection()
                    } else {
                        activateInlineSearchMode()
                    }
                }
            )
        }
    }

    private fun showRideDetailForHistoryRide(ride: RideListItem) {
        if (ride.lifecycleStatus == RideLifecycleStatus.ONGOING) {
            showRideInProcessContent()
            return
        }
        selectedHistoryRide = ride
        showRideDetailContent()
    }

    private fun selectRideForDetail(ride: RideListItem) {
        selectedRide = ride
        rideRoutePoints = emptyList()
        pickupRoutePoints = emptyList()
        collapseRidePanel()  // Reset to peek state when showing detail
        requestRideRoute(ride.meetupLatLng, ride.destinationLatLng)
        currentLatLng?.let { requestPickupRoute(it, ride.meetupLatLng) }
        
        // Find and highlight the corresponding marker
        clearSelectedMeetupMarker()
        val marker = meetupMarkerController?.findMarkerForRide(ride)
        marker?.let {
            meetupMarkerController?.setSelectedMarker(it)
        }
        
        // Show "Join Ride" button
        binding.createRideButton.visibility = View.VISIBLE
    }

    private fun clearRideDetailSelection(restoreCreateRideButton: Boolean = false) {
        selectedRide = null
        rideRoutePolyline?.remove()
        rideRoutePolyline = null
        pickupRoutePolyline?.remove()
        pickupRoutePolyline = null
        rideRoutePoints = emptyList()
        pickupRoutePoints = emptyList()
        
        // Clear marker selection
        clearSelectedMeetupMarker()

        binding.createRideButton.visibility = if (
            restoreCreateRideButton && shouldShowPrimaryActionButton()
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun shouldShowPrimaryActionButton(): Boolean {
        if (!isRidePanelVisible) {
            return false
        }
        if (selectedRide != null) {
            return true
        }
        return dropLatLng != null || searchQuery.isNotBlank() || allRidePanelItems.isNotEmpty()
    }

    private fun requestRideRoute(meetupLatLng: LatLng, destinationLatLng: LatLng) {
        val routesApiKey = getString(R.string.routes_api_key)
        if (routesApiKey.isBlank()) {
            drawRideRouteFallback(meetupLatLng, destinationLatLng)
            return
        }

        routesApiClient.computeRoute(
            origin = meetupLatLng,
            destination = destinationLatLng,
            routesApiKey = routesApiKey
        ) { latLngList ->
            runOnUiThread {
                if (latLngList != null && latLngList.size >= 2) {
                    drawRideRoutePath(latLngList)
                } else {
                    drawRideRouteFallback(meetupLatLng, destinationLatLng)
                }
            }
        }
    }

    private fun drawRideRoutePath(latLngList: List<LatLng>) {
        rideRoutePolyline?.remove()
        rideRoutePoints = latLngList

        val builder = LatLngBounds.Builder()
        for (latLng in latLngList) {
            builder.include(latLng)
        }
        val bounds = builder.build()
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

        val polylineOptions = PolylineOptions()
        polylineOptions.color(ContextCompat.getColor(this, R.color.colorAccent))
        polylineOptions.width(8f)
        polylineOptions.addAll(latLngList)
        rideRoutePolyline = googleMap.addPolyline(polylineOptions)
    }

    private fun drawRideRouteFallback(meetupLatLng: LatLng, destinationLatLng: LatLng) {
        rideRoutePolyline?.remove()
        rideRoutePoints = listOf(meetupLatLng, destinationLatLng)

        val polylineOptions = PolylineOptions()
        polylineOptions.color(ContextCompat.getColor(this, R.color.colorAccent))
        polylineOptions.width(8f)
        polylineOptions.add(meetupLatLng, destinationLatLng)
        rideRoutePolyline = googleMap.addPolyline(polylineOptions)
    }

    private fun requestPickupRoute(startLatLng: LatLng, meetupLatLng: LatLng) {
        val routesApiKey = getString(R.string.routes_api_key)
        if (routesApiKey.isBlank()) {
            drawPickupRouteFallback(startLatLng, meetupLatLng)
            return
        }

        routesApiClient.computeRoute(
            origin = startLatLng,
            destination = meetupLatLng,
            routesApiKey = routesApiKey
        ) { latLngList ->
            runOnUiThread {
                if (latLngList != null && latLngList.size >= 2) {
                    drawPickupRoutePath(latLngList)
                } else {
                    drawPickupRouteFallback(startLatLng, meetupLatLng)
                }
            }
        }
    }

    private fun drawPickupRoutePath(latLngList: List<LatLng>) {
        pickupRoutePolyline?.remove()
        pickupRoutePoints = latLngList

        val polylineOptions = PolylineOptions()
        polylineOptions.color(ContextCompat.getColor(this, R.color.colorPrimary))
        polylineOptions.width(8f)
        polylineOptions.addAll(latLngList)
        pickupRoutePolyline = googleMap.addPolyline(polylineOptions)
    }

    private fun drawPickupRouteFallback(startLatLng: LatLng, meetupLatLng: LatLng) {
        pickupRoutePolyline?.remove()
        pickupRoutePoints = listOf(startLatLng, meetupLatLng)

        val polylineOptions = PolylineOptions()
        polylineOptions.color(ContextCompat.getColor(this, R.color.colorPrimary))
        polylineOptions.width(8f)
        polylineOptions.add(startLatLng, meetupLatLng)
        pickupRoutePolyline = googleMap.addPolyline(polylineOptions)
    }

    private fun showRideResultsPanel() {
        ridePanelCoordinator.showRideResultsPanel()
    }

    private fun hideRideResultsPanel(clearData: Boolean) {
        ridePanelCoordinator.hideRideResultsPanel(clearData)
    }

    private fun expandRidePanel() {
        ridePanelCoordinator.expandRidePanel()
    }

    private fun collapseRidePanel() {
        ridePanelCoordinator.collapseRidePanel()
    }

    private fun addMeetupLocationMarkers(rides: List<RideListItem>) {
        meetupMarkerController?.addMeetupLocationMarkers(rides)
    }

    private fun clearMeetupLocationMarkers() {
        meetupMarkerController?.clearMeetupLocationMarkers()
    }

    private fun setUpImeSpacing() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val extraGapPx = 150
            val bottomMargin = imeInsets.bottom + extraGapPx

            val params = binding.predictionsCard.layoutParams as? ConstraintLayout.LayoutParams
            if (params != null && params.bottomMargin != bottomMargin) {
                params.bottomMargin = bottomMargin
                binding.predictionsCard.layoutParams = params
            }

            if (binding.bottomNav.paddingBottom != navigationInsets.bottom) {
                binding.bottomNav.setPadding(
                    binding.bottomNav.paddingLeft,
                    binding.bottomNav.paddingTop,
                    binding.bottomNav.paddingRight,
                    navigationInsets.bottom
                )
            }

            insets
        }
    }

    private fun handleSearchQueryChange(query: String) {
        if (BuildConfig.USE_MOCK_DATA) {
            return
        }

        clearSelectedMeetupMarker()

        if (!searchUiCoordinator.isSearchBarAtTop()) {
            moveSearchBarToTop()
        }

        // Hide ride results while typing a new query.
        hideRideResultsPanel(clearData = true)
        binding.createRideButton.visibility = View.GONE

        pendingSearchRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
        pendingSearchRunnable = Runnable { fetchAutocompletePredictions(query) }
        searchDebounceHandler.postDelayed(pendingSearchRunnable!!, 300)
    }

    private fun setUpClickListener() {
        // Map touch overlay - tapping empty map area restores default UI
        binding.mapTouchOverlay.setOnClickListener {
            restoreDefaultMapUiIfNeeded()
        }

        setUpBottomNavCompose()
        setUpCreateRideButtonCompose()
    }

    private fun setUpCreateRideButtonCompose() {
        binding.createRideButton.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.createRideButton.setContent {
            mapHomePrimaryActionButton(
                selectedRide = selectedRide,
                onCreateRideClick = ::handleCreateRideClick,
                onJoinRideClick = ::handleJoinRideClick
            )
        }
    }

    private fun setUpBottomNavCompose() {
        binding.bottomNav.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.bottomNav.setContent {
            mapHomeBottomNavigationScreen(
                selectedItem = selectedBottomNavItem,
                onItemSelected = ::handleBottomNavSelection
            )
        }
    }

    private fun handleBottomNavSelection(item: MapsBottomNavItem) {
        selectedBottomNavItem = item
        when (mapBottomNavItemToAction(item)) {
            MapsBottomNavAction.NoOp -> Unit
            MapsBottomNavAction.ShowRides -> {
                showMapContent()
            }
            MapsBottomNavAction.ShowHistory -> {
                showHistoryContent()
            }
            MapsBottomNavAction.ShowProfile -> {
                showProfileContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        selectedBottomNavItem = when {
            isHistoryVisible || isRideDetailVisible -> MapsBottomNavItem.RIDES
            isProfileVisible -> MapsBottomNavItem.PROFILE
            else -> MapsBottomNavItem.HOME
        }
    }

    private fun showHistoryContent() {
        animateMainContentTransition()
        isHistoryVisible = true
        isRideDetailVisible = false
        isProfileVisible = false
        isRideInProcessVisible = false
        isCreateRideVisible = false
        isRidePanelVisible = false
        isRidePanelExpanded = false
        historyRideItems = RideHistoryProvider.loadCurrentUserHistoryRides(
            context = this,
            pickupDistanceMetersForMeetup = ::calculatePickupDistanceMeters
        )
        binding.historyContent.visibility = View.VISIBLE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.GONE
        binding.createRideContent.visibility = View.GONE
        binding.searchBarContainer.visibility = View.GONE
        binding.predictionsCard.visibility = View.GONE
        binding.ridesBottomSheetCard.visibility = View.GONE
        binding.mapTouchOverlay.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        clearRideDetailSelection()
        clearMeetupLocationMarkers()
    }

    private fun showMapContent() {
        animateMainContentTransition()
        isHistoryVisible = false
        isRideDetailVisible = false
        isProfileVisible = false
        isRideInProcessVisible = false
        isCreateRideVisible = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.GONE
        binding.createRideContent.visibility = View.GONE
        binding.searchBarContainer.visibility = View.VISIBLE
        binding.predictionsCard.visibility = View.GONE
        if (isRidePanelVisible) {
            binding.ridesBottomSheetCard.visibility = View.VISIBLE
        }
        binding.createRideButton.visibility = if (shouldShowPrimaryActionButton()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showProfileContent() {
        animateMainContentTransition()
        isHistoryVisible = false
        isRideDetailVisible = false
        isProfileVisible = true
        isRideInProcessVisible = false
        isCreateRideVisible = false
        isRidePanelVisible = false
        isRidePanelExpanded = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.VISIBLE
        binding.rideInProcessContent.visibility = View.GONE
        binding.createRideContent.visibility = View.GONE
        binding.searchBarContainer.visibility = View.GONE
        binding.predictionsCard.visibility = View.GONE
        binding.ridesBottomSheetCard.visibility = View.GONE
        binding.mapTouchOverlay.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        clearRideDetailSelection()
        clearMeetupLocationMarkers()
        selectedHistoryRide = null
    }

    private fun showRideDetailContent() {
        animateMainContentTransition()
        isHistoryVisible = false
        isRideDetailVisible = true
        isProfileVisible = false
        isRideInProcessVisible = false
        isCreateRideVisible = false
        isRidePanelVisible = false
        isRidePanelExpanded = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.VISIBLE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.GONE
        binding.createRideContent.visibility = View.GONE
        binding.searchBarContainer.visibility = View.GONE
        binding.predictionsCard.visibility = View.GONE
        binding.ridesBottomSheetCard.visibility = View.GONE
        binding.mapTouchOverlay.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        clearRideDetailSelection()
        clearMeetupLocationMarkers()
    }

    private fun showRideInProcessContent() {
        animateMainContentTransition()
        isHistoryVisible = false
        isRideDetailVisible = false
        isProfileVisible = false
        isRideInProcessVisible = true
        isCreateRideVisible = false
        isRidePanelVisible = false
        isRidePanelExpanded = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.VISIBLE
        binding.createRideContent.visibility = View.GONE
        binding.searchBarContainer.visibility = View.GONE
        binding.predictionsCard.visibility = View.GONE
        binding.ridesBottomSheetCard.visibility = View.GONE
        binding.mapTouchOverlay.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        clearRideDetailSelection()
        clearMeetupLocationMarkers()
        selectedHistoryRide = null

        val currentUserId = SessionManager.getCurrentUserId(this)
        val ongoingRide = MockData.ongoingRideForUser(currentUserId)
        requestRideInProcessRoutes(
            current = currentLatLng,
            meetup = ongoingRide?.meetupLatLng,
            destination = ongoingRide?.destinationLatLng
        )
    }

    private fun requestRideInProcessRoutes(
        current: LatLng?,
        meetup: LatLng?,
        destination: LatLng?
    ) {
        pickupRoutePoints = emptyList()
        rideRoutePoints = emptyList()

        val routesApiKey = getString(R.string.routes_api_key)
        if (routesApiKey.isBlank()) {
            pickupRoutePoints = listOfNotNull(current, meetup)
            rideRoutePoints = listOfNotNull(meetup, destination)
            return
        }

        if (current != null && meetup != null) {
            routesApiClient.computeRoute(
                origin = current,
                destination = meetup,
                routesApiKey = routesApiKey
            ) { latLngList ->
                runOnUiThread {
                    pickupRoutePoints = if (latLngList != null && latLngList.size >= 2) {
                        latLngList
                    } else {
                        listOf(current, meetup)
                    }
                }
            }
        }

        if (meetup != null && destination != null) {
            routesApiClient.computeRoute(
                origin = meetup,
                destination = destination,
                routesApiKey = routesApiKey
            ) { latLngList ->
                runOnUiThread {
                    rideRoutePoints = if (latLngList != null && latLngList.size >= 2) {
                        latLngList
                    } else {
                        listOf(meetup, destination)
                    }
                }
            }
        }
    }

    private fun showCreateRideContent() {
        animateMainContentTransition()
        isHistoryVisible = false
        isRideDetailVisible = false
        isProfileVisible = false
        isRideInProcessVisible = false
        isCreateRideVisible = true
        isRidePanelVisible = false
        isRidePanelExpanded = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.GONE
        binding.createRideContent.visibility = View.VISIBLE
        binding.searchBarContainer.visibility = View.GONE
        binding.predictionsCard.visibility = View.GONE
        binding.ridesBottomSheetCard.visibility = View.GONE
        binding.mapTouchOverlay.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        clearRideDetailSelection()
        clearMeetupLocationMarkers()
        selectedHistoryRide = null
        clearCreateRidePredictions()
    }

    private fun animateMainContentTransition() {
        val transition = AutoTransition().apply {
            duration = 180
        }
        TransitionManager.beginDelayedTransition(binding.root as ConstraintLayout, transition)
    }

    private fun handleCreateRideClick() {
        createRideDestination = searchQuery.trim().ifEmpty { getString(R.string.selected_place) }
        createRideDestinationLatLng = dropLatLng
        createRideInitialMeetupLocation = getString(R.string.current_location)
        createRideInitialMeetupLatLng = currentLatLng ?: pickUpLatLng
        createRideInitialDestination = createRideDestination
        createRideInitialDestinationLatLng = createRideDestinationLatLng
        createRideMeetupLocation = createRideInitialMeetupLocation
        createRideMeetupLatLng = createRideInitialMeetupLatLng
        createRideDestinationInput = createRideInitialDestination
        createRideDestinationInputLatLng = createRideInitialDestinationLatLng
        activeCreateRideField = null
        clearCreateRidePredictions()
        showCreateRideContent()
    }

    private fun handleCreateRideSubmit(submission: CreateRideSubmission) {
        val currentUserId = SessionManager.getCurrentUserId(this)
        val createdRide = MockData.addCreatedRide(submission, currentUserId)
        var isStartedAsOngoing = false

        if (shouldAutoStartCreatedRide(createdRide.meetupDateLabel, createdRide.meetupTimeLabel)) {
            isStartedAsOngoing = MockData.startOngoingRide(
                userId = currentUserId,
                meetupLabel = createdRide.meetupLabel,
                destinationLabel = createdRide.destinationLabel,
                meetupDateTimeLabel = createdRide.meetupDateTimeLabel,
                hostName = createdRide.host.name
            )
            if (isStartedAsOngoing && !BuildConfig.USE_MOCK_DATA) {
                sendRideOngoingNotification()
            }
        }

        historyRideItems = RideHistoryProvider.loadCurrentUserHistoryRides(
            context = this,
            pickupDistanceMetersForMeetup = ::calculatePickupDistanceMeters
        )

        createRideDestination = createdRide.destinationLabel
        createRideDestinationLatLng = createdRide.destinationLatLng

        if (isStartedAsOngoing) {
            showRideInProcessContent()
        } else {
            showMapContent()
            applySelectedPlace(createdRide.destinationLabel, createdRide.destinationLatLng)
            Toast.makeText(this, getString(R.string.create_ride), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleNavigationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_RIDE_IN_PROCESS, false) == true) {
            selectedBottomNavItem = MapsBottomNavItem.RIDES
            showRideInProcessContent()
            intent.removeExtra(EXTRA_OPEN_RIDE_IN_PROCESS)
        }
    }

    private fun ensureRideOngoingNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            RIDE_ONGOING_CHANNEL_ID,
            getString(R.string.ride_ongoing_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.ride_ongoing_notification_channel_description)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun sendRideOngoingNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openRideInProcessIntent = Intent(this, RootHostActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_RIDE_IN_PROCESS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            RIDE_ONGOING_NOTIFICATION_ID,
            openRideInProcessIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, RIDE_ONGOING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.ride_ongoing_notification_title))
            .setContentText(getString(R.string.ride_ongoing_notification_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(RIDE_ONGOING_NOTIFICATION_ID, notification)
    }

    private fun shouldAutoStartCreatedRide(meetupDate: String, meetupTime: String): Boolean {
        val scheduledAtMillis = RideDateTimeFormatter.parseSubmissionMeetupToEpochMillis(
            meetupDate.trim(),
            meetupTime.trim()
        )
            ?: return false
        return scheduledAtMillis <= System.currentTimeMillis()
    }

    private fun handleCreateRideFocusChanged(field: CreateRideLocationField, hasFocus: Boolean) {
        if (!hasFocus) {
            restoreCreateRideFieldIfBlank(field)
            clearCreateRidePredictions()
            if (activeCreateRideField == field) {
                activeCreateRideField = null
            }
            return
        }

        activeCreateRideField = field
        if (BuildConfig.USE_MOCK_DATA) {
            clearCreateRidePredictions()
            return
        }

        val query = getCreateRideQuery(field).trim()
        if (query.length >= 2) {
            scheduleCreateRidePredictionFetch(field, query)
        } else {
            clearCreateRidePredictions()
        }
    }

    private fun restoreCreateRideFieldIfBlank(field: CreateRideLocationField) {
        if (getCreateRideQuery(field).trim().isNotEmpty()) {
            return
        }

        when (field) {
            CreateRideLocationField.MEETUP -> {
                createRideMeetupLocation = createRideInitialMeetupLocation
                createRideMeetupLatLng = createRideInitialMeetupLatLng
            }

            CreateRideLocationField.DESTINATION -> {
                createRideDestinationInput = createRideInitialDestination
                createRideDestinationInputLatLng = createRideInitialDestinationLatLng
            }
        }
    }

    private fun handleCreateRideQueryChange(field: CreateRideLocationField, query: String) {
        setCreateRideQuery(field, query)
        setCreateRideLatLng(field, null)
        activeCreateRideField = field

        if (BuildConfig.USE_MOCK_DATA) {
            clearCreateRidePredictions()
            return
        }

        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) {
            clearCreateRidePredictions()
            return
        }

        scheduleCreateRidePredictionFetch(field, trimmedQuery)
    }

    private fun handleCreateRideSearchAction(field: CreateRideLocationField) {
        activeCreateRideField = field
        if (BuildConfig.USE_MOCK_DATA) {
            return
        }

        val query = getCreateRideQuery(field).trim()
        if (query.isEmpty()) {
            return
        }

        pendingCreateRideSearchRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
        pendingCreateRideSearchRunnable = null

        val matchingPrediction = createRidePredictions.firstOrNull { prediction ->
            val fullText = prediction.getFullText(null).toString().trim()
            val primaryText = prediction.getPrimaryText(null).toString().trim()
            fullText.equals(query, ignoreCase = true) ||
                primaryText.equals(query, ignoreCase = true)
        } ?: createRidePredictions.firstOrNull()

        if (matchingPrediction != null) {
            fetchCreateRidePlaceDetailsAndApplySelection(matchingPrediction)
            return
        }

        val dataSource = placesSearchDataSource ?: return
        val requestToken = ++createRideRequestToken
        dataSource.findPredictions(
            query = query,
            onSuccess = { predictions ->
                if (requestToken != createRideRequestToken || activeCreateRideField != field) {
                    return@findPredictions
                }
                createRidePredictions = predictions
                if (predictions.isNotEmpty()) {
                    fetchCreateRidePlaceDetailsAndApplySelection(predictions[0])
                } else {
                    showCreateRideEmptyPredictions = true
                }
            },
            onFailure = {
                Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun scheduleCreateRidePredictionFetch(field: CreateRideLocationField, query: String) {
        pendingCreateRideSearchRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
        pendingCreateRideSearchRunnable = Runnable {
            fetchCreateRidePredictions(field, query)
        }
        searchDebounceHandler.postDelayed(pendingCreateRideSearchRunnable!!, 300)
    }

    private fun fetchCreateRidePredictions(field: CreateRideLocationField, query: String) {
        val dataSource = placesSearchDataSource ?: return
        val requestToken = ++createRideRequestToken

        dataSource.findPredictions(
            query = query,
            onSuccess = { predictions ->
                if (requestToken != createRideRequestToken) {
                    return@findPredictions
                }
                val currentQuery = getCreateRideQuery(field).trim()
                if (currentQuery != query || activeCreateRideField != field) {
                    return@findPredictions
                }
                createRidePredictions = predictions
                showCreateRideEmptyPredictions = currentQuery.length >= 2 && predictions.isEmpty()
            },
            onFailure = {
                createRidePredictions = emptyList()
                showCreateRideEmptyPredictions = false
            }
        )
    }

    private fun fetchCreateRidePlaceDetailsAndApplySelection(prediction: AutocompletePrediction) {
        val selectedField = activeCreateRideField ?: return
        val dataSource = placesSearchDataSource ?: return
        dataSource.fetchPlaceDetails(
            prediction = prediction,
            onSuccess = { name, latLng ->
                setCreateRideQuery(selectedField, name)
                setCreateRideLatLng(selectedField, latLng)
                clearCreateRidePredictions()
                activeCreateRideField = null
            },
            onFailure = {
                Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun clearCreateRideField(field: CreateRideLocationField) {
        setCreateRideQuery(field, "")
        setCreateRideLatLng(field, null)
        activeCreateRideField = field
        clearCreateRidePredictions()
    }

    private fun setCreateRideQuery(field: CreateRideLocationField, value: String) {
        when (field) {
            CreateRideLocationField.MEETUP -> createRideMeetupLocation = value
            CreateRideLocationField.DESTINATION -> createRideDestinationInput = value
        }
    }

    private fun getCreateRideQuery(field: CreateRideLocationField): String {
        return when (field) {
            CreateRideLocationField.MEETUP -> createRideMeetupLocation
            CreateRideLocationField.DESTINATION -> createRideDestinationInput
        }
    }

    private fun setCreateRideLatLng(field: CreateRideLocationField, value: LatLng?) {
        when (field) {
            CreateRideLocationField.MEETUP -> createRideMeetupLatLng = value
            CreateRideLocationField.DESTINATION -> createRideDestinationInputLatLng = value
        }
    }

    private fun clearCreateRidePredictions() {
        pendingCreateRideSearchRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
        pendingCreateRideSearchRunnable = null
        createRidePredictions = emptyList()
        showCreateRideEmptyPredictions = false
    }

    private fun dismissCreateRideEditingUi() {
        clearCreateRidePredictions()
        activeCreateRideField = null
    }

    private fun handleJoinRideClick() {
        selectedRide?.let {
            val currentUserId = SessionManager.getCurrentUserId(this)
            if (MockData.hasOngoingRide(currentUserId)) {
                Toast.makeText(this, getString(R.string.one_ongoing_ride_only), Toast.LENGTH_SHORT).show()
                return
            }

            val joinedRide = MockData.joinRide(
                userId = currentUserId,
                meetupLabel = it.meetupLabel,
                destinationLabel = it.destinationLabel,
                meetupDateTimeLabel = it.meetupDateTimeLabel,
                hostName = it.hostName
            )

            if (joinedRide == null) {
                Toast.makeText(this, getString(R.string.unable_to_join_ride), Toast.LENGTH_SHORT).show()
                return
            }

            val started = MockData.startOngoingRide(
                userId = currentUserId,
                meetupLabel = it.meetupLabel,
                destinationLabel = it.destinationLabel,
                meetupDateTimeLabel = it.meetupDateTimeLabel,
                hostName = it.hostName
            )
            if (!started) {
                Toast.makeText(this, getString(R.string.unable_to_start_ride), Toast.LENGTH_SHORT).show()
                return
            }

            showRideInProcessContent()
        }
    }

    private fun logoutAndNavigateToLogin() {
        // Clear login state and reset task stack to auth screen.
        SessionManager.setLoggedIn(this, false)
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun activateInlineSearchMode() {
        searchUiCoordinator.activateInlineSearchMode(
            clearSelectedMeetupMarker = ::clearSelectedMeetupMarker,
            hideRideResultsPanel = ::hideRideResultsPanel,
            hideCreateRideButton = { binding.createRideButton.visibility = View.GONE },
            requestSearchFocus = { shouldRequestSearchFocus = true },
            showPredictionsCard = { binding.predictionsCard.visibility = View.VISIBLE }
        )
    }

    private fun clearSearchFocus() {
        clearSearchFocusSignal = searchUiCoordinator.clearSearchFocus(
            clearSearchFocusRequest = { shouldRequestSearchFocus = false },
            clearFocusSignal = clearSearchFocusSignal
        )
    }

    private fun handleSearchAction() {
        if (BuildConfig.USE_MOCK_DATA) {
            applyPlaceholderPlaceSelection()
            return
        }

        val query = searchQuery.trim()
        if (query.isEmpty()) {
            return
        }

        pendingSearchRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
        pendingSearchRunnable = null

        val matchingPrediction = latestPredictions.firstOrNull { prediction ->
            val fullText = prediction.getFullText(null).toString().trim()
            val primaryText = prediction.getPrimaryText(null).toString().trim()
            fullText.equals(query, ignoreCase = true) ||
                primaryText.equals(query, ignoreCase = true)
        } ?: latestPredictions.firstOrNull()

        if (matchingPrediction != null) {
            fetchPlaceDetailsAndApplySelection(matchingPrediction)
            return
        }

        val dataSource = placesSearchDataSource ?: return
        val submittedQuery = query
        val requestToken = ++latestRequestToken
        dataSource.findPredictions(
            query = submittedQuery,
            onSuccess = { predictions ->
                val currentQuery = searchQuery.trim()
                if (requestToken != latestRequestToken ||
                    !currentQuery.equals(submittedQuery, ignoreCase = true)
                ) {
                    return@findPredictions
                }

                latestPredictions = predictions
                if (latestPredictions.isNotEmpty()) {
                    fetchPlaceDetailsAndApplySelection(latestPredictions[0])
                } else {
                    showEmptyPredictions = currentQuery.length >= 2
                    binding.predictionsCard.visibility = View.VISIBLE
                }
            },
            onFailure = {
                Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun fetchAutocompletePredictions(query: String) {
        val dataSource = placesSearchDataSource ?: return
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) {
            latestPredictions = emptyList()
            showEmptyPredictions = false
            return
        }

        val requestToken = ++latestRequestToken

        dataSource.findPredictions(
            query = trimmedQuery,
            onSuccess = { predictions ->
                val currentQuery = searchQuery.trim()
                if (requestToken != latestRequestToken || currentQuery != trimmedQuery) {
                    return@findPredictions
                }
                latestPredictions = predictions
                showEmptyPredictions = currentQuery.length >= 2 && latestPredictions.isEmpty()
                binding.predictionsCard.visibility = View.VISIBLE
            },
            onFailure = {
                latestPredictions = emptyList()
                showEmptyPredictions = false
            }
        )
    }

    private fun fetchPlaceDetailsAndApplySelection(prediction: AutocompletePrediction) {
        val dataSource = placesSearchDataSource ?: return
        dataSource.fetchPlaceDetails(
            prediction = prediction,
            onSuccess = { name, latLng ->
                applySelectedPlace(name, latLng)
                binding.predictionsCard.visibility = View.GONE
                clearSearchFocus()
            },
            onFailure = {
                Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun applyPlaceholderPlaceSelection() {
        val place = MockData.placeholderPlaces[placeholderPlaceIndex % MockData.placeholderPlaces.size]
        placeholderPlaceIndex++

        applySelectedPlace(place.name, place.latLng)
    }

    private fun applySelectedPlace(name: String, latLng: LatLng) {
        // Update selected destination in UI and map state.
        dropLatLng = latLng
        searchQuery = name
        shouldRequestSearchFocus = false
        moveSearchBarToTop()
        showRideResultsPanel()
        binding.createRideButton.visibility = View.VISIBLE
        val rideItems = showFilteredRides(name, latLng)

        if (!isMapReady) {
            Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
            return
        }

        destinationMarker?.remove()
        destinationMarker = addOriginDestinationMarkerAndGet(latLng)
        destinationMarker?.setAnchor(0.5f, 0.5f)
        val nearestMeetupLatLng = rideItems.firstOrNull()?.meetupLatLng ?: latLng
        animateCamera(nearestMeetupLatLng)
    }

    private fun showFilteredRides(destinationName: String, destinationLatLng: LatLng): List<RideListItem> {
        val normalizedDestination = destinationName.trim().lowercase(Locale.ROOT)
        val rideItems = MockData.mockRides
            .filter { ride ->
                val rideDestination = ride.destinationLabel.lowercase(Locale.ROOT)
                rideDestination == normalizedDestination ||
                    normalizedDestination.contains(rideDestination) ||
                    rideDestination.contains(normalizedDestination) ||
                    calculateDistanceMeters(ride.destinationLatLng, destinationLatLng) <= 400f
            }
            .map { ride ->
                RideListItem(
                    meetupLabel = ride.meetupLabel,
                    meetupLatLng = ride.meetupLatLng,
                    destinationLabel = ride.destinationLabel,
                    destinationLatLng = ride.destinationLatLng,
                    pickupDistanceMeters = calculatePickupDistanceMeters(ride.meetupLatLng),
                    meetupDateTimeLabel = ride.meetupDateTimeLabel,
                    waitTimeMinutes = ride.waitTimeMinutes,
                    hostName = ride.host.name,
                    hostRating = ride.host.rating,
                    hostVehicleType = ride.host.vehicleType,
                    peopleCount = ride.peopleCount,
                    maxPeopleCount = ride.maxPeopleCount
                )
            }
            .sortedBy { it.pickupDistanceMeters }

        ridePanelItems = rideItems
        allRidePanelItems = rideItems
        addMeetupLocationMarkers(rideItems)
        return rideItems
    }

    private fun calculatePickupDistanceMeters(meetupLatLng: LatLng): Float {
        val userLatLng = currentLatLng ?: pickUpLatLng ?: meetupLatLng
        return calculateDistanceMeters(userLatLng, meetupLatLng)
    }

    private fun calculateDistanceMeters(start: LatLng, end: LatLng): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            start.latitude,
            start.longitude,
            end.latitude,
            end.longitude,
            result
        )
        return result[0]
    }

    private fun moveCamera(latLng: LatLng) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun animateCamera(latLng: LatLng) {
        val cameraPosition = CameraPosition.Builder().target(latLng).zoom(15.5f).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun addCarMarkerAndGet(latLng: LatLng): Marker? {
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(MapUtils.getCarBitmap(this))
        return googleMap.addMarker(
            MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor)
        )
    }

    private fun addOriginDestinationMarkerAndGet(latLng: LatLng): Marker? {
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
            MapUtils.getLocationIconBitmap(
                this,
                R.drawable.ic_target,
                R.color.colorAccent,
                sizePx = 72
            )
        )
        return googleMap.addMarker(
            MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor)
        )
    }

    private fun setCurrentLocationAsPickUp() {
        // Pickup defaults to user's current location.
        pickUpLatLng = currentLatLng
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationOnMap() {
        googleMap.setPadding(0, ViewUtils.dpToPx(48f), 0, ViewUtils.dpToPx(124f))
        googleMap.isMyLocationEnabled = true
    }

    private fun setUpLocationListener() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).setMinUpdateIntervalMillis(2000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                // Skip location updates in mock mode (use pre-set mock location instead).
                if (BuildConfig.USE_MOCK_DATA) {
                    return
                }

                // In live mode, allow real location to override the default Assumption University.
                // Lock after first non-default location is obtained.
                if (currentLatLng != mockCurrentLocation && currentLatLng != null) {
                    return
                }

                for (location in locationResult.locations) {
                    if (currentLatLng == mockCurrentLocation || currentLatLng == null) {
                        currentLatLng = LatLng(location.latitude, location.longitude)
                        setCurrentLocationAsPickUp()
                        enableMyLocationOnMap()
                        moveCamera(currentLatLng!!)
                        animateCamera(currentLatLng!!)
                    }
                }
            }
        }

        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }


    private fun reset() {
        // Restore map and UI to pre-booking default state.
        isCreateRideVisible = false
        binding.createRideContent.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        hideRideResultsPanel(clearData = true)
        searchQuery = ""
        clearSearchFocus()
        moveSearchBarToBottom()

        previousLatLngFromServer = null
        currentLatLngFromServer = null

        if (currentLatLng != null) {
            moveCamera(currentLatLng!!)
            animateCamera(currentLatLng!!)
            setCurrentLocationAsPickUp()
        }

        movingCabMarker?.remove()
        greyPolyLine?.remove()
        blackPolyline?.remove()
        originMarker?.remove()
        destinationMarker?.remove()

        dropLatLng = null
        greyPolyLine = null
        blackPolyline = null
        originMarker = null
        destinationMarker = null
        movingCabMarker = null
        clearCreateRidePredictions()
        activeCreateRideField = null
    }

    override fun onStart() {
        super.onStart()

        // Request permission/GPS if needed, then start location updates.
        // In mock mode, location is already set; in live mode, initialize from provider.
        if (!BuildConfig.USE_MOCK_DATA) {
            when {
                PermissionUtils.isAccessFineLocationGranted(this) -> {
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            setUpLocationListener()
                        }
                        else -> {
                            PermissionUtils.showGPSNotEnabledDialog(this)
                        }
                    }
                }
                else -> {
                    PermissionUtils.requestAccessFineLocationPermission(
                        this,
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        presenter?.onDetach()
        if (::locationCallback.isInitialized) {
            fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            setUpLocationListener()
                        }
                        else -> {
                            PermissionUtils.showGPSNotEnabledDialog(this)
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.location_permission_not_granted),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun moveSearchBarToTop() {
        searchUiCoordinator.moveSearchBarToTop()
    }

    private fun moveSearchBarToBottom() {
        searchUiCoordinator.moveSearchBarToBottom()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        isMapReady = true
        meetupMarkerController = MeetupMarkerController(
            context = this,
            googleMap = this.googleMap,
            normalPinSizePx = meetupPinSizePx,
            selectedPinSizePx = meetupPinSelectedSizePx
        )

        val mapStyleApplied = this.googleMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_minimal)
        )
        if (!mapStyleApplied) {
            Log.e(TAG, "Failed to apply minimal map style")
        }

        // Tapping empty map area collapses ride result panel if visible, otherwise restores default UI.
        this.googleMap.setOnMapClickListener {
            mapUiStateCoordinator.handleMapTap(
                onBeforeDismiss = {
                    clearSelectedMeetupMarker()
                    clearSearchFocus()
                }
            )
        }
        this.googleMap.setOnMarkerClickListener { marker ->
            val ride = marker.tag as? RideListItem ?: return@setOnMarkerClickListener false
            selectMeetupMarker(marker, ride)
            true
        }
    }

    private fun selectMeetupMarker(marker: Marker, ride: RideListItem) {
        if (selectedRide != null) {
            selectRideForDetail(ride)
            return
        }
        meetupMarkerController?.setSelectedMarker(marker)
        ridePanelItems = listOf(ride)
        showRideResultsPanel()
    }

    private fun clearSelectedMeetupMarker() {
        meetupMarkerController?.clearSelectedMarker()
        if (allRidePanelItems.isNotEmpty()) {
            ridePanelItems = allRidePanelItems
        }
    }


    override fun informCabBooked() {
        Toast.makeText(this, getString(R.string.your_cab_is_booked), Toast.LENGTH_SHORT).show()
    }

    override fun showPath(latLngList: List<LatLng>) {
        // Fit camera and draw route polylines from server path points.
        val builder = LatLngBounds.Builder()
        for (latLng in latLngList) {
            builder.include(latLng)
        }
        val bounds = builder.build()
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 2))

        val polylineOptions = PolylineOptions()
        polylineOptions.color(Color.GRAY)
        polylineOptions.width(5f)
        polylineOptions.addAll(latLngList)
        greyPolyLine = googleMap.addPolyline(polylineOptions)

        val blackPolylineOptions = PolylineOptions()
        blackPolylineOptions.width(5f)
        blackPolylineOptions.color(Color.BLACK)
        blackPolyline = googleMap.addPolyline(blackPolylineOptions)

        originMarker = addOriginDestinationMarkerAndGet(latLngList[0])
        originMarker?.setAnchor(0.5f, 0.5f)
        destinationMarker = addOriginDestinationMarkerAndGet(latLngList[latLngList.size - 1])
        destinationMarker?.setAnchor(0.5f, 0.5f)

        // Animate black overlay on top of gray route for progress effect.
        val polylineAnimator = AnimationUtils.polyLineAnimator()
        polylineAnimator.addUpdateListener { valueAnimator ->
            val percentValue = (valueAnimator.animatedValue as Int)
            val index = (greyPolyLine?.points!!.size * (percentValue / 100.0f)).toInt()
            blackPolyline?.points = greyPolyLine?.points!!.subList(0, index)
        }
        polylineAnimator.start()
    }

    override fun updateCabLocation(latLng: LatLng) {
        // Animate vehicle marker from previous server location to latest location.
        if (movingCabMarker == null) {
            movingCabMarker = addCarMarkerAndGet(latLng)
        }
        if (previousLatLngFromServer == null) {
            currentLatLngFromServer = latLng
            previousLatLngFromServer = currentLatLngFromServer
            movingCabMarker?.position = currentLatLngFromServer!!
            movingCabMarker?.setAnchor(0.5f, 0.5f)
            animateCamera(currentLatLngFromServer!!)
        } else {
            previousLatLngFromServer = currentLatLngFromServer
            currentLatLngFromServer = latLng
            val valueAnimator = AnimationUtils.cabAnimator()
            valueAnimator.addUpdateListener { va ->
                if (currentLatLngFromServer != null && previousLatLngFromServer != null) {
                    val multiplier = va.animatedFraction
                    val nextLocation = LatLng(
                        multiplier * currentLatLngFromServer!!.latitude + (1 - multiplier) * previousLatLngFromServer!!.latitude,
                        multiplier * currentLatLngFromServer!!.longitude + (1 - multiplier) * previousLatLngFromServer!!.longitude
                    )
                    movingCabMarker?.position = nextLocation
                    movingCabMarker?.setAnchor(0.5f, 0.5f)
                    val rotation = MapUtils.getRotation(previousLatLngFromServer!!, nextLocation)
                    if (!rotation.isNaN()) {
                        movingCabMarker?.rotation = rotation
                    }
                    animateCamera(nextLocation)
                }
            }
            valueAnimator.start()
        }
    }

    override fun informCabIsArriving() {
        Toast.makeText(this, getString(R.string.your_cab_is_arriving), Toast.LENGTH_SHORT).show()
    }

    override fun informCabArrived() {
        Toast.makeText(this, getString(R.string.your_cab_has_arrived), Toast.LENGTH_SHORT).show()
    }

    override fun informTripStart() {
        Toast.makeText(this, getString(R.string.you_are_on_a_trip), Toast.LENGTH_SHORT).show()
    }

    override fun informTripEnd() {
        Toast.makeText(this, getString(R.string.trip_end), Toast.LENGTH_SHORT).show()
        reset()
    }

    override fun showRoutesNotAvailableError() {
        Toast.makeText(
            this,
            getString(R.string.route_not_available_choose_different_locations),
            Toast.LENGTH_LONG
        ).show()
        reset()
    }

    override fun showDirectionApiFailedError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        reset()
    }
}
