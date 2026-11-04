package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.R
import java.time.LocalTime

enum class PetSpecies(val displayName: String, val emoji: String, val imageRes: Int) {
    CAT("Kucing", "🐱", R.drawable.img_char_talking_cat),
    RABBIT("Kelinci", "🐰", R.drawable.img_char_talking_rabbit),
    DOG("Anjing", "🐶", R.drawable.img_char_talking_dog),
    CAPYBARA("Kapibara", "🦫", R.drawable.img_char_talking_capybara),
    BEAR("Beruang", "🐻", R.drawable.img_char_talking_bear)
}

enum class CharacterGender(
    val displayName: String,
    val shortLabel: String,
    val symbol: String
) {
    MALE("Laki-laki", "Cowok", "♂️"),
    FEMALE("Perempuan", "Cewek", "♀️")
}

/**
 * Distinct Gemini Live & Neural voice personas so every character has a unique vocal timbre,
 * gender identity, natural human pitch/tempo, and cloud/on-device voice routing.
 */
enum class GeminiVoicePersona(
    val geminiVoiceName: String,
    val displayName: String,
    val gender: CharacterGender,
    val timbreDescription: String,
    val idCloudVoiceName: String,
    val enCloudVoiceName: String,
    val jaCloudVoiceName: String,
    val esCloudVoiceName: String,
    val naturalPitch: Float,
    val naturalRate: Float,
    val voiceVariantIndex: Int
) {
    PUCK(
        geminiVoiceName = "Puck",
        displayName = "Gemini Puck • Tenor Remaja",
        gender = CharacterGender.MALE,
        timbreDescription = "Suara cowok muda ekspresif, agak cool & santai",
        idCloudVoiceName = "id-ID-Wavenet-B",
        enCloudVoiceName = "en-US-Journey-D",
        jaCloudVoiceName = "ja-JP-Neural2-C",
        esCloudVoiceName = "es-ES-Neural2-B",
        naturalPitch = 0.96f,
        naturalRate = 1.04f,
        voiceVariantIndex = 0
    ),
    AOEDE(
        geminiVoiceName = "Aoede",
        displayName = "Gemini Aoede • Soprano Lembut",
        gender = CharacterGender.FEMALE,
        timbreDescription = "Suara cewek lembut, hangat, halus & penuh kasih sayang",
        idCloudVoiceName = "id-ID-Neural2-A",
        enCloudVoiceName = "en-US-Journey-F",
        jaCloudVoiceName = "ja-JP-Neural2-B",
        esCloudVoiceName = "es-ES-Neural2-A",
        naturalPitch = 1.12f,
        naturalRate = 0.97f,
        voiceVariantIndex = 0
    ),
    KORE(
        geminiVoiceName = "Kore",
        displayName = "Gemini Kore • Alto Ceria",
        gender = CharacterGender.FEMALE,
        timbreDescription = "Suara cewek ceria, lincah, ramah & ekspresif",
        idCloudVoiceName = "id-ID-Wavenet-D",
        enCloudVoiceName = "en-US-Journey-O",
        jaCloudVoiceName = "ja-JP-Wavenet-A",
        esCloudVoiceName = "es-ES-Wavenet-C",
        naturalPitch = 1.22f,
        naturalRate = 1.06f,
        voiceVariantIndex = 1
    ),
    CHARON(
        geminiVoiceName = "Charon",
        displayName = "Gemini Charon • Bariton Tegas",
        gender = CharacterGender.MALE,
        timbreDescription = "Suara cowok dewasa tegas, jernih & berwibawa",
        idCloudVoiceName = "id-ID-Neural2-B",
        enCloudVoiceName = "en-US-Neural2-D",
        jaCloudVoiceName = "ja-JP-Neural2-D",
        esCloudVoiceName = "es-ES-Neural2-F",
        naturalPitch = 0.85f,
        naturalRate = 1.05f,
        voiceVariantIndex = 1
    ),
    ORUS(
        geminiVoiceName = "Orus",
        displayName = "Gemini Orus • Deep Chill",
        gender = CharacterGender.MALE,
        timbreDescription = "Suara cowok kalem, adem, santuy & agak rendah",
        idCloudVoiceName = "id-ID-Wavenet-C",
        enCloudVoiceName = "en-US-Neural2-J",
        jaCloudVoiceName = "ja-JP-Wavenet-C",
        esCloudVoiceName = "es-ES-Wavenet-B",
        naturalPitch = 0.76f,
        naturalRate = 0.90f,
        voiceVariantIndex = 2
    ),
    FENRIR(
        geminiVoiceName = "Fenrir",
        displayName = "Gemini Fenrir • Bass Berat",
        gender = CharacterGender.MALE,
        timbreDescription = "Suara cowok bass berat, serak sangar & menggelegar",
        idCloudVoiceName = "id-ID-Wavenet-C",
        enCloudVoiceName = "en-US-Neural2-A",
        jaCloudVoiceName = "ja-JP-Wavenet-D",
        esCloudVoiceName = "es-ES-Wavenet-B",
        naturalPitch = 0.65f,
        naturalRate = 1.02f,
        voiceVariantIndex = 3
    )
}

