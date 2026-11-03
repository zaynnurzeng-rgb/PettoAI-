package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.model.AppLanguage
import com.example.model.PetCharacter
import com.example.model.PetSpecies
import com.example.model.TouchZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class SpeechTtsEngine(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private val TAG = "SpeechTtsEngine"
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentLanguage = AppLanguage.INDONESIAN
    private var currentPitch: Float = 1.35f
    private var currentRate: Float = 1.15f

    // Neural TTS Client
    private val neuralClient = NeuralTtsClient(context, onSpeakingStateChanged)

    // Voice Engine Mode & API Keys
    var engineMode: VoiceEngineMode = VoiceEngineMode.CARTOON_MIMIC
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
            selectBestExpressiveVoice(currentLanguage)
            tts?.setPitch(currentPitch)
            tts?.setSpeechRate(currentRate)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    onSpeakingStateChanged(false)
                }
            })
            Log.d(TAG, "Expressive TTS initialized successfully")
        } else {
            Log.w(TAG, "TTS init returned status $status")
        }
    }

    /**
     * Finds and selects the highest-quality, expressive voice available in the engine,
     * avoiding flat robotic default voices.
     */
    private fun selectBestExpressiveVoice(language: AppLanguage) {
        try {
            val voices = tts?.voices ?: return
            val targetLocale = when (language) {
                AppLanguage.INDONESIAN -> Locale.forLanguageTag("id-ID")
                AppLanguage.ENGLISH -> Locale.US
                AppLanguage.JAPANESE -> Locale.JAPANESE
                AppLanguage.SPANISH -> Locale.forLanguageTag("es-ES")
            }

            // Prefer high-quality voices matching the language
            val matchedVoices = voices.filter { it.locale.language == targetLocale.language }
            val bestVoice = matchedVoices.firstOrNull { voice ->
                voice.quality >= Voice.QUALITY_HIGH && !voice.isNetworkConnectionRequired
            } ?: matchedVoices.firstOrNull { voice ->
                voice.quality >= Voice.QUALITY_NORMAL
            } ?: matchedVoices.firstOrNull()

            if (bestVoice != null) {
                tts?.voice = bestVoice
                Log.d(TAG, "Selected expressive voice: ${bestVoice.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Voice selection error: ${e.message}")
        }
    }

    fun setVoiceProfile(pitch: Float, rate: Float) {
        currentPitch = pitch
        currentRate = rate
        if (isInitialized) {
            tts?.setPitch(pitch)
            tts?.setSpeechRate(rate)
        }
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        if (isInitialized) {
            applyLanguage(language)
            selectBestExpressiveVoice(language)
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
            Log.w(TAG, "Language $locale not fully supported, falling back to default")
            tts?.setLanguage(Locale.getDefault())
        }
    }

    /**
     * Speaks the text using either Cloud Neural TTS or Expressive Cartoon Mimic Voice.
     */
    fun speak(
        text: String,
        pitch: Float = currentPitch,
        rate: Float = currentRate,
        character: PetCharacter? = null
    ) {
        stop()

        // 1. Try ElevenLabs Neural if selected
        if (engineMode == VoiceEngineMode.ELEVENLABS_NEURAL && elevenLabsApiKey.isNotBlank() && character != null) {
            CoroutineScope(Dispatchers.Main).launch {
                val success = neuralClient.speakWithElevenLabs(text, elevenLabsApiKey, character)
                if (!success) {
                    Log.d(TAG, "ElevenLabs failed, falling back to Cartoon Mimic Voice")
                    speakCartoonMimic(text, pitch, rate)
                }
            }
            return
        }

        // 2. Try Google Cloud Neural2 if selected
        if (engineMode == VoiceEngineMode.GOOGLE_CLOUD_NEURAL && googleApiKey.isNotBlank() && character != null) {
            CoroutineScope(Dispatchers.Main).launch {
                val success = neuralClient.speakWithGoogleCloud(
                    text = text,
                    apiKey = googleApiKey,
                    character = character,
                    language = currentLanguage,
                    pitch = pitch,
                    rate = rate
                )
                if (!success) {
                    Log.d(TAG, "Google Cloud TTS failed, falling back to Cartoon Mimic Voice")
                    speakCartoonMimic(text, pitch, rate)
                }
            }
            return
        }

        // 3. Expressive Cartoon Mimic Voice (Talking Tom pitch & speed shift)
        speakCartoonMimic(text, pitch, rate)
    }

    /**
     * Expressive Talking Tom style speech with pitch, tempo, and natural breathing pauses.
     */
    private fun speakCartoonMimic(text: String, pitch: Float, rate: Float) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not yet initialized, speech skipped")
            return
        }

        // Apply relaxed cartoon pitch boost and expressive natural pacing:
        // Ensures cute animated cartoon animal voice with clear breathing pauses
        val effectivePitch = (pitch * 1.10f).coerceIn(0.6f, 2.0f)
        val effectiveRate = (rate * 1.02f).coerceIn(0.8f, 1.45f)

        tts?.setPitch(effectivePitch)
        tts?.setSpeechRate(effectiveRate)

        // Ensure commas and ellipses have proper spacing for Android TTS pause cadence
        val formattedSpeech = formatTextForBreathingPauses(text)

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.speak(formattedSpeech, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    /**
     * Formats speech text with explicit pause tokens so Android TTS
     * naturally pauses and takes a simulated breath on commas and ellipses.
     */
    private fun formatTextForBreathingPauses(rawText: String): String {
        return rawText
            // Normalize ellipses to have spacing so TTS engine triggers a pause
            .replace(Regex("\\.{2,}"), "... ")
            // Normalize commas to have trailing space
            .replace(Regex(",\\s*"), ", ")
            // Reduce multiple exclamation/question marks to single punctuation with space
            .replace(Regex("!+"), "! ")
            .replace(Regex("\\?+"), "? ")
            // Clean up multi-spaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun stop() {
        neuralClient.stop()
        if (isInitialized) {
            tts?.stop()
            onSpeakingStateChanged(false)
        }
    }

    /**
     * Zero-latency cartoon sound effects on body touch events.
     */
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
