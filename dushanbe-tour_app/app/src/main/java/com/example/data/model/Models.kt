package com.example.data.model

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    RU("ru", "Русский", "🇷🇺"),
    TJ("tj", "Тоҷикӣ", "🇹🇯"),
    EN("en", "English", "🇬🇧")
}

enum class AttractionCategory(val code: String, val labelRu: String, val labelTj: String, val labelEn: String) {
    ALL("ALL", "Все", "Ҳама", "All"),
    MONUMENT("MONUMENT", "Памятники", "Ёдгориҳо", "Monuments"),
    MUSEUM("MUSEUM", "Музеи", "Осорхонаҳо", "Museums"),
    PARK("PARK", "Парки", "Боғҳо", "Parks"),
    ARCHITECTURE("ARCHITECTURE", "Архитектура", "Меъморӣ", "Architecture"),
    CULTURE("CULTURE", "Культура", "Фарҳанг", "Culture")
}

enum class PartnerCategory(val code: String, val labelRu: String, val labelTj: String, val labelEn: String) {
    ALL("ALL", "Все", "Ҳама", "All"),
    RESTAURANT("RESTAURANT", "Рестораны", "Ошхонаҳо", "Restaurants"),
    HOTEL("HOTEL", "Отели", "Меҳмонхонаҳо", "Hotels"),
    MUSEUM("MUSEUM", "Музеи", "Осорхонаҳо", "Museums"),
    TRAVEL("TRAVEL", "Тур агентства", "Агентиҳои сайёҳӣ", "Travel Agencies"),
    SOUVENIR("SOUVENIR", "Сувениры", "Тӯҳфаҳо", "Souvenirs"),
    EXCURSION("EXCURSION", "Экскурсии", "Сайрҳо", "Excursions")
}

enum class BusStatus(val labelRu: String, val colorHex: Long) {
    ACTIVE("На линии", 0xFF10B981),
    IN_DEPOT("В парке", 0xFF6B7280),
    MAINTENANCE("Техосмотр", 0xFFF59E0B)
}

enum class PaymentMethod(val id: String, val title: String, val desc: String, val iconName: String) {
    KORTI_MILLI("korti_milli", "Корти Миллӣ", "Экспресс оплата через НПС Корти Милли", "credit_card"),
    ALIF_SALOM("alif_salom", "Alif Salom / DC", "Оплата через Alif / Dushanbe City", "account_balance_wallet"),
    VISA_MASTERCARD("visa_mastercard", "Visa / Mastercard", "Международные банковские карты", "payment")
}

data class AudioTrack(
    val attractionId: String,
    val attractionName: String,
    val audioDurationSec: Int,
    val script: String,
    val language: AppLanguage,
    val categoryLabel: String = "Аудиоэкскурсия"
)

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val currentTrack: AudioTrack? = null,
    val currentPositionSec: Int = 0,
    val durationSec: Int = 0,
    val speed: Float = 1.0f,
    val language: AppLanguage = AppLanguage.RU,
    val isAutoGuideEnabled: Boolean = false
)

data class TourStatistics(
    val totalUsers: Int = 1420,
    val totalTicketsSold: Int = 384,
    val grossRevenueTjs: Double = 34560.0,
    val activeTripsToday: Int = 92,
    val activeBusesCount: Int = 3,
    val busFleetTotal: Int = 4,
    val averageBusOccupancyPercent: Int = 74,
    val topStopName: String = "Пл. Исмоили Сомони",
    val topAttractionName: String = "Национальный Музей Таджикистана"
)
