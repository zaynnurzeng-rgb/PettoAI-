package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MoodJournalEntry
import com.example.model.MoodType
import com.example.model.PetCharacter

/**
 * =======================================================================
 * 📓 JURNAL MOOD HARIAN & CURHAT TRACKER
 * =======================================================================
 */
@Composable
fun MoodJournalSheet(
    character: PetCharacter,
    entries: List<MoodJournalEntry>,
    isSavingMood: Boolean,
    onSubmitMoodEntry: (MoodType, String) -> Unit,
    onSpeakResponse: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMood by remember { mutableStateOf(MoodType.SENANG) }
    var curhatText by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF2E1065), Color(0xFF090D1A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("mood_journal_sheet")
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
                            .testTag("mood_close_button")
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
                            text = "📓 Jurnal Mood & Curhat Harian",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Ceritakan harimu bersama ${character.name}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFD8B4FE)
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
                // Mood Picker Card
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF1E1B4B).copy(alpha = 0.85f),
                        border = BorderStroke(1.5.dp, Color(0xFFA855F7).copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Gimana perasaanmu hari ini?",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(MoodType.values()) { mood ->
                                    val isSelected = mood == selectedMood
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) mood.badgeColor else Color.White.copy(alpha = 0.08f),
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (isSelected) Color.White else Color.White.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier
                                            .clickable { selectedMood = mood }
                                            .testTag("mood_option_${mood.id}")
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(text = mood.emoji, fontSize = 24.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = mood.label,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = curhatText,
                                onValueChange = { curhatText = it },
                                placeholder = {
                                    Text(
                                        "Tulis cerita atau curhatanmu hari ini ke ${character.name}...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                },
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFC084FC),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mood_curhat_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onSubmitMoodEntry(selectedMood, curhatText)
                                    curhatText = ""
                                },
                                enabled = !isSavingMood,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9333EA),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("mood_submit_button")
                            ) {
                                if (isSavingMood) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${character.name} sedang mendengarkan...")
                                } else {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Simpan Jurnal & Curhat", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Mood History Log
                item {
                    Text(
                        text = "📖 Riwayat Jurnal & Balasan ${character.name} (${entries.size}):",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(entries) { entry ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.07f),
                        border = BorderStroke(1.dp, entry.mood.badgeColor.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = entry.mood.emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Mood: ${entry.mood.label}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = entry.formattedDate,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            if (entry.userNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"${entry.userNote}\"",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF3B0764).copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(text = entry.characterEmoji, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Balasan ${entry.characterName}:",
                                            color = Color(0xFFD8B4FE),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = entry.petResponse,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { onSpeakResponse(entry.petResponse) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Dengarkan",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
