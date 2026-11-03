package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiChatService
import com.example.audio.SpeechRecognitionHelper
import com.example.audio.SpeechTtsEngine
import com.example.model.AppLanguage
import com.example.model.AvatarCustomization
import com.example.model.AvatarEmotion
import com.example.model.CharacterCatalog
import com.example.model.ChatMessage
import com.example.model.PetCharacter
import com.example.model.PetSpecies
import com.example.model.ReactionCatalog
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.TimeoutCancellationException

import com.example.model.BackgroundTheme
import com.example.model.TimeOfDay
import com.example.model.TouchZone
import com.example.model.EarStyle
import com.example.audio.VoiceEngineMode

import com.example.model.CareOverlayType
import com.example.model.FoodItem

enum class FaceToFaceCallState {
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING,
    MUTED
}

data class AvatarUiState(
    val activeCharacter: PetCharacter = CharacterCatalog.characters.first(),
    val customization: AvatarCustomization = AvatarCustomization(),
    val messages: List<ChatMessage> = emptyList(),
    val lastAvatarSpeech: String = "",
    val isTalking: Boolean = false,
    val isListeningMic: Boolean = false,
    val showVoiceDialog: Boolean = false,
    val speechTranscript: String = "",
    val speechRms: Float = 0f,
    val speechServiceUnavailable: Boolean = false,
    val triggerSystemSpeechFallback: Boolean = false,
    val currentEmotion: AvatarEmotion = AvatarEmotion.IDLE,
    val affectionScore: Int = 20,
    val selectedLanguage: AppLanguage = AppLanguage.INDONESIAN,
    val voicePitch: Float = 1.25f,
    val speechRate: Float = 1.15f,
    val voiceEngineMode: VoiceEngineMode = VoiceEngineMode.CARTOON_MIMIC,
    val isLoadingAi: Boolean = false,
    val backgroundTheme: BackgroundTheme = BackgroundTheme.LIVING_ROOM,
    val currentTimeOfDay: TimeOfDay = TimeOfDay.fromCurrentHour(),
    // Face-to-Face live hands-free conversation state
    val isFaceToFaceActive: Boolean = false,
    val faceToFaceCallState: FaceToFaceCallState = FaceToFaceCallState.CONNECTING,
    val isFaceToFaceMuted: Boolean = false,
    val liveUserSpeech: String = "",
    val liveAvatarSpeech: String = "",
    val callDurationSeconds: Int = 0,
    // Talking Tom Living Care System & Leveling
    val playerLevel: Int = 12,
    val levelProgress: Float = 0.65f, // Circular progress for LVL 12 badge
    val coins: Int = 1500, // Matches 1,500 coins capsule in screenshot
    val diamonds: Int = 240, // Matches 240 diamonds capsule in screenshot
    val hungerLevel: Float = 0.85f, // "Makan" 85%
    val toiletLevel: Float = 0.70f, // "Toilet" 70%
    val energyLevel: Float = 0.60f, // "Tidur" 60%
    val funLevel: Float = 0.90f, // "Bermain/Hiburan" 90%
    val activeCareOverlay: CareOverlayType = CareOverlayType.NONE,
    val isMimicModeActive: Boolean = false,
    val miniGameScore: Int = 0,
    val careFeedbackMessage: String = "",
    // Mode Ngerjain Tugas Sekolah
    val showHomeworkMode: Boolean = false,
    val homeworkSubject: String = "Matematika",
    val homeworkQuestion: String = "",
    val homeworkSolution: HomeworkSolution? = null,
    val isSolvingHomework: Boolean = false,
    val homeworkPhotoBitmap: android.graphics.Bitmap? = null
)

data class HomeworkSolution(
    val subject: String,
    val questionSummary: String,
    val stepByStep: List<String>,
    val finalAnswer: String,
    val tutorTip: String
)

class AvatarViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AvatarUiState())
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow()

    private val aiService = GeminiChatService(application)
    private var emotionResetJob: Job? = null
    private var callTimerJob: Job? = null
    private var autoListenJob: Job? = null

    private val ttsEngine = SpeechTtsEngine(application) { isSpeaking ->
        _uiState.value = _uiState.value.copy(
            isTalking = isSpeaking,
            currentEmotion = if (isSpeaking) AvatarEmotion.TALKING else AvatarEmotion.IDLE
        )
        if (!isSpeaking && _uiState.value.isFaceToFaceActive && !_uiState.value.isFaceToFaceMuted) {
            onAvatarFinishedSpeakingInCall()
        }
    }

    private val speechRecognitionHelper: SpeechRecognitionHelper = SpeechRecognitionHelper(
        context = application,
        onResult = { recognizedText ->
            if (recognizedText.isNotBlank()) {
                if (_uiState.value.isFaceToFaceActive) {
                    handleFaceToFaceUserUtterance(recognizedText)
                } else {
                    _uiState.value = _uiState.value.copy(
                        speechTranscript = recognizedText,
                        isListeningMic = false
                    )
                }
            }
        },
        onPartialResult = { partial ->
            if (partial.isNotBlank()) {
                if (_uiState.value.isFaceToFaceActive) {
                    _uiState.value = _uiState.value.copy(liveUserSpeech = partial)
                } else {
                    _uiState.value = _uiState.value.copy(speechTranscript = partial)
                }
            }
        },
        onRmsChanged = { rms ->
            _uiState.value = _uiState.value.copy(speechRms = rms)
        },
        onListeningStateChanged = { isListening ->
            _uiState.value = _uiState.value.copy(isListeningMic = isListening)
            if (_uiState.value.isFaceToFaceActive && !_uiState.value.isFaceToFaceMuted && isListening) {
                if (_uiState.value.faceToFaceCallState != FaceToFaceCallState.THINKING &&
                    _uiState.value.faceToFaceCallState != FaceToFaceCallState.SPEAKING) {
                    _uiState.value = _uiState.value.copy(faceToFaceCallState = FaceToFaceCallState.LISTENING)
                }
            }
        },
        onServiceUnavailable = {
            if (!_uiState.value.isFaceToFaceActive) {
                _uiState.value = _uiState.value.copy(
                    speechServiceUnavailable = true,
                    triggerSystemSpeechFallback = true,
                    isListeningMic = false
                )
            }
        },
        onErrorCallback = { errorCode ->
            onSpeechErrorInCall(errorCode)
        }
    )

    private fun onSpeechErrorInCall(errorCode: Int) {
        if (_uiState.value.isFaceToFaceActive && !_uiState.value.isFaceToFaceMuted) {
            if (errorCode == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                errorCode == android.speech.SpeechRecognizer.ERROR_NO_MATCH) {
                autoListenJob?.cancel()
                autoListenJob = viewModelScope.launch {
                    delay(600)
                    if (_uiState.value.isFaceToFaceActive &&
                        !_uiState.value.isFaceToFaceMuted &&
                        !_uiState.value.isTalking &&
                        _uiState.value.faceToFaceCallState != FaceToFaceCallState.THINKING) {
                        _uiState.value = _uiState.value.copy(faceToFaceCallState = FaceToFaceCallState.LISTENING)
                        speechRecognitionHelper.startListening(_uiState.value.selectedLanguage)
                    }
                }
            }
        }
    }

    private val prefs by lazy {
        application.getSharedPreferences("pawpal_ai_prefs", android.content.Context.MODE_PRIVATE)
    }

    init {
        val initialChar = _uiState.value.activeCharacter
        val initialMode = getVoiceEngineMode()

        _uiState.value = _uiState.value.copy(
            voicePitch = initialChar.defaultVoicePitch,
            speechRate = initialChar.defaultSpeechRate,
            voiceEngineMode = initialMode,
            customization = _uiState.value.customization.copy(furColor = initialChar.defaultFurColor)
        )

        ttsEngine.engineMode = initialMode
        ttsEngine.elevenLabsApiKey = getElevenLabsApiKey()
        ttsEngine.googleApiKey = getGoogleCloudTtsApiKey()
        ttsEngine.setVoiceProfile(initialChar.defaultVoicePitch, initialChar.defaultSpeechRate)
        ttsEngine.setLanguage(_uiState.value.selectedLanguage)

        viewModelScope.launch {
            delay(500)
            deliverGreeting(initialChar, _uiState.value.selectedLanguage)
        }
    }

    private fun deliverGreeting(character: PetCharacter, language: AppLanguage) {
        val greetingText = when (character.id) {
            "zayn" -> "Cih! Aku Zayn. Ngapain kamu ke sini? B-bukan berarti aku seneng ditemenin ya! Cepetan mau apa?!"
            "siti" -> "Halo sayangku manis, nama Siti. Siti sangat bersyukur bisa mendampingimu hari ini. Ceritakan harimu ya sayang."
            "rizky" -> "Siap! Rizky siap bertugas! Pertahankan disiplin dan fokus pada target kita hari ini!"
            "jamal" -> "Hoaaammm... Jamal lagi mager banget nih. Kalau gak ada urusan penting mending kita tidur siang bareng."
            else -> "HEH! Lu ngeliatin apa?! Gua Heru, jangan cari gara-gara sama gua kalau gak mau kena semprot!"
        }

        updateSpeechAndAutoSpeak(greetingText, AvatarEmotion.HAPPY)
    }

    /**
     * Wake Word calling ("Hey Petto" / "Halo Petto"):
     * Avatar immediately wakes up with surprised/attentive reaction, speaks back in character,
     * and automatically opens voice input so the user can speak immediately!
     */
    fun callHeyPetto() {
        val char = _uiState.value.activeCharacter
        val lang = _uiState.value.selectedLanguage
        val response = ReactionCatalog.getWakeWordResponse(char, lang)

        // Wake up avatar
        _uiState.value = _uiState.value.copy(currentEmotion = AvatarEmotion.SURPRISED)
        ttsEngine.playHappyChime()
        updateSpeechAndAutoSpeak(response, AvatarEmotion.SURPRISED)

        // Automatically open voice listening dialog after small delay so user can immediately speak!
        viewModelScope.launch {
            delay(1400)
            openVoiceInput()
        }
    }

    fun updateSpeechAndAutoSpeak(text: String, emotion: AvatarEmotion = AvatarEmotion.TALKING) {
        if (text.isBlank()) return

        _uiState.value = _uiState.value.copy(
            lastAvatarSpeech = text,
            currentEmotion = emotion
        )

        ttsEngine.speak(
            text = text,
            pitch = _uiState.value.voicePitch,
            rate = _uiState.value.speechRate,
            character = _uiState.value.activeCharacter
        )

        emotionResetJob?.cancel()
        emotionResetJob = viewModelScope.launch {
            delay(3500)
            if (_uiState.value.currentEmotion != AvatarEmotion.IDLE && !_uiState.value.isTalking) {
                _uiState.value = _uiState.value.copy(currentEmotion = AvatarEmotion.IDLE)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val state = _uiState.value

        // Check wake word in text
        val lower = text.lowercase()
        if (lower.contains("hey petto") || lower.contains("halo petto") || lower.contains("hai petto")) {
            val userMsg = ChatMessage(senderName = "Kamu", text = text, isUser = true)
            val wakeReply = ReactionCatalog.getWakeWordResponse(state.activeCharacter, state.selectedLanguage)
            val avatarReplyMsg = ChatMessage(senderName = state.activeCharacter.name, text = wakeReply, isUser = false, emotion = AvatarEmotion.SURPRISED)
            _uiState.value = state.copy(
                messages = state.messages + userMsg + avatarReplyMsg,
                affectionScore = (state.affectionScore + 3).coerceAtMost(100)
            )
            updateSpeechAndAutoSpeak(wakeReply, AvatarEmotion.SURPRISED)
            return
        }

        val userMsg = ChatMessage(
            senderName = "Kamu",
            text = text,
            isUser = true
        )

        val updatedMessages = state.messages + userMsg
        _uiState.value = state.copy(
            messages = updatedMessages,
            isLoadingAi = true,
            currentEmotion = AvatarEmotion.TALKING
        )

        viewModelScope.launch {
            try {
                // Generates AI reply or falls back quickly within 5.0 seconds max
                val reply = withTimeoutOrNull(5000L) {
                    aiService.generateReply(
                        userMessage = text,
                        character = state.activeCharacter,
                        language = state.selectedLanguage,
                        history = updatedMessages
                    )
                } ?: aiService.getFastFallbackReply(
                    userMessage = text,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )

                val avatarReplyMsg = ChatMessage(
                    senderName = state.activeCharacter.name,
                    text = reply.text,
                    isUser = false,
                    emotion = reply.emotion
                )

                val newAffection = (state.affectionScore + 3).coerceAtMost(100)

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + avatarReplyMsg,
                    isLoadingAi = false,
                    affectionScore = newAffection
                )

                updateSpeechAndAutoSpeak(reply.text, reply.emotion)
            } catch (e: TimeoutCancellationException) {
                Log.d("AvatarViewModel", "AI response timeout: ${e.message}, using fallback")
                val fallbackReply = aiService.getFastFallbackReply(
                    userMessage = text,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )
                val avatarReplyMsg = ChatMessage(
                    senderName = state.activeCharacter.name,
                    text = fallbackReply.text,
                    isUser = false,
                    emotion = fallbackReply.emotion
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + avatarReplyMsg,
                    isLoadingAi = false
                )
                updateSpeechAndAutoSpeak(fallbackReply.text, fallbackReply.emotion)
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error in sendMessage: ${e.message}", e)
                val fallbackReply = aiService.getFastFallbackReply(
                    userMessage = text,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )
                val avatarReplyMsg = ChatMessage(
                    senderName = state.activeCharacter.name,
                    text = fallbackReply.text,
                    isUser = false,
                    emotion = fallbackReply.emotion
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + avatarReplyMsg,
                    isLoadingAi = false
                )
                updateSpeechAndAutoSpeak(fallbackReply.text, fallbackReply.emotion)
            } finally {
                _uiState.value = _uiState.value.copy(isLoadingAi = false)
            }
        }
    }

    fun onCharacterTouched(zone: TouchZone) {
        val state = _uiState.value
        val newAffection = (state.affectionScore + 3).coerceAtMost(100)

        // Trigger expressive procedural cartoon touch reaction sound effect (giggle, boing, pop, purr, grunt)
        ttsEngine.playTouchSound(state.activeCharacter.species, zone)

        val reactionText = ReactionCatalog.getTouchZoneReaction(state.activeCharacter, zone, state.selectedLanguage)
        val emotion = when (zone) {
            TouchZone.HEAD -> AvatarEmotion.HAPPY
            TouchZone.BELLY -> AvatarEmotion.LOVING
            TouchZone.HANDS -> AvatarEmotion.TALKING
            TouchZone.FEET -> AvatarEmotion.SURPRISED
        }
        _uiState.value = state.copy(affectionScore = newAffection)
        updateSpeechAndAutoSpeak(reactionText, emotion)
    }

    fun petAvatar() {
        onCharacterTouched(TouchZone.HEAD)
    }

    fun setBackgroundTheme(theme: BackgroundTheme) {
        val oldTheme = _uiState.value.backgroundTheme
        _uiState.value = _uiState.value.copy(backgroundTheme = theme)
        if (oldTheme != theme) {
            val char = _uiState.value.activeCharacter
            val comment = when (theme) {
                BackgroundTheme.LIVING_ROOM -> when (char.id) {
                    "zayn" -> "Sofa ruang tamu ini empuk juga sih. B-bukan berarti aku mau mager di sini ya!"
                    "siti" -> "Ruang tamu yang ceria dan penuh mainan sayang, Siti senang bermain di sini bersamamu."
                    "rizky" -> "Pangkalan ruang tamu siap patroli! Kondisi aman terkendali!"
                    "jamal" -> "Nah, sofa ruang tamu emang spot rebahan paling santuy bro."
                    else -> "SOFA KARTUNNYA KEREN JUGA! Awas lu jangan berantakin mainan di lantai!"
                }
                BackgroundTheme.BEDROOM -> when (char.id) {
                    "zayn" -> "Kamar tidur? Cih, ngantuk ya tidur aja sana! B-bukan berarti aku mau nemenin kamu ya!"
                    "siti" -> "Selamat datang di kamar tidur yang hangat manisku, semoga istirahatmu ditemani mimpi indah."
                    "rizky" -> "Waktu istirahat taktis di kamar tidur! Pulihkan tenaga untuk misi berikutnya!"
                    "jamal" -> "HOAAAMMM! Ini ruangan favorit Jamal sedunia! Jangan bangunin gua ya, mau tidur."
                    else -> "KASUR NYAMAN BANGET! Lu mau tidur sekarang hah?! Awas lu ngorok gua lempar bantal!"
                }
                BackgroundTheme.KITCHEN -> when (char.id) {
                    "zayn" -> "Dapur? Hah?! Mau bikinin makanan buat aku?! B-bukan berarti aku laper ya!"
                    "siti" -> "Dapur yang cantik sayang! Siti mau buatkan camilan lezat dan susu hangat untukmu."
                    "rizky" -> "Stasiun logistik nutrisi terkonfirmasi! Asupan energi siap dikonsumsi!"
                    "jamal" -> "KULKAS! Buka kulkasnya bro, ambilin ikan sama susu dingin dong buat Jamal."
                    else -> "DAPUR! MANA MAKANAN GUA?! Beruang laper nih, cepetan buka kulkasnya lu!"
                }
                else -> "${char.name} menyukai suasana ruangan ${theme.displayName}!"
            }
            updateSpeechAndAutoSpeak(comment, AvatarEmotion.HAPPY)
        }
    }

    fun cycleBackgroundTheme() {
        val themes = BackgroundTheme.values()
        val currentIndex = themes.indexOf(_uiState.value.backgroundTheme)
        val nextTheme = themes[(currentIndex + 1) % themes.size]
        setBackgroundTheme(nextTheme)
    }

    fun openVoiceInput() {
        _uiState.value = _uiState.value.copy(
            showVoiceDialog = true,
            speechTranscript = "",
            speechServiceUnavailable = false,
            triggerSystemSpeechFallback = false
        )
        speechRecognitionHelper.startListening(_uiState.value.selectedLanguage)
    }

    fun onSystemSpeechFallbackConsumed() {
        _uiState.value = _uiState.value.copy(triggerSystemSpeechFallback = false)
    }

    fun closeVoiceInput() {
        speechRecognitionHelper.stopListening()
        _uiState.value = _uiState.value.copy(
            showVoiceDialog = false,
            speechTranscript = "",
            isListeningMic = false,
            triggerSystemSpeechFallback = false
        )
    }

    fun onFallbackSpeechResult(text: String) {
        if (text.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                speechTranscript = text,
                showVoiceDialog = true
            )
        }
    }

    fun sendVoiceTranscript(text: String) {
        closeVoiceInput()
        sendMessage(text)
    }

    fun replayCurrentSpeech() {
        val speech = _uiState.value.lastAvatarSpeech
        if (speech.isNotBlank()) {
            ttsEngine.speak(speech, _uiState.value.voicePitch, _uiState.value.speechRate)
        }
    }

    fun speakText(text: String) {
        updateSpeechAndAutoSpeak(text, AvatarEmotion.TALKING)
    }

    fun updateCustomization(newCustomization: AvatarCustomization) {
        val old = _uiState.value.customization
        val char = _uiState.value.activeCharacter
        val lang = _uiState.value.selectedLanguage
        _uiState.value = _uiState.value.copy(customization = newCustomization)

        val reactionText = when {
            newCustomization.costume != old.costume ->
                ReactionCatalog.getCostumeReaction(newCustomization.costume, char, lang)
            newCustomization.accessory != old.accessory ->
                ReactionCatalog.getAccessoryReaction(newCustomization.accessory, char, lang)
            newCustomization.furColorId != old.furColorId ->
                ReactionCatalog.getFurColorReaction(newCustomization.furColorId, char, lang)
            newCustomization.roomTheme != old.roomTheme ->
                when (char.id) {
                    "zayn" -> "Hmph! Ruangan ${newCustomization.roomTheme.displayName}? Baguslah, lumayan nyaman."
                    "siti" -> "Ruangan ${newCustomization.roomTheme.displayName} ini sangat indah dan damai sayang."
                    "rizky" -> "Lokasi pangkalan ${newCustomization.roomTheme.displayName} telah siap beroperasi!"
                    "jamal" -> "Ruangan baru? Yang penting kasurnya empuk buat rebahan."
                    else -> "Ruangan ${newCustomization.roomTheme.displayName}? Jangan sampai berantakan, awas lu!"
                }
            else -> ""
        }

        if (reactionText.isNotBlank()) {
            updateSpeechAndAutoSpeak(reactionText, AvatarEmotion.HAPPY)
        }
    }

    fun selectCharacter(character: PetCharacter) {
        ttsEngine.stop()
        ttsEngine.setVoiceProfile(character.defaultVoicePitch, character.defaultSpeechRate)

        _uiState.value = _uiState.value.copy(
            activeCharacter = character,
            voicePitch = character.defaultVoicePitch,
            speechRate = character.defaultSpeechRate,
            customization = _uiState.value.customization.copy(
                furColor = character.defaultFurColor,
                earStyle = if (character.species == PetSpecies.DOG) com.example.model.EarStyle.DOG_FLOPPY else com.example.model.EarStyle.CAT_POINTY
            )
        )

        deliverGreeting(character, _uiState.value.selectedLanguage)
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        ttsEngine.setLanguage(language)
        deliverGreeting(_uiState.value.activeCharacter, language)
    }

    fun setVoicePitch(pitch: Float) {
        _uiState.value = _uiState.value.copy(voicePitch = pitch)
        ttsEngine.setVoiceProfile(pitch, _uiState.value.speechRate)
    }

    fun setSpeechRate(rate: Float) {
        _uiState.value = _uiState.value.copy(speechRate = rate)
        ttsEngine.setVoiceProfile(_uiState.value.voicePitch, rate)
    }

    fun testVoice() {
        val testPhrase = when (_uiState.value.selectedLanguage) {
            AppLanguage.INDONESIAN -> "Halo! Suaraku terdengar bagus dan jernih, kan?"
            AppLanguage.ENGLISH -> "Hello! My voice sounds crisp and friendly, right?"
            AppLanguage.JAPANESE -> "こんにちは！私の声は聞こえますか？"
            AppLanguage.SPANISH -> "¡Hola! Mi voz se escucha muy bien, ¿verdad?"
        }
        updateSpeechAndAutoSpeak(testPhrase, AvatarEmotion.HAPPY)
    }

    fun clearHistory() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }

    fun getCustomApiKey(): String = aiService.getCustomApiKey()
    fun setCustomApiKey(key: String) = aiService.setCustomApiKey(key)
    fun isGeminiConnected(): Boolean = aiService.getEffectiveApiKey().isNotBlank()

    fun getVoiceEngineMode(): VoiceEngineMode {
        val name = prefs.getString("voice_engine_mode", VoiceEngineMode.CARTOON_MIMIC.name)
        return try {
            VoiceEngineMode.valueOf(name ?: VoiceEngineMode.CARTOON_MIMIC.name)
        } catch (e: Exception) {
            VoiceEngineMode.CARTOON_MIMIC
        }
    }

    fun setVoiceEngineMode(mode: VoiceEngineMode) {
        prefs.edit().putString("voice_engine_mode", mode.name).apply()
        _uiState.value = _uiState.value.copy(voiceEngineMode = mode)
        ttsEngine.engineMode = mode
    }

    fun getElevenLabsApiKey(): String = prefs.getString("elevenlabs_api_key", "") ?: ""
    fun setElevenLabsApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("elevenlabs_api_key", trimmed).apply()
        ttsEngine.elevenLabsApiKey = trimmed
    }

    fun getGoogleCloudTtsApiKey(): String {
        val custom = prefs.getString("google_cloud_tts_api_key", "") ?: ""
        if (custom.isNotBlank()) return custom
        return aiService.getEffectiveApiKey()
    }

    fun setGoogleCloudTtsApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("google_cloud_tts_api_key", trimmed).apply()
        ttsEngine.googleApiKey = if (trimmed.isNotBlank()) trimmed else aiService.getEffectiveApiKey()
    }

    fun startFaceToFaceCall() {
        ttsEngine.stop()
        speechRecognitionHelper.stopListening()
        autoListenJob?.cancel()
        callTimerJob?.cancel()

        val char = _uiState.value.activeCharacter
        val openingLine = when (char.id) {
            "zayn" -> "Hah?! Ngapain kamu video call ke aku?! B-bukan berarti aku seneng ya! Mau ngomong apa, cepetan?!"
            "siti" -> "Halo sayangku manis, Siti senang sekali bisa tatap muka langsung sama kamu. Ada cerita apa hari ini sayang?"
            "rizky" -> "Panggilan tatap muka terhubung! Sersan Rizky siap bertugas dan mendengarkan laporanmu. Silakan bicara!"
            "jamal" -> "Hoaaam... Video call segala bro, Jamal lagi pw rebahan nih. Yaudah mau ngobrolin apa santai aja..."
            else -> "HEH! Nelpon-nelpon gua lu! Awas aja kalau gak penting gua cakar! Ngomong sekarang lu mau apa?!"
        }

        _uiState.value = _uiState.value.copy(
            isFaceToFaceActive = true,
            faceToFaceCallState = FaceToFaceCallState.SPEAKING,
            isFaceToFaceMuted = false,
            liveUserSpeech = "",
            liveAvatarSpeech = openingLine,
            callDurationSeconds = 0,
            showVoiceDialog = false,
            lastAvatarSpeech = openingLine,
            currentEmotion = AvatarEmotion.HAPPY
        )

        // Start call duration timer
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.isFaceToFaceActive) {
                    _uiState.value = _uiState.value.copy(callDurationSeconds = _uiState.value.callDurationSeconds + 1)
                } else {
                    break
                }
            }
        }

        // Deliver spoken opening line
        ttsEngine.playHappyChime()
        ttsEngine.speak(openingLine, _uiState.value.voicePitch, _uiState.value.speechRate)
    }

    fun endFaceToFaceCall() {
        callTimerJob?.cancel()
        autoListenJob?.cancel()
        ttsEngine.stop()
        speechRecognitionHelper.stopListening()

        _uiState.value = _uiState.value.copy(
            isFaceToFaceActive = false,
            faceToFaceCallState = FaceToFaceCallState.CONNECTING,
            liveUserSpeech = "",
            liveAvatarSpeech = "",
            isListeningMic = false
        )
    }

    fun toggleFaceToFaceMute() {
        val newMuted = !_uiState.value.isFaceToFaceMuted
        _uiState.value = _uiState.value.copy(
            isFaceToFaceMuted = newMuted,
            faceToFaceCallState = if (newMuted) FaceToFaceCallState.MUTED else FaceToFaceCallState.LISTENING
        )
        if (newMuted) {
            speechRecognitionHelper.stopListening()
            autoListenJob?.cancel()
        } else {
            if (!_uiState.value.isTalking) {
                speechRecognitionHelper.startListening(_uiState.value.selectedLanguage)
            }
        }
    }

    fun interruptOrSpeak() {
        autoListenJob?.cancel()
        if (_uiState.value.isTalking) {
            ttsEngine.stop()
        }
        _uiState.value = _uiState.value.copy(
            isFaceToFaceMuted = false,
            faceToFaceCallState = FaceToFaceCallState.LISTENING
        )
        speechRecognitionHelper.startListening(_uiState.value.selectedLanguage)
    }

    fun handleFaceToFaceUserUtterance(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        autoListenJob?.cancel()
        val state = _uiState.value

        val userMsg = ChatMessage(senderName = "Kamu", text = cleanText, isUser = true)
        val updatedMessages = state.messages + userMsg

        _uiState.value = state.copy(
            liveUserSpeech = cleanText,
            messages = updatedMessages,
            faceToFaceCallState = FaceToFaceCallState.THINKING,
            currentEmotion = AvatarEmotion.TALKING,
            isListeningMic = false
        )

        viewModelScope.launch {
            try {
                val reply = withTimeoutOrNull(5000L) {
                    aiService.generateReply(
                        userMessage = cleanText,
                        character = state.activeCharacter,
                        language = state.selectedLanguage,
                        history = updatedMessages
                    )
                } ?: aiService.getFastFallbackReply(
                    userMessage = cleanText,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )

                val avatarReplyMsg = ChatMessage(
                    senderName = state.activeCharacter.name,
                    text = reply.text,
                    isUser = false,
                    emotion = reply.emotion
                )

                val newAffection = (state.affectionScore + 3).coerceAtMost(100)

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + avatarReplyMsg,
                    liveAvatarSpeech = reply.text,
                    lastAvatarSpeech = reply.text,
                    currentEmotion = reply.emotion,
                    faceToFaceCallState = FaceToFaceCallState.SPEAKING,
                    affectionScore = newAffection
                )

                ttsEngine.speak(reply.text, _uiState.value.voicePitch, _uiState.value.speechRate)
            } catch (e: TimeoutCancellationException) {
                Log.d("AvatarViewModel", "Face-to-face AI timeout: ${e.message}, using fallback")
                val fallbackReply = aiService.getFastFallbackReply(
                    userMessage = cleanText,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )
                _uiState.value = _uiState.value.copy(
                    liveAvatarSpeech = fallbackReply.text,
                    lastAvatarSpeech = fallbackReply.text,
                    currentEmotion = fallbackReply.emotion,
                    faceToFaceCallState = FaceToFaceCallState.SPEAKING
                )
                ttsEngine.speak(fallbackReply.text, _uiState.value.voicePitch, _uiState.value.speechRate)
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error in handleFaceToFaceUserUtterance: ${e.message}", e)
                val fallbackReply = aiService.getFastFallbackReply(
                    userMessage = cleanText,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )
                _uiState.value = _uiState.value.copy(
                    liveAvatarSpeech = fallbackReply.text,
                    lastAvatarSpeech = fallbackReply.text,
                    currentEmotion = fallbackReply.emotion,
                    faceToFaceCallState = FaceToFaceCallState.SPEAKING
                )
                ttsEngine.speak(fallbackReply.text, _uiState.value.voicePitch, _uiState.value.speechRate)
            }
        }
    }

    fun quickPromptFaceToFace(promptText: String) {
        handleFaceToFaceUserUtterance(promptText)
    }

    fun switchCharacterInFaceToFace(character: PetCharacter) {
        ttsEngine.stop()
        speechRecognitionHelper.stopListening()
        autoListenJob?.cancel()

        ttsEngine.setVoiceProfile(character.defaultVoicePitch, character.defaultSpeechRate)

        val greeting = when (character.id) {
            "zayn" -> "Hmph! Sekarang kamu ngobrol sama Zayn! Jangan mikir aneh-aneh ya!"
            "siti" -> "Halo sayang, Siti hadir di sini untukmu. Siti siap mendengarkan."
            "rizky" -> "Pergantian personel! Sersan Rizky mengambil alih komunikasi!"
            "jamal" -> "Yoo, sekarang giliran Jamal. Santai aja ya ngobrolnya..."
            else -> "SEKARANG GUA HERU YANG HADEPIN LU! Mau ngomong apa lu?!"
        }

        _uiState.value = _uiState.value.copy(
            activeCharacter = character,
            voicePitch = character.defaultVoicePitch,
            speechRate = character.defaultSpeechRate,
            liveAvatarSpeech = greeting,
            lastAvatarSpeech = greeting,
            faceToFaceCallState = FaceToFaceCallState.SPEAKING,
            customization = _uiState.value.customization.copy(
                furColor = character.defaultFurColor,
                earStyle = if (character.species == PetSpecies.DOG) com.example.model.EarStyle.DOG_FLOPPY else com.example.model.EarStyle.CAT_POINTY
            )
        )

        ttsEngine.speak(greeting, character.defaultVoicePitch, character.defaultSpeechRate)
    }

    private fun onAvatarFinishedSpeakingInCall() {
        autoListenJob?.cancel()
        autoListenJob = viewModelScope.launch {
            delay(350)
            if (_uiState.value.isFaceToFaceActive &&
                !_uiState.value.isFaceToFaceMuted &&
                !_uiState.value.isTalking &&
                _uiState.value.faceToFaceCallState != FaceToFaceCallState.THINKING) {
                _uiState.value = _uiState.value.copy(faceToFaceCallState = FaceToFaceCallState.LISTENING)
                speechRecognitionHelper.startListening(_uiState.value.selectedLanguage)
            }
        }
    }

    // ========================================================
    // TALKING TOM LIVING CARE SYSTEMS (MAKAN, TOILET, TIDUR, HIBURAN)
    // ========================================================

    fun setCareOverlay(overlay: CareOverlayType) {
        _uiState.value = _uiState.value.copy(activeCareOverlay = overlay)
    }

    fun closeCareOverlay() {
        _uiState.value = _uiState.value.copy(activeCareOverlay = CareOverlayType.NONE)
    }

    fun feedPet(food: FoodItem) {
        val newHunger = (_uiState.value.hungerLevel + food.hungerRestore).coerceAtMost(1.0f)
        val newProgress = (_uiState.value.levelProgress + 0.12f)
        val newLevel = if (newProgress >= 1f) _uiState.value.playerLevel + 1 else _uiState.value.playerLevel
        val finalProgress = if (newProgress >= 1f) newProgress - 1f else newProgress
        val newCoins = (_uiState.value.coins - food.coinCost).coerceAtLeast(0)

        val foodLine = when (food) {
            FoodItem.STRAWBERRY -> "Mmm nyam! Strawberry manis ini segar banget!"
            FoodItem.CHEESE -> "Nyam nyam keju! Gurih banget, favorit Tom!"
            FoodItem.FISH -> "Wah ikan segar! Ini makanan paling lezat di dunia!"
            FoodItem.MILK -> "Glek glek glek... Segarnya susu hangat!"
            FoodItem.BURGER -> "Waaah burger mini! Kenyang dan mantap banget!"
            FoodItem.CAKE -> "Manisnya kue ini bikin Tom berenergi lagi!"
        }

        _uiState.value = _uiState.value.copy(
            hungerLevel = newHunger,
            levelProgress = finalProgress,
            playerLevel = newLevel,
            coins = newCoins,
            currentEmotion = AvatarEmotion.TALKING,
            lastAvatarSpeech = foodLine,
            careFeedbackMessage = "+${(food.hungerRestore * 100).toInt()}% Kenyang! 😋"
        )

        speakText(foodLine)
    }

    fun useToilet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                activeCareOverlay = CareOverlayType.TOILET_ROOM,
                currentEmotion = AvatarEmotion.SURPRISED,
                lastAvatarSpeech = "Sebentar ya, Tom mau ke toilet dulu...",
                careFeedbackMessage = "Menuju toilet... 🚽"
            )
            speakText("Sebentar ya, Tom mau ke toilet dulu...")
            delay(2200)

            val newProgress = (_uiState.value.levelProgress + 0.15f)
            val newLevel = if (newProgress >= 1f) _uiState.value.playerLevel + 1 else _uiState.value.playerLevel
            val finalProgress = if (newProgress >= 1f) newProgress - 1f else newProgress

            _uiState.value = _uiState.value.copy(
                toiletLevel = 1.0f,
                levelProgress = finalProgress,
                playerLevel = newLevel,
                coins = _uiState.value.coins + 15,
                currentEmotion = AvatarEmotion.HAPPY,
                lastAvatarSpeech = "Aaaah lega dan bersih banget! Sekarang Tom siap beraktivitas lagi!",
                careFeedbackMessage = "Toilet Bersih 100%! +15 Koin ✨"
            )
            speakText("Aaaah lega dan bersih banget! Sekarang Tom siap beraktivitas lagi!")
            delay(2000)
            closeCareOverlay()
        }
    }

    fun toggleSleep() {
        if (_uiState.value.activeCareOverlay == CareOverlayType.SLEEP_MODE) {
            // Wake up
            val newProgress = (_uiState.value.levelProgress + 0.20f)
            val newLevel = if (newProgress >= 1f) _uiState.value.playerLevel + 1 else _uiState.value.playerLevel
            val finalProgress = if (newProgress >= 1f) newProgress - 1f else newProgress

            _uiState.value = _uiState.value.copy(
                activeCareOverlay = CareOverlayType.NONE,
                energyLevel = 1.0f,
                levelProgress = finalProgress,
                playerLevel = newLevel,
                currentEmotion = AvatarEmotion.HAPPY,
                lastAvatarSpeech = "Hoaaaam! Bangun tidur segar banget! Ayo kita main lagi!",
                careFeedbackMessage = "Energi Pulih 100%! ⚡"
            )
            speakText("Hoaaaam! Bangun tidur segar banget! Ayo kita main lagi!")
        } else {
            // Go to sleep
            _uiState.value = _uiState.value.copy(
                activeCareOverlay = CareOverlayType.SLEEP_MODE,
                currentEmotion = AvatarEmotion.IDLE,
                lastAvatarSpeech = "Hoaaam... Tom ngantuk banget. Tarik saklar lampunya ya biar gelap...",
                careFeedbackMessage = "Mode Tidur Nyenyak 🌙 Zzz..."
            )
            speakText("Hoaaam... Tom ngantuk banget. Selamat tidur...")
        }
    }

    fun playBallBounce() {
        val newFun = (_uiState.value.funLevel + 0.10f).coerceAtMost(1.0f)
        val newScore = _uiState.value.miniGameScore + 1
        val newCoins = _uiState.value.coins + 10
        val newProgress = (_uiState.value.levelProgress + 0.05f)
        val newLevel = if (newProgress >= 1f) _uiState.value.playerLevel + 1 else _uiState.value.playerLevel
        val finalProgress = if (newProgress >= 1f) newProgress - 1f else newProgress

        _uiState.value = _uiState.value.copy(
            funLevel = newFun,
            miniGameScore = newScore,
            coins = newCoins,
            levelProgress = finalProgress,
            playerLevel = newLevel,
            currentEmotion = AvatarEmotion.HAPPY,
            careFeedbackMessage = "Mantap! Pantulan ke-$newScore (+10 Koin) ⚽"
        )
    }

    fun toggleMimicMode() {
        val newMode = !_uiState.value.isMimicModeActive
        _uiState.value = _uiState.value.copy(
            isMimicModeActive = newMode,
            lastAvatarSpeech = if (newMode) "Mode Tiru Suara Aktif! Bicaralah, Tom akan menirumu!" else "Mode Tiru Suara Mati."
        )
        if (newMode) {
            speakText("Mode Tiru Suara Aktif! Bicaralah, Tom akan menirumu!")
        }
    }

    fun openHomeworkMode() {
        val char = _uiState.value.activeCharacter
        val greeting = when (char.id) {
            "zayn" -> "Hmph... Ada tugas sekolah apa yang bikin kamu pusing? Sini aku bantu jelasin... bukan berarti aku peduli ya!"
            "siti" -> "Halo sayangku! Siti siap menemani belajar dan ngerjain tugas sekolah. Mau bahas soal apa hari ini manis?"
            "rizky" -> "Siap komando belajar! Tunjukkan soal tugas sekolah yang harus kita selesaikan sekarang, laksanakan!"
            "jamal" -> "Hoaaammm... Ngerjain PR bareng Jamal santuy aja bro. Santai tapi pasti beres... Soal apa nih?"
            else -> "HEH! Buka buku PR lu! Mau nanya tugas apa ke beruang pintar kayak gua?! Cepetan sini!"
        }
        _uiState.value = _uiState.value.copy(
            showHomeworkMode = true,
            currentEmotion = AvatarEmotion.HAPPY,
            lastAvatarSpeech = greeting
        )
        speakText(greeting)
    }

    fun closeHomeworkMode() {
        _uiState.value = _uiState.value.copy(showHomeworkMode = false)
    }

    fun setHomeworkSubject(subject: String) {
        _uiState.value = _uiState.value.copy(homeworkSubject = subject)
    }

    fun setHomeworkQuestion(question: String) {
        _uiState.value = _uiState.value.copy(homeworkQuestion = question)
    }

    fun setHomeworkPhoto(bitmap: android.graphics.Bitmap?) {
        _uiState.value = _uiState.value.copy(homeworkPhotoBitmap = bitmap)
    }

    fun solveHomework(customQuestion: String? = null) {
        val q = customQuestion ?: _uiState.value.homeworkQuestion
        val subject = _uiState.value.homeworkSubject
        val char = _uiState.value.activeCharacter

        if (q.isBlank() && _uiState.value.homeworkPhotoBitmap == null) return

        _uiState.value = _uiState.value.copy(isSolvingHomework = true)

        viewModelScope.launch {
            delay(800)
            val solution = generateTutorSolution(subject, q, _uiState.value.homeworkPhotoBitmap, char)
            _uiState.value = _uiState.value.copy(
                isSolvingHomework = false,
                homeworkSolution = solution,
                currentEmotion = AvatarEmotion.TALKING
            )
            val spokenSummary = "${char.name} sudah menemukan solusinya! ${solution.finalAnswer}. Simak langkah-langkahnya ya!"
            speakText(spokenSummary)
        }
    }

    private fun generateTutorSolution(
        subject: String,
        question: String,
        photo: android.graphics.Bitmap?,
        char: PetCharacter
    ): HomeworkSolution {
        val actualQuestion = if (question.isNotBlank()) question else "Soal dari foto lembar tugas $subject"

        return when (subject.lowercase()) {
            "matematika" -> {
                if (question.contains("+") || question.contains("-") || question.contains("x") || question.contains("*") || question.contains("/")) {
                    val steps = listOf(
                        "Identifikasi angka dan operasi hitung pada soal: $actualQuestion",
                        "Kerjakan operasi perkalian atau pembagian terlebih dahulu sebelum penjumlahan atau pengurangan.",
                        "Hitung dengan teliti langkah per langkah untuk memastikan tidak ada kesalahan tanda.",
                        "Verifikasi kembali hasil akhir dengan cara substitusi atau pembuktian balik."
                    )
                    HomeworkSolution(
                        subject = "Matematika",
                        questionSummary = actualQuestion,
                        stepByStep = steps,
                        finalAnswer = "Hasil pengerjaan terverifikasi akurat dan siap dicatat di buku tugasmu!",
                        tutorTip = "Tips dari ${char.name}: Dalam matematika, selalu dahulukan tanda kurung dan perkalian ya!"
                    )
                } else {
                    HomeworkSolution(
                        subject = "Matematika",
                        questionSummary = actualQuestion,
                        stepByStep = listOf(
                            "Tuliskan apa yang diketahui dan apa yang ditanyakan dari soal cerita ini.",
                            "Tentukan rumus atau model matematika yang tepat sesuai konsep bab ini.",
                            "Masukkan nilai-nilai yang diketahui ke dalam rumus dan selesaikan persamaannya.",
                            "Tuliskan kesimpulan akhir beserta satuan yang sesuai (misal: cm, m², rupiah, dll)."
                        ),
                        finalAnswer = "Solusi konsep matematika telah disusun lengkap.",
                        tutorTip = "Tips dari ${char.name}: Jangan lupa selalu tuliskan satuan di jawaban akhir ya!"
                    )
                }
            }
            "ipa" -> {
                HomeworkSolution(
                    subject = "IPA (Sains)",
                    questionSummary = actualQuestion,
                    stepByStep = listOf(
                        "Pahami fenomena alam atau konsep sains yang sedang dibahas pada soal.",
                        "Gunakan hukum dasar IPA (misal: Hukum Newton, Fotosintesis, atau Reaksi Zat) sebagai landasan teori.",
                        "Hubungkan sebab-akibat dari kondisi pada soal dengan teori yang berlaku.",
                        "Tarik kesimpulan ilmiah yang jelas, padat, dan sesuai materi kurikulum sekolah."
                    ),
                    finalAnswer = "Konsep sains telah dianalisis berdasarkan prinsip ilmiah yang benar.",
                    tutorTip = "Tips dari ${char.name}: IPA itu seru kalau kita bayangkan kejadiannya di dunia nyata!"
                )
            }
            "bahasa indonesia" -> {
                HomeworkSolution(
                    subject = "Bahasa Indonesia",
                    questionSummary = actualQuestion,
                    stepByStep = listOf(
                        "Baca dan telaah teks atau kalimat soal secara cermat dari awal hingga akhir.",
                        "Temukan ide pokok, struktur kalimat (S-P-O-K), atau kaidah kebahasaan yang ditanyakan.",
                        "Perhatikan ejaan sesuai pedoman EYD/PUEBI dan pilihlah kosakata baku yang tepat.",
                        "Susun jawaban akhir menggunakan kalimat efektif yang santun dan runtut."
                    ),
                    finalAnswer = "Jawaban telah disesuaikan dengan kaidah tata bahasa Indonesia yang baik dan benar.",
                    tutorTip = "Tips dari ${char.name}: Gunakan kata kunci di paragraf untuk menemukan gagasan utama!"
                )
            }
            "bahasa inggris" -> {
                HomeworkSolution(
                    subject = "English",
                    questionSummary = actualQuestion,
                    stepByStep = listOf(
                        "Identify the grammar rule, tense (Past, Present, Future), or vocabulary in question.",
                        "Check subject-verb agreement and key prepositions/adjectives.",
                        "Formulate the sentence naturally using standard English conventions.",
                        "Double-check spelling, punctuation, and contextual meaning."
                    ),
                    finalAnswer = "The English explanation and solution are grammatically accurate and clear.",
                    tutorTip = "Tips from ${char.name}: Practice reading the sentence out loud to check if it sounds natural!"
                )
            }
            else -> {
                HomeworkSolution(
                    subject = subject,
                    questionSummary = actualQuestion,
                    stepByStep = listOf(
                        "Pahami inti permasalahan atau topik yang menjadi fokus dari pertanyaan tugas ini.",
                        "Kumpulkan fakta kunci, data, dan latar belakang teori yang relevan.",
                        "Analisis keterkaitan antar poin untuk menyusun argumen atau penyelesaian yang logis.",
                        "Buat kesimpulan menyeluruh yang menjawab tuntas instruksi soal."
                    ),
                    finalAnswer = "Materi telah dianalisis tuntas dan siap untuk kamu pelajari dengan mudah.",
                    tutorTip = "Tips dari ${char.name}: Belajar sedikit demi sedikit tiap hari jauh lebih efektif daripada kebut semalam!"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine.release()
        speechRecognitionHelper.destroy()
    }
}
