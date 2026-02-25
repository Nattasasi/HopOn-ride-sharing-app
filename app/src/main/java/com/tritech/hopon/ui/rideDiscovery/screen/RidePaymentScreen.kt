package com.tritech.hopon.ui.rideDiscovery.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.hopOnButton
import com.tritech.hopon.ui.components.statusBadge
import com.tritech.hopon.utils.MapUtils

private enum class PaymentStatus {
    WAITING,
    APPROVED,
    DECLINED
}

private const val PaymentMapCameraPaddingPx = 120

@Composable
fun ridePaymentScreen(
    isHost: Boolean,
    hostName: String,
    passengerNames: List<String>,
    fareBaht: Int,
    durationMinutes: Int,
    distanceMiles: Double,
    currentLocationLatLng: LatLng?,
    meetupLatLng: LatLng?,
    destinationLatLng: LatLng?,
    pickupRoutePoints: List<LatLng>,
    rideRoutePoints: List<LatLng>,
    onHostFinishClick: () -> Unit,
    onPassengerConfirmClick: () -> Unit
) {
    val paymentStatuses = remember(passengerNames) {
        mutableStateMapOf<String, PaymentStatus>().apply {
            passengerNames.forEach { name ->
                this[name] = PaymentStatus.WAITING
            }
        }
    }
    var rating by remember { mutableStateOf(4) }
    var comment by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = colorResource(id = R.color.colorPrimary),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(56.dp)
                    .padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(id = R.string.ride_payment_arrived_message),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 14.dp),
                textAlign = TextAlign.Center
            )

            paymentSummaryCard(
                fareBaht = fareBaht,
                durationMinutes = durationMinutes,
                distanceMiles = distanceMiles,
                currentLocationLatLng = currentLocationLatLng,
                meetupLatLng = meetupLatLng,
                destinationLatLng = destinationLatLng,
                pickupRoutePoints = pickupRoutePoints,
                rideRoutePoints = rideRoutePoints
            )

            if (isHost) {
                hostPaymentSection(
                    passengerNames = passengerNames,
                    fareBaht = fareBaht,
                    paymentStatuses = paymentStatuses
                )
                Spacer(modifier = Modifier.height(14.dp))
                hostEarningCard(
                    fareBaht = fareBaht,
                    paymentStatuses = paymentStatuses
                )
            } else {
                passengerPaymentSection(
                    hostName = hostName,
                    rating = rating,
                    onRatingChange = { rating = it },
                    comment = comment,
                    onCommentChange = { comment = it }
                )
            }

            Spacer(modifier = Modifier.height(88.dp))
        }

        hopOnButton(
            text = if (isHost) {
                stringResource(id = R.string.ride_payment_finish_host)
            } else {
                stringResource(id = R.string.ride_payment_confirm_passenger)
            },
            onClick = if (isHost) onHostFinishClick else onPassengerConfirmClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun paymentSummaryCard(
    fareBaht: Int,
    durationMinutes: Int,
    distanceMiles: Double,
    currentLocationLatLng: LatLng?,
    meetupLatLng: LatLng?,
    destinationLatLng: LatLng?,
    pickupRoutePoints: List<LatLng>,
    rideRoutePoints: List<LatLng>
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        paymentRoutePreviewMap(
            currentLocationLatLng = currentLocationLatLng,
            meetupLatLng = meetupLatLng,
            destinationLatLng = destinationLatLng,
            pickupRoutePoints = pickupRoutePoints,
            rideRoutePoints = rideRoutePoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            metricColumn(
                title = stringResource(id = R.string.ride_payment_fare_label),
                value = stringResource(id = R.string.ride_payment_fare_value_format, fareBaht)
            )
            metricColumn(
                title = stringResource(id = R.string.ride_payment_duration_label),
                value = stringResource(id = R.string.ride_payment_duration_value_format, durationMinutes)
            )
            metricColumn(
                title = stringResource(id = R.string.ride_payment_distance_label),
                value = stringResource(id = R.string.ride_payment_distance_value_format, distanceMiles)
            )
        }
    }
}

@Composable
private fun paymentRoutePreviewMap(
    currentLocationLatLng: LatLng?,
    meetupLatLng: LatLng?,
    destinationLatLng: LatLng?,
    pickupRoutePoints: List<LatLng>,
    rideRoutePoints: List<LatLng>,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_VARIABLE")
    val unusedPickupRoutePoints = pickupRoutePoints
    @Suppress("UNUSED_VARIABLE")
    val unusedRideRoutePoints = rideRoutePoints

    val context = LocalContext.current
    val mapStyle = remember(context) {
        runCatching {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_minimal)
        }.getOrNull()
    }
    val cameraPositionState = rememberCameraPositionState()
    val destinationIcon = remember(context) {
        BitmapDescriptorFactory.fromBitmap(
            MapUtils.getLocationIconBitmap(
                context,
                R.drawable.ic_target,
                R.color.colorAccent,
                sizePx = 72
            )
        )
    }
    val fallbackCameraPoint = destinationLatLng ?: currentLocationLatLng ?: meetupLatLng ?: LatLng(13.7370, 100.6270)
    var isMapLoaded by remember { mutableStateOf(false) }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false,
            mapStyleOptions = mapStyle
        ),
        onMapLoaded = { isMapLoaded = true }
    ) {
        destinationLatLng?.let { destination ->
            paymentMarker(
                position = destination,
                title = stringResource(id = R.string.destination_label),
                icon = destinationIcon
            )
        }
    }

    LaunchedEffect(isMapLoaded, destinationLatLng, fallbackCameraPoint) {
        if (!isMapLoaded) return@LaunchedEffect

        if (destinationLatLng != null) {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(destinationLatLng, 12f)
            )
        } else {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(fallbackCameraPoint, 13f)
            )
        }
    }
}

