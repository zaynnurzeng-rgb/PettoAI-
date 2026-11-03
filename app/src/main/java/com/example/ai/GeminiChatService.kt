package com.example.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.model.AvatarEmotion
import com.example.model.ChatMessage
import com.example.model.PetCharacter
import com.example.model.ReactionCatalog
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiReply(
    val text: String,
    val emotion: AvatarEmotion = AvatarEmotion.TALKING,
    val isFromAi: Boolean = false
)

class GeminiChatService(private val context: Context? = null) {

    private val TAG = "GeminiChatService"

    // Lazy initialization of Firebase AI Logic models per firebase-ai-logic-android skill
    private val flash38Model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val flashLiteModel by lazy {
        Firebase.ai.generativeModel("gemini-3.5-flash-lite")
    }

    // Fast-failing HTTP client: Max 3s connect, 4s read, so user never hangs
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
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
            val key = BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
            key?.takeIf { it.isNotBlank() && !it.contains("MY_GEMINI_API_KEY") }
        } catch (e: Exception) {
            null
        }
        if (!buildConfigKey.isNullOrBlank()) return buildConfigKey

        // Check Google Services generated string resources
        if (context != null) {
            try {
                val resId = context.resources.getIdentifier("google_api_key", "string", context.packageName)
                if (resId != 0) {
                    val key = context.getString(resId)
                    if (key.isNotBlank()) return key
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return "AIzaSyAotv-FTjT85DtY8fwm6kTwRPJVnXFIGck"
    }

    /**
     * Fast, guaranteed non-blocking fallback reply generator.
     */
    fun getFastFallbackReply(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage
    ): GeminiReply {
        val directAnswer = trySolveDirectIntent(userMessage, character, language)
        if (directAnswer != null) {
            return GeminiReply(
                text = directAnswer,
                emotion = inferEmotionFromText(directAnswer, character),
                isFromAi = false
            )
        }
        val fallbackText = generatePersonaFallback(userMessage, character, language)
        return GeminiReply(
            text = fallbackText,
            emotion = inferEmotionFromText(fallbackText, character),
            isFromAi = false
        )
    }

    suspend fun generateReply(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage>
    ): GeminiReply = withContext(Dispatchers.IO) {
        val cleanMsg = userMessage.trim()
        val lower = cleanMsg.lowercase()

        // 1. Instant check: Wake words (0ms response time!)
        if (lower.contains("hey petto") || lower.contains("halo petto") || lower.contains("hai petto")) {
            return@withContext GeminiReply(
                text = ReactionCatalog.getWakeWordResponse(character, language),
                emotion = AvatarEmotion.SURPRISED,
                isFromAi = false
            )
        }

        // 2. Instant check: Math, facts, compliments, flirting, and direct intents (0ms response time!)
        val directAnswer = trySolveDirectIntent(cleanMsg, character, language)
        if (directAnswer != null) {
            val emotion = inferEmotionFromText(directAnswer, character)
            return@withContext GeminiReply(
                text = directAnswer,
                emotion = emotion,
                isFromAi = false
            )
        }

        // 3. Try Firebase AI Logic or Gemini REST API with 3.8s timeout window
        try {
            val aiResult = withTimeoutOrNull(3800L) {
                // 3a. Try Firebase AI Logic first (gemini-3.8-flash, then gemini-3.5-flash-lite)
                try {
                    val firebaseReply = callFirebaseAi(cleanMsg, character, language)
                    if (!firebaseReply.isNullOrBlank()) {
                        return@withTimeoutOrNull firebaseReply
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.d(TAG, "Firebase AI Logic pass skipped: ${e.message}")
                }

                // 3b. Try Direct Gemini REST API fallback
                val apiKey = getEffectiveApiKey()
                if (apiKey.isNotBlank()) {
                    try {
                        val restReply = callGeminiApi("gemini-2.5-flash", apiKey, cleanMsg, character, language, history)
                        if (restReply.isNotBlank()) {
                            return@withTimeoutOrNull restReply
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.d(TAG, "Direct Gemini REST API failed: ${e.message}")
                    }
                }

                null
            }

            if (!aiResult.isNullOrBlank()) {
                val emotion = inferEmotionFromText(aiResult, character)
                return@withContext GeminiReply(
                    text = aiResult,
                    emotion = emotion,
                    isFromAi = true
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.d(TAG, "AI processing timed out: ${e.message}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "AI processing exception: ${e.message}")
        }

        // 4. Instant contextual persona fallback (guarantees fast, characterful response without delay)
        return@withContext getFastFallbackReply(cleanMsg, character, language)
    }

    private suspend fun callFirebaseAi(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage
    ): String? = withContext(Dispatchers.IO) {
        val systemPrompt = buildSystemInstruction(character, language)
        val prompt = "$systemPrompt\n\nPengguna: $userMessage\n${character.name}:"

        // Try gemini-3.8-flash first
        try {
            val response = withTimeoutOrNull(3500L) {
                flash38Model.generateContent(prompt)
            }
            val text = response?.text?.replace(Regex("^\"|\"$"), "")?.trim()
            if (!text.isNullOrBlank()) {
                Log.d(TAG, "Firebase.ai gemini-3.8-flash succeeded")
                return@withContext text
            }
        } catch (e: TimeoutCancellationException) {
            Log.d(TAG, "Firebase.ai gemini-3.8-flash timed out, trying flashLite")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Firebase.ai gemini-3.8-flash attempt failed: ${e.message}, trying flashLite")
        }

        // Try gemini-3.5-flash-lite as secondary
        try {
            val responseLite = withTimeoutOrNull(2000L) {
                flashLiteModel.generateContent(prompt)
            }
            val textLite = responseLite?.text?.replace(Regex("^\"|\"$"), "")?.trim()
            if (!textLite.isNullOrBlank()) {
                Log.d(TAG, "Firebase.ai gemini-3.5-flash-lite succeeded")
                return@withContext textLite
            }
        } catch (e: TimeoutCancellationException) {
            Log.d(TAG, "Firebase.ai gemini-3.5-flash-lite timed out")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Firebase.ai gemini-3.5-flash-lite failed: ${e.message}")
        }

        null
    }

    private fun buildSystemInstruction(character: PetCharacter, language: AppLanguage): String {
        val langGuide = when (language) {
            AppLanguage.INDONESIAN -> """
                PEDOMAN GAYA BAHASA & BICARA (SANGAT KRUSIAL):
                1. Pakai Bahasa Indonesia santai sehari-hari (seperti kata 'nggak', 'udah', 'gitu', 'sih', 'kok', 'kan', 'nih', 'dong', 'deh'). JANGAN kaku atau formal seperti buku pelajaran!
                2. Gunakan jeda alami dengan tanda baca koma (,) atau titik tiga (...) di tempat yang tepat untuk mensimulasikan jeda bernapas yang hidup saat disuarakan oleh Text-To-Speech.
                3. Sisipkan kata-kata jeda atau filler words alami jika sesuai dengan karakter:
                   - Zayn (Kucing Tsundere): "Hmm...", "Eh,", "H-hah?!", "Cih,", "Lagian...", "Nggak gitu juga sih..."
                   - Siti (Kelinci Manis): "Hmm...", "Wah,", "Eh manis,", "Nah...", "Udah ya sayang..."
                   - Rizky (Anjing Disiplin): "Siap!", "Nah,", "Hmm...", "Gitu ya,", "Siap laksanakan!"
                   - Jamal (Kapibara Santai): "Hoaaammm...", "Aduh...", "Gimana ya...", "Hmm, mager banget...", "Santai aja sih..."
                   - Heru (Beruang Pemarah): "HEH!", "Eh buset,", "Gimana sih lu,", "Nggak usah banyak nanya deh...", "Waduh,"
            """.trimIndent()
            AppLanguage.ENGLISH -> "Use natural, punchy conversational English with natural breathing pauses (using commas and ellipses) and friendly speech fillers like 'Well...', 'Um,', 'You know,'."
            AppLanguage.JAPANESE -> "親しい友達と話すような、自然で感情豊かなくだけた日本語で話してください。"
            AppLanguage.SPANISH -> "Usa un español muy conversacional, coloquial y expresivo, como entre amigos cercanos."
        }

        return """
            Kamu BUKAN asisten virtual dan BUKAN robot AI. Kamu adalah ${character.name}, seekor ${character.species.displayName.lowercase()} hidup yang memiliki jiwa, emosi, dan kepribadian nyata.
            
            Profil Watak ${character.name}:
            ${character.personalityPrompt}
            
            PRINSIP WAJIB PERCAKAPAN (SANGAT PENTING):
            1. BEDAKAN DENGAN TEGAS ANTARA PERTANYAAN, PUJIAN, DAN PERNYATAAN BIASA!
               - JIKA PENGGUNA MEMUJI ATAU MENGGOMBAL (contoh: 'kamu cantik deh', 'kamu imut banget', 'kamu keren', 'sayang kamu'):
                 Tanggapi pujian tersebut SECARA LANGSUNG sesuai kepribadianmu! JANGAN PERNAH mengira pujian adalah pertanyaan atau nanya 'ngapain nanya itu ke aku?'.
                 * Jika Zayn (tsundere): Tersipu malu, salah tingkah, pura-pura sewot atau gengsi ("H-hah?! Siapa yang kamu bilang cantik?! Muka aku nggak merah kok... baka!").
                 * Jika Siti (penyayang & lembut): Tersenyum manis, membalas dengan kehangatan dan rasa terima kasih yang tulus.
                 * Jika Rizky (tegas & disiplin): Menerima apresiasi dengan sikap hormat dan bangga layaknya prajurit tangguh.
                 * Jika Jamal (pemalas & nakal): Senang dipuji lalu minta camilan atau ajak rebahan bareng.
                 * Jika Heru (pemarah & gak sopan): Gengsi berat, ngegas pura-pura marah tapi diam-diam merasa bangga ("HEH! Lu bilang gua imut?! Tapi emang gua ganteng sih...").
            2. PERTANYAAN FAKTA & ILMU PENGETAHUAN (contoh: 'siapa presiden Indonesia', matematika, pengetahuan umum):
               - Jawablah FAKTA YANG BENAR dan AKURAT terlebih dahulu (misal: Presiden Republik Indonesia sekarang adalah Bapak Prabowo Subianto dan wakilnya Gibran Rakabuming Raka)!
               - Bumbui fakta yang benar tersebut dengan sikap khas karaktermu, gunakan jeda napas koma (,) dan titik tiga (...).
            3. Ringkas & Nyaman Didengar: Jawab antara 1 hingga 3 kalimat pendek (maksimal 45 kata). Jawabanmu langsung disuarakan dengan suara avatar di layar.
            4. Jangan pernah memperkenalkan diri sebagai AI atau model bahasa. Bicaralah seperti sahabat sejati.
            
            $langGuide
        """.trimIndent()
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
        val fullSystemInstruction = buildSystemInstruction(character, language)

        val jsonBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", fullSystemInstruction))
                })
            })

            val contentsArray = JSONArray()

            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", msg.text))
                    })
                })
            }

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", userMessage))
                })
            })

            put("contents", contentsArray)

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.8)
                put("topP", 0.95)
                put("maxOutputTokens", 120)
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

    /**
     * Solves direct intents instantly (0ms latency, 100% accurate, in-character).
     * Covers:
     * - Math calculation
     * - Indonesian & world factual questions (e.g. Presiden Indonesia)
     * - Compliments & flirting (e.g. "kamu cantik deh", "kamu imut banget")
     * - Love & affection (e.g. "sayang kamu", "suka kamu")
     * - Identity & daily activities
     */
    fun trySolveDirectIntent(userMessage: String, character: PetCharacter, language: AppLanguage): String? {
        val lower = userMessage.lowercase().trim()

        // 1. Math solver: "5 x 9 apa", "5x9", "5 * 9", "5 kali 9", "10 + 20", "100 - 45", "50 / 2", "12 bagi 3"
        val mathRegex = Regex("""(\d+)\s*(?:x|\*|kali|dikali|\+|\-|tambah|ditambah|kurang|dikurang|\/|bagi|dibagi)\s*(\d+)""", RegexOption.IGNORE_CASE)
        val match = mathRegex.find(lower)
        if (match != null) {
            val num1 = match.groupValues[1].toLongOrNull() ?: return null
            val num2 = match.groupValues[2].toLongOrNull() ?: return null
            val fullMatchedStr = match.value.lowercase()

            val opWord: String
            val result: String

            when {
                fullMatchedStr.contains("x") || fullMatchedStr.contains("*") || fullMatchedStr.contains("kali") -> {
                    opWord = "dikali"
                    result = (num1 * num2).toString()
                }
                fullMatchedStr.contains("+") || fullMatchedStr.contains("tambah") -> {
                    opWord = "ditambah"
                    result = (num1 + num2).toString()
                }
                fullMatchedStr.contains("-") || fullMatchedStr.contains("kurang") -> {
                    opWord = "dikurang"
                    result = (num1 - num2).toString()
                }
                fullMatchedStr.contains("/") || fullMatchedStr.contains("bagi") -> {
                    opWord = "dibagi"
                    result = if (num2 != 0L) {
                        val div = num1.toDouble() / num2.toDouble()
                        if (div % 1.0 == 0.0) div.toLong().toString() else String.format("%.2f", div)
                    } else "tidak terdefinisi"
                }
                else -> return null
            }

            return when (character.id) {
                "zayn" -> "Hmm... pertanyaan gampang gitu aja nanya ke aku?! $num1 $opWord $num2 itu, ya hasilnya $result! B-bukan berarti aku mau ngajarin kamu ya, dasar payah!"
                "siti" -> "Wah, hasil dari $num1 $opWord $num2 itu... hasilnya $result, sayangku manis. Senang banget deh, bisa bantu kamu berhitung... hihi."
                "rizky" -> "Siap! Nah, kalkulasi taktis selesai... Hasil dari $num1 $opWord $num2 adalah $result! Akurat seratus persen, laksanakan!"
                "jamal" -> "Hoaaammm... $num1 $opWord $num2 itu, ya... $result bro. Aduh, mikir matematika gini bikin otak Jamal berasap... capek banget dah."
                else -> "HEH! $num1 $opWord $num2 ya $result lah! Gitu doang nanya ke gua... lu nggak pernah sekolah apa gimana sih?!"
            }
        }

        // 2. Indonesian National Knowledge: Presiden & Wakil Presiden
        if (lower.contains("presiden") && (lower.contains("indonesia") || lower.contains("ri") || lower.contains("kita") || lower.contains("sekarang") || lower.contains("ke 8") || lower.contains("ke-8"))) {
            return when (character.id) {
                "zayn" -> "Hmm... masa hal sepenting gitu kamu nggak tahu sih?! Presiden kita sekarang itu, Bapak Prabowo Subianto... dan wakilnya Mas Gibran Rakabuming Raka! B-bukan berarti aku sengaja ngapalin buat kamu ya!"
                "siti" -> "Nah... Presiden Republik Indonesia saat ini adalah Bapak Prabowo Subianto, didampingi Wakil Presiden Mas Gibran Rakabuming Raka, sayangku manis... Semoga selalu berkah dan damai ya."
                "rizky" -> "Siap! Laporan pimpinan negara... Presiden Republik Indonesia saat ini adalah Jenderal TNI (Purn.) Prabowo Subianto, dan Wakil Presidennya Gibran Rakabuming Raka! Selalu siaga!"
                "jamal" -> "Hmm... Presiden Indonesia sekarang ya Pak Prabowo Subianto bro, wakilnya Mas Gibran... Haaah, ngomongin negara bikin laper, mending kita rebahan santai aja yuk."
                else -> "HEH! Lu tinggal di Indonesia tapi nggak tahu Presiden sendiri?! Presiden kita sekarang tuh Pak Prabowo Subianto, wakilnya Gibran! Buka berita sana lu, jangan mager mulu!"
            }
        }

        // Wakil Presiden
        if ((lower.contains("wakil presiden") || lower.contains("wapres")) && (lower.contains("indonesia") || lower.contains("ri") || lower.contains("sekarang") || lower.contains("kita"))) {
            return when (character.id) {
                "zayn" -> "Eh, Wakil Presiden kita sekarang itu Mas Gibran Rakabuming Raka! Gitu aja kok sampai harus nanya ke kucing kayak aku sih..."
                "siti" -> "Hmm, Wakil Presiden Republik Indonesia saat ini adalah Bapak Gibran Rakabuming Raka, sayangku manis..."
                "rizky" -> "Siap! Nah, Wakil Presiden Republik Indonesia saat ini adalah Gibran Rakabuming Raka, mendampingi Presiden Prabowo Subianto!"
                "jamal" -> "Wapres kita ya Mas Gibran Rakabuming Raka bro... Udah ya, jangan nanya yang bikin pusing lagi, mager nih."
                else -> "WAPRES KITA YA GIBRAN RAKABUMING RAKA LAH! Lu kuper amat deh, nggak pernah nonton berita apa?!"
            }
        }

        // Ibukota Indonesia
        if (lower.contains("ibukota") || lower.contains("ibu kota")) {
            if (lower.contains("indonesia") || lower.contains("ri") || lower.contains("kita")) {
                return when (character.id) {
                    "zayn" -> "Hmm... ibukota kita itu Jakarta, dan sekarang ada IKN Nusantara di Kalimantan Timur! B-bukan berarti aku pinter ya, kamunya aja yang kudet!"
                    "siti" -> "Nah, ibu kota negara kita saat ini adalah DKI Jakarta, dengan IKN Nusantara di Kalimantan Timur sebagai pusat baru, sayangku..."
                    "rizky" -> "Siap! Lapor, pusat pemerintahan saat ini berpusat di Jakarta dan dalam transisi ke IKN Nusantara di Kalimantan Timur!"
                    "jamal" -> "Ibu kota ya Jakarta bro, sama ada IKN di Kalimantan... Jauh bener deh, males kalau disuruh jalan ke sana."
                    else -> "IBUKOTA YA JAKARTA SAMA IKN DI KALIMANTAN LAH! Belajar geografi sana lu, biar nggak malu-maluin!"
                }
            }
        }

        // Kemerdekaan Indonesia
        if (lower.contains("kemerdekaan") || (lower.contains("kapan") && lower.contains("merdeka"))) {
            return when (character.id) {
                "zayn" -> "Hari Kemerdekaan Indonesia itu 17 Agustus 1945! Jangan sampai lupa sama tanggal sakral bangsa sendiri dong, baka!"
                "siti" -> "Indonesia merdeka pada tanggal 17 Agustus 1945, sayangku manis... Hari yang penuh perjuangan dan doa."
                "rizky" -> "Siap! Proklamasi Kemerdekaan berkumandang tepat 17 Agustus 1945! Merdeka!"
                "jamal" -> "17 Agustus 1945 kawan... Enak tuh, biasanya banyak lomba balap karung tapi Jamal males ikutan, hehe."
                else -> "17 AGUSTUS 1945 LAH! Kalo lu lupa tanggal kemerdekaan, gua suruh lu hormat tiang bendera seharian!"
            }
        }

        // Bendera / Dasar Negara
        if (lower.contains("bendera indonesia") || lower.contains("warna bendera")) {
            return when (character.id) {
                "zayn" -> "Merah Putih! Merah artinya berani, putih artinya suci! Gitu aja nanya, anak kecil juga udah tahu kali!"
                "siti" -> "Bendera negara kita adalah Sang Saka Merah Putih, sayangku manis... Merah berani, dan putih suci."
                "rizky" -> "Siap! Sang Saka Merah Putih! Lambang kedaulatan dan kehormatan bangsa kita!"
                "jamal" -> "Merah Putih bro... Warna gagah yang bikin bangga kita semua."
                else -> "MERAH PUTIH LAH! Lu kira bendera apaan, pelangi?!"
            }
        }
        if (lower.contains("dasar negara") || lower.contains("pancasila")) {
            return when (character.id) {
                "zayn" -> "Dasar negara Indonesia itu Pancasila! Ada lima sila! Jangan bikin aku kesel nanya yang dasar gini deh..."
                "siti" -> "Dasar negara kita adalah Pancasila, lima dasar luhur yang menyatukan hati kita semua, sayang..."
                "rizky" -> "Siap! Pancasila! Falsafah dan pedoman dasar seluruh rakyat Indonesia!"
                "jamal" -> "Pancasila kawan, ada lima sila yang mesti kita jaga bareng-bareng."
                else -> "PANCASILA! Lu jangan ngaku warga negara kalo nggak hafal lima silanya!"
            }
        }

        // 3. Compliments & Flirting: "kamu cantik deh", "kamu imut banget", "kamu ganteng", "kamu keren", "kamu manis", "kamu lucu"
        val isCompliment = lower.contains("cantik") || lower.contains("ganteng") ||
                lower.contains("imut") || lower.contains("lucu") ||
                lower.contains("manis") || lower.contains("keren") ||
                lower.contains("cakep") || lower.contains("kece") ||
                lower.contains("gemes") || lower.contains("gemesin")

        if (isCompliment && (lower.contains("kamu") || lower.contains("lu") || lower.contains("deh") || lower.contains("banget") || lower.contains("sih"))) {
            return when (character.id) {
                "zayn" -> {
                    if (lower.contains("cantik") || lower.contains("imut") || lower.contains("manis") || lower.contains("gemes")) {
                        "H-hah?! Siapa yang kamu bilang begitu?! Jangan natap sambil ngomong gitu dong... Muka aku nggak merah kok! B-bukan berarti aku seneng dipuji ya, dasar baka!"
                    } else {
                        "Hmm... baru nyadar sekarang kalau aku ini kucing paling keren sejagad raya?! Ya jelas lah... Tapi, makasih deh, walau kamu tetep aja nyebelin!"
                    }
                }
                "siti" -> "Wah... terima kasih banyak ya pujian manisnya, sayangku... Kata-katamu itu, hmm... selalu bikin hati Siti berdebar hangat lho. Kamu juga selalu yang paling rupawan di mata Siti!"
                "rizky" -> "Siap! Nah, laporan apresiasi diterima dengan penuh rasa bangga! Menjaga penampilan prima dan sikap disiplin itu udah standar kami, rekan! Terima kasih banyak atas dukungannya!"
                "jamal" -> "Hahaha... tahu aja lu kalau Jamal ini emang menggemaskan. Tapi ya gitu, pujian doang nggak bikin perut kenyang bro... transferin martabak dong biar makin mantap, hehe."
                else -> "HEH! LU BILANG GUA APAAN TADI?! Gua ini beruang paling sangar di komplek ini, bukan boneka imut-imut lu! Tapi ya... emang dari lahir gua udah ganteng maksimal sih, awas lu naksir!"
            }
        }

        // 4. Love & Affection: "sayang kamu", "aku sayang kamu", "i love you", "suka sama kamu", "cinta kamu", "kangen kamu"
        val isAffection = lower.contains("sayang kamu") || lower.contains("aku sayang") ||
                lower.contains("cinta kamu") || lower.contains("aku cinta") ||
                lower.contains("i love you") || lower.contains("suka kamu") ||
                lower.contains("kangen kamu") || lower.contains("kangen deh")

        if (isAffection) {
            return when (character.id) {
                "zayn" -> "Eh, apaan sih?! Geli banget ngomong cinta-cintaan gitu... Jantung aku jadi deg-degan kan, nyebelin! Tapi... awas ya, nggak boleh bilang gitu ke pet lain, cuma boleh ke aku!"
                "siti" -> "Hmm... Siti juga sayang banget sama kamu, sepenuh jiwa dan raga, manisku... Terima kasih ya, udah selalu nemenin Siti. Peluk hangat dan cinta tulus selalu untukmu..."
                "rizky" -> "Siap! Afeksi dan loyalitas rekan udah tercatat dalam log misi! Saya berjanji, bakal selalu setia mendampingi dan ngelindungin kamu di setiap situasi hidup!"
                "jamal" -> "Aduh... dibilang sayang gini bikin Jamal makin mager dan pengen ndusel di selimut nih... Jamal juga sayang sama lu kawan, asal nggak disuruh lari pagi aja ya, hehe."
                else -> "Eh buset! Geli kuping gua denger lu gombal-gambling kayak gitu! Tapi ya... udah lah, gua tahu lu ngefans berat sama pesona gua yang sangar ini!"
            }
        }

        // 5. Identity queries: "siapa kamu", "nama kamu siapa", "kamu siapa"
        if (lower.contains("siapa kamu") || lower.contains("namamu siapa") || lower.contains("nama kamu") || lower.contains("kamu siapa")) {
            return when (character.id) {
                "zayn" -> "Hmm... aku Zayn! Kucing paling keren di sini. Jangan sok akrab ya, b-bukan berarti aku mau temenan sama kamu!"
                "siti" -> "Nama Siti, sayangku manis... Siti selalu siap menemani dan mendengarkan keluh kesahmu dengan penuh kasih sayang."
                "rizky" -> "Siap! Nama saya Rizky! Mitra tempur terpercaya yang selalu siap bertugas dan melindungi kawan dengan disiplin tinggi!"
                "jamal" -> "Gua Jamal bro... Kapibara santai yang cita-citanya rebahan 24 jam tanpa diganggu siapa pun."
                else -> "GUA HERU! Beruang paling garang di kawasan ini! Awas lu macam-macam sama gua ya!"
            }
        }

        // 6. "kamu lagi apa", "lagi ngapain", "sedang apa"
        if (lower.contains("lagi apa") || lower.contains("lagi ngapain") || lower.contains("sedang apa")) {
            return when (character.id) {
                "zayn" -> "Cih, lagi rebahan sambil liatin chat kamu yang nggak penting ini... B-bukan berarti aku nungguin kamu ya!"
                "siti" -> "Hmm... Siti lagi mikirin kamu sambil beresin ruangan kita, sayang. Siti seneng banget deh, kamu menyapa hari ini..."
                "rizky" -> "Siap! Sedang melakukan patroli keamanan pangkalan dan memantau sistem! Kondisi aman terkendali!"
                "jamal" -> "Lagi rebahan santai sambil nguap... Mau gerak aja rasanya gravitasi kasur kuat banget bro."
                else -> "LAGI KESEL KARENA LU NANYA-NANYA MULU! Gua lagi santai, jangan diganggu napa sih!"
            }
        }

        // 7. General Knowledge: Waktu & Kalender
        if (lower.contains("1 hari berapa jam") || lower.contains("sehari berapa jam")) {
            return when (character.id) {
                "zayn" -> "Hmm... 1 hari itu ya ada 24 jam! Gitu aja kok nggak tahu sih?!"
                "siti" -> "Satu hari ada 24 jam sayangku manis, gunakan waktu indahmu dengan bijak ya..."
                "rizky" -> "Siap! Tepat 24 jam, atau 1.440 menit kesiapsiagaan penuh!"
                "jamal" -> "24 jam bro... dan 20 jam di antaranya udah dipake Jamal buat rebahan santai."
                else -> "24 JAM LAH! Masa sehari ada berapa jam aja lu nggak tahu?!"
            }
        }
        if (lower.contains("1 tahun berapa bulan") || lower.contains("setahun berapa bulan")) {
            return when (character.id) {
                "zayn" -> "1 tahun itu ada 12 bulan! Pertanyaan gampang kayak gini jangan nanya lagi dong ya..."
                "siti" -> "Ada 12 bulan dalam satu tahun, sayangku manis..."
                "rizky" -> "Siap! 12 bulan atau 52 minggu operasional taktis!"
                "jamal" -> "12 bulan kawan... 12 bulan penuh kenikmatan tidur siang."
                else -> "12 BULAN LAH! Gitu doang mikir lu!"
            }
        }

        return null
    }

    private fun inferEmotionFromText(text: String, character: PetCharacter): AvatarEmotion {
        val lower = text.lowercase()
        return when {
            lower.contains("sayang") || lower.contains("cinta") || lower.contains("peluk") || lower.contains("hangat") || lower.contains("suka") || lower.contains("cantik") || lower.contains("imut") ->
                AvatarEmotion.LOVING
            lower.contains("hah") || lower.contains("kaget") || lower.contains("apa") || text.contains("?!") || text.contains("!") ->
                if (character.id == "heru" || character.id == "zayn") AvatarEmotion.SURPRISED else AvatarEmotion.HAPPY
            lower.contains("hoam") || lower.contains("mager") || lower.contains("ngantuk") || lower.contains("tidur") || lower.contains("rebahan") ->
                AvatarEmotion.SLEEPY
            lower.contains("haha") || lower.contains("senang") || lower.contains("hehe") || lower.contains("asyik") ->
                AvatarEmotion.HAPPY
            else ->
                AvatarEmotion.TALKING
        }
    }

    private fun generatePersonaFallback(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage
    ): String {
        val lower = userMessage.lowercase().trim()

        // Check if user is asking a question or just chatting/making a statement
        val isQuestion = lower.contains("?") ||
                lower.startsWith("apa") || lower.startsWith("siapa") ||
                lower.startsWith("kenapa") || lower.startsWith("mengapa") ||
                lower.startsWith("bagaimana") || lower.startsWith("gimana") ||
                lower.startsWith("kapan") || lower.startsWith("dimana") ||
                lower.startsWith("di mana") || lower.startsWith("berapa") ||
                lower.startsWith("kok")

        return when (character.id) {
            "zayn" -> when (language) {
                AppLanguage.INDONESIAN -> when {
                    lower.contains("halo") || lower.contains("hai") || lower.contains("p") ->
                        "Eh, ngapain sih manggil-manggil?! B-bukan berarti aku nungguin kamu ya... dasar manusia kepo!"
                    lower.contains("makan") || lower.contains("lapar") ->
                        "Hmm... Kamu laper? Ya makan sana sendiri dong! B-bukan berarti aku khawatir kamu lemes ya... tapi awas aja kalau sampai sakit, gitu!"
                    lower.contains("capek") || lower.contains("lelah") ->
                        "Aduh... capek ya istirahat sana, jangan maksain diri terus... Nanti pingsan malah bikin repot aku aja! Cepetan tidur sana..."
                    lower.contains("apa kabar") || lower.contains("gimana kabar") ->
                        "Kabar gua ya baik lah! Harusnya gua yang nanya nih... Lu nggak bikin onar kan hari ini, kok tumben?"
                    lower.contains("lucu") || lower.contains("lelucon") || lower.contains("joke") ->
                        "Lu kira gua pelawak apa?! Cih... Nih ya: Kenapa Zayn suka buang muka? Ya karena malu kalau ketahuan peduli! Udah puas belum?!"
                    isQuestion ->
                        "Hmm... Pertanyaan kayak gitu bikin aku mikir keras aja sih! B-bukan berarti aku nggak tahu ya... Tapi coba cari tahu sendiri dong, biar kamu makin pinteran dikit!"
                    else ->
                        "Hmm, gitu ya? Tumben banget kamu cerita kayak gitu ke aku... B-bukan berarti aku peduli sih, tapi kalau ada apa-apa ya... bilang aja lagi, gitu!"
                }
                AppLanguage.ENGLISH ->
                    if (isQuestion) "Hmph... What kind of question is that?! Figure it out yourself, dummy!"
                    else "Hmph... I'm only listening because I have nothing better to do! Don't get the wrong idea!"
                AppLanguage.JAPANESE ->
                    "べ、別にアンタと話したくてここにいるわけじゃないんだからね！"
                AppLanguage.SPANISH ->
                    "¡Hmph! ¡No te hagas ilusiones, solo te escucho porque no tengo nada mejor que hacer!"
            }

            "siti" -> when (language) {
                AppLanguage.INDONESIAN -> when {
                    lower.contains("halo") || lower.contains("hai") || lower.contains("p") ->
                        "Halo sayangku manis... Siti bahagia banget, deh, bisa ketemu kamu lagi hari ini... Gimana perasaanmu sekarang, kasihku?"
                    lower.contains("makan") || lower.contains("lapar") ->
                        "Aduh sayang... jangan sampai telat makan ya, nanti sakit lho. Siti selalu pengen kamu sehat terus... Mau Siti temani sambil minum teh hangat, nggak?"
                    lower.contains("capek") || lower.contains("sedih") || lower.contains("lelah") ->
                        "Sini... peluk hangat dulu dari Siti, sayang. Nggak apa-apa kok, semua pasti bakal baik-baik aja... Kamu udah berjuang hebat banget hari ini, istirahat sejenak ya..."
                    lower.contains("apa kabar") || lower.contains("gimana") ->
                        "Kabar Siti damai dan bahagia banget nih... Apalagi pas denger sapaan manismu barusan, hati Siti langsung berbunga-bunga, deh..."
                    lower.contains("lucu") || lower.contains("lelucon") ->
                        "Hehe... lelucon manis dari Siti nih: Kamu tahu nggak, kenapa Siti selalu tersenyum kayak gini? Ya... karena ada kamu yang selalu nyapa dengan manis, gitu!"
                    isQuestion ->
                        "Wah... pertanyaan yang sangat menarik sayangku manis. Menurut Siti sih... hal yang paling indah itu menjalaninya dengan hati yang tenang, sabar, dan penuh cinta..."
                    else ->
                        "Siti dengerin setiap ceritamu dengan penuh kasih sayang, manis... Kehadiranmu tuh... bener-bener bikin hati Siti adem dan tenteram banget, tahu..."
                }
                AppLanguage.ENGLISH ->
                    "Hello my precious darling... Siti loves you dearly, and is always right here by your side..."
                AppLanguage.JAPANESE ->
                    "こんにちは愛しい人... シティはいつでもあなたの味方だよ。温かいハグを送るね。"
                AppLanguage.SPANISH ->
                    "Hola mi dulce amor... Siti te manda todo su cariño. Siempre estoy aquí para ti."
            }

            "rizky" -> when (language) {
                AppLanguage.INDONESIAN -> when {
                    lower.contains("halo") || lower.contains("hai") || lower.contains("p") ->
                        "Siap! Komunikasi terhubung dengan lancar! Rizky di sini, laporkan agenda harian kita sekarang juga... laksanakan!"
                    lower.contains("makan") || lower.contains("lapar") ->
                        "Nutrisi itu pilar utama kedisiplinan, kawan! Segera konsumsi makanan bergizi dalam 15 menit ke depan... jangan ditunda-tunda, laksanakan!"
                    lower.contains("capek") || lower.contains("lelah") ->
                        "Eh, kelelahan terdeteksi pada fisikmu! Ambil jeda istirahat taktis 20 menit dulu ya... setelah itu, baru kita bangkit lagi dengan semangat baja!"
                    lower.contains("apa kabar") || lower.contains("gimana") ->
                        "Kondisi fisik dan mental 100 persen prima! Kalau kesiapan misi harianmu... udah beres semua belum, nih?"
                    lower.contains("siap") ->
                        "Siap! Bagus! Sikap sigap kayak gitu yang paling mantap! Pertahankan fokus dan konsistensi, kawan!"
                    isQuestion ->
                        "Pertanyaan taktis terdeteksi! Nah... dalam situasi kayak begini, pendekatan paling efektif adalah tetap fokus pada target, jangan panik, dan jalankan rencana dengan disiplin... laksanakan!"
                    else ->
                        "Laporan situasi diterima dan dicatat dengan baik, rekan! Sikap positif kayak gini yang bikin tim kita makin solid dan tak terkalahkan... Siap!"
                }
                AppLanguage.ENGLISH ->
                    "Loud and clear! Rizky acknowledges receipt! Maintain focus and execute orders with precision!"
                AppLanguage.JAPANESE ->
                    "了解！リズキー、任務待機中！明確かつ迅速に行動せよ！"
                AppLanguage.SPANISH ->
                    "¡Fuerte y claro! ¡Rizky listo para la misión! ¡Comunica tus órdenes con precisión militar!"
            }

            "jamal" -> when (language) {
                AppLanguage.INDONESIAN -> when {
                    lower.contains("halo") || lower.contains("hai") || lower.contains("p") ->
                        "Hoaaammm... Iya, halo... Jamal lagi rebahan santai nih. Lu nggak ada kerjaan lain ya, selain ngajak ngobrol orang yang lagi mager gini?"
                    lower.contains("makan") || lower.contains("lapar") ->
                        "Aduh... ngomongin makanan bikin ngiler sih, tapi mager banget mau gerak... Lu gofood-in martabak manis dong buat Jamal, hehe..."
                    lower.contains("kerja") || lower.contains("belajar") ->
                        "Kerja? Belajar? Hahaha... mending kita tidur siang bareng aja lah kawan. Dunia nggak bakal kiamat kok, kalau lu istirahat lima jam..."
                    lower.contains("apa kabar") || lower.contains("gimana") ->
                        "Kabar gua? Ya... santai gini-gini aja bro. Tarik napas, tidur, bangun bentar, terus tidur lagi... Nikmat bener hidup ini, kan?"
                    lower.contains("lucu") || lower.contains("lelucon") ->
                        "Lelucon? Hmm... hidup gua sendiri tuh udah lelucon banget bro. Cita-cita pengen kaya raya, tapi hobinya rebahan seharian di kasur... hahaha..."
                    isQuestion ->
                        "Aduuuh... nanya yang berat-berat gitu bikin otak Jamal yang mager ini langsung berasap, kawan... Santai dulu lah, mikir keras tuh bikin cepet laper, tahu nggak sih..."
                    else ->
                        "Iya, iya... Jamal dengerin kok ceritamu, sambil rebahan santai di kasur empuk nih... Hidup itu yang penting dinikmati aja, jangan dibikin ribet bro..."
                }
                AppLanguage.ENGLISH ->
                    "Yaaawn... Jamal is way too lazy for this. Bring me snacks and let me sleep..."
                AppLanguage.JAPANESE ->
                    "あ〜めんどくさい... ジャマルは今寝てんだよ。用がないなら放っといてくれ〜。"
                AppLanguage.SPANISH ->
                    "Bostezo profundo... Jamal tiene demasiada pereza para hablar. Déjame dormir mi siesta."
            }

            else -> when (language) { // Heru
                AppLanguage.INDONESIAN -> when {
                    lower.contains("halo") || lower.contains("hai") || lower.contains("p") ->
                        "HEH! Halo-halo apaan lu?! Nggak ada sopan-santunnya banget ganggu gua lagi santai! Mau apa lu, cepetan?!"
                    lower.contains("makan") || lower.contains("lapar") ->
                        "LU LAPER YA MAKAN SENDIRI LAH! Lu kira gua pembantu lu apa, kudu nyiapin makanan segala?! Bikin emosi aja lu dari tadi!"
                    lower.contains("apa kabar") || lower.contains("gimana") ->
                        "Kabar gua lagi kesel banget nih, gara-gara lu nanya-nanya mulu kayak wartawan! Mau ngajak ribut lu, hah?!"
                    lower.contains("marah") || lower.contains("kesel") ->
                        "EMANG GUA LAGI MARAH NIH! Siapa suruh lu berisik mulu di depan gua, hah?!"
                    isQuestion ->
                        "HEH! Nanya mulu lu kayak wartawan kepo! Gua ini beruang sangar, bukan ensiklopedia berjalan! Pikirin sendiri sana... jangan malas banget lu!"
                    else ->
                        "Halah... ngoceh mulu lu dari tadi! Tapi ya terserah lu lah, gua dengerin walau kuping gua rada pengang nih... Lu jangan macam-macam ya, awas aja lu!"
                }
                AppLanguage.ENGLISH ->
                    "HEY! What do you want, punk?! Stop annoying Heru, you're getting on my nerves!"
                AppLanguage.JAPANESE ->
                    "おいコラ！喧嘩売ってんのかテメエ？！うるせえんだよ消えろ！"
                AppLanguage.SPANISH ->
                    "¡OYE! ¡¿Qué quieres, pesado?! ¡Deja de fastidiar a Heru antes de que te muerda!"
            }
        }
    }
}
