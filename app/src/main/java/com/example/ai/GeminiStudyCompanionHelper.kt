package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.model.FlashcardCatalog
import com.example.model.MoodType
import com.example.model.PetCharacter
import com.example.model.QuizCatalog
import com.example.model.QuizQuestion
import com.example.model.StudyFlashcard
import com.example.ui.HomeworkSolution
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Modular AI helper for Multimodal Homework Vision Scan, Flashcard Generation,
 * Interactive Quiz Generation, and Daily Mood Journal Responses using Firebase AI Logic.
 */
class GeminiStudyCompanionHelper(
    private val chatService: GeminiChatService
) {
    companion object {
        private const val TAG = "GeminiStudyHelper"
        private val MODEL_PRIORITY = listOf(
            "gemini-3.5-flash-lite",
            "gemini-3.8-flash"
        )
    }

    private val flashcardCache = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, List<StudyFlashcard>>(24, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<StudyFlashcard>>?): Boolean = size > 20
        }
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val homeworkVisionSchema by lazy {
        Schema.obj(
            mapOf(
                "subject" to Schema.string("Mata pelajaran"),
                "questionSummary" to Schema.string("Teks soal yang terbaca dari foto"),
                "stepByStep" to Schema.array(
                    Schema.string("Langkah penyelesaian ringkas dan akurat")
                ),
                "finalAnswer" to Schema.string("Jawaban akhir akurat dari soal di foto"),
                "tutorTip" to Schema.string("Tips belajar singkat gaya gaul khas karakter")
            )
        )
    }

    private val flashcardListSchema by lazy {
        Schema.array(
            Schema.obj(
                mapOf(
                    "questionFront" to Schema.string("Pertanyaan singkat untuk sisi depan kartu"),
                    "answerBack" to Schema.string("Jawaban inti/rumus akurat untuk sisi belakang kartu"),
                    "memoryTip" to Schema.string("Cara cepat menghafal ala karakter")
                )
            )
        )
    }

    /**
     * 1. MULTIMODAL VISION: Reads and solves homework from a Camera/Gallery photo (Bitmap).
     */
    suspend fun analyzeHomeworkWithVision(
        subject: String,
        questionText: String,
        photoBitmap: Bitmap,
        character: PetCharacter
    ): HomeworkSolution? = withContext(Dispatchers.IO) {
        val scaledBitmap = scaleBitmapDown(photoBitmap, 768)
        val prompt = """
            Tutor: ${character.name} (${character.species.displayName}), watak: ${character.personalityPrompt}.
            Baca soal tugas sekolah di foto ini (Mapel: $subject, Catatan: "${questionText.ifBlank { "Selesaikan soal di foto" }}").
            Jelaskan 3 langkah penyelesaian akurat & jawaban akhirnya dengan Bahasa Indonesia kasual gaul (santuy, gaspol, sabi, wkwk).
        """.trimIndent()

        // Pass 1 (Primary): Firebase AI Logic Multimodal Vision with Structured JSON Schema
        try {
            val fbSolution = withTimeoutOrNull(35_000L) {
                for (modelName in MODEL_PRIORITY) {
                    try {
                        val model = Firebase.ai.generativeModel(
                            modelName = modelName,
                            generationConfig = generationConfig {
                                responseMimeType = "application/json"
                                responseSchema = homeworkVisionSchema
                                temperature = 0.25f
                                maxOutputTokens = 450
                            }
                        )
                        val response = model.generateContent(
                            content {
                                image(scaledBitmap)
                                text(prompt)
                            }
                        )
                        val fbJsonText = response.text?.trim()
                        if (!fbJsonText.isNullOrBlank()) {
                            parseHomeworkJson(fbJsonText, subject, questionText, character)?.let {
                                Log.d(TAG, "Firebase vision homework succeeded with $modelName")
                                return@withTimeoutOrNull it
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (inner: Exception) {
                        Log.w(TAG, "Firebase vision model $modelName failed: ${inner.message}")
                    }
                }
                null
            }
            if (fbSolution != null) {
                return@withContext fbSolution
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase multimodal vision error: ${e.message}", e)
        }

        // Pass 2 (Secondary): Direct Gemini REST API with Base64 inline_data if custom API key is set
        val apiKey = chatService.getEffectiveApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val base64Jpeg = encodeBitmapToBase64Jpeg(scaledBitmap)
                val jsonText = withTimeoutOrNull(20_000L) {
                    var result = ""
                    for (modelName in MODEL_PRIORITY) {
                        result = callMultimodalGeminiRest(modelName, apiKey, prompt, base64Jpeg)
                        if (result.isNotBlank()) break
                    }
                    result
                }
                if (!jsonText.isNullOrBlank()) {
                    parseHomeworkJson(jsonText, subject, questionText, character)?.let {
                        return@withContext it
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Multimodal REST vision call failed: ${e.message}")
            }
        }

        // Pass 3: Smart Vision Fallback when offline
        val fallbackSummary = if (questionText.isNotBlank()) {
            "Hasil Scan Foto Soal ($subject): $questionText"
        } else {
            "Hasil Scan Foto Lembar Tugas $subject (${scaledBitmap.width}x${scaledBitmap.height}px)"
        }
        val baseFallback = chatService.solveHomeworkDeterministically(
            subject = subject,
            question = questionText.ifBlank { "Analisis soal dari foto lembar tugas $subject" },
            char = character
        )
        return@withContext baseFallback.copy(
            questionSummary = fallbackSummary
        )
    }

    /**
     * 2. FLASHCARD GENERATOR: Generates interactive study flashcards from any school topic.
     */
    suspend fun generateFlashcardsForTopic(
        subject: String,
        customTopic: String,
        character: PetCharacter
    ): List<StudyFlashcard> = withContext(Dispatchers.IO) {
        val topicLabel = customTopic.trim().ifBlank { subject }
        val cacheKey = "${character.id}_${subject.lowercase()}_${topicLabel.lowercase()}"
        flashcardCache[cacheKey]?.let { return@withContext it }

        val prompt = """
            Tutor: ${character.name}. Buat 4 kartu flashcard belajar ringkas (Tanya-Jawab akurat + memoryTip gaya gaul ${character.name}) untuk Mapel: $subject, Topik: $topicLabel.
        """.trimIndent()

        // Pass 1 (Primary): Firebase AI Logic with Structured JSON Array Schema
        try {
            val fbCards = withTimeoutOrNull(30_000L) {
                for (modelName in MODEL_PRIORITY) {
                    try {
                        val model = Firebase.ai.generativeModel(
                            modelName = modelName,
                            generationConfig = generationConfig {
                                responseMimeType = "application/json"
                                responseSchema = flashcardListSchema
                                temperature = 0.4f
                                maxOutputTokens = 450
                            }
                        )
                        val response = model.generateContent(prompt)
                        val rawJson = response.text?.trim()
                        if (!rawJson.isNullOrBlank()) {
                            val parsed = parseFlashcardArrayJson(rawJson, subject, topicLabel, character)
                            if (parsed.isNotEmpty()) {
                                Log.d(TAG, "Firebase AI flashcards succeeded with $modelName")
                                return@withTimeoutOrNull parsed
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (inner: Exception) {
                        Log.w(TAG, "Firebase AI flashcard model $modelName failed: ${inner.message}")
                    }
                }
                null
            }
            if (!fbCards.isNullOrEmpty()) {
                flashcardCache[cacheKey] = fbCards
                return@withContext fbCards
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI flashcard generation error: ${e.message}", e)
        }

        // Pass 2 (Secondary): Custom API Key REST call if configured
        val apiKey = chatService.getEffectiveApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val rawJson = withTimeoutOrNull(30_000L) {
                    var res = ""
                    for (modelName in MODEL_PRIORITY) {
                        res = callTextJsonGeminiRest(modelName, apiKey, prompt)
                        if (res.isNotBlank()) break
                    }
                    res
                }
                if (!rawJson.isNullOrBlank()) {
                    val cards = parseFlashcardArrayJson(rawJson, subject, topicLabel, character)
                    if (cards.isNotEmpty()) return@withContext cards
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Custom REST Flashcard generation fallback: ${e.message}")
            }
        }

        // Fallback: Combine default deck with custom topic card if user typed a custom topic
        val baseDeck = FlashcardCatalog.getDefaultDeck(subject)
        if (customTopic.isNotBlank() && !customTopic.equals(subject, ignoreCase = true)) {
            val solved = chatService.solveHomeworkDeterministically(subject, customTopic, character)
            val customCard = StudyFlashcard(
                id = "custom_${System.currentTimeMillis()}",
                subject = "$subject • $customTopic",
                questionFront = "Konsep & Pembahasan Inti: $customTopic",
                answerBack = "${solved.finalAnswer}\n• ${solved.stepByStep.firstOrNull() ?: ""}",
                memoryTip = solved.tutorTip
            )
            return@withContext listOf(customCard) + baseDeck
        }
        return@withContext baseDeck
    }

    private fun parseFlashcardArrayJson(
        rawJson: String,
        subject: String,
        topicLabel: String,
        character: PetCharacter
    ): List<StudyFlashcard> {
        return try {
            val clean = rawJson.replace(Regex("^```json|^```|```$", RegexOption.MULTILINE), "").trim()
            val arr = JSONArray(clean)
            val cards = mutableListOf<StudyFlashcard>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val q = obj.optString("questionFront").trim()
                val a = obj.optString("answerBack").trim()
                val tip = obj.optString("memoryTip").trim()
                if (q.isNotBlank() && a.isNotBlank()) {
                    cards.add(
                        StudyFlashcard(
                            id = "ai_card_${UUID.randomUUID()}",
                            subject = "$subject • $topicLabel",
                            questionFront = q,
                            answerBack = a,
                            memoryTip = tip.ifBlank { "Tips ${character.name}: Pahami kata kuncinya biar auto nempel di kepala!" }
                        )
                    )
                }
            }
            cards
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 3. EMPATHETIC MOOD JOURNAL RESPONDER: Responds to daily mood & curhat in character.
     */
    suspend fun generateMoodCurhatResponse(
        mood: MoodType,
        userNote: String,
        character: PetCharacter
    ): String = withContext(Dispatchers.IO) {
        val cleanNote = userNote.trim()
        val prompt = """
            Sahabatmu baru saja mengisi Jurnal Mood Harian:
            - Mood hari ini: ${mood.emoji} ${mood.label}
            - Isi curhatan: "${cleanNote.ifBlank { "Hari ini aku lagi ngerasa ${mood.label.lowercase()} nih." }}"
            
            Berikan tanggapan empatik, hangat, menghibur, dan nyambung sebagai ${character.name} dengan gaya bahasa gaul kekinian Indonesia (santuy, sabi, relate, wkwk, bestie, semangat) dalam 2-3 kalimat!
        """.trimIndent()

        try {
            val reply = withTimeoutOrNull(45_000L) {
                chatService.generateReply(
                    userMessage = prompt,
                    character = character,
                    language = com.example.model.AppLanguage.INDONESIAN,
                    history = emptyList()
                )
            }
            if (reply != null && reply.isFromAi && reply.text.isNotBlank()) {
                return@withContext reply.text
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Mood response AI fallback: ${e.message}")
        }

        return@withContext buildDeterministicMoodResponse(mood, cleanNote, character)
    }

    private fun buildDeterministicMoodResponse(
        mood: MoodType,
        note: String,
        character: PetCharacter
    ): String {
        val snippet = if (note.isNotBlank()) " Soal \"$note\", " else " "
        return when (character.id) {
            "zayn" -> when (mood) {
                MoodType.SENANG, MoodType.SEMANGAT -> "Hmph, kelihatan banget tuh muka kamu lagi ${mood.label.lowercase()}!$snippet ikut seneng deh liatnya, wkwk. B-bukan berarti aku merhatiin kamu terus ya, tapi pertahanin vibes positifnya!"
                MoodType.SANTUY -> "Wah lagi mode santuy nih ceritanya?$snippet sesekali chill dan rebahan itu sabi banget kok biar otak nggak ngebul, wkwk!"
                MoodType.CAPEK, MoodType.GALAU -> "Heh, kenapa kamu kelihatan ${mood.label.lowercase()} gitu?!$snippet jangan dipendem sendiri dong, sini cerita sama aku! Istirahat dulu gih, aku temenin di sini sampai mood kamu balik lagi!"
            }
            "siti" -> when (mood) {
                MoodType.SENANG, MoodType.SEMANGAT -> "Alhamdulillah, Siti ikut bahagia banget denger kamu lagi ${mood.label.lowercase()} hari ini sayangku!$snippet Semoga harimu makin penuh berkah dan senyuman gemoy ya, hihi."
                MoodType.SANTUY -> "Senangnya lihat kamu lagi santuy dan tenang hari ini manisku...$snippet Nikmati waktu istirahatmu ya, Siti siap nemenin kamu ngobrol santai."
                MoodType.CAPEK, MoodType.GALAU -> "Peluk jauh buat kamu sayangku...$snippet Wajar banget kok kalau hari ini rasanya ${mood.label.lowercase()}, kamu udah berjuang hebat banget. Istirahat ya manis, Siti selalu ada buat kamu."
            }
            "rizky" -> when (mood) {
                MoodType.SENANG, MoodType.SEMANGAT -> "SIAP KOMANDAN! Semangat dan energi positif terpantau 100% penuh!$snippet Mari kita gaspol pertahankan momentum juara ini!"
                MoodType.SANTUY -> "Siap! Mode pemulihan taktis (Santuy) diaktifkan!$snippet Menjaga ketenangan pikiran itu strategi penting buat misi selanjutnya, komandan!"
                MoodType.CAPEK, MoodType.GALAU -> "Perhatian Komandan!$snippet Setiap prajurit hebat pasti pernah merasa ${mood.label.lowercase()}. Ambil waktu istirahat sekarang, tarik napas dalam-dalam, Rizky siap siaga menjaga komandan!"
            }
            "jamal" -> when (mood) {
                MoodType.SENANG, MoodType.SEMANGAT -> "Mantap jiwa bro! Lagi ${mood.label.lowercase()} gini auranya langsung cerah banget wkwk.$snippet Gas terus hal-hal yang bikin lu happy!"
                MoodType.SANTUY -> "Nah ini baru satu frekuensi sama Jamal bro!$snippet Mode santuy tanpa beban emang paling juara, sini rebahan bareng wkwk."
                MoodType.CAPEK, MoodType.GALAU -> "Waduh bro, lagi ${mood.label.lowercase()} ya?$snippet Relate banget sih, hidup kadang suka bikin pegel. Rebahan dulu bro, seduh minuman hangat, besok kita mulai lagi dengan santuy."
            }
            else -> when (mood) {
                MoodType.SENANG, MoodType.SEMANGAT -> "WUIH GITU DONG! Seneng gua liat bestie gua lagi ${mood.label.lowercase()} gini wkwk!$snippet Gaspol terus jangan kasih kendor!"
                MoodType.SANTUY -> "Asyik bener yang lagi mode santuy wkwk!$snippet Nikmatin aja waktu luang lu bro, jangan mikirin yang berat-berat dulu!"
                MoodType.CAPEK, MoodType.GALAU -> "HEH! Siapa yang bikin bestie gua ${mood.label.lowercase()} hari ini?!$snippet Udah jangan dipikirin terus sampai pusing, sini istirahat bareng gua biar energi lu balik 100% lagi!"
            }
        }
    }

    private fun parseHomeworkJson(
        rawJson: String,
        defaultSubject: String,
        defaultQuestion: String,
        character: PetCharacter
    ): HomeworkSolution? {
        return try {
            val clean = rawJson.replace(Regex("^```json|^```|```$", RegexOption.MULTILINE), "").trim()
            val obj = JSONObject(clean)
            val steps = mutableListOf<String>()
            val arr = obj.optJSONArray("stepByStep")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    steps.add(arr.getString(i))
                }
            }
            if (steps.isEmpty()) return null
            HomeworkSolution(
                subject = obj.optString("subject", defaultSubject),
                questionSummary = obj.optString("questionSummary", defaultQuestion.ifBlank { "Soal dari Foto ($defaultSubject)" }),
                stepByStep = steps,
                finalAnswer = obj.optString("finalAnswer", "Pembahasan foto soal selesai."),
                tutorTip = obj.optString("tutorTip", "Tips ${character.name}: Pahami konsep dari langkah pertama biar makin jago!")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun callMultimodalGeminiRest(
        modelName: String,
        apiKey: String,
        prompt: String,
        base64Jpeg: String
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Jpeg)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.35)
                put("maxOutputTokens", 800)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val bodyStr = response.body?.string() ?: return ""
            val json = JSONObject(bodyStr)
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text").trim()
                }
            }
        }
        return ""
    }

    private fun callTextJsonGeminiRest(
        modelName: String,
        apiKey: String,
        prompt: String
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.5)
                put("maxOutputTokens", 800)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val bodyStr = response.body?.string() ?: return ""
            val json = JSONObject(bodyStr)
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text").trim()
                }
            }
        }
        return ""
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (newW, newH) = if (ratio > 1f) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun encodeBitmapToBase64Jpeg(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
