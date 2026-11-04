package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * 1. Special Character Favorite Foods & Treats (Feature #4: Sistem Rawat Pet & Makanan Favorit)
 */
data class SpecialPetFood(
    val id: String,
    val name: String,
    val emoji: String,
    val priceCoins: Int,
    val hungerRestore: Float,
    val affectionBonus: Int,
    val favoriteCharacterId: String,
    val description: String
)

object SpecialPetFoodCatalog {
    val foods = listOf(
        SpecialPetFood(
            id = "salmon_zayn",
            name = "Salmon Sashimi Spesial",
            emoji = "🍣",
            priceCoins = 30,
            hungerRestore = 0.55f,
            affectionBonus = 10,
            favoriteCharacterId = "zayn",
            description = "Favorit Zayn! Ikan salmon segar premium bikin kucing tsundere auto luluh."
        ),
        SpecialPetFood(
            id = "carrot_siti",
            name = "Wortel Madu Gemoy",
            emoji = "🥕",
            priceCoins = 25,
            hungerRestore = 0.50f,
            affectionBonus = 10,
            favoriteCharacterId = "siti",
            description = "Favorit Siti! Wortel organik manis dengan glasir madu hangat."
        ),
        SpecialPetFood(
            id = "steak_rizky",
            name = "Steak Daging Komando",
            emoji = "🥩",
            priceCoins = 35,
            hungerRestore = 0.65f,
            affectionBonus = 10,
            favoriteCharacterId = "rizky",
            description = "Favorit Rizky! Protein tinggi buat menjaga energi dan disiplin tempur."
        ),
        SpecialPetFood(
            id = "watermelon_jamal",
            name = "Semangka Es Santuy",
            emoji = "🍉",
            priceCoins = 20,
            hungerRestore = 0.45f,
            affectionBonus = 10,
            favoriteCharacterId = "jamal",
            description = "Favorit Jamal! Segar dan berair, paling mantap dimakan sambil rebahan."
        ),
        SpecialPetFood(
            id = "honey_heru",
            name = "Madu Hutan Premium",
            emoji = "🍯",
            priceCoins = 30,
            hungerRestore = 0.60f,
            affectionBonus = 10,
            favoriteCharacterId = "heru",
            description = "Favorit Heru! Manis legit bikin beruang galak langsung senyum lebar."
        ),
        SpecialPetFood(
            id = "boba_milk",
            name = "Boba Brown Sugar",
            emoji = "🧋",
            priceCoins = 18,
            hungerRestore = 0.35f,
            affectionBonus = 6,
            favoriteCharacterId = "all",
            description = "Minuman kekinian manis segar disukai semua pet!"
        ),
        SpecialPetFood(
            id = "pizza_slice",
            name = "Pizza Keju Lumer",
            emoji = "🍕",
            priceCoins = 22,
            hungerRestore = 0.45f,
            affectionBonus = 7,
            favoriteCharacterId = "all",
            description = "Potongan pizza hangat dengan keju mozzarella melimpah."
        )
    )

    fun getReactionForFood(food: SpecialPetFood, character: PetCharacter): String {
        val isFav = food.favoriteCharacterId == character.id
        return when (character.id) {
            "zayn" -> if (isFav) {
                "H-hah?! Kamu beliin aku ${food.name}?! Nyaa~ enak banget tau! B-bukan berarti aku bakal manja-manja sama kamu ya, tapi makasih banyak wkwk!"
            } else {
                "Nyam nyam... Lumayan enak juga ${food.name} ini! Makasih ya udah traktir aku, perutku auto kenyang nih!"
            }
            "siti" -> if (isFav) {
                "Ya ampun sayangku, ini kan ${food.name} kesukaan Siti! Rasanya manis dan gemoy banget di mulut, makasih banyak ya bestie kesayanganku, hihi!"
            } else {
                "Mmm nyam! Terima kasih banyak untuk ${food.name}-nya ya manisku, Siti jadi makin semangat nemenin kamu hari ini!"
            }
            "rizky" -> if (isFav) {
                "SIAP KOMANDAN! Logistik utama ${food.name} diterima dengan sempurna! Energi tempur langsung naik 100% valid no debat, gaspol!"
            } else {
                "Siap! Suplai ${food.name} lezat sudah disantap, komandan! Stamina kembali prima untuk lanjut belajar!"
            }
            "jamal" -> if (isFav) {
                "Waaah mantap jiwa bro! ${food.name} pas lagi rebahan gini tuh definisi surga dunia wkwk. Makasih berat ya broku!"
            } else {
                "Nyam... Enak banget bro ${food.name}-nya. Perut kenyang, hati tenang, rebahan makin santuy nih hehe."
            }
            else -> if (isFav) {
                "BUSET! Kok lu tahu sih gua doyan banget sama ${food.name}?! Grr... enak parah woi! Makasih ya bestie, lu emang paling pengertian wkwk!"
            } else {
                "Nyam nyam! Lumayan mantap juga nih ${food.name} buatan lu! Awas kalau besok nggak dikasih makan enak lagi ya wkwk!"
            }
        }
    }
}

/**
 * 2. Mini-Games & Kuis Cerdas Bareng Pet (Feature #2)
 */
data class QuizQuestion(
    val id: String,
    val category: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val funFact: String
)

object QuizCatalog {
    val categories = listOf(
        "Semua Topik",
        "Matematika Cepat",
        "Sains & Alam",
        "Pengetahuan Umum",
        "Tebak Gaul & Logika"
    )

    val defaultQuestions = listOf(
        QuizQuestion(
            id = "q1",
            category = "Matematika Cepat",
            question = "Kalau kamu punya 8 kotak pensil dan tiap kotak berisi 12 pensil, terus kamu kasih 16 pensil ke sahabatmu, sisa berapa pensilmu?",
            options = listOf("76 pensil", "80 pensil", "84 pensil", "96 pensil"),
            correctIndex = 1,
            funFact = "8 x 12 = 96 pensil. Dikurangi 16 pensil hasilnya tepat 80 pensil!"
        ),
        QuizQuestion(
            id = "q2",
            category = "Sains & Alam",
            question = "Planet manakah di Tata Surya kita yang punya julukan 'Planet Merah' karena permukaannya kaya akan besi oksida?",
            options = listOf("Venus", "Mars", "Jupiter", "Merkurius"),
            correctIndex = 1,
            funFact = "Mars tampak kemerahan karena debu karat (besi oksida) yang menyelimuti permukaannya."
        ),
        QuizQuestion(
            id = "q3",
            category = "Pengetahuan Umum",
            question = "Lagu kebangsaan Indonesia Raya pertama kali dikumandangkan pada peristiwa bersejarah apa?",
            options = listOf(
                "Proklamasi 17 Agustus 1945",
                "Sumpah Pemuda 28 Oktober 1928",
                "Kebangkitan Nasional 20 Mei 1908",
                "Konferensi Asia Afrika 1955"
            ),
            correctIndex = 1,
            funFact = "W.R. Supratman memperdengarkan Indonesia Raya dengan biola pada Kongres Pemuda II tanggal 28 Oktober 1928."
        ),
        QuizQuestion(
            id = "q4",
            category = "Sains & Alam",
            question = "Gas apakah yang paling banyak diserap oleh tumbuhan hijau saat melakukan proses fotosintesis di siang hari?",
            options = listOf("Oksigen (O2)", "Karbondioksida (CO2)", "Nitrogen (N2)", "Hidrogen (H2)"),
            correctIndex = 1,
            funFact = "Tumbuhan menyerap CO2 dan mengubahnya menjadi glukosa serta melepaskan Oksigen (O2) yang kita hirup!"
        ),
        QuizQuestion(
            id = "q5",
            category = "Matematika Cepat",
            question = "Sebuah segitiga siku-siku memiliki alas 6 cm dan tinggi 8 cm. Berapakah panjang sisi miring (hipotenusa)-nya?",
            options = listOf("10 cm", "12 cm", "14 cm", "9 cm"),
            correctIndex = 0,
            funFact = "Dari rumus Pythagoras: √(6² + 8²) = √(36 + 64) = √100 = 10 cm!"
        ),
        QuizQuestion(
            id = "q6",
            category = "Tebak Gaul & Logika",
            question = "Aku selalu naik setiap tahun, tapi nggak pernah bisa turun lagi. Siapakah aku?",
            options = listOf("Harga kuota internet", "Umur / Usia", "Balon udara", "Tangga rumah"),
            correctIndex = 1,
            funFact = "Umur manusia selalu bertambah (naik) dan tidak pernah berkurang (turun), wkwk!"
        ),
        QuizQuestion(
            id = "q7",
            category = "Pengetahuan Umum",
            question = "Hewan mamalia semi-akuatik asal Amerika Selatan yang terkenal super santuy dan mudah berteman dengan hewan lain adalah...",
            options = listOf("Berang-berang", "Kapibara", "Platipus", "Koala"),
            correctIndex = 1,
            funFact = "Kapibara (seperti Jamal!) adalah hewan pengerat terbesar di dunia yang terkenal paling chill dan ramah."
        ),
        QuizQuestion(
            id = "q8",
            category = "Matematika Cepat",
            question = "Berapakah hasil dari 25% dari 240?",
            options = listOf("45", "50", "60", "75"),
            correctIndex = 2,
            funFact = "25% sama dengan 1/4. Jadi 240 dibagi 4 hasilnya adalah 60!"
        ),
        QuizQuestion(
            id = "q9",
            category = "Sains & Alam",
            question = "Bagian sel yang berfungsi sebagai 'pembangkit energi' (tempat respirasi seluler) disebut...",
            options = listOf("Ribosom", "Mitokondria", "Lisosom", "Badan Golgi"),
            correctIndex = 1,
            funFact = "The powerhouse of the cell adalah Mitokondria yang menghasilkan energi ATP!"
        ),
        QuizQuestion(
            id = "q10",
            category = "Tebak Gaul & Logika",
            question = "Bulan apa dalam setahun yang punya 28 hari?",
            options = listOf("Cuma bulan Februari", "Semua 12 bulan punya tanggal 28!", "Bulan Kabisat", "Bulan Desember"),
            correctIndex = 1,
            funFact = "Pertanyaan jebakan! Semua bulan dari Januari sampai Desember pasti melewati tanggal 28, wkwk!"
        )
    )

    fun getReactionForAnswer(isCorrect: Boolean, character: PetCharacter, funFact: String): String {
        return if (isCorrect) {
            when (character.id) {
                "zayn" -> "Hmph, bener banget jawabanmu! Lumayan encer juga otak kamu ya, wkwk. $funFact B-bukan berarti aku kagum lho!"
                "siti" -> "Yeay! Jawaban kamu 100% bener sayangku! Pinter dan gemoy banget sih, Siti bangga banget! $funFact"
                "rizky" -> "SIAP TEPAT SASARAN! Analisis komandan akurat no debat! $funFact Pertahankan performa juara ini, gaspol!"
                "jamal" -> "Mantap jiwa bro, bener banget! Nggak sia-sia belajar sambil santuy wkwk. $funFact"
                else -> "WUIH BENER DONG! Gitu dong pinter, nggak malu-maluin jadi bestie gua wkwk! $funFact"
            }
        } else {
            when (character.id) {
                "zayn" -> "Hmph, kurang tepat tau! Jangan asal tebak makanya, dengerin nih: $funFact Yuk fokus lagi di soal berikutnya!"
                "siti" -> "Aduh hampir bener manisku, jangan sedih ya... Sini Siti spill: $funFact Soal selanjutnya pasti kamu bisa kok!"
                "rizky" -> "Siap koreksi taktis! Jawaban barusan meleset sedikit, komandan. Catatan penting: $funFact Ayo bangkit di soal berikutnya!"
                "jamal" -> "Yah meleset dikit bro, santuy aja nggak usah panik wkwk. Info pentingnya nih: $funFact"
                else -> "LAH SALAH PILIH LU WKWK! Makanya baca baik-baik dulu! Dengerin nih: $funFact Gas bales di soal selanjutnya!"
            }
        }
    }
}

/**
 * 3. Pomodoro Study Timer (Feature #3: Teman Fokus Belajar)
 */
enum class PomodoroPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val durationMinutes: Int,
    val rewardCoins: Int,
    val affectionReward: Int,
    val isBreak: Boolean = false
) {
    FOCUS_25("focus_25", "Fokus Standar", "25 Menit Belajar Fokus", "🔥", 25, 50, 10, false),
    SPRINT_15("sprint_15", "Sprint Tugas", "15 Menit Kebut PR", "⚡", 15, 35, 7, false),
    QUICK_5("quick_5", "Review Kilat", "5 Menit Hafalan Cepat", "🎯", 5, 20, 5, false),
    BREAK_5("break_5", "Istirahat Santuy", "5 Menit Rehat Otak", "☕", 5, 15, 5, true)
}

