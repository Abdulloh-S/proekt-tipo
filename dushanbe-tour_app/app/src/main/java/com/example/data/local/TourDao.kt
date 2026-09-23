package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BusDao {
    @Query("SELECT * FROM buses")
    fun getAllBusesFlow(): Flow<List<BusEntity>>

    @Query("SELECT * FROM buses")
    suspend fun getAllBuses(): List<BusEntity>

    @Query("SELECT * FROM buses WHERE id = :id")
    suspend fun getBusById(id: String): BusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBus(bus: BusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBuses(buses: List<BusEntity>)

    @Update
    suspend fun updateBus(bus: BusEntity)

    @Query("DELETE FROM buses WHERE id = :id")
    suspend fun deleteBus(id: String)
}

@Dao
interface BusStopDao {
    @Query("SELECT * FROM bus_stops ORDER BY stopOrder ASC")
    fun getAllStopsFlow(): Flow<List<BusStopEntity>>

    @Query("SELECT * FROM bus_stops ORDER BY stopOrder ASC")
    suspend fun getAllStops(): List<BusStopEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: BusStopEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStops(stops: List<BusStopEntity>)

    @Update
    suspend fun updateStop(stop: BusStopEntity)

    @Query("DELETE FROM bus_stops WHERE id = :id")
    suspend fun deleteStop(id: String)
}

@Dao
interface AttractionDao {
    @Query("SELECT * FROM attractions")
    fun getAllAttractionsFlow(): Flow<List<AttractionEntity>>

    @Query("SELECT * FROM attractions")
    suspend fun getAllAttractions(): List<AttractionEntity>

    @Query("SELECT * FROM attractions WHERE id = :id")
    suspend fun getAttractionById(id: String): AttractionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttraction(attraction: AttractionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttractions(attractions: List<AttractionEntity>)

    @Update
    suspend fun updateAttraction(attraction: AttractionEntity)

    @Query("UPDATE attractions SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: String, isFav: Boolean)

    @Query("DELETE FROM attractions WHERE id = :id")
    suspend fun deleteAttraction(id: String)
}

@Dao
interface TariffDao {
    @Query("SELECT * FROM tariffs ORDER BY priceTjs ASC")
    fun getAllTariffsFlow(): Flow<List<TariffEntity>>

    @Query("SELECT * FROM tariffs")
    suspend fun getAllTariffs(): List<TariffEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariff(tariff: TariffEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTariffs(tariffs: List<TariffEntity>)

    @Update
    suspend fun updateTariff(tariff: TariffEntity)

    @Query("DELETE FROM tariffs WHERE id = :id")
    suspend fun deleteTariff(id: String)
}

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY purchaseTimestamp DESC")
    fun getAllTicketsFlow(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE status = 'ACTIVE' ORDER BY purchaseTimestamp DESC LIMIT 1")
    fun getActiveTicketFlow(): Flow<TicketEntity?>

    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun getTicketById(id: String): TicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity)

    @Update
    suspend fun updateTicket(ticket: TicketEntity)

    @Query("UPDATE tickets SET status = :status WHERE id = :id")
    suspend fun updateTicketStatus(id: String, status: String)

    @Query("DELETE FROM tickets WHERE id = :id")
    suspend fun deleteTicket(id: String)
}

@Dao
interface PartnerDao {
    @Query("SELECT * FROM partners")
    fun getAllPartnersFlow(): Flow<List<PartnerEntity>>

    @Query("SELECT * FROM partners")
    suspend fun getAllPartners(): List<PartnerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: PartnerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPartners(partners: List<PartnerEntity>)

    @Update
    suspend fun updatePartner(partner: PartnerEntity)

    @Query("DELETE FROM partners WHERE id = :id")
    suspend fun deletePartner(id: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<AppNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}
