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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
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
import com.tritech.hopon.BuildConfig
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.tritech.hopon.R
import com.tritech.hopon.data.network.NetworkService
import com.tritech.hopon.databinding.ActivityMapsBinding
import com.tritech.hopon.ui.auth.LoginActivity
import com.tritech.hopon.ui.components.hopOnComposeTheme
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
import com.tritech.hopon.ui.rideDiscovery.core.MockChatMessage
import com.tritech.hopon.ui.rideDiscovery.core.RideDateTimeFormatter
import com.tritech.hopon.ui.rideDiscovery.core.RideListItem
import com.tritech.hopon.ui.rideDiscovery.core.RideLifecycleStatus
import com.tritech.hopon.ui.rideDiscovery.core.MapsPresenter
import com.tritech.hopon.ui.rideDiscovery.core.MapsView
import com.tritech.hopon.utils.AnimationUtils
import com.tritech.hopon.utils.MapUtils
import com.tritech.hopon.utils.PermissionUtils
import com.tritech.hopon.utils.SessionManager
import com.tritech.hopon.utils.ViewUtils
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import com.tritech.hopon.ui.rideDiscovery.core.ApiBooking
import com.tritech.hopon.ui.rideDiscovery.core.ApiBookingRepository
import com.tritech.hopon.ui.rideDiscovery.core.ApiClient
import com.tritech.hopon.ui.rideDiscovery.core.ApiCreateFeedbackRequest
import com.tritech.hopon.ui.rideDiscovery.core.ApiCreatePaymentRequest
import com.tritech.hopon.ui.rideDiscovery.core.ApiRideRepository
import com.tritech.hopon.ui.rideDiscovery.core.ApiUpdateStatusRequest
import com.tritech.hopon.ui.rideDiscovery.core.BookingRepository
import com.tritech.hopon.ui.rideDiscovery.core.ChatSocketManager
import com.tritech.hopon.ui.rideDiscovery.core.FeedbackService
import com.tritech.hopon.ui.rideDiscovery.core.MessagesService
import com.tritech.hopon.ui.rideDiscovery.core.PaymentsService
import com.tritech.hopon.ui.rideDiscovery.core.PostsService
import com.tritech.hopon.ui.rideDiscovery.core.RideParticipationRole
import com.tritech.hopon.ui.rideDiscovery.core.RideRepository
import com.tritech.hopon.ui.rideDiscovery.core.TrackingService
import com.tritech.hopon.ui.rideDiscovery.core.ApiUpdateTrackingRequest
import com.tritech.hopon.ui.rideDiscovery.core.isActiveBooking
import com.tritech.hopon.ui.rideDiscovery.core.normaliseBookingStatus
import com.tritech.hopon.ui.rideDiscovery.core.toMockChatMessages
import com.tritech.hopon.ui.rideDiscovery.core.toRideListItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class RootHostActivity : AppCompatActivity(), MapsView, OnMapReadyCallback {

    private fun ComposeView.setHopOnContent(content: @Composable () -> Unit) {
        setContent {
            hopOnComposeTheme {
                content()
            }
        }
    }

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
        private const val ARRIVAL_RADIUS_METERS = 100f
    }

    private lateinit var binding: ActivityMapsBinding
    private lateinit var searchUiCoordinator: SearchUiCoordinator
    private lateinit var mapUiStateCoordinator: MapUiStateCoordinator
    private lateinit var ridePanelCoordinator: RidePanelCoordinator
    private lateinit var googleMap: GoogleMap
    private lateinit var rideRepository: RideRepository
    private lateinit var trackingService: TrackingService
    private var isMapReady = false
    private var presenter: MapsPresenter? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private var placesSearchDataSource: PlacesSearchDataSource? = null
    private val routesApiClient = RoutesApiClient()

    private var currentLatLng: LatLng? = null
    private var pickUpLatLng: LatLng? = null
    private var dropLatLng: LatLng? = null

    // Default current location: Assumption University Thailand (13.7370, 100.6270)
    private val defaultCurrentLocation = LatLng(13.7370, 100.6270)

    private var destinationMarker: Marker? = null
    private var originMarker: Marker? = null
    private var greyPolyLine: Polyline? = null
    private var blackPolyline: Polyline? = null
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
    private var isRidePaymentVisible by mutableStateOf(false)
    private var isCreateRideVisible by mutableStateOf(false)
    private var isGroupChatVisible by mutableStateOf(false)
    private var groupChatMessages by mutableStateOf<List<MockChatMessage>>(emptyList())
    private var groupChatParticipants by mutableStateOf<List<String>>(emptyList())
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
    private var isRideDetailOpenedFromMap by mutableStateOf(false)
    private var historyPendingBookingRequestCount by mutableStateOf(0)
    private var rideRoutePolyline: Polyline? = null
    private var pickupRoutePolyline: Polyline? = null
    private var rideRoutePoints by mutableStateOf<List<LatLng>>(emptyList())
    private var pickupRoutePoints by mutableStateOf<List<LatLng>>(emptyList())
    /** The ride currently being viewed in ride-in-process or payment screens. */
    private var activeInProcessRide: RideListItem? = null
    private var isPendingPaymentHost by mutableStateOf(false)
    /** Socket.IO manager for real-time group chat. */
    private var chatSocketManager: ChatSocketManager? = null
    /** MongoDB _id of the post whose chat room is currently open. */
    private var currentChatPostId: String? = null
    /** Passenger names for the active in-process ride (fetched from bookings). */
    private var activeRidePassengerNames by mutableStateOf<List<String>>(emptyList())
    private var hasLocallyDetectedTripEnd = false
    private val meetupPinSizePx = 94
    private val meetupPinSelectedSizePx = 112

    // ── Phase 6: Booking state ────────────────────────────────────────────────
    private lateinit var bookingRepository: BookingRepository
    /** MongoDB _id of the user's active booking for the currently selected ride. */
    private var activeBookingId: String? = null
    /** Status of the user's active booking ("pending"|"confirmed"|"rejected"|null). */
    private var activeBookingStatus by mutableStateOf<String?>(null)
    /** True while a booking network call is in-flight. */
    private var isBookingLoading by mutableStateOf(false)
    /** Booking requests fetched for the driver's selected ride. */
    private var bookingRequestItems by mutableStateOf<List<ApiBooking>>(emptyList())
    /** Post UUID currently loaded for host booking-request actions. */
    private var bookingRequestsPostUuid: String? = null
    /** Number of pending requests for the driver badge. */
    private var pendingBookingRequestCount by mutableStateOf(0)
    /** True when the selected ride belongs to the current user (driver view). */
    private var isSelectedRideDriver by mutableStateOf(false)
    /** Non-simulator: latest tracked driver location shown in in-process map. */
    private var trackedInProcessLatLng by mutableStateOf<LatLng?>(null)
    /** Non-simulator: polling job for passenger tracking updates. */
    private var trackingPollJob: Job? = null
    /** Non-simulator: throttle timestamp for host tracking updates. */
    private var lastTrackingPushAtMillis: Long = 0L

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
        rideRepository = ApiRideRepository(applicationContext) { currentLatLng ?: pickUpLatLng }
        bookingRepository = ApiBookingRepository(applicationContext)
        trackingService = ApiClient.create(applicationContext)
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
        setUpRidePaymentCompose()
        setUpGroupChatCompose()
        setUpCreateRideCompose()
        setUpPredictionsCompose()
        setUpSearchBarCompose()
        setUpBackPressHandler()
        ensureRideOngoingNotificationChannel()
        setUpMainMapCompose()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesSearchDataSource = PlacesSearchDataSource(Places.createClient(this))
        if (BuildConfig.USE_SIMULATOR) {
            presenter = MapsPresenter(NetworkService())
            presenter?.onAttach(this)
        }

        // Initialize to Assumption University as default for testing on emulator without GPS.
        // Real location provider will override this if a valid location is obtained.
        currentLatLng = defaultCurrentLocation
        pickUpLatLng = defaultCurrentLocation

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
                if (isRideDetailOpenedFromMap) showMapContent() else showHistoryContent()
            } else if (isGroupChatVisible) {
                showRideInProcessContent()
            } else if (
                isHistoryVisible ||
                isProfileVisible ||
                isRideInProcessVisible ||
                isRidePaymentVisible ||
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
        binding.mapCompose.setHopOnContent {
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
        binding.ridesBottomSheetCard.setHopOnContent {
            mapHomeRideResultsScreen(
                visible = isRidePanelVisible,
                expanded = isRidePanelExpanded,
                currentUserId = SessionManager.getCurrentUserId(this),
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
        binding.predictionsCompose.setHopOnContent {
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
        binding.historyContent.setHopOnContent {
            historyRidesScreen(
                rides = historyRideItems,
                currentUserId = SessionManager.getCurrentUserId(this),
                currentUserName = resolveCurrentUserDisplayName(),
                onRideClick = ::showRideDetailForHistoryRide
            )
        }
    }

    private fun setUpRideDetailCompose() {
        binding.rideDetailContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.rideDetailContent.setHopOnContent {
            val ride = selectedHistoryRide
            if (ride != null) {
                rideDetailScreen(
                    onBackClick = {
                        if (isRideDetailOpenedFromMap) showMapContent() else showHistoryContent()
                    },
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
                    hostUserId = ride.hostUserId,
                    currentUserId = SessionManager.getCurrentUserId(this),
                    currentUserName = resolveCurrentUserDisplayName(),
                    hostRating = ride.hostRating,
                    hostVehicleType = ride.hostVehicleType,
                    peopleCount = ride.peopleCount,
                    pricePerSeat = ride.pricePerSeat,
                    seatsAvailable = ride.maxPeopleCount - ride.peopleCount,
                    bookingStatus = ride.bookingStatus,
                    pendingRequestCount = historyPendingBookingRequestCount,
                    bookingRequests = bookingRequestItems,
                    onApproveRequest = ::handleAcceptBooking,
                    onDeclineRequest = ::handleRejectBooking
                )
            }
        }
    }

    private fun setUpProfileCompose() {
        binding.profileContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.profileContent.setHopOnContent {
            profileScreen(
                userName = resolveCurrentUserDisplayName(),
                onLogoutClick = ::logoutAndNavigateToLogin
            )
        }
    }

    private fun resolveCurrentUserDisplayName(): String {
        return SessionManager.getDisplayName(this)
            ?: SessionManager.getCurrentUserId(this).orEmpty()
    }

    private fun setUpRideInProcessCompose() {
        binding.rideInProcessContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.rideInProcessContent.setHopOnContent {
            val ride = activeInProcessRide
            rideInProcessScreen(
                destinationLabel = ride?.destinationLabel,
                meetupLabel = ride?.meetupLabel,
                meetupDateTimeLabel = ride?.meetupDateTimeLabel,
                waitTimeMinutes = ride?.waitTimeMinutes,
                peopleCount = ride?.peopleCount,
                maxPeopleCount = ride?.maxPeopleCount,
                hostName = ride?.hostName,
                currentLocationLatLng = trackedInProcessLatLng ?: currentLatLng,
                meetupLatLng = ride?.meetupLatLng,
                destinationLatLng = ride?.destinationLatLng,
                pickupRoutePoints = pickupRoutePoints,
                rideRoutePoints = rideRoutePoints,
                onGroupChatClick = ::showGroupChatContent,
                onCancelRideClick = ::showCancelRideConfirmation,
                onBackClick = ::showHistoryContent
            )
        }
    }

    private fun setUpRidePaymentCompose() {
        binding.ridePaymentContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.ridePaymentContent.setHopOnContent {
            val ride = activeInProcessRide
            val passengerNames = activeRidePassengerNames
            val durationMinutes = ride?.rideTimeMinutes ?: 18
            val distanceMiles = ride?.let {
                val meters = calculateDistanceMeters(it.meetupLatLng, it.destinationLatLng)
                (meters / 1609.344).toDouble()
            } ?: 5.2

            ridePaymentScreen(
                isHost = isPendingPaymentHost,
                hostName = ride?.hostName ?: resolveCurrentUserDisplayName(),
                passengerNames = passengerNames,
                fareBaht = ride?.pricePerSeat?.toInt() ?: 30,
                durationMinutes = durationMinutes,
                distanceMiles = distanceMiles,
                currentLocationLatLng = currentLatLng,
                meetupLatLng = ride?.meetupLatLng,
                destinationLatLng = ride?.destinationLatLng,
                pickupRoutePoints = pickupRoutePoints,
                rideRoutePoints = rideRoutePoints,
                onHostFinishClick = ::completeRideAfterPayment,
                onPassengerConfirmClick = ::handlePassengerPaymentConfirm
            )
        }
    }

    private fun showCancelRideConfirmation() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.cancel_ride_confirm_title))
            .setMessage(getString(R.string.cancel_ride_confirm_message))
            .setNegativeButton(getString(R.string.go_back)) { _, _ -> }
            .setPositiveButton(R.string.cancel_ride) { _, _ ->
                cancelCurrentOngoingRide()
            }
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(this, R.color.cancelRideRed))
    }

    private fun cancelCurrentOngoingRide() {
        val ride = activeInProcessRide

        // Cancel the booking or post via API (fire-and-forget).
        if (ride != null) {
            lifecycleScope.launch {
                val bookingId = ride.bookingId
                if (!bookingId.isNullOrBlank()) {
                    runCatching { bookingRepository.cancelBooking(bookingId) }
                } else if (ride.postId.isNotBlank()) {
                    runCatching {
                        ApiClient.create<PostsService>(this@RootHostActivity)
                            .updateStatus(ride.postId, ApiUpdateStatusRequest("cancelled"))
                    }
                }
            }
        }

        chatSocketManager?.disconnect()
        currentChatPostId = null
        activeInProcessRide = null
        activeRidePassengerNames = emptyList()
        NotificationManagerCompat.from(this).cancel(RIDE_ONGOING_NOTIFICATION_ID)
        pickupRoutePoints = emptyList()
        rideRoutePoints = emptyList()
        selectedBottomNavItem = MapsBottomNavItem.RIDES
        showHistoryContent()
        Toast.makeText(this, getString(R.string.cancel_ride_success), Toast.LENGTH_SHORT).show()
    }

    private fun setUpGroupChatCompose() {
        binding.groupChatContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.groupChatContent.setHopOnContent {
            groupChatScreen(
                currentUserId = SessionManager.getCurrentUserId(this),
                participants = groupChatParticipants,
                messages = groupChatMessages,
                onSendMessage = ::handleSendGroupChatMessage,
                onBackClick = ::showRideInProcessContent
            )
        }
    }

    private fun handleSendGroupChatMessage(message: String) {
        chatSocketManager?.sendMessage(message)
    }

    private fun setUpCreateRideCompose() {
        binding.createRideContent.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.createRideContent.setHopOnContent {
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
        binding.searchBarContainer.setHopOnContent {
            mapHomeSearchBarScreen(
                query = searchQuery,
                requestFocus = shouldRequestSearchFocus,
                onFocusHandled = { shouldRequestSearchFocus = false },
                clearFocusSignal = clearSearchFocusSignal,
                onFocusChanged = { hasFocus ->
                    if (hasFocus) {
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
                    activateInlineSearchMode()
                }
            )
        }
    }

    private fun showRideDetailForHistoryRide(ride: RideListItem) {
        if (ride.lifecycleStatus == RideLifecycleStatus.ONGOING) {
            activeInProcessRide = ride
            showRideInProcessContent()
            return
        }

        isRideDetailOpenedFromMap = false
        historyPendingBookingRequestCount = 0
        bookingRequestItems = emptyList()
        val currentUserId = SessionManager.getCurrentUserId(this).orEmpty()
        val iAmDriver = (ride.hostUserId.isNotBlank() && ride.hostUserId == currentUserId) ||
            ride.participationRole == RideParticipationRole.HOSTED
        if (iAmDriver && ride.postUuid.isNotEmpty()) {
            bookingRequestsPostUuid = ride.postUuid
            loadBookingRequestsForPost(ride.postUuid)
        } else {
            bookingRequestsPostUuid = null
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
        
        // Show "Join Ride" / action button
        binding.createRideButton.visibility = View.VISIBLE

        // Reset Phase 6 booking state for the newly selected ride
        activeBookingId = null
        activeBookingStatus = null
        isSelectedRideDriver = false
        bookingRequestItems = emptyList()
        pendingBookingRequestCount = 0

        // Load booking / driver state from API when this is a real post
        if (ride.postId.isNotEmpty()) {
            val currentUserId = SessionManager.getCurrentUserId(this).orEmpty()

            // Check if the current user is the driver of this ride
            lifecycleScope.launch {
                val iAmDriver = (ride.hostUserId.isNotBlank() && ride.hostUserId == currentUserId) ||
                    ride.participationRole == RideParticipationRole.HOSTED

                if (iAmDriver && ride.postUuid.isNotEmpty()) {
                    isSelectedRideDriver = true
                    loadBookingRequestsForPost(ride.postUuid)
                } else if (ride.postId.isNotEmpty()) {
                    loadMyBookingForRide(ride.postId)
                }
            }
        }
    }

    /** Load the current user's booking (if any) for the given post MongoDB _id. */
    private fun loadMyBookingForRide(postMongoId: String) {
        lifecycleScope.launch {
            val result = bookingRepository.getBookingForPost(postMongoId)
            result.getOrNull()?.let { booking ->
                activeBookingId = booking.id
                activeBookingStatus = booking.status
            }
        }
    }

    /** Driver: load booking requests for a post (by UUID). */
    private fun loadBookingRequestsForPost(postUuid: String) {
        lifecycleScope.launch {
            val result = bookingRepository.getBookingsForPost(postUuid)
            result.fold(
                onSuccess = { requests ->
                    bookingRequestItems = requests
                    val pendingCount = requests.count { it.status == "pending" }
                    pendingBookingRequestCount = pendingCount
                    historyPendingBookingRequestCount = pendingCount
                },
                onFailure = {
                    bookingRequestItems = emptyList()
                    pendingBookingRequestCount = 0
                    historyPendingBookingRequestCount = 0
                }
            )
        }
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

        // Reset Phase 6 booking state
        activeBookingId = null
        activeBookingStatus = null
        isSelectedRideDriver = false
        bookingRequestItems = emptyList()
        pendingBookingRequestCount = 0

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
        binding.createRideButton.setHopOnContent {
            mapHomePrimaryActionButton(
                selectedRide = selectedRide,
                onCreateRideClick = ::handleCreateRideClick,
                onJoinRideClick = ::handleJoinRideClick,
                bookingStatus = activeBookingStatus,
                isBookingLoading = isBookingLoading,
                onCancelBookingClick = ::handleCancelBookingClick,
                onViewRequestsClick = ::handleViewRequestsClick,
                pendingRequestCount = pendingBookingRequestCount,
                isOwnRide = isSelectedRideDriver
            )
        }
    }

    private fun setUpBottomNavCompose() {
        binding.bottomNav.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.bottomNav.setHopOnContent {
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
            isHistoryVisible || isRideDetailVisible || isRideInProcessVisible || isRidePaymentVisible -> MapsBottomNavItem.RIDES
            isProfileVisible -> MapsBottomNavItem.PROFILE
            else -> MapsBottomNavItem.HOME
        }
    }

    private fun showHistoryContent() {
        stopInProcessTrackingSync()
        animateMainContentTransition()
        isHistoryVisible = true
        isRideDetailVisible = false
        isProfileVisible = false
        isRideInProcessVisible = false
        isRidePaymentVisible = false
        isCreateRideVisible = false
        isGroupChatVisible = false
        isRideDetailOpenedFromMap = false
        isRidePanelVisible = false
        isRidePanelExpanded = false
        binding.historyContent.visibility = View.VISIBLE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.GONE
        binding.ridePaymentContent.visibility = View.GONE
        binding.createRideContent.visibility = View.GONE
        binding.groupChatContent.visibility = View.GONE
        binding.bookingRequestsContent.visibility = View.GONE
        binding.searchBarContainer.visibility = View.GONE
        binding.bottomNav.visibility = View.VISIBLE
        binding.predictionsCard.visibility = View.GONE
        binding.ridesBottomSheetCard.visibility = View.GONE
        binding.mapTouchOverlay.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        clearRideDetailSelection()
        historyPendingBookingRequestCount = 0

        lifecycleScope.launch {
            val currentUserId = SessionManager.getCurrentUserId(this@RootHostActivity)
            val userLatLng = currentLatLng

            // Hosted rides (user is the driver)
            val myRides = rideRepository.getMyRides()

            // Joined rides (user has a booking as a passenger)
            val myBookings = bookingRepository.getMyBookings().getOrElse { emptyList() }
            val joinedRides = myBookings.mapNotNull { booking ->
                val post = booking.post_id ?: return@mapNotNull null
                post.toRideListItem(userLatLng, currentUserId).copy(
                    bookingId     = booking.id,
                    bookingStatus = normaliseBookingStatus(booking.status)
                )
            }

            val apiRides = (myRides + joinedRides)
                .distinctBy { it.postId.ifEmpty { "${it.meetupLabel}|${it.destinationLabel}|${it.meetupDateTimeLabel}" } }

            historyRideItems = apiRides
        }
        clearMeetupLocationMarkers()
    }

    private fun showMapContent() {
        stopInProcessTrackingSync()
        animateMainContentTransition()
        isHistoryVisible = false
        isRideDetailVisible = false
        isProfileVisible = false
        isRideInProcessVisible = false
        isRidePaymentVisible = false
        isCreateRideVisible = false
        isGroupChatVisible = false
        isRideDetailOpenedFromMap = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.GONE
        binding.ridePaymentContent.visibility = View.GONE
        binding.createRideContent.visibility = View.GONE
        binding.groupChatContent.visibility = View.GONE
        binding.bookingRequestsContent.visibility = View.GONE
        binding.bottomNav.visibility = View.VISIBLE
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
        stopInProcessTrackingSync()
        animateMainContentTransition()
        isHistoryVisible = false
        isRideDetailVisible = false
        isProfileVisible = true
        isRideInProcessVisible = false
        isRidePaymentVisible = false
        isCreateRideVisible = false
        isGroupChatVisible = false
        isRideDetailOpenedFromMap = false
        isRidePanelVisible = false
        isRidePanelExpanded = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.VISIBLE
        binding.rideInProcessContent.visibility = View.GONE
        binding.ridePaymentContent.visibility = View.GONE
        binding.createRideContent.visibility = View.GONE
        binding.groupChatContent.visibility = View.GONE
        binding.bookingRequestsContent.visibility = View.GONE
        binding.bottomNav.visibility = View.VISIBLE
        binding.searchBarContainer.visibility = View.GONE
        binding.predictionsCard.visibility = View.GONE
        binding.ridesBottomSheetCard.visibility = View.GONE
        binding.mapTouchOverlay.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        clearRideDetailSelection()
        clearMeetupLocationMarkers()
        selectedHistoryRide = null
        historyPendingBookingRequestCount = 0
    }

    private fun showRideDetailContent() {
        stopInProcessTrackingSync()
        animateMainContentTransition()
        isHistoryVisible = false
        isRideDetailVisible = true
        isProfileVisible = false
        isRideInProcessVisible = false
        isRidePaymentVisible = false
        isCreateRideVisible = false
        isGroupChatVisible = false
        isRidePanelVisible = false
        isRidePanelExpanded = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.VISIBLE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.GONE
        binding.ridePaymentContent.visibility = View.GONE
        binding.createRideContent.visibility = View.GONE
        binding.groupChatContent.visibility = View.GONE
        binding.bookingRequestsContent.visibility = View.GONE
        binding.bottomNav.visibility = View.VISIBLE
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
        isRidePaymentVisible = false
        isCreateRideVisible = false
        isGroupChatVisible = false
        isRideDetailOpenedFromMap = false
        isRidePanelVisible = false
        isRidePanelExpanded = false
        hasLocallyDetectedTripEnd = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.VISIBLE
        binding.ridePaymentContent.visibility = View.GONE
        binding.createRideContent.visibility = View.GONE
        binding.groupChatContent.visibility = View.GONE
        binding.bookingRequestsContent.visibility = View.GONE
        binding.bottomNav.visibility = View.VISIBLE
        binding.searchBarContainer.visibility = View.GONE
        binding.predictionsCard.visibility = View.GONE
        binding.ridesBottomSheetCard.visibility = View.GONE
        binding.mapTouchOverlay.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        clearRideDetailSelection()
        clearMeetupLocationMarkers()
        selectedHistoryRide = null
        trackedInProcessLatLng = currentLatLng

        val ride = activeInProcessRide
        requestRideInProcessRoutes(
            current = currentLatLng,
            meetup = ride?.meetupLatLng,
            destination = ride?.destinationLatLng
        )

        // Fetch confirmed passenger names for the active ride.
        val postUuid = ride?.postUuid
        if (!postUuid.isNullOrBlank()) {
            lifecycleScope.launch {
                val bookings = bookingRepository.getBookingsForPost(postUuid).getOrNull().orEmpty()
                activeRidePassengerNames = bookings
                    .filter { it.status == "confirmed" }
                    .mapNotNull { it.passenger_id?.fullName }
            }
        }

        startInProcessTrackingSync()
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
        stopInProcessTrackingSync()
        animateMainContentTransition()
        isHistoryVisible = false
        isRideDetailVisible = false
        isProfileVisible = false
        isRideInProcessVisible = false
        isRidePaymentVisible = false
        isCreateRideVisible = true
        isGroupChatVisible = false
        isRideDetailOpenedFromMap = false
        isRidePanelVisible = false
        isRidePanelExpanded = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.GONE
        binding.ridePaymentContent.visibility = View.GONE
        binding.createRideContent.visibility = View.VISIBLE
        binding.groupChatContent.visibility = View.GONE
        binding.bookingRequestsContent.visibility = View.GONE
        binding.bottomNav.visibility = View.VISIBLE
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

    private fun showGroupChatContent() {
        stopInProcessTrackingSync()
        animateMainContentTransition()
        isHistoryVisible = false
        isRideDetailVisible = false
        isProfileVisible = false
        isRideInProcessVisible = false
        isRidePaymentVisible = false
        isCreateRideVisible = false
        isGroupChatVisible = true
        isRideDetailOpenedFromMap = false
        isRidePanelVisible = false
        isRidePanelExpanded = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.GONE
        binding.ridePaymentContent.visibility = View.GONE
        binding.createRideContent.visibility = View.GONE
        binding.groupChatContent.visibility = View.VISIBLE
        binding.bookingRequestsContent.visibility = View.GONE
        binding.bottomNav.visibility = View.GONE
        binding.searchBarContainer.visibility = View.GONE
        binding.predictionsCard.visibility = View.GONE
        binding.ridesBottomSheetCard.visibility = View.GONE
        binding.mapTouchOverlay.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        clearRideDetailSelection()
        clearMeetupLocationMarkers()
        selectedHistoryRide = null

        val postId = activeInProcessRide?.postId
        if (postId.isNullOrBlank()) {
            groupChatMessages = emptyList()
            groupChatParticipants = emptyList()
            return
        }

        // Build participant list from the active ride host + passenger names.
        val hostName = activeInProcessRide?.hostName.orEmpty()
        groupChatParticipants = (listOf(hostName) + activeRidePassengerNames).filter { it.isNotBlank() }

        // Load message history via REST.
        currentChatPostId = postId
        lifecycleScope.launch {
            val messages = runCatching {
                ApiClient.create<MessagesService>(this@RootHostActivity)
                    .getMessages(postId)
            }.getOrNull().orEmpty()
            groupChatMessages = messages.toMockChatMessages()
        }

        // Connect Socket.IO for real-time messages.
        chatSocketManager?.disconnect()
        chatSocketManager = ChatSocketManager(applicationContext)
        chatSocketManager?.connect(postId) { newMessage ->
            // Avoid duplicates from our own sends echoed back.
            groupChatMessages = groupChatMessages + newMessage
        }
    }

    private fun showRidePaymentContent(isHost: Boolean) {
        stopInProcessTrackingSync()
        animateMainContentTransition()
        selectedBottomNavItem = MapsBottomNavItem.RIDES
        isPendingPaymentHost = isHost
        isHistoryVisible = false
        isRideDetailVisible = false
        isProfileVisible = false
        isRideInProcessVisible = false
        isRidePaymentVisible = true
        isCreateRideVisible = false
        isGroupChatVisible = false
        isRideDetailOpenedFromMap = false
        isRidePanelVisible = false
        isRidePanelExpanded = false
        binding.historyContent.visibility = View.GONE
        binding.rideDetailContent.visibility = View.GONE
        binding.profileContent.visibility = View.GONE
        binding.rideInProcessContent.visibility = View.GONE
        binding.ridePaymentContent.visibility = View.VISIBLE
        binding.createRideContent.visibility = View.GONE
        binding.groupChatContent.visibility = View.GONE
        binding.bookingRequestsContent.visibility = View.GONE
        binding.bottomNav.visibility = View.VISIBLE
        binding.searchBarContainer.visibility = View.GONE
        binding.predictionsCard.visibility = View.GONE
        binding.ridesBottomSheetCard.visibility = View.GONE
        binding.mapTouchOverlay.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        clearRideDetailSelection()
        clearMeetupLocationMarkers()
        selectedHistoryRide = null
    }

    private fun completeRideAfterPayment() {
        stopInProcessTrackingSync()
        chatSocketManager?.disconnect()
        currentChatPostId = null
        activeInProcessRide = null
        activeRidePassengerNames = emptyList()
        isPendingPaymentHost = false
        Toast.makeText(this, getString(R.string.trip_end), Toast.LENGTH_SHORT).show()
        reset()
        selectedBottomNavItem = MapsBottomNavItem.RIDES
        showHistoryContent()
    }

    /**
     * Called when a passenger taps "Confirm & Pay" on the payment screen.
     * Submits feedback to the API and optionally creates a payment record.
     */
    private fun handlePassengerPaymentConfirm(rating: Int, comment: String) {
        val ride = activeInProcessRide
        val currentUserId = SessionManager.getCurrentUserId(this).orEmpty()

        if (ride != null && ride.postId.isNotBlank() && ride.hostUserId.isNotBlank()) {
            lifecycleScope.launch {
                // Submit feedback.
                runCatching {
                    ApiClient.create<FeedbackService>(this@RootHostActivity)
                        .createFeedback(
                            ApiCreateFeedbackRequest(
                                post_id = ride.postId,
                                reviewer_id = currentUserId,
                                reviewee_id = ride.hostUserId,
                                rating = rating,
                                comment = comment.ifBlank { null }
                            )
                        )
                }

                // Create payment record if the user has a booking.
                val bookingId = ride.bookingId
                if (!bookingId.isNullOrBlank()) {
                    runCatching {
                        ApiClient.create<PaymentsService>(this@RootHostActivity)
                            .createPayment(
                                ApiCreatePaymentRequest(
                                    booking_id = bookingId,
                                    amount = ride.pricePerSeat,
                                    status = "completed",
                                    payment_type = "cash"
                                )
                            )
                    }
                }
            }
        }

        completeRideAfterPayment()
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
        lifecycleScope.launch {
            val apiItem = rideRepository.createRide(submission)

            if (apiItem != null) {
                showMapContent()
                applySelectedPlace(apiItem.destinationLabel, apiItem.destinationLatLng)
                Toast.makeText(this@RootHostActivity, getString(R.string.create_ride), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@RootHostActivity, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleNavigationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_RIDE_IN_PROCESS, false) == true) {
            selectedBottomNavItem = MapsBottomNavItem.RIDES
            // Try to resolve an ongoing ride from the API.
            lifecycleScope.launch {
                resolveActiveInProcessRide()
                showRideInProcessContent()
            }
            intent.removeExtra(EXTRA_OPEN_RIDE_IN_PROCESS)
        }
    }

    /**
     * Attempts to resolve the current user's in-progress ride from the API
     * and set [activeInProcessRide].  Checks both hosted and joined rides.
     */
    private suspend fun resolveActiveInProcessRide() {
        if (activeInProcessRide != null) return

        val currentUserId = SessionManager.getCurrentUserId(this).orEmpty()

        // Check hosted rides.
        val myRides = rideRepository.getMyRides()
        val hostedOngoing = myRides.firstOrNull { it.lifecycleStatus == RideLifecycleStatus.ONGOING }
        if (hostedOngoing != null) {
            activeInProcessRide = hostedOngoing
            return
        }

        // Check joined rides (via bookings).
        val bookings = bookingRepository.getMyBookings().getOrNull().orEmpty()
        val joinedOngoing = bookings
            .filter { it.status == "confirmed" }
            .mapNotNull { booking ->
                booking.post_id?.takeIf { it.status == "in_progress" }
                    ?.toRideListItem(currentUserLatLng = currentLatLng, currentUserId = currentUserId)
                    ?.copy(
                        bookingId = booking.id,
                        bookingStatus = normaliseBookingStatus(booking.status)
                    )
            }
            .firstOrNull()
        if (joinedOngoing != null) {
            activeInProcessRide = joinedOngoing
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

        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) {
            clearCreateRidePredictions()
            return
        }

        scheduleCreateRidePredictionFetch(field, trimmedQuery)
    }

    private fun handleCreateRideSearchAction(field: CreateRideLocationField) {
        activeCreateRideField = field
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
        val ride = selectedRide ?: return
        if (ride.postUuid.isEmpty()) {
            Toast.makeText(this, getString(R.string.booking_error_generic), Toast.LENGTH_SHORT).show()
            return
        }

        // Real API ride — create a booking
        isBookingLoading = true
        lifecycleScope.launch {
            val result = bookingRepository.createBooking(ride.postUuid)
            isBookingLoading = false
            result.fold(
                onSuccess = { booking ->
                    activeBookingId     = booking.id
                    activeBookingStatus = booking.status
                    Toast.makeText(
                        this@RootHostActivity,
                        getString(R.string.booking_sent_success),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = {
                    val msg = extractApiErrorMessage(it) ?: it.message.orEmpty()
                    val userMsg = when {
                        msg.contains("own ride", ignoreCase = true) ->
                            getString(R.string.booking_error_own_ride)
                        msg.contains("already", ignoreCase = true) ->
                            getString(R.string.booking_error_already_booked)
                        msg.contains("seats", ignoreCase = true) ->
                            getString(R.string.booking_error_no_seats)
                        else -> getString(R.string.booking_error_generic)
                    }
                    Toast.makeText(this@RootHostActivity, userMsg, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun extractApiErrorMessage(error: Throwable): String? {
        val http = error as? HttpException ?: return null
        val raw = runCatching { http.response()?.errorBody()?.string() }.getOrNull() ?: return null
        return runCatching { JSONObject(raw).optString("message") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private fun handleCancelBookingClick() {
        val bookingId = activeBookingId ?: return
        isBookingLoading = true
        lifecycleScope.launch {
            val result = bookingRepository.cancelBooking(bookingId)
            isBookingLoading = false
            result.fold(
                onSuccess = {
                    activeBookingId     = null
                    activeBookingStatus = null
                    Toast.makeText(
                        this@RootHostActivity,
                        getString(R.string.booking_cancel_success),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = {
                    Toast.makeText(
                        this@RootHostActivity,
                        getString(R.string.booking_error_generic),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun handleViewRequestsClick() {
        val ride = selectedRide ?: return
        val currentUserId = SessionManager.getCurrentUserId(this).orEmpty()
        val iAmDriver = (ride.hostUserId.isNotBlank() && ride.hostUserId == currentUserId) ||
            ride.participationRole == RideParticipationRole.HOSTED
        if (!iAmDriver || ride.postUuid.isEmpty()) return

        isRideDetailOpenedFromMap = true
        selectedHistoryRide = ride
        historyPendingBookingRequestCount = 0
        bookingRequestItems = emptyList()
        bookingRequestsPostUuid = ride.postUuid
        loadBookingRequestsForPost(ride.postUuid)
        showRideDetailContent()
    }

    private fun handleAcceptBooking(bookingId: String) {
        lifecycleScope.launch {
            val result = bookingRepository.respondToBooking(bookingId, accept = true)
            result.fold(
                onSuccess = { updated ->
                    // Refresh booking list
                    val postUuid = bookingRequestsPostUuid.orEmpty()
                    if (postUuid.isNotEmpty()) loadBookingRequestsForPost(postUuid)
                    Toast.makeText(
                        this@RootHostActivity,
                        getString(R.string.booking_respond_accept_success),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = {
                    Toast.makeText(
                        this@RootHostActivity,
                        getString(R.string.booking_error_generic),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun handleRejectBooking(bookingId: String) {
        lifecycleScope.launch {
            val result = bookingRepository.respondToBooking(bookingId, accept = false)
            result.fold(
                onSuccess = {
                    val postUuid = bookingRequestsPostUuid.orEmpty()
                    if (postUuid.isNotEmpty()) loadBookingRequestsForPost(postUuid)
                    Toast.makeText(
                        this@RootHostActivity,
                        getString(R.string.booking_respond_reject_success),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = {
                    Toast.makeText(
                        this@RootHostActivity,
                        getString(R.string.booking_error_generic),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
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

    private fun applySelectedPlace(name: String, latLng: LatLng) {
        // Update selected destination in UI and map state.
        dropLatLng = latLng
        searchQuery = name
        shouldRequestSearchFocus = false
        moveSearchBarToTop()
        showRideResultsPanel()
        binding.createRideButton.visibility = View.VISIBLE
        loadAndFilterRidesByDestination(name, latLng)

        if (!isMapReady) {
            Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
            return
        }

        destinationMarker?.remove()
        destinationMarker = addOriginDestinationMarkerAndGet(latLng)
        destinationMarker?.setAnchor(0.5f, 0.5f)
        // Camera will be re-aimed at the nearest meetup after rides load inside the coroutine;
        // animate to destination immediately as a default so the map responds right away.
        animateCamera(latLng)
    }

    /**
     * Loads rides near [destinationLatLng] from the API and falls back to mock data
     * when the API returns nothing (offline / dev mode).
     * Updates [ridePanelItems], [allRidePanelItems], meetup markers, and camera in one pass.
     */
    private fun loadAndFilterRidesByDestination(destinationName: String, destinationLatLng: LatLng) {
        lifecycleScope.launch {
            val apiRides = rideRepository.getNearbyRides(
                lat      = destinationLatLng.latitude,
                lng      = destinationLatLng.longitude
            )

            val rides = apiRides

            ridePanelItems    = rides
            allRidePanelItems = rides
            addMeetupLocationMarkers(rides)

            // Re-aim camera once we know the nearest meetup location.
            if (isMapReady) {
                val nearestMeetup = rides.firstOrNull()?.meetupLatLng ?: destinationLatLng
                animateCamera(nearestMeetup)
            }
        }
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

    private fun resolveActiveRideDestination(): LatLng? {
        return activeInProcessRide?.destinationLatLng
    }

    private fun maybeHandleLocalTripArrival(currentLocation: LatLng) {
        if (!isRideInProcessVisible || hasLocallyDetectedTripEnd) {
            return
        }

        val destination = resolveActiveRideDestination() ?: return
        val distanceToDestinationMeters = calculateDistanceMeters(currentLocation, destination)
        Log.d(
            TAG,
            "localArrivalCheck distance=${distanceToDestinationMeters.toInt()}m current=${currentLocation.latitude},${currentLocation.longitude} destination=${destination.latitude},${destination.longitude}"
        )
        if (distanceToDestinationMeters <= ARRIVAL_RADIUS_METERS) {
            hasLocallyDetectedTripEnd = true
            informTripEnd()
        }
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

                for (location in locationResult.locations) {
                    val latestLatLng = LatLng(location.latitude, location.longitude)
                    val isFirstFix = currentLatLng == null || currentLatLng == defaultCurrentLocation
                    currentLatLng = latestLatLng

                    if (isFirstFix || pickUpLatLng == null) {
                        setCurrentLocationAsPickUp()
                        enableMyLocationOnMap()
                        moveCamera(currentLatLng!!)
                        animateCamera(currentLatLng!!)
                    } else if (isRideInProcessVisible && isMapReady) {
                        animateCamera(latestLatLng)
                    }

                    maybeHandleLocalTripArrival(latestLatLng)
                    pushTrackingIfNeeded(latestLatLng)
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
        stopInProcessTrackingSync()
        // Restore map and UI to pre-booking default state.
        isRidePaymentVisible = false
        isCreateRideVisible = false
        isGroupChatVisible = false
        isRideDetailOpenedFromMap = false
        binding.ridePaymentContent.visibility = View.GONE
        binding.createRideContent.visibility = View.GONE
        binding.groupChatContent.visibility = View.GONE
        binding.bookingRequestsContent.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        activeInProcessRide = null
        trackedInProcessLatLng = null
        isPendingPaymentHost = false
        groupChatMessages = emptyList()
        groupChatParticipants = emptyList()
        hideRideResultsPanel(clearData = true)
        searchQuery = ""
        clearSearchFocus()
        moveSearchBarToBottom()

        hasLocallyDetectedTripEnd = false

        if (currentLatLng != null) {
            moveCamera(currentLatLng!!)
            animateCamera(currentLatLng!!)
            setCurrentLocationAsPickUp()
        }

        greyPolyLine?.remove()
        blackPolyline?.remove()
        originMarker?.remove()
        destinationMarker?.remove()

        dropLatLng = null
        greyPolyLine = null
        blackPolyline = null
        originMarker = null
        destinationMarker = null
        clearCreateRidePredictions()
        activeCreateRideField = null
    }

    override fun onStart() {
        super.onStart()

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

    override fun onDestroy() {
        stopInProcessTrackingSync()
        chatSocketManager?.disconnect()
        chatSocketManager = null
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

    private fun isActiveRideHost(): Boolean {
        val ride = activeInProcessRide ?: return false
        val currentUserId = SessionManager.getCurrentUserId(this).orEmpty()
        return ride.participationRole == RideParticipationRole.HOSTED ||
            (currentUserId.isNotBlank() && ride.hostUserId == currentUserId)
    }

    private fun startInProcessTrackingSync() {
        stopInProcessTrackingSync()
        if (BuildConfig.USE_SIMULATOR) return

        val ride = activeInProcessRide ?: return
        val postId = ride.postId.ifBlank { return }

        if (isActiveRideHost()) {
            trackedInProcessLatLng = currentLatLng
            return
        }

        trackingPollJob = lifecycleScope.launch {
            while (isActive && isRideInProcessVisible) {
                val tracking = runCatching {
                    trackingService.getTracking(postId)
                }.getOrNull()

                if (tracking != null) {
                    val trackedLatLng = LatLng(tracking.current_lat, tracking.current_lng)
                    trackedInProcessLatLng = trackedLatLng
                    requestRideInProcessRoutes(
                        current = trackedLatLng,
                        meetup = ride.meetupLatLng,
                        destination = ride.destinationLatLng
                    )
                    maybeHandleLocalTripArrival(trackedLatLng)
                }

                delay(3000L)
            }
        }
    }

    private fun stopInProcessTrackingSync() {
        trackingPollJob?.cancel()
        trackingPollJob = null
        trackedInProcessLatLng = null
    }

    private fun pushTrackingIfNeeded(latestLatLng: LatLng) {
        if (BuildConfig.USE_SIMULATOR || !isRideInProcessVisible) return
        if (!isActiveRideHost()) return

        val ride = activeInProcessRide ?: return
        val postId = ride.postId.ifBlank { return }
        val now = System.currentTimeMillis()
        if (now - lastTrackingPushAtMillis < 2000L) return
        lastTrackingPushAtMillis = now
        trackedInProcessLatLng = latestLatLng

        lifecycleScope.launch {
            runCatching {
                trackingService.updateTracking(
                    postId,
                    ApiUpdateTrackingRequest(
                        current_lat = latestLatLng.latitude,
                        current_lng = latestLatLng.longitude,
                        eta_minutes = null
                    )
                )
            }
        }
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


    override fun informTripEnd() {
        hasLocallyDetectedTripEnd = true
        val currentUserId = SessionManager.getCurrentUserId(this)
        val ride = activeInProcessRide
        if (ride == null) {
            Toast.makeText(this, getString(R.string.trip_end), Toast.LENGTH_SHORT).show()
            reset()
            return
        }

        val isHost = ride.participationRole == RideParticipationRole.HOSTED
                || ride.hostUserId == currentUserId

        // Mark the post as completed via API (fire-and-forget).
        if (isHost && ride.postId.isNotBlank()) {
            lifecycleScope.launch {
                runCatching {
                    ApiClient.create<PostsService>(this@RootHostActivity)
                        .updateStatus(ride.postId, ApiUpdateStatusRequest("completed"))
                }
            }
        }

        showRidePaymentContent(isHost = isHost)
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
