package com.tritech.hopon.ui.maps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.tritech.hopon.R

@Composable
fun placePredictionsPanel(
    predictions: List<AutocompletePrediction>,
    showEmptyState: Boolean,
    onPredictionClick: (AutocompletePrediction) -> Unit
) {
    if (showEmptyState) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.no_places_found),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(predictions, key = { it.placeId }) { prediction ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPredictionClick(prediction) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = prediction.getFullText(null).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    maxLines = 1,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}
