package com.tritech.hopon.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tritech.hopon.R

@Composable
fun hopOnButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val urbanistSemiBold = FontFamily(
        Font(R.font.urbanist_semibold, FontWeight.SemiBold)
    )

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(
            horizontal = 35.dp,
            vertical = 10.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.colorPrimary),
            contentColor = Color.White,
            disabledContainerColor = colorResource(id = R.color.colorPrimaryDark),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        )
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = urbanistSemiBold,
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
