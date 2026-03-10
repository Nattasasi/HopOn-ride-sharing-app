package com.tritech.hopon.ui.rideDiscovery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCarFilled
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import com.tritech.hopon.R

enum class MapsBottomNavItem {
    HOME,
    RIDES,
    PROFILE
}

sealed interface MapsBottomNavAction {
    data object NoOp : MapsBottomNavAction
    data object ShowRides : MapsBottomNavAction
    data object ShowHistory : MapsBottomNavAction
    data object ShowProfile : MapsBottomNavAction
}

fun mapBottomNavItemToAction(item: MapsBottomNavItem): MapsBottomNavAction {
    return when (item) {
        MapsBottomNavItem.HOME -> MapsBottomNavAction.ShowRides
        MapsBottomNavItem.RIDES -> MapsBottomNavAction.ShowHistory
        MapsBottomNavItem.PROFILE -> MapsBottomNavAction.ShowProfile
    }
}

@Composable
fun mapsBottomNavigation(
    selectedItem: MapsBottomNavItem,
    onItemSelected: (MapsBottomNavItem) -> Unit
) {
    val accentColor = colorResource(id = R.color.colorPrimary)
    val itemColors = NavigationBarItemDefaults.colors(
        indicatorColor = Color.Transparent,
        selectedIconColor = accentColor,
        selectedTextColor = Color.Black,
        unselectedIconColor = Color.DarkGray,
        unselectedTextColor = Color.DarkGray
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.White,
            contentColor = Color.DarkGray,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            NavigationBarItem(
                selected = selectedItem == MapsBottomNavItem.HOME,
                onClick = {
                    onItemSelected(MapsBottomNavItem.HOME)
                },
                icon = {
                    BottomNavIconWithIndicator(
                        imageVector = Icons.Filled.DirectionsCarFilled,
                        selected = selectedItem == MapsBottomNavItem.HOME,
                        accentColor = accentColor
                    )
                },
                label = null,
                alwaysShowLabel = false,
                colors = itemColors
            )

            NavigationBarItem(
                selected = selectedItem == MapsBottomNavItem.RIDES,
                onClick = {
                    onItemSelected(MapsBottomNavItem.RIDES)
                },
                icon = {
                    BottomNavIconWithIndicator(
                        imageVector = Icons.Filled.History,
                        selected = selectedItem == MapsBottomNavItem.RIDES,
                        accentColor = accentColor
                    )
                },
                label = null,
                alwaysShowLabel = false,
                colors = itemColors
            )

            NavigationBarItem(
                selected = selectedItem == MapsBottomNavItem.PROFILE,
                onClick = {
                    onItemSelected(MapsBottomNavItem.PROFILE)
                },
                icon = {
                    BottomNavIconWithIndicator(
                        imageVector = Icons.Filled.Person,
                        selected = selectedItem == MapsBottomNavItem.PROFILE,
                        accentColor = accentColor
                    )
                },
                label = null,
                alwaysShowLabel = false,
                colors = itemColors
            )
        }
    }
}

@Composable
private fun BottomNavIconWithIndicator(
    imageVector: ImageVector,
    selected: Boolean,
    accentColor: Color
) {
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "indicatorWidth"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "indicatorAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "iconScale"
    )

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .scale(iconScale)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(3.dp)
                .alpha(indicatorAlpha)
                .background(
                    color = accentColor,
                    shape = RoundedCornerShape(percent = 50)
                )
        )
    }
}
