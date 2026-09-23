package com.example.data.repository

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.local.AttractionEntity
import com.example.data.local.BusEntity
import com.example.data.local.BusStopEntity
import com.example.data.model.AppLanguage
import com.example.data.model.AudioPlayerState
import com.example.data.model.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class AudioGuideManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    private var progressTickerJob: Job? = null
    private var pendingSpeakText: String? = null
    private var lastAutoAnnouncedStopId: String? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                applyLanguage(_playerState.value.language)
                tts?.setSpeechRate(_playerState.value.speed)

                // If a speech request arrived before TTS engine was ready, execute it immediately
                pendingSpeakText?.let { text ->
                    pendingSpeakText = null
                    speakInternal(text)
                }
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _playerState.update { it.copy(isPlaying = true) }
                startProgressTicker()
            }

            override fun onDone(utteranceId: String?) {
                stopProgressTicker()
                _playerState.update { it.copy(isPlaying = false, currentPositionSec = it.durationSec) }
            }

            override fun onError(utteranceId: String?) {
                stopProgressTicker()
                _playerState.update { it.copy(isPlaying = false) }
            }
        })
    }

    private fun applyLanguage(language: AppLanguage) {
        val ttsEngine = tts ?: return
        val targetLocale = when (language) {
            AppLanguage.RU -> Locale("ru", "RU")
            AppLanguage.EN -> Locale.US
            AppLanguage.TJ -> Locale("tg", "TJ")
        }

        var result = ttsEngine.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // For Tajik or unsupported locales, fallback smoothly to Russian or system default
            val fallbackLocale = if (language == AppLanguage.TJ) Locale("ru", "RU") else Locale.getDefault()
            result = ttsEngine.setLanguage(fallbackLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsEngine.setLanguage(Locale.ENGLISH)
            }
        }
    }

    fun playTrack(track: AudioTrack) {
        _playerState.update {
            it.copy(
                currentTrack = track,
                currentPositionSec = 0,
                durationSec = track.audioDurationSec.coerceAtLeast(10),
                isPlaying = true,
                language = track.language
            )
        }
        applyLanguage(track.language)
        speakText(track.script)
    }

    fun playAttraction(attraction: AttractionEntity, language: AppLanguage) {
        val script = when (language) {
            AppLanguage.RU -> attraction.audioScriptRu
            AppLanguage.TJ -> attraction.audioScriptTj
            AppLanguage.EN -> attraction.audioScriptEn
        }
        val attrName = when (language) {
            AppLanguage.RU -> attraction.nameRu
            AppLanguage.TJ -> attraction.nameTj
            AppLanguage.EN -> attraction.nameEn
        }
        val track = AudioTrack(
            attractionId = attraction.id,
            attractionName = attrName,
            audioDurationSec = attraction.audioDurationSec.coerceAtLeast(20),
            script = script,
            language = language,
            categoryLabel = "Аудиогид достопримечательности"
        )
        playTrack(track)
    }

    fun playStop(stop: BusStopEntity, language: AppLanguage) {
        val stopName = when (language) {
            AppLanguage.RU -> stop.nameRu
            AppLanguage.TJ -> stop.nameTj
            AppLanguage.EN -> stop.nameEn
        }
        val stopDesc = when (language) {
            AppLanguage.RU -> stop.descriptionRu
            AppLanguage.TJ -> stop.descriptionTj
            AppLanguage.EN -> stop.descriptionEn
        }

        val script = when (language) {
            AppLanguage.RU -> "Остановка номер ${stop.stopOrder}: $stopName. $stopDesc. Рядом расположены: ${stop.nearbyAttractions}. Следующий туристический автобус ожидается через ${stop.nextBusMinutes} минут."
            AppLanguage.TJ -> "Истгоҳи рақами ${stop.stopOrder}: $stopName. $stopDesc. Дар наздикӣ ҷойгиранд: ${stop.nearbyAttractions}. Автобуси навбатӣ баъд аз ${stop.nextBusMinutes} дақиқа мерасад."
            AppLanguage.EN -> "Stop number ${stop.stopOrder}: $stopName. $stopDesc. Nearby attractions: ${stop.nearbyAttractions}. Next tour bus expected in ${stop.nextBusMinutes} minutes."
        }

        val estimatedDuration = (script.split(" ").size / 2).coerceAtLeast(15)

        val track = AudioTrack(
            attractionId = "stop_${stop.id}",
            attractionName = stopName,
            audioDurationSec = estimatedDuration,
            script = script,
            language = language,
            categoryLabel = "Голосовой гид остановки"
        )
        playTrack(track)
    }

    fun playTourOverview(language: AppLanguage) {
        val title = when (language) {
            AppLanguage.RU -> "Обзорный аудиогид: Душанбе Тур"
            AppLanguage.TJ -> "Сайри умумии шаҳри Душанбе"
            AppLanguage.EN -> "Dushanbe Hop-On Hop-Off City Tour"
        }
        val script = when (language) {
            AppLanguage.RU -> "Добро пожаловать в солнечный Душанбе — столицу Республики Таджикистан! Наш круговой туристический маршрут Hop-On Hop-Off охватывает восемь главных жемчужин города. Вы увидите тринадцатиметровый золотой монумент Исмоили Сомони, грандиозный 165-метровый флагшток в парке Рудаки, уникальный музей с спящим Буддой, шедевр зодчества дворец Навруз, тенистый Ботанический сад «Боги Ирам» и восточный базар Мехргон. Выходите на любой остановке, наслаждайтесь городом и продолжайте путь на следующем автобусе!"
            AppLanguage.TJ -> "Хуш омадед ба шаҳри зебоманзари Душанбе — пойтахти Тоҷикистон! Сайри сайёҳии мо бо автобусҳои дуошёнаи бароҳат 8 истгоҳи муҳимтарини шаҳрро фаро мегирад: аз муҷассамаи шоҳ Исмоили Сомонӣ ва Парчами миллӣ то Осорхонаи миллӣ ва Қасри Наврӯз. Шумо метавонед дар дилхоҳ истгоҳ фуроянд ва сайри худро идома диҳед!"
            AppLanguage.EN -> "Welcome to sunny Dushanbe, the capital of Tajikistan! Our circular Hop-On Hop-Off sightseeing tour connects 8 major city landmarks. Marvel at the golden Ismail Samani monument, the 165-meter flagpole, the National Museum with its reclining Buddha, the palatial Navruz complex, and the vibrant Mehrgon bazaar. Feel free to hop on and hop off whenever you wish!"
        }

        val track = AudioTrack(
            attractionId = "tour_overview",
            attractionName = title,
            audioDurationSec = 65,
            script = script,
            language = language,
            categoryLabel = "Обзорная экскурсия"
        )
        playTrack(track)
    }

    fun announceBusStatus(bus: BusEntity, stop: BusStopEntity?, language: AppLanguage) {
        val title = "${bus.busNumber} (${bus.plateNumber})"
        val script = when (language) {
            AppLanguage.RU -> "Информация о рейсе: ${bus.busNumber}, государственный номер ${bus.plateNumber}. Направляется к остановке «${bus.nextStopName}». Расчетное время прибытия: ${bus.estimatedArrivalMin} мин. Текущая скорость: ${bus.speedKmH} километров в час. В салоне свободно ${bus.capacity - bus.currentPassengers} мест из ${bus.capacity}."
            AppLanguage.TJ -> "Маълумот оид ба автобус: ${bus.busNumber}, рақами қайди давлатӣ ${bus.plateNumber}. Ба самти истгоҳи «${bus.nextStopName}» равон аст. Вақти расидан: ${bus.estimatedArrivalMin} дақиқа. Суръат: ${bus.speedKmH} километр дар як соат. Ҷойҳои холӣ: ${bus.capacity - bus.currentPassengers}."
            AppLanguage.EN -> "Tour bus telemetry: ${bus.busNumber}, license plate ${bus.plateNumber}. In transit to ${bus.nextStopName}. ETA is ${bus.estimatedArrivalMin} minutes. Speed: ${bus.speedKmH} kilometers per hour. Seats available: ${bus.capacity - bus.currentPassengers} of ${bus.capacity}."
        }

        val track = AudioTrack(
            attractionId = "bus_${bus.id}",
            attractionName = title,
            audioDurationSec = 22,
            script = script,
            language = language,
            categoryLabel = "Автоинформатор автобуса"
        )
        playTrack(track)
    }

    fun toggleAutoGuide() {
        val newState = !_playerState.value.isAutoGuideEnabled
        _playerState.update { it.copy(isAutoGuideEnabled = newState) }

        val lang = _playerState.value.language
        val notificationText = if (newState) {
            when (lang) {
                AppLanguage.RU -> "Режим голосового автогида включен. Остановки и достопримечательности будут озвучиваться автоматически."
                AppLanguage.TJ -> "Ҳолати роҳнамои овозӣ фаъол шуд. Истгоҳҳо ва ёдгориҳо ба таври худкор эълон карда мешаванд."
                AppLanguage.EN -> "Auto-guide enabled. Stops and landmarks will be narrated automatically."
            }
        } else {
            when (lang) {
                AppLanguage.RU -> "Режим автогида выключен."
                AppLanguage.TJ -> "Ҳолати роҳнамои овозӣ хомӯш карда шуд."
                AppLanguage.EN -> "Auto-guide disabled."
            }
        }

        speakBriefNotification(notificationText, lang)
    }

    fun triggerAutoGuideStopAnnouncement(stop: BusStopEntity, language: AppLanguage) {
        if (!_playerState.value.isAutoGuideEnabled) return
        if (lastAutoAnnouncedStopId == stop.id && _playerState.value.isPlaying) return

        lastAutoAnnouncedStopId = stop.id
        playStop(stop, language)
    }

    private fun speakBriefNotification(text: String, language: AppLanguage) {
        applyLanguage(language)
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "NOTIFICATION_UTTERANCE")
        } else {
            pendingSpeakText = text
        }
    }

    private fun speakText(text: String) {
        if (isTtsReady) {
            speakInternal(text)
        } else {
            pendingSpeakText = text
            startProgressTicker()
        }
    }

    private fun speakInternal(text: String) {
        val params = Bundle()
        tts?.setSpeechRate(_playerState.value.speed)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "AUDIO_GUIDE_TRACK_${System.currentTimeMillis()}")
        startProgressTicker()
    }

    fun togglePlayPause() {
        val current = _playerState.value
        if (current.isPlaying) {
            tts?.stop()
            stopProgressTicker()
            _playerState.update { it.copy(isPlaying = false) }
        } else {
            val track = current.currentTrack
            if (track != null) {
                _playerState.update { it.copy(isPlaying = true) }
                speakText(track.script)
            }
        }
    }

    fun seekTo(seconds: Int) {
        val dur = _playerState.value.durationSec
        val clamped = seconds.coerceIn(0, dur)
        _playerState.update { it.copy(currentPositionSec = clamped) }
    }

    fun skipForward() {
        val current = _playerState.value.currentPositionSec
        seekTo(current + 10)
    }

    fun skipBackward() {
        val current = _playerState.value.currentPositionSec
        seekTo(current - 10)
    }

    fun changeLanguage(lang: AppLanguage, script: String) {
        _playerState.update {
            it.copy(
                language = lang,
                currentTrack = it.currentTrack?.copy(language = lang, script = script),
                currentPositionSec = 0
            )
        }
        applyLanguage(lang)
        if (_playerState.value.isPlaying) {
            speakText(script)
        }
    }

    fun setSpeed(speed: Float) {
        _playerState.update { it.copy(speed = speed) }
        tts?.setSpeechRate(speed)
    }

    fun stop() {
        tts?.stop()
        stopProgressTicker()
        _playerState.update { it.copy(isPlaying = false, currentTrack = null, currentPositionSec = 0) }
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressTickerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val current = _playerState.value
                if (current.isPlaying && current.currentPositionSec < current.durationSec) {
                    _playerState.update { it.copy(currentPositionSec = it.currentPositionSec + 1) }
                } else if (current.currentPositionSec >= current.durationSec) {
                    _playerState.update { it.copy(isPlaying = false) }
                    break
                }
            }
        }
    }

    private fun stopProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }

    fun release() {
        stopProgressTicker()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }
}
