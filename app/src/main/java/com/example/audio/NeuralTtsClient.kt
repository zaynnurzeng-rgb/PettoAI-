package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import com.example.model.AppLanguage
import com.example.model.PetCharacter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class VoiceEngineMode(val displayName: String, val icon: String, val description: String) {
    CARTOON_MIMIC(
        displayName = "Kartun Ekspresif (Talking Tom)",
        icon = "🐱",
        description = "Pitch shift dan formant modulasi karakter kartun dengan efek suara fisik sentuh seketika."
    ),
    GOOGLE_CLOUD_NEURAL(
        displayName = "Google Cloud Neural2 Voice",
        icon = "🌐",
        description = "Intonasi manusia alami dan ekspresif menggunakan Google Cloud Text-to-Speech Neural2."
    ),
    ELEVENLABS_NEURAL(
        displayName = "ElevenLabs Neural Voice",
        icon = "✨",
        description = "Suara AI hiper-realistis dengan emosi mendalam dan gaya ekspresi dinamis."
    )
}

class NeuralTtsClient(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) {
    private val TAG = "NeuralTtsClient"
    private var mediaPlayer: MediaPlayer? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    fun stop() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
            onSpeakingStateChanged(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }
    }

    /**
     * Synthesize and play speech via Google Cloud Text-to-Speech (Neural2 / Journey).
     */
    suspend fun speakWithGoogleCloud(
        text: String,
        apiKey: String,
        character: PetCharacter,
        language: AppLanguage,
        pitch: Float,
        rate: Float
    ): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.w(TAG, "Google Cloud TTS API Key is blank")
            return@withContext false
        }

        try {
            stop()

            val (langCode, voiceName) = getGoogleVoiceConfig(language, character)
            // Pitch in semitones (-20.0 to 20.0), scale from pitch factor
            val pitchSemitones = ((pitch - 1.0f) * 8.0f).coerceIn(-12.0f, 12.0f)
            val speedRate = rate.coerceIn(0.7f, 1.6f)

            val jsonBody = JSONObject().apply {
                put("input", JSONObject().put("text", text))
                put("voice", JSONObject().apply {
                    put("languageCode", langCode)
                    put("name", voiceName)
                })
                put("audioConfig", JSONObject().apply {
                    put("audioEncoding", "MP3")
                    put("pitch", pitchSemitones)
                    put("speakingRate", speedRate)
                })
            }

            val request = Request.Builder()
                .url("https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Google Cloud TTS error ${response.code}: ${response.message}")
                return@withContext false
            }

            val responseBody = response.body?.string() ?: return@withContext false
            val audioContentBase64 = JSONObject(responseBody).optString("audioContent")
            if (audioContentBase64.isBlank()) return@withContext false

            val audioBytes = Base64.decode(audioContentBase64, Base64.DEFAULT)
            return@withContext playAudioBytes(audioBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Google Cloud TTS request failed", e)
            return@withContext false
        }
    }

    /**
     * Synthesize and play speech via ElevenLabs Neural Voice API.
     */
    suspend fun speakWithElevenLabs(
        text: String,
        apiKey: String,
        character: PetCharacter
    ): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.w(TAG, "ElevenLabs API Key is blank")
            return@withContext false
        }

        try {
            stop()

            // Character tailored ElevenLabs voice presets
            val voiceId = when (character.id) {
                "zayn" -> "TX3LPaxmHKxFdv7VOQHJ" // Liam / youthful lively
                "siti" -> "EXAVITQu4vr4xnSDxMaL" // Bella / sweet gentle
                "rizky" -> "pNInz6obpgDQGcFmaJgB" // Adam / confident dynamic
                "jamal" -> "onwK4e9ZLuTAKqWW03F9" // Daniel / deep chill
                else -> "VR6AewLTigWG4xSOukaG"    // Arnold / bold energetic bear
            }

            val jsonBody = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.40)
                    put("similarity_boost", 0.85)
                    put("style", 0.45)
                    put("use_speaker_boost", true)
                })
            }

            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Accept", "audio/mpeg")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "ElevenLabs error ${response.code}: ${response.message}")
                return@withContext false
            }

            val audioBytes = response.body?.bytes() ?: return@withContext false
            return@withContext playAudioBytes(audioBytes)
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs request failed", e)
            return@withContext false
        }
    }

    private fun playAudioBytes(bytes: ByteArray): Boolean {
        try {
            val tempFile = File.createTempFile("tts_audio_", ".mp3", context.cacheDir)
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use { fos ->
                fos.write(bytes)
            }

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { player ->
                    onSpeakingStateChanged(true)
                    player.start()
                }
                setOnCompletionListener {
                    onSpeakingStateChanged(false)
                    tempFile.delete()
                }
                setOnErrorListener { _, _, _ ->
                    onSpeakingStateChanged(false)
                    tempFile.delete()
                    false
                }
                prepareAsync()
            }
            mediaPlayer = mp
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio bytes", e)
            onSpeakingStateChanged(false)
            return false
        }
    }

    private fun getGoogleVoiceConfig(language: AppLanguage, character: PetCharacter): Pair<String, String> {
        return when (language) {
            AppLanguage.INDONESIAN -> {
                val voiceName = if (character.id == "siti") "id-ID-Neural2-A" else "id-ID-Neural2-B"
                "id-ID" to voiceName
            }
            AppLanguage.ENGLISH -> {
                val voiceName = if (character.id == "siti") "en-US-Journey-F" else "en-US-Journey-D"
                "en-US" to voiceName
            }
            AppLanguage.JAPANESE -> {
                val voiceName = if (character.id == "siti") "ja-JP-Neural2-B" else "ja-JP-Neural2-C"
                "ja-JP" to voiceName
            }
            AppLanguage.SPANISH -> {
                val voiceName = if (character.id == "siti") "es-ES-Neural2-A" else "es-ES-Neural2-B"
                "es-ES" to voiceName
            }
        }
    }
}