@Composable
private fun paymentMarker(
    position: LatLng,
    title: String,
    icon: BitmapDescriptor
) {
    Marker(
        state = MarkerState(position = position),
        title = title,
        icon = icon
    )
}

@Composable
private fun metricColumn(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun hostPaymentSection(
    passengerNames: List<String>,
    fareBaht: Int,
    paymentStatuses: MutableMap<String, PaymentStatus>
) {
    Text(
        text = stringResource(id = R.string.ride_payment_passenger_payments),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )

    if (passengerNames.isEmpty()) {
        Text(
            text = stringResource(id = R.string.ride_payment_no_passengers),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        passengerNames.forEach { name ->
            val status = paymentStatuses[name] ?: PaymentStatus.WAITING
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = colorResource(id = R.color.colorPrimary),
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(id = R.string.ride_payment_fare_amount_inline, fareBaht),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorResource(id = R.color.colorPrimary),
                            fontWeight = FontWeight.SemiBold
                        )
                        paymentStatusChip(status = status)
                    }

                    if (status == PaymentStatus.WAITING) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            paymentActionButton(
                                text = stringResource(id = R.string.ride_payment_approve),
                                onClick = { paymentStatuses[name] = PaymentStatus.APPROVED },
                                modifier = Modifier.weight(1f),
                                isPrimary = true
                            )
                            paymentActionButton(
                                text = stringResource(id = R.string.ride_payment_decline),
                                onClick = { paymentStatuses[name] = PaymentStatus.DECLINED },
                                modifier = Modifier.weight(1f),
                                isPrimary = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun paymentStatusChip(status: PaymentStatus) {
    val (label, bgColor, textColor) = when (status) {
        PaymentStatus.WAITING -> Triple(
            R.string.ride_payment_status_waiting,
            colorResource(id = R.color.colorAccent).copy(alpha = 0.18f),
            colorResource(id = R.color.colorAccent)
        )

        PaymentStatus.APPROVED -> Triple(
            R.string.ride_payment_status_approved,
            Color(0xFFD3F5DF),
            Color(0xFF0B8B4B)
        )

        PaymentStatus.DECLINED -> Triple(
            R.string.ride_payment_status_declined,
            Color(0xFFF7D8D8),
            Color(0xFFB43A3A)
        )
    }

    statusBadge(
        text = stringResource(id = label),
        backgroundColor = bgColor,
        textColor = textColor
    )
}

@Composable
private fun hostEarningCard(
    fareBaht: Int,
    paymentStatuses: Map<String, PaymentStatus>
) {
    val passengerCount = paymentStatuses.size
    val totalEarnings = fareBaht * passengerCount

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = stringResource(id = R.string.ride_payment_est_net_earning),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(id = R.string.ride_payment_baht_value_format, totalEarnings),
                    color = colorResource(id = R.color.colorPrimary),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun paymentActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean
) {
    val containerColor = if (isPrimary) {
        colorResource(id = R.color.colorPrimary)
    } else {
        Color.White
    }
    val contentColor = if (isPrimary) {
        Color.White
    } else {
        colorResource(id = R.color.colorPrimaryDark)
    }
    val borderColor = if (isPrimary) {
        Color.Transparent
    } else {
        colorResource(id = R.color.colorPrimaryDark)
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun passengerPaymentSection(
    hostName: String,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    comment: String,
    onCommentChange: (String) -> Unit
) {
    Text(
        text = stringResource(id = R.string.ride_payment_rate_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        textAlign = TextAlign.Center
    )
    Text(
        text = stringResource(id = R.string.ride_payment_rate_subtitle_format, hostName),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        textAlign = TextAlign.Center
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(5) { index ->
            val starIndex = index + 1
            Icon(
                imageVector = if (starIndex <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = colorResource(id = R.color.colorAccent),
                modifier = Modifier
                    .size(36.dp)
                    .padding(horizontal = 3.dp)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        AssistChip(
            onClick = { onRatingChange(5) },
            label = { Text(text = stringResource(id = R.string.ride_payment_chip_clean_car)) },
            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
        )
        Spacer(modifier = Modifier.size(8.dp))
        AssistChip(
            onClick = { onRatingChange(5) },
            label = { Text(text = stringResource(id = R.string.ride_payment_chip_smooth_driving)) },
            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
        )
    }

    OutlinedTextField(
        value = comment,
        onValueChange = onCommentChange,
        placeholder = { Text(text = stringResource(id = R.string.ride_payment_comment_hint)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(110.dp),
        shape = RoundedCornerShape(18.dp)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.AddPhotoAlternate,
                contentDescription = null,
                tint = colorResource(id = R.color.colorPrimary),
                modifier = Modifier.size(42.dp)
            )
            Text(
                text = stringResource(id = R.string.ride_payment_attach_slip_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = stringResource(id = R.string.ride_payment_attach_slip_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}