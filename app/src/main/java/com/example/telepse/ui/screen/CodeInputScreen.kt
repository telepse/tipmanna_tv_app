package com.example.telepse.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.ui.tv.TvFocusBridge
import com.example.telepse.ui.tv.TvLazyColumn
import com.example.telepse.ui.tv.TvNavButton
import com.example.telepse.ui.tv.requestInitialTvFocus
import com.example.telepse.ui.tv.tvFocusChain
import com.example.telepse.ui.tv.tvFocusRing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.telepse.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.telepse.data.model.Event
import com.example.telepse.ui.network.NetworkStatusIndicator
import com.example.telepse.ui.network.rememberNetworkMonitor
import com.example.telepse.ui.theme.*
import com.example.telepse.ui.viewmodel.CodeInputViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.LocalDate
import java.time.LocalTime

@OptIn(
    ExperimentalTvMaterial3Api::class,
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    ExperimentalComposeUiApi::class,
)
@Composable
fun CodeInputScreen(
    onCodeValidated: (String) -> Unit,
    onLoadOverlay: (String) -> Unit,
    onOpenYouTubeSpike: () -> Unit = {},
    onBackToHub: () -> Unit = {},
    viewModel: CodeInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var textValue by remember { mutableStateOf(uiState.code) }
    val context = LocalContext.current
    val networkMonitor = rememberNetworkMonitor()
    val hubFocus = remember { FocusRequester() }
    val youtubeFocus = remember { FocusRequester() }
    val codeFocus = remember { FocusRequester() }
    val iconSearchFocus = remember { FocusRequester() }
    val primaryActionFocus = remember { FocusRequester() }
    
    val currentEvent = viewModel.getCurrentEvent()
    val hasEvents = uiState.events.isNotEmpty()
    val canNavigateNext = uiState.events.size > 1
    val canNavigatePrevious = uiState.events.size > 1
    
    LaunchedEffect(Unit) {
        codeFocus.requestInitialTvFocus()
    }

    DisposableEffect(Unit) {
        TvFocusBridge.onDpadDown = null
        TvFocusBridge.onDpadUp = null
        onDispose { }
    }

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
        
        // Network Status Indicator - shows at top when offline
        NetworkStatusIndicator(
            networkMonitor = networkMonitor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(2000f) // Above everything
        )

        Box(
            modifier =
                Modifier.align(Alignment.TopStart)
                    .padding(16.dp)
                    .zIndex(2001f),
        ) {
            TvNavButton(
                text = "Hub",
                onClick = onBackToHub,
                focusRequester = hubFocus,
                focusChainDown = codeFocus,
                focusChainRight = youtubeFocus,
            )
        }

        Box(
            modifier =
                Modifier.align(Alignment.TopEnd)
                    .padding(16.dp)
                    .zIndex(2001f),
        ) {
            TvNavButton(
                text = "YouTube spike",
                onClick = onOpenYouTubeSpike,
                focusRequester = youtubeFocus,
                focusChainDown = codeFocus,
                focusChainLeft = hubFocus,
            )
        }
        
        TvLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 64.dp),
            scrollable = true,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
            // Content Container with reduced width
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.75f), // Reduced width to 75%
                horizontalAlignment = Alignment.Start
            ) {
                // Title: "Join Event" - Only show when no event details (Racing Sans One)
                if (!hasEvents) {
                    Text(
                        text = "Join Event",
                        style = MaterialTheme.typography.displaySmall, // Racing Sans One
                        color = TextBlack,
                        modifier = Modifier.padding(top = 80.dp, bottom = 8.dp)
                    )
                }
                
                // Subtitle: "Search for your event" (Open Sans)
                Text(
                    text = "Search for your event",
                    style = MaterialTheme.typography.bodyLarge, // Open Sans
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 48.dp)
                )
                
                // Section Header: "Search" (Racing Sans One)
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.titleLarge, // Racing Sans One
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
                                style = MaterialTheme.typography.bodyLarge, // Open Sans
                                color = TextGray.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .focusRequester(codeFocus)
                            .tvFocusChain(
                                up = hubFocus,
                                down = iconSearchFocus,
                                right = iconSearchFocus,
                            )
                            .tvFocusRing(cornerRadiusDp = 12)
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
                        onClick = { viewModel.searchEvents() },
                        modifier = Modifier
                            .size(56.dp)
                            .focusRequester(iconSearchFocus)
                            .tvFocusChain(
                                up = codeFocus,
                                down = primaryActionFocus,
                                left = codeFocus,
                            )
                            .onKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                when (event.key) {
                                    Key.DirectionCenter,
                                    Key.Enter,
                                    Key.NumPadEnter,
                                    -> {
                                        viewModel.searchEvents()
                                        true
                                    }
                                    else -> false
                                }
                            }
                            .tvFocusRing(cornerRadiusDp = 8)
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
                            onLoadOverlay(currentEvent.id)
                        } else {
                            viewModel.searchEvents()
                        }
                    },
                    enabled =
                        !uiState.isLoading &&
                            (hasEvents && currentEvent != null || !hasEvents),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .focusRequester(primaryActionFocus)
                        .tvFocusChain(up = iconSearchFocus)
                        .tvFocusRing(cornerRadiusDp = 8, enabled = !uiState.isLoading)
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
                            text = if (hasEvents) "Load full display" else "Search",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp) // Montserrat
                        )
                    }
                }
                
                // Error Message
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium, // Open Sans
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
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
    
    // Determine status based on event date and time
    val (status, statusBgColor, statusTextColor) = getEventStatus(event)
    
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
            // Event Name - Allow wrapping (Racing Sans One)
            Text(
                text = event.eventName,
                style = MaterialTheme.typography.headlineSmall, // Racing Sans One
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
                    style = MaterialTheme.typography.bodyLarge, // Open Sans
                    color = TextGray
                )
                
                // Status Tag (Montserrat for buttons/offers)
                Box(
                    modifier = Modifier
                        .background(
                            color = statusBgColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium, // Montserrat
                        color = statusTextColor
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
                    style = MaterialTheme.typography.bodyLarge, // Open Sans
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
                    style = MaterialTheme.typography.bodyLarge, // Open Sans
                    color = TextGray
                )
            }
            
            // Tip Count (using numOfTips)
            val tipCount = event.numOfTips ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = "Tip",
                    tint = TextGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$tipCount tip${if (tipCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyLarge, // Open Sans
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

/**
 * Determines the event status based on event date and end time.
 * Returns a triple of (status text, background color, text color).
 */
@Composable
fun getEventStatus(event: Event): Triple<String, Color, Color> {
    val now = LocalDateTime.now()
    
    return try {
        // Parse event date
        val eventInstant = Instant.parse(event.eventDate)
        val eventDateTime = LocalDateTime.ofInstant(eventInstant, ZoneId.systemDefault())
        val eventDate = eventDateTime.toLocalDate()
        val currentDate = now.toLocalDate()
        
        // Parse end time if available
        val endTime = event.endTime?.let {
            try {
                LocalTime.parse(it)
            } catch (e: Exception) {
                null
            }
        }
        
        when {
            // Event date is in the past
            eventDate.isBefore(currentDate) -> {
                Triple("Completed", Color.Black, Color.White)
            }
            // Event date is today
            eventDate.isEqual(currentDate) -> {
                // Check if end time has passed
                if (endTime != null && now.toLocalTime().isAfter(endTime)) {
                    Triple("Completed", Color.Black, Color.White)
                } else {
                    Triple("Ongoing", OngoingBlue, Color.White)
                }
            }
            // Event date is in the future
            else -> {
                Triple("Not started", Color(0xFFF5F6F7), Color.Black)
            }
        }
    } catch (e: Exception) {
        // Fallback to default status if parsing fails
        Triple("Pending", OngoingBlue, Color.White)
    }
}
