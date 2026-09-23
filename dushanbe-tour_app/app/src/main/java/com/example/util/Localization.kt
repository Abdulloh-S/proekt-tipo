package com.example.util

import com.example.data.model.AppLanguage

class Strings(val lang: AppLanguage) {
    // App header
    val appTitle: String = "Dushanbe Tour"
    val appSubtitle: String = when (lang) {
        AppLanguage.TJ -> "Хатсайри сайёҳии Душанбе"
        AppLanguage.EN -> "Tajikistan Hop-On Hop-Off"
        AppLanguage.RU -> "Туристический автобус Душанбе"
    }

    // Navigation
    val navHome: String = when (lang) {
        AppLanguage.TJ -> "Асосӣ"
        AppLanguage.EN -> "Home"
        AppLanguage.RU -> "Главная"
    }
    val navMap: String = when (lang) {
        AppLanguage.TJ -> "Харита"
        AppLanguage.EN -> "Map"
        AppLanguage.RU -> "Карта"
    }
    val navRoute: String = when (lang) {
        AppLanguage.TJ -> "Масир"
        AppLanguage.EN -> "Route"
        AppLanguage.RU -> "Маршрут"
    }
    val navTickets: String = when (lang) {
        AppLanguage.TJ -> "Чиптаҳо"
        AppLanguage.EN -> "Tickets"
        AppLanguage.RU -> "Билеты"
    }
    val navAttractions: String = when (lang) {
        AppLanguage.TJ -> "Маконҳо"
        AppLanguage.EN -> "Places"
        AppLanguage.RU -> "Места"
    }
    val navPartners: String = when (lang) {
        AppLanguage.TJ -> "Тахфифҳо"
        AppLanguage.EN -> "Discounts"
        AppLanguage.RU -> "Скидки"
    }
    val navProfile: String = when (lang) {
        AppLanguage.TJ -> "Профил"
        AppLanguage.EN -> "Profile"
        AppLanguage.RU -> "Профиль"
    }

    // Home screen
    val nearestStopTitle: String = when (lang) {
        AppLanguage.TJ -> "Истгоҳи наздиктарин"
        AppLanguage.EN -> "Nearest Stop"
        AppLanguage.RU -> "Ближайшая остановка"
    }
    val nearestBusTitle: String = when (lang) {
        AppLanguage.TJ -> "Автобуси наздиктарин"
        AppLanguage.EN -> "Next Bus"
        AppLanguage.RU -> "Ближайший автобус"
    }
    val arrivalInMin: String = when (lang) {
        AppLanguage.TJ -> "дақиқа пас мерасад"
        AppLanguage.EN -> "min arrival"
        AppLanguage.RU -> "мин прибытие"
    }
    val buyTicketBtn: String = when (lang) {
        AppLanguage.TJ -> "Харидани чипта"
        AppLanguage.EN -> "Buy Ticket"
        AppLanguage.RU -> "Купить билет"
    }
    val openMapBtn: String = when (lang) {
        AppLanguage.TJ -> "Кушодани харита"
        AppLanguage.EN -> "Open Map"
        AppLanguage.RU -> "Открыть карту"
    }
    val popularAttractions: String = when (lang) {
        AppLanguage.TJ -> "Ҷойҳои машҳур"
        AppLanguage.EN -> "Top Attractions"
        AppLanguage.RU -> "Популярные места"
    }
    val partnerDiscounts: String = when (lang) {
        AppLanguage.TJ -> "Тахфифҳои шарикон"
        AppLanguage.EN -> "Partner Offers"
        AppLanguage.RU -> "Скидки партнёров"
    }
    val activeTicketBadge: String = when (lang) {
        AppLanguage.TJ -> "Чиптаи фаъол"
        AppLanguage.EN -> "Active Ticket"
        AppLanguage.RU -> "Активный билет"
    }
    val showQrCode: String = when (lang) {
        AppLanguage.TJ -> "Нишон додани QR-код"
        AppLanguage.EN -> "Show QR Code"
        AppLanguage.RU -> "Показать QR-код"
    }
    val listenAudioGuide: String = when (lang) {
        AppLanguage.TJ -> "Роҳнамои овозӣ"
        AppLanguage.EN -> "Audio Guide"
        AppLanguage.RU -> "Аудиогид"
    }

    // Map screen
    val searchPlaceholder: String = when (lang) {
        AppLanguage.TJ -> "Ҷустуҷӯи мавзеъ ё истгоҳ..."
        AppLanguage.EN -> "Search places or stops..."
        AppLanguage.RU -> "Поиск места или остановки..."
    }
    val layerScheme: String = when (lang) {
        AppLanguage.TJ -> "Схема"
        AppLanguage.EN -> "Map"
        AppLanguage.RU -> "Схема"
    }
    val layerSatellite: String = when (lang) {
        AppLanguage.TJ -> "Спутник"
        AppLanguage.EN -> "Satellite"
        AppLanguage.RU -> "Спутник"
    }
    val myLocation: String = when (lang) {
        AppLanguage.TJ -> "Макони ман"
        AppLanguage.EN -> "My Location"
        AppLanguage.RU -> "Моё место"
    }
    val centerDushanbe: String = when (lang) {
        AppLanguage.TJ -> "Маркази Душанбе"
        AppLanguage.EN -> "Dushanbe Center"
        AppLanguage.RU -> "Центр Душанбе"
    }
    val satelliteGpsLive: String = when (lang) {
        AppLanguage.TJ -> "GPS/ГЛОНАСС фаъол"
        AppLanguage.EN -> "Satellite GPS Live"
        AppLanguage.RU -> "Спутниковый GPS Live"
    }
    val liveBusesCount: String = when (lang) {
        AppLanguage.TJ -> "Автобус дар хат"
        AppLanguage.EN -> "Buses on line"
        AppLanguage.RU -> "Автобусов на линии"
    }

