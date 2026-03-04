package com.tritech.hopon.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.tritech.hopon.R

private const val BadgeBackgroundAlpha = 0.18f

enum class HopOnBadgeTone {
    BLUE,
    YELLOW,
    GREEN
}

data class HopOnBadgeColors(
    val backgroundColor: Color,
    val textColor: Color
)

@Composable
fun hopOnBadgeColors(tone: HopOnBadgeTone): HopOnBadgeColors {
    return when (tone) {
        HopOnBadgeTone.BLUE -> {
            val blue = colorResource(id = R.color.colorPrimary)
            HopOnBadgeColors(
                backgroundColor = blue.copy(alpha = BadgeBackgroundAlpha),
                textColor = blue
            )
        }

        HopOnBadgeTone.YELLOW -> {
            val yellow = colorResource(id = R.color.colorAccent)
            HopOnBadgeColors(
                backgroundColor = yellow.copy(alpha = BadgeBackgroundAlpha),
                textColor = yellow
            )
        }

        HopOnBadgeTone.GREEN -> {
            HopOnBadgeColors(
                backgroundColor = colorResource(id = R.color.badgeGreenBackground),
                textColor = colorResource(id = R.color.badgeGreenText)
            )
        }
    }
}
