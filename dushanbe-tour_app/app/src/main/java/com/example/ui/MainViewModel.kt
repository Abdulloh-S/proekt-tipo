package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.model.AppLanguage
import com.example.data.model.AudioPlayerState
import com.example.data.model.AudioTrack
import com.example.data.repository.AudioGuideManager
import com.example.data.repository.TourRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = TourRepository(database, application)
    val audioGuideManager = AudioGuideManager(application)

    val buses: StateFlow<List<BusEntity>> = repository.busesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stops: StateFlow<List<BusStopEntity>> = repository.stopsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tariffs: StateFlow<List<TariffEntity>> = repository.tariffsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attractions: StateFlow<List<AttractionEntity>> = repository.attractionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partners: StateFlow<List<PartnerEntity>> = repository.partnersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tickets: StateFlow<List<TicketEntity>> = repository.ticketsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTicket: StateFlow<TicketEntity?> = repository.activeTicketFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val user: StateFlow<UserEntity?> = repository.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val notifications: StateFlow<List<AppNotificationEntity>> = repository.notificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioPlayerState: StateFlow<AudioPlayerState> = audioGuideManager.playerState

    private val _currentLanguage = MutableStateFlow(AppLanguage.RU)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Nearest stop and bus
    val nearestStop: StateFlow<BusStopEntity?> = stops.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val nearestBus: StateFlow<BusEntity?> = buses.map { it.firstOrNull { b -> b.status == "ACTIVE" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val routeWaypoints = repository.routeWaypoints

    init {
        // Monitor live bus progress for automated voice tour guide announcements
        viewModelScope.launch {
            buses.collect { busList ->
                if (audioPlayerState.value.isAutoGuideEnabled) {
                    val activeBus = busList.firstOrNull { it.status == "ACTIVE" }
                    if (activeBus != null && activeBus.estimatedArrivalMin <= 2) {
                        val currentStops = stops.value
                        val approachingStop = currentStops.find { it.nameRu.equals(activeBus.nextStopName, ignoreCase = true) }
                            ?: currentStops.firstOrNull()
                        approachingStop?.let { stop ->
                            audioGuideManager.triggerAutoGuideStopAnnouncement(stop, _currentLanguage.value)
                        }
                    }
                }
            }
        }
    }

    fun buyTicket(tariff: TariffEntity, name: String, phone: String, email: String) {
        viewModelScope.launch {
            repository.purchaseTicket(tariff, name, phone, email)
        }
    }

    fun updateTariffPrice(tariffId: String, newPrice: Double) {
        viewModelScope.launch {
            val existing = tariffs.value.find { it.id == tariffId }
            if (existing != null) {
                repository.updateTariff(existing.copy(priceTjs = newPrice))
            }
        }
    }

    fun toggleBusStatus(busId: String, status: String) {
        viewModelScope.launch {
            val existing = buses.value.find { it.id == busId }
            if (existing != null) {
                repository.updateBus(existing.copy(status = status))
            }
        }
    }

    fun addBus(busNumber: String, plateNumber: String) {
        viewModelScope.launch {
            val newBus = BusEntity(
                id = "bus_${System.currentTimeMillis()}",
                busNumber = busNumber,
                plateNumber = plateNumber,
                status = "ACTIVE",
                currentLat = 38.5753,
                currentLng = 68.7865,
                speedKmH = 26,
                heading = 45f,
                nextStopName = "Площадь Исмоили Сомони",
                estimatedArrivalMin = 4,
                capacity = 50,
                currentPassengers = 14
            )
            repository.addBus(newBus)
        }
    }

    fun playAudioGuide(attraction: AttractionEntity) {
        audioGuideManager.playAttraction(attraction, _currentLanguage.value)
    }

    fun playStopAudio(stop: BusStopEntity) {
        audioGuideManager.playStop(stop, _currentLanguage.value)
    }

    fun playTourOverview() {
        audioGuideManager.playTourOverview(_currentLanguage.value)
    }

    fun announceBus(bus: BusEntity) {
        val nextStop = stops.value.find { it.nameRu.equals(bus.nextStopName, ignoreCase = true) }
        audioGuideManager.announceBusStatus(bus, nextStop, _currentLanguage.value)
    }

    fun toggleAutoGuide() {
        audioGuideManager.toggleAutoGuide()
    }

    fun setAppLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        val currentTrack = audioPlayerState.value.currentTrack
        if (currentTrack != null) {
            val attraction = attractions.value.find { it.id == currentTrack.attractionId }
            if (attraction != null) {
                val newScript = when (language) {
                    AppLanguage.RU -> attraction.audioScriptRu
                    AppLanguage.TJ -> attraction.audioScriptTj
                    AppLanguage.EN -> attraction.audioScriptEn
                }
                audioGuideManager.changeLanguage(language, newScript)
            } else if (currentTrack.attractionId.startsWith("stop_")) {
                val stopId = currentTrack.attractionId.removePrefix("stop_")
                val stop = stops.value.find { it.id == stopId }
                if (stop != null) {
                    audioGuideManager.playStop(stop, language)
                }
            } else if (currentTrack.attractionId == "tour_overview") {
                audioGuideManager.playTourOverview(language)
            }
        }
    }

    fun toggleFavorite(attractionId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(attractionId, isFavorite)
        }
    }

    fun updateUser(name: String, email: String, phone: String) {
        viewModelScope.launch {
            val existing = user.value
            val updated = existing?.copy(name = name, email = email, phone = phone) ?: UserEntity(
                id = "usr_default",
                name = name,
                email = email,
                phone = phone,
                language = _currentLanguage.value.code,
                isAdmin = true,
                notificationsEnabled = true
            )
            repository.updateUser(updated)
        }
    }

    suspend fun validateTicket(qrCode: String): Pair<Boolean, String> {
        return repository.validateTicketByQr(qrCode)
    }

    fun setSimulationMultiplier(multiplier: Int) {
        repository.setSimulationMultiplier(multiplier)
    }

    fun toggleSimulation(enable: Boolean) {
        repository.toggleSimulation(enable)
    }

    fun broadcastNotification(title: String, message: String) {
        viewModelScope.launch {
            val notif = AppNotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                type = "SERVICE_ALERT"
            )
            repository.addNotification(notif)
        }
    }

    fun revokeTicket(ticketId: String) {
        viewModelScope.launch {
            val existing = tickets.value.find { it.id == ticketId }
            if (existing != null) {
                repository.updateTicket(existing.copy(status = "REVOKED"))
            }
        }
    }

    fun activateTicket(ticketId: String) {
        viewModelScope.launch {
            val existing = tickets.value.find { it.id == ticketId }
            if (existing != null) {
                repository.updateTicket(existing.copy(status = "ACTIVE"))
            }
        }
    }

    fun updateAttractionAudio(id: String, scriptRu: String, scriptTj: String, scriptEn: String) {
        viewModelScope.launch {
            val existing = attractions.value.find { it.id == id }
            if (existing != null) {
                repository.updateAttraction(
                    existing.copy(
                        audioScriptRu = scriptRu,
                        audioScriptTj = scriptTj,
                        audioScriptEn = scriptEn
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioGuideManager.release()
    }
}
