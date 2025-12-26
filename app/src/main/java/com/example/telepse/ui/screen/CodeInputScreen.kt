package com.example.telepse.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telepse.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.telepse.data.model.Event
import com.example.telepse.ui.theme.*
import com.example.telepse.ui.viewmodel.CodeInputViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CodeInputScreen(
    onCodeValidated: (String) -> Unit,
    onLoadOverlay: (String) -> Unit,
    viewModel: CodeInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var textValue by remember { mutableStateOf(uiState.code) }
    val context = LocalContext.current
    
    val currentEvent = viewModel.getCurrentEvent()
    val hasEvents = uiState.events.isNotEmpty()
    val canNavigateNext = uiState.events.size > 1
    val canNavigatePrevious = uiState.events.size > 1
    
    LaunchedEffect(uiState.code) {
        if (textValue != uiState.code) {
            textValue = uiState.code
        }
    }
    
    // Show event details when events are loaded
    LaunchedEffect(uiState.events) {
        // Events loaded, UI will update automatically
    }
    
    // Background image path
    // Place your background image PNG file at: app/src/main/res/drawable/join_event_background.png
    val backgroundImagePath = "android.resource://${context.packageName}/drawable/join_event_background"
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BackgroundWhite,  // Top-left: #FFFFFF
                        BackgroundCream, // Bottom-right: #FFF8F1
                    ),
                    start = Offset(0f, 0f), // Top-left
                    end = Offset(10000f, 10000f) // Bottom-right (large enough value for diagonal gradient)
                )
            )
    ) {
        // Background Image Overlay
        val imagePainter = rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(backgroundImagePath)
                .error(android.R.drawable.ic_menu_gallery) // Fallback if image not found
                .build()
        )
        
        Image(
            painter = imagePainter,
            contentDescription = "Background Pattern",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        
        // Content - Reduced width and scrollable
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 48.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Content Container with reduced width
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.75f), // Reduced width to 75%
                horizontalAlignment = Alignment.Start
            ) {
                // Title: "Join Event" - Only show when no event details
                if (!hasEvents) {
                    Text(
                        text = "Join Event",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack,
                        modifier = Modifier.padding(top = 80.dp, bottom = 8.dp)
                    )
                }
                
                // Subtitle: "Search for your event"
                Text(
                    text = "Search for your event",
                    fontSize = 16.sp,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 48.dp)
                )
                
                // Section Header: "Search"
                Text(
                    text = "Search",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Input Field and Search Icon Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Input Field
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            textValue = newValue
                            viewModel.updateCode(newValue)
                        },
                        placeholder = {
                            Text(
                                text = "Enter Event Code",
                                color = TextGray.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(
                                color = BackgroundWhite,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = IconBorderGray,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = BackgroundWhite,
                            focusedContainerColor = BackgroundWhite,
                            unfocusedTextColor = TextBlack,
                            focusedTextColor = TextBlack,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        singleLine = true
                    )
                    
                    // Search Icon Button
                    IconButton(
                        onClick = {
                            if (textValue.isNotBlank()) {
                                viewModel.searchEvents()
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = BackgroundWhite,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = IconBorderGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search_custom),
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Event Details Section (shown after search)
                if (hasEvents && currentEvent != null) {
                    EventDetailsCard(
                        event = currentEvent,
                        canNavigatePrevious = canNavigatePrevious,
                        canNavigateNext = canNavigateNext,
                        onPrevious = { viewModel.previousEvent() },
                        onNext = { viewModel.nextEvent() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp) // Spacing before button
                    )
                }
                
                // Button - Changes based on state
                Button(
                    onClick = {
                        if (hasEvents && currentEvent != null) {
                            // Load Overlay action - navigate to fundraising milestone screen
                            onLoadOverlay(currentEvent.id)
                        } else {
                            // Search action
                            if (textValue.isNotBlank()) {
                                viewModel.searchEvents()
                            }
                        }
                    },
                    enabled = !uiState.isLoading && textValue.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            color = if (hasEvents) LoadOverlayButtonBg else SearchButtonOrange,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .then(
                            if (hasEvents) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = IconBorderGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            } else {
                                Modifier // No border for orange Search button
                            }
                        ),
                    shape = RoundedCornerShape(8.dp),
                    colors = if (hasEvents) {
                        // Load Overlay button - white/light gray
                        ButtonDefaults.buttonColors(
                            containerColor = LoadOverlayButtonBg,
                            contentColor = LoadOverlayButtonText,
                            disabledContainerColor = LoadOverlayButtonBg.copy(alpha = 0.6f),
                            disabledContentColor = LoadOverlayButtonText.copy(alpha = 0.6f)
                        )
                    } else {
                        // Search button - orange
                        ButtonDefaults.buttonColors(
                            containerColor = SearchButtonOrange,
                            contentColor = BackgroundWhite,
                            disabledContainerColor = SearchButtonOrange.copy(alpha = 0.6f),
                            disabledContentColor = BackgroundWhite.copy(alpha = 0.6f)
                        )
                    }
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = if (hasEvents) LoadOverlayButtonText else BackgroundWhite
                        )
                    } else {
                        Text(
                            text = if (hasEvents) "Load Overlay" else "Search",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Error Message
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .align(Alignment.CenterHorizontally),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EventDetailsCard(
    event: Event,
    canNavigatePrevious: Boolean = false,
    canNavigateNext: Boolean = false,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Format date and time
    val formattedDate = try {
        val instant = Instant.parse(event.eventDate)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        dateFormatter.format(dateTime)
    } catch (e: Exception) {
        event.eventDate
    }
    
    val formattedTime = event.startTime
    
    // Determine status (simplified - you can enhance this based on eventDate vs current time)
    val status = if (event.approved == true && event.rejected != true) {
        "Ongoing"
    } else if (event.rejected == true) {
        "Rejected"
    } else {
        "Pending"
    }
    
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Navigation Button (Previous)
            if (canNavigatePrevious) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = BackgroundWhite,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = IconBorderGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back_custom),
                        contentDescription = "Previous",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // Event Details Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Event Name - Allow wrapping
            Text(
                text = event.eventName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                maxLines = 2
            )
            
            // Host and Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hosted by ${event.hostName}",
                    fontSize = 16.sp,
                    color = TextGray
                )
                
                // Status Tag
                Box(
                    modifier = Modifier
                        .background(
                            color = OngoingBlue,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = BackgroundWhite
                    )
                }
            }
            
            // Date and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Date",
                    tint = TextGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$formattedDate • $formattedTime",
                    fontSize = 16.sp,
                    color = TextGray
                )
            }
            
            // Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = TextGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.venueLocation,
                    fontSize = 16.sp,
                    color = TextGray
                )
            }
            
            // Tipped Count (using amountToRaise or a placeholder)
            val tippedAmount = event.amountToRaise?.toInt() ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = "Tipped",
                    tint = TextGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$tippedAmount Tipped",
                    fontSize = 16.sp,
                    color = TextGray
                )
            }
            }
            
            // Right Navigation Button (Next) or View Details Arrow
            if (canNavigateNext) {
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = BackgroundWhite,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = IconBorderGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_forward_custom),
                        contentDescription = "Next",
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Navigation Arrow Button (when no next event)
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = {
                        // Navigate to full display
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = BackgroundWhite,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = IconBorderGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_forward_custom),
                        contentDescription = "View Details",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
