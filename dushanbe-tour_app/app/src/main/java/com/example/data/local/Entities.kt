package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buses")
data class BusEntity(
    @PrimaryKey val id: String,
    val busNumber: String,
    val plateNumber: String,
    val status: String, // ACTIVE, IN_DEPOT, MAINTENANCE
    val currentLat: Double,
    val currentLng: Double,
    val speedKmH: Int,
    val heading: Float,
    val nextStopName: String,
    val estimatedArrivalMin: Int,
    val capacity: Int,
    val currentPassengers: Int,
    val satelliteCount: Int = 14,
    val gpsAccuracyM: Float = 1.8f,
    val altitudeM: Double = 824.0,
    val driverName: String = "Алишер Раҳимов"
)

@Entity(tableName = "bus_stops")
data class BusStopEntity(
    @PrimaryKey val id: String,
    val stopOrder: Int,
    val nameRu: String,
    val nameTj: String,
    val nameEn: String,
    val descriptionRu: String,
    val descriptionTj: String,
    val descriptionEn: String,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String,
    val nearbyAttractions: String, // comma-separated names
    val nextBusMinutes: Int
)

@Entity(tableName = "attractions")
data class AttractionEntity(
    @PrimaryKey val id: String,
    val nameRu: String,
    val nameTj: String,
    val nameEn: String,
    val descriptionRu: String,
    val descriptionTj: String,
    val descriptionEn: String,
    val category: String, // MONUMENT, MUSEUM, PARK, ARCHITECTURE, CULTURE
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val workingHours: String,
    val photoUrl: String,
    val audioDurationSec: Int,
    val audioScriptRu: String,
    val audioScriptTj: String,
    val audioScriptEn: String,
    val rating: Double,
    val reviewCount: Int,
    val isFavorite: Boolean = false
)

@Entity(tableName = "tariffs")
data class TariffEntity(
    @PrimaryKey val id: String,
    val code: String, // TARIFF_24H, TARIFF_48H, TARIFF_72H, TARIFF_FAMILY
    val nameRu: String,
    val nameTj: String,
    val nameEn: String,
    val durationHours: Int,
    val priceTjs: Double,
    val isFamily: Boolean,
    val isPopular: Boolean,
    val isActive: Boolean,
    val featuresRu: String // comma-separated features
)

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
    val ticketNumber: String,
    val tariffId: String,
    val tariffName: String,
    val passengerName: String,
    val passengerPhone: String,
    val passengerEmail: String,
    val purchaseTimestamp: Long,
    val validFrom: Long,
    val validUntil: Long,
    val qrCodeData: String,
    val status: String, // ACTIVE, USED, EXPIRED
    val pricePaidTjs: Double
)

@Entity(tableName = "partners")
data class PartnerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // RESTAURANT, HOTEL, MUSEUM, TRAVEL, SOUVENIR, EXCURSION
    val descriptionRu: String,
    val descriptionTj: String,
    val descriptionEn: String,
    val discountPercent: Int,
    val discountDescription: String,
    val promoCode: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String,
    val isFeatured: Boolean
)

@Entity(tableName = "notifications")
data class AppNotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
    val type: String // BUS_ARRIVAL, PROXIMITY, PARTNER_DISCOUNT, TICKET_EXPIRY
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val language: String, // ru, tj, en
    val isAdmin: Boolean,
    val notificationsEnabled: Boolean
)
