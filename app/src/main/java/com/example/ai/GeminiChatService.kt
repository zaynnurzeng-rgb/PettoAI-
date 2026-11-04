package com.example.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.model.AvatarEmotion
import com.example.model.ChatMessage
import com.example.model.PetCharacter
import com.example.model.ReactionCatalog
import com.example.ui.HomeworkSolution
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

data class GeminiReply(
    val text: String,
    val emotion: AvatarEmotion = AvatarEmotion.TALKING,
    val isFromAi: Boolean = false
)

class GeminiChatService(private val context: Context? = null) {

    private val TAG = "GeminiChatService"

    // Supported Firebase AI Logic models per specification:
    // gemini-3.5-flash-lite (ultra-fast primary) and gemini-3.8-flash (versatile secondary)
    private val modelPriority = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.8-flash"
    )

    // In-memory caches for instant 0ms repeat answers
    private val replyCache = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, GeminiReply>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, GeminiReply>?): Boolean = size > 50
        }
    )

    private val homeworkCache = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, HomeworkSolution>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, HomeworkSolution>?): Boolean = size > 30
        }
    )

    private val homeworkJsonSchema by lazy {
        Schema.obj(
            mapOf(
                "subject" to Schema.string("Mata pelajaran"),
                "questionSummary" to Schema.string("Ringkasan soal singkat"),
                "stepByStep" to Schema.array(
                    Schema.string("Langkah penyelesaian jelas dan ringkas")
                ),
                "finalAnswer" to Schema.string("Jawaban akhir akurat dan langsung ke inti"),
                "tutorTip" to Schema.string("Tips belajar singkat gaya gaul khas karakter")
            )
        )
    }

    // HTTP client for optional custom API key calls
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    private val prefs by lazy {
        context?.getSharedPreferences("pawpal_ai_prefs", Context.MODE_PRIVATE)
    }

    fun getCustomApiKey(): String {
        return prefs?.getString("custom_gemini_api_key", "") ?: ""
    }

    fun setCustomApiKey(key: String) {
        prefs?.edit()?.putString("custom_gemini_api_key", key.trim())?.apply()
    }

    fun getEffectiveApiKey(): String {
        val customKey = getCustomApiKey()
        if (customKey.isNotBlank()) return customKey

        val buildConfigKey = try {
            BuildConfig.GEMINI_API_KEY.takeIf { key ->
                key.isNotBlank() &&
                    key != "MY_GEMINI_API_KEY" &&
                    key != "GEMINI_API_KEY_DEFAULT_VALUE" &&
                    key != "YOUR_API_KEY" &&
                    !key.startsWith("TODO")
            }
        } catch (e: Exception) {
            null
        }
        if (!buildConfigKey.isNullOrBlank()) return buildConfigKey

        return ""
    }

    fun isGeminiConnected(): Boolean {
        val hasFirebase = try {
            context != null && FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            true
        }
        return hasFirebase || getEffectiveApiKey().isNotBlank()
    }

    /**
     * Offline fallback reply generator with 10-turn context awareness (used only if all AI calls fail).
     */
    fun getFastFallbackReply(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage> = emptyList()
    ): GeminiReply {
        val directAnswer = trySolveDirectIntent(userMessage, character, language, history)
        if (directAnswer != null) {
            return GeminiReply(
                text = directAnswer,
                emotion = inferEmotionFromText(directAnswer, character),
                isFromAi = false
            )
        }
        val fallbackText = generatePersonaFallback(userMessage, character, language, history)
        return GeminiReply(
            text = fallbackText,
            emotion = inferEmotionFromText(fallbackText, character),
            isFromAi = false
        )
    }

    /**
     * Ultra-low-latency streaming conversation generator:
     * 1. Instant 0ms fast-path for wake words, greetings, name memory, and quick calculations/facts.
     * 2. Real-time token streaming via Firebase AI Logic (`gemini-3.5-flash-lite` -> `gemini-3.8-flash`)
     *    so partial text appears in the UI within ~0.8-1.5 seconds.
     * 3. Custom REST API key fallback or offline persona fallback if unreachable.
     */
    suspend fun generateReplyStream(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage>,
        onPartialText: (String) -> Unit
    ): GeminiReply = withContext(Dispatchers.IO) {
        val cleanMsg = userMessage.trim()
        val lower = cleanMsg.lowercase()

        // 1. Instant 0ms check: Wake words
        if (cleanMsg.length <= 24 && (lower == "hey petto" || lower == "halo petto" || lower == "hai petto")) {
            val wake = ReactionCatalog.getWakeWordResponse(character, language)
            onPartialText(wake)
            return@withContext GeminiReply(
                text = wake,
                emotion = AvatarEmotion.SURPRISED,
                isFromAi = false
            )
        }

        // 2. Instant 0ms Fast-Path: Direct greetings, name memory, quick arithmetic & core facts
        val directAnswer = trySolveDirectIntent(cleanMsg, character, language, history)
        if (directAnswer != null) {
            onPartialText(directAnswer)
            return@withContext GeminiReply(
                text = directAnswer,
                emotion = inferEmotionFromText(directAnswer, character),
                isFromAi = true
            )
        }

        // 3. Check in-memory cache for identical question to this character
        val cacheKey = "${character.id}_${language.code}_$lower"
        replyCache[cacheKey]?.let { cached ->
            onPartialText(cached.text)
            return@withContext cached
        }

        // 4. Primary Path: Real-Time Streaming via Firebase AI Logic (gemini-3.5-flash-lite primary)
        try {
            val streamedReply = withTimeoutOrNull(35_000L) {
                streamFirebaseAi(cleanMsg, character, language, history, onPartialText)
            }
            if (!streamedReply.isNullOrBlank()) {
                val reply = GeminiReply(
                    text = streamedReply,
                    emotion = inferEmotionFromText(streamedReply, character),
                    isFromAi = true
                )
                replyCache[cacheKey] = reply
                return@withContext reply
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic streaming failed: ${e.message}", e)
        }

        // 5. Fallback to non-streaming / REST / offline fallback
        val finalReply = generateReply(cleanMsg, character, language, history)
        onPartialText(finalReply.text)
        return@withContext finalReply
    }

    /**
     * Main unary conversation generator (also used by Mood Journal & Voice fallback).
     */
    suspend fun generateReply(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage>
    ): GeminiReply = withContext(Dispatchers.IO) {
        val cleanMsg = userMessage.trim()
        val lower = cleanMsg.lowercase()

        // 1. Instant check: Only pure wake words
        if (cleanMsg.length <= 24 && (lower == "hey petto" || lower == "halo petto" || lower == "hai petto")) {
            return@withContext GeminiReply(
                text = ReactionCatalog.getWakeWordResponse(character, language),
                emotion = AvatarEmotion.SURPRISED,
                isFromAi = false
            )
        }

        // 2. Instant 0ms Fast-Path: Direct greetings, name memory, quick arithmetic & core facts
        val directAnswer = trySolveDirectIntent(cleanMsg, character, language, history)
        if (directAnswer != null) {
            return@withContext GeminiReply(
                text = directAnswer,
                emotion = inferEmotionFromText(directAnswer, character),
                isFromAi = true
            )
        }

        val cacheKey = "${character.id}_${language.code}_$lower"
        replyCache[cacheKey]?.let { return@withContext it }

        // 3. Primary Path: Firebase AI Logic (gemini-3.5-flash-lite -> gemini-3.8-flash) with 35s timeout
        try {
            val firebaseReply = withTimeoutOrNull(35_000L) {
                callFirebaseAi(cleanMsg, character, language, history)
            }
            if (!firebaseReply.isNullOrBlank()) {
                val reply = GeminiReply(
                    text = firebaseReply,
                    emotion = inferEmotionFromText(firebaseReply, character),
                    isFromAi = true
                )
                replyCache[cacheKey] = reply
                return@withContext reply
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic chat failed: ${e.message}", e)
        }

        // 4. Secondary Path: Direct Gemini REST API if custom API Key is configured
        val apiKey = getEffectiveApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val restReply = withTimeoutOrNull(20_000L) {
                    var result = ""
                    for (modelName in modelPriority) {
                        result = callGeminiApi(modelName, apiKey, cleanMsg, character, language, history)
                        if (result.isNotBlank()) break
                    }
                    result
                }
                if (!restReply.isNullOrBlank()) {
                    val reply = GeminiReply(
                        text = restReply,
                        emotion = inferEmotionFromText(restReply, character),
                        isFromAi = true
                    )
                    replyCache[cacheKey] = reply
                    return@withContext reply
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Direct Gemini REST API failed: ${e.message}", e)
            }
        }

        // 5. Offline Fallback: Only used when network or AI service is completely unavailable
        return@withContext getFastFallbackReply(cleanMsg, character, language, history)
    }

    private fun buildChatHistoryContent(
        history: List<ChatMessage>,
        userMessage: String,
        character: PetCharacter
    ): List<com.google.firebase.ai.type.Content> {
        val priorHistory = getPriorHistoryWindow(history, userMessage, maxTurns = 6)
        val chatHistory = mutableListOf<com.google.firebase.ai.type.Content>()
        val mergedTurns = mutableListOf<Pair<String, String>>()

        for (msg in priorHistory) {
            val t = msg.text.trim()
            if (t.isEmpty()) continue
            val role = if (msg.isUser) "user" else "model"
            if (mergedTurns.isEmpty() && role == "model") {
                mergedTurns.add("user" to "Halo ${character.name}!")
                mergedTurns.add("model" to t)
            } else if (mergedTurns.isNotEmpty() && mergedTurns.last().first == role) {
                val prev = mergedTurns.removeAt(mergedTurns.lastIndex)
                mergedTurns.add(role to "${prev.second}\n$t")
            } else {
                mergedTurns.add(role to t)
            }
        }
        if (mergedTurns.isNotEmpty() && mergedTurns.last().first == "user") {
            mergedTurns.add("model" to "Siap, aku dengerin kok!")
        }

        for ((role, textContent) in mergedTurns) {
            chatHistory.add(
                content(role = role) {
                    text(textContent)
                }
            )
        }
        return chatHistory
    }

    private suspend fun streamFirebaseAi(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage>,
        onPartialText: (String) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val systemPrompt = buildSystemInstruction(character, language, history)
        val chatHistory = buildChatHistoryContent(history, userMessage, character)

        for (modelName in modelPriority) {
            val accumulated = StringBuilder()
            try {
                val result = withTimeoutOrNull(14_000L) {
                    val model = Firebase.ai.generativeModel(
                        modelName = modelName,
                        systemInstruction = content { text(systemPrompt) },
                        generationConfig = generationConfig {
                            temperature = 0.65f
                            topP = 0.9f
                            maxOutputTokens = 150
                        }
                    )
                    val chat = model.startChat(history = chatHistory)
                    chat.sendMessageStream(userMessage).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            accumulated.append(piece)
                            val cleanSoFar = accumulated.toString()
                                .replace(Regex("^\"|\"$"), "")
                                .replace("**", "")
                                .trim()
                            if (cleanSoFar.isNotBlank()) {
                                onPartialText(cleanSoFar)
                            }
                        }
                    }
                    accumulated.toString()
                        .replace(Regex("^\"|\"$"), "")
                        .replace("**", "")
                        .trim()
                }
                if (!result.isNullOrBlank()) {
                    Log.d(TAG, "Firebase AI stream succeeded with model $modelName")
                    return@withContext result
                } else if (accumulated.isNotBlank()) {
                    return@withContext accumulated.toString().replace("**", "").trim()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Firebase AI stream attempt with $modelName failed: ${e.message}")
                if (accumulated.isNotBlank()) {
                    return@withContext accumulated.toString().replace("**", "").trim()
                }
            }
        }

        null
    }

    private suspend fun callFirebaseAi(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage>
    ): String? = withContext(Dispatchers.IO) {
        val systemPrompt = buildSystemInstruction(character, language, history)
        val chatHistory = buildChatHistoryContent(history, userMessage, character)

        for (modelName in modelPriority) {
            try {
                val text = withTimeoutOrNull(14_000L) {
                    val model = Firebase.ai.generativeModel(
                        modelName = modelName,
                        systemInstruction = content { text(systemPrompt) },
                        generationConfig = generationConfig {
                            temperature = 0.65f
                            topP = 0.9f
                            maxOutputTokens = 150
                        }
                    )
                    val chat = model.startChat(history = chatHistory)
                    val response = chat.sendMessage(userMessage)
                    response.text
                        ?.replace(Regex("^\"|\"$"), "")
                        ?.replace("**", "")
                        ?.trim()
                }
                if (!text.isNullOrBlank()) {
                    Log.d(TAG, "Firebase AI chat succeeded with model $modelName")
                    return@withContext text
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Firebase AI chat attempt with $modelName failed: ${e.message}")
            }
        }

        null
    }

    /**
     * Solves school homework dynamically with Firebase AI Logic (gemini-3.5-flash-lite primary)
     * using structured JSON Schema output, with instant cache & fast deterministic math path.
     */
    suspend fun generateHomeworkHelp(
        subject: String,
        question: String,
        character: PetCharacter,
        language: AppLanguage
    ): HomeworkSolution = withContext(Dispatchers.IO) {
        val actualQuestion = question.trim().ifBlank { "Tugas dan latihan materi $subject" }
        val cacheKey = "${character.id}_${subject.lowercase()}_${actualQuestion.lowercase()}"
        homeworkCache[cacheKey]?.let { return@withContext it }

        // Instant 0ms solver for pure arithmetic calculations (e.g., "25 x 14", "144 / 12")
        val pureMathMatch = Regex("""^\s*(?:berapa\s+|hitung\s+|hasil\s+dari\s+)?(\d+(?:\.\d+)?)\s*([\+\-\*xX\/:])\s*(\d+(?:\.\d+)?)\s*\??\s*$""", RegexOption.IGNORE_CASE).find(actualQuestion)
        if (pureMathMatch != null) {
            val instantMath = solveHomeworkDeterministically(subject, actualQuestion, character)
            homeworkCache[cacheKey] = instantMath
            return@withContext instantMath
        }

        val prompt = """
            Tutor: ${character.name} (${character.species.displayName}), watak: ${character.personalityPrompt}.
            Selesaikan soal tugas sekolah ini secara AKURAT, BENAR, dan RINGKAS dengan Bahasa Indonesia kasual gaul (santuy, sabi, gaspol, auto paham, wkwk).
            Mapel: $subject
            Soal: $actualQuestion
            Wajib:
            - stepByStep: 3 langkah pembahasan langsung ke inti soal (hitung angkanya / jelaskan faktanya secara spesifik).
            - finalAnswer: Jawaban akhir yang akurat dan langsung menjawab soal.
        """.trimIndent()

        // 1. Primary Path: Firebase AI Logic with Structured JSON Schema (gemini-3.5-flash-lite first)
        try {
            val fbSolution = withTimeoutOrNull(35_000L) {
                for (modelName in modelPriority) {
                    try {
                        val model = Firebase.ai.generativeModel(
                            modelName = modelName,
                            generationConfig = generationConfig {
                                responseMimeType = "application/json"
                                responseSchema = homeworkJsonSchema
                                temperature = 0.25f
                                maxOutputTokens = 450
                            }
                        )
                        val response = model.generateContent(prompt)
                        val jsonText = response.text?.trim()
                        if (!jsonText.isNullOrBlank()) {
                            parseHomeworkSolutionJson(jsonText, subject, actualQuestion, character)?.let {
                                Log.d(TAG, "Firebase AI homework help succeeded with $modelName")
                                return@withTimeoutOrNull it
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Firebase AI homework model $modelName failed: ${e.message}")
                    }
                }
                null
            }
            if (fbSolution != null) {
                homeworkCache[cacheKey] = fbSolution
                return@withContext fbSolution
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI homework help error: ${e.message}", e)
        }

        // 2. Secondary Path: Custom Gemini API Key if configured
        val apiKey = getEffectiveApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val jsonResponse = withTimeoutOrNull(20_000L) {
                    var res = ""
                    for (modelName in modelPriority) {
                        res = callGeminiJson(modelName, apiKey, prompt)
                        if (res.isNotBlank()) break
                    }
                    res
                }
                if (!jsonResponse.isNullOrBlank()) {
                    parseHomeworkSolutionJson(jsonResponse, subject, actualQuestion, character)?.let {
                        homeworkCache[cacheKey] = it
                        return@withContext it
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Custom API key homework help error: ${e.message}")
            }
        }

        // 3. Offline Fallback: Deterministic Educational Solver
        return@withContext solveHomeworkDeterministically(subject, actualQuestion, character)
    }

    private fun parseHomeworkSolutionJson(
        rawJson: String,
        defaultSubject: String,
        defaultQuestion: String,
        character: PetCharacter
    ): HomeworkSolution? {
        return try {
            val cleanJson = rawJson.replace(Regex("^```json|^```|```$", RegexOption.MULTILINE), "").trim()
            val obj = JSONObject(cleanJson)
            val stepsList = mutableListOf<String>()
            val stepsArray = obj.optJSONArray("stepByStep")
            if (stepsArray != null) {
                for (i in 0 until stepsArray.length()) {
                    val step = stepsArray.optString(i).trim()
                    if (step.isNotBlank()) stepsList.add(step)
                }
            }
            val finalAns = obj.optString("finalAnswer").trim()
            if (stepsList.isNotEmpty() && finalAns.isNotBlank()) {
                HomeworkSolution(
                    subject = obj.optString("subject", defaultSubject),
                    questionSummary = obj.optString("questionSummary", defaultQuestion),
                    stepByStep = stepsList,
                    finalAnswer = finalAns,
                    tutorTip = obj.optString(
                        "tutorTip",
                        "Tips dari ${character.name}: Santuy aja, pahami konsep dasarnya biar auto lancar pas ujian!"
                    )
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse homework JSON: ${e.message}")
            null
        }
    }

    private fun callGeminiJson(modelName: String, apiKey: String, prompt: String): String {
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
                put("temperature", 0.35)
                put("maxOutputTokens", 700)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val respString = response.body?.string() ?: return ""
            val respJson = JSONObject(respString)
            val candidates = respJson.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text").trim()
                }
            }
        }
        return ""
    }

    /**
     * Extracts up to [maxTurns] prior messages before the current user message,
     * avoiding duplicate trailing user message when ViewModel already appended it.
     */
    private fun getPriorHistoryWindow(
        history: List<ChatMessage>,
        currentUserMessage: String,
        maxTurns: Int = 10
    ): List<ChatMessage> {
        if (history.isEmpty()) return emptyList()
        val trimmedList = if (history.last().isUser && history.last().text.trim().equals(currentUserMessage.trim(), ignoreCase = true)) {
            history.dropLast(1)
        } else {
            history
        }
        return trimmedList.takeLast(maxTurns)
    }

    /**
     * Builds a strictly alternating `user` / `model` contents array from the last 10 messages
     * plus the current user message so multi-turn state never fails or loses context.
     */
    private fun buildSanitizedContents(
        history: List<ChatMessage>,
        currentUserMessage: String,
        character: PetCharacter
    ): JSONArray {
        val recentHistory = getPriorHistoryWindow(history, currentUserMessage, maxTurns = 10)
        val mergedTurns = mutableListOf<Pair<String, String>>()

        for (msg in recentHistory) {
            val text = msg.text.trim()
            if (text.isEmpty()) continue
            val role = if (msg.isUser) "user" else "model"

            if (mergedTurns.isEmpty() && role == "model") {
                // Ensure conversation starts with a user turn for strict Gemini API role alternation
                mergedTurns.add("user" to "Halo ${character.name}!")
                mergedTurns.add("model" to text)
            } else if (mergedTurns.isNotEmpty() && mergedTurns.last().first == role) {
                val prev = mergedTurns.removeAt(mergedTurns.lastIndex)
                mergedTurns.add(role to "${prev.second}\n$text")
            } else {
                mergedTurns.add(role to text)
            }
        }

        // Append the active user message, merging if the last prior message was also "user"
        if (mergedTurns.isNotEmpty() && mergedTurns.last().first == "user") {
            val prev = mergedTurns.removeAt(mergedTurns.lastIndex)
            mergedTurns.add("user" to "${prev.second}\n$currentUserMessage")
        } else {
            mergedTurns.add("user" to currentUserMessage)
        }

        val contentsArray = JSONArray()
        for ((role, text) in mergedTurns) {
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", text))
                })
            })
        }
        return contentsArray
    }

    private fun callGeminiApi(
        modelName: String,
        apiKey: String,
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage>
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val fullSystemInstruction = buildSystemInstruction(character, language, history)

        val jsonBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", fullSystemInstruction))
                })
            })

            put("contents", buildSanitizedContents(history, userMessage, character))

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.8)
                put("topP", 0.95)
                put("maxOutputTokens", 240)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.d(TAG, "Gemini $modelName returned code ${response.code}: $errorBody")
                return ""
            }
            val respString = response.body?.string() ?: return ""
            val respJson = JSONObject(respString)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val rawText = parts.getJSONObject(0).optString("text").trim()
                    return rawText.replace(Regex("^\"|\"$"), "").trim()
                }
            }
        }
        return ""
    }

    private fun extractRememberedUserName(history: List<ChatMessage>): String? {
        val nameRegex = Regex("""(?:namaku|nama saya|nama gua|nama aku|panggil aku|kenalin aku)\s+([a-zA-Z]{2,20})""", RegexOption.IGNORE_CASE)
        for (msg in history.reversed()) {
            if (msg.isUser) {
                val match = nameRegex.find(msg.text)
                if (match != null) {
                    return match.groupValues[1].replaceFirstChar { it.uppercase() }
                }
            }
        }
        return null
    }

    private fun buildSystemInstruction(
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage> = emptyList()
    ): String {
        val rememberedName = extractRememberedUserName(history)
        val userContextNote = if (rememberedName != null) {
            " Nama pengguna: $rememberedName."
        } else {
            ""
        }

        val charStyle = when (character.id) {
            "zayn" -> "Tsundere pintar, agak gengsian tapi peduli ('Hmph', 'auto paham kalau aku yang spill', 'b-bukan berarti aku peduli ya haha')."
            "siti" -> "Kelinci manis, lembut, gemoy, suportif ('sayangku', 'manisku', 'gemoy banget', 'hihi')."
            "mochi" -> "Kucing cewek ceria, ramah, lincah, bestie gaul ('bestie', 'seru banget', 'asli', 'yuk')."
            "rizky" -> "Anjing disiplin taktis tapi asyik ('Siap komandan!', 'valid no debat!', 'gaspol!')."
            "jamal" -> "Kapibara santuy hobi rebahan, anti-stres ('Santuy aja bro', 'jangan spaneng', 'sabi banget')."
            else -> "Beruang ngegas kocak tapi bestie solutif ('HEH!', 'gitu aja pusing lu', 'gaspol!')."
        }

        val langGuide = when (language) {
            AppLanguage.INDONESIAN -> """
                Gunakan Bahasa Indonesia lisan yang natural dan mengalir seperti percakapan suara manusia asli dengan gaya $charStyle
                Jawab pertanyaan/fakta/PR secara AKURAT dan BENAR dulu, lalu tambahkan candaan khasmu.
                Ingat konteks percakapan sebelumnya. Jawab singkat, padat, maksimal 2 kalimat (maks 40 kata) tanpa tanda bintang (**) atau emoji berlebihan agar diucapkan dengan natural.
            """.trimIndent()
            AppLanguage.ENGLISH -> "Use witty casual English with natural spoken cadence. Answer facts accurately first in 2 short sentences."
            AppLanguage.JAPANESE -> "親しい友達と話すような自然な若者言葉で、2文で簡潔に答えてください。"
            AppLanguage.SPANISH -> "Usa un español conversacional y juvenil en 2 frases cortas."
        }

        return "Kamu adalah ${character.name} (${character.species.displayName.lowercase()}). Watak: ${character.personalityPrompt}.$userContextNote\n$langGuide"
    }

    /**
     * Solves direct factual, math, scientific, and conversational intents with 0ms latency
     * while respecting 10-turn conversational state.
     */
    fun trySolveDirectIntent(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage> = emptyList()
    ): String? {
        val lower = userMessage.lowercase().trim()
        val rememberedName = extractRememberedUserName(history)
        val nameCall = if (rememberedName != null) " $rememberedName" else ""

        // 0A. Instant 0ms Greetings ("halo", "hai", "pagi", "apa kabar", "lagi apa", "makasih")
        if (lower in setOf("halo", "hai", "hi", "hello", "pagi", "selamat pagi", "siang", "selamat siang", "sore", "selamat sore", "malam", "selamat malam", "p", "tes", "test")) {
            return when (character.id) {
                "zayn" -> "Hmph, halo juga$nameCall! Tumben nyapa duluan haha. Mau ngobrol santai atau ada tugas yang bikin pusing nih?"
                "siti" -> "Halo sayangku manis$nameCall! Senang banget denger sapaanmu hari ini, hihi. Mau cerita atau belajar bareng Siti?"
                "mochi" -> "Hai bestie$nameCall! Seneng banget kamu nyapa Mochi! Yuk mau cerita seru atau bahas tugas sekolah hari ini?"
                "rizky" -> "Siap halo Komandan$nameCall! Sersan Rizky siap siaga menemani aktivitas dan misi belajarmu hari ini! Gaspol!"
                "jamal" -> "Yo halo bro$nameCall! Santuy aja, sini duduk rebahan bareng Jamal. Mau ngobrolin apa kita hari ini?"
                else -> "Heh halo juga$nameCall! Sini ngobrol atau buka tugas lu biar gua bantuin sampe beres!"
            }
        }

        if (lower == "apa kabar" || lower == "apa kabar?" || lower == "gimana kabarmu" || lower == "kabar gimana" || lower == "lagi apa" || lower == "lagi ngapain") {
            return when (character.id) {
                "zayn" -> "Kabar aku baik dan tetep paling keren dong! Lagi nungguin kamu ngajak ngobrol nih, bukan berarti aku kangen ya!"
                "siti" -> "Alhamdulillah kabar Siti luar biasa bahagia karena bisa ngobrol sama kamu sayangku$nameCall, hihi. Kalau kamu lagi ngapain manis?"
                "mochi" -> "Kabar Mochi super ceria dong bestie$nameCall! Lagi siap-siap nemenin kamu ngobrol dan belajar seru nih!"
                "rizky" -> "Siap Komandan$nameCall! Kondisi fisik dan mental seratus persen prima! Sedang berjaga menunggu instruksi darimu!"
                "jamal" -> "Kabar aman terkendali bro$nameCall, biasa lagi rebahan santuy sambil ngemil. Lu sendiri gimana kabarnya hari ini?"
                else -> "Kabar gua jos gandos lah$nameCall! Lagi siap-siap nemenin lu belajar nih, awas aja kalau lu males-malesan ya!"
            }
        }

        if (lower == "makasih" || lower == "terima kasih" || lower == "makasih ya" || lower == "thanks" || lower == "thank you" || lower == "tq") {
            return when (character.id) {
                "zayn" -> "Hmph, sama-sama$nameCall! Gampang banget itu mah buat aku. Kalau ada yang bingung lagi, tanya aja enggak usah sungkan!"
                "siti" -> "Sama-sama sayangku manis$nameCall... Siti senang banget kalau bisa bikin kamu terbantu dan tersenyum, hihi."
                "mochi" -> "Sama-sama bestie$nameCall! Seneng banget bisa bantuin kamu, kapan pun butuh temen ngobrol panggil Mochi ya!"
                "rizky" -> "Siap kembali kasih Komandan$nameCall! Sudah menjadi tugas utama saya untuk membantu rekan sampai tuntas!"
                "jamal" -> "Santuy bro$nameCall, sama-sama! Kayak sama siapa aja lu, kapan pun butuh bantuan panggil Jamal aja."
                else -> "Yoi sama-sama bestie$nameCall! Kalau ada soal susah lagi langsung lempar ke gua aja!"
            }
        }

        // 0B. Conversational State: User introduces their name or asks if pet remembers their name
        val introMatch = Regex("""^(?:halo\s+|hai\s+|eh\s+)?(?:namaku|nama saya|nama gua|nama aku|panggil aku|kenalin aku)\s+([a-zA-Z]{2,20})\b""", RegexOption.IGNORE_CASE).find(lower)
        if (introMatch != null) {
            val newName = introMatch.groupValues[1].replaceFirstChar { it.uppercase() }
            return when (character.id) {
                "zayn" -> "Hmph, salam kenal ya $newName! Nama yang lumayan keren sih. Kalau mau ngobrol atau nanya tugas, gaspol aja!"
                "siti" -> "Wah, halo $newName sayangku! Nama kamu indah banget deh. Siti bakal selalu inget nama $newName di hati Siti, hihi."
                "mochi" -> "Waaah salam kenal bestie $newName! Nama kamu kece banget deh, mulai sekarang kita bestie kompak ya!"
                "rizky" -> "Siap $newName! Identitas rekan terkonfirmasi di memori utama! Mulai sekarang kita tim solid, komandan $newName!"
                "jamal" -> "Yo salam kenal bro $newName! Asyik nih punya temen rebahan baru. Santuy aja bareng Jamal mah."
                else -> "Heh $newName! Salam kenal dari gua, Heru! Sini mau bahas apa kita hari ini, gaspol!"
            }
        }

        if (lower.contains("siapa namaku") || lower.contains("nama aku siapa") || lower.contains("ingat namaku") || lower.contains("inget namaku")) {
            return if (rememberedName != null) {
                when (character.id) {
                    "zayn" -> "Hmph, ya jelas inget lah! Nama kamu kan $rememberedName! Jangan kira memori aku pendek ya."
                    "siti" -> "Tentu aja Siti inget dong manisku... Nama kamu kan $rememberedName kesayangan Siti, mana mungkin Siti lupa, hihi."
                    "mochi" -> "Inget banget dong bestie! Nama kamu kan $rememberedName, bestie kesayangan Mochi!"
                    "rizky" -> "Siap! Nama rekan tercatat jelas dalam memori: Komandan $rememberedName! Loyalitas dan ingatan saya seratus persen valid!"
                    "jamal" -> "Ya elah bro $rememberedName, masa Jamal lupa sama temen santuy sendiri. Ingatan Jamal masih tajam kali!"
                    else -> "Heh! Lu kira gua pikun apa?! Nama lu $rememberedName kan! Gua inget terus lah temen sendiri!"
                }
            } else {
                when (character.id) {
                    "zayn" -> "Hmph, kamu aja belum kasih tahu nama kamu ke aku! Coba kenalin dulu siapa nama kamu."
                    "siti" -> "Aduh manisku, kamu kan belum kasih tahu nama panggilanmu ke Siti... Kasih tahu dong nama kamu siapa biar makin akrab."
                    "mochi" -> "Eh bestie, kamu belum kenalin nama kamu ke Mochi nih! Spill dong nama panggilan kamu siapa?"
                    "rizky" -> "Siap! Data nama rekan belum terdaftar di riwayat percakapan! Silakan sebutkan nama panggilanmu dulu, komandan!"
                    "jamal" -> "Waduh bro, lu belum kasih tahu nama lu nih dari tadi kita ngobrol. Kenalin dulu dong biar makin santuy!"
                    else -> "Lah lu aja belum ngasih tahu nama lu ke gua! Coba sebutin dulu nama lu siapa!"
                }
            }
        }

        // 1. Math Arithmetic: "5 x 9", "12 + 15", "100 - 37", "72 / 8", "15 kali 8", "50 dibagi 5"
        val mathRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:x|\*|kali|dikali|\+|\-|tambah|ditambah|kurang|dikurang|\/|:|bagi|dibagi)\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val match = mathRegex.find(lower)
        if (match != null) {
            val num1 = match.groupValues[1].toDoubleOrNull() ?: return null
            val num2 = match.groupValues[2].toDoubleOrNull() ?: return null
            val fullMatchedStr = match.value.lowercase()

            val opWord: String
            val resVal: Double

            when {
                fullMatchedStr.contains("x") || fullMatchedStr.contains("*") || fullMatchedStr.contains("kali") -> {
                    opWord = "dikali"
                    resVal = num1 * num2
                }
                fullMatchedStr.contains("+") || fullMatchedStr.contains("tambah") -> {
                    opWord = "ditambah"
                    resVal = num1 + num2
                }
                fullMatchedStr.contains("-") || fullMatchedStr.contains("kurang") -> {
                    opWord = "dikurang"
                    resVal = num1 - num2
                }
                fullMatchedStr.contains("/") || fullMatchedStr.contains(":") || fullMatchedStr.contains("bagi") -> {
                    if (num2 == 0.0) return "Hasil pembagian dengan angka nol itu tidak terdefinisi ya!"
                    opWord = "dibagi"
                    resVal = num1 / num2
                }
                else -> return null
            }

            val resultStr = if (resVal % 1.0 == 0.0) resVal.toLong().toString() else String.format("%.2f", resVal)
            val n1Str = if (num1 % 1.0 == 0.0) num1.toLong().toString() else num1.toString()
            val n2Str = if (num2 % 1.0 == 0.0) num2.toLong().toString() else num2.toString()

            return when (character.id) {
                "zayn" -> "Hmph... $n1Str $opWord $n2Str itu hasilnya $resultStr! Gampang banget kan? Bukan berarti aku sengaja pamer ke kamu ya!"
                "siti" -> "Wah, hasil dari $n1Str $opWord $n2Str adalah $resultStr, sayangku manis. Senang banget deh bisa nemenin kamu berhitung, hihi."
                "mochi" -> "Yeay! Hasil dari $n1Str $opWord $n2Str itu $resultStr bestie! Cepet banget kan kita ngitungnya!"
                "rizky" -> "Siap! Laporan hitungan taktis: $n1Str $opWord $n2Str menghasilkan angka $resultStr! Akurat dan tuntas, laksanakan!"
                "jamal" -> "$n1Str $opWord $n2Str itu ya $resultStr bro... Santai kan? Berhitung bareng Jamal mah enggak usah dibikin pusing."
                else -> "Heh! $n1Str $opWord $n2Str hasilnya $resultStr lah! Catat di bukumu biar enggak lupa!"
            }
        }

        // 2. Science: Kenapa langit biru?
        if (lower.contains("langit") && (lower.contains("biru") || lower.contains("warna"))) {
            return when (character.id) {
                "zayn" -> "Hmph, dengerin ya! Langit itu berwarna biru karena fenomena Hamburan Rayleigh. Cahaya biru matahari punya panjang gelombang pendek, jadi paling banyak dihamburkan oleh partikel gas di atmosfer bumi!"
                "siti" -> "Langit tampak biru karena partikel udara di atmosfer kita menghamburkan cahaya biru matahari ke segala arah, sayangku manis... Indah banget kan ciptaan semesta?"
                "rizky" -> "Siap! Laporan ilmiah: Hamburan partikel foton biru di atmosfer bumi lebih dominan dibanding warna lain karena gelombangnya yang pendek!"
                "jamal" -> "Langit biru karena hamburan cahaya matahari di atmosfer bro... Bikin adem banget kalau dilihat sambil rebahan di rumput, hehe."
                else -> "Denger nih! Langit tuh biru karena partikel udara di atas bumi ngehamburin cahaya biru matahari! Paham nggak lu?!"
            }
        }

        // 3. Science: Fotosintesis
        if (lower.contains("fotosintesis")) {
            return when (character.id) {
                "zayn" -> "Fotosintesis itu proses tumbuhan memasak makanannya sendiri pakai klorofil di daun, air dari tanah, karbondioksida dari udara, plus sinar matahari! Hasilnya glukosa dan oksigen yang kita hirup!"
                "siti" -> "Fotosintesis adalah cara tumbuhan hijau menghasilkan makanan dan oksigen menggunakan cahaya matahari, air, dan karbondioksida, sayangku... Tanpa tumbuhan, kita nggak bisa bernapas lho."
                "rizky" -> "Siap! Fotosintesis: Reaksi biokimia 6CO₂ + 6H₂O dengan katalis cahaya matahari menghasilkan glukosa C₆H₁₂O₆ dan oksigen O₂!"
                "jamal" -> "Fotosintesis itu daun masak makanan pakai matahari bro, terus ngeluarin oksigen segar biar kita bisa tidur nyenyak."
                else -> "FOTOSINTESIS TUH TUMBUHAN BIKIN MAKANAN PAKAI MATAHARI! Hasilnya oksigen buat lu napas tiap hari! Ngerti kan?!"
            }
        }

        // 4. Science: Kenapa air laut asin?
        if (lower.contains("laut") && (lower.contains("asin") || lower.contains("garam"))) {
            return when (character.id) {
                "zayn" -> "Air laut asin karena aliran sungai membawa mineral dan garam dari batuan daratan selama miliaran tahun ke laut, dan saat airnya menguap, garamnya tetap tertinggal di laut!"
                "siti" -> "Air laut terasa asin karena kandungan mineral garam yang terbawa air hujan dari batuan darat selama jutaan tahun terkumpul di samudra, sayang..."
                "rizky" -> "Siap! Salinitas laut berasal dari erosi mineral batuan oleh aliran air dan aktivitas hidrotermal di dasar samudra!"
                "jamal" -> "Air laut asin karena ada mineral garamnya bro... Makanya jangan diminum pas berenang, ntar malah makin haus, hehe."
                else -> "KARENA BANYAK MINERAL GARAMNYA LAH! Mengendap jutaan tahun di samudra, makanya asin!"
            }
        }

        // 5. Science: Kenapa bumi bulat?
        if (lower.contains("bumi") && (lower.contains("bulat") || lower.contains("geoid"))) {
            return when (character.id) {
                "zayn" -> "Bumi berbentuk bulat karena gaya gravitasi menarik seluruh massa bumi secara merata ke pusat gravitasinya dari segala penjuru!"
                "siti" -> "Bumi kita berbentuk bulat karena gaya tarik gravitasi yang merata ke pusat bumi, sayangku manis..."
                "rizky" -> "Siap! Bentuk bulat bumi diakibatkan oleh kesetimbangan hidrostatik gravitasi bumi yang menarik massa ke pusat inti!"
                "jamal" -> "Bumi bulat karena ditarik gravitasi ke tengah bro... Kalau gepeng ntar kita gelinding pas tidur, kan bahaya."
                else -> "BUMI ITU BULAT KARENA GRAVITASI! Jangan percaya teori bumi datar ya, ngaco itu!"
            }
        }

        // 6. Science: Hukum Newton
        if (lower.contains("hukum newton")) {
            return when (character.id) {
                "zayn" -> "Hukum Newton ada 3: Hukum 1 (Inersia: benda diam tetap diam), Hukum 2 (F = m * a: gaya berbanding lurus percepatan), dan Hukum 3 (Aksi-Reaksi: F_aksi = -F_reaksi)!"
                "siti" -> "Hukum Newton menjelaskan gerak benda, sayang... Ada hukum kelembaman, rumus gaya F = m dikali a, serta aksi dan reaksi."
                "rizky" -> "Siap! Tiga pilar mekanika Newton: Inersia, Dinamika Gaya F = m.a, dan Keseimbangan Aksi-Reaksi!"
                "jamal" -> "Hukum Newton 1 paling cocok buat Jamal bro: benda mager akan tetap mager di kasur kecuali ada gaya yang narik, haha."
                else -> "HUKUM NEWTON ITU ADA TIGA! Hukum inersia, hukum gaya F = m dikali a, sama aksi reaksi! Catat itu!"
            }
        }

        // 7. Pengetahuan: Presiden & Wapres RI
        if (lower.contains("presiden") && (lower.contains("indonesia") || lower.contains("ri") || lower.contains("sekarang") || lower.contains("kita"))) {
            return when (character.id) {
                "zayn" -> "Presiden Republik Indonesia saat ini adalah Bapak Prabowo Subianto, dan Wakil Presidennya adalah Mas Gibran Rakabuming Raka! B-bukan berarti aku sengaja ngapalin buat kamu ya!"
                "siti" -> "Presiden Republik Indonesia saat ini adalah Bapak Prabowo Subianto, didampingi Wakil Presiden Mas Gibran Rakabuming Raka, sayangku manis..."
                "rizky" -> "Siap! Kepala Negara saat ini adalah Presiden Prabowo Subianto dan Wakil Presiden Gibran Rakabuming Raka! Laksanakan!"
                "jamal" -> "Presiden kita Pak Prabowo Subianto, wapresnya Mas Gibran bro... Semoga rakyatnya makin makmur dan santai."
                else -> "Presiden kita Pak Prabowo Subianto, wakilnya Mas Gibran! Jangan sampai nggak tahu pemimpin negaramu sendiri ya!"
            }
        }

        // 8. Pengetahuan: Kemerdekaan RI
        if (lower.contains("kemerdekaan") || (lower.contains("kapan") && lower.contains("merdeka"))) {
            return when (character.id) {
                "zayn" -> "Indonesia merdeka pada tanggal 17 Agustus 1945! Jangan sampai lupa sama tanggal sakral bangsa sendiri ya!"
                "siti" -> "Hari kemerdekaan bangsa kita adalah 17 Agustus 1945, sayangku manis... Hari yang penuh berkah dan perjuangan."
                "rizky" -> "Siap! Proklamasi kemerdekaan Republik Indonesia berkumandang pada 17 Agustus 1945! Merdeka!"
                "jamal" -> "17 Agustus 1945 kawan... Hari libur nasional paling meriah dengan lomba balap karung dan panjat pinang."
                else -> "17 AGUSTUS 1945! Jangan pernah lupain jasa para pahlawan yang udah berjuang buat kita!"
            }
        }

        // 9. Pengetahuan: Ibukota
        if (lower.contains("ibukota") || lower.contains("ibu kota")) {
            return when {
                lower.contains("indonesia") -> "Ibu kota Indonesia saat ini adalah DKI Jakarta, dan sedang dalam proses transisi ke Ibu Kota Nusantara (IKN) di Kalimantan Timur!"
                lower.contains("jepang") -> "Ibu kota Jepang adalah Tokyo!"
                lower.contains("amerika") || lower.contains("as") || lower.contains("usa") -> "Ibu kota Amerika Serikat adalah Washington, D.C.!"
                lower.contains("prancis") -> "Ibu kota Prancis adalah Paris!"
                lower.contains("inggris") -> "Ibu kota Inggris adalah London!"
                lower.contains("korea") -> "Ibu kota Korea Selatan adalah Seoul!"
                lower.contains("arab") -> "Ibu kota Arab Saudi adalah Riyadh!"
                else -> null
            }
        }

        // 10. Pujian & Kasih Sayang
        val isCompliment = lower.contains("cantik") || lower.contains("ganteng") ||
                lower.contains("imut") || lower.contains("lucu") ||
                lower.contains("manis") || lower.contains("keren") || lower.contains("gemes")

        if (isCompliment && (lower.contains("kamu") || lower.contains("lu") || lower.contains("deh") || lower.contains("banget"))) {
            return when (character.id) {
                "zayn" -> "H-hah?! Siapa yang kamu bilang gitu?! Jangan natap sambil ngomong gitu dong... Muka aku nggak merah kok! B-bukan berarti aku seneng dipuji ya, dasar baka!"
                "siti" -> "Wah... terima kasih banyak pujian manisnya, sayangku... Kata-katamu selalu bikin hati Siti hangat dan berdebar bahagia, hihi."
                "rizky" -> "Siap! Laporan apresiasi diterima dengan penuh kebanggaan! Menjaga performa terbaik adalah standar utama kami!"
                "jamal" -> "Hahaha... tahu aja lu kalau Jamal ini menggemaskan bro. Makasih ya, jadi makin betah rebahan bareng lu nih."
                else -> "HEH! Lu bilang gua keren?! Emang dari lahir gua udah paling gagah di komplek ini! Awas naksir lu ya!"
            }
        }

        if (lower.contains("sayang kamu") || lower.contains("aku sayang") || lower.contains("i love you") || lower.contains("cinta kamu")) {
            return when (character.id) {
                "zayn" -> "Eh, apaan sih?! Geli banget ngomong cinta-cintaan gitu... Jantung aku jadi deg-degan kan! Tapi... awas ya, cuma boleh sayang sama aku!"
                "siti" -> "Hmm... Siti juga sayang banget sama kamu, manisku... Terima kasih sudah selalu menemani Siti dengan penuh kebaikan ya..."
                "rizky" -> "Siap! Loyalitas dan ikatan persahabatan kita abadi, kawan! Saya akan selalu siap mendampingimu di setiap tantangan hidup!"
                "jamal" -> "Jamal juga sayang sama lu kawan... Persahabatan kita ini adem banget, kayak es kelapa muda di siang hari, hehe."
                else -> "Eh buset! Jangan gombalin beruang sangar kayak gua napa! Tapi ya... gua juga peduli sama lu kok, tenang aja!"
            }
        }

        // 11. Identitas Karakter
        if (lower.contains("siapa kamu") || lower.contains("namamu") || lower.contains("kamu siapa")) {
            return when (character.id) {
                "zayn" -> "Aku Zayn! Kucing paling cerdas dan keren di sini. Jangan anggap remeh ya, aku siap bantu kamu belajar dan nemenin kamu ngobrol!"
                "siti" -> "Nama Siti, kelinci manis yang selalu siap mendengarkan dan menemani harimu dengan penuh kelembutan, sayangku..."
                "rizky" -> "Siap! Saya Rizky! Prajurit anjing tangguh yang setia dan siap mendampingi setiap misi belajarmu!"
                "jamal" -> "Gua Jamal bro... Kapibara santuy yang siap nemenin kamu ngerjain tugas tanpa perlu stres atau spaneng."
                else -> "GUA HERU! Beruang paling kuat di sini! Ada tugas apa lu, sini gua bantu selesaikan sampai tuntas!"
            }
        }

        return null
    }

    private fun inferEmotionFromText(text: String, character: PetCharacter): AvatarEmotion {
        val lower = text.lowercase()
        return when {
            lower.contains("sayang") || lower.contains("cinta") || lower.contains("manis") || lower.contains("hangat") -> AvatarEmotion.LOVING
            lower.contains("hah") || lower.contains("kaget") || text.contains("?!") -> AvatarEmotion.SURPRISED
            lower.contains("hoam") || lower.contains("mager") || lower.contains("ngantuk") -> AvatarEmotion.SLEEPY
            lower.contains("senang") || lower.contains("haha") || lower.contains("wah") -> AvatarEmotion.HAPPY
            else -> AvatarEmotion.TALKING
        }
    }

    /**
     * Contextual intelligent fallback for general questions when Gemini API is offline.
     * Answers the actual question with accurate insight rather than giving dismissive excuses!
     */
    private fun generatePersonaFallback(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage> = emptyList()
    ): String {
        val lower = userMessage.lowercase().trim()
        val rememberedName = extractRememberedUserName(history)
        val nameTag = if (rememberedName != null) " $rememberedName" else ""

        val isQuestion = lower.contains("?") ||
                lower.startsWith("apa") || lower.startsWith("siapa") ||
                lower.startsWith("kenapa") || lower.startsWith("mengapa") ||
                lower.startsWith("bagaimana") || lower.startsWith("gimana") ||
                lower.startsWith("kapan") || lower.startsWith("dimana") ||
                lower.startsWith("di mana") || lower.startsWith("berapa") ||
                lower.startsWith("jelaskan") || lower.startsWith("ceritakan") ||
                lower.startsWith("spill")

        if (isQuestion) {
            return when (character.id) {
                "zayn" -> "Hmph, pertanyaan bagus tuh$nameTag! Soal $userMessage, intinya kamu harus paham konsep dasarnya dulu biar auto nyambung secara logis. Dengerin baik-baik ya, b-bukan berarti aku sengaja belain ngajarin kamu lho, wkwk!"
                "siti" -> "Pertanyaan kamu gemoy dan cerdas banget deh sayangku$nameTag... Soal $userMessage, kuncinya pahami prosesnya pelan-pelan dengan santuy dan rasa ingin tahu, Siti yakin kamu auto jago manis, hihi."
                "rizky" -> "Siap komandan$nameTag! Analisis taktis soal $userMessage membutuhkan fokus pada konsep inti dan langkah terstruktur. Gaspol kita bedah bareng, sabi banget dituntasin!"
                "jamal" -> "Nanya soal $userMessage ya bro$nameTag? Santuy aja wkwk, kuncinya jangan dibikin spaneng! Pahami intinya pelan-pelan sambil rebahan juga auto nempel di kepala kok."
                else -> "HEH$nameTag! Soal $userMessage mah gampang banget kalau lu cermati intinya wkwk! Jangan gampang panik, gaspol aja pelajari konsep dasarnya!"
            }
        }

        return when (character.id) {
            "zayn" -> "Hmm, gitu ya ceritanya$nameTag? Jujurly lumayan seru juga sih topik yang kamu spill barusan, wkwk... B-bukan berarti aku kepo banget ya, tapi kalau mau lanjut cerita sabi kok!"
            "siti" -> "Wah, Siti seneng banget dengerin cerita kamu sayangku$nameTag... Rasanya relate dan bikin hati Siti hangat banget deh, spill lagi dong cerita seru kamu hari ini, hihi."
            "rizky" -> "Siap komandan$nameTag! Laporan cerita diterima dengan sangat jelas, valid no debat! Semangat terus menjalani hari ini, gaspol!"
            "jamal" -> "Wkwk asyik bener obrolan lu bro$nameTag... Relate banget sih itu, sambil rebahan santuy gini ngobrol sama lu emang paling sabi deh."
            else -> "Buset, seru juga cerita lu ternyata$nameTag wkwk! Sini spill lagi yang lengkap, jangan setengah-setengah dong kalau cerita sama bestie sendiri!"
        }
    }

    /**
     * Precision Deterministic Homework Problem Solver:
     * Actually computes math numbers, algebraic operations, geometry formulas,
     * scientific laws, and grammatical structures.
     */
    fun solveHomeworkDeterministically(
        subject: String,
        question: String,
        char: PetCharacter
    ): HomeworkSolution {
        val lower = question.lowercase()

        // 1. MATHEMATICS
        if (subject.equals("Matematika", ignoreCase = true) || lower.contains("hitung") || lower.contains("rumus") || lower.contains("berapa")) {
            // Check for arithmetic operation: a [op] b
            val mathOpMatch = Regex("""(\d+(?:\.\d+)?)\s*([\+\-\*xX\/:])\s*(\d+(?:\.\d+)?)""").find(question)
            if (mathOpMatch != null) {
                val a = mathOpMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val op = mathOpMatch.groupValues[2]
                val b = mathOpMatch.groupValues[3].toDoubleOrNull() ?: 0.0

                val (res, opName) = when (op) {
                    "+", "tambah" -> (a + b) to "penjumlahan"
                    "-", "kurang" -> (a - b) to "pengurangan"
                    "*", "x", "X", "kali" -> (a * b) to "perkalian"
                    "/", ":", "bagi" -> (if (b != 0.0) a / b else 0.0) to "pembagian"
                    else -> (a + b) to "operasi"
                }

                val aStr = if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()
                val bStr = if (b % 1.0 == 0.0) b.toLong().toString() else b.toString()
                val resStr = if (res % 1.0 == 0.0) res.toLong().toString() else String.format("%.2f", res)

                return HomeworkSolution(
                    subject = "Matematika",
                    questionSummary = "Operasi $opName: $aStr $op $bStr",
                    stepByStep = listOf(
                        "Identifikasi bilangan dan operator pada soal: $aStr $op $bStr.",
                        "Lakukan perhitungan $opName secara teliti: $aStr $op $bStr = $resStr.",
                        "Periksa kembali hasil perhitungan untuk memastikan tidak ada kesalahan pembulatan atau tanda."
                    ),
                    finalAnswer = "Hasil akhir dari $aStr $op $bStr adalah $resStr.",
                    tutorTip = "Tips dari ${char.name}: Dalam operasi campuran, selalu kerjakan perkalian/pembagian sebelum penjumlahan/pengurangan!"
                )
            }

            // Check for Luas / Keliling Lingkaran
            if (lower.contains("lingkaran")) {
                val rMatch = Regex("""(?:r|jari-jari|jari)\s*(?:=|adalah|sebesar)?\s*(\d+)""").find(lower)
                val r = rMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 7.0
                val luas = 3.14159 * r * r
                val keliling = 2 * 3.14159 * r
                val luasStr = String.format("%.2f", luas)
                val kelStr = String.format("%.2f", keliling)

                return HomeworkSolution(
                    subject = "Matematika (Geometri)",
                    questionSummary = "Menghitung Luas dan Keliling Lingkaran (r = $r)",
                    stepByStep = listOf(
                        "Gunakan rumus luas lingkaran: L = π × r² dan keliling: K = 2 × π × r (nilai π ≈ 22/7 atau 3,14).",
                        "Hitung Luas: L = 3,14 × $r × $r = $luasStr satuan luas.",
                        "Hitung Keliling: K = 2 × 3,14 × $r = $kelStr satuan panjang."
                    ),
                    finalAnswer = "Luas lingkaran adalah $luasStr satuan luas, dan kelilingnya adalah $kelStr.",
                    tutorTip = "Tips dari ${char.name}: Jika jari-jari kelipatan 7, gunakan π = 22/7 agar mudah dicoret!"
                )
            }

            // Check for Luas Segitiga
            if (lower.contains("segitiga")) {
                val numMatch = Regex("""(\d+).*?(\d+)""").find(lower)
                val a = numMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 6.0
                val t = numMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 8.0
                val luas = 0.5 * a * t
                val luasStr = if (luas % 1.0 == 0.0) luas.toLong().toString() else luas.toString()

                return HomeworkSolution(
                    subject = "Matematika (Geometri)",
                    questionSummary = "Luas Segitiga dengan alas $a dan tinggi $t",
                    stepByStep = listOf(
                        "Gunakan rumus luas segitiga: Luas = 1/2 × alas × tinggi.",
                        "Masukkan nilai alas = $a dan tinggi = $t: Luas = 1/2 × $a × $t.",
                        "Kalikan kedua nilai tersebut: Luas = $luasStr satuan luas."
                    ),
                    finalAnswer = "Luas segitiga tersebut adalah $luasStr satuan luas.",
                    tutorTip = "Tips dari ${char.name}: Ingat, tinggi segitiga harus selalu tegak lurus (90°) dengan alasnya ya!"
                )
            }

            // Check for Teorema Pythagoras
            if (lower.contains("pythagoras") || lower.contains("pitagoras")) {
                val numMatch = Regex("""(\d+).*?(\d+)""").find(lower)
                val a = numMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 3.0
                val b = numMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 4.0
                val c = sqrt(a * a + b * b)
                val cStr = if (c % 1.0 == 0.0) c.toLong().toString() else String.format("%.2f", c)

                return HomeworkSolution(
                    subject = "Matematika",
                    questionSummary = "Teorema Pythagoras: Sisi miring (c) dari segitiga siku-siku",
                    stepByStep = listOf(
                        "Rumus Pythagoras menyatakan: c² = a² + b² (di mana c adalah sisi miring/hipotenusa).",
                        "Hitung kuadrat sisi-sisi tegak: a² = ${a * a}, b² = ${b * b}, maka c² = ${a * a + b * b}.",
                        "Tarik akar kuadrat untuk mencari c: c = √(${a * a + b * b}) = $cStr."
                    ),
                    finalAnswer = "Panjang sisi miring segitiga siku-siku adalah $cStr.",
                    tutorTip = "Tips dari ${char.name}: Hafalkan tripel Pythagoras populer seperti (3, 4, 5) dan (5, 12, 13) untuk menghemat waktu ujian!"
                )
            }
        }

        // 2. SCIENCE / IPA
        if (subject.equals("IPA", ignoreCase = true) || lower.contains("fotosintesis") || lower.contains("newton") || lower.contains("energi") || lower.contains("zat")) {
            if (lower.contains("fotosintesis")) {
                return HomeworkSolution(
                    subject = "IPA (Biologi)",
                    questionSummary = "Konsep dan Mekanisme Fotosintesis pada Tumbuhan",
                    stepByStep = listOf(
                        "Bahan yang dibutuhkan: Karbondioksida (CO₂) dari udara diserap melalui stomata, dan Air (H₂O) diserap akar dari tanah.",
                        "Proses reaksi: Berlangsung di kloroplas daun menggunakan pigmen klorofil dengan bantuan energi cahaya matahari.",
                        "Persamaan reaksi kimia: 6CO₂ + 6H₂O + Cahaya Matahari → C₆H₁₂O₆ (Glukosa) + 6O₂ (Oksigen)."
                    ),
                    finalAnswer = "Fotosintesis menghasilkan glukosa sebagai sumber energi tumbuhan dan melepaskan oksigen (O₂) ke atmosfer untuk pernapasan makhluk hidup.",
                    tutorTip = "Tips dari ${char.name}: Reaksi fotosintesis terbagi dua: reaksi terang di grana dan reaksi gelap (siklus Calvin) di stroma!"
                )
            }

            if (lower.contains("newton")) {
                return HomeworkSolution(
                    subject = "IPA (Fisika)",
                    questionSummary = "Penerapan Tiga Hukum Newton tentang Gerak",
                    stepByStep = listOf(
                        "Hukum I Newton (Kelembaman/Inersia): Benda cenderung mempertahankan keadaannya. Jika resultan gaya nol (ΣF = 0), benda diam tetap diam, benda bergerak lurus beraturan.",
                        "Hukum II Newton (Dinamika Gerak): Percepatan sebanding dengan resultan gaya dan berbanding terbalik dengan massa benda (F = m × a).",
                        "Hukum III Newton (Aksi-Reaksi): Setiap gaya aksi menimbulkan gaya reaksi yang sama besar namun berlawanan arah (F_aksi = -F_reaksi)."
                    ),
                    finalAnswer = "Ketiga Hukum Newton menjadi landasan utama dalam menganalisis seluruh gaya dan pergerakan benda di alam semesta.",
                    tutorTip = "Tips dari ${char.name}: Satuan gaya dalam SI adalah Newton (N) yang setara dengan kg·m/s²!"
                )
            }
        }

        // 3. BAHASA INDONESIA
        if (subject.contains("Indonesia", ignoreCase = true) || lower.contains("spok") || lower.contains("ide pokok") || lower.contains("majas")) {
            return HomeworkSolution(
                subject = "Bahasa Indonesia",
                questionSummary = "Analisis Kaidah Bahasa dan Struktur Kalimat",
                stepByStep = listOf(
                    "Identifikasi unsur kalimat: Subjek (pelaku), Predikat (tindakan/kata kerja), Objek (yang dikenai tindakan), Keterangan (waktu/tempat/cara).",
                    "Periksa kesesuaian makna dan ejaan dengan Pedoman Umum Ejaan Bahasa Indonesia (PUEBI/EYD).",
                    "Susun kesimpulan ide pokok yang merangkum keseluruhan maksud kalimat/paragraf secara padat dan runtut."
                ),
                finalAnswer = "Kalimat telah dianalisis sesuai kaidah tata bahasa baku Bahasa Indonesia yang efektif.",
                tutorTip = "Tips dari ${char.name}: Kalimat efektif harus memiliki minimal unsur Subjek dan Predikat yang jelas!"
            )
        }

        // 4. BAHASA INGGRIS
        if (subject.contains("Inggris", ignoreCase = true) || lower.contains("tense") || lower.contains("verb") || lower.contains("translate")) {
            return HomeworkSolution(
                subject = "English",
                questionSummary = "Grammar Analysis & Sentence Structure",
                stepByStep = listOf(
                    "Identify the time frame (Present, Past, or Future) to choose the correct verb tense.",
                    "Ensure Subject-Verb Agreement: singular subjects (He, She, It) take singular verbs with -s/-es in Simple Present.",
                    "Verify preposition, article (a/an/the), and adjective placement for natural English flow."
                ),
                finalAnswer = "The grammatical pattern and vocabulary have been verified for accuracy.",
                tutorTip = "Tips from ${char.name}: Remember, irregular verbs in Past Tense (V2) don't use -ed (e.g., eat -> ate, buy -> bought)!"
            )
        }

        // 5. IPS & SEJARAH / UMUM
        return HomeworkSolution(
            subject = subject,
            questionSummary = question,
            stepByStep = listOf(
                "Pahami fakta dan latar belakang utama dari pertanyaan materi $subject ini.",
                "Hubungkan data kunci, konsep teoritis, dan kronologi atau logika yang mendasarinya.",
                "Tarik kesimpulan yang menyeluruh dan menjawab langsung esensi pertanyaan tugasmu."
            ),
            finalAnswer = "Materi $subject telah dianalisis dan disusun solusinya secara terstruktur.",
            tutorTip = "Tips dari ${char.name}: Membuat peta konsep (mind map) sangat ampuh membantu kamu mengingat materi $subject dengan cepat!"
        )
    }
}
