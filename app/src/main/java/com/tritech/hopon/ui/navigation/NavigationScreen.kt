package com.tritech.hopon.ui.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.fillMaxSize
import com.google.android.libraries.navigation.NavigationView

@Composable
fun navigationScreen(
    savedInstanceState: Bundle?,
    onViewReady: (NavigationView) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            NavigationView(context).also { view ->
                view.onCreate(savedInstanceState)
                onViewReady(view)
            }
        }
    )
}
