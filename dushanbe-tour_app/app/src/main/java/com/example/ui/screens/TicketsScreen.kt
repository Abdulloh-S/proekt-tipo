package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.TariffEntity
import com.example.data.local.TicketEntity
import com.example.data.model.PaymentMethod
import com.example.ui.components.QrCodeCanvas
import com.example.ui.theme.BusRed
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GoldCrown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.data.model.AppLanguage
import com.example.util.appStrings

@Composable
fun TicketsScreen(
    tariffs: List<TariffEntity>,
    activeTicket: TicketEntity?,
    ticketHistory: List<TicketEntity>,
    onBuyTicket: (TariffEntity, String, String, String) -> Unit,
    onOpenQrTicket: (TicketEntity) -> Unit,
    language: AppLanguage = AppLanguage.RU,
    modifier: Modifier = Modifier
) {
    val strings = remember(language) { appStrings(language) }
    var selectedTariffForPurchase by remember { mutableStateOf<TariffEntity?>(null) }
    var passengerName by remember { mutableStateOf("Алишер Раҳимов") }
    var passengerPhone by remember { mutableStateOf("+992 900 12 34 56") }
    var passengerEmail by remember { mutableStateOf("tourist.tj@gmail.com") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.KORTI_MILLI) }

    var selectedTab by remember { mutableStateOf(0) } // 0: Тарифы, 1: Мои билеты

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("tickets_screen")
    ) {
        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Emerald600
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Купить билет", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Мои билеты (${ticketHistory.size})", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            // Tariffs List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Выберите тариф Hop-On Hop-Off",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Все тарифы включают безлимитный проезд, аудиогид и скидки у партнеров",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(tariffs.filter { it.isActive }) { tariff ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (tariff.isPopular) Emerald600.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (tariff.isPopular) 2.dp else 1.dp,
                                color = if (tariff.isPopular) Emerald600 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    if (tariff.isPopular) {
                                        Surface(
                                            color = Emerald600,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Text(
                                                text = "ХИТ ПРОДАЖ",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = tariff.nameRu,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${tariff.durationHours} часов активности",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${tariff.priceTjs.toInt()} TJS",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                        color = Emerald600
                                    )
                                    Text(
                                        text = "Сомони",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Features bullet list
                            tariff.featuresRu.split(",").forEach { feature ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Emerald600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = feature.trim(), style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { selectedTariffForPurchase = tariff },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tariff.isPopular) BusRed else Emerald600
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("buy_tariff_${tariff.code}")
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Купить за ${tariff.priceTjs.toInt()} TJS",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // My Tickets Tab
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (ticketHistory.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "У вас пока нет купленных билетов",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { selectedTab = 0 },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                                ) {
                                    Text("Выбрать билет")
                                }
                            }
                        }
                    }
                } else {
                    items(ticketHistory) { ticket ->
                        val isActive = ticket.status == "ACTIVE" && System.currentTimeMillis() < ticket.validUntil
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenQrTicket(ticket) }
                                .border(
                                    1.dp,
                                    if (isActive) Emerald600 else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mini QR
                                QrCodeCanvas(
                                    payload = ticket.qrCodeData,
                                    sizeDp = 70.dp,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ticket.tariffName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )

                                        Surface(
                                            color = if (isActive) Emerald600 else Color.Gray,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isActive) "АКТИВЕН" else "ИСТЁК",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "№ ${ticket.ticketNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Действует до: ${dateFormat.format(Date(ticket.validUntil))}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Пассажир: ${ticket.passengerName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Test Payment Checkout Bottom Sheet / Dialog
    selectedTariffForPurchase?.let { tariff ->
        AlertDialog(
            onDismissRequest = { selectedTariffForPurchase = null },
            title = {
                Text(
                    text = "Оплата билета «${tariff.nameRu}»",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Стоимость: ${tariff.priceTjs.toInt()} TJS (Таджикский сомони)",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Emerald600
                    )

                    OutlinedTextField(
                        value = passengerName,
                        onValueChange = { passengerName = it },
                        label = { Text("Имя и фамилия пассажира") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passengerPhone,
                        onValueChange = { passengerPhone = it },
                        label = { Text("Телефон (+992)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Способ оплаты:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    PaymentMethod.values().forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedPaymentMethod = method }
                                .background(
                                    if (selectedPaymentMethod == method) Emerald600.copy(alpha = 0.1f) else Color.Transparent
                                )
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == method,
                                onClick = { selectedPaymentMethod = method }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = method.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = method.desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBuyTicket(tariff, passengerName, passengerPhone, passengerEmail)
                        selectedTariffForPurchase = null
                        selectedTab = 1
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BusRed),
                    modifier = Modifier.testTag("confirm_test_payment_btn")
                ) {
                    Text("Оплатить ${tariff.priceTjs.toInt()} TJS (Тест)")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTariffForPurchase = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}
