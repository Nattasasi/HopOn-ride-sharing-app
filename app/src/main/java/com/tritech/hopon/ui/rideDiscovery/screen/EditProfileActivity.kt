package com.tritech.hopon.ui.rideDiscovery.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.hopOnButton
import com.tritech.hopon.ui.components.hopOnComposeTheme
import com.tritech.hopon.ui.rideDiscovery.core.ApiClient
import com.tritech.hopon.ui.rideDiscovery.core.ApiUpdateUserRequest
import com.tritech.hopon.ui.rideDiscovery.core.UsersService
import com.tritech.hopon.utils.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class EditProfileActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_UPDATED_DISPLAY_NAME = "extra_updated_display_name"
        const val EXTRA_UPDATED_PROFILE_PHOTO = "extra_updated_profile_photo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            hopOnComposeTheme {
                editProfileScreen(
                    onBackClick = { finish() },
                    onSaved = { updatedDisplayName, updatedProfilePhoto ->
                        setResult(
                            RESULT_OK,
                            android.content.Intent().apply {
                                putExtra(EXTRA_UPDATED_DISPLAY_NAME, updatedDisplayName)
                                putExtra(EXTRA_UPDATED_PROFILE_PHOTO, updatedProfilePhoto)
                            }
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun editProfileScreen(
    onBackClick: () -> Unit,
    onSaved: (updatedDisplayName: String, updatedProfilePhoto: String?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val usersService = remember { ApiClient.create<UsersService>(context) }
    val primary = colorResource(id = R.color.colorPrimary)
    val gray = colorResource(id = R.color.colorPrimaryDark)

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var photoBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var photoChangedByPicker by remember { mutableStateOf(false) }

    var originalFirstName by remember { mutableStateOf("") }
    var originalLastName by remember { mutableStateOf("") }
    var originalEmail by remember { mutableStateOf("") }
    var originalPhoto by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val encoded = encodeUriToBase64(context, uri)
            if (encoded != null) {
                photoBase64 = encoded
                photoBitmap = decodeBase64ToBitmap(encoded)?.asImageBitmap()
                photoChangedByPicker = true
                successMessage = null
                errorMessage = null
            } else {
                errorMessage = context.getString(R.string.profile_edit_photo_failed)
            }
        }
    }

    LaunchedEffect(Unit) {
        val userId = SessionManager.getCurrentUserId(context)
        if (userId.isNullOrBlank()) {
            errorMessage = context.getString(R.string.profile_edit_user_not_found)
            isLoading = false
            return@LaunchedEffect
        }

        runCatching { usersService.getUser(userId) }
            .onSuccess { user ->
                firstName = user.first_name
                lastName = user.last_name
                email = user.email
                photoBase64 = user.profile_photo
                photoBitmap = decodeBase64ToBitmap(user.profile_photo)?.asImageBitmap()

                originalFirstName = user.first_name
                originalLastName = user.last_name
                originalEmail = user.email
                originalPhoto = user.profile_photo
                photoChangedByPicker = false
            }
            .onFailure {
                errorMessage = context.getString(R.string.profile_edit_load_failed)
            }
        isLoading = false
    }

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
                text = stringResource(id = R.string.profile_personal_information),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(124.dp)
                .align(Alignment.CenterHorizontally)
                .background(color = gray.copy(alpha = 0.28f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = photoBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(id = R.string.profile_edit_photo),
                    modifier = Modifier
                        .size(124.dp)
                        .background(Color.Transparent, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(id = R.string.profile_edit_photo),
                    tint = gray.copy(alpha = 0.9f),
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        hopOnButton(
            text = stringResource(id = R.string.profile_edit_choose_photo),
            onClick = { photoPicker.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            containerColor = primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = primary
            )
        } else {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text(stringResource(id = R.string.register_first_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text(stringResource(id = R.string.register_last_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text(stringResource(id = R.string.profile_edit_current_password_required)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text(stringResource(id = R.string.profile_edit_new_password_optional)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = colorResource(id = R.color.cancelRideRed),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            if (successMessage != null) {
                Text(
                    text = successMessage.orEmpty(),
                    color = primary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            hopOnButton(
                text = if (isSaving) {
                    stringResource(id = R.string.profile_edit_saving)
                } else {
                    stringResource(id = R.string.profile_edit_save_changes)
                },
                onClick = {
                    if (isSaving) return@hopOnButton

                    val trimmedFirstName = firstName.trim()
                    val trimmedLastName = lastName.trim()
                    val trimmedEmail = email.trim()
                    val trimmedCurrentPassword = currentPassword.trim()
                    val trimmedNewPassword = newPassword.trim()

                    val hasRemoteProfileChanges = trimmedFirstName != originalFirstName ||
                        trimmedLastName != originalLastName ||
                        trimmedEmail != originalEmail ||
                        photoChangedByPicker ||
                        photoBase64 != originalPhoto
                    val hasPasswordChange = trimmedNewPassword.isNotEmpty()

                    when {
                        !hasRemoteProfileChanges && !hasPasswordChange -> {
                            successMessage = context.getString(R.string.profile_edit_no_changes)
                            errorMessage = null
                            return@hopOnButton
                        }
                        trimmedCurrentPassword.isEmpty() -> {
                            errorMessage = context.getString(R.string.profile_edit_current_password_required_error)
                            successMessage = null
                            return@hopOnButton
                        }
                        hasPasswordChange && trimmedNewPassword.length < 6 -> {
                            errorMessage = context.getString(R.string.profile_edit_password_too_short)
                            successMessage = null
                            return@hopOnButton
                        }
                    }

                    val userId = SessionManager.getCurrentUserId(context)
                    if (userId.isNullOrBlank()) {
                        errorMessage = context.getString(R.string.profile_edit_user_not_found)
                        successMessage = null
                        return@hopOnButton
                    }

                    val request = ApiUpdateUserRequest(
                        first_name = trimmedFirstName.takeIf { hasRemoteProfileChanges && it != originalFirstName },
                        last_name = trimmedLastName.takeIf { hasRemoteProfileChanges && it != originalLastName },
                        email = trimmedEmail.takeIf { hasRemoteProfileChanges && it != originalEmail },
                        profile_photo = photoBase64.takeIf {
                            hasRemoteProfileChanges && (photoChangedByPicker || it != originalPhoto)
                        },
                        password = trimmedNewPassword.takeIf { hasPasswordChange },
                        current_password = trimmedCurrentPassword
                    )

                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        successMessage = null

                        runCatching { usersService.updateUser(userId, request) }
                            .onSuccess { updated ->
                                val displayName = "${updated.first_name} ${updated.last_name}".trim()
                                SessionManager.setDisplayName(context, displayName)

                                originalFirstName = updated.first_name
                                originalLastName = updated.last_name
                                originalEmail = updated.email
                                originalPhoto = updated.profile_photo
                                photoChangedByPicker = false

                                firstName = updated.first_name
                                lastName = updated.last_name
                                email = updated.email
                                photoBase64 = updated.profile_photo
                                photoBitmap = decodeBase64ToBitmap(updated.profile_photo)?.asImageBitmap()
                                currentPassword = ""
                                newPassword = ""

                                successMessage = context.getString(R.string.profile_edit_saved)
                                onSaved(displayName, updated.profile_photo)
                            }
                            .onFailure { err ->
                                errorMessage = extractApiErrorMessage(err)
                                    ?: err.message
                                    ?: context.getString(R.string.profile_edit_save_failed)
                            }
                        isSaving = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                containerColor = primary
            )
        }
    }
}

private fun decodeBase64ToBitmap(base64: String?): Bitmap? {
    if (base64.isNullOrBlank()) return null
    return runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun encodeUriToBase64(context: Context, uri: Uri): String? {
    return runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: return null

        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val resized = resizeBitmapMaintainingRatio(original, 640)
        val output = java.io.ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, output)
        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }.getOrNull()
}

private fun resizeBitmapMaintainingRatio(bitmap: Bitmap, maxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxSize && height <= maxSize) return bitmap

    val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

private fun extractApiErrorMessage(error: Throwable): String? {
    val httpError = error as? HttpException ?: return null
    val raw = runCatching { httpError.response()?.errorBody()?.string() }.getOrNull() ?: return null
    return runCatching {
        val json = JSONObject(raw)
        val direct = json.optString("message").takeIf { it.isNotBlank() }
        if (direct != null) return@runCatching direct

        val errors = json.optJSONArray("errors")
        if (errors != null && errors.length() > 0) {
            errors.optJSONObject(0)?.optString("msg")?.takeIf { it.isNotBlank() }
        } else {
            null
        }
    }.getOrNull()
}
