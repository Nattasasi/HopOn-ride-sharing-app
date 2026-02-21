package com.tritech.hopon.ui.maps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.rideResultCard
import kotlinx.coroutines.launch

private val RideResultCardHeight = 130.dp
private val HandleHeight = 5.dp
private val HandlePadding = 16.dp  // approximate top+bottom

@Composable
fun rideResultsBottomSheetPanel(
    visible: Boolean,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    rides: List<RideListItem>,
    onRideClick: (RideListItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val peekHeight = RideResultCardHeight * 3/2 + HandleHeight + HandlePadding
    val expandedHeight = screenHeight - 100.dp - 70.dp - 30.dp

    val dragDelta = remember { mutableStateOf(0f) }
    val listState = rememberLazyListState()

    val targetHeight = if (expanded) expandedHeight else peekHeight
    val animatedHeight by animateDpAsState(targetValue = targetHeight)

    val nestedScrollConnection = remember(expanded, listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                val listAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

                // Drag down to collapse only when list is at top.
                if (dy > 0 && expanded && listAtTop) {
                    dragDelta.value += dy
                    return Offset(0f, dy)
                }

                // Drag up to expand when collapsed.
                if (dy < 0 && !expanded) {
                    dragDelta.value += dy
                    return Offset(0f, dy)
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val threshold = 30f
                val shouldExpand = dragDelta.value < -threshold
                val shouldCollapse = dragDelta.value > threshold
                dragDelta.value = 0f
                when {
                    shouldExpand && !expanded -> onExpandChange(true)
                    shouldCollapse && expanded -> onExpandChange(false)
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                dragDelta.value = 0f
                return Velocity.Zero
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedHeight)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                    )
                    .nestedScroll(nestedScrollConnection)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(top = 10.dp, bottom = 6.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { dragDelta.value = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragDelta.value += dragAmount
                                },
                                onDragEnd = {
                                    val threshold = 30f
                                    val shouldExpand = dragDelta.value < -threshold
                                    val shouldCollapse = dragDelta.value > threshold
                                    when {
                                        shouldExpand && !expanded -> coroutineScope.launch { onExpandChange(true) }
                                        shouldCollapse && expanded -> coroutineScope.launch { onExpandChange(false) }
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(HandleHeight)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(colorResource(id = R.color.colorPrimaryDark))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Crossfade(targetState = rides, label = "ride-list-crossfade") { rideList ->
                        if (rideList.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.no_rides_for_destination),
                                color = Color.DarkGray,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                state = listState,
                                contentPadding = PaddingValues(bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(rideList) { ride ->
                                    rideResultCard(
                                        meetupLabel = ride.meetupLabel,
                                        meetupDateTimeLabel = ride.meetupDateTimeLabel,
                                        pickupDistanceMeters = ride.pickupDistanceMeters,
                                        hostName = ride.hostName,
                                        waitTimeMinutes = ride.waitTimeMinutes,
                                        peopleCount = ride.peopleCount,
                                        maxPeopleCount = ride.maxPeopleCount,
                                        onClick = { onRideClick(ride) },
                                        modifier = Modifier.height(RideResultCardHeight)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
