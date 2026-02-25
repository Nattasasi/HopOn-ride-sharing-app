package com.tritech.hopon.ui.rideDiscovery.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onLogoutClick: () -> Unit
) {
    val primary = colorResource(id = R.color.colorPrimary)
    val gray = colorResource(id = R.color.colorPrimaryDark)

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
        )

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
            text = stringResource(id = R.string.profile_account_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = gray,
            modifier = Modifier.padding(top = 24.dp)
        )

        settingGroupCard(
            rows = listOf(
                SettingRowItem(
                    label = stringResource(id = R.string.profile_personal_information),
                    leadingIcon = Icons.Default.Person
                ),
                SettingRowItem(
                    label = stringResource(id = R.string.profile_preferences),
                    leadingIcon = Icons.Default.Settings
                )
            ),
            primaryTint = primary,
            secondaryTint = gray,
            modifier = Modifier.padding(top = 10.dp)
        )

        Text(
            text = stringResource(id = R.string.profile_ride_details_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = gray,
            modifier = Modifier.padding(top = 22.dp)
        )

        settingGroupCard(
            rows = listOf(
                SettingRowItem(
                    label = stringResource(id = R.string.profile_schedule_route_info),
                    leadingIcon = Icons.Default.Timeline
                ),
                SettingRowItem(
                    label = stringResource(id = R.string.profile_payment_cost),
                    leadingIcon = Icons.Default.CreditCard
                ),
                SettingRowItem(
                    label = stringResource(id = R.string.profile_rating_reviews),
                    leadingIcon = Icons.Default.Star
                )
            ),
            primaryTint = primary,
            secondaryTint = gray,
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
