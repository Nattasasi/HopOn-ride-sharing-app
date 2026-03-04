package com.tritech.hopon.ui.rideDiscovery.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.hopOnButton
import com.tritech.hopon.ui.rideDiscovery.components.placePredictionsPanel
import com.tritech.hopon.ui.rideDiscovery.core.CreateRideSubmission
import com.tritech.hopon.ui.rideDiscovery.core.RideDateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun createRideScreen(
    initialMeetupLocation: String,
    initialMeetupLatLng: LatLng?,
    initialDestination: String,
    initialDestinationLatLng: LatLng?,
    meetupLocation: String,
    meetupLatLng: LatLng?,
    destination: String,
    destinationLatLng: LatLng?,
    locationPredictions: List<AutocompletePrediction>,
    showLocationPredictions: Boolean,
    onMeetupLocationChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onMeetupFocusChanged: (Boolean) -> Unit,
    onDestinationFocusChanged: (Boolean) -> Unit,
    onMeetupSearchAction: () -> Unit,
    onDestinationSearchAction: () -> Unit,
    onMeetupClearClick: () -> Unit,
    onDestinationClearClick: () -> Unit,
    onPredictionClick: (AutocompletePrediction) -> Unit,
    onDismissLocationOverlay: () -> Unit,
    onBackClick: () -> Unit,
    onCreateRideClick: (CreateRideSubmission) -> Unit
) {
    val primaryColor = colorResource(id = R.color.colorPrimary)
    val neutralIconColor = colorResource(id = R.color.colorPrimaryDark)
    val defaultVehicleInfo = stringResource(id = R.string.default_vehicle_info)
    val defaultContactInfo = stringResource(id = R.string.default_contact_info)
    val defaultWaitTime = stringResource(id = R.string.create_ride_default_wait_time)
    val currentLocationFallback = stringResource(id = R.string.current_location)
    val selectedPlaceFallback = stringResource(id = R.string.selected_place)

    var date by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }
    var waitTime by rememberSaveable(defaultWaitTime) { mutableStateOf(defaultWaitTime) }
    var maxPeople by rememberSaveable { mutableStateOf(4) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showMaxPeopleMenu by remember { mutableStateOf(false) }
    var pricePerSeat by rememberSaveable { mutableStateOf("0") }
    var showVehicleMenu by remember { mutableStateOf(false) }
    var showContactMenu by remember { mutableStateOf(false) }
    var vehiclePlate by rememberSaveable { mutableStateOf("") }
    var vehicleInfo by rememberSaveable(defaultVehicleInfo) { mutableStateOf(defaultVehicleInfo) }
    var contactInfo by rememberSaveable(defaultContactInfo) { mutableStateOf(defaultContactInfo) }
    var notes by rememberSaveable { mutableStateOf("") }
    var locationBoxX by remember { mutableIntStateOf(0) }
    var locationBoxBottomY by remember { mutableIntStateOf(0) }
    var locationBoxWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val locationBoxWidthDp = with(density) { locationBoxWidthPx.toDp() }
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    var wasImeVisible by remember { mutableStateOf(false) }
    val vehicleOptions = listOf(defaultVehicleInfo)
    val contactOptions = listOf(defaultContactInfo)

    fun dismissEditingUi() {
        focusManager.clearFocus(force = true)
        onDismissLocationOverlay()
    }

    LaunchedEffect(imeVisible, showLocationPredictions) {
        if (showLocationPredictions && wasImeVisible && !imeVisible) {
            focusManager.clearFocus(force = true)
            onDismissLocationOverlay()
        }
        wasImeVisible = imeVisible
    }

    BackHandler(enabled = showLocationPredictions || imeVisible) {
        dismissEditingUi()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
            ) {
                IconButton(
                    onClick = {
                        if (showLocationPredictions || imeVisible) {
                            dismissEditingUi()
                        } else {
                            onBackClick()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(id = R.string.create_ride_back),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(id = R.string.create_ride_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            createRideLocationInputs(
                meetupLocation = meetupLocation,
                destination = destination,
                onMeetupLocationChange = onMeetupLocationChange,
                onDestinationChange = onDestinationChange,
                onMeetupFocusChanged = onMeetupFocusChanged,
                onDestinationFocusChanged = onDestinationFocusChanged,
                onMeetupSearchAction = onMeetupSearchAction,
                onDestinationSearchAction = onDestinationSearchAction,
                onMeetupClearClick = onMeetupClearClick,
                onDestinationClearClick = onDestinationClearClick,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    locationBoxX = bounds.left.toInt()
                    locationBoxBottomY = bounds.bottom.toInt()
                    locationBoxWidthPx = bounds.width.toInt()
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { showDatePicker = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = stringResource(id = R.string.create_ride_date_label),
                        tint = neutralIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = date.ifBlank { stringResource(id = R.string.create_ride_today_label) },
                        color = primaryColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    modifier = Modifier.clickable { showTimePicker = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = stringResource(id = R.string.create_ride_time_label),
                        tint = neutralIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = time.ifBlank { stringResource(id = R.string.create_ride_default_time_label) },
                        color = primaryColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = waitTime,
                        onValueChange = { input ->
                            waitTime = input.filter { it.isDigit() }.take(2)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        textStyle = MaterialTheme.typography.titleMedium,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = primaryColor,
                            unfocusedTextColor = primaryColor,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = primaryColor
                        ),
                        modifier = Modifier.width(48.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.create_ride_wait_min_suffix),
                        color = primaryColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    modifier = Modifier.clickable { showMaxPeopleMenu = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = stringResource(id = R.string.create_ride_max_people_label),
                        tint = neutralIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = maxPeople.toString(),
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(id = R.string.create_ride_max_people_label),
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                    DropdownMenu(
                        expanded = showMaxPeopleMenu,
                        onDismissRequest = { showMaxPeopleMenu = false }
                    ) {
                        (1..12).forEach { count ->
                            DropdownMenuItem(
                                text = { Text(count.toString()) },
                                onClick = {
                                    maxPeople = count
                                    showMaxPeopleMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.create_ride_price_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = neutralIconColor
                )
                Text(
                    text = stringResource(id = R.string.create_ride_price_suffix),
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryColor
                )
                TextField(
                    value = pricePerSeat,
                    onValueChange = { input ->
                        pricePerSeat = input.filter { it.isDigit() || it == '.' }.take(6)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.clearFocus() }
                    ),
                    textStyle = MaterialTheme.typography.titleMedium,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = primaryColor,
                        unfocusedTextColor = primaryColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = primaryColor
                    ),
                    modifier = Modifier.width(72.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                createRideInfoDisplay(
                    label = stringResource(id = R.string.vehicle_label),
                    value = vehicleInfo,
                    expanded = showVehicleMenu,
                    options = vehicleOptions,
                    onExpandedChange = { showVehicleMenu = it },
                    onOptionSelected = { selected ->
                        vehicleInfo = selected
                        showVehicleMenu = false
                    },
                    modifier = Modifier.weight(1f)
                )
                createRideInfoDisplay(
                    label = stringResource(id = R.string.contact_label),
                    value = contactInfo,
                    expanded = showContactMenu,
                    options = contactOptions,
                    onExpandedChange = { showContactMenu = it },
                    onOptionSelected = { selected ->
                        contactInfo = selected
                        showContactMenu = false
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = stringResource(id = R.string.create_ride_vehicle_plate_label),
                style = MaterialTheme.typography.titleMedium,
                color = colorResource(id = R.color.colorPrimaryDark),
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
            TextField(
                value = vehiclePlate,
                onValueChange = { input ->
                    vehiclePlate = input.uppercase().take(12)
                },
                placeholder = { Text(stringResource(id = R.string.create_ride_vehicle_plate_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = colorResource(id = R.color.light_grey),
                    unfocusedContainerColor = colorResource(id = R.color.light_grey),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = primaryColor
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(id = R.string.create_ride_notes_short_label),
                style = MaterialTheme.typography.titleMedium,
                color = colorResource(id = R.color.colorPrimaryDark),
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            TextField(
                value = notes,
                onValueChange = { notes = it },
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = colorResource(id = R.color.light_grey),
                    unfocusedContainerColor = colorResource(id = R.color.light_grey),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = primaryColor
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
            )

            Spacer(modifier = Modifier.height(96.dp))
        }

        hopOnButton(
            text = stringResource(id = R.string.create_ride_action),
            onClick = {
                val effectiveInitialMeetup = initialMeetupLocation.ifBlank { currentLocationFallback }
                val effectiveInitialDestination = initialDestination.ifBlank { selectedPlaceFallback }
                val normalizedMeetup = meetupLocation.trim().ifEmpty { effectiveInitialMeetup }
                val normalizedDestination = destination.trim().ifEmpty { effectiveInitialDestination }
                val resolvedMeetupLatLng = if (meetupLocation.trim().isEmpty()) {
                    initialMeetupLatLng ?: meetupLatLng
                } else {
                    meetupLatLng
                }
                val resolvedDestinationLatLng = if (destination.trim().isEmpty()) {
                    initialDestinationLatLng ?: destinationLatLng
                } else {
                    destinationLatLng
                }
                val normalizedDate = date.trim()
                val normalizedTime = time.trim()
                val parsedWaitTime = waitTime.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
                val parsedMaxPeople = maxPeople.coerceAtLeast(1)
                val normalizedVehicleInfo = vehicleInfo.trim().ifEmpty { defaultVehicleInfo }
                val normalizedContactInfo = contactInfo.trim().ifEmpty { defaultContactInfo }
                val normalizedNotes = notes.trim()

                val parsedPrice = pricePerSeat.trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                onCreateRideClick(
                    CreateRideSubmission(
                        meetupLocation = normalizedMeetup,
                        meetupLatLng = resolvedMeetupLatLng,
                        destination = normalizedDestination,
                        destinationLatLng = resolvedDestinationLatLng,
                        meetupDate = normalizedDate,
                        meetupTime = normalizedTime,
                        waitTimeMinutes = parsedWaitTime,
                        maxPeopleCount = parsedMaxPeople,
                        pricePerSeat = parsedPrice,
                        vehiclePlate = vehiclePlate.trim(),
                        vehicleInfo = normalizedVehicleInfo,
                        contactInfo = normalizedContactInfo,
                        additionalNotes = normalizedNotes
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }

    if (showLocationPredictions && locationBoxWidthPx > 0) {
        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(locationBoxX, locationBoxBottomY)
        ) {
            Card(
                modifier = Modifier
                    .width(locationBoxWidthDp)
                    .height(220.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                placePredictionsPanel(
                    predictions = locationPredictions,
                    showEmptyState = locationPredictions.isEmpty(),
                    onPredictionClick = { prediction ->
                        dismissEditingUi()
                        onPredictionClick(prediction)
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = millis
                        }
                        date = RideDateTimeFormatter.canonicalDateLabelForNow(calendar)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }
                    time = RideDateTimeFormatter.canonicalTimeLabelForNow(calendar)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@Composable
private fun createRideLocationInputs(
    meetupLocation: String,
    destination: String,
    onMeetupLocationChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onMeetupFocusChanged: (Boolean) -> Unit,
    onDestinationFocusChanged: (Boolean) -> Unit,
    onMeetupSearchAction: () -> Unit,
    onDestinationSearchAction: () -> Unit,
    onMeetupClearClick: () -> Unit,
    onDestinationClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = colorResource(id = R.color.colorPrimary)
    val dividerColor = colorResource(id = R.color.light_grey)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = dividerColor, shape = MaterialTheme.shapes.medium)
            .background(Color.White, shape = MaterialTheme.shapes.medium)
    ) {
        createRideLocationField(
            value = meetupLocation,
            label = stringResource(id = R.string.meetup_location_label),
            primaryColor = primaryColor,
            onValueChange = onMeetupLocationChange,
            onFocusChanged = onMeetupFocusChanged,
            onSearchAction = onMeetupSearchAction,
            onClearClick = onMeetupClearClick
        )

        HorizontalDivider(
            color = dividerColor,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )

        createRideLocationField(
            value = destination,
            label = stringResource(id = R.string.destination_label),
            primaryColor = primaryColor,
            onValueChange = onDestinationChange,
            onFocusChanged = onDestinationFocusChanged,
            onSearchAction = onDestinationSearchAction,
            onClearClick = onDestinationClearClick
        )
    }
}

@Composable
private fun createRideLocationField(
    value: String,
    label: String,
    primaryColor: Color,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSearchAction: () -> Unit,
    onClearClick: () -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, color = primaryColor) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { onSearchAction() },
            onDone = { onSearchAction() }
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = primaryColor),
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.close),
                        tint = primaryColor
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedTextColor = primaryColor,
            unfocusedTextColor = primaryColor,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedLabelColor = primaryColor,
            unfocusedLabelColor = primaryColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = primaryColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .onFocusChanged { onFocusChanged(it.isFocused) }
    )
}

@Composable
private fun createRideInfoDisplay(
    label: String,
    value: String,
    expanded: Boolean,
    options: List<String>,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = colorResource(id = R.color.colorPrimary)
    val subtleLabelColor = colorResource(id = R.color.colorPrimaryDark)

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = subtleLabelColor.copy(alpha = 0.55f)
        )
        Box {
            Row(
            modifier = Modifier
                .fillMaxWidth()
                    .padding(top = 2.dp)
                    .clickable { onExpandedChange(true) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifBlank { stringResource(id = R.string.default_vehicle_info) },
                    style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = label,
                tint = primaryColor,
                modifier = Modifier.size(24.dp)
            )
        }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onOptionSelected(option) }
                    )
                }
            }
        }
    }
}
