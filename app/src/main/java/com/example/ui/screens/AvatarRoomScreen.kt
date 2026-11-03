package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.SpeechRecognitionHelper
import com.example.model.AppLanguage
import com.example.ui.AvatarViewModel
import com.example.model.CareOverlayType
import com.example.ui.components.Avatar3DCanvas
import com.example.ui.components.BackgroundThemeDialog
import com.example.ui.components.CharacterPickerDialog
import com.example.ui.components.ChatHistoryDialog
import com.example.ui.components.DynamicBackgroundCanvas
import com.example.ui.components.FaceToFaceCallView
import com.example.ui.components.LiquidGlassDefaults
import com.example.ui.components.LiquidGlassIconButton
import com.example.ui.components.LiquidGlassPillButton
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.SettingsDialog
import com.example.ui.components.VoiceInputDialog
import com.example.ui.components.WardrobeSheet
import com.example.ui.components.TalkingTomTopHud
import com.example.ui.components.HomeworkStudyView
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarRoomScreen(
    viewModel: AvatarViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showWardrobe by remember { mutableStateOf(false) }
    var showCharacterPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showChatHistory by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var isSleepLightsOff by remember { mutableStateOf(false) }
    var showChatInput by remember { mutableStateOf(false) }

    val wardrobeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Fallback system speech launcher (Google Voice Typing)
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                viewModel.onFallbackSpeechResult(matches[0])
            }
        }
    }

    LaunchedEffect(uiState.triggerSystemSpeechFallback) {
        if (uiState.triggerSystemSpeechFallback) {
            try {
                systemSpeechLauncher.launch(
                    SpeechRecognitionHelper.createSystemSpeechIntent(uiState.selectedLanguage)
                )
            } catch (e: Exception) {
                Log.e("AvatarRoomScreen", "Failed to launch system speech", e)
            }
            viewModel.onSystemSpeechFallbackConsumed()
        }
    }

    // Permission launcher for microphone recording
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startFaceToFaceCall()
        }
    }

    val launchFaceToFaceCall: () -> Unit = {
        try {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                viewModel.startFaceToFaceCall()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        } catch (e: Exception) {
            Log.e("AvatarRoomScreen", "Error launching face-to-face call", e)
            viewModel.startFaceToFaceCall()
        }
    }

    val quickChips = when (uiState.selectedLanguage) {
        AppLanguage.INDONESIAN -> listOf(
            "📞 Tatap Muka Langsung",
            "🛋️ Ruang Tamu",
            "🛏️ Kamar Tidur",
            "🍳 Dapur",
            "📢 Hey Petto!",
            "5 x 9 apa?",
            "Lagi ngapain kamu?",
            "Ceritakan lelucon!"
        )
        AppLanguage.ENGLISH -> listOf(
            "📞 Face-to-Face Call",
            "🛋️ Living Room",
            "🛏️ Bedroom",
            "🍳 Kitchen",
            "📢 Hey Petto!",
            "What is 5 x 9?",
            "What are you doing?",
            "Tell me a joke!"
        )
        AppLanguage.JAPANESE -> listOf(
            "📢 Hey Petto!",
            "Halo Petto!",
            "何してるの？",
            "面白い話して！"
        )
        AppLanguage.SPANISH -> listOf(
            "📢 Hey Petto!",
            "Halo Petto!",
            "¿Qué estás haciendo?",
            "¡Cuéntame un chiste!"
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Dynamic Layered 3D Living Room Background Canvas
            DynamicBackgroundCanvas(
                theme = uiState.backgroundTheme,
                currentTimeOfDay = uiState.currentTimeOfDay,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Main Talking Tom Interactive UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top HUD: Level Badge, Coins, Diamonds, Settings & Shop
                TalkingTomTopHud(
                    level = uiState.playerLevel,
                    levelProgress = uiState.levelProgress,
                    coins = uiState.coins,
                    diamonds = uiState.diamonds,
                    onLevelClick = { showCharacterPicker = true },
                    onSettingsClick = { showSettings = true },
                    onShopClick = {
                        showWardrobe = true
                        coroutineScope.launch { wardrobeSheetState.show() }
                    }
                )

                // Speech Bubble when Tom is speaking or replying
                if (uiState.lastAvatarSpeech.isNotBlank() || uiState.isTalking || uiState.isLoadingAi) {
                    LiquidGlassSpeechBubble(
                        speakerName = uiState.activeCharacter.name,
                        speakerEmoji = uiState.activeCharacter.species.emoji,
                        text = uiState.lastAvatarSpeech,
                        isTalking = uiState.isTalking,
                        isLoading = uiState.isLoadingAi,
                        onReplay = { viewModel.replayCurrentSpeech() }
                    )
                }

                // 3D Touchable Character Scene (Clean Room without clutter)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Alive 3D Character standing on the carpet
                    Avatar3DCanvas(
                        species = uiState.activeCharacter.species,
                        customization = uiState.customization,
                        emotion = uiState.currentEmotion,
                        isTalking = uiState.isTalking,
                        onZoneTouched = { zone -> viewModel.onCharacterTouched(zone) },
                        backgroundTheme = uiState.backgroundTheme,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("avatar_interactive_canvas")
                    )
                }

                // Optional expandable text chat input bar
                if (showChatInput) {
                    LiquidGlassBottomChatInputBar(
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        isListeningMic = uiState.isListeningMic,
                        isLoading = uiState.isLoadingAi,
                        onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        onMicClick = { launchFaceToFaceCall() }
                    )
                }

                // Clean Streamlined Bottom Action Dock: Video Call Live, Mode Tugas Sekolah, Chat, Bicara
                CleanBottomActionDock(
                    onVideoCallClick = { launchFaceToFaceCall() },
                    onHomeworkClick = { viewModel.openHomeworkMode() },
                    onChatToggle = { showChatInput = !showChatInput },
                    isChatActive = showChatInput,
                    onMicClick = { launchFaceToFaceCall() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Live Voice Input Modal Dialog with Crash Protection
        if (uiState.showVoiceDialog) {
            VoiceInputDialog(
                isListening = uiState.isListeningMic,
                liveTranscript = uiState.speechTranscript,
                rmsLevel = uiState.speechRms,
                characterName = uiState.activeCharacter.name,
                isServiceUnavailable = uiState.speechServiceUnavailable,
                onLaunchSystemSpeech = {
                    try {
                        systemSpeechLauncher.launch(
                            SpeechRecognitionHelper.createSystemSpeechIntent(uiState.selectedLanguage)
                        )
                    } catch (e: Exception) {
                        Log.e("AvatarRoomScreen", "Failed to launch system speech", e)
                    }
                },
                onSendTranscript = { text ->
                    viewModel.sendVoiceTranscript(text)
                },
                onCancel = {
                    viewModel.closeVoiceInput()
                }
            )
        }

        // Modals & Sheets
        if (showWardrobe) {
            WardrobeSheet(
                sheetState = wardrobeSheetState,
                customization = uiState.customization,
                onCustomizationChanged = { viewModel.updateCustomization(it) },
                onDismiss = {
                    coroutineScope.launch {
                        wardrobeSheetState.hide()
                        showWardrobe = false
                    }
                }
            )
        }

        if (showThemeDialog) {
            BackgroundThemeDialog(
                currentTheme = uiState.backgroundTheme,
                currentTimeOfDay = uiState.currentTimeOfDay,
                onSelectTheme = { theme ->
                    viewModel.setBackgroundTheme(theme)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        if (showCharacterPicker) {
            CharacterPickerDialog(
                currentCharacter = uiState.activeCharacter,
                onSelectCharacter = { char ->
                    viewModel.selectCharacter(char)
                    showCharacterPicker = false
                },
                onPreviewVoice = { char ->
                    val sampleLine = when (char.id) {
                        "zayn" -> "Hah?! Lu mau denger suara gua?! B-bukan berarti gua mau ngomong sama lu ya!"
                        "siti" -> "Halo sayangku, Siti si kelinci manis selalu siap mendengarkanmu dengan penuh kehangatan."
                        "rizky" -> "Siap! Laporan vokal Rizky terdengar tegas, jelas, dan siap bertugas!"
                        "jamal" -> "Hoaaammm... Kapibara lagi mager ngomong nih bro, tapi buat lu gua sapa deh."
                        else -> "HEH! Lu ngapain nyuruh-nyuruh beruang ngomong?! Berisik amat lu jangan bikin emosi!"
                    }
                    viewModel.speakText(sampleLine)
                },
                onDismiss = { showCharacterPicker = false }
            )
        }

        if (showSettings) {
            SettingsDialog(
                selectedLanguage = uiState.selectedLanguage,
                onLanguageSelected = { viewModel.setLanguage(it) },
                voicePitch = uiState.voicePitch,
                onPitchChanged = { viewModel.setVoicePitch(it) },
                speechRate = uiState.speechRate,
                onSpeechRateChanged = { viewModel.setSpeechRate(it) },
                onTestVoice = { viewModel.testVoice() },
                onClearHistory = {
                    viewModel.clearHistory()
                    showSettings = false
                },
                isGeminiConnected = viewModel.isGeminiConnected(),
                currentApiKey = viewModel.getCustomApiKey(),
                onApiKeySaved = { newKey -> viewModel.setCustomApiKey(newKey) },
                voiceEngineMode = uiState.voiceEngineMode,
                onVoiceEngineModeSelected = { viewModel.setVoiceEngineMode(it) },
                elevenLabsApiKey = viewModel.getElevenLabsApiKey(),
                onElevenLabsApiKeySaved = { viewModel.setElevenLabsApiKey(it) },
                googleCloudTtsApiKey = viewModel.getGoogleCloudTtsApiKey(),
                onGoogleCloudTtsApiKeySaved = { viewModel.setGoogleCloudTtsApiKey(it) },
                onDismiss = { showSettings = false }
            )
        }

        if (showChatHistory) {
            ChatHistoryDialog(
                messages = uiState.messages,
                onSpeakMessage = { viewModel.speakText(it) },
                onDismiss = { showChatHistory = false }
            )
        }

        // Fullscreen Face-to-Face Live Conversational Call Overlay
        AnimatedVisibility(
            visible = uiState.isFaceToFaceActive,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { it })
        ) {
            BackHandler(enabled = uiState.isFaceToFaceActive) {
                viewModel.endFaceToFaceCall()
            }

            FaceToFaceCallView(
                uiState = uiState,
                onEndCall = { viewModel.endFaceToFaceCall() },
                onToggleMute = { viewModel.toggleFaceToFaceMute() },
                onInterruptOrSpeak = { viewModel.interruptOrSpeak() },
                onQuickPrompt = { prompt -> viewModel.quickPromptFaceToFace(prompt) },
                onSwitchCharacter = { char -> viewModel.switchCharacterInFaceToFace(char) },
                onPetAvatar = { viewModel.petAvatar() }
            )
        }

        // Fullscreen Mode Ngerjain Tugas Sekolah Overlay
        AnimatedVisibility(
            visible = uiState.showHomeworkMode,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { it })
        ) {
            BackHandler(enabled = uiState.showHomeworkMode) {
                viewModel.closeHomeworkMode()
            }

            HomeworkStudyView(
                character = uiState.activeCharacter,
                selectedSubject = uiState.homeworkSubject,
                questionText = uiState.homeworkQuestion,
                solution = uiState.homeworkSolution,
                isSolving = uiState.isSolvingHomework,
                photoBitmap = uiState.homeworkPhotoBitmap,
                onSelectSubject = { viewModel.setHomeworkSubject(it) },
                onQuestionChange = { viewModel.setHomeworkQuestion(it) },
                onSetPhoto = { viewModel.setHomeworkPhoto(it) },
                onSolve = { viewModel.solveHomework(it) },
                onSpeakExplanation = { viewModel.speakText(it) },
                onClose = { viewModel.closeHomeworkMode() },
                onOpenVoiceInput = { viewModel.openVoiceInput() }
            )
        }
    }
}

/**
 * Modern Liquid Glass Top Bar
 */
@Composable
private fun LiquidGlassTopRoomBar(
    characterName: String,
    characterSpeciesEmoji: String,
    speciesTitle: String,
    affectionScore: Int,
    currentThemeIcon: String,
    onOpenCharacters: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenChatHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    LiquidGlassSurface(
        shape = RoundedCornerShape(26.dp),
        backgroundColor = Color(0x38151D33),
        elevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("top_room_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Character quick pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenCharacters)
                    .background(Color(0x30FFFFFF))
                    .border(BorderStroke(1.dp, Color(0x40FFFFFF)), RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("switch_character_button")
            ) {
                Text(text = characterSpeciesEmoji, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = characterName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Ganti >",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = Color(0xFF93C5FD)
                        )
                    )
                }
            }

            // Theme Switcher Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenThemes)
                    .background(Color(0x30FFFFFF))
                    .border(BorderStroke(1.dp, Color(0x40FFFFFF)), RoundedCornerShape(16.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp)
                    .testTag("open_theme_button")
            ) {
                Text(text = currentThemeIcon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Ruangan",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Affection score pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x44E91E63))
                    .border(BorderStroke(1.dp, Color(0x66FF80AB)), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Affection",
                    tint = Color(0xFFFF80AB),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$affectionScore%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            // Action icon buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenWardrobe,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("open_wardrobe_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = "Wardrobe",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onOpenChatHistory,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("open_chat_history_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Riwayat Chat",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Modern Floating Liquid Glass Speech Bubble
 */
@Composable
private fun LiquidGlassSpeechBubble(
    speakerName: String,
    speakerEmoji: String,
    text: String,
    isTalking: Boolean,
    isLoading: Boolean,
    onReplay: () -> Unit
) {
    if (text.isBlank() && !isLoading) return

    LiquidGlassSurface(
        shape = RoundedCornerShape(22.dp),
        backgroundColor = Color(0x40131B2E),
        elevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("speech_bubble_view")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = speakerEmoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = speakerName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD)
                        )
                    )
                    if (isTalking) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "● Berbicara...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onReplay,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Putar ulang suara",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF93C5FD)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sedang berpikir...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp
                        )
                    )
                }
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}

/**
 * Modern Liquid Glass Bottom Chat Input Bar
 */
@Composable
private fun LiquidGlassBottomChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    isListeningMic: Boolean,
    isLoading: Boolean,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListeningMic) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    LiquidGlassSurface(
        shape = RoundedCornerShape(26.dp),
        backgroundColor = Color(0x40131B2E),
        elevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("bottom_chat_input_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Microphone live call launcher
            FloatingActionButton(
                onClick = onMicClick,
                shape = CircleShape,
                containerColor = if (isListeningMic) Color(0xFFEF4444) else Color(0xFF38BDF8),
                contentColor = Color.White,
                modifier = Modifier
                    .size(46.dp)
                    .scale(micScale)
                    .testTag("microphone_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Bicara via Mikrofon",
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Liquid Glass text field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        "Ketik pesan ke sahabat 3D...",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF818CF8),
                    unfocusedBorderColor = Color(0x35FFFFFF),
                    focusedContainerColor = Color(0x280F172A),
                    unfocusedContainerColor = Color(0x200F172A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send button
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLoading) Color(0x606366F1)
                        else if (inputText.isNotBlank()) Color(0xFF6366F1)
                        else Color(0x20FFFFFF)
                    )
                    .testTag("send_message_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim",
                        tint = if (inputText.isNotBlank()) Color.White else Color(0x60FFFFFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Streamlined Clean Bottom Action Dock:
 * - Live Video Call with Front Camera PiP
 * - Mode Ngerjain Tugas Sekolah (Homework & Study Companion)
 * - Keyboard Text Chat Toggle
 * - Fast Mic Dictation
 */
@Composable
private fun CleanBottomActionDock(
    onVideoCallClick: () -> Unit,
    onHomeworkClick: () -> Unit,
    onChatToggle: () -> Unit,
    isChatActive: Boolean,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xD90F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        shadowElevation = 12.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            // 1. Live Video Call (WhatsApp style)
            Button(
                onClick = onVideoCallClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.testTag("action_video_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Video Call",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Mode Ngerjain Tugas Sekolah
            Button(
                onClick = onHomeworkClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.testTag("action_homework_button")
            ) {
                Text(text = "📚", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tugas",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 3. Chat Keyboard Toggle
            IconButton(
                onClick = onChatToggle,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isChatActive) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.12f))
                    .testTag("action_chat_toggle_button")
            ) {
                Text(text = "💬", fontSize = 16.sp)
            }

            // 4. Quick Voice Mic
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.85f))
                    .testTag("action_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Bicara",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
