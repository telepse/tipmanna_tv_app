package com.example.telepse.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.telepse.data.model.Event

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FullDisplayScreen(
    event: Event? = null
) {
    val context = LocalContext.current
    
    // TODO: Replace with actual asset path when GIF is provided
    // For now, using a placeholder path
    // The GIF should be placed in: app/src/main/res/drawable/background_animation.gif
    val gifPath = "android.resource://${context.packageName}/drawable/background_animation"
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Animated GIF Background
        // Note: Place your GIF file at: app/src/main/res/drawable/background_animation.gif
        val imagePainter = rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(gifPath)
                .error(android.R.drawable.ic_menu_gallery) // Fallback if GIF not found
                .build()
        )
        
        Image(
            painter = imagePainter,
            contentDescription = "Background Animation",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Content Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Placeholder for content that will be provided later
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (event != null) {
                        Text(
                            text = event.eventName,
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Hosted by ${event.hostName}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = event.venueLocation,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Full Display Screen",
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Content will be displayed here",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

