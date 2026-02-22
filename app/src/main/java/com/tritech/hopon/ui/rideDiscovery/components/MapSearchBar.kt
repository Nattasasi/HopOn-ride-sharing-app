package com.tritech.hopon.ui.rideDiscovery.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R

@Composable
fun mapSearchBar(
    query: String,
    requestFocus: Boolean,
    onFocusHandled: () -> Unit,
    clearFocusSignal: Int,
    onFocusChanged: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
            onFocusHandled()
        }
    }

    LaunchedEffect(clearFocusSignal) {
        if (clearFocusSignal > 0) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.DarkGray
                )
            },
            placeholder = {
                Text(text = stringResource(id = R.string.where_to))
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { onSearchAction() },
                onDone = { onSearchAction() }
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state -> onFocusChanged(state.isFocused) },
            maxLines = 1
        )
    }
}
