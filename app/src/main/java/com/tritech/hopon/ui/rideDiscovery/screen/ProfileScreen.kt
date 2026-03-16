package com.tritech.hopon.ui.rideDiscovery.screen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.hopOnButton

@Composable
fun profileScreen(
    userName: String,
    profilePhotoBase64: String?,
    verificationStatus: String?,
    onPersonalInformationClick: () -> Unit,
    onVerificationClick: () -> Unit,
    onIssueReportsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val primary = colorResource(id = R.color.colorPrimary)
    val gray = colorResource(id = R.color.colorPrimaryDark)
    val personalInfoLabel = stringResource(id = R.string.profile_personal_information)
    val verificationLabel = stringResource(id = R.string.profile_verification_safety)
    val issueReportsLabel = stringResource(id = R.string.report_status_title)

    val profileImageBitmap = remember(profilePhotoBase64) {
        decodeBase64ToBitmap(profilePhotoBase64)?.asImageBitmap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.profile_setting_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth(),
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 26.dp)
                .size(148.dp)
                .align(Alignment.CenterHorizontally)
                .background(color = gray.copy(alpha = 0.38f), shape = CircleShape)
        ) {
            if (profileImageBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = profileImageBitmap,
                    contentDescription = userName,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 14.dp)
        )

        Text(
            text = verificationStatusDisplayLabel(verificationStatus),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = primary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 6.dp)
        )

        Text(
            text = stringResource(id = R.string.profile_account_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = gray,
            modifier = Modifier.padding(top = 24.dp)
        )

        settingGroupCard(
            rows = listOf(
                SettingRowItem(
                    label = personalInfoLabel,
                    leadingIcon = Icons.Default.Person
                )
            ),
            primaryTint = primary,
            secondaryTint = gray,
            onRowClick = { row ->
                if (row.label == personalInfoLabel) onPersonalInformationClick()
            },
            modifier = Modifier.padding(top = 10.dp)
        )

        Text(
            text = verificationLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = gray,
            modifier = Modifier.padding(top = 22.dp)
        )

        settingGroupCard(
            rows = listOf(
                SettingRowItem(
                    label = verificationLabel,
                    leadingIcon = Icons.Default.CreditCard
                ),
                SettingRowItem(
                    label = issueReportsLabel,
                    leadingIcon = Icons.Default.Flag
                )
            ),
            primaryTint = primary,
            secondaryTint = gray,
            onRowClick = { row ->
                when (row.label) {
                    verificationLabel -> onVerificationClick()
                    issueReportsLabel -> onIssueReportsClick()
                }
            },
            modifier = Modifier.padding(top = 10.dp)
        )

        hopOnButton(
            text = stringResource(id = R.string.logout),
            onClick = onLogoutClick,
            modifier = Modifier
                .padding(top = 32.dp, bottom = 12.dp)
                .fillMaxWidth(),
            containerColor = colorResource(id = R.color.cancelRideRed)
        )
    }
}

private data class SettingRowItem(
    val label: String,
    val leadingIcon: ImageVector
)

@Composable
private fun settingGroupCard(
    rows: List<SettingRowItem>,
    primaryTint: Color,
    secondaryTint: Color,
    onRowClick: ((SettingRowItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(1.dp, secondaryTint.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onRowClick != null) { onRowClick?.invoke(row) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = row.leadingIcon,
                    contentDescription = row.label,
                    tint = primaryTint,
                    modifier = Modifier.size(25.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = row.label,
                    color = Color.Black,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = row.label,
                    tint = secondaryTint.copy(alpha = 0.85f),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun verificationStatusDisplayLabel(status: String?): String {
    return when (status?.trim()?.lowercase()) {
        "verified" -> stringResource(R.string.verification_status_verified)
        "pending" -> stringResource(R.string.verification_status_pending)
        "rejected" -> stringResource(R.string.verification_status_rejected)
        else -> stringResource(R.string.verification_status_unverified)
    }
}

private fun decodeBase64ToBitmap(base64: String?): android.graphics.Bitmap? {
    if (base64.isNullOrBlank()) return null
    return runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