/**
 * 4. Daily Mood Journal & Curhat Tracker (Feature #5)
 */
enum class MoodType(
    val id: String,
    val label: String,
    val emoji: String,
    val badgeColor: Color
) {
    SENANG("senang", "Senang", "😄", Color(0xFF10B981)),
    SEMANGAT("semangat", "Semangat", "🔥", Color(0xFFF59E0B)),
    SANTUY("santuy", "Santuy", "😎", Color(0xFF06B6D4)),
    CAPEK("capek", "Capek", "😴", Color(0xFF8B5CF6)),
    GALAU("galau", "Galau", "🥺", Color(0xFFEC4899))
}

data class MoodJournalEntry(
    val id: String,
    val timestampMillis: Long,
    val formattedDate: String,
    val mood: MoodType,
    val userNote: String,
    val characterName: String,
    val characterEmoji: String,
    val petResponse: String
)

/**
 * 5. Study Flashcards (Feature #6: Flashcard Otomatis dari Materi Sekolah)
 */
data class StudyFlashcard(
    val id: String,
    val subject: String,
    val questionFront: String,
    val answerBack: String,
    val memoryTip: String,
    val isMastered: Boolean = false
)

object FlashcardCatalog {
    fun getDefaultDeck(subject: String): List<StudyFlashcard> {
        val norm = subject.lowercase()
        return when {
            norm.contains("matematika") || norm.contains("rumus") || norm.contains("hitung") -> listOf(
                StudyFlashcard(
                    id = "math_1",
                    subject = "Matematika",
                    questionFront = "Apa rumus Luas dan Volume Tabung?",
                    answerBack = "• Volume Tabung = π × r² × t\n• Luas Permukaan = 2 × π × r × (r + t)",
                    memoryTip = "Ingat aja 'Pi-R-Kuadrat-T'! Alas tabung kan lingkaran (πr²), tinggal dikali tingginya (t)!"
                ),
                StudyFlashcard(
                    id = "math_2",
                    subject = "Matematika",
                    questionFront = "Bagaimana bunyi dan rumus Teorema Pythagoras?",
                    answerBack = "Pada segitiga siku-siku: c² = a² + b²\n(Sisi miring kuadrat = jumlah kuadrat kedua sisi tegaknya)",
                    memoryTip = "Hafalin tripel Pythagoras favorit: (3, 4, 5), (5, 12, 13), dan (6, 8, 10) biar ngitung cuma 2 detik!"
                ),
                StudyFlashcard(
                    id = "math_3",
                    subject = "Matematika",
                    questionFront = "Apa rumus Kecepatan, Jarak, dan Waktu (Segitiga JKW)?",
                    answerBack = "• Jarak (J) = Kecepatan (K) × Waktu (W)\n• Kecepatan (K) = J / W\n• Waktu (W) = J / K",
                    memoryTip = "Jembatan keledai: 'Pak Jokowi' (J di atas, K dan W di bawah). Kalau cari J tinggal K dikali W!"
                ),
                StudyFlashcard(
                    id = "math_4",
                    subject = "Matematika",
                    questionFront = "Apa rumus suku ke-n (Un) pada Barisan Aritmatika?",
                    answerBack = "Un = a + (n - 1) × b\n(a = suku pertama, b = beda/selisih antar suku)",
                    memoryTip = "Ingat 'A-N-min-1-B': Mulai dari suku awal (a), lalu lompat sebanyak (n-1) kali sebesar beda (b)!"
                )
            )
            norm.contains("ipa") || norm.contains("sains") || norm.contains("biologi") || norm.contains("fisika") -> listOf(
                StudyFlashcard(
                    id = "ipa_1",
                    subject = "IPA",
                    questionFront = "Apa reaksi kimia lengkap dari proses Fotosintesis?",
                    answerBack = "6 CO₂ (Karbondioksida) + 6 H₂O (Air) --(Cahaya + Klorofil)--> C₆H₁₂O₆ (Glukosa) + 6 O₂ (Oksigen)",
                    memoryTip = "Ingat: Daun 'masak' pakai air + CO2 + sinar matahari, hasilnya makanan manis (glukosa) dan oksigen segar!"
                ),
                StudyFlashcard(
                    id = "ipa_2",
                    subject = "IPA",
                    questionFront = "Sebutkan urutan 8 planet di Tata Surya dari yang terdekat dengan Matahari!",
                    answerBack = "Merkurius, Venus, Bumi, Mars, Jupiter, Saturnus, Uranus, Neptunus.",
                    memoryTip = "Jembatan keledai: 'Me-Ve-Bu-Ma-Ju-Sa-U-Ne' (Merdeka, Venus, Bumi, Mars, Juara, Satu, Untuk, Negeri)!"
                ),
                StudyFlashcard(
                    id = "ipa_3",
                    subject = "IPA",
                    questionFront = "Apa bunyi Hukum I, II, dan III Newton?",
                    answerBack = "• Hukum I (Inersia): ΣF = 0 (benda diam tetap diam)\n• Hukum II: F = m × a\n• Hukum III: F_aksi = -F_reaksi",
                    memoryTip = "Hukum 1 = Mager/Inersia, Hukum 2 = Dorong mobil (F=m.a), Hukum 3 = Memantul (Aksi-Reaksi)!"
                ),
                StudyFlashcard(
                    id = "ipa_4",
                    subject = "IPA",
                    questionFront = "Apa perbedaan sel tumbuhan dan sel hewan?",
                    answerBack = "Sel tumbuhan punya Dinding Sel, Kloroplas (hijau daun), dan Vakuola besar. Sel hewan tidak punya dinding sel namun punya Sentriol.",
                    memoryTip = "Tumbuhan itu tegak & hijau -> pasti punya Dinding Sel & Kloroplas!"
                )
            )
            norm.contains("inggris") || norm.contains("english") -> listOf(
                StudyFlashcard(
                    id = "eng_1",
                    subject = "Bahasa Inggris",
                    questionFront = "Kapan kita menggunakan Simple Present Tense dan apa rumusnya?",
                    answerBack = "Digunakan untuk fakta umum & kebiasaan sehari-hari (habit).\nRumus: Subject + Verb 1 (+ s/es untuk He/She/It).",
                    memoryTip = "Kalau subjeknya tunggal (He, She, It), kata kerjanya suka 's' (ditambah -s/-es), contoh: She plays!"
                ),
                StudyFlashcard(
                    id = "eng_2",
                    subject = "Bahasa Inggris",
                    questionFront = "Apa bedanya Simple Past Tense dan Present Perfect Tense?",
                    answerBack = "• Simple Past (S + V2): Kejadian lampau dengan waktu spesifik (yesterday, last week).\n• Present Perfect (S + have/has + V3): Sudah terjadi dan efeknya masih terasa sekarang.",
                    memoryTip = "Ada keterangan waktu lampau jelas (yesterday) = wajib Verb 2!"
                ),
                StudyFlashcard(
                    id = "eng_3",
                    subject = "Bahasa Inggris",
                    questionFront = "Sebutkan 3 tipe Conditional Sentence (Kalimat Pengandaian)!",
                    answerBack = "• Type 1 (Mungkin terjadi): If + Present, Will + V1\n• Type 2 (Khayalan sekarang): If + Past (V2), Would + V1\n• Type 3 (Penyesalan lampau): If + Had V3, Would have V3",
                    memoryTip = "Makin naik tipenya, tenses-nya makin mundur ke masa lalu!"
                )
            )
            norm.contains("ips") || norm.contains("sejarah") -> listOf(
                StudyFlashcard(
                    id = "ips_1",
                    subject = "IPS & Sejarah",
                    questionFront = "Sebutkan isi Tiga Tuntutan atau Isi Sumpah Pemuda (28 Oktober 1928)!",
                    answerBack = "1. Bertumpah darah yang satu, tanah air Indonesia.\n2. Berbangsa yang satu, bangsa Indonesia.\n3. Menjunjung bahasa persatuan, bahasa Indonesia.",
                    memoryTip = "Urutannya gampang: Tanah Air -> Bangsa -> Bahasa Indonesia!"
                ),
                StudyFlashcard(
                    id = "ips_2",
                    subject = "IPS & Sejarah",
                    questionFront = "Siapa tokoh Tiga Serangkai pendiri Indische Partij dan perumus naskah Proklamasi?",
                    answerBack = "• Tiga Serangkai: Douwes Dekker, Cipto Mangunkusumo, Ki Hajar Dewantara.\n• Perumus Proklamasi: Ir. Soekarno, Drs. Moh. Hatta, Mr. Ahmad Soebardjo.",
                    memoryTip = "Perumus teks proklamasi di rumah Laksamana Maeda: Soekarno-Hatta-Soebardjo!"
                ),
                StudyFlashcard(
                    id = "ips_3",
                    subject = "IPS & Sejarah",
                    questionFront = "Apa perbedaan Hukum Permintaan (Demand) dan Penawaran (Supply)?",
                    answerBack = "• Permintaan (Pembeli): Harga naik -> jumlah diminta turun.\n• Penawaran (Penjual): Harga naik -> jumlah ditawarkan naik.",
                    memoryTip = "Posisikan diri jadi pembeli (suka harga diskon/murah) vs penjual (suka harga tinggi biar untung)!"
                )
            )
            else -> listOf(
                StudyFlashcard(
                    id = "id_1",
                    subject = "Bahasa Indonesia",
                    questionFront = "Apa perbedaan Kalimat Deduktif dan Kalimat Induktif?",
                    answerBack = "• Deduktif: Gagasan utama berada di AWAL paragraf (Umum -> Khusus).\n• Induktif: Gagasan utama berada di AKHIR paragraf (Khusus -> Umum).",
                    memoryTip = "Ingat 'De-Depan' (Deduktif di Depan/Awal) dan 'In-Akhir'!"
                ),
                StudyFlashcard(
                    id = "id_2",
                    subject = "Bahasa Indonesia",
                    questionFront = "Apa yang dimaksud dengan Majas Personifikasi dan Hiperbola?",
                    answerBack = "• Personifikasi: Benda mati seolah-olah hidup seperti manusia ('Angin malam membisikkan rindu').\n• Hiperbola: Pernyataan melebih-lebihkan ('Suaranya menggelegar membelah angkasa').",
                    memoryTip = "Person = Orang (benda mati berlagak kayak orang). Hiper = Berlebihan!"
                ),
                StudyFlashcard(
                    id = "id_3",
                    subject = "Bahasa Indonesia",
                    questionFront = "Apa struktur utama dari Teks Eksposisi?",
                    answerBack = "1. Tesis (Pernyataan pendapat)\n2. Argumentasi (Alasan & bukti pendukung)\n3. Penegasan Ulang (Kesimpulan).",
                    memoryTip = "Ingat singkatan 'T-A-P': Tesis -> Argumentasi -> Penegasan Ulang!"
                )
            )
        }
    }
}
