package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BusEntity::class,
        BusStopEntity::class,
        AttractionEntity::class,
        TariffEntity::class,
        TicketEntity::class,
        PartnerEntity::class,
        AppNotificationEntity::class,
        UserEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun busDao(): BusDao
    abstract fun busStopDao(): BusStopDao
    abstract fun attractionDao(): AttractionDao
    abstract fun tariffDao(): TariffDao
    abstract fun ticketDao(): TicketDao
    abstract fun partnerDao(): PartnerDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dushanbe_tour_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