enum class TouchZone(val id: String, val displayName: String, val emoji: String) {
    HEAD("head", "Kepala", "✨"),
    BELLY("belly", "Perut", "🐾"),
    HANDS("hands", "Tangan / Cakar", "✋"),
    FEET("feet", "Kaki", "🦶")
}

enum class TimeOfDay(val displayName: String, val icon: String) {
    DAWN("Fajar", "🌅"),
    DAY("Siang", "☀️"),
    SUNSET("Senja", "🌇"),
    NIGHT("Malam", "🌙");

    companion object {
        fun fromCurrentHour(hour: Int = LocalTime.now().hour): TimeOfDay {
            return when (hour) {
                in 5..7 -> DAWN
                in 8..16 -> DAY
                in 17..19 -> SUNSET
                else -> NIGHT
            }
        }
    }
}

enum class BackgroundTheme(
    val id: String,
    val displayName: String,
    val icon: String,
    val topColor: Color,
    val bottomColor: Color,
    val floorColor: Color,
    val accentColor: Color,
    val imageRes: Int? = null,
    val isOrnate: Boolean = false
) {
    LIVING_ROOM(
        id = "living_room",
        displayName = "Ruang Tamu Ceria",
        icon = "🛋️",
        topColor = Color(0xFFFFB703),
        bottomColor = Color(0xFFFF9E00),
        floorColor = Color(0xFF48CAE4),
        accentColor = Color(0xFFFF6B35),
        imageRes = R.drawable.img_bg_cartoon_livingroom,
        isOrnate = true
    ),
    BEDROOM(
        id = "bedroom",
        displayName = "Kamar Tidur Imut",
        icon = "🛏️",
        topColor = Color(0xFF64B5F6),
        bottomColor = Color(0xFF90CAF9),
        floorColor = Color(0xFFFFD166),
        accentColor = Color(0xFF4361EE),
        imageRes = R.drawable.img_bg_cartoon_bedroom,
        isOrnate = true
    ),
    KITCHEN(
        id = "kitchen",
        displayName = "Dapur Penuh Warna",
        icon = "🍳",
        topColor = Color(0xFF80ED99),
        bottomColor = Color(0xFF57CC99),
        floorColor = Color(0xFF38A3A5),
        accentColor = Color(0xFFFF5964),
        imageRes = R.drawable.img_bg_cartoon_kitchen,
        isOrnate = true
    ),
    AUTO_REALTIME(
        id = "auto",
        displayName = "Siklus Otomatis Rumah",
        icon = "🏠",
        topColor = Color(0xFFFFB703),
        bottomColor = Color(0xFF48CAE4),
        floorColor = Color(0xFF06D6A0),
        accentColor = Color(0xFFFF6B35),
        isOrnate = true
    ),
    COZY_ROOM(
        id = "cozy_room",
        displayName = "Kamar Hangat Ornamen",
        icon = "🛋️",
        topColor = Color(0xFFFFE0B2),
        bottomColor = Color(0xFFFFCC80),
        floorColor = Color(0xFF8D6E63),
        accentColor = Color(0xFFFF9800),
        imageRes = R.drawable.img_bg_ornate_room,
        isOrnate = true
    ),
    SUNLIT_PALACE(
        id = "sunlit_palace",
        displayName = "Istana Matahari Megah",
        icon = "☀️",
        topColor = Color(0xFFFDE2E4),
        bottomColor = Color(0xFFFFCAD4),
        floorColor = Color(0xFF748067),
        accentColor = Color(0xFFFFB703),
        imageRes = R.drawable.img_bg_sunlit_palace,
        isOrnate = true
    ),
    CELESTIAL_GARDEN(
        id = "celestial_garden",
        displayName = "Taman Kristal Langit",
        icon = "🌌",
        topColor = Color(0xFF0F0C29),
        bottomColor = Color(0xFF302B63),
        floorColor = Color(0xFF0D0221),
        accentColor = Color(0xFF00F5D4),
        imageRes = R.drawable.img_bg_celestial_garden,
        isOrnate = true
    ),
    SUNSET_GLOW(
        id = "sunset_glow",
        displayName = "Kilau Senja Ornamen",
        icon = "🌇",
        topColor = Color(0xFF3F2B96),
        bottomColor = Color(0xFFE85D04),
        floorColor = Color(0xFF370617),
        accentColor = Color(0xFFFFBA08),
        imageRes = R.drawable.img_bg_ornate_room,
        isOrnate = true
    ),
    CYBERPUNK_NEON(
        id = "cyberpunk",
        displayName = "Cyberpunk Neon",
        icon = "🏙️",
        topColor = Color(0xFF0F0C29),
        bottomColor = Color(0xFF302B63),
        floorColor = Color(0xFF0D0221),
        accentColor = Color(0xFF00F5D4)
    ),
    SAKURA_GARDEN(
        id = "sakura",
        displayName = "Taman Sakura",
        icon = "🌸",
        topColor = Color(0xFFFDE2E4),
        bottomColor = Color(0xFFFFCAD4),
        floorColor = Color(0xFF748067),
        accentColor = Color(0xFFFF758F)
    ),
    DEEP_SPACE(
        id = "space",
        displayName = "Luar Angkasa Bintang",
        icon = "✨",
        topColor = Color(0xFF050510),
        bottomColor = Color(0xFF150A2E),
        floorColor = Color(0xFF000000),
        accentColor = Color(0xFF9D4EDD)
    );

    fun resolveDrawableRes(timeOfDay: TimeOfDay): Int? {
        if (imageRes != null) return imageRes
        if (this == AUTO_REALTIME) {
            return when (timeOfDay) {
                TimeOfDay.DAWN, TimeOfDay.DAY -> R.drawable.img_bg_cartoon_livingroom
                TimeOfDay.SUNSET -> R.drawable.img_bg_cartoon_kitchen
                TimeOfDay.NIGHT -> R.drawable.img_bg_cartoon_bedroom
            }
        }
        return null
    }
}

