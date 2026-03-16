package com.tritech.hopon.ui.rideDiscovery.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.hopOnButton
import com.tritech.hopon.ui.components.hopOnComposeTheme
import com.tritech.hopon.ui.rideDiscovery.core.ApiClient
import com.tritech.hopon.ui.rideDiscovery.core.ApiUpdateUserRequest
import com.tritech.hopon.ui.rideDiscovery.core.UsersService
import com.tritech.hopon.utils.SessionManager
import kotlinx.coroutines.launch

class VehicleSettingsActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_VEHICLE_NAME = "extra_vehicle_name"
        const val EXTRA_VEHICLE_COLOR = "extra_vehicle_color"
        const val EXTRA_VEHICLE_PLATE = "extra_vehicle_plate"
        const val EXTRA_CONTACT_INFO = "extra_contact_info"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            hopOnComposeTheme {
                vehicleSettingsScreen(
                    onBackClick = { finish() },
                    onSaved = { name, color, plate, contact ->
                        saveVehicleSettings(name, color, plate, contact)
                    }
                )
            }
        }
    }

    private fun saveVehicleSettings(name: String, color: String, plate: String, contact: String) {
        val userId = SessionManager.getCurrentUserId(this)
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.profile_edit_user_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            runCatching {
                ApiClient.create<UsersService>(this@VehicleSettingsActivity).updateUser(
                    userId,
                    ApiUpdateUserRequest(
                        default_vehicle_name = name,
                        default_vehicle_color = color,
                        default_vehicle_plate = plate,
                        default_contact_info = contact
                    )
                )
            }.onSuccess { updated ->
                val savedName = updated.default_vehicle_name.orEmpty().ifBlank { name }
                val savedColor = updated.default_vehicle_color.orEmpty().ifBlank { color }
                val savedPlate = updated.default_vehicle_plate.orEmpty().ifBlank { plate }
                val savedContact = updated.default_contact_info.orEmpty().ifBlank { contact }

                SessionManager.setDefaultVehicleName(this@VehicleSettingsActivity, savedName)
                SessionManager.setDefaultVehicleColor(this@VehicleSettingsActivity, savedColor)
                SessionManager.setDefaultVehiclePlate(this@VehicleSettingsActivity, savedPlate)
                SessionManager.setDefaultContactInfo(this@VehicleSettingsActivity, savedContact)

                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(EXTRA_VEHICLE_NAME, savedName)
                        putExtra(EXTRA_VEHICLE_COLOR, savedColor)
                        putExtra(EXTRA_VEHICLE_PLATE, savedPlate)
                        putExtra(EXTRA_CONTACT_INFO, savedContact)
                    }
                )
                finish()
            }.onFailure {
                Toast.makeText(
                    this@VehicleSettingsActivity,
                    getString(R.string.profile_edit_save_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

@Composable
private fun vehicleSettingsScreen(
    onBackClick: () -> Unit,
    onSaved: (vehicleName: String, vehicleColor: String, vehiclePlate: String, contactInfo: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var vehicleName by remember {
        mutableStateOf(SessionManager.getDefaultVehicleName(context).orEmpty())
    }
    var vehicleColor by remember {
        mutableStateOf(SessionManager.getDefaultVehicleColor(context).orEmpty())
    }
    var vehiclePlate by remember {
        mutableStateOf(SessionManager.getDefaultVehiclePlate(context).orEmpty())
    }
    var contactInfo by remember {
        mutableStateOf(SessionManager.getDefaultContactInfo(context).orEmpty())
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(id = R.string.profile_edit_back),
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                text = stringResource(id = R.string.vehicle_settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = vehicleName,
            onValueChange = { vehicleName = it },
            label = { Text(stringResource(id = R.string.vehicle_settings_name_label)) },
            placeholder = { Text(stringResource(id = R.string.vehicle_settings_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = vehicleColor,
            onValueChange = { vehicleColor = it },
            label = { Text(stringResource(id = R.string.vehicle_settings_color_label)) },
            placeholder = { Text(stringResource(id = R.string.vehicle_settings_color_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = vehiclePlate,
            onValueChange = { input ->
                vehiclePlate = input.trimStart().uppercase().take(12)
            },
            label = { Text(stringResource(id = R.string.vehicle_settings_plate_label)) },
            placeholder = { Text(stringResource(id = R.string.vehicle_settings_plate_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = contactInfo,
            onValueChange = { contactInfo = it },
            label = { Text(stringResource(id = R.string.vehicle_settings_contact_label)) },
            placeholder = { Text(stringResource(id = R.string.vehicle_settings_contact_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        hopOnButton(
            text = stringResource(id = R.string.vehicle_settings_save_action),
            onClick = {
                val normalizedName = vehicleName.trim()
                val normalizedColor = vehicleColor.trim()
                val normalizedPlate = vehiclePlate.trim().uppercase()
                val normalizedContact = contactInfo.trim()
                if (
                    normalizedName.isBlank() ||
                    normalizedColor.isBlank() ||
                    normalizedPlate.isBlank() ||
                    normalizedContact.isBlank()
                ) {
                    errorMessage = context.getString(R.string.vehicle_settings_required_fields)
                    return@hopOnButton
                }
                errorMessage = null
                onSaved(normalizedName, normalizedColor, normalizedPlate, normalizedContact)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )
    }
}
