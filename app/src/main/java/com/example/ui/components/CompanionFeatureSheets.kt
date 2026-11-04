package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PetCharacter
import com.example.model.PomodoroPreset
import com.example.model.QuizCatalog
import com.example.model.QuizQuestion
import com.example.model.StudyFlashcard

/**
 * =======================================================================
 * FEATURE #2: 🎮 MINI-GAMES & KUIS CERDAS BARENG PET (TRIVIA BATTLE)
 * =======================================================================
 */
@Composable
fun QuizBattleSheet(
    character: PetCharacter,
    coins: Int,
    quizStreak: Int,
    onAnswerQuestion: (QuizQuestion, Int) -> Unit,
    onSpeakReaction: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("Semua Topik") }
    val filteredQuestions = remember(selectedCategory) {
        if (selectedCategory == "Semua Topik") {
            QuizCatalog.defaultQuestions
        } else {
            QuizCatalog.defaultQuestions.filter { it.category == selectedCategory }.ifEmpty {
                QuizCatalog.defaultQuestions
            }
        }
    }

    var currentIndex by remember(selectedCategory) { mutableIntStateOf(0) }
    var selectedOptionIndex by remember(currentIndex, selectedCategory) { mutableStateOf<Int?>(null) }
    var petReactionText by remember(currentIndex, selectedCategory) { mutableStateOf("") }

    val currentQuestion = filteredQuestions[currentIndex % filteredQuestions.size]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF090D1A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("quiz_battle_sheet")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("quiz_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "🎮 Kuis Cerdas Bareng ${character.name}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Jawab benar dapat +25 Koin ⭐ & +5% Kasih Sayang!",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFA5B4FC)
                            )
                        )
                    }
                }

                // Coins & Streak Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF08A).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFFACC15).copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "⭐ $coins", color = Color(0xFFFDE047), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "🔥 $quizStreak", color = Color(0xFFFB923C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Selector
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(QuizCatalog.categories) { cat ->
                            val isSelected = cat == selectedCategory
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.18f)
                                ),
                                modifier = Modifier.clickable {
                                    selectedCategory = cat
                                }
                            ) {
                                Text(
                                    text = cat,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Question Card
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.9f),
                        border = BorderStroke(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF6366F1).copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = currentQuestion.category,
                                        color = Color(0xFFC7D2FE),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Soal ${(currentIndex % filteredQuestions.size) + 1}/${filteredQuestions.size}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = currentQuestion.question,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Answer Options
                            currentQuestion.options.forEachIndexed { idx, optionText ->
                                val isAnswered = selectedOptionIndex != null
                                val isThisSelected = selectedOptionIndex == idx
                                val isThisCorrect = idx == currentQuestion.correctIndex

                                val bgColor = when {
                                    !isAnswered -> Color.White.copy(alpha = 0.07f)
                                    isThisCorrect -> Color(0xFF10B981).copy(alpha = 0.28f)
                                    isThisSelected -> Color(0xFFEF4444).copy(alpha = 0.28f)
                                    else -> Color.White.copy(alpha = 0.04f)
                                }
                                val borderColor = when {
                                    !isAnswered -> Color.White.copy(alpha = 0.18f)
                                    isThisCorrect -> Color(0xFF10B981)
                                    isThisSelected -> Color(0xFFEF4444)
                                    else -> Color.White.copy(alpha = 0.08f)
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = bgColor,
                                    border = BorderStroke(1.5.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                        .clickable(enabled = !isAnswered) {
                                            selectedOptionIndex = idx
                                            val reaction = QuizCatalog.getReactionForAnswer(
                                                isCorrect = idx == currentQuestion.correctIndex,
                                                character = character,
                                                funFact = currentQuestion.funFact
                                            )
                                            petReactionText = reaction
                                            onAnswerQuestion(currentQuestion, idx)
                                        }
                                        .testTag("quiz_option_$idx")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        val prefix = listOf("A", "B", "C", "D").getOrElse(idx) { "•" }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(borderColor.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = prefix,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = optionText,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Pet Reaction & Explanation Banner
                if (selectedOptionIndex != null && petReactionText.isNotBlank()) {
                    item {
                        val isCorrect = selectedOptionIndex == currentQuestion.correctIndex
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isCorrect) Color(0xFF064E3B).copy(alpha = 0.75f) else Color(0xFF450A0A).copy(alpha = 0.75f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isCorrect) Color(0xFF34D399) else Color(0xFFF87171)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = character.species.emoji, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isCorrect) "🎉 ${character.name}: Jawaban Benar! (+25 ⭐)" else "💡 ${character.name}: Pembahasan",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = { onSpeakReaction(petReactionText) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Dengarkan",
                                            tint = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = petReactionText,
                                    color = Color(0xFFF1F5F9),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { currentIndex++ },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quiz_next_question_button")
                                ) {
                                    Text(text = "Soal Berikutnya ➡️", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * =======================================================================
 * FEATURE #3: ⏱️ POMODORO STUDY TIMER (TEMAN FOKUS BELAJAR)
 * =======================================================================
 */
@Composable
fun PomodoroStudySheet(
    character: PetCharacter,
    activePreset: PomodoroPreset,
    remainingSeconds: Int,
    isRunning: Boolean,
    completedSessions: Int,
    onSelectPreset: (PomodoroPreset) -> Unit,
    onToggleStartPause: () -> Unit,
    onResetTimer: () -> Unit,
    onCompleteNowAndClaim: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSeconds = (activePreset.durationMinutes * 60).coerceAtLeast(1)
    val progress = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", mins, secs)

    val companionMotivation = when (character.id) {
        "zayn" -> if (isRunning) {
            "Hmph! Fokus ke buku kamu ya, jangan curi-curi pandang ke aku terus! Beresin dulu sampai timer habis biar dapet +${activePreset.rewardCoins} koin!"
        } else {
            "Ayo nyalain timernya! Aku tungguin kamu belajar di sini... B-bukan berarti aku kangen pengen nemenin kamu lho ya!"
        }
        "siti" -> if (isRunning) {
            "Semangat fokusnya ya sayangku... Siti duduk tenang di sebelahmu sambil doain ilmu kamu berkah dan auto nempel di kepala, hihi."
        } else {
            "Pilih durasi fokus yang kamu suka ya manisku, Siti siap nemenin kamu belajar tanpa gangguan!"
        }
        "rizky" -> if (isRunning) {
            "OPERASI FOKUS BERJALAN! Jauhkan distraksi, komandan! Target hadiah +${activePreset.rewardCoins} Koin menanti di garis akhir!"
        } else {
            "Siap komandan! Pilih durasi misi belajar Pomodoro dan tekan Mulai Fokus untuk gaspol!"
        }
        "jamal" -> if (isRunning) {
            "Belajar fokus tapi hati tetap santuy bro... Pelan-pelan aja bacanya, Jamal jagain timernya sambil rebahan di pojok ya wkwk."
        } else {
            "Yuk cicil belajarnya pake teknik Pomodoro bro, biar nggak spaneng dan ada waktu istirahat santuy-nya!"
        }
        else -> if (isRunning) {
            "AWAS AJA KALAU LU BUKA SOSMED PAS TIMER JALAN! Fokus belajar dulu bareng gua, ntar gua kasih +${activePreset.rewardCoins} koin kalau tuntas!"
        } else {
            "HEH! Jangan menunda-nunda tugas mulu wkwk! Pencet tombol Mulai Fokus sekarang, gua jagain dari sini!"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF172554), Color(0xFF090D1A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("pomodoro_study_sheet")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("pomodoro_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "⏱️ Pomodoro Teman Fokus",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Ditemani ${character.name} • Sesi Selesai: $completedSessions",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF93C5FD)
                            )
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preset Chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PomodoroPreset.values()) { preset ->
                            val isSelected = preset == activePreset
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.18f)
                                ),
                                modifier = Modifier
                                    .clickable { onSelectPreset(preset) }
                                    .testTag("pomodoro_preset_${preset.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(text = preset.emoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${preset.title} (${preset.durationMinutes}m)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Circular Countdown Ring
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(230.dp)
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW = 14.dp.toPx()
                            drawArc(
                                color = Color.White.copy(alpha = 0.1f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeW)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFF34D399))
                                ),
                                startAngle = -90f,
                                sweepAngle = progress * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = character.species.emoji, fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formattedTime,
                                color = Color.White,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (isRunning) "🔥 Sedang Fokus..." else activePreset.subtitle,
                                color = Color(0xFF93C5FD),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Companion Speech Card
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(text = character.species.emoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pesan Penyemangat ${character.name}:",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = companionMotivation,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Controls: Start/Pause, Reset, & Complete Claim
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onToggleStartPause,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) Color(0xFFF59E0B) else Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("pomodoro_start_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRunning) "Jeda Timer" else "Mulai Fokus",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onResetTimer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("pomodoro_reset_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ulang")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onCompleteNowAndClaim,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pomodoro_claim_reward_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Selesaikan Sesi & Klaim +${activePreset.rewardCoins} Koin ⭐",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * =======================================================================
 * FEATURE #6: 🃏 FLASHCARD OTOMATIS DARI MATERI SEKOLAH
 * =======================================================================
 */
@Composable
fun FlashcardStudySheet(
    character: PetCharacter,
    selectedSubject: String,
    flashcards: List<StudyFlashcard>,
    isGenerating: Boolean,
    onSelectSubject: (String) -> Unit,
    onGenerateCustomTopic: (String) -> Unit,
    onMarkCardMastered: (String) -> Unit,
    onSpeakCard: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customTopicInput by remember { mutableStateOf("") }
    var currentIndex by remember(flashcards) { mutableIntStateOf(0) }
    var isFlipped by remember(currentIndex, flashcards) { mutableStateOf(false) }

    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    val safeCards = flashcards.ifEmpty {
        com.example.model.FlashcardCatalog.getDefaultDeck(selectedSubject)
    }
    val currentCard = safeCards[currentIndex.coerceIn(0, safeCards.lastIndex)]
    val masteredCount = safeCards.count { it.isMastered }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF090D1A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("flashcard_study_sheet")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("flashcard_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "🃏 Flashcard Pintar ${character.name}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Hafal: $masteredCount/${safeCards.size} Kartu • Tap kartu untuk balik!",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFA5B4FC)
                            )
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Subject Chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SUBJECT_OPTIONS) { subj ->
                            val isSelected = selectedSubject.equals(subj.name, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) subj.color else Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) subj.color else Color.White.copy(alpha = 0.18f)
                                ),
                                modifier = Modifier.clickable {
                                    onSelectSubject(subj.name)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(text = subj.icon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = subj.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Topic Generator Input
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.07f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customTopicInput,
                                onValueChange = { customTopicInput = it },
                                placeholder = {
                                    Text(
                                        "Ketik bab/materi (misal: Tata Surya, Aljabar)...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF818CF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("flashcard_topic_input")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onGenerateCustomTopic(customTopicInput.ifBlank { selectedSubject })
                                },
                                enabled = !isGenerating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("flashcard_generate_button")
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Buat AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Interactive 3D Flip Card
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (!isFlipped) Color(0xFF1E293B) else Color(0xFF064E3B),
                        border = BorderStroke(
                            2.dp,
                            if (!isFlipped) Color(0xFF818CF8) else Color(0xFF34D399)
                        ),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .graphicsLayer {
                                rotationY = if (flipRotation <= 90f) flipRotation else 180f - flipRotation
                                cameraDistance = 12f * density
                            }
                            .clickable { isFlipped = !isFlipped }
                            .testTag("flashcard_flip_card")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (!isFlipped) "❓ PERTANYAAN (${currentIndex + 1}/${safeCards.size})" else "✅ JAWABAN & TIPS ${character.name.uppercase()}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val speech = if (!isFlipped) currentCard.questionFront else "${currentCard.answerBack}. ${currentCard.memoryTip}"
                                        onSpeakCard(speech)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Dengarkan Kartu",
                                        tint = Color.White
                                    )
                                }
                            }

                            AnimatedContent(
                                targetState = isFlipped,
                                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                                label = "cardContent"
                            ) { flipped ->
                                if (!flipped) {
                                    Text(
                                        text = currentCard.questionFront,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 25.sp
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = currentCard.answerBack,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 21.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "💡 ${currentCard.memoryTip}",
                                            color = Color(0xFFFDE68A),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (!isFlipped) "👆 Ketuk kartu untuk melihat jawaban" else "👆 Ketuk kartu untuk balik ke pertanyaan",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Navigation & Mastery Controls
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                currentIndex = if (currentIndex > 0) currentIndex - 1 else safeCards.lastIndex
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sebelumnya")
                        }

                        Button(
                            onClick = {
                                onMarkCardMastered(currentCard.id)
                                if (currentIndex < safeCards.lastIndex) {
                                    currentIndex++
                                } else {
                                    currentIndex = 0
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentCard.isMastered) Color(0xFF059669) else Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("flashcard_mastered_button")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentCard.isMastered) "Sudah Dihafal ✅" else "Sudah Hafal (+10 ⭐)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                currentIndex = (currentIndex + 1) % safeCards.size
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("flashcard_next_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Berikutnya")
                        }
                    }
                }
            }
        }
    }
}
