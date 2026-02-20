package com.tritech.hopon.ui.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.transition.TransitionManager
import com.google.android.gms.common.api.Status
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.tritech.hopon.BuildConfig
import com.tritech.hopon.R
import com.tritech.hopon.data.network.NetworkService
import com.tritech.hopon.databinding.ActivityMapsBinding
import com.tritech.hopon.ui.auth.LoginActivity
import com.tritech.hopon.utils.AnimationUtils
import com.tritech.hopon.utils.MapUtils
import com.tritech.hopon.utils.PermissionUtils
import com.tritech.hopon.utils.SessionManager
import com.tritech.hopon.utils.ViewUtils

class MapsActivity : AppCompatActivity(), MapsView, OnMapReadyCallback {

    companion object {
        private const val TAG = "MapsActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 999
        private const val PLACE_REQUEST_CODE = 1
    }

    private data class PlaceholderPlace(val name: String, val latLng: LatLng)

    private lateinit var binding: ActivityMapsBinding
    private lateinit var googleMap: GoogleMap
    private var isMapReady = false
    private var presenter: MapsPresenter? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private var autocompleteLauncher: ActivityResultLauncher<Intent>? = null
    private var pendingAutocompleteRequestCode: Int? = null

    private var currentLatLng: LatLng? = null
    private var pickUpLatLng: LatLng? = null
    private var dropLatLng: LatLng? = null

    private val nearbyCabMarkerList = arrayListOf<Marker>()
    private var destinationMarker: Marker? = null
    private var originMarker: Marker? = null
    private var greyPolyLine: Polyline? = null
    private var blackPolyline: Polyline? = null
    private var previousLatLngFromServer: LatLng? = null
    private var currentLatLngFromServer: LatLng? = null
    private var movingCabMarker: Marker? = null
    private var isSearchBarAtTop = false

    private val placeholderPlaces = listOf(
        PlaceholderPlace("Siam Paragon", LatLng(13.7466, 100.5347)),
        PlaceholderPlace("CentralWorld", LatLng(13.7460, 100.5395)),
        PlaceholderPlace("Terminal 21", LatLng(13.7373, 100.5607))
    )
    private var placeholderPlaceIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SessionManager.isLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewUtils.enableTransparentStatusBar(window)
        setUpBackPressHandler()

