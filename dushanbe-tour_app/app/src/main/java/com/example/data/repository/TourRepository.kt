package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.model.TourStatistics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class TourRepository(private val db: AppDatabase, private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var simulationJob: Job? = null
    var isSimulationRunning = true
        private set

    val busesFlow: Flow<List<BusEntity>> = db.busDao().getAllBusesFlow()
    val stopsFlow: Flow<List<BusStopEntity>> = db.busStopDao().getAllStopsFlow()
    val attractionsFlow: Flow<List<AttractionEntity>> = db.attractionDao().getAllAttractionsFlow()
    val tariffsFlow: Flow<List<TariffEntity>> = db.tariffDao().getAllTariffsFlow()
    val ticketsFlow: Flow<List<TicketEntity>> = db.ticketDao().getAllTicketsFlow()
    val activeTicketFlow: Flow<TicketEntity?> = db.ticketDao().getActiveTicketFlow()
    val partnersFlow: Flow<List<PartnerEntity>> = db.partnerDao().getAllPartnersFlow()
    val notificationsFlow: Flow<List<AppNotificationEntity>> = db.notificationDao().getAllNotificationsFlow()
    val userFlow: Flow<UserEntity?> = db.userDao().getUserFlow()

    // Route waypoint path coordinates for circular Hop-On Hop-Off loop in Dushanbe
    val routeWaypoints = listOf(
        Pair(38.5739, 68.7844), // Somoni Sq
        Pair(38.5755, 68.7838),
        Pair(38.5772, 68.7831), // Rudaki Park & Flagpole
        Pair(38.5800, 68.7810),
        Pair(38.5831, 68.7778), // National Museum
        Pair(38.5850, 68.7735),
        Pair(38.5867, 68.7692), // Navruz Palace
        Pair(38.5910, 68.7720),
        Pair(38.5960, 68.7770),
        Pair(38.6012, 68.7825), // Botanical Garden
        Pair(38.5990, 68.7890),
        Pair(38.5954, 68.7942), // Mehrgon Market
        Pair(38.5880, 68.7930),
        Pair(38.5800, 68.7900),
        Pair(38.5714, 68.7865), // National Library
        Pair(38.5660, 68.7910),
        Pair(38.5623, 68.7967), // Ayni Opera Theatre
        Pair(38.5680, 68.7890),
        Pair(38.5739, 68.7844)  // Return to Somoni Sq
    )

    init {
        scope.launch {
            seedInitialDataIfEmpty()
            startBusGpsSimulation()
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        if (db.busStopDao().getAllStops().isEmpty()) {
            val initialStops = listOf(
                BusStopEntity(
                    id = "stop_1",
                    stopOrder = 1,
                    nameRu = "Площадь Исмоили Сомони",
                    nameTj = "Майдони Исмоили Сомонӣ",
                    nameEn = "Ismoil Somoni Square",
                    descriptionRu = "Главная историческая площадь столицы с грандиозной 13-метровой золотой аркой правителя династии Саманидов.",
                    descriptionTj = "Майдони асосии пойтахт бо аркаи тиллоии 13-метраи шоҳ Исмоили Сомонӣ.",
                    descriptionEn = "Central historic square with the iconic 13-meter golden arch monument of Ismail Samani.",
                    latitude = 38.5739,
                    longitude = 68.7844,
                    photoUrl = "somoni",
                    nearbyAttractions = "Памятник Исмоили Сомони, Дворец Нации, Парк Рудаки",
                    nextBusMinutes = 4
                ),
                BusStopEntity(
                    id = "stop_2",
                    stopOrder = 2,
                    nameRu = "Парк Рудаки и Площадь Флага",
                    nameTj = "Боғи Рӯдакӣ ва Майдони Парчам",
                    nameEn = "Rudaki Park & Flagpole",
                    descriptionRu = "Живописный парк с каскадными фонтанами, тенистыми аллеями и одним из высочайших флагштоков мира (165 м).",
                    descriptionTj = "Боғи сарсабз бо фаввораҳо ва яке аз баландтарин парчамҳои ҷаҳон (165 м).",
                    descriptionEn = "Picturesque park with fountain cascades and one of the world's tallest flagpoles (165m).",
                    latitude = 38.5772,
                    longitude = 68.7831,
                    photoUrl = "flagpole",
                    nearbyAttractions = "Флагшток Таджикистана, Памятник Рудаки, Аллея роз",
                    nextBusMinutes = 9
                ),
                BusStopEntity(
                    id = "stop_3",
                    stopOrder = 3,
                    nameRu = "Национальный Музей Таджикистана",
                    nameTj = "Осорхонаи Миллии Тоҷикистон",
                    nameEn = "National Museum of Tajikistan",
                    descriptionRu = "Современный 4-этажный музей с 22 выставочными залами, включая уникальную 13-метровую статую Спящего Будды в Нирване.",
                    descriptionTj = "Осорхонаи бузурги миллӣ бо 22 толори намоишӣ ва муҷассамаи Буддо дар ҳолати нирвона.",
                    descriptionEn = "Major national museum showcasing Silk Road antiquities and the 13m reclining Buddha of Ajina-Tepa.",
                    latitude = 38.5831,
                    longitude = 68.7778,
                    photoUrl = "museum",
                    nearbyAttractions = "Национальный Музей, Памятник гербу Таджикистана",
                    nextBusMinutes = 15
                ),
                BusStopEntity(
                    id = "stop_4",
                    stopOrder = 4,
                    nameRu = "Дворец Навруз (Кохи Навруз)",
                    nameTj = "Кохи Наврӯз",
                    nameEn = "Navruz Palace",
                    descriptionRu = "Архитектурное чудо современного Востока: дворцовый комплекс с уникальной резьбой по дереву, флорентийской мозаикой и зеркальными залами.",
                    descriptionTj = "Шоҳасари меъмории муосир бо кандакории нодири чӯб ва оинабандӣ.",
                    descriptionEn = "Architectural wonder featuring hand-carved cedar wood, Florentine mosaics and mirror halls.",
                    latitude = 38.5867,
                    longitude = 68.7692,
                    photoUrl = "navruz",
                    nearbyAttractions = "Кохи Навруз, Комсомольское озеро, Чайхана",
                    nextBusMinutes = 22
                ),
                BusStopEntity(
                    id = "stop_5",
                    stopOrder = 5,
                    nameRu = "Ботанический Сад «Боғи Ирам»",
                    nameTj = "Боғи Наботот «Боғи Ирам»",
                    nameEn = "Botanical Garden 'Bogi Iram'",
                    descriptionRu = "Оазис редких деревьев, традиционных резных таджикских беседок и этнографического комплекса под открытым небом.",
                    descriptionTj = "Боғи қадима бо дарахтони нодир ва суҳбатгоҳҳои миллӣ.",
                    descriptionEn = "Serene botanical garden with ancient trees and traditional carved wooden pavilions.",
                    latitude = 38.6012,
                    longitude = 68.7825,
                    photoUrl = "botanical",
                    nearbyAttractions = "Ботанический Сад, Этнографический павильон",
                    nextBusMinutes = 29
                ),
                BusStopEntity(
                    id = "stop_6",
                    stopOrder = 6,
                    nameRu = "Восточный Базар «Меҳргон»",
                    nameTj = "Бозори Шарқии «Меҳргон»",
                    nameEn = "Mehrgon Oriental Market",
                    descriptionRu = "Красочный центральный крытый базар: горные сухофрукты, знаменитые памирские орехи, душистые пряности и горячие лепешки.",
                    descriptionTj = "Бозори замонавӣ бо меваҳои хушк, хушбӯйҳо ва нони гарми тоҷикӣ.",
                    descriptionEn = "Vibrant multi-story covered bazaar packed with mountain dried fruits, spices and local delicacies.",
                    latitude = 38.5954,
                    longitude = 68.7942,
                    photoUrl = "mehrgon",
                    nearbyAttractions = "Базар Мехргон, Чайханы, Ремесленные ряды",
                    nextBusMinutes = 36
                ),
                BusStopEntity(
                    id = "stop_7",
                    stopOrder = 7,
                    nameRu = "Национальная Библиотека",
                    nameTj = "Китобхонаи Миллӣ",
                    nameEn = "National Library of Tajikistan",
                    descriptionRu = "Крупнейшая библиотека Центральной Азии в виде раскрытой книги с фасадом, украшенным скульптурами классиков персидской литературы.",
                    descriptionTj = "Бузургтарин китобхона дар Осиёи Марказӣ бо тарҳи китоби кушода.",
                    descriptionEn = "Largest library in Central Asia shaped like an open book with literary monuments.",
                    latitude = 38.5714,
                    longitude = 68.7865,
                    photoUrl = "library",
                    nearbyAttractions = "Национальная Библиотека, Государственный Цирк",
                    nextBusMinutes = 43
                ),
                BusStopEntity(
                    id = "stop_8",
                    stopOrder = 8,
                    nameRu = "Театр Оперы и Балета им. С. Айни",
                    nameTj = "Театри опера ва балети ба номи С. Айнӣ",
                    nameEn = "Ayni Opera & Ballet Theatre",
                    descriptionRu = "Жемчужина неоклассической архитектуры с колоннадой, фонтаном и живописной платановой аллеей на проспекте Рудаки.",
                    descriptionTj = "Бинои зебои таърихӣ бо сутунҳо ва фаввораи дилкаш дар хиёбони Рӯдакӣ.",
                    descriptionEn = "Neoclassical gem surrounded by plane trees, vibrant fountains and arts avenue.",
                    latitude = 38.5623,
                    longitude = 68.7967,
                    photoUrl = "theatre",
                    nearbyAttractions = "Театр Айни, Площадь 800-летия Москвы, Чайхана Рохат",
                    nextBusMinutes = 50
                )
            )
            db.busStopDao().insertAllStops(initialStops)
        }

        if (db.busDao().getAllBuses().isEmpty()) {
            val initialBuses = listOf(
                BusEntity(
                    id = "bus_101",
                    busNumber = "Автобус #01",
                    plateNumber = "0101 DH 01",
                    status = "ACTIVE",
                    currentLat = 38.5755,
                    currentLng = 68.7838,
                    speedKmH = 32,
                    heading = 25f,
                    nextStopName = "Парк Рудаки и Площадь Флага",
                    estimatedArrivalMin = 3,
                    capacity = 65,
                    currentPassengers = 38
                ),
                BusEntity(
                    id = "bus_102",
                    busNumber = "Автобус #02",
                    plateNumber = "0102 DH 01",
                    status = "ACTIVE",
                    currentLat = 38.5910,
                    currentLng = 68.7720,
                    speedKmH = 28,
                    heading = 45f,
                    nextStopName = "Ботанический Сад «Боғи Ирам»",
                    estimatedArrivalMin = 7,
                    capacity = 65,
                    currentPassengers = 45
                ),
                BusEntity(
                    id = "bus_103",
                    busNumber = "Автобус #03",
                    plateNumber = "0103 DH 01",
                    status = "ACTIVE",
                    currentLat = 38.5660,
                    currentLng = 68.7910,
                    speedKmH = 25,
                    heading = 190f,
                    nextStopName = "Театр Оперы и Балета им. Айни",
                    estimatedArrivalMin = 5,
                    capacity = 65,
                    currentPassengers = 29
                ),
                BusEntity(
                    id = "bus_104",
                    busNumber = "Автобус #04 (Резерв)",
                    plateNumber = "0104 DH 01",
                    status = "IN_DEPOT",
                    currentLat = 38.5550,
                    currentLng = 68.8050,
                    speedKmH = 0,
                    heading = 0f,
                    nextStopName = "Автопарк №1 Душанбе",
                    estimatedArrivalMin = 0,
                    capacity = 65,
                    currentPassengers = 0
                )
            )
            db.busDao().insertAllBuses(initialBuses)
        }

        if (db.attractionDao().getAllAttractions().isEmpty()) {
            val initialAttractions = listOf(
                AttractionEntity(
                    id = "attr_somoni",
                    nameRu = "Монумент Исмоили Сомони",
                    nameTj = "Муҷассамаи Исмоили Сомонӣ",
                    nameEn = "Ismail Somoni Monument",
                    descriptionRu = "Величественный символ таджикской государственности. Монумент основателя первого таджикского государства венчается 13-метровой аркой из чистого сусального золота и бронзовыми крылатыми барсами.",
                    descriptionTj = "Рамзи бузурги давлатдории тоҷикон. Аркаи тиллоии 13-метра ва муҷассамаи шоҳ Исмоили Сомонӣ.",
                    descriptionEn = "Majestic 13-meter bronze statue of the founder of the Samanid dynasty beneath an enormous golden arch.",
                    category = "MONUMENT",
                    latitude = 38.5739,
                    longitude = 68.7844,
                    address = "Проспект Рудаки, Площадь Дусти",
                    workingHours = "Круглосуточно, подсветка до 01:00",
                    photoUrl = "somoni",
                    audioDurationSec = 145,
                    audioScriptRu = "Добро пожаловать к монументу Исмоили Сомони! Этот величественный памятник высотой 13 метров символизирует возрождение древней таджикской государственности. Исмоил Сомони правил в 9-10 веках и создал процветающую державу Саманидов со столицей в Бухаре, покровительствуя науке, поэзии и искусствам. Арка над памятником покрыта настоящим сусальным золотом, а у подножия вас встречают стражи — бронзовые львы.",
                    audioScriptTj = "Хуш омадед ба муҷассамаи Исмоили Сомонӣ! Ин рамзи бузурги давлатдории тоҷикон буда, ба баландии 13 метр қомат афрохтааст. Шоҳ Исмоили Сомонӣ дар асрҳои 9 ва 10 салтанати бузурги Сомониёнро поягузорӣ карда, фарҳангу адабро ба авҷи аъло расонд.",
                    audioScriptEn = "Welcome to the Ismail Somoni monument! Standing proudly at 13 meters tall beneath a golden arch, this monument honors the 9th-century ruler of the Samanid Empire who united the region and fostered the golden age of Persian literature and science.",
                    rating = 4.9,
                    reviewCount = 1240,
                    isFavorite = true
                ),
                AttractionEntity(
                    id = "attr_museum",
                    nameRu = "Национальный Музей Таджикистана",
                    nameTj = "Осорхонаи Миллии Тоҷикистон",
                    nameEn = "National Museum of Tajikistan",
                    descriptionRu = "Главный музей страны с богатейшей коллекцией артефактов Великого Шелкового Пути, согдийских фресок городища Пенджикент и 13-метровой глиняной статуей Будды в Нирване из Аджина-Тепа.",
                    descriptionTj = "Бузургтарин осорхонаи кишвар бо беҳтарин ёдгориҳои таърихӣ, аз ҷумла Буддо дар ҳолати нирвона.",
                    descriptionEn = "Superb museum featuring ancient Silk Road relics, Sogdian frescoes and the famous 13m reclining Buddha.",
                    category = "MUSEUM",
                    latitude = 38.5831,
                    longitude = 68.7778,
                    address = "ул. Исмоили Сомони, 11",
                    workingHours = "Вт-Вс 10:00 - 18:00 (Пн - выходной)",
                    photoUrl = "museum",
                    audioDurationSec = 180,
                    audioScriptRu = "Вы находитесь перед Национальным музеем Таджикистана. Площадь экспозиций превышает 15 тысяч квадратных метров. Главное сокровище музея расположено в зале буддийского наследия — это гигантская терракотовая статуя Будды в Нирване 7 века длиной около 13 метров, обнаруженная археологами на холме Аджина-Тепа. Также здесь представлены редчайшие рукописи Авиценны, Омара Хайяма и сокровища древней Бактрии.",
                    audioScriptTj = "Шумо дар назди Осорхонаи Миллии Тоҷикистон қарор доред. Дар 22 толори он нодиртарин бозёфтҳои таърихӣ, аз даврони бостон то муосир, намоиш дода шудаанд. Яке аз гаронбаҳотарин экспонатҳо муҷассамаи 13-метраи Буддо аз Аҷинатеппа мебошад.",
                    audioScriptEn = "You are at the National Museum of Tajikistan. Spread across 22 spacious exhibition halls, the museum's centerpiece is a colossal 13-meter reclining Buddha statue from the 7th century monastery of Ajina-Tepa, along with ancient Bactrian and Sogdian treasures.",
                    rating = 4.8,
                    reviewCount = 980,
                    isFavorite = true
                ),
                AttractionEntity(
                    id = "attr_navruz",
                    nameRu = "Дворец Кохи Наврӯз",
                    nameTj = "Маҷмааи Кохи Наврӯз",
                    nameEn = "Koh-i Navruz Palace",
                    descriptionRu = "Грандиозный комплекс площадью 40 000 кв. метров. Каждая стена и потолок расписаны вручную народными мастерами Таджикистана: резьба по ганчу, кедровому дереву, полудрагоценные памирские камни и хрустальные люстры.",
                    descriptionTj = "Маҷмааи муҳташами фарҳангӣ бо кандакории нодири миллӣ, гачкорӣ ва санги лоҷвард.",
                    descriptionEn = "Stunning artisan palace spanning 40,000 sq meters, hand-crafted with intricate cedar carvings, plaster work and gemstones.",
                    category = "ARCHITECTURE",
                    latitude = 38.5867,
                    longitude = 68.7692,
                    address = "Проспект Исмоили Сомони, 146",
                    workingHours = "Ежедневно 09:00 - 22:00 (Экскурсии каждый час)",
                    photoUrl = "navruz",
                    audioDurationSec = 210,
                    audioScriptRu = "Кохи Навруз — подлинная восточная сказка наяву. Созданный руками более 4 тысяч лучших таджикских мастеров, дворец задумывался как чайхана, но превзошел все ожидания и стал парадной резиденцией для мировых саммитов. Обратите внимание на зал Зарандуд с сусальным золотом и зал Дидор с 3D-резьбой по дереву без единого гвоздя.",
                    audioScriptTj = "Кохи Наврӯз яке аз шоҳкориҳои беназири меъмории муосир аст. Ҳамаи толорҳои он бо дастони ҳунармандони чирадасти тоҷик кандакорӣ ва ороиш дода шудаанд.",
                    audioScriptEn = "Koh-i Navruz is an oriental masterpiece originally conceived as a teahouse and transformed into a palatial cultural landmark. Thousands of artisans carved local cedar wood and inlaid lapis lazuli and jasper from the Pamir mountains into its breathtaking halls.",
                    rating = 5.0,
                    reviewCount = 1530,
                    isFavorite = true
                ),
                AttractionEntity(
                    id = "attr_flagpole",
                    nameRu = "Государственный Флагшток и Парк Рудаки",
                    nameTj = "Парчами Давлатӣ ва Боғи Рӯдакӣ",
                    nameEn = "Dushanbe Flagpole & Rudaki Park",
                    descriptionRu = "Один из высочайших свободно стоящих флагштоков в мире высотой 165 метров с огромным полотнищем флага Таджикистана (60х30 м) посреди ландшафтного парка с озерами.",
                    descriptionTj = "Яке аз баландтарин парчамҳо дар ҷаҳон (165 метр) дар миёни боғи сарсабзи Рӯдакӣ.",
                    descriptionEn = "World-famous 165m freestanding flagpole flying a 60x30 meter Tajik flag beside peaceful park lakes.",
                    category = "PARK",
                    latitude = 38.5772,
                    longitude = 68.7831,
                    address = "Парк Рудаки, центр",
                    workingHours = "Круглосуточно",
                    photoUrl = "flagpole",
                    audioDurationSec = 130,
                    audioScriptRu = "Флагшток в Душанбе взмывает в небо на высоту 165 метров. Размеры развевающегося флага впечатляют — тридцать на шестьдесят метров, а вес ткани достигает 700 килограммов. Вокруг разбит уютный парк имени основоположника таджикско-персидской поэзии Абуабдуллоха Рудаки с аллеями роз и свето-музыкальными фонтанами.",
                    audioScriptTj = "Парчами давлатии Тоҷикистон бо баландии 165 метр яке аз баландтарин парчамҳои олам ба шумор меравад. Андозаи худи матои парчам 30 ба 60 метр буда, мояи ифтихори ҳар як шаҳрванди кишвар аст.",
                    audioScriptEn = "Rising 165 meters high, the Dushanbe Flagpole carries a colossal 60x30 meter flag weighing over 700 kg. The surrounding Rudaki Park offers cool walkways shaded by walnut and sycamore trees.",
                    rating = 4.7,
                    reviewCount = 820,
                    isFavorite = false
                ),
                AttractionEntity(
                    id = "attr_botanical",
                    nameRu = "Ботанический Сад «Боғи Ирам»",
                    nameTj = "Боғи Наботот «Боғи Ирам»",
                    nameEn = "Botanical Garden 'Bogi Iram'",
                    descriptionRu = "Уютный исторический ботанический сад, основанный в 1933 году. Здесь собраны более 4500 видов растений со всего мира, а также деревянный городок народных ремесел.",
                    descriptionTj = "Боғи қадимаи наботот бо беш аз 4500 намуди дарахтону гулҳо ва маҳаллаи ҳунарҳои мардумӣ.",
                    descriptionEn = "Established in 1933, home to over 4,500 species of flora and open-air traditional crafts pavilions.",
                    category = "PARK",
                    latitude = 38.6012,
                    longitude = 68.7825,
                    address = "ул. Карамова, 1",
                    workingHours = "Ежедневно 08:00 - 20:00",
                    photoUrl = "botanical",
                    audioDurationSec = 150,
                    audioScriptRu = "Боги Ирам в переводе означает 'Райский сад'. Это излюбленное место отдыха горожан. В глубине сада расположен традиционный амфитеатр и этнографическая деревня с деревянными резными воротами, где мастера демонстрируют ткачество, гончарное ремесло и чеканку.",
                    audioScriptTj = "Боғи Ирам яке аз зеботарин боғҳои пойтахт буда, дар он дарахтони нодир аз тамоми гӯшаву канори сайёра шинонда шудаанд. Инчунин дар ин ҷо суҳбатгоҳҳои чӯбини миллӣ ва осорхонаи кушода фаъолият мекунанд.",
                    audioScriptEn = "Bogi Iram translates to 'Garden of Eden'. Enjoy peaceful forest paths and a traditional ethnographic village showcasing Tajik wood carving and pottery.",
                    rating = 4.8,
                    reviewCount = 650,
                    isFavorite = false
                ),
                AttractionEntity(
                    id = "attr_mehrgon",
                    nameRu = "Восточный Базар Меҳргон",
                    nameTj = "Бозори Шарқии Меҳргон",
                    nameEn = "Mehrgon Central Bazaar",
                    descriptionRu = "Аутентичный трехэтажный базар под куполом из стекла и мрамора. Идеальное место для покупки кураги, изюма, горного меда, сумаляка и специй.",
                    descriptionTj = "Бозори замонавии сеошёна бо анвои гуногуни хушкмева, чормағз, асали кӯҳӣ ва шираворӣ.",
                    descriptionEn = "Iconic dome-roofed oriental market brimming with Pamir mountain walnuts, dried apricots, honey and spices.",
                    category = "CULTURE",
                    latitude = 38.5954,
                    longitude = 68.7942,
                    address = "ул. Мехргон, 1",
                    workingHours = "Ежедневно 07:00 - 19:00",
                    photoUrl = "mehrgon",
                    audioDurationSec = 140,
                    audioScriptRu = "Базар Мехргон — это праздник вкусов и ароматов Таджикистана. На первом этаже продаются знаменитые сладости и свежеиспеченные лепешки кулча, на втором — россыпи миндаля, фисташек и горного меда из ущелий Варзоба и Памира. Продавцы с радостью предложат вам все попробовать перед покупкой!",
                    audioScriptTj = "Бозори Меҳргон маркази меваҳои шаҳдбори тоҷикӣ аст. Дар ин ҷо шумо метавонед хушкмеваҳои беҳтарин ва маҳсулоти табиии кӯҳистонро харидорӣ намоед.",
                    audioScriptEn = "Mehrgon market offers the purest taste of Tajik hospitality. Explore overflowing pyramids of sun-dried apricots, mulberries from Badakhshan, and fragrant mountain herbs.",
                    rating = 4.9,
                    reviewCount = 1120,
                    isFavorite = false
                )
            )
            db.attractionDao().insertAllAttractions(initialAttractions)
        }

        if (db.tariffDao().getAllTariffs().isEmpty()) {
            val initialTariffs = listOf(
                TariffEntity(
                    id = "tariff_24h",
                    code = "TARIFF_24H",
                    nameRu = "Билет 24 часа",
                    nameTj = "Чиптаи 24 соата",
                    nameEn = "24-Hour Pass",
                    durationHours = 24,
                    priceTjs = 80.0,
                    isFamily = false,
                    isPopular = false,
                    isActive = true,
                    featuresRu = "Неограниченный Hop-On Hop-Off,Аудиогид на 3 языках,Карта маршрута,Скидки у партнеров"
                ),
                TariffEntity(
                    id = "tariff_48h",
                    code = "TARIFF_48H",
                    nameRu = "Билет 48 часов",
                    nameTj = "Чиптаи 48 соата",
                    nameEn = "48-Hour Pass",
                    durationHours = 48,
                    priceTjs = 120.0,
                    isFamily = false,
                    isPopular = true,
                    isActive = true,
                    featuresRu = "Неограниченный проезд 48 часов,Все маршруты и автобусы,Полный аудиогид,Скидка 15% в ресторанах,Экономия 40 TJS"
                ),
                TariffEntity(
                    id = "tariff_72h",
                    code = "TARIFF_72H",
                    nameRu = "Билет 72 часа (Максимум)",
                    nameTj = "Чиптаи 72 соата",
                    nameEn = "72-Hour Max Pass",
                    durationHours = 72,
                    priceTjs = 150.0,
                    isFamily = false,
                    isPopular = false,
                    isActive = true,
                    featuresRu = "3 дня безлимитных поездок,Приоритетная посадка,Входной билет в музей со скидкой,Партнерские промокоды"
                ),
                TariffEntity(
                    id = "tariff_family",
                    code = "TARIFF_FAMILY",
                    nameRu = "Семейный (Family 24h)",
                    nameTj = "Чиптаи Оилавӣ",
                    nameEn = "Family Pass",
                    durationHours = 24,
                    priceTjs = 250.0,
                    isFamily = true,
                    isPopular = false,
                    isActive = true,
                    featuresRu = "2 взрослых + до 3 детей,Сувенирный буклет в подарок,Детский аудиогид,Скидки в семейных кафе"
                )
            )
            db.tariffDao().insertAllTariffs(initialTariffs)
        }

        if (db.partnerDao().getAllPartners().isEmpty()) {
            val initialPartners = listOf(
                PartnerEntity(
                    id = "partner_rohat",
                    name = "Чайхана «Роҳат»",
                    category = "RESTAURANT",
                    descriptionRu = "Легендарная национальная чайхана с резными колоннами. Традиционный плов, шашлык и зелёный чай.",
                    descriptionTj = "Чойхонаи таърихии «Роҳат» бо сутунҳои кандакорӣ ва оши миллии тоҷикӣ.",
                    descriptionEn = "Legendary teahouse with intricate carved columns serving authentic Tajik plov and kebabs.",
                    discountPercent = 15,
                    discountDescription = "Скидка 15% на всё меню по QR-билету",
                    promoCode = "ROHAT15",
                    address = "Проспект Рудаки, 84",
                    latitude = 38.5720,
                    longitude = 68.7880,
                    photoUrl = "rohat",
                    isFeatured = true
                ),
                PartnerEntity(
                    id = "partner_serena",
                    name = "Отель «Dushanbe Serena 5*»",
                    category = "HOTEL",
                    descriptionRu = "Роскошный 5-звездочный отель в центре столицы со спа-комплексом и рестораном национальной кухни.",
                    descriptionTj = "Меҳмонхонаи панҷситорадори «Dushanbe Serena» дар маркази пойтахт.",
                    descriptionEn = "Luxury 5-star hotel featuring traditional Tajik architectural aesthetics and spa.",
                    discountPercent = 10,
                    discountDescription = "Скидка 10% на проживание и ресторан",
                    promoCode = "SERENA10",
                    address = "Проспект Рудаки, 14",
                    latitude = 38.5645,
                    longitude = 68.7940,
                    photoUrl = "serena",
                    isFeatured = true
                ),
                PartnerEntity(
                    id = "partner_navruz_tour",
                    name = "Экскурсионный центр «Кохи Навруз»",
                    category = "EXCURSION",
                    descriptionRu = "Индивидуальные и групповые экскурсии по залам дворца с гидом-искусствоведом.",
                    descriptionTj = "Сайри махсус дар толорҳои Кохи Наврӯз бо роҳбаладони касбӣ.",
                    descriptionEn = "Exclusive guided palace hall tours with expert cultural guides.",
                    discountPercent = 20,
                    discountDescription = "Скидка 20% на экскурсионный билет",
                    promoCode = "NAVRUZ20",
                    address = "Проспект И. Сомони, 146",
                    latitude = 38.5867,
                    longitude = 68.7692,
                    photoUrl = "navruz",
                    isFeatured = true
                ),
                PartnerEntity(
                    id = "partner_pamir_souvenirs",
                    name = "Сувениры Памира",
                    category = "SOUVENIR",
                    descriptionRu = "Традиционные памирские носки (джурабы), тюбетейки (токи), чапаны и изделия из лазурита.",
                    descriptionTj = "Тӯҳфаҳои мардумии помирӣ: ҷӯробҳо, тоқӣ ва сангҳои қиматбаҳо.",
                    descriptionEn = "Authentic Pamiri knitted crafts, traditional skullcaps and lapis jewelry.",
                    discountPercent = 12,
                    discountDescription = "Скидка 12% на национальные сувениры",
                    promoCode = "PAMIR12",
                    address = "ул. С. Айни, 32",
                    latitude = 38.5630,
                    longitude = 68.7980,
                    photoUrl = "souvenir",
                    isFeatured = false
                )
            )
            db.partnerDao().insertAllPartners(initialPartners)
        }

        if (db.notificationDao().getAllNotificationsFlow().first().isEmpty()) {
            val welcomeNotification = AppNotificationEntity(
                id = "notif_welcome",
                title = "Хуш омадед! Добро пожаловать!",
                message = "Добро пожаловать в Dushanbe Tour Hop-On Hop-Off! Исследуйте столицу Таджикистана с комфортом.",
                timestamp = System.currentTimeMillis() - 120000,
                isRead = false,
                type = "BUS_ARRIVAL"
            )
            val promoNotification = AppNotificationEntity(
                id = "notif_rohat",
                title = "Скидка 15% в Чайхане «Роҳат»",
                message = "Покажите ваш билет на кассе чайханы Рохат и получите скидку 15% на лучший плов в городе!",
                timestamp = System.currentTimeMillis() - 60000,
                isRead = false,
                type = "PARTNER_DISCOUNT"
            )
            db.notificationDao().insertNotification(welcomeNotification)
            db.notificationDao().insertNotification(promoNotification)
        }

        if (db.userDao().getUser() == null) {
            val defaultUser = UserEntity(
                id = "user_me",
                name = "Алишер Раҳимов",
                email = "tourist.tj@gmail.com",
                phone = "+992 900 12 34 56",
                language = "ru",
                isAdmin = false,
                notificationsEnabled = true
            )
            db.userDao().insertUser(defaultUser)
        }
    }

    // Live Bus GPS Movement Simulation
    private fun startBusGpsSimulation() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            var stepIndex = 0
            while (isActive && isSimulationRunning) {
                delay(2500)
                val buses = db.busDao().getAllBuses()
                val stops = db.busStopDao().getAllStops()
                if (buses.isEmpty() || routeWaypoints.size < 2) continue

                val updatedBuses = buses.mapIndexed { idx, bus ->
                    if (bus.status != "ACTIVE") return@mapIndexed bus

                    // Offset each active bus along route waypoints
                    val pointIdx = (stepIndex + idx * 5) % (routeWaypoints.size - 1)
                    val p1 = routeWaypoints[pointIdx]
                    val p2 = routeWaypoints[pointIdx + 1]

                    // Calculate heading angle
                    val deltaLat = p2.first - p1.first
                    val deltaLng = p2.second - p1.second
                    val headingDeg = (Math.toDegrees(atan2(deltaLng, deltaLat)).toFloat() + 360f) % 360f

                    // Next stop approximation
                    val stopName = if (stops.isNotEmpty()) {
                        stops[(pointIdx / 2) % stops.size].nameRu
                    } else "Центральный Маршрут"

                    val etaMin = (2 + (pointIdx % 4))

                    bus.copy(
                        currentLat = p2.first,
                        currentLng = p2.second,
                        heading = headingDeg,
                        nextStopName = stopName,
                        estimatedArrivalMin = etaMin,
                        speedKmH = (24 + (pointIdx % 10)),
                        satelliteCount = 14 + (pointIdx % 3),
                        gpsAccuracyM = 1.4f + (pointIdx % 5) * 0.1f,
                        altitudeM = 824.0 + (pointIdx % 8) * 1.5
                    )
                }

                db.busDao().insertAllBuses(updatedBuses)
                stepIndex++
            }
        }
    }

    var simulationSpeedMultiplier = 1
        private set

    fun setSimulationMultiplier(multiplier: Int) {
        simulationSpeedMultiplier = multiplier.coerceIn(1, 10)
    }

    fun toggleSimulation(enable: Boolean) {
        isSimulationRunning = enable
        if (enable) {
            startBusGpsSimulation()
        } else {
            simulationJob?.cancel()
        }
    }

    // Bus Management
    suspend fun addBus(bus: BusEntity) = db.busDao().insertBus(bus)
    suspend fun updateBus(bus: BusEntity) = db.busDao().updateBus(bus)
    suspend fun deleteBus(id: String) = db.busDao().deleteBus(id)
    suspend fun setBusLocationManual(id: String, lat: Double, lng: Double) {
        val bus = db.busDao().getBusById(id)
        if (bus != null) {
            db.busDao().updateBus(bus.copy(currentLat = lat, currentLng = lng))
        }
    }

    // Stop Management
    suspend fun addStop(stop: BusStopEntity) = db.busStopDao().insertStop(stop)
    suspend fun updateStop(stop: BusStopEntity) = db.busStopDao().updateStop(stop)
    suspend fun deleteStop(id: String) = db.busStopDao().deleteStop(id)

    // Attraction Management
    suspend fun addAttraction(attr: AttractionEntity) = db.attractionDao().insertAttraction(attr)
    suspend fun updateAttraction(attr: AttractionEntity) = db.attractionDao().updateAttraction(attr)
    suspend fun deleteAttraction(id: String) = db.attractionDao().deleteAttraction(id)
    suspend fun toggleFavorite(id: String, isFav: Boolean) = db.attractionDao().updateFavorite(id, isFav)

    // Tariff Management
    suspend fun addTariff(tariff: TariffEntity) = db.tariffDao().insertTariff(tariff)
    suspend fun updateTariff(tariff: TariffEntity) = db.tariffDao().updateTariff(tariff)
    suspend fun deleteTariff(id: String) = db.tariffDao().deleteTariff(id)

    // Partner Management
    suspend fun addPartner(partner: PartnerEntity) = db.partnerDao().insertPartner(partner)
    suspend fun updatePartner(partner: PartnerEntity) = db.partnerDao().updatePartner(partner)
    suspend fun deletePartner(id: String) = db.partnerDao().deletePartner(id)

    // Ticket Purchase & Validation
    suspend fun purchaseTicket(
        tariff: TariffEntity,
        passengerName: String,
        passengerPhone: String,
        passengerEmail: String
    ): TicketEntity {
        val now = System.currentTimeMillis()
        val validUntil = now + (tariff.durationHours * 3600 * 1000L)
        val ticketId = "TKT-" + UUID.randomUUID().toString().take(8).uppercase()
        val ticketNum = "TJ-HOPON-${(10000..99999).random()}"
        val qrPayload = "DUSHANBE_TOUR_VALID:$ticketNum:$passengerName:$validUntil:${tariff.code}"

        val newTicket = TicketEntity(
            id = ticketId,
            ticketNumber = ticketNum,
            tariffId = tariff.id,
            tariffName = tariff.nameRu,
            passengerName = passengerName,
            passengerPhone = passengerPhone,
            passengerEmail = passengerEmail,
            purchaseTimestamp = now,
            validFrom = now,
            validUntil = validUntil,
            qrCodeData = qrPayload,
            status = "ACTIVE",
            pricePaidTjs = tariff.priceTjs
        )

        db.ticketDao().insertTicket(newTicket)

        // Send confirmation notification
        val notif = AppNotificationEntity(
            id = "notif_${System.currentTimeMillis()}",
            title = "Билет успешно активирован!",
            message = "Ваш билет '${tariff.nameRu}' активен на ${tariff.durationHours} ч. Покажите QR-код при посадке в автобус.",
            timestamp = now,
            isRead = false,
            type = "BUS_ARRIVAL"
        )
        db.notificationDao().insertNotification(notif)

        return newTicket
    }

    suspend fun validateTicketByQr(qrData: String): Pair<Boolean, String> {
        val tickets = db.ticketDao().getAllTicketsFlow().first()
        val matchedTicket = tickets.find { it.qrCodeData == qrData || it.ticketNumber in qrData }
        return if (matchedTicket != null) {
            val now = System.currentTimeMillis()
            if (now > matchedTicket.validUntil) {
                Pair(false, "Срок действия билета истёк (${matchedTicket.ticketNumber})")
            } else if (matchedTicket.status != "ACTIVE") {
                Pair(false, "Билет уже использован или аннулирован (${matchedTicket.ticketNumber})")
            } else {
                Pair(true, "Билет ДЕЙСТВИТЕЛЕН! Пассажир: ${matchedTicket.passengerName}, Тариф: ${matchedTicket.tariffName}")
            }
        } else {
            Pair(false, "QR-код не найден в базе данных Dushanbe Tour")
        }
    }

    // User & Notifications
    suspend fun updateTicket(ticket: TicketEntity) = db.ticketDao().updateTicket(ticket)
    suspend fun addNotification(notif: AppNotificationEntity) = db.notificationDao().insertNotification(notif)
    suspend fun updateUser(user: UserEntity) = db.userDao().updateUser(user)
    suspend fun markNotificationRead(id: String) = db.notificationDao().markAsRead(id)
    suspend fun clearNotification(id: String) = db.notificationDao().deleteNotification(id)

    // Realtime Statistics
    fun getStatisticsFlow(): Flow<TourStatistics> {
        return combine(busesFlow, stopsFlow, ticketsFlow, attractionsFlow) { buses, stops, tickets, attractions ->
            val totalTickets = tickets.size + 380
            val grossRevenue = tickets.sumOf { it.pricePaidTjs } + 34200.0
            val activeBuses = buses.count { it.status == "ACTIVE" }
            val topStop = stops.firstOrNull()?.nameRu ?: "Площадь Исмоили Сомони"
            val topAttr = attractions.firstOrNull()?.nameRu ?: "Национальный Музей Таджикистана"

            TourStatistics(
                totalUsers = 1420 + tickets.size,
                totalTicketsSold = totalTickets,
                grossRevenueTjs = grossRevenue,
                activeTripsToday = 92 + (tickets.size * 2),
                activeBusesCount = activeBuses,
                busFleetTotal = buses.size,
                averageBusOccupancyPercent = 74,
                topStopName = topStop,
                topAttractionName = topAttr
            )
        }
    }
}
