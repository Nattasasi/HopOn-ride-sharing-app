package com.tritech.hopon.ui.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.tritech.hopon.BuildConfig
import com.tritech.hopon.R
import com.tritech.hopon.data.network.NetworkService
import com.tritech.hopon.databinding.ActivityMapsBinding
import com.tritech.hopon.ui.auth.LoginActivity
import com.tritech.hopon.ui.rides.RideDetailActivity
import com.tritech.hopon.utils.AnimationUtils
import com.tritech.hopon.utils.MapUtils
import com.tritech.hopon.utils.PermissionUtils
import com.tritech.hopon.utils.SessionManager
import com.tritech.hopon.ui.components.hopOnButton
import com.tritech.hopon.ui.maps.mapsBottomNavigation
import com.tritech.hopon.ui.maps.rideResultsBottomSheetPanel
import com.tritech.hopon.utils.ViewUtils
import java.util.Locale
import kotlin.math.max

class MapsActivity : AppCompatActivity(), MapsView, OnMapReadyCallback {

    companion object {
        private const val TAG = "MapsActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 999
        private const val PLACE_REQUEST_CODE = 2001
    }

    private data class PlaceholderPlace(val name: String, val latLng: LatLng)

    private data class MockRide(
        val meetupLabel: String,
        val meetupLatLng: LatLng,
        val destinationLabel: String,
        val destinationLatLng: LatLng
    )

    private lateinit var binding: ActivityMapsBinding
    private lateinit var googleMap: GoogleMap
    private var isMapReady = false
    private var presenter: MapsPresenter? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private var placesClient: PlacesClient? = null

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
    private var meetupLocationMarkers: MutableList<Marker> = mutableListOf()
    private var meetupMarkerIcon: BitmapDescriptor? = null
    private var isSearchBarAtTop = false
    private lateinit var predictionAdapter: PlacePredictionAdapter
    private val searchDebounceHandler = Handler(Looper.getMainLooper())
    private var pendingSearchRunnable: Runnable? = null
    private var latestRequestToken = 0L
    private var latestPredictions: List<AutocompletePrediction> = emptyList()
    private var suppressSearchWatcher = false
    private var isRidePanelVisible by mutableStateOf(false)
    private var ridePanelItems by mutableStateOf<List<RideListItem>>(emptyList())
    private var isRidePanelExpanded by mutableStateOf(false)

    // Demo destinations used when mock mode is enabled.
    private val placeholderPlaces = listOf(
        PlaceholderPlace("Siam Paragon", LatLng(13.7466, 100.5347)),
        PlaceholderPlace("CentralWorld", LatLng(13.7460, 100.5395)),
        PlaceholderPlace("Terminal 21", LatLng(13.7373, 100.5607))
    )
    private var placeholderPlaceIndex = 0

    private val mockRides = listOf(
        MockRide("Asok BTS", LatLng(13.7370, 100.5603), "Siam Paragon", LatLng(13.7466, 100.5347)),
        MockRide("Phrom Phong", LatLng(13.7306, 100.5696), "Siam Paragon", LatLng(13.7466, 100.5347)),
        MockRide("Chit Lom", LatLng(13.7449, 100.5431), "Siam Paragon", LatLng(13.7466, 100.5347)),
        MockRide("Nana", LatLng(13.7405, 100.5550), "CentralWorld", LatLng(13.7460, 100.5395)),
        MockRide("Ratchathewi", LatLng(13.7514, 100.5310), "CentralWorld", LatLng(13.7460, 100.5395)),
        MockRide("Victory Monument", LatLng(13.7628, 100.5372), "CentralWorld", LatLng(13.7460, 100.5395)),
        MockRide("Sukhumvit Soi 11", LatLng(13.7429, 100.5559), "Terminal 21", LatLng(13.7373, 100.5607)),
        MockRide("Benjasiri Park", LatLng(13.7308, 100.5680), "Terminal 21", LatLng(13.7373, 100.5607)),
        MockRide("Ekkamai", LatLng(13.7197, 100.5850), "Terminal 21", LatLng(13.7373, 100.5607)),
        MockRide("Silom Complex", LatLng(13.7296, 100.5349), "Siam Paragon", LatLng(13.7466, 100.5347)),
        MockRide("Samyan Mitrtown", LatLng(13.7327, 100.5291), "CentralWorld", LatLng(13.7460, 100.5395)),
        MockRide("Thong Lo", LatLng(13.7241, 100.5783), "Terminal 21", LatLng(13.7373, 100.5607))
    )

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
        ViewUtils.enableTransparentStatusBar(window)
        setUpImeSpacing()
        setUpRideResultsPanelCompose()
        setUpBackPressHandler()

