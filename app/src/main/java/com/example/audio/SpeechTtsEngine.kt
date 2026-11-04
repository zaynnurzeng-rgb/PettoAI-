package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.model.AppLanguage
import com.example.model.CharacterCatalog
import com.example.model.CharacterGender
import com.example.model.PetCharacter
import com.example.model.PetSpecies
import com.example.model.TouchZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class SpeechTtsEngine(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private val TAG = "SpeechTtsEngine"
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentLanguage = AppLanguage.INDONESIAN
    private var currentCharacter: PetCharacter = CharacterCatalog.characters.first()
    private var currentPitch: Float = currentCharacter.defaultVoicePitch
    private var currentRate: Float = currentCharacter.defaultSpeechRate
    private val lastQueuedUtteranceId = AtomicReference<String?>(null)

    // Neural TTS Client (Gemini Native TTS, Google Cloud Neural2, ElevenLabs)
    private val neuralClient = NeuralTtsClient(context, onSpeakingStateChanged)

    // Default to natural Gemini-like voice instead of squeaky cartoon pitch
    var engineMode: VoiceEngineMode = VoiceEngineMode.GEMINI_NATURAL
    var googleApiKey: String = ""
    var elevenLabsApiKey: String = ""

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            applyLanguage(currentLanguage)
            selectBestVoiceForCharacter(currentLanguage, currentCharacter)
            tts?.setPitch(currentPitch)
            tts?.setSpeechRate(currentRate)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    val expectedLast = lastQueuedUtteranceId.get()
                    if (expectedLast == null || utteranceId == expectedLast) {
                        onSpeakingStateChanged(false)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    onSpeakingStateChanged(false)
                }
            })
            Log.d(TAG, "Natural Gemini-style TTS initialized successfully")
        } else {
            Log.w(TAG, "TTS init returned status $status")
        }
    }

    /**
     * Selects a distinct, gender-appropriate neural/high-quality Android Voice for the given character.
     * Separates Male and Female voices in the TTS engine and assigns different voice variants per character.
     */
    private fun selectBestVoiceForCharacter(language: AppLanguage, character: PetCharacter) {
        try {
            val voices = tts?.voices ?: return
            val targetLangCodes = when (language) {
                // Indonesian can be reported as "id" or legacy ISO "in" on Android
                AppLanguage.INDONESIAN -> setOf("id", "in", "ind")
                AppLanguage.ENGLISH -> setOf("en", "eng")
                AppLanguage.JAPANESE -> setOf("ja", "jpn")
                AppLanguage.SPANISH -> setOf("es", "spa")
            }

            val matchedVoices = voices
                .filter {
                    it.locale.language.lowercase() in targetLangCodes &&
                        !it.features.contains("notInstalled")
                }
                .ifEmpty {
                    voices.filter { it.locale.language.lowercase() in targetLangCodes }
                }
                .sortedWith(
                    compareByDescending<Voice> { it.quality }
                        .thenBy { it.latency }
                        .thenBy { it.isNetworkConnectionRequired }
                        .thenBy { it.name }
                )

            if (matchedVoices.isEmpty()) return

            val femaleTokens = listOf(
                "female", "#female", "ida", "idd", "dfz", "wavenet-a", "wavenet-d",
                "neural2-a", "neural2-d", "journey-f", "journey-o", "sfg", "tpf", "iob", "iol", "kda", "htm", "vla", "vlc"
            )
            val maleTokens = listOf(
                "male", "#male", "idb", "idc", "ide", "wavenet-b", "wavenet-c",
                "neural2-b", "neural2-c", "journey-d", "iom", "iog", "kdb", "htd", "vlb", "vld"
            )

            val femaleVoices = matchedVoices.filter { voice ->
                val signature = (voice.name + " " + voice.features.joinToString(" ")).lowercase()
                femaleTokens.any { token -> signature.contains(token) } &&
                    !signature.contains("#male")
            }

            val maleVoices = matchedVoices.filter { voice ->
                val signature = (voice.name + " " + voice.features.joinToString(" ")).lowercase()
                maleTokens.any { token -> signature.contains(token) } &&
                    !signature.contains("female")
            }

            val targetPool = when (character.gender) {
                CharacterGender.FEMALE -> femaleVoices.ifEmpty { matchedVoices }
                CharacterGender.MALE -> maleVoices.ifEmpty { matchedVoices.reversed() }
            }

            val variantIdx = character.voicePersona.voiceVariantIndex.coerceAtLeast(0)
            val chosenVoice = targetPool[variantIdx % targetPool.size]

            tts?.voice = chosenVoice
            Log.d(
                TAG,
                "Selected natural voice for ${character.name} (${character.gender}): ${chosenVoice.name} (quality=${chosenVoice.quality})"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Voice selection error: ${e.message}")
        }
    }

    fun setVoiceProfile(pitch: Float, rate: Float, character: PetCharacter? = null) {
        if (character != null) {
            currentCharacter = character
        }
        currentPitch = pitch
        currentRate = rate
        if (isInitialized) {
            selectBestVoiceForCharacter(currentLanguage, currentCharacter)
            tts?.setPitch(pitch)
            tts?.setSpeechRate(rate)
        }
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        if (isInitialized) {
            applyLanguage(language)
            selectBestVoiceForCharacter(language, currentCharacter)
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        val locale = when (language) {
            AppLanguage.INDONESIAN -> Locale.forLanguageTag("id-ID")
            AppLanguage.ENGLISH -> Locale.US
            AppLanguage.JAPANESE -> Locale.JAPANESE
            AppLanguage.SPANISH -> Locale.forLanguageTag("es-ES")
        }
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language $locale not fully supported, trying fallback")
            if (language == AppLanguage.INDONESIAN) {
                tts?.setLanguage(Locale.forLanguageTag("in-ID"))
            } else {
                tts?.setLanguage(Locale.getDefault())
            }
        }
    }

    /**
     * Speaks the text using the selected Voice Engine, ensuring distinct Male/Female character voice
     * and natural Gemini AI prosody.
     */
    fun speak(
        text: String,
        pitch: Float = currentPitch,
        rate: Float = currentRate,
        character: PetCharacter? = null
    ) {
        stop()

        val activeChar = character ?: currentCharacter
        currentCharacter = activeChar

        val cleanText = sanitizeTextForNaturalSpeech(text)
        if (cleanText.isBlank()) return

        // 1. ElevenLabs Neural Voice if selected & key provided
        if (engineMode == VoiceEngineMode.ELEVENLABS_NEURAL && elevenLabsApiKey.isNotBlank()) {
            CoroutineScope(Dispatchers.Main).launch {
                val success = neuralClient.speakWithElevenLabs(cleanText, elevenLabsApiKey, activeChar)
                if (!success) {
                    speakNaturalAndroidVoice(cleanText, pitch, rate, activeChar, flushFirst = true)
                }
            }
            return
        }

        // 2. Google Cloud Neural2 / WaveNet if selected & key provided
        if (engineMode == VoiceEngineMode.GOOGLE_CLOUD_NEURAL && googleApiKey.isNotBlank()) {
            CoroutineScope(Dispatchers.Main).launch {
                val success = neuralClient.speakWithGoogleCloud(
                    text = cleanText,
                    apiKey = googleApiKey,
                    character = activeChar,
                    language = currentLanguage,
                    pitch = pitch,
                    rate = rate
                )
                if (!success) {
                    speakNaturalAndroidVoice(cleanText, pitch, rate, activeChar, flushFirst = true)
                }
            }
            return
        }

        // 3. Gemini Natural Voice (or automatic ElevenLabs when ElevenLabs secret is configured)
        if (engineMode == VoiceEngineMode.GEMINI_NATURAL) {
            if (elevenLabsApiKey.isNotBlank() || googleApiKey.isNotBlank()) {
                CoroutineScope(Dispatchers.Main).launch {
                    val elevenSuccess = if (elevenLabsApiKey.isNotBlank()) {
                        neuralClient.speakWithElevenLabs(cleanText, elevenLabsApiKey, activeChar)
                    } else {
                        false
                    }
                    val geminiSuccess = elevenSuccess || (googleApiKey.isNotBlank() && (
                        neuralClient.speakWithGeminiNativeTts(
                            text = cleanText,
                            apiKey = googleApiKey,
                            character = activeChar
                        ) || neuralClient.speakWithGoogleCloud(
                            text = cleanText,
                            apiKey = googleApiKey,
                            character = activeChar,
                            language = currentLanguage,
                            pitch = pitch,
                            rate = rate
                        )
                    ))
                    if (!geminiSuccess) {
                        speakNaturalAndroidVoice(cleanText, pitch, rate, activeChar, flushFirst = true)
                    }
                }
                return
            }
            speakNaturalAndroidVoice(cleanText, pitch, rate, activeChar, flushFirst = true)
            return
        }

        // 4. Cartoon Mimic Mode (Talking Tom style high pitch)
        speakCartoonMimic(cleanText, pitch, rate, activeChar)
    }

    /**
     * Returns true when a cloud neural or ElevenLabs / Gemini Native TTS API key is configured for the active mode.
     */
    fun hasCloudNeuralKey(): Boolean {
        return when (engineMode) {
            VoiceEngineMode.ELEVENLABS_NEURAL -> elevenLabsApiKey.isNotBlank()
            VoiceEngineMode.GEMINI_NATURAL -> elevenLabsApiKey.isNotBlank() || googleApiKey.isNotBlank()
            VoiceEngineMode.GOOGLE_CLOUD_NEURAL -> googleApiKey.isNotBlank()
            VoiceEngineMode.CARTOON_MIMIC -> false
        }
    }

    /**
     * Speaks a newly streamed sentence immediately without waiting for the entire AI paragraph to finish.
     * When [flushQueue] is true (first sentence), it starts speaking immediately; subsequent sentences
     * are appended via QUEUE_ADD for zero-gap natural prosody.
     */
    fun speakStreamedSentence(
        sentenceText: String,
        pitch: Float = currentPitch,
        rate: Float = currentRate,
        character: PetCharacter? = null,
        flushQueue: Boolean = false
    ) {
        val activeChar = character ?: currentCharacter
        currentCharacter = activeChar

        val cleanText = sanitizeTextForNaturalSpeech(sentenceText)
        if (cleanText.isBlank()) return

        if (flushQueue) {
            stop()
        }

        if (engineMode == VoiceEngineMode.CARTOON_MIMIC) {
            speakCartoonMimic(cleanText, pitch, rate, activeChar, flushQueue = flushQueue)
        } else {
            speakNaturalAndroidVoice(cleanText, pitch, rate, activeChar, flushFirst = flushQueue)
        }
    }

    /**
     * Natural Gemini Live-style speech with gender-accurate vocal timbre,
     * character-specific voice selection, and dynamic sentence-by-sentence intonation.
     */
    private fun speakNaturalAndroidVoice(
        cleanText: String,
        pitch: Float,
        rate: Float,
        character: PetCharacter,
        flushFirst: Boolean = true
    ) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not yet initialized, speech skipped")
            return
        }

        selectBestVoiceForCharacter(currentLanguage, character)

        // Calibrate natural human vocal register based on gender & character persona
        // (No cartoon chipmunk multiplier!)
        val basePitch = when (character.gender) {
            CharacterGender.FEMALE -> pitch.coerceIn(1.04f, 1.32f)
            CharacterGender.MALE -> pitch.coerceIn(0.64f, 0.96f)
        }
        val baseRate = rate.coerceIn(0.88f, 1.24f)

        val clauses = splitIntoNaturalClauses(cleanText)
        if (clauses.isEmpty()) return

        clauses.forEachIndexed { index, clause ->
            val utteranceId = UUID.randomUUID().toString()
            if (index == clauses.lastIndex) {
                lastQueuedUtteranceId.set(utteranceId)
            }

            // Dynamic micro-prosody per sentence so speech feels conversational like Gemini Live
            val isQuestion = clause.endsWith("?")
            val isExclamation = clause.endsWith("!")
            val isSoftPause = clause.contains("...")

            val sentencePitch = when {
                isQuestion -> (basePitch + 0.04f).coerceIn(0.60f, 1.40f)
                isExclamation -> (basePitch + 0.02f).coerceIn(0.60f, 1.40f)
                isSoftPause -> (basePitch - 0.02f).coerceIn(0.60f, 1.40f)
                else -> basePitch
            }

            val sentenceRate = when {
                isExclamation -> (baseRate + 0.03f).coerceIn(0.82f, 1.32f)
                isSoftPause -> (baseRate - 0.03f).coerceIn(0.82f, 1.32f)
                else -> baseRate
            }

            tts?.setPitch(sentencePitch)
            tts?.setSpeechRate(sentenceRate)

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            val queueMode = if (index == 0 && flushFirst) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(clause, queueMode, params, utteranceId)
        }
    }

    /**
     * Optional Talking-Tom style cartoon voice when CARTOON_MIMIC mode is explicitly selected.
     */
    private fun speakCartoonMimic(
        cleanText: String,
        pitch: Float,
        rate: Float,
        character: PetCharacter,
        flushQueue: Boolean = true
    ) {
        if (!isInitialized) return
        selectBestVoiceForCharacter(currentLanguage, character)

        val effectivePitch = (pitch * 1.15f).coerceIn(0.65f, 1.95f)
        val effectiveRate = (rate * 1.04f).coerceIn(0.82f, 1.45f)

        tts?.setPitch(effectivePitch)
        tts?.setSpeechRate(effectiveRate)

        val utteranceId = UUID.randomUUID().toString()
        lastQueuedUtteranceId.set(utteranceId)
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        val mode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(cleanText, mode, params, utteranceId)
    }

    /**
     * Cleans text and normalizes Indonesian conversational slang/abbreviations
     * so TTS speaks like a warm, natural human rather than spelling out abbreviations or emojis.
     */
    fun sanitizeTextForNaturalSpeech(rawText: String): String {
        return rawText
            // Remove markdown bold/italic/code/bullet symbols
            .replace(Regex("[*_~`#•]+"), "")
            // Convert comic stutters like "B-bukan" or "H-hah" into smooth spoken words
            .replace(Regex("\\b([A-Za-z])-([a-z]+)"), "$2")
            // Normalize drawn-out yawns/laughs & casual slang into natural spoken Indonesian
            .replace(Regex("(?i)\\bhoaa+m+\\b"), "Hoaam")
            .replace(Regex("(?i)\\bwkwk+w?k?\\b"), "haha")
            .replace(Regex("(?i)\\bhehe+\\b"), "hehe")
            .replace(Regex("(?i)\\bhihi+\\b"), "hihi")
            .replace(Regex("(?i)\\bnggak\\b"), "enggak")
            .replace(Regex("(?i)\\bgak\\b"), "enggak")
            .replace(Regex("(?i)\\bbgt\\b"), "banget")
            .replace(Regex("(?i)\\byg\\b"), "yang")
            .replace(Regex("(?i)\\bdgn\\b"), "dengan")
            .replace(Regex("(?i)\\butk\\b"), "untuk")
            .replace(Regex("(?i)\\bbtw\\b"), "omong-omong")
            // Normalize math & scientific symbols for natural speech
            .replace("×", " dikali ")
            .replace("÷", " dibagi ")
            .replace("²", " kuadrat ")
            .replace("√", " akar ")
            .replace("π", " pi ")
            .replace("CO₂", "karbondioksida")
            .replace("H₂O", "air")
            .replace("O₂", "oksigen")
            // Strip emojis and pictographs so TTS does not read emoji descriptions aloud
            .replace(
                Regex("[\\p{So}\\p{Cn}\\u2600-\\u27BF\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"),
                " "
            )
            // Normalize ellipses and punctuation spacing for natural breathing pauses
            .replace(Regex("\\.{2,}"), "... ")
            .replace(Regex(",\\s*"), ", ")
            .replace(Regex("!+"), "! ")
            .replace(Regex("\\?+"), "? ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Splits text into natural conversational sentences for dynamic intonation per sentence.
     */
    fun splitIntoNaturalClauses(cleanText: String): List<String> {
        val rawClauses = cleanText.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return if (rawClauses.isEmpty()) listOf(cleanText) else rawClauses
    }

    fun stop() {
        lastQueuedUtteranceId.set(null)
        neuralClient.stop()
        if (isInitialized) {
            tts?.stop()
            onSpeakingStateChanged(false)
        }
    }

    fun playTouchSound(species: PetSpecies, zone: TouchZone) {
        ExpressiveAudioSynthesizer.playTouchReactionSound(species, zone)
    }

    fun playHappyChime() {
        ExpressiveAudioSynthesizer.playHeadPetChime()
    }

    fun playPurrEffect() {
        ExpressiveAudioSynthesizer.playTouchReactionSound(PetSpecies.CAT, TouchZone.HEAD)
    }

    fun release() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing TTS", e)
        }
    }
}
