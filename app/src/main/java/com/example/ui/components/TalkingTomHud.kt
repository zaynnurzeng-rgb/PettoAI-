package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CharacterGender
import com.example.model.PetCharacter

/**
 * Clean, distraction-free Top HUD:
 * - Left: Active Character pill (with gender badge & tap to switch character)
 * - Right: Room Theme toggle, Chat History button, and Settings button
 */
@Composable
fun TalkingTomTopHud(
    activeCharacter: PetCharacter,
    currentThemeIcon: String,
    onSwitchCharacterClick: () -> Unit,
    onThemeClick: () -> Unit,
    onChatHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFemale = activeCharacter.gender == CharacterGender.FEMALE
    val genderBadgeColor = if (isFemale) Color(0xFFF472B6) else Color(0xFF60A5FA)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Active Character Selector Pill
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xD90F172A),
            border = BorderStroke(1.5.dp, activeCharacter.defaultFurColor.copy(alpha = 0.75f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .testTag("top_level_badge")
                .clickable(onClick = onSwitchCharacterClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(activeCharacter.defaultFurColor.copy(alpha = 0.25f))
                        .border(1.dp, activeCharacter.defaultFurColor, CircleShape)
                ) {
                    Text(
                        text = activeCharacter.species.emoji,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeCharacter.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = genderBadgeColor.copy(alpha = 0.22f),
                            border = BorderStroke(0.8.dp, genderBadgeColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "${activeCharacter.gender.symbol} ${activeCharacter.gender.shortLabel}",
                                color = genderBadgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Suara ${activeCharacter.voicePersona.geminiVoiceName} • Ketuk Ganti ▾",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 2. Right Quick Utility Buttons (Room Theme, Chat History, Settings)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Room Theme Button
            GlossyIconButton(
                bgColor = Color(0xFF1E293B),
                borderColor = Color(0xFF475569),
                icon = {
                    Text(
                        text = currentThemeIcon,
                        fontSize = 18.sp
                    )
                },
                testTag = "open_theme_button",
                onClick = onThemeClick,
                size = 42
            )

            // Chat History Button
            GlossyIconButton(
                bgColor = Color(0xFF1E293B),
                borderColor = Color(0xFF475569),
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Riwayat Obrolan",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                },
                testTag = "open_chat_history_button",
                onClick = onChatHistoryClick,
                size = 42
            )

            // Settings Button
            GlossyIconButton(
                bgColor = Color(0xFF2563EB),
                borderColor = Color(0xFF93C5FD),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                },
                testTag = "settings_button",
                onClick = onSettingsClick,
                size = 42
            )
        }
    }
}

/**
 * Circular Glossy Button with bevel border & soft glow
 */
@Composable
fun GlossyIconButton(
    bgColor: Color,
    borderColor: Color,
    icon: @Composable () -> Unit,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 44
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .testTag(testTag)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        bgColor.copy(alpha = 0.95f),
                        bgColor,
                        bgColor.copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.5.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = (size / 2).dp),
                onClick = onClick
            )
    ) {
        icon()
    }
}