        if (!BuildConfig.USE_MOCK_DATA) {
            autocompleteLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                handleAutocompleteResult(result.resultCode, result.data)
            }
            presenter = MapsPresenter(NetworkService())
            presenter?.onAttach(this)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setUpClickListener()
    }

    private fun setUpBackPressHandler() {
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
        if (binding.resultPopupCard.visibility != View.VISIBLE && !isSearchBarAtTop) {
            return false
        }

        binding.resultPopupCard.visibility = View.GONE
        binding.createRideButton.visibility = View.GONE
        binding.resultPlaceTextView.text = ""
        binding.searchBarTextView.text = getString(R.string.where_to)
        moveSearchBarToBottom()
        return true
    }

    private fun setUpClickListener() {
        binding.searchBarContainer.setOnClickListener {
            if (BuildConfig.USE_MOCK_DATA) {
                applyPlaceholderPlaceSelection()
            } else {
                launchLocationAutoCompleteActivity()
            }
        }
        binding.searchBarTextView.setOnClickListener {
            if (BuildConfig.USE_MOCK_DATA) {
                applyPlaceholderPlaceSelection()
            } else {
                launchLocationAutoCompleteActivity()
            }
        }
        binding.createRideButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.create_ride), Toast.LENGTH_SHORT).show()
        }
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_rides -> {
                    Toast.makeText(this, getString(R.string.nav_rides), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    logoutAndNavigateToLogin()
                    true
                }
                else -> false
            }
        }
    }

    private fun logoutAndNavigateToLogin() {
        SessionManager.setLoggedIn(this, false)
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun launchLocationAutoCompleteActivity() {
        val fields: List<Place.Field> =
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .build(this)
        pendingAutocompleteRequestCode = PLACE_REQUEST_CODE
        autocompleteLauncher?.launch(intent)
    }

    private fun handleAutocompleteResult(resultCode: Int, data: Intent?) {
        val requestCode = pendingAutocompleteRequestCode ?: return
        pendingAutocompleteRequestCode = null
        if (requestCode != PLACE_REQUEST_CODE) {
            return
        }
        when (resultCode) {
            RESULT_OK -> {
                if (data == null) {
                    return
                }
                val place = Autocomplete.getPlaceFromIntent(data)
                Log.d(TAG, "Place: " + place.name + ", " + place.id + ", " + place.latLng)
                if (place.latLng != null) {
                    applySelectedPlace(place.name ?: getString(R.string.where_to), place.latLng!!)
                }
            }

            AutocompleteActivity.RESULT_ERROR -> {
                if (data == null) {
                    Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_LONG).show()
                    return
                }
                val status: Status = Autocomplete.getStatusFromIntent(data)
                val statusMessage = status.statusMessage ?: getString(R.string.generic_error)
                Log.e(TAG, "Places autocomplete error: code=${status.statusCode}, message=$statusMessage")
                Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
            }

            RESULT_CANCELED -> {
                Log.d(TAG, "Place Selection Canceled")
            }
        }
    }

    private fun applyPlaceholderPlaceSelection() {
        val place = placeholderPlaces[placeholderPlaceIndex % placeholderPlaces.size]
        placeholderPlaceIndex++

        applySelectedPlace(place.name, place.latLng)
    }

    private fun applySelectedPlace(name: String, latLng: LatLng) {
        dropLatLng = latLng
        binding.searchBarTextView.text = name
        binding.resultPlaceTextView.text = name
        moveSearchBarToTop()
        binding.resultPopupCard.visibility = View.VISIBLE
        binding.createRideButton.visibility = View.VISIBLE

        if (!isMapReady) {
            Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
            return
        }

        destinationMarker?.remove()
        destinationMarker = addOriginDestinationMarkerAndGet(latLng)
        destinationMarker?.setAnchor(0.5f, 0.5f)
        animateCamera(latLng)
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
                if (currentLatLng != null) {
                    return
                }

                for (location in locationResult.locations) {
                    if (currentLatLng == null) {
                        currentLatLng = LatLng(location.latitude, location.longitude)
                        setCurrentLocationAsPickUp()
                        enableMyLocationOnMap()
                        moveCamera(currentLatLng!!)
                        animateCamera(currentLatLng!!)
                        if (BuildConfig.USE_MOCK_DATA) {
                            showNearbyCabs(generateMockNearbyCabs(currentLatLng!!))
                        } else {
                            presenter?.requestNearbyCabs(currentLatLng!!)
                        }
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

    private fun generateMockNearbyCabs(center: LatLng): List<LatLng> {
        return listOf(
            LatLng(center.latitude + 0.0012, center.longitude + 0.0011),
            LatLng(center.latitude - 0.0010, center.longitude + 0.0015),
            LatLng(center.latitude + 0.0008, center.longitude - 0.0014)
        )
    }

    private fun reset() {
        binding.createRideButton.visibility = View.GONE
        binding.resultPopupCard.visibility = View.GONE
        binding.resultPlaceTextView.text = ""
        binding.searchBarTextView.text = getString(R.string.where_to)
        moveSearchBarToBottom()

        nearbyCabMarkerList.forEach { it.remove() }
        nearbyCabMarkerList.clear()
        previousLatLngFromServer = null
        currentLatLngFromServer = null

        if (currentLatLng != null) {
            moveCamera(currentLatLng!!)
            animateCamera(currentLatLng!!)
            setCurrentLocationAsPickUp()
            if (BuildConfig.USE_MOCK_DATA) {
                showNearbyCabs(generateMockNearbyCabs(currentLatLng!!))
            } else {
                presenter?.requestNearbyCabs(currentLatLng!!)
            }
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
        if (currentLatLng == null) {
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

        val root = binding.root as ConstraintLayout
        val constraintSet = ConstraintSet()
        constraintSet.clone(root)
        constraintSet.clear(R.id.searchBarContainer, ConstraintSet.BOTTOM)
        constraintSet.connect(
            R.id.searchBarContainer,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            ViewUtils.dpToPx(56f)
        )
        TransitionManager.beginDelayedTransition(root)
        constraintSet.applyTo(root)
        isSearchBarAtTop = true
    }

    private fun moveSearchBarToBottom() {
        if (!isSearchBarAtTop) {
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
        TransitionManager.beginDelayedTransition(root)
        constraintSet.applyTo(root)
        isSearchBarAtTop = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        isMapReady = true
        this.googleMap.setOnMapClickListener {
            restoreDefaultMapUiIfNeeded()
        }
        this.googleMap.setOnMarkerClickListener {
            false
        }
    }

    override fun showNearbyCabs(latLngList: List<LatLng>) {
        nearbyCabMarkerList.forEach { it.remove() }
        nearbyCabMarkerList.clear()
        for (latLng in latLngList) {
            val nearbyCabMarker = addCarMarkerAndGet(latLng)
            nearbyCabMarker?.let { nearbyCabMarkerList.add(it) }
        }
    }

    override fun informCabBooked() {
        Toast.makeText(this, getString(R.string.your_cab_is_booked), Toast.LENGTH_SHORT).show()
    }

    override fun showPath(latLngList: List<LatLng>) {
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

        val polylineAnimator = AnimationUtils.polyLineAnimator()
        polylineAnimator.addUpdateListener { valueAnimator ->
            val percentValue = (valueAnimator.animatedValue as Int)
            val index = (greyPolyLine?.points!!.size * (percentValue / 100.0f)).toInt()
            blackPolyline?.points = greyPolyLine?.points!!.subList(0, index)
        }
        polylineAnimator.start()
    }

    override fun updateCabLocation(latLng: LatLng) {
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
