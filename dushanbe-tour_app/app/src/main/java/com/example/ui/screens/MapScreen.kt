package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.AttractionEntity
import com.example.data.local.BusEntity
import com.example.data.local.BusStopEntity
import com.example.data.local.PartnerEntity
import com.example.data.model.AppLanguage
import com.example.ui.components.LiveStatusBadge
import com.example.ui.components.RatingBadge
import com.example.ui.theme.BusRed
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GoldCrown
import com.example.util.appStrings
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

enum class MapFilterCategory(val titleRu: String, val titleTj: String, val titleEn: String, val icon: String) {
    ALL("Все объекты", "Ҳама", "All Objects", "🗺️"),
    BUSES("Автобусы live", "Автобусҳо", "Live Buses", "🚌"),
    STOPS("Остановки", "Истгоҳҳо", "Stops", "🚏"),
    ATTRACTIONS("Достопримечательности", "Ҷойҳои таърихӣ", "Attractions", "🏛️"),
    PARTNERS("Партнёры", "Шарикон", "Partners", "🍽️")
}

sealed class MapSelection {
    data class BusSelect(val bus: BusEntity) : MapSelection()
    data class StopSelect(val stop: BusStopEntity) : MapSelection()
    data class AttractionSelect(val attraction: AttractionEntity) : MapSelection()
    data class PartnerSelect(val partner: PartnerEntity) : MapSelection()
}

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    buses: List<BusEntity>,
    stops: List<BusStopEntity>,
    attractions: List<AttractionEntity>,
    partners: List<PartnerEntity>,
    routeWaypoints: List<Pair<Double, Double>>,
    language: AppLanguage = AppLanguage.RU,
    onPlayAudioGuide: (AttractionEntity) -> Unit,
    onPlayStopAudio: (BusStopEntity) -> Unit = {},
    onAnnounceBus: (BusEntity) -> Unit = {},
    isAutoGuideEnabled: Boolean = false,
    onToggleAutoGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val strings = remember(language) { appStrings(language) }

    // Dushanbe City Center: Somoni Monument / Rudaki Avenue
    val dushanbeCenter = remember { LatLng(38.5737, 68.7850) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(dushanbeCenter, 14.2f)
    }

    // Selected object for bottom detail sheet
    var selectedItem by remember { mutableStateOf<MapSelection?>(null) }

    // Filter categories
    var selectedCategory by remember { mutableStateOf(MapFilterCategory.ALL) }

    // Map style/type (Normal, Hybrid, Terrain)
    var currentMapType by remember { mutableStateOf(MapType.NORMAL) }
    var showLayerDialog by remember { mutableStateOf(false) }

    // User location state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var userLocation by remember { mutableStateOf<Location?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    userLocation = loc
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15.5f)
                        )
                    }
                }
            }
        }
    }

    // Convert route waypoints to Google Maps LatLng
    val polylinePoints = remember(routeWaypoints) {
        routeWaypoints.map { LatLng(it.first, it.second) }
    }

    // Primary bus for telemetry bar
    val activeBus = buses.firstOrNull { it.status == "ACTIVE" } ?: buses.firstOrNull()

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = true,
            zoomGesturesEnabled = true,
            mapToolbarEnabled = true
        )
    }

    val mapProperties = remember(currentMapType, hasLocationPermission) {
        MapProperties(
            mapType = currentMapType,
            isMyLocationEnabled = hasLocationPermission,
            isTrafficEnabled = false
        )
    }

    Box(modifier = modifier.fillMaxSize().testTag("map_screen")) {
        // Real Interactive Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapClick = {
                selectedItem = null
            }
        ) {
            // 1. Hop-On Hop-Off Route Polylines
            // Outer casing for high contrast
            Polyline(
                points = polylinePoints,
                color = Color.White.copy(alpha = 0.85f),
                width = 16f,
                geodesic = true
            )
            // Inner brand route line
            Polyline(
                points = polylinePoints,
                color = BusRed,
                width = 10f,
                geodesic = true
            )

            // 2. Bus Stop Markers
            if (selectedCategory == MapFilterCategory.ALL || selectedCategory == MapFilterCategory.STOPS) {
                stops.forEach { stop ->
                    val stopName = when (language) {
                        AppLanguage.TJ -> stop.nameTj
                        AppLanguage.EN -> stop.nameEn
                        AppLanguage.RU -> stop.nameRu
                    }
                    val stopState = rememberMarkerState(
                        key = "stop_${stop.id}",
                        position = LatLng(stop.latitude, stop.longitude)
                    )
                    MarkerComposable(
                        state = stopState,
                        title = stopName,
                        snippet = "Следующий автобус через ${stop.nextBusMinutes} мин",
                        onClick = {
                            selectedItem = MapSelection.StopSelect(stop)
                            true
                        }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BusRed,
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${stop.stopOrder}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // 3. Live Buses Markers
            if (selectedCategory == MapFilterCategory.ALL || selectedCategory == MapFilterCategory.BUSES) {
                buses.forEach { bus ->
                    val busState = rememberMarkerState(
                        key = "bus_${bus.id}",
                        position = LatLng(bus.currentLat, bus.currentLng)
                    )
                    MarkerComposable(
                        state = busState,
                        title = bus.busNumber,
                        snippet = "Скорость: ${bus.speedKmH} км/ч • Мест свободно: ${bus.capacity - bus.currentPassengers}",
                        onClick = {
                            selectedItem = MapSelection.BusSelect(bus)
                            true
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.8f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldCrown),
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("⚡", fontSize = 10.sp)
                                    Text(
                                        text = bus.busNumber,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(BusRed)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = bus.busNumber,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(bus.heading)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Attractions & Museums Markers
            if (selectedCategory == MapFilterCategory.ALL || selectedCategory == MapFilterCategory.ATTRACTIONS) {
                attractions.forEach { attr ->
                    val attrName = when (language) {
                        AppLanguage.TJ -> attr.nameTj
                        AppLanguage.EN -> attr.nameEn
                        AppLanguage.RU -> attr.nameRu
                    }
                    val attrState = rememberMarkerState(
                        key = "attr_${attr.id}",
                        position = LatLng(attr.latitude, attr.longitude)
                    )
                    MarkerComposable(
                        state = attrState,
                        title = attrName,
                        snippet = "★ ${attr.rating} • ${attr.category}",
                        onClick = {
                            selectedItem = MapSelection.AttractionSelect(attr)
                            true
                        }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Emerald600,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (attr.category.uppercase()) {
                                        "MUSEUM" -> Icons.Default.AccountBalance
                                        "PARK" -> Icons.Default.Park
                                        "MONUMENT" -> Icons.Default.EmojiEvents
                                        else -> Icons.Default.Place
                                    },
                                    contentDescription = attrName,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Partners & Gastronomy Markers
            if (selectedCategory == MapFilterCategory.ALL || selectedCategory == MapFilterCategory.PARTNERS) {
                partners.forEach { partner ->
                    val partnerState = rememberMarkerState(
                        key = "partner_${partner.id}",
                        position = LatLng(partner.latitude, partner.longitude)
                    )
                    MarkerComposable(
                        state = partnerState,
                        title = partner.name,
                        snippet = "${partner.discountPercent}% скидка • ${partner.category}",
                        onClick = {
                            selectedItem = MapSelection.PartnerSelect(partner)
                            true
                        }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GoldCrown,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = partner.name,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Overlay: Live Telemetry Bar & Category Filters
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            // Live GPS & Satellite Telemetry Pill
            activeBus?.let { bus ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiveStatusBadge(text = "LIVE GPS", color = Emerald500)
                        Text(
                            text = "🛰️ ${bus.satelliteCount} спутников",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "±${bus.gpsAccuracyM}м",
                            fontSize = 11.sp,
                            color = Emerald600,
                            fontWeight = FontWeight.Bold
                        )
                        Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${bus.altitudeM}м над ур. моря",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MapFilterCategory.values()) { category ->
                    val isSelected = selectedCategory == category
                    val label = when (language) {
                        AppLanguage.TJ -> category.titleTj
                        AppLanguage.EN -> category.titleEn
                        AppLanguage.RU -> category.titleRu
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(category.icon, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald600,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }

        // Floating Action Buttons (Right side: Map Layers, My Location, Center Route)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Map Layers Toggle
            FloatingActionButton(
                onClick = { showLayerDialog = true },
                modifier = Modifier.size(46.dp).testTag("map_layer_btn"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Слои карты", modifier = Modifier.size(22.dp))
            }

            // My Location
            FloatingActionButton(
                onClick = {
                    if (hasLocationPermission) {
                        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                userLocation = loc
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f)
                                    )
                                }
                            }
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier.size(46.dp).testTag("map_my_location_btn"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Emerald600,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Моя локация", modifier = Modifier.size(22.dp))
            }

            // Zoom to Full Hop-On Hop-Off Route
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        val boundsBuilder = LatLngBounds.builder()
                        stops.forEach { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80)
                        )
                    }
                },
                modifier = Modifier.size(46.dp).testTag("map_fit_route_btn"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BusRed,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.AltRoute, contentDescription = "Весь маршрут", modifier = Modifier.size(22.dp))
            }

            // Auto Voice Guide Toggle
            FloatingActionButton(
                onClick = onToggleAutoGuide,
                modifier = Modifier.size(46.dp).testTag("map_auto_guide_btn"),
                containerColor = if (isAutoGuideEnabled) Emerald600 else MaterialTheme.colorScheme.surface,
                contentColor = if (isAutoGuideEnabled) Color.White else MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = if (isAutoGuideEnabled) Icons.Default.Campaign else Icons.Default.Headphones,
                    contentDescription = "Голосовой гид",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Auto Guide Floating Status Pill
        if (isAutoGuideEnabled) {
            Surface(
                color = Emerald600,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
                    .clickable { onToggleAutoGuide() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🎙️ Автогид тура включён",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Map Layer Selection Dialog
        if (showLayerDialog) {
            AlertDialog(
                onDismissRequest = { showLayerDialog = false },
                title = { Text("Слой карты Google Maps", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentMapType = MapType.NORMAL
                                    showLayerDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = currentMapType == MapType.NORMAL,
                                onClick = {
                                    currentMapType = MapType.NORMAL
                                    showLayerDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🗺️ Обычная схема города (Default)", fontSize = 14.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentMapType = MapType.HYBRID
                                    showLayerDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = currentMapType == MapType.HYBRID,
                                onClick = {
                                    currentMapType = MapType.HYBRID
                                    showLayerDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🛰️ Спутник высокого разрешения (Google Satellite)", fontSize = 14.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentMapType = MapType.TERRAIN
                                    showLayerDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = currentMapType == MapType.TERRAIN,
                                onClick = {
                                    currentMapType = MapType.TERRAIN
                                    showLayerDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("⛰️ Рельеф и ландшафт (Terrain)", fontSize = 14.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLayerDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }

        // Bottom Details Card for Selected Object
        selectedItem?.let { selection ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("map_detail_sheet")
                ) {
                    when (selection) {
                        is MapSelection.BusSelect -> {
                            val bus = selection.bus
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(BusRed),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(bus.busNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("Водитель: ${bus.driverName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { selectedItem = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Скорость", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${bus.speedKmH} км/ч", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Column {
                                        Text("Госномер", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(bus.plateNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Column {
                                        Text("Занято мест", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${bus.currentPassengers} из ${bus.capacity}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emerald600)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onAnnounceBus(bus) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BusRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("📢 Озвучить статус автобуса", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        is MapSelection.StopSelect -> {
                            val stop = selection.stop
                            val stopName = when (language) {
                                AppLanguage.TJ -> stop.nameTj
                                AppLanguage.EN -> stop.nameEn
                                AppLanguage.RU -> stop.nameRu
                            }
                            val stopDesc = when (language) {
                                AppLanguage.TJ -> stop.descriptionTj
                                AppLanguage.EN -> stop.descriptionEn
                                AppLanguage.RU -> stop.descriptionRu
                            }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = BusRed,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${stop.stopOrder}", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(stopName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Остановка #${stop.stopOrder} кругового тура", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { selectedItem = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stopDesc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)

                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Emerald600.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Emerald600, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Следующий автобус", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text("через ${stop.nextBusMinutes} мин", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Emerald600)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onPlayStopAudio(stop) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Голосовой гид", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(LatLng(stop.latitude, stop.longitude), 17f)
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        is MapSelection.AttractionSelect -> {
                            val attr = selection.attraction
                            val attrName = when (language) {
                                AppLanguage.TJ -> attr.nameTj
                                AppLanguage.EN -> attr.nameEn
                                AppLanguage.RU -> attr.nameRu
                            }
                            val attrDesc = when (language) {
                                AppLanguage.TJ -> attr.descriptionTj
                                AppLanguage.EN -> attr.descriptionEn
                                AppLanguage.RU -> attr.descriptionRu
                            }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(attrName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(attr.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { selectedItem = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(attrDesc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { onPlayAudioGuide(attr) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.listenAudioGuide, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(LatLng(attr.latitude, attr.longitude), 17f)
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        is MapSelection.PartnerSelect -> {
                            val partner = selection.partner
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(partner.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${partner.category} • ${partner.address}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { selectedItem = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GoldCrown.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🎁", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Скидка ${partner.discountPercent}% по билету Dushanbe Tour", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