        // Live mode initializes Places API client + websocket presenter.
        if (!BuildConfig.USE_MOCK_DATA) {
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, getString(R.string.google_maps_key))
            }
            placesClient = Places.createClient(this)
            presenter = MapsPresenter(NetworkService())
            presenter?.onAttach(this)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize to Assumption University as default for testing on emulator without GPS.
        // Real location provider will override this if a valid location is obtained.
        currentLatLng = mockCurrentLocation
        pickUpLatLng = mockCurrentLocation

        setUpClickListener()
        setUpInlineSearch()
    }

    private fun setUpBackPressHandler() {
        // First back press restores collapsed/default map UI before exiting screen.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (restoreDefaultMapUiIfNeeded()) {
                    return
                }
                finish()
            }
        })
    }

    private fun restoreDefaultMapUiIfNeeded(): Boolean {
        if (!isRidePanelVisible &&
            !isSearchBarAtTop &&
            binding.predictionsCard.visibility != View.VISIBLE
        ) {
            return false
        }

        pendingSearchRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
        pendingSearchRunnable = null
        latestPredictions = emptyList()
        predictionAdapter.submitList(emptyList())
        binding.emptyPredictionsTextView.visibility = View.GONE
        binding.predictionsCard.visibility = View.GONE
        binding.searchBarTextView.clearFocus()
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.searchBarTextView.windowToken, 0)

        hideRideResultsPanel(clearData = true)
        binding.createRideButton.visibility = View.GONE
        binding.searchBarTextView.setText("")
        moveSearchBarToBottom()
        return true
    }

    private fun setUpRideResultsPanelCompose() {
        binding.ridesBottomSheetCard.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.ridesBottomSheetCard.setContent {
            rideResultsBottomSheetPanel(
                visible = isRidePanelVisible,
                expanded = isRidePanelExpanded,
                onExpandChange = { expanded ->
                    if (expanded) expandRidePanel() else collapseRidePanel()
                },
                rides = ridePanelItems,
                onRideClick = { ride ->
                    openRideDetail(ride)
                }
            )
        }
    }

    private fun openRideDetail(ride: RideListItem) {
            val distanceKm = String.format(Locale.US, "%.2f", ride.pickupDistanceMeters / 1000f)
            startActivity(
                RideDetailActivity.createIntent(
                    this,
                    ride.meetupLabel,
                    ride.destinationLabel,
                    distanceKm
                )
            )
    }

    private fun showRideResultsPanel() {
        binding.ridesBottomSheetCard.visibility = View.VISIBLE
        binding.mapTouchOverlay.visibility = View.GONE  // Keep invisible - map's own click listener handles tap to dismiss
        isRidePanelVisible = true
    }

    private fun hideRideResultsPanel(clearData: Boolean) {
        isRidePanelVisible = false
        isRidePanelExpanded = false
        binding.ridesBottomSheetCard.visibility = View.GONE
        binding.mapTouchOverlay.visibility = View.GONE
        clearMeetupLocationMarkers()
        if (clearData) {
            ridePanelItems = emptyList()
        }
    }

    private fun expandRidePanel() {
        if (!isRidePanelVisible) showRideResultsPanel()
        isRidePanelExpanded = true
    }

    private fun collapseRidePanel() {
        isRidePanelExpanded = false
    }

    private fun addMeetupLocationMarkers(rides: List<RideListItem>) {
        clearMeetupLocationMarkers()
        if (meetupMarkerIcon == null) {
            meetupMarkerIcon = BitmapDescriptorFactory.fromBitmap(
                MapUtils.getLocationIconBitmap(this, R.drawable.location_on_24, R.color.colorPrimaryDark)
            )
        }
        for (ride in rides) {
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(ride.meetupLatLng)
                    .icon(meetupMarkerIcon)
                    .flat(false)
                    .title(ride.meetupLabel)
            )
            if (marker != null) {
                meetupLocationMarkers.add(marker)
            }
        }
    }

    private fun clearMeetupLocationMarkers() {
        for (marker in meetupLocationMarkers) {
            marker.remove()
        }
        meetupLocationMarkers.clear()
    }

    private fun setUpImeSpacing() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val extraGapPx = 150
            val bottomMargin = imeInsets.bottom + extraGapPx

            val params = binding.predictionsCard.layoutParams as? ConstraintLayout.LayoutParams
            if (params != null && params.bottomMargin != bottomMargin) {
                params.bottomMargin = bottomMargin
                binding.predictionsCard.layoutParams = params
            }

            insets
        }
    }

    private fun setUpInlineSearch() {
        predictionAdapter = PlacePredictionAdapter(emptyList()) { prediction ->
            fetchPlaceDetailsAndApplySelection(prediction)
        }
        binding.predictionsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.predictionsRecyclerView.adapter = predictionAdapter

        binding.searchBarTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (suppressSearchWatcher || BuildConfig.USE_MOCK_DATA) {
                    return
                }

                if (!isSearchBarAtTop) {
                    moveSearchBarToTop()
                }

                // Hide ride results while typing a new query.
                hideRideResultsPanel(clearData = true)
                binding.createRideButton.visibility = View.GONE

                pendingSearchRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
                val query = s?.toString().orEmpty()
                pendingSearchRunnable = Runnable { fetchAutocompletePredictions(query) }
                searchDebounceHandler.postDelayed(pendingSearchRunnable!!, 300)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.searchBarTextView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !BuildConfig.USE_MOCK_DATA) {
                moveSearchBarToTop()
                val query = binding.searchBarTextView.text?.toString().orEmpty().trim()
                if (query.length >= 2) {
                    fetchAutocompletePredictions(query)
                }
            }
        }

        // Handle Enter key to auto-select first autocomplete suggestion
        binding.searchBarTextView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                // Select the first prediction if available
                if (latestPredictions.isNotEmpty()) {
                    fetchPlaceDetailsAndApplySelection(latestPredictions[0])
                }
                true
            } else {
                false
            }
        }
    }

    private fun setUpClickListener() {
        // Search interaction uses placeholder in mock mode or Places in live mode.
        binding.searchBarContainer.setOnClickListener {
            if (BuildConfig.USE_MOCK_DATA) {
                applyPlaceholderPlaceSelection()
            } else {
                activateInlineSearchMode()
            }
        }
        binding.searchBarTextView.setOnClickListener {
            if (BuildConfig.USE_MOCK_DATA) {
                applyPlaceholderPlaceSelection()
            } else {
                activateInlineSearchMode()
            }
        }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 15.dp, bottom = 5.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                hopOnButton(
                    text = "Create ride",
                    onClick = ::handleCreateRideClick
                )
            }
        }
    }

    private fun setUpBottomNavCompose() {
        binding.bottomNav.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.bottomNav.setContent {
            mapsBottomNavigation(onItemSelected = ::handleBottomNavSelection)
        }
    }

    private fun handleBottomNavSelection(item: MapsBottomNavItem) {
        when (mapBottomNavItemToAction(item)) {
            MapsBottomNavAction.NoOp -> Unit
            MapsBottomNavAction.ShowRides -> {
                Toast.makeText(this, getString(R.string.nav_rides), Toast.LENGTH_SHORT).show()
            }
            MapsBottomNavAction.Logout -> {
                logoutAndNavigateToLogin()
            }
        }
    }

    private fun handleCreateRideClick() {
        Toast.makeText(this, getString(R.string.create_ride), Toast.LENGTH_SHORT).show()
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
        // Hide ride results while searching.
        hideRideResultsPanel(clearData = true)
        binding.createRideButton.visibility = View.GONE

        moveSearchBarToTop()
        binding.searchBarTextView.requestFocus()
        binding.searchBarTextView.setSelection(binding.searchBarTextView.text?.length ?: 0)
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.showSoftInput(binding.searchBarTextView, InputMethodManager.SHOW_IMPLICIT)

        val currentText = binding.searchBarTextView.text?.toString().orEmpty()
        if (currentText == getString(R.string.where_to)) {
            suppressSearchWatcher = true
            binding.searchBarTextView.setText("")
            suppressSearchWatcher = false
        }
        binding.predictionsCard.visibility = View.VISIBLE
    }

    private fun fetchAutocompletePredictions(query: String) {
        val client = placesClient ?: return
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) {
            latestPredictions = emptyList()
            predictionAdapter.submitList(emptyList())
            binding.emptyPredictionsTextView.visibility = View.GONE
            return
        }

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(trimmedQuery)
            .build()
        val requestToken = ++latestRequestToken

        client.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val currentQuery = binding.searchBarTextView.text?.toString().orEmpty().trim()
                if (requestToken != latestRequestToken || currentQuery != trimmedQuery) {
                    return@addOnSuccessListener
                }
                latestPredictions = response.autocompletePredictions
                predictionAdapter.submitList(latestPredictions)
                val showEmpty = currentQuery.length >= 2 && latestPredictions.isEmpty()
                binding.emptyPredictionsTextView.visibility = if (showEmpty) View.VISIBLE else View.GONE
                binding.predictionsCard.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                predictionAdapter.submitList(emptyList())
                binding.emptyPredictionsTextView.visibility = View.GONE
            }
    }

    private fun fetchPlaceDetailsAndApplySelection(prediction: AutocompletePrediction) {
        val client = placesClient ?: return
        val placeRequest = FetchPlaceRequest.builder(
            prediction.placeId,
            listOf(Place.Field.LAT_LNG, Place.Field.NAME)
        ).build()

        client.fetchPlace(placeRequest)
            .addOnSuccessListener { response ->
                val latLng = response.place.latLng
                if (latLng == null) {
                    Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                applySelectedPlace(prediction.getFullText(null).toString(), latLng)
                binding.predictionsCard.visibility = View.GONE
                binding.searchBarTextView.clearFocus()
                val imm = getSystemService(InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(binding.searchBarTextView.windowToken, 0)
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyPlaceholderPlaceSelection() {
        val place = placeholderPlaces[placeholderPlaceIndex % placeholderPlaces.size]
        placeholderPlaceIndex++

        applySelectedPlace(place.name, place.latLng)
    }

    private fun applySelectedPlace(name: String, latLng: LatLng) {
        // Update selected destination in UI and map state.
        dropLatLng = latLng
        suppressSearchWatcher = true
        binding.searchBarTextView.setText(name)
        suppressSearchWatcher = false
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
        val rideItems = mockRides
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
                    pickupDistanceMeters = calculatePickupDistanceMeters(ride.meetupLatLng)
                )
            }
            .sortedBy { it.pickupDistanceMeters }

        ridePanelItems = rideItems
        addMeetupLocationMarkers(rideItems)
        return rideItems
    }

    private class PlacePredictionAdapter(
        private var items: List<AutocompletePrediction>,
        private val onClick: (AutocompletePrediction) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<PlacePredictionAdapter.PredictionViewHolder>() {

        fun submitList(newItems: List<AutocompletePrediction>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PredictionViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_place_prediction, parent, false)
            return PredictionViewHolder(view)
        }

        override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
            holder.bind(items[position], onClick)
        }

        override fun getItemCount(): Int = items.size

        class PredictionViewHolder(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val textView: android.widget.TextView = itemView.findViewById(R.id.predictionTextView)

            fun bind(item: AutocompletePrediction, onClick: (AutocompletePrediction) -> Unit) {
                textView.text = item.getFullText(null).toString()
                itemView.setOnClickListener { onClick(item) }
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
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(MapUtils.getDestinationBitmap())
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
        binding.createRideButton.visibility = View.GONE
        hideRideResultsPanel(clearData = true)
        binding.searchBarTextView.setText("")
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
        if (isSearchBarAtTop) {
            return
        }

        // Re-anchor search bar near top when destination is selected.
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
        isSearchBarAtTop = true
    }

    private fun moveSearchBarToBottom() {
        if (!isSearchBarAtTop) {
            return
        }

        // Re-anchor search bar above bottom navigation in idle state.
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
        isSearchBarAtTop = false
    }

    private fun getSearchBarTopMargin(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        return max(ViewUtils.dpToPx(28f), statusBarHeight + ViewUtils.dpToPx(16f))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        isMapReady = true

        val mapStyleApplied = this.googleMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_minimal)
        )
        if (!mapStyleApplied) {
            Log.e(TAG, "Failed to apply minimal map style")
        }

        // Tapping empty map area collapses ride result panel if visible, otherwise restores default UI.
        this.googleMap.setOnMapClickListener {
            if (isRidePanelVisible) {
                collapseRidePanel()
            } else {
                restoreDefaultMapUiIfNeeded()
            }
        }
        this.googleMap.setOnMarkerClickListener {
            false
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
