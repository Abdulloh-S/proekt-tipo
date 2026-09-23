package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TicketEntity
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.AudioPlayerSheet
import com.example.ui.screens.*
import com.example.ui.theme.BusRed
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GoldCrown
import com.example.ui.theme.MyApplicationTheme
import com.example.util.appStrings

enum class Screen(val icon: androidx.compose.ui.graphics.vector.ImageVector, val testTag: String) {
    HOME(Icons.Default.Home, "nav_home"),
    MAP(Icons.Default.Map, "nav_map"),
    ROUTE(Icons.Default.AltRoute, "nav_route"),
    TICKETS(Icons.Default.ConfirmationNumber, "nav_tickets"),
    ATTRACTIONS(Icons.Default.AccountBalance, "nav_attractions"),
    PARTNERS(Icons.Default.Loyalty, "nav_partners"),
    PROFILE(Icons.Default.Person, "nav_profile")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(Screen.HOME) }
                var showAdminPanel by remember { mutableStateOf(false) }
                var selectedQrTicket by remember { mutableStateOf<TicketEntity?>(null) }
                var showAuthDialog by remember { mutableStateOf(false) }

                val buses by viewModel.buses.collectAsStateWithLifecycle()
                val stops by viewModel.stops.collectAsStateWithLifecycle()
                val tariffs by viewModel.tariffs.collectAsStateWithLifecycle()
                val attractions by viewModel.attractions.collectAsStateWithLifecycle()
                val partners by viewModel.partners.collectAsStateWithLifecycle()
                val tickets by viewModel.tickets.collectAsStateWithLifecycle()
                val activeTicket by viewModel.activeTicket.collectAsStateWithLifecycle()
                val user by viewModel.user.collectAsStateWithLifecycle()
                val notifications by viewModel.notifications.collectAsStateWithLifecycle()
                val audioPlayerState by viewModel.audioPlayerState.collectAsStateWithLifecycle()
                val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
                val nearestStop by viewModel.nearestStop.collectAsStateWithLifecycle()
                val nearestBus by viewModel.nearestBus.collectAsStateWithLifecycle()

                val strings = remember(currentLanguage) { appStrings(currentLanguage) }

                // Language quick menu dropdown
                var languageMenuExpanded by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (!showAdminPanel && selectedQrTicket == null) {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Emerald600),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_app_logo),
                                                contentDescription = "Logo",
                                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Dushanbe Tour",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                fontSize = 17.sp
                                            )
                                            Text(
                                                text = strings.appSubtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Emerald600,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    // Language quick-toggle
                                    Box {
                                        TextButton(onClick = { languageMenuExpanded = true }) {
                                            Text(
                                                text = "${currentLanguage.flag} ${currentLanguage.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = languageMenuExpanded,
                                            onDismissRequest = { languageMenuExpanded = false }
                                        ) {
                                            AppLanguage.values().forEach { lang ->
                                                DropdownMenuItem(
                                                    text = { Text("${lang.flag} ${lang.displayName}") },
                                                    onClick = {
                                                        viewModel.setAppLanguage(lang)
                                                        languageMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    },
                    bottomBar = {
                        if (!showAdminPanel && selectedQrTicket == null) {
                            Column {
                                // Bottom Navigation Bar with reactive localized labels
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 6.dp
                                ) {
                                    Screen.values().forEach { screen ->
                                        val labelText = when (screen) {
                                            Screen.HOME -> strings.navHome
                                            Screen.MAP -> strings.navMap
                                            Screen.ROUTE -> strings.navRoute
                                            Screen.TICKETS -> strings.navTickets
                                            Screen.ATTRACTIONS -> strings.navAttractions
                                            Screen.PARTNERS -> strings.navPartners
                                            Screen.PROFILE -> strings.navProfile
                                        }
                                        NavigationBarItem(
                                            selected = currentScreen == screen,
                                            onClick = { currentScreen = screen },
                                            icon = {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = labelText
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = labelText,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (currentScreen == screen) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Emerald600,
                                                selectedTextColor = Emerald600,
                                                indicatorColor = Emerald600.copy(alpha = 0.15f)
                                            ),
                                            modifier = Modifier.testTag(screen.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main Navigation Switcher
                        when {
                            showAdminPanel -> {
                                AdminPanelScreen(
                                    buses = buses,
                                    stops = stops,
                                    tariffs = tariffs,
                                    attractions = attractions,
                                    partners = partners,
                                    tickets = tickets,
                                    onUpdateTariffPrice = { id, price -> viewModel.updateTariffPrice(id, price) },
                                    onToggleBusStatus = { id, status -> viewModel.toggleBusStatus(id, status) },
                                    onAddBus = { num, plate -> viewModel.addBus(num, plate) },
                                    onSetSimulationMultiplier = { m -> viewModel.setSimulationMultiplier(m) },
                                    onToggleSimulation = { en -> viewModel.toggleSimulation(en) },
                                    onBroadcastNotification = { t, m -> viewModel.broadcastNotification(t, m) },
                                    onRevokeTicket = { id -> viewModel.revokeTicket(id) },
                                    onActivateTicket = { id -> viewModel.activateTicket(id) },
                                    onUpdateAttractionAudio = { id, r, t, e -> viewModel.updateAttractionAudio(id, r, t, e) },
                                    onLogout = { showAdminPanel = false },
                                    onBack = { showAdminPanel = false }
                                )
                            }

                            selectedQrTicket != null -> {
                                QrTicketScreen(
                                    ticket = selectedQrTicket,
                                    onValidateTicket = { qr -> viewModel.validateTicket(qr) },
                                    onBack = { selectedQrTicket = null }
                                )
                            }

                            else -> {
                                when (currentScreen) {
                                    Screen.HOME -> HomeScreen(
                                        nearestStop = nearestStop,
                                        nearestBus = nearestBus,
                                        activeTicket = activeTicket,
                                        attractions = attractions,
                                        partners = partners,
                                        language = currentLanguage,
                                        onNavigateToMap = { currentScreen = Screen.MAP },
                                        onNavigateToTickets = { currentScreen = Screen.TICKETS },
                                        onNavigateToRoute = { currentScreen = Screen.ROUTE },
                                        onNavigateToQrTicket = { selectedQrTicket = activeTicket ?: tickets.firstOrNull() },
                                        onSelectAttraction = { currentScreen = Screen.ATTRACTIONS },
                                        onPlayAudioGuide = { attr -> viewModel.playAudioGuide(attr) },
                                        onPlayStopAudio = { stop -> viewModel.playStopAudio(stop) },
                                        onAnnounceBus = { bus -> viewModel.announceBus(bus) },
                                        onPlayTourOverview = { viewModel.playTourOverview() },
                                        isAutoGuideEnabled = audioPlayerState.isAutoGuideEnabled,
                                        onToggleAutoGuide = { viewModel.toggleAutoGuide() }
                                    )

                                    Screen.MAP -> MapScreen(
                                        buses = buses,
                                        stops = stops,
                                        attractions = attractions,
                                        partners = partners,
                                        routeWaypoints = viewModel.routeWaypoints,
                                        language = currentLanguage,
                                        onPlayAudioGuide = { attr -> viewModel.playAudioGuide(attr) },
                                        onPlayStopAudio = { stop -> viewModel.playStopAudio(stop) },
                                        onAnnounceBus = { bus -> viewModel.announceBus(bus) },
                                        isAutoGuideEnabled = audioPlayerState.isAutoGuideEnabled,
                                        onToggleAutoGuide = { viewModel.toggleAutoGuide() }
                                    )

                                    Screen.ROUTE -> RouteScreen(
                                        stops = stops,
                                        language = currentLanguage,
                                        onOpenMap = { currentScreen = Screen.MAP },
                                        onSelectStop = { currentScreen = Screen.MAP },
                                        onPlayStopAudio = { stop -> viewModel.playStopAudio(stop) },
                                        onPlayTourOverview = { viewModel.playTourOverview() },
                                        isAutoGuideEnabled = audioPlayerState.isAutoGuideEnabled,
                                        onToggleAutoGuide = { viewModel.toggleAutoGuide() }
                                    )

                                    Screen.TICKETS -> TicketsScreen(
                                        tariffs = tariffs,
                                        activeTicket = activeTicket,
                                        ticketHistory = tickets,
                                        language = currentLanguage,
                                        onBuyTicket = { tariff, name, phone, email ->
                                            viewModel.buyTicket(tariff, name, phone, email)
                                        },
                                        onOpenQrTicket = { ticket -> selectedQrTicket = ticket }
                                    )

                                    Screen.ATTRACTIONS -> AttractionsScreen(
                                        attractions = attractions,
                                        onSelectAttraction = { attr -> viewModel.playAudioGuide(attr) },
                                        onPlayAudioGuide = { attr -> viewModel.playAudioGuide(attr) },
                                        onToggleFavorite = { id, isFav -> viewModel.toggleFavorite(id, isFav) },
                                        onShowOnMap = { currentScreen = Screen.MAP }
                                    )

                                    Screen.PARTNERS -> PartnersScreen(
                                        partners = partners,
                                        onShowOnMap = { currentScreen = Screen.MAP }
                                    )

                                    Screen.PROFILE -> ProfileScreen(
                                        user = user,
                                        tickets = tickets,
                                        notifications = notifications,
                                        currentLanguage = currentLanguage,
                                        onChangeLanguage = { lang -> viewModel.setAppLanguage(lang) },
                                        onNavigateToTickets = { currentScreen = Screen.TICKETS },
                                        onNavigateToPartners = { currentScreen = Screen.PARTNERS },
                                        onOpenAdminPanel = { showAdminPanel = true },
                                        onOpenAuthDialog = { showAuthDialog = true }
                                    )
                                }
                            }
                        }

                        // Floating Persistent Audio Guide Player Bar
                        if (audioPlayerState.currentTrack != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = if (showAdminPanel || selectedQrTicket != null) 8.dp else 12.dp)
                            ) {
                                AudioPlayerSheet(
                                    playerState = audioPlayerState,
                                    onTogglePlay = { viewModel.audioGuideManager.togglePlayPause() },
                                    onSeek = { pos -> viewModel.audioGuideManager.seekTo(pos) },
                                    onSkipForward = { viewModel.audioGuideManager.skipForward() },
                                    onSkipBackward = { viewModel.audioGuideManager.skipBackward() },
                                    onChangeLanguage = { lang -> viewModel.setAppLanguage(lang) },
                                    onSetSpeed = { speed -> viewModel.audioGuideManager.setSpeed(speed) },
                                    onToggleAutoGuide = { viewModel.toggleAutoGuide() },
                                    onClose = { viewModel.audioGuideManager.stop() }
                                )
                            }
                        }
                    }
                }

                // Auth / Registration Dialog
                if (showAuthDialog) {
                    AuthDialog(
                        currentUser = user,
                        onDismiss = { showAuthDialog = false },
                        onSaveUser = { name, email, phone ->
                            viewModel.updateUser(name, email, phone)
                        }
                    )
                }
            }
        }
    }
}
