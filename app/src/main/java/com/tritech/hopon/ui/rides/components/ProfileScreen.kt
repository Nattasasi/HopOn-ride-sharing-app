package com.tritech.hopon.ui.rides.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.hopOnButton

@Composable
fun profileScreen(
    userName: String,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(id = R.string.profile_title),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        )
        Text(
            text = userName,
            color = Color.DarkGray,
            modifier = Modifier.padding(top = 12.dp)
        )
        hopOnButton(
            text = stringResource(id = R.string.logout),
            onClick = onLogoutClick,
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
        )
    }
}
