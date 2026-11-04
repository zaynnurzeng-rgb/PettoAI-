package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import com.example.model.AppLanguage
import com.example.model.CharacterGender
import com.example.model.PetCharacter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class VoiceEngineMode(val displayName: String, val icon: String, val description: String) {
    GEMINI_NATURAL(
        displayName = "Gemini Natural Voice (Rekomendasi)",
        icon = "✨",
        description = "Suara manusia natural ala Gemini Live dengan pembeda gender Cowok/Cewek & intonasi kalimat dinamis."
    ),
    GOOGLE_CLOUD_NEURAL(
        displayName = "Google Cloud Neural2 & WaveNet",
        icon = "🌐",
        description = "Vokal studio Google Cloud Neural2 dengan karakter suara berbeda untuk tiap avatar."
    ),
    ELEVENLABS_NEURAL(
        displayName = "ElevenLabs Neural Voice",
        icon = "🎙️",
        description = "Suara AI ekspresif multibahasa dengan warna vokal unik tiap karakter."
    ),
    CARTOON_MIMIC(
        displayName = "Kartun Imut (Talking Tom)",
        icon = "🐱",
        description = "Gaya suara animasi kartun dengan pitch tinggi dan efek suara sentuhan lucu."
    )
}

class NeuralTtsClient(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) {
    private val TAG = "NeuralTtsClient"
    private var mediaPlayer: MediaPlayer? = null
    private var pcmAudioTrack: AudioTrack? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    fun stop() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }
        try {
            pcmAudioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
            pcmAudioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        }
        onSpeakingStateChanged(false)
    }

    /**
     * Synthesize and play natural character voice using Gemini TTS model
     * (gemini-3.1-flash-tts-preview / gemini-2.5-flash-preview-tts) with prebuiltVoiceConfig
     * (Puck, Aoede, Kore, Charon, Orus, Fenrir) when an API key is configured.
     */
    suspend fun speakWithGeminiNativeTts(
        text: String,
        apiKey: String,
        character: PetCharacter
    ): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false

        val ttsModels = listOf("gemini-2.5-flash-preview-tts", "gemini-2.0-flash-exp")
        for (modelName in ttsModels) {
            try {
                stop()
                val jsonBody = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().apply {
                                put(
                                    "parts",
                                    JSONArray().put(
                                        JSONObject().put("text", text)
                                    )
                                )
                            }
                        )
                    )
                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("responseModalities", JSONArray().put("AUDIO"))
                            put(
                                "speechConfig",
                                JSONObject().apply {
                                    put(
                                        "voiceConfig",
                                        JSONObject().apply {
                                            put(
                                                "prebuiltVoiceConfig",
                                                JSONObject().apply {
                                                    put("voiceName", character.voicePersona.geminiVoiceName)
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=${apiKey.trim()}")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) continue

                val responseBody = response.body?.string() ?: continue
                val root = JSONObject(responseBody)
                val inlineData = root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optJSONObject("inlineData") ?: continue

                val mimeType = inlineData.optString("mimeType", "")
                val base64Audio = inlineData.optString("data", "")
                if (base64Audio.isBlank()) continue

                val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                return@withContext if (mimeType.contains("pcm", ignoreCase = true) || mimeType.contains("L16", ignoreCase = true)) {
                    playPcm24kAsWav(audioBytes)
                } else {
                    playAudioBytes(audioBytes)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Gemini TTS ($modelName) fallback: ${e.message}")
            }
        }
        false
    }

    /**
     * Synthesize and play speech via Google Cloud Text-to-Speech (Neural2 / WaveNet)
     * with character-specific voice & gender differentiation.
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
            val ssmlGender = if (character.gender == CharacterGender.FEMALE) "FEMALE" else "MALE"
            // Natural semitone shift (-6.0 to +6.0 semitones around the neural voice baseline)
            val pitchSemitones = ((pitch - 1.0f) * 10.0f).coerceIn(-8.0f, 8.0f)
            val speedRate = rate.coerceIn(0.80f, 1.35f)

            val jsonBody = JSONObject().apply {
                put("input", JSONObject().put("text", text))
                put("voice", JSONObject().apply {
                    put("languageCode", langCode)
                    put("name", voiceName)
                    put("ssmlGender", ssmlGender)
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
     * Synthesize and play speech via ElevenLabs Neural Voice API with distinct Male/Female character voices.
     */
    suspend fun speakWithElevenLabs(
        text: String,
        apiKey: String,
        character: PetCharacter
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            Log.w(TAG, "ElevenLabs API Key is blank")
            return@withContext false
        }

        // Character-tailored ElevenLabs voice presets (Male vs Female differentiation)
        val voiceId = when (character.id) {
            "zayn" -> "TX3LPaxmHKxFdv7VOQHJ"  // Liam - Youthful cool male
            "siti" -> "EXAVITQu4vr4xnSDxMaL"  // Bella - Warm gentle female
            "mochi" -> "jBpfuIE2acCO8z3wKNLl" // Gigi - Bright cheerful female
            "rizky" -> "pNInz6obpgDQGcFmaJgB" // Adam - Confident baritone male
            "jamal" -> "onwK4e9ZLuTAKqWW03F9" // Daniel - Deep chill male
            else -> "VR6AewLTigWG4xSOukaG"    // Arnold - Deep bass male bear
        }

        val models = listOf("eleven_multilingual_v2", "eleven_flash_v2_5", "eleven_turbo_v2_5")
        for (modelId in models) {
            try {
                stop()

                val jsonBody = JSONObject().apply {
                    put("text", text)
                    put("model_id", modelId)
                    put("voice_settings", JSONObject().apply {
                        put("stability", 0.45)
                        put("similarity_boost", 0.85)
                        put("style", 0.35)
                        put("use_speaker_boost", true)
                    })
                }

                val request = Request.Builder()
                    .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                    .addHeader("xi-api-key", cleanKey)
                    .addHeader("Accept", "audio/mpeg")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "ElevenLabs ($modelId) error ${response.code}: ${response.message}")
                    continue
                }

                val audioBytes = response.body?.bytes() ?: continue
                if (audioBytes.isEmpty()) continue
                return@withContext playAudioBytes(audioBytes)
            } catch (e: Exception) {
                Log.e(TAG, "ElevenLabs ($modelId) request failed", e)
            }
        }
        return@withContext false
    }

    private fun playPcm24kAsWav(pcmBytes: ByteArray): Boolean {
        return try {
            val wavBytes = wrapPcm16MonoToWav(pcmBytes, sampleRate = 24000)
            val tempFile = File.createTempFile("gemini_tts_", ".wav", context.cacheDir)
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use { fos ->
                fos.write(wavBytes)
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
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error playing Gemini WAV audio", e)
            onSpeakingStateChanged(false)
            false
        }
    }

    private fun wrapPcm16MonoToWav(pcmData: ByteArray, sampleRate: Int): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * 2 // 16-bit mono = 2 bytes per sample
        val header = ByteArray(44)

        // RIFF/WAVE header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat = 1 (PCM)
        header[21] = 0
        header[22] = 1 // NumChannels = 1 (Mono)
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2 // BlockAlign = 2 bytes
        header[33] = 0
        header[34] = 16 // BitsPerSample = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmData.size and 0xff).toByte()
        header[41] = ((pcmData.size shr 8) and 0xff).toByte()
        header[42] = ((pcmData.size shr 16) and 0xff).toByte()
        header[43] = ((pcmData.size shr 24) and 0xff).toByte()

        val wavBytes = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, wavBytes, 0, 44)
        System.arraycopy(pcmData, 0, wavBytes, 44, pcmData.size)
        return wavBytes
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
        val persona = character.voicePersona
        return when (language) {
            AppLanguage.INDONESIAN -> "id-ID" to persona.idCloudVoiceName
            AppLanguage.ENGLISH -> "en-US" to persona.enCloudVoiceName
            AppLanguage.JAPANESE -> "ja-JP" to persona.jaCloudVoiceName
            AppLanguage.SPANISH -> "es-ES" to persona.esCloudVoiceName
        }
    }
}