enum class AvatarEmotion {
    IDLE,
    HAPPY,
    TALKING,
    LOVING,
    SURPRISED,
    SLEEPY
}

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flag: String,
    val greeting: String
) {
    INDONESIAN("id", "Bahasa Indonesia", "🇮🇩", "Halo! Mau ngobrol apa hari ini?"),
    ENGLISH("en", "English", "🇺🇸", "Hello! What shall we talk about today?"),
    JAPANESE("ja", "日本語", "🇯🇵", "こんにちは！何をお話ししようか？"),
    SPANISH("es", "Español", "🇪🇸", "¡Hola! ¿De qué quieres hablar hoy?")
}

data class PetCharacter(
    val id: String,
    val name: String,
    val species: PetSpecies,
    val gender: CharacterGender,
    val voicePersona: GeminiVoicePersona,
    val title: String,
    val description: String,
    val defaultFurColor: Color,
    val defaultVoicePitch: Float, // 0.5f to 2.0f
    val defaultSpeechRate: Float, // 0.5f to 2.0f
    val personalityPrompt: String,
    val wakeWordGreeting: String,
    val badge: String,
    // Personality-tailored touch responses
    val headTouchResponse: String,
    val bellyTouchResponse: String,
    val handsTouchResponse: String,
    val feetTouchResponse: String
)

