package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.*
import com.example.data.model.AppLanguage
import com.example.ui.components.LiveStatusBadge
import com.example.ui.theme.BusRed
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GoldCrown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    buses: List<BusEntity>,
    stops: List<BusStopEntity>,
    tariffs: List<TariffEntity>,
    attractions: List<AttractionEntity>,
    partners: List<PartnerEntity>,
    tickets: List<TicketEntity>,
    onUpdateTariffPrice: (String, Double) -> Unit,
    onToggleBusStatus: (String, String) -> Unit,
    onAddBus: (String, String) -> Unit,
    onSetSimulationMultiplier: (Int) -> Unit = {},
    onToggleSimulation: (Boolean) -> Unit = {},
    onBroadcastNotification: (String, String) -> Unit = { _, _ -> },
    onRevokeTicket: (String) -> Unit = {},
    onActivateTicket: (String) -> Unit = {},
    onUpdateAttractionAudio: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onLogout: () -> Unit = {},
    onBack: () -> Unit
) {
    // 0: GPS & Диспетчер, 1: Тарифы, 2: Автопарк, 3: Билеты, 4: Оповещения, 5: Аудиогид
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("🛰️ GPS Диспетчер", "🏷️ Тарифы", "🚌 Автопарк", "🎟️ Билеты", "📢 Оповещения", "🎧 Аудиогид")

    // State for editing tariff
    var editingTariff by remember { mutableStateOf<TariffEntity?>(null) }
    var newPriceText by remember { mutableStateOf("") }

    // State for adding bus
    var showAddBusDialog by remember { mutableStateOf(false) }
    var newBusNumber by remember { mutableStateOf("") }
    var newBusPlate by remember { mutableStateOf("") }

    // State for sending broadcast
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var showBroadcastSuccess by remember { mutableStateOf(false) }

    // Ticket search query
    var ticketSearchQuery by remember { mutableStateOf("") }

    // Simulation multiplier state
    var currentMultiplier by remember { mutableStateOf(1) }
    var isSimRunning by remember { mutableStateOf(true) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Панель Диспетчера", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Emerald600.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "ROOT ADMIN",
                                    color = Emerald600,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("Dushanbe Tour Central CMS", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Lock / Logout Button
                    FilledTonalButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Выйти", fontSize = 12.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Scrollable Tab Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                // Tab 0: Satellite GPS & Fleet Telemetry
                0 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // Satellite Live Stream Status Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSimRunning) Emerald600 else BusRed)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                if (isSimRunning) "Спутниковый канал АКТИВЕН" else "Спутниковый поток ПРИОСТАНОВЛЕН",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Switch(
                                            checked = isSimRunning,
                                            onCheckedChange = {
                                                isSimRunning = it
                                                onToggleSimulation(it)
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("• Группировка: GPS + GLONASS (14 спутников в захвате)", fontSize = 12.sp)
                                    Text("• Точность позиционирования: HDOP 0.9 (±1.4 м)", fontSize = 12.sp)
                                    Text("• Высота над уровнем моря: 824 м (Долина Душанбе)", fontSize = 12.sp)
                                    Text("• Период обновления телеметрии: 2.5 сек", fontSize = 12.sp)

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("Скорость симуляции движения:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        listOf(1, 2, 5, 10).forEach { mult ->
                                            FilterChip(
                                                selected = currentMultiplier == mult,
                                                onClick = {
                                                    currentMultiplier = mult
                                                    onSetSimulationMultiplier(mult)
                                                },
                                                label = { Text("${mult}x") }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Телеметрия автобусов на линии", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        items(buses) { bus ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = BusRed)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(bus.busNumber, fontWeight = FontWeight.Bold)
                                        }
                                        LiveStatusBadge(
                                            text = when (bus.status) {
                                                "ACTIVE" -> "На линии"
                                                "IN_DEPOT" -> "В парке"
                                                else -> "Техосмотр"
                                            },
                                            color = if (bus.status == "ACTIVE") Emerald500 else BusRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Госномер: ${bus.plateNumber} • Водитель: ${bus.driverName}", fontSize = 12.sp)
                                    Text("GPS: ${"%.4f".format(bus.currentLat)}, ${"%.4f".format(bus.currentLng)} • Курс: ${bus.heading.toInt()}°", fontSize = 12.sp)
                                    Text("Скорость: ${bus.speedKmH} км/ч • Спутники: ${bus.satelliteCount} шт.", fontSize = 12.sp)
                                    Text("Следующая остановка: ${bus.nextStopName} (ETA ~${bus.estimatedArrivalMin} мин)", fontSize = 12.sp, color = Emerald600)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onToggleBusStatus(bus.id, if (bus.status == "ACTIVE") "IN_DEPOT" else "ACTIVE") },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (bus.status == "ACTIVE") "В депо" else "На линию", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { onToggleBusStatus(bus.id, "MAINTENANCE") },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("ТО", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 1: Tariffs & Pricing
                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("Управление тарифами и ценами", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Изменение стоимости применяется мгновенно для всех туристов.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        items(tariffs) { tariff ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tariff.nameRu, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${tariff.durationHours} часов действия", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "${tariff.priceTjs.toInt()} TJS",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Emerald600,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            editingTariff = tariff
                                            newPriceText = tariff.priceTjs.toInt().toString()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Изменить")
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 2: Fleet Management (Add bus)
                2 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Автопарк Hop-On Hop-Off", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Button(
                                    onClick = { showAddBusDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BusRed)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Добавить")
                                }
                            }
                        }

                        items(buses) { bus ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(BusRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bus.busNumber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("Госномер: ${bus.plateNumber}", fontSize = 12.sp)
                                        Text("Вместимость: ${bus.capacity} чел • Пассажиров: ${bus.currentPassengers}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    LiveStatusBadge(
                                        text = if (bus.status == "ACTIVE") "На линии" else "В парке",
                                        color = if (bus.status == "ACTIVE") Emerald500 else BusRed
                                    )
                                }
                            }
                        }
                    }
                }

                // Tab 3: Tickets Inspection
                3 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        OutlinedTextField(
                            value = ticketSearchQuery,
                            onValueChange = { ticketSearchQuery = it },
                            placeholder = { Text("Поиск по номеру билета или имени...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val filteredTickets = remember(tickets, ticketSearchQuery) {
                            if (ticketSearchQuery.isBlank()) tickets
                            else tickets.filter {
                                it.ticketNumber.contains(ticketSearchQuery, ignoreCase = true) ||
                                it.passengerName.contains(ticketSearchQuery, ignoreCase = true)
                            }
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filteredTickets) { ticket ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("№ ${ticket.ticketNumber}", fontWeight = FontWeight.Bold)
                                            Text("${ticket.pricePaidTjs.toInt()} TJS", fontWeight = FontWeight.Black, color = Emerald600)
                                        }
                                        Text("Пассажир: ${ticket.passengerName} (${ticket.passengerPhone})", fontSize = 12.sp)
                                        Text("Тариф: ${ticket.tariffName} • Статус: ${ticket.status}", fontSize = 12.sp)
                                        Text("Куплен: ${dateFormat.format(Date(ticket.purchaseTimestamp))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (ticket.status == "ACTIVE") {
                                                OutlinedButton(
                                                    onClick = { onRevokeTicket(ticket.id) },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BusRed)
                                                ) {
                                                    Text("Аннулировать", fontSize = 11.sp)
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = { onActivateTicket(ticket.id) },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald600)
                                                ) {
                                                    Text("Активировать", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 4: Push Broadcast Alerts
                4 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Рассылка оповещений туристам", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Сообщение отобразится в шторке уведомлений и в центре сообщений всех пользователей.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it },
                            label = { Text("Заголовок оповещения") },
                            placeholder = { Text("Например: Изменение в расписании маршрута") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = broadcastMessage,
                            onValueChange = { broadcastMessage = it },
                            label = { Text("Текст сообщения") },
                            placeholder = { Text("Автобус №2 временно изменил траекторию в районе Парка Рудаки...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (broadcastTitle.isNotBlank() && broadcastMessage.isNotBlank()) {
                                    onBroadcastNotification(broadcastTitle, broadcastMessage)
                                    broadcastTitle = ""
                                    broadcastMessage = ""
                                    showBroadcastSuccess = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Отправить оповещение")
                        }

                        if (showBroadcastSuccess) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald600.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "✓ Оповещение успешно отправлено на все устройства!",
                                    color = Emerald600,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Tab 5: Audio Guide CMS
                5 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text("Тексты аудиогида по достопримечательностям", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        items(attractions) { attr ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(attr.nameRu, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("TJ: ${attr.nameTj} • EN: ${attr.nameEn}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("RU скрипт: ${attr.audioScriptRu.take(120)}...", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Длительность: ${attr.audioDurationSec} сек", fontSize = 11.sp, color = Emerald600, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Tariff Price Edit Dialog
    editingTariff?.let { tariff ->
        AlertDialog(
            onDismissRequest = { editingTariff = null },
            title = { Text("Изменить цену: ${tariff.nameRu}") },
            text = {
                Column {
                    Text("Укажите новую стоимость в таджикских сомони (TJS):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPriceText,
                        onValueChange = { newPriceText = it },
                        label = { Text("Цена (TJS)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val price = newPriceText.toDoubleOrNull()
                    if (price != null && price > 0) {
                        onUpdateTariffPrice(tariff.id, price)
                        editingTariff = null
                    }
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTariff = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Add Bus Dialog
    if (showAddBusDialog) {
        AlertDialog(
            onDismissRequest = { showAddBusDialog = false },
            title = { Text("Добавить автобус в автопарк") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newBusNumber,
                        onValueChange = { newBusNumber = it },
                        label = { Text("Название (напр. Автобус #05)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newBusPlate,
                        onValueChange = { newBusPlate = it },
                        label = { Text("Госномер (напр. 0105 DH 01)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newBusNumber.isNotBlank() && newBusPlate.isNotBlank()) {
                        onAddBus(newBusNumber, newBusPlate)
                        showAddBusDialog = false
                        newBusNumber = ""
                        newBusPlate = ""
                    }
                }) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBusDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