    // Route screen
    val circularRouteTitle: String = when (lang) {
        AppLanguage.TJ -> "Хатсайри доиравии Душанбе"
        AppLanguage.EN -> "Circular Hop-On Loop"
        AppLanguage.RU -> "Круговой маршрут Hop-On"
    }
    val totalStopsCount: String = when (lang) {
        AppLanguage.TJ -> "8 истгоҳи асосӣ"
        AppLanguage.EN -> "8 major stops"
        AppLanguage.RU -> "8 ключевых остановок"
    }
    val waitingTime: String = when (lang) {
        AppLanguage.TJ -> "Вақти интизорӣ"
        AppLanguage.EN -> "Wait time"
        AppLanguage.RU -> "Время ожидания"
    }
    val nearbyTitle: String = when (lang) {
        AppLanguage.TJ -> "Дар наздикии истгоҳ"
        AppLanguage.EN -> "Nearby places"
        AppLanguage.RU -> "Рядом с остановкой"
    }

    // Tickets screen
    val selectTariff: String = when (lang) {
        AppLanguage.TJ -> "Тарофаи муносибро интихоб намоед"
        AppLanguage.EN -> "Select your tour pass"
        AppLanguage.RU -> "Выберите подходящий тариф"
    }
    val payTicketBtn: String = when (lang) {
        AppLanguage.TJ -> "Пардохт кардан"
        AppLanguage.EN -> "Pay Now"
        AppLanguage.RU -> "Оплатить"
    }
    val myTicketsTab: String = when (lang) {
        AppLanguage.TJ -> "Чиптаҳои ман"
        AppLanguage.EN -> "My Tickets"
        AppLanguage.RU -> "Мои билеты"
    }
    val buyPassTab: String = when (lang) {
        AppLanguage.TJ -> "Харид"
        AppLanguage.EN -> "Purchase"
        AppLanguage.RU -> "Покупка"
    }
    val paymentSuccess: String = when (lang) {
        AppLanguage.TJ -> "Пардохт бомуваффақият анҷом ёфт!"
        AppLanguage.EN -> "Payment successful!"
        AppLanguage.RU -> "Оплата прошла успешно!"
    }

    // QR Ticket
    val boardingPass: String = when (lang) {
        AppLanguage.TJ -> "Варақаи нишаст (QR Чипта)"
        AppLanguage.EN -> "Digital Boarding Pass"
        AppLanguage.RU -> "Посадочный талон (QR)"
    }
    val showToDriver: String = when (lang) {
        AppLanguage.TJ -> "Ҳангоми савор шудан ба ронанда нишон диҳед"
        AppLanguage.EN -> "Scan when boarding the bus"
        AppLanguage.RU -> "Покажите водителю при посадке"
    }
    val testScanBtn: String = when (lang) {
        AppLanguage.TJ -> "Санҷиши чипта аз ҷониби ронанда"
        AppLanguage.EN -> "Driver Validation Test"
        AppLanguage.RU -> "Тест проверки билета водителем"
    }

    // Profile screen
    val profileTitle: String = when (lang) {
        AppLanguage.TJ -> "Маркази идоракунии сайёҳ"
        AppLanguage.EN -> "Traveler Center"
        AppLanguage.RU -> "Центр управления туриста"
    }
    val languageSection: String = when (lang) {
        AppLanguage.TJ -> "Забони барнома"
        AppLanguage.EN -> "Application Language"
        AppLanguage.RU -> "Язык приложения"
    }
    val adminPortalBtn: String = when (lang) {
        AppLanguage.TJ -> "Вуруд барои маъмурон ва диспетчер"
        AppLanguage.EN -> "Dispatcher & Admin Login"
        AppLanguage.RU -> "Вход для администратора / диспетчера"
    }
    val supportSection: String = when (lang) {
        AppLanguage.TJ -> "Маркази дастгирии сайёҳон"
        AppLanguage.EN -> "Tourist Support Hotline"
        AppLanguage.RU -> "Служба заботы о туристах"
    }
    val callHotline: String = when (lang) {
        AppLanguage.TJ -> "Занг задан (+992 44 600 00 00)"
        AppLanguage.EN -> "Call Hotline (+992 44 600 00 00)"
        AppLanguage.RU -> "Позвонить (+992 44 600 00 00)"
    }
}

fun appStrings(lang: AppLanguage): Strings = Strings(lang)