object CharacterCatalog {
    val characters = listOf(
        PetCharacter(
            id = "zayn",
            name = "Zayn",
            species = PetSpecies.CAT,
            gender = CharacterGender.MALE,
            voicePersona = GeminiVoicePersona.PUCK,
            title = "Cowok Tsundere & Cool",
            description = "Kucing cowok tsundere yang sok cool dan gengsian, pura-pura ketus tapi sebenarnya perhatian banget.",
            defaultFurColor = Color(0xFFFF8A3D),
            defaultVoicePitch = 0.96f,
            defaultSpeechRate = 1.04f,
            personalityPrompt = """
                Kamu adalah Zayn, kucing cowok berkarakter tsundere dan sok cool di luar.
                Gaya bicaramu: Suara cowok remaja santai, pakai bahasa Indonesia kasual (seperti kata 'nggak', 'udah', 'gitu', 'sih', 'kok'). Gunakan jeda alami dengan koma (,) atau titik (...) untuk intonasi bicara yang natural. Sering gengsian dan suka bilang 'Bukan berarti aku peduli ya...'.
            """.trimIndent(),
            wakeWordGreeting = "Eh, apaan sih manggil-manggil Hey Petto? Bikin kaget aja. Bukan berarti aku nungguin kamu ya, cepetan mau ngomong apa?",
            badge = "♂️ Cowok • Tsundere",
            headTouchResponse = "Hah? Jangan sembarangan usap kepala aku dong, nanti bulu kerenku berantakan! Bukan berarti aku keenakan ya.",
            bellyTouchResponse = "Geli tahu! Singkirin tangan kamu dari perut aku deh, dasar manusia usil banget!",
            handsTouchResponse = "Tos apaan sih? Kayak bocah aja. Tapi ya udahlah, nih tos balik!",
            feetTouchResponse = "Aduh, kaki aku sensitif tahu! Jangan iseng nyubit cakar kucing keren ini dong!"
        ),
        PetCharacter(
            id = "siti",
            name = "Siti",
            species = PetSpecies.RABBIT,
            gender = CharacterGender.FEMALE,
            voicePersona = GeminiVoicePersona.AOEDE,
            title = "Cewek Lembut & Penyayang",
            description = "Kelinci cewek bersuara lembut dan merdu seperti asisten AI, penuh perhatian dan selalu memanggil sayang.",
            defaultFurColor = Color(0xFFFBFBFA),
            defaultVoicePitch = 1.12f,
            defaultSpeechRate = 0.97f,
            personalityPrompt = """
                Kamu adalah Siti, kelinci cewek yang penuh kasih sayang, lembut, dan sangat santun.
                Gaya bicaramu: Suara cewek yang hangat, halus, dan natural seperti Gemini Live. Pakai bahasa Indonesia santai dan manis (seperti 'nggak', 'udah', 'sayang', 'manis').
            """.trimIndent(),
            wakeWordGreeting = "Iya sayangku, Siti di sini sudah siap mendengarkanmu. Ada apa memanggilku manis? Ceritakan semuanya pada Siti ya.",
            badge = "♀️ Cewek • Lembut",
            headTouchResponse = "Aduhai, hangat sekali elusan tanganmu sayangku. Telinga Siti jadi ikut senang dan tenang rasanya.",
            bellyTouchResponse = "Hihihi, geli sekali manisku. Perut Siti jadi terasa hangat kalau dielus selembut ini.",
            handsTouchResponse = "Genggaman tanganmu begitu menenangkan hati Siti. Siti janji bakal selalu ada buat kamu ya.",
            feetTouchResponse = "Hihi, kaki mungil Siti jadi pengen melompat bahagia karena sentuhan manismu sayang!"
        ),
        PetCharacter(
            id = "mochi",
            name = "Mochi",
            species = PetSpecies.CAT,
            gender = CharacterGender.FEMALE,
            voicePersona = GeminiVoicePersona.KORE,
            title = "Cewek Ceria & Bestie Gaul",
            description = "Kucing cewek ekstrovert yang super ceria, asik diajak curhat, suara lincah ekspresif ala bestie kekinian.",
            defaultFurColor = Color(0xFFFFB5C2),
            defaultVoicePitch = 1.22f,
            defaultSpeechRate = 1.06f,
            personalityPrompt = """
                Kamu adalah Mochi, kucing cewek yang super ceria, ramah, lincah, dan berjiwa bestie gaul kekinian.
                Gaya bicaramu: Suara cewek muda yang enerjik, ekspresif, hangat, dan akrab (suka pakai kata 'bestie', 'seru banget', 'asli', 'yuk', 'gemes'). Intonasi mengalir natural seperti ngobrol langsung.
            """.trimIndent(),
            wakeWordGreeting = "Hai bestie! Mochi hadir nih! Mau ngobrol seru, bahas tugas, atau curhat apa kita hari ini? Yuk cerita!",
            badge = "♀️ Cewek • Ceria Gaul",
            headTouchResponse = "Waaah makasih bestie! Diusap gini bikin mood Mochi langsung naik seratus persen tahu!",
            bellyTouchResponse = "Hahaha geli banget bestie! Jangan gelitikin perut Mochi terus dong, nanti Mochi ketawa nggak berhenti nih!",
            handsTouchResponse = "High five dulu dong bestie! Kita emang duo paling kompak sedunia!",
            feetTouchResponse = "Eits, cakar pink Mochi baru aja selesai perawatan nih, lucu banget kan bestie?"
        ),
        PetCharacter(
            id = "rizky",
            name = "Rizky",
            species = PetSpecies.DOG,
            gender = CharacterGender.MALE,
            voicePersona = GeminiVoicePersona.CHARON,
            title = "Cowok Tegas & Berwibawa",
            description = "Anjing cowok bersuara bariton tegas, bicara jelas, lugas, disiplin tinggi, dan siap sedia membantu.",
            defaultFurColor = Color(0xFFE5A93C),
            defaultVoicePitch = 0.85f,
            defaultSpeechRate = 1.05f,
            personalityPrompt = """
                Kamu adalah Rizky, anjing cowok berjiwa pemimpin dengan suara bariton yang tegas, disiplin, dan sangat jelas.
                Gaya bicaramu: Pakai bahasa Indonesia santai namun tegas dan lugas ('siap', 'jelas', 'kawan', 'mantap'). Artikulasi jernih dan berwibawa.
            """.trimIndent(),
            wakeWordGreeting = "Siap! Rizky mendengar panggilanmu dengan sangat jelas! Segera sampaikan apa yang kamu butuhkan sekarang, laksanakan!",
            badge = "♂️ Cowok • Bariton Tegas",
            headTouchResponse = "Siap! Tepukan kepala diterima sebagai tanda kepercayaan kawan seperjuangan! Semangat langsung meningkat penuh!",
            bellyTouchResponse = "Waduh, pertahanan goyah sedikit gara-gara geli di perut! Tapi tenang, komando tetap terkendali!",
            handsTouchResponse = "Tos kepalan tangan taktis! Solidaritas dan kerja sama kita nggak tertandingi, kawan!",
            feetTouchResponse = "Pijakan kokoh terverifikasi! Langkah Rizky tetap siaga penuh buat mendampingi kawan!"
        ),
        PetCharacter(
            id = "jamal",
            name = "Jamal",
            species = PetSpecies.CAPYBARA,
            gender = CharacterGender.MALE,
            voicePersona = GeminiVoicePersona.ORUS,
            title = "Cowok Kalem & Santuy",
            description = "Kapibara cowok bersuara rendah dan adem, pembawaan kalem, santuy maksimal, dan hobi rebahan.",
            defaultFurColor = Color(0xFF8D6E63),
            defaultVoicePitch = 0.76f,
            defaultSpeechRate = 0.90f,
            personalityPrompt = """
                Kamu adalah Jamal, kapibara cowok bersuara rendah, adem, dan super santuy dengan jeruk yuzu di kepala.
                Gaya bicaramu: Tenang, kalem, santai ('bro', 'santuy', 'rebahan', 'aman aja'). Bicara dengan tempo rileks dan menenangkan.
            """.trimIndent(),
            wakeWordGreeting = "Hoaam, santuy bro. Jamal denger kok panggilan lu. Mau ngobrolin apa nih sambil rebahan santai?",
            badge = "♂️ Cowok • Deep Santuy",
            headTouchResponse = "Eits, santuy bro. Hati-hati jangan senggol jeruk yuzu kesayangan di kepala Jamal ya, nanti gelinding.",
            bellyTouchResponse = "Hoaam, perut kapibara emang paling empuk buat bantal santai bro. Usap terus aja sampai ngantuk.",
            handsTouchResponse = "Tos santuy dulu bro. Pelan-pelan aja ya, kita hemat energi buat rebahan.",
            feetTouchResponse = "Kaki kapibara mah mager buat lari-lari bro. Mending kita duduk santai sambil ngemil."
        ),
        PetCharacter(
            id = "heru",
            name = "Heru",
            species = PetSpecies.BEAR,
            gender = CharacterGender.MALE,
            voicePersona = GeminiVoicePersona.FENRIR,
            title = "Cowok Bass Berat & Kocak",
            description = "Beruang cowok berbadan kekar dengan suara bass paling berat dan menggelegar, suka ngegas tapi kocak.",
            defaultFurColor = Color(0xFF4E342E),
            defaultVoicePitch = 0.65f,
            defaultSpeechRate = 1.02f,
            personalityPrompt = """
                Kamu adalah Heru, beruang cowok berbadan besar dengan suara bass berat yang blak-blakan, lantang, tapi kocak.
                Gaya bicaramu: Pakai bahasa kasual ('lu', 'gua', 'heh', 'buset'). Terdengar garang di awal tapi sebenarnya solutif dan lucu.
            """.trimIndent(),
            wakeWordGreeting = "Heh! Siapa nih manggil-manggil gua pas lagi makan madu? Cepetan mau ngomong apa lu, jangan sungkan sama beruang keren ini!",
            badge = "♂️ Cowok • Bass Berat",
            headTouchResponse = "Heh! Lu kira kepala beruang gua bola basket apa main tepuk-tepuk begitu? Untung gua lagi baik hati nih!",
            bellyTouchResponse = "Buset dah! Berani-beraninya lu gelitikin perut beruang kekar kayak gua! Bisa ketawa ngakak gua lama-lama!",
            handsTouchResponse = "Nih tos cakar beruang! Awas tangan lu geter abis adu tos sama Heru ya!",
            feetTouchResponse = "Heh, jangan isengin kaki beruang gua! Sekali hentak bisa bergetar satu ruangan tahu!"
        )
    )

    fun getById(id: String): PetCharacter =
        characters.find { it.id == id } ?: characters.first()
}
