package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
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
import com.example.data.CharacterStateManager

import com.example.model.CareOverlayType
import com.example.model.FoodItem
import com.example.ai.GeminiStudyCompanionHelper
import com.example.model.AccessoryStyle
import com.example.model.CostumeStyle
import com.example.model.FlashcardCatalog
import com.example.model.MoodJournalEntry
import com.example.model.MoodType
import com.example.model.PomodoroPreset
import com.example.model.QuizCatalog
import com.example.model.QuizQuestion
import com.example.model.SpecialPetFood
import com.example.model.SpecialPetFoodCatalog
import com.example.model.StudyFlashcard
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    val voicePitch: Float = 0.96f,
    val speechRate: Float = 1.04f,
    val voiceEngineMode: VoiceEngineMode = VoiceEngineMode.ELEVENLABS_NEURAL,
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
    // Mode Ngerjain Tugas Sekolah & Scan Foto Soal (Multimodal Vision)
    val showHomeworkMode: Boolean = false,
    val homeworkSubject: String = "Matematika",
    val homeworkQuestion: String = "",
    val homeworkSolution: HomeworkSolution? = null,
    val isSolvingHomework: Boolean = false,
    val homeworkPhotoBitmap: android.graphics.Bitmap? = null,
    // 6 New Study & Companion Features
    val showQuizSheet: Boolean = false,
    val quizStreak: Int = 0,
    val showPomodoroSheet: Boolean = false,
    val activePomodoroPreset: PomodoroPreset = PomodoroPreset.FOCUS_25,
    val pomodoroRemainingSeconds: Int = 25 * 60,
    val isPomodoroRunning: Boolean = false,
    val pomodoroCompletedSessions: Int = 0,
    val showPetCareShopSheet: Boolean = false,
    val showMoodJournalSheet: Boolean = false,
    val moodJournalEntries: List<MoodJournalEntry> = emptyList(),
    val isSavingMoodEntry: Boolean = false,
    val showFlashcardSheet: Boolean = false,
    val flashcards: List<StudyFlashcard> = FlashcardCatalog.getDefaultDeck("Matematika"),
    val isGeneratingFlashcards: Boolean = false,
    val isRoomDbActive: Boolean = true
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
    private val studyCompanionHelper = GeminiStudyCompanionHelper(aiService)
    private var emotionResetJob: Job? = null
    private var callTimerJob: Job? = null
    private var autoListenJob: Job? = null
    private var pomodoroTimerJob: Job? = null

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

    val characterStateManager: CharacterStateManager = CharacterStateManager.getInstance(application)

    init {
        val initialChar = _uiState.value.activeCharacter
        val initialMode = getVoiceEngineMode()
        val savedCoins = prefs.getInt("saved_petto_coins", 1500)
        val savedMoodEntries = loadMoodEntriesFromPrefs()

        _uiState.value = _uiState.value.copy(
            voicePitch = initialChar.defaultVoicePitch,
            speechRate = initialChar.defaultSpeechRate,
            voiceEngineMode = initialMode,
            coins = savedCoins,
            moodJournalEntries = savedMoodEntries,
            customization = _uiState.value.customization.copy(furColor = initialChar.defaultFurColor)
        )

        ttsEngine.engineMode = initialMode
        ttsEngine.elevenLabsApiKey = getElevenLabsApiKey()
        ttsEngine.googleApiKey = getGoogleCloudTtsApiKey()
        ttsEngine.setVoiceProfile(initialChar.defaultVoicePitch, initialChar.defaultSpeechRate, initialChar)
        ttsEngine.setLanguage(_uiState.value.selectedLanguage)

        viewModelScope.launch {
            val restored = try {
                characterStateManager.loadOrInitializeState(
                    legacyCoins = savedCoins,
                    legacyVoiceMode = initialMode,
                    legacyMoodEntries = savedMoodEntries
                )
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Failed to load state from Room: ${e.message}", e)
                null
            }

            if (restored != null) {
                val activeVoiceMode = if (getElevenLabsApiKey().isNotBlank() && !prefs.contains("voice_engine_mode_v3")) {
                    VoiceEngineMode.ELEVENLABS_NEURAL
                } else {
                    restored.voiceEngineMode
                }
                _uiState.value = _uiState.value.copy(
                    activeCharacter = restored.activeCharacter,
                    customization = restored.customization,
                    selectedLanguage = restored.selectedLanguage,
                    backgroundTheme = restored.backgroundTheme,
                    voiceEngineMode = activeVoiceMode,
                    voicePitch = restored.voicePitch,
                    speechRate = restored.speechRate,
                    affectionScore = restored.affectionScore,
                    coins = restored.coins,
                    diamonds = restored.diamonds,
                    playerLevel = restored.playerLevel,
                    levelProgress = restored.levelProgress,
                    hungerLevel = restored.hungerLevel,
                    toiletLevel = restored.toiletLevel,
                    energyLevel = restored.energyLevel,
                    funLevel = restored.funLevel,
                    quizStreak = restored.quizStreak,
                    pomodoroCompletedSessions = restored.pomodoroCompletedSessions,
                    messages = if (restored.messages.isNotEmpty()) restored.messages else _uiState.value.messages,
                    moodJournalEntries = if (restored.moodJournalEntries.isNotEmpty()) restored.moodJournalEntries else _uiState.value.moodJournalEntries,
                    isRoomDbActive = true
                )
                ttsEngine.engineMode = activeVoiceMode
                ttsEngine.elevenLabsApiKey = getElevenLabsApiKey()
                ttsEngine.setVoiceProfile(
                    restored.voicePitch,
                    restored.speechRate,
                    restored.activeCharacter
                )
                ttsEngine.setLanguage(restored.selectedLanguage)
            }

            delay(300)
            deliverGreeting(_uiState.value.activeCharacter, _uiState.value.selectedLanguage)
        }
    }

    private fun persistCurrentState() {
        val snapshot = _uiState.value
        viewModelScope.launch {
            try {
                characterStateManager.saveFullState(
                    activeCharacter = snapshot.activeCharacter,
                    customization = snapshot.customization,
                    selectedLanguage = snapshot.selectedLanguage,
                    backgroundTheme = snapshot.backgroundTheme,
                    voiceEngineMode = snapshot.voiceEngineMode,
                    voicePitch = snapshot.voicePitch,
                    speechRate = snapshot.speechRate,
                    affectionScore = snapshot.affectionScore,
                    coins = snapshot.coins,
                    diamonds = snapshot.diamonds,
                    playerLevel = snapshot.playerLevel,
                    levelProgress = snapshot.levelProgress,
                    hungerLevel = snapshot.hungerLevel,
                    toiletLevel = snapshot.toiletLevel,
                    energyLevel = snapshot.energyLevel,
                    funLevel = snapshot.funLevel,
                    quizStreak = snapshot.quizStreak,
                    pomodoroCompletedSessions = snapshot.pomodoroCompletedSessions,
                    messages = snapshot.messages,
                    moodJournalEntries = snapshot.moodJournalEntries
                )
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Failed to persist character state: ${e.message}", e)
            }
        }
    }

    private fun deliverGreeting(character: PetCharacter, language: AppLanguage) {
        val greetingText = when (character.id) {
            "zayn" -> "Cih! Aku Zayn. Ngapain kamu ke sini? Bukan berarti aku seneng ditemenin ya, cepetan mau ngobrol apa?"
            "siti" -> "Halo sayangku manis, Siti di sini. Siti senang sekali bisa menemanimu hari ini. Ceritakan harimu ya sayang."
            "mochi" -> "Hai bestie! Aku Mochi! Wah seru banget kita bisa ngobrol bareng hari ini, mau bahas apa nih kita?"
            "rizky" -> "Siap! Rizky siap bertugas! Pertahankan semangat dan fokus pada target kita hari ini, kawan!"
            "jamal" -> "Hoaam, Jamal lagi santuy banget nih bro. Kalau mau ngobrol sambil rebahan, sini merapat."
            else -> "Heh! Gua Heru nih! Jangan sungkan kalau mau ngobrol atau tanya tugas sama beruang keren ini ya!"
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
        val streamingMsgId = UUID.randomUUID().toString()
        _uiState.value = state.copy(
            messages = updatedMessages,
            isLoadingAi = true,
            currentEmotion = AvatarEmotion.TALKING
        )

        viewModelScope.launch {
            var spokenFirstSentencePrefix: String? = null
            try {
                // Real-time streaming AI reply via Firebase AI Logic (gemini-3.5-flash-lite primary)
                val reply = withTimeoutOrNull(30_000L) {
                    aiService.generateReplyStream(
                        userMessage = text,
                        character = state.activeCharacter,
                        language = state.selectedLanguage,
                        history = updatedMessages,
                        onPartialText = { partialText ->
                            val currentMsgs = _uiState.value.messages
                            val existingIndex = currentMsgs.indexOfFirst { it.id == streamingMsgId }
                            val updatedStreamList = if (existingIndex >= 0) {
                                currentMsgs.toMutableList().apply {
                                    this[existingIndex] = this[existingIndex].copy(text = partialText)
                                }
                            } else {
                                currentMsgs + ChatMessage(
                                    id = streamingMsgId,
                                    senderName = state.activeCharacter.name,
                                    text = partialText,
                                    isUser = false,
                                    emotion = AvatarEmotion.TALKING
                                )
                            }
                            _uiState.value = _uiState.value.copy(
                                messages = updatedStreamList,
                                lastAvatarSpeech = partialText,
                                isLoadingAi = false,
                                currentEmotion = AvatarEmotion.TALKING
                            )

                            // Start speaking the first complete sentence immediately for ultra-low voice latency
                            // when using local natural voice; if a Cloud/Gemini Native TTS API key is active,
                            // synthesize the complete response with Gemini Native Audio TTS for studio vocal quality.
                            if (!ttsEngine.hasCloudNeuralKey() && spokenFirstSentencePrefix == null && partialText.length >= 16) {
                                val clauses = ttsEngine.splitIntoNaturalClauses(partialText)
                                if (clauses.size >= 2 && clauses.first().length >= 10) {
                                    val firstSentence = clauses.first()
                                    spokenFirstSentencePrefix = firstSentence
                                    ttsEngine.speakStreamedSentence(
                                        sentenceText = firstSentence,
                                        pitch = _uiState.value.voicePitch,
                                        rate = _uiState.value.speechRate,
                                        character = state.activeCharacter,
                                        flushQueue = true
                                    )
                                }
                            }
                        }
                    )
                } ?: aiService.getFastFallbackReply(
                    userMessage = text,
                    character = state.activeCharacter,
                    language = state.selectedLanguage,
                    history = updatedMessages
                )

                val finalAvatarMsg = ChatMessage(
                    id = streamingMsgId,
                    senderName = state.activeCharacter.name,
                    text = reply.text,
                    isUser = false,
                    emotion = reply.emotion
                )

                val currentMsgs = _uiState.value.messages
                val existingIndex = currentMsgs.indexOfFirst { it.id == streamingMsgId }
                val finalMessages = if (existingIndex >= 0) {
                    currentMsgs.toMutableList().apply { this[existingIndex] = finalAvatarMsg }
                } else {
                    currentMsgs + finalAvatarMsg
                }

                val newAffection = (state.affectionScore + 3).coerceAtMost(100)

                _uiState.value = _uiState.value.copy(
                    messages = finalMessages,
                    lastAvatarSpeech = reply.text,
                    currentEmotion = reply.emotion,
                    isLoadingAi = false,
                    affectionScore = newAffection
                )
                persistCurrentState()

                val alreadySpoken = spokenFirstSentencePrefix
                if (alreadySpoken != null && reply.text.startsWith(alreadySpoken)) {
                    val remaining = reply.text.removePrefix(alreadySpoken).trim()
                    if (remaining.isNotBlank()) {
                        ttsEngine.speakStreamedSentence(
                            sentenceText = remaining,
                            pitch = _uiState.value.voicePitch,
                            rate = _uiState.value.speechRate,
                            character = state.activeCharacter,
                            flushQueue = false
                        )
                    }
                } else {
                    updateSpeechAndAutoSpeak(reply.text, reply.emotion)
                }
            } catch (e: TimeoutCancellationException) {
                Log.d("AvatarViewModel", "AI response timeout: ${e.message}, using fallback")
                val fallbackReply = aiService.getFastFallbackReply(
                    userMessage = text,
                    character = state.activeCharacter,
                    language = state.selectedLanguage,
                    history = updatedMessages
                )
                val avatarReplyMsg = ChatMessage(
                    id = streamingMsgId,
                    senderName = state.activeCharacter.name,
                    text = fallbackReply.text,
                    isUser = false,
                    emotion = fallbackReply.emotion
                )
                val currentMsgs = _uiState.value.messages.filterNot { it.id == streamingMsgId }
                _uiState.value = _uiState.value.copy(
                    messages = currentMsgs + avatarReplyMsg,
                    isLoadingAi = false
                )
                updateSpeechAndAutoSpeak(fallbackReply.text, fallbackReply.emotion)
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error in sendMessage: ${e.message}", e)
                val fallbackReply = aiService.getFastFallbackReply(
                    userMessage = text,
                    character = state.activeCharacter,
                    language = state.selectedLanguage,
                    history = updatedMessages
                )
                val avatarReplyMsg = ChatMessage(
                    id = streamingMsgId,
                    senderName = state.activeCharacter.name,
                    text = fallbackReply.text,
                    isUser = false,
                    emotion = fallbackReply.emotion
                )
                val currentMsgs = _uiState.value.messages.filterNot { it.id == streamingMsgId }
                _uiState.value = _uiState.value.copy(
                    messages = currentMsgs + avatarReplyMsg,
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
        persistCurrentState()
        updateSpeechAndAutoSpeak(reactionText, emotion)
    }

    fun petAvatar() {
        onCharacterTouched(TouchZone.HEAD)
    }

    fun setBackgroundTheme(theme: BackgroundTheme) {
        val oldTheme = _uiState.value.backgroundTheme
        _uiState.value = _uiState.value.copy(backgroundTheme = theme)
        persistCurrentState()
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
            ttsEngine.speak(
                text = speech,
                pitch = _uiState.value.voicePitch,
                rate = _uiState.value.speechRate,
                character = _uiState.value.activeCharacter
            )
        }
    }

    fun speakText(text: String) {
        updateSpeechAndAutoSpeak(text, AvatarEmotion.TALKING)
    }

    /**
     * Previews a specific character's voice with their exact gender, persona, pitch, and speed
     * without requiring the user to close the CharacterPickerDialog first.
     */
    fun previewCharacterVoice(character: PetCharacter) {
        val sampleLine = when (character.id) {
            "zayn" -> "Halo, aku Zayn. Suaraku suara cowok muda yang santai dan cool, kan? Bukan berarti aku pamer ya."
            "siti" -> "Halo sayangku, ini suara lembut Siti. Siti siap menemani dan mendengarkan ceritamu dengan hangat."
            "mochi" -> "Hai bestie! Ini suara ceria Mochi! Yuk kita ngobrol seru dan belajar bareng setiap hari!"
            "rizky" -> "Siap komando! Ini suara bariton Rizky yang tegas, lugas, dan jelas! Siap membantu kapan saja!"
            "jamal" -> "Yoo bro, ini suara kalem Jamal. Enaknya kita ngobrol santuy sambil rebahan aja nih."
            else -> "Heh! Ini suara bass berat punya Heru si beruang! Mantap dan menggelegar banget kan?!"
        }
        _uiState.value = _uiState.value.copy(
            lastAvatarSpeech = sampleLine,
            currentEmotion = AvatarEmotion.TALKING
        )
        ttsEngine.speak(
            text = sampleLine,
            pitch = character.defaultVoicePitch,
            rate = character.defaultSpeechRate,
            character = character
        )
    }

    fun updateCustomization(newCustomization: AvatarCustomization) {
        val old = _uiState.value.customization
        val char = _uiState.value.activeCharacter
        val lang = _uiState.value.selectedLanguage
        _uiState.value = _uiState.value.copy(customization = newCustomization)
        persistCurrentState()

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
                    "mochi" -> "Waaah ruangan ${newCustomization.roomTheme.displayName} ini estetik banget bestie!"
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
        // Save the outgoing character's state before switching
        persistCurrentState()
        ttsEngine.stop()

        viewModelScope.launch {
            val savedCharEntity = try {
                characterStateManager.getOrCreateCharacterCustomization(character)
            } catch (e: Exception) {
                null
            }
            val restoredCustomization = savedCharEntity?.toAvatarCustomization(character)
                ?: _uiState.value.customization.copy(
                    furColor = character.defaultFurColor,
                    earStyle = if (character.species == PetSpecies.DOG) com.example.model.EarStyle.DOG_FLOPPY else com.example.model.EarStyle.CAT_POINTY
                )
            val restoredPitch = savedCharEntity?.voicePitch ?: character.defaultVoicePitch
            val restoredRate = savedCharEntity?.speechRate ?: character.defaultSpeechRate

            ttsEngine.setVoiceProfile(restoredPitch, restoredRate, character)

            _uiState.value = _uiState.value.copy(
                activeCharacter = character,
                voicePitch = restoredPitch,
                speechRate = restoredRate,
                affectionScore = savedCharEntity?.affectionScore ?: _uiState.value.affectionScore,
                hungerLevel = savedCharEntity?.hungerLevel ?: _uiState.value.hungerLevel,
                toiletLevel = savedCharEntity?.toiletLevel ?: _uiState.value.toiletLevel,
                energyLevel = savedCharEntity?.energyLevel ?: _uiState.value.energyLevel,
                funLevel = savedCharEntity?.funLevel ?: _uiState.value.funLevel,
                customization = restoredCustomization
            )
            persistCurrentState()
            deliverGreeting(character, _uiState.value.selectedLanguage)
        }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        ttsEngine.setLanguage(language)
        persistCurrentState()
        deliverGreeting(_uiState.value.activeCharacter, language)
    }

    fun setVoicePitch(pitch: Float) {
        _uiState.value = _uiState.value.copy(voicePitch = pitch)
        ttsEngine.setVoiceProfile(pitch, _uiState.value.speechRate, _uiState.value.activeCharacter)
        persistCurrentState()
    }

    fun setSpeechRate(rate: Float) {
        _uiState.value = _uiState.value.copy(speechRate = rate)
        ttsEngine.setVoiceProfile(_uiState.value.voicePitch, rate, _uiState.value.activeCharacter)
        persistCurrentState()
    }

    fun testVoice() {
        val char = _uiState.value.activeCharacter
        val testPhrase = when (_uiState.value.selectedLanguage) {
            AppLanguage.INDONESIAN -> "Halo, ini suara ${char.name} dengan gaya ${char.voicePersona.displayName}. Terdengar natural dan jernih, kan?"
            AppLanguage.ENGLISH -> "Hello! This is ${char.name}'s natural voice. It sounds clear and lifelike, right?"
            AppLanguage.JAPANESE -> "こんにちは！${char.name}の自然な声です。きれいに聞こえますか？"
            AppLanguage.SPANISH -> "¡Hola! Esta es la voz natural de ${char.name}. ¿Verdad que suena genial?"
        }
        updateSpeechAndAutoSpeak(testPhrase, AvatarEmotion.HAPPY)
    }

    fun clearHistory() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
        viewModelScope.launch {
            try {
                characterStateManager.clearChatHistory()
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error clearing Room chat history: ${e.message}", e)
            }
        }
    }

    fun getCustomApiKey(): String = aiService.getEffectiveApiKey()
    fun setCustomApiKey(key: String) {
        val trimmed = key.trim()
        aiService.setCustomApiKey(trimmed)
        ttsEngine.googleApiKey = getGoogleCloudTtsApiKey()
        if (trimmed.isNotBlank()) {
            testVoice()
        }
    }
    fun isGeminiConnected(): Boolean = aiService.isGeminiConnected()

    fun getVoiceEngineMode(): VoiceEngineMode {
        val defaultMode = if (getElevenLabsApiKey().isNotBlank()) {
            VoiceEngineMode.ELEVENLABS_NEURAL
        } else {
            VoiceEngineMode.GEMINI_NATURAL
        }
        val name = prefs.getString("voice_engine_mode_v3", defaultMode.name)
        return try {
            VoiceEngineMode.valueOf(name ?: defaultMode.name)
        } catch (e: Exception) {
            defaultMode
        }
    }

    fun setVoiceEngineMode(mode: VoiceEngineMode) {
        prefs.edit().putString("voice_engine_mode_v3", mode.name).apply()
        _uiState.value = _uiState.value.copy(voiceEngineMode = mode)
        ttsEngine.engineMode = mode
        persistCurrentState()
    }

    private fun isValidElevenLabsSecret(candidate: String): Boolean {
        val trimmed = candidate.trim()
        return trimmed.isNotBlank() &&
            !trimmed.startsWith("MY_") &&
            !trimmed.contains("DEFAULT_VALUE") &&
            trimmed != "YOUR_API_KEY" &&
            trimmed != "null"
    }

    fun getElevenLabsApiKey(): String {
        val custom = (prefs.getString("elevenlabs_api_key", "") ?: "").trim()
        if (isValidElevenLabsSecret(custom)) return custom

        val secretCandidates = listOf(
            BuildConfig.ELEVENLABS_API_KEY,
            BuildConfig.ELEVEN_LABS_API_KEY,
            BuildConfig.XI_API_KEY,
            BuildConfig.ELEVENLABS_KEY
        )
        for (secret in secretCandidates) {
            if (isValidElevenLabsSecret(secret)) {
                return secret.trim()
            }
        }

        // Fallback if the user placed an ElevenLabs sk_... key inside GEMINI_API_KEY in Secrets
        val geminiSecret = BuildConfig.GEMINI_API_KEY.trim()
        if (geminiSecret.startsWith("sk_") && isValidElevenLabsSecret(geminiSecret)) {
            return geminiSecret
        }

        return ""
    }

    fun setElevenLabsApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("elevenlabs_api_key", trimmed).apply()
        ttsEngine.elevenLabsApiKey = getElevenLabsApiKey()
        if (ttsEngine.elevenLabsApiKey.isNotBlank()) {
            setVoiceEngineMode(VoiceEngineMode.ELEVENLABS_NEURAL)
            testVoice()
        }
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
        if (trimmed.isNotBlank()) {
            testVoice()
        }
    }

    fun startFaceToFaceCall() {
        ttsEngine.stop()
        speechRecognitionHelper.stopListening()
        autoListenJob?.cancel()
        callTimerJob?.cancel()

        val char = _uiState.value.activeCharacter
        val openingLine = when (char.id) {
            "zayn" -> "Hah? Ngapain kamu video call ke aku? Bukan berarti aku seneng ya, mau ngomong apa cepetan?"
            "siti" -> "Halo sayangku manis, Siti senang sekali bisa tatap muka langsung sama kamu. Ada cerita apa hari ini sayang?"
            "mochi" -> "Hai bestie! Asik banget kita bisa teleponan tatap muka gini! Mau cerita apa nih ke Mochi?"
            "rizky" -> "Panggilan tatap muka terhubung! Sersan Rizky siap bertugas dan mendengarkan laporanmu. Silakan bicara!"
            "jamal" -> "Hoaam, video call segala bro, Jamal lagi enak rebahan nih. Yaudah mau ngobrolin apa santai aja."
            else -> "Heh! Nelpon-nelpon gua lu! Mau ngobrol apa nih sama beruang keren kayak Heru?"
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
        ttsEngine.speak(openingLine, _uiState.value.voicePitch, _uiState.value.speechRate, char)
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
            var spokenFirstSentencePrefix: String? = null
            try {
                val reply = withTimeoutOrNull(28_000L) {
                    aiService.generateReplyStream(
                        userMessage = cleanText,
                        character = state.activeCharacter,
                        language = state.selectedLanguage,
                        history = updatedMessages,
                        onPartialText = { partialText ->
                            _uiState.value = _uiState.value.copy(
                                liveAvatarSpeech = partialText,
                                lastAvatarSpeech = partialText,
                                faceToFaceCallState = FaceToFaceCallState.SPEAKING
                            )

                            if (!ttsEngine.hasCloudNeuralKey() && spokenFirstSentencePrefix == null && partialText.length >= 16) {
                                val clauses = ttsEngine.splitIntoNaturalClauses(partialText)
                                if (clauses.size >= 2 && clauses.first().length >= 10) {
                                    val firstSentence = clauses.first()
                                    spokenFirstSentencePrefix = firstSentence
                                    ttsEngine.speakStreamedSentence(
                                        sentenceText = firstSentence,
                                        pitch = _uiState.value.voicePitch,
                                        rate = _uiState.value.speechRate,
                                        character = state.activeCharacter,
                                        flushQueue = true
                                    )
                                }
                            }
                        }
                    )
                } ?: aiService.getFastFallbackReply(
                    userMessage = cleanText,
                    character = state.activeCharacter,
                    language = state.selectedLanguage,
                    history = updatedMessages
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
                persistCurrentState()

                val alreadySpoken = spokenFirstSentencePrefix
                if (alreadySpoken != null && reply.text.startsWith(alreadySpoken)) {
                    val remaining = reply.text.removePrefix(alreadySpoken).trim()
                    if (remaining.isNotBlank()) {
                        ttsEngine.speakStreamedSentence(
                            sentenceText = remaining,
                            pitch = _uiState.value.voicePitch,
                            rate = _uiState.value.speechRate,
                            character = state.activeCharacter,
                            flushQueue = false
                        )
                    }
                } else {
                    ttsEngine.speak(reply.text, _uiState.value.voicePitch, _uiState.value.speechRate, state.activeCharacter)
                }
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
                ttsEngine.speak(fallbackReply.text, _uiState.value.voicePitch, _uiState.value.speechRate, state.activeCharacter)
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
                ttsEngine.speak(fallbackReply.text, _uiState.value.voicePitch, _uiState.value.speechRate, state.activeCharacter)
            }
        }
    }

    fun quickPromptFaceToFace(promptText: String) {
        handleFaceToFaceUserUtterance(promptText)
    }

    fun switchCharacterInFaceToFace(character: PetCharacter) {
        persistCurrentState()
        ttsEngine.stop()
        speechRecognitionHelper.stopListening()
        autoListenJob?.cancel()

        viewModelScope.launch {
            val savedCharEntity = try {
                characterStateManager.getOrCreateCharacterCustomization(character)
            } catch (e: Exception) {
                null
            }
            val restoredCustomization = savedCharEntity?.toAvatarCustomization(character)
                ?: _uiState.value.customization.copy(
                    furColor = character.defaultFurColor,
                    earStyle = if (character.species == PetSpecies.DOG) com.example.model.EarStyle.DOG_FLOPPY else com.example.model.EarStyle.CAT_POINTY
                )
            val restoredPitch = savedCharEntity?.voicePitch ?: character.defaultVoicePitch
            val restoredRate = savedCharEntity?.speechRate ?: character.defaultSpeechRate

            ttsEngine.setVoiceProfile(restoredPitch, restoredRate, character)

            val greeting = when (character.id) {
                "zayn" -> "Hmph! Sekarang kamu ngobrol sama Zayn! Jangan mikir aneh-aneh ya!"
                "siti" -> "Halo sayang, Siti hadir di sini untukmu. Siti siap mendengarkan."
                "mochi" -> "Hai bestie! Sekarang giliran Mochi yang ngobrol bareng kamu nih!"
                "rizky" -> "Pergantian personel! Sersan Rizky mengambil alih komunikasi!"
                "jamal" -> "Yoo, sekarang giliran Jamal. Santai aja ya ngobrolnya."
                else -> "Sekarang gua Heru yang nemenin lu ngobrol! Mau bahas apa nih kita?"
            }

            _uiState.value = _uiState.value.copy(
                activeCharacter = character,
                voicePitch = restoredPitch,
                speechRate = restoredRate,
                liveAvatarSpeech = greeting,
                lastAvatarSpeech = greeting,
                faceToFaceCallState = FaceToFaceCallState.SPEAKING,
                customization = restoredCustomization
            )
            persistCurrentState()
            ttsEngine.speak(greeting, restoredPitch, restoredRate, character)
        }
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
        persistCurrentState()

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
            persistCurrentState()
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
            persistCurrentState()
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
        persistCurrentState()
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
        val lang = _uiState.value.selectedLanguage
        val photoBitmap = _uiState.value.homeworkPhotoBitmap

        if (q.isBlank() && photoBitmap == null) return

        _uiState.value = _uiState.value.copy(
            isSolvingHomework = true,
            lastAvatarSpeech = "Bentar ya, ${char.name} lagi ngitung dan bedah soalnya secara kilat..."
        )

        viewModelScope.launch {
            try {
                // 1. If a photo is attached, use Multimodal Vision (Firebase AI Logic)
                val solution = if (photoBitmap != null) {
                    studyCompanionHelper.analyzeHomeworkWithVision(
                        subject = subject,
                        questionText = q,
                        photoBitmap = photoBitmap,
                        character = char
                    ) ?: aiService.solveHomeworkDeterministically(subject, q, char)
                } else {
                    withTimeoutOrNull(35_000L) {
                        aiService.generateHomeworkHelp(subject, q, char, lang)
                    } ?: aiService.solveHomeworkDeterministically(subject, q, char)
                }

                val updatedCoins = _uiState.value.coins + 15
                prefs.edit().putInt("saved_petto_coins", updatedCoins).apply()

                _uiState.value = _uiState.value.copy(
                    isSolvingHomework = false,
                    homeworkSolution = solution,
                    coins = updatedCoins,
                    currentEmotion = AvatarEmotion.TALKING
                )
                persistCurrentState()
                val spokenSummary = "${char.name} sudah menemukan solusinya! ${solution.finalAnswer}. Simak langkah-langkahnya ya!"
                speakText(spokenSummary)
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error solving homework: ${e.message}", e)
                val fallbackSolution = aiService.solveHomeworkDeterministically(subject, q, char)
                _uiState.value = _uiState.value.copy(
                    isSolvingHomework = false,
                    homeworkSolution = fallbackSolution,
                    currentEmotion = AvatarEmotion.TALKING
                )
                speakText("${char.name} sudah siapkan pembahasan lengkapnya. Yuk disimak!")
            } finally {
                _uiState.value = _uiState.value.copy(isSolvingHomework = false)
            }
        }
    }

    // ========================================================
    // FEATURE #2: 🎮 MINI-GAMES & KUIS CERDAS BARENG PET
    // ========================================================

    fun openQuizBattle() {
        val char = _uiState.value.activeCharacter
        val line = when (char.id) {
            "zayn" -> "Hmph! Berani adu kuis cerdas sama aku?! Jangan nangis kalau salah ya, tapi kalau bener aku kasih +25 koin wkwk!"
            "siti" -> "Yeay! Ayo kita main kuis cerdas bareng sayangku! Setiap jawaban bener dapet +25 koin bintang lho, hihi."
            "rizky" -> "SIAP KOMANDAN! Simulasi Kuis Taktis dimulai! Jawab tepat sasaran untuk raih +25 koin per soal, gaspol!"
            "jamal" -> "Main tebak-tebakan santuy bareng Jamal yuk bro... Lumayan dapet koin buat jajan makanan enak wkwk."
            else -> "HEH! Ayo buktiin seberapa encer otak lu di kuis ini wkwk! Jawab bener gua kasih +25 koin!"
        }
        _uiState.value = _uiState.value.copy(
            showQuizSheet = true,
            currentEmotion = AvatarEmotion.HAPPY,
            lastAvatarSpeech = line
        )
        speakText(line)
    }

    fun closeQuizBattle() {
        _uiState.value = _uiState.value.copy(showQuizSheet = false)
    }

    fun answerQuizQuestion(question: QuizQuestion, selectedIndex: Int) {
        val char = _uiState.value.activeCharacter
        val isCorrect = selectedIndex == question.correctIndex
        val reaction = QuizCatalog.getReactionForAnswer(isCorrect, char, question.funFact)

        val newStreak = if (isCorrect) _uiState.value.quizStreak + 1 else 0
        val coinReward = if (isCorrect) 25 else 5
        val newCoins = _uiState.value.coins + coinReward
        val newAffection = (_uiState.value.affectionScore + if (isCorrect) 5 else 1).coerceAtMost(100)
        val newFun = (_uiState.value.funLevel + 0.12f).coerceAtMost(1.0f)

        prefs.edit().putInt("saved_petto_coins", newCoins).apply()

        _uiState.value = _uiState.value.copy(
            coins = newCoins,
            quizStreak = newStreak,
            affectionScore = newAffection,
            funLevel = newFun,
            currentEmotion = if (isCorrect) AvatarEmotion.HAPPY else AvatarEmotion.SURPRISED,
            lastAvatarSpeech = reaction
        )
        persistCurrentState()
        speakText(reaction)
    }

    // ========================================================
    // FEATURE #3: ⏱️ POMODORO STUDY TIMER (TEMAN FOKUS)
    // ========================================================

    fun openPomodoroTimer() {
        val char = _uiState.value.activeCharacter
        val line = when (char.id) {
            "zayn" -> "Hmph, mau fokus belajar pakai Pomodoro ya? Ya udah sini aku jagain timernya biar kamu nggak main HP terus!"
            "siti" -> "Wah, ayo kita fokus belajar bareng pakai timer Pomodoro sayangku... Siti temenin dengan tenang di sini ya."
            "rizky" -> "Siap Komandan! Timer Fokus Pomodoro siap diaktifkan! Disiplin waktu adalah kunci kesuksesan!"
            "jamal" -> "Belajar pake timer Pomodoro paling pas bro: fokus bentar, terus istirahat rebahan santuy bareng Jamal wkwk."
            else -> "Buka buku lu sekarang! Kita nyalain timer fokus Pomodoro biar PR lu cepet kelar dan dapet banyak koin!"
        }
        _uiState.value = _uiState.value.copy(
            showPomodoroSheet = true,
            currentEmotion = AvatarEmotion.HAPPY,
            lastAvatarSpeech = line
        )
        speakText(line)
    }

    fun closePomodoroTimer() {
        _uiState.value = _uiState.value.copy(showPomodoroSheet = false)
    }

    fun selectPomodoroPreset(preset: PomodoroPreset) {
        pomodoroTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            activePomodoroPreset = preset,
            pomodoroRemainingSeconds = preset.durationMinutes * 60,
            isPomodoroRunning = false
        )
    }

    fun togglePomodoroStartPause() {
        val currentlyRunning = _uiState.value.isPomodoroRunning
        if (currentlyRunning) {
            pomodoroTimerJob?.cancel()
            _uiState.value = _uiState.value.copy(isPomodoroRunning = false)
        } else {
            _uiState.value = _uiState.value.copy(isPomodoroRunning = true)
            pomodoroTimerJob?.cancel()
            pomodoroTimerJob = viewModelScope.launch {
                while (_uiState.value.isPomodoroRunning && _uiState.value.pomodoroRemainingSeconds > 0) {
                    delay(1000L)
                    val nextSec = (_uiState.value.pomodoroRemainingSeconds - 1).coerceAtLeast(0)
                    _uiState.value = _uiState.value.copy(pomodoroRemainingSeconds = nextSec)
                    if (nextSec == 0) {
                        completePomodoroAndClaimReward()
                        break
                    }
                }
            }
        }
    }

    fun resetPomodoroTimer() {
        pomodoroTimerJob?.cancel()
        val preset = _uiState.value.activePomodoroPreset
        _uiState.value = _uiState.value.copy(
            isPomodoroRunning = false,
            pomodoroRemainingSeconds = preset.durationMinutes * 60
        )
    }

    fun completePomodoroAndClaimReward() {
        pomodoroTimerJob?.cancel()
        val state = _uiState.value
        val preset = state.activePomodoroPreset
        val char = state.activeCharacter
        val newCoins = state.coins + preset.rewardCoins
        val newAffection = (state.affectionScore + preset.affectionReward).coerceAtMost(100)
        val newSessions = state.pomodoroCompletedSessions + 1
        val newProgress = state.levelProgress + 0.25f
        val newLevel = if (newProgress >= 1f) state.playerLevel + 1 else state.playerLevel
        val finalProgress = if (newProgress >= 1f) newProgress - 1f else newProgress

        prefs.edit().putInt("saved_petto_coins", newCoins).apply()

        val congrats = when (char.id) {
            "zayn" -> "Hmph! Sesi fokus ${preset.title} beres juga! Nih hadiah +${preset.rewardCoins} koin buat kamu. Keren banget sih kerja kerasmu, wkwk!"
            "siti" -> "Yeay alhamdulillah! Sesi ${preset.title} selesai sayangku! Kamu hebat banget, ini hadiah +${preset.rewardCoins} koin bintang buatmu manis!"
            "rizky" -> "MISI FOKUS TUNTAS KOMANDAN! Sesi ${preset.title} berhasil diselesaikan! Bonus +${preset.rewardCoins} koin telah ditambahkan!"
            "jamal" -> "Mantap jiwa bro! Sesi ${preset.title} kelar tanpa stres, dapet +${preset.rewardCoins} koin pula! Saatnya kita rebahan santuy wkwk."
            else -> "WUIH KELAR JUGA SESI FOKUS LU! Nih gua kasih +${preset.rewardCoins} koin karena lu udah rajin banget hari ini, gaspol!"
        }

        _uiState.value = state.copy(
            isPomodoroRunning = false,
            pomodoroRemainingSeconds = preset.durationMinutes * 60,
            pomodoroCompletedSessions = newSessions,
            coins = newCoins,
            affectionScore = newAffection,
            playerLevel = newLevel,
            levelProgress = finalProgress,
            currentEmotion = AvatarEmotion.HAPPY,
            lastAvatarSpeech = congrats
        )
        persistCurrentState()
        speakText(congrats)
    }

    // ========================================================
    // FEATURE #4: 🍖 SISTEM RAWAT PET, MAKANAN FAVORIT & TOKO
    // ========================================================

    fun openPetCareShop() {
        val char = _uiState.value.activeCharacter
        val line = when (char.id) {
            "zayn" -> "Hmph, mau traktir aku makanan enak ya? K-kalau ada Salmon Sashimi sih aku nggak bakal nolak wkwk!"
            "siti" -> "Wah, ayo kita lihat makanan lezat dan aksesoris gemoy di toko sayangku... Siti suka banget Wortel Madu!"
            "rizky" -> "Siap Komandan! Memasuki ruang logistik makanan dan perlengkapan taktis!"
            "jamal" -> "Asyik makan-makan nih bro... Kalau ada Semangka Es Santuy sabi banget tuh buat temen rebahan wkwk."
            else -> "HAH mau nraktir gua makan?! Sini beliin gua Madu Hutan Premium biar tenaga gua makin jos!"
        }
        _uiState.value = _uiState.value.copy(
            showPetCareShopSheet = true,
            currentEmotion = AvatarEmotion.HAPPY,
            lastAvatarSpeech = line
        )
        speakText(line)
    }

    fun closePetCareShop() {
        _uiState.value = _uiState.value.copy(showPetCareShopSheet = false)
    }

    fun feedSpecialFood(food: SpecialPetFood) {
        val state = _uiState.value
        if (state.coins < food.priceCoins) return
        val char = state.activeCharacter
        val isFav = food.favoriteCharacterId == char.id
        val newCoins = (state.coins - food.priceCoins).coerceAtLeast(0)
        val newHunger = (state.hungerLevel + food.hungerRestore).coerceAtMost(1.0f)
        val affectionAdd = if (isFav) food.affectionBonus + 5 else food.affectionBonus
        val newAffection = (state.affectionScore + affectionAdd).coerceAtMost(100)
        val newProgress = state.levelProgress + 0.15f
        val newLevel = if (newProgress >= 1f) state.playerLevel + 1 else state.playerLevel
        val finalProgress = if (newProgress >= 1f) newProgress - 1f else newProgress

        prefs.edit().putInt("saved_petto_coins", newCoins).apply()

        val reaction = SpecialPetFoodCatalog.getReactionForFood(food, char)
        _uiState.value = state.copy(
            coins = newCoins,
            hungerLevel = newHunger,
            affectionScore = newAffection,
            playerLevel = newLevel,
            levelProgress = finalProgress,
            currentEmotion = AvatarEmotion.LOVING,
            lastAvatarSpeech = reaction,
            careFeedbackMessage = "${food.emoji} +${(food.hungerRestore * 100).toInt()}% Kenyang & +$affectionAdd% Sayang!"
        )
        persistCurrentState()
        speakText(reaction)
    }

    fun equipAccessoryWithCoins(accessory: AccessoryStyle, cost: Int) {
        val state = _uiState.value
        if (state.coins < cost) return
        val newCoins = (state.coins - cost).coerceAtLeast(0)
        prefs.edit().putInt("saved_petto_coins", newCoins).apply()

        val char = state.activeCharacter
        val line = "Wah, ${accessory.displayName} sudah terpasang! Gimana penampilan ${char.name}, makin keren dan gemoy kan wkwk!"
        _uiState.value = state.copy(
            coins = newCoins,
            customization = state.customization.copy(accessory = accessory),
            currentEmotion = AvatarEmotion.HAPPY,
            lastAvatarSpeech = line,
            careFeedbackMessage = "✨ ${accessory.displayName} Terpasang!"
        )
        persistCurrentState()
        speakText(line)
    }

    fun equipCostumeWithCoins(costume: CostumeStyle, cost: Int) {
        val state = _uiState.value
        if (state.coins < cost) return
        val newCoins = (state.coins - cost).coerceAtLeast(0)
        prefs.edit().putInt("saved_petto_coins", newCoins).apply()

        val char = state.activeCharacter
        val line = "Yeay! Kostum ${costume.displayName} sudah dipakai sama ${char.name}! Auto tampil kece badai nih!"
        _uiState.value = state.copy(
            coins = newCoins,
            customization = state.customization.copy(costume = costume),
            currentEmotion = AvatarEmotion.HAPPY,
            lastAvatarSpeech = line,
            careFeedbackMessage = "🧥 ${costume.displayName} Terpasang!"
        )
        persistCurrentState()
        speakText(line)
    }

    // ========================================================
    // FEATURE #5: 📓 JURNAL MOOD HARIAN & CURHAT TRACKER
    // ========================================================

    fun openMoodJournal() {
        val char = _uiState.value.activeCharacter
        val line = when (char.id) {
            "zayn" -> "Hmph, gimana mood kamu hari ini? Tulis aja di jurnal, aku dengerin kok semua curhatan kamu... b-bukan berarti aku kepo ya!"
            "siti" -> "Halo sayangku, gimana perasaanmu hari ini? Tulis di jurnal mood ya, Siti siap dengerin curhatanmu dengan penuh kasih sayang."
            "rizky" -> "Siap Komandan! Silakan laporkan kondisi mood dan cerita harianmu di Jurnal Mood, Rizky siap mendengarkan!"
            "jamal" -> "Lagi seneng, santuy, atau lagi capek nih bro? Spill aja di jurnal mood, Jamal siap jadi temen curhat paling santuy wkwk."
            else -> "HEH bestie! Gimana hari lu? Lagi happy atau ada yang bikin kesel? Sini spill di jurnal mood ke gua!"
        }
        _uiState.value = _uiState.value.copy(
            showMoodJournalSheet = true,
            currentEmotion = AvatarEmotion.HAPPY,
            lastAvatarSpeech = line
        )
        speakText(line)
    }

    fun closeMoodJournal() {
        _uiState.value = _uiState.value.copy(showMoodJournalSheet = false)
    }

    fun submitMoodJournalEntry(mood: MoodType, userNote: String) {
        val state = _uiState.value
        val char = state.activeCharacter
        _uiState.value = state.copy(isSavingMoodEntry = true)

        viewModelScope.launch {
            try {
                val petResponse = studyCompanionHelper.generateMoodCurhatResponse(
                    mood = mood,
                    userNote = userNote,
                    character = char
                )
                val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                val entry = MoodJournalEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = System.currentTimeMillis(),
                    formattedDate = sdf.format(Date()),
                    mood = mood,
                    userNote = userNote.trim().ifBlank { "Mood hari ini: ${mood.label}" },
                    characterName = char.name,
                    characterEmoji = char.species.emoji,
                    petResponse = petResponse
                )

                val updatedEntries = listOf(entry) + _uiState.value.moodJournalEntries
                val newCoins = _uiState.value.coins + 20
                val newAffection = (_uiState.value.affectionScore + 6).coerceAtMost(100)

                saveMoodEntriesToPrefs(updatedEntries)
                prefs.edit().putInt("saved_petto_coins", newCoins).apply()

                _uiState.value = _uiState.value.copy(
                    isSavingMoodEntry = false,
                    moodJournalEntries = updatedEntries,
                    coins = newCoins,
                    affectionScore = newAffection,
                    currentEmotion = AvatarEmotion.LOVING,
                    lastAvatarSpeech = petResponse
                )
                persistCurrentState()
                speakText(petResponse)
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error saving mood entry: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isSavingMoodEntry = false)
            }
        }
    }

    private fun saveMoodEntriesToPrefs(entries: List<MoodJournalEntry>) {
        try {
            val arr = JSONArray()
            entries.take(30).forEach { item ->
                arr.put(JSONObject().apply {
                    put("id", item.id)
                    put("timestampMillis", item.timestampMillis)
                    put("formattedDate", item.formattedDate)
                    put("moodId", item.mood.id)
                    put("userNote", item.userNote)
                    put("characterName", item.characterName)
                    put("characterEmoji", item.characterEmoji)
                    put("petResponse", item.petResponse)
                })
            }
            prefs.edit().putString("saved_mood_journal_entries", arr.toString()).apply()
        } catch (e: Exception) {
            Log.d("AvatarViewModel", "Failed to save mood entries: ${e.message}")
        }
    }

    private fun loadMoodEntriesFromPrefs(): List<MoodJournalEntry> {
        return try {
            val raw = prefs.getString("saved_mood_journal_entries", null) ?: return emptyList()
            val arr = JSONArray(raw)
            val list = mutableListOf<MoodJournalEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val moodId = obj.optString("moodId", "senang")
                val mood = MoodType.values().firstOrNull { it.id == moodId } ?: MoodType.SENANG
                list.add(
                    MoodJournalEntry(
                        id = obj.optString("id"),
                        timestampMillis = obj.optLong("timestampMillis"),
                        formattedDate = obj.optString("formattedDate"),
                        mood = mood,
                        userNote = obj.optString("userNote"),
                        characterName = obj.optString("characterName", "Petto"),
                        characterEmoji = obj.optString("characterEmoji", "🐾"),
                        petResponse = obj.optString("petResponse")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ========================================================
    // FEATURE #6: 🃏 FLASHCARD OTOMATIS DARI MATERI SEKOLAH
    // ========================================================

    fun openFlashcardStudy(customTopic: String? = null) {
        val state = _uiState.value
        val char = state.activeCharacter
        val subj = state.homeworkSubject
        val initialCards = FlashcardCatalog.getDefaultDeck(subj)

        _uiState.value = state.copy(
            showFlashcardSheet = true,
            showHomeworkMode = false,
            flashcards = if (state.flashcards.isEmpty()) initialCards else state.flashcards,
            currentEmotion = AvatarEmotion.HAPPY
        )

        if (!customTopic.isNullOrBlank()) {
            generateFlashcards(customTopic)
        } else {
            val line = "Ayo kita hafalin materi $subj pakai kartu Flashcard pintar bareng ${char.name}! Ketuk kartunya buat membalik jawaban ya!"
            _uiState.value = _uiState.value.copy(lastAvatarSpeech = line)
            speakText(line)
        }
    }

    fun closeFlashcardStudy() {
        _uiState.value = _uiState.value.copy(showFlashcardSheet = false)
    }

    fun selectFlashcardSubject(subject: String) {
        _uiState.value = _uiState.value.copy(
            homeworkSubject = subject,
            flashcards = FlashcardCatalog.getDefaultDeck(subject)
        )
    }

    fun generateFlashcards(customTopic: String) {
        val state = _uiState.value
        val subject = state.homeworkSubject
        val char = state.activeCharacter

        _uiState.value = state.copy(isGeneratingFlashcards = true)

        viewModelScope.launch {
            try {
                val generated = studyCompanionHelper.generateFlashcardsForTopic(
                    subject = subject,
                    customTopic = customTopic,
                    character = char
                )
                val line = "${char.name} udah buatin ${generated.size} kartu Flashcard tentang ${customTopic.ifBlank { subject }}! Yuk kita hafalin bareng!"
                _uiState.value = _uiState.value.copy(
                    isGeneratingFlashcards = false,
                    flashcards = generated,
                    lastAvatarSpeech = line,
                    currentEmotion = AvatarEmotion.HAPPY
                )
                speakText(line)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingFlashcards = false,
                    flashcards = FlashcardCatalog.getDefaultDeck(subject)
                )
            }
        }
    }

    fun markFlashcardMastered(cardId: String) {
        val state = _uiState.value
        var newlyMastered = false
        val updated = state.flashcards.map { card ->
            if (card.id == cardId) {
                if (!card.isMastered) newlyMastered = true
                card.copy(isMastered = true)
            } else {
                card
            }
        }
        val newCoins = if (newlyMastered) state.coins + 10 else state.coins
        if (newlyMastered) {
            prefs.edit().putInt("saved_petto_coins", newCoins).apply()
        }
        _uiState.value = state.copy(
            flashcards = updated,
            coins = newCoins,
            affectionScore = (state.affectionScore + if (newlyMastered) 2 else 0).coerceAtMost(100)
        )
        persistCurrentState()
    }

    override fun onCleared() {
        super.onCleared()
        pomodoroTimerJob?.cancel()
        ttsEngine.release()
        speechRecognitionHelper.destroy()
    }
}
