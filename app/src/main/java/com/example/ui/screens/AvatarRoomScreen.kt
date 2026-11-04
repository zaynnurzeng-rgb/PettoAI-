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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.SpeechRecognitionHelper
import com.example.ui.AvatarViewModel
import com.example.ui.components.Avatar3DCanvas
import com.example.ui.components.BackgroundThemeDialog
import com.example.ui.components.CharacterPickerDialog
import com.example.ui.components.ChatHistoryDialog
import com.example.ui.components.DynamicBackgroundCanvas
import com.example.ui.components.FaceToFaceCallView
import com.example.ui.components.FlashcardStudySheet
import com.example.ui.components.HomeworkStudyView
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.MoodJournalSheet
import com.example.ui.components.PomodoroStudySheet
import com.example.ui.components.QuizBattleSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TalkingTomTopHud
import com.example.ui.components.VoiceInputDialog
import com.example.ui.components.WardrobeSheet
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
    var showActivityMenu by remember { mutableStateOf(false) }
    var showCharacterPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showChatHistory by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var showChatInput by remember { mutableStateOf(false) }

    val wardrobeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activitySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Dynamic Layered 3D Living Room Background Canvas
            DynamicBackgroundCanvas(
                theme = uiState.backgroundTheme,
                currentTimeOfDay = uiState.currentTimeOfDay,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Main Interactive UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Clean Top HUD: Active Character Switcher, Room Theme, Chat History & Settings
                TalkingTomTopHud(
                    activeCharacter = uiState.activeCharacter,
                    currentThemeIcon = uiState.backgroundTheme.icon,
                    onSwitchCharacterClick = { showCharacterPicker = true },
                    onThemeClick = { showThemeDialog = true },
                    onChatHistoryClick = { showChatHistory = true },
                    onSettingsClick = { showSettings = true }
                )

                // Speech Bubble when character is speaking or replying
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

                // 3D Touchable Character Scene
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
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
                        onMicClick = { viewModel.openVoiceInput() }
                    )
                }

                // Main Streamlined Bottom Dock with 1 Consolidated "Menu Aktivitas" Button
                CleanBottomActionDock(
                    onVideoCallClick = { launchFaceToFaceCall() },
                    onHomeworkClick = { viewModel.openHomeworkMode() },
                    onChatToggle = { showChatInput = !showChatInput },
                    isChatActive = showChatInput,
                    isPomodoroRunning = uiState.isPomodoroRunning,
                    pomodoroRemainingSeconds = uiState.pomodoroRemainingSeconds,
                    onActivityMenuClick = { showActivityMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Consolidated Activity Menu Bottom Sheet
        if (showActivityMenu) {
            ActivityMenuBottomSheet(
                sheetState = activitySheetState,
                isPomodoroRunning = uiState.isPomodoroRunning,
                pomodoroRemainingSeconds = uiState.pomodoroRemainingSeconds,
                onDismiss = {
                    coroutineScope.launch {
                        activitySheetState.hide()
                        showActivityMenu = false
                    }
                },
                onOpenPomodoro = {
                    showActivityMenu = false
                    viewModel.openPomodoroTimer()
                },
                onOpenFlashcard = {
                    showActivityMenu = false
                    viewModel.openFlashcardStudy()
                },
                onOpenQuiz = {
                    showActivityMenu = false
                    viewModel.openQuizBattle()
                },
                onOpenMoodJournal = {
                    showActivityMenu = false
                    viewModel.openMoodJournal()
                },
                onOpenWardrobe = {
                    showActivityMenu = false
                    showWardrobe = true
                    coroutineScope.launch { wardrobeSheetState.show() }
                },
                onOpenRoomTheme = {
                    showActivityMenu = false
                    showThemeDialog = true
                }
            )
        }

        // Live Voice Input Modal Dialog
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

        // Separate Wardrobe (Menu Kostum) Modal Sheet
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
                    viewModel.previewCharacterVoice(char)
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
                onOpenVoiceInput = { viewModel.openVoiceInput() },
                onOpenFlashcardsForTopic = { topic -> viewModel.openFlashcardStudy(topic) }
            )
        }

        // 🎮 Kuis Cerdas Overlay
        AnimatedVisibility(
            visible = uiState.showQuizSheet,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { it })
        ) {
            BackHandler(enabled = uiState.showQuizSheet) {
                viewModel.closeQuizBattle()
            }
            QuizBattleSheet(
                character = uiState.activeCharacter,
                coins = uiState.coins,
                quizStreak = uiState.quizStreak,
                onAnswerQuestion = { q, idx -> viewModel.answerQuizQuestion(q, idx) },
                onSpeakReaction = { viewModel.speakText(it) },
                onClose = { viewModel.closeQuizBattle() }
            )
        }

        // ⏱️ Pomodoro Study Timer Overlay
        AnimatedVisibility(
            visible = uiState.showPomodoroSheet,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { it })
        ) {
            BackHandler(enabled = uiState.showPomodoroSheet) {
                viewModel.closePomodoroTimer()
            }
            PomodoroStudySheet(
                character = uiState.activeCharacter,
                activePreset = uiState.activePomodoroPreset,
                remainingSeconds = uiState.pomodoroRemainingSeconds,
                isRunning = uiState.isPomodoroRunning,
                completedSessions = uiState.pomodoroCompletedSessions,
                onSelectPreset = { viewModel.selectPomodoroPreset(it) },
                onToggleStartPause = { viewModel.togglePomodoroStartPause() },
                onResetTimer = { viewModel.resetPomodoroTimer() },
                onCompleteNowAndClaim = { viewModel.completePomodoroAndClaimReward() },
                onClose = { viewModel.closePomodoroTimer() }
            )
        }

        // 📓 Jurnal Mood Harian & Curhat Tracker Overlay
        AnimatedVisibility(
            visible = uiState.showMoodJournalSheet,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { it })
        ) {
            BackHandler(enabled = uiState.showMoodJournalSheet) {
                viewModel.closeMoodJournal()
            }
            MoodJournalSheet(
                character = uiState.activeCharacter,
                entries = uiState.moodJournalEntries,
                isSavingMood = uiState.isSavingMoodEntry,
                onSubmitMoodEntry = { mood, note -> viewModel.submitMoodJournalEntry(mood, note) },
                onSpeakResponse = { viewModel.speakText(it) },
                onClose = { viewModel.closeMoodJournal() }
            )
        }

        // 🃏 Flashcard Otomatis dari Materi Sekolah Overlay
        AnimatedVisibility(
            visible = uiState.showFlashcardSheet,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { it })
        ) {
            BackHandler(enabled = uiState.showFlashcardSheet) {
                viewModel.closeFlashcardStudy()
            }
            FlashcardStudySheet(
                character = uiState.activeCharacter,
                selectedSubject = uiState.homeworkSubject,
                flashcards = uiState.flashcards,
                isGenerating = uiState.isGeneratingFlashcards,
                onSelectSubject = { viewModel.selectFlashcardSubject(it) },
                onGenerateCustomTopic = { viewModel.generateFlashcards(it) },
                onMarkCardMastered = { viewModel.markFlashcardMastered(it) },
                onSpeakCard = { viewModel.speakText(it) },
                onClose = { viewModel.closeFlashcardStudy() }
            )
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
            // Voice Dictation Button
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
                    contentDescription = "Dikte Suara",
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
                        "Ketik pesan ke sahabat AI...",
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
 * Streamlined Bottom Action Dock with 4 clear actions:
 * 1. Video Call (Live Voice/Video Call)
 * 2. Tugas (Scan PR & Tanya Tugas)
 * 3. Chat (Toggle Text & Voice Dictation Bar)
 * 4. Menu Aktivitas (Opens consolidated study & companion activity sheet)
 */
@Composable
private fun CleanBottomActionDock(
    onVideoCallClick: () -> Unit,
    onHomeworkClick: () -> Unit,
    onChatToggle: () -> Unit,
    isChatActive: Boolean,
    isPomodoroRunning: Boolean,
    pomodoroRemainingSeconds: Int,
    onActivityMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val menuLabel = if (isPomodoroRunning) {
        val m = pomodoroRemainingSeconds / 60
        val s = pomodoroRemainingSeconds % 60
        String.format("⏱️ %02d:%02d", m, s)
    } else {
        "Aktivitas"
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xD90F172A),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
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
            // 1. Live Video Call
            Button(
                onClick = onVideoCallClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
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
            Button(
                onClick = onChatToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isChatActive) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.testTag("action_chat_toggle_button")
            ) {
                Text(text = "💬", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Chat",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4. Consolidated Menu Aktivitas Button
            Button(
                onClick = onActivityMenuClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPomodoroRunning) Color(0xFF06B6D4) else Color(0xFF334155),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.testTag("action_activity_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Menu Aktivitas",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = menuLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class ActivityMenuItem(
    val tag: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ActivityMenuBottomSheet(
    sheetState: androidx.compose.material3.SheetState,
    isPomodoroRunning: Boolean,
    pomodoroRemainingSeconds: Int,
    onDismiss: () -> Unit,
    onOpenPomodoro: () -> Unit,
    onOpenFlashcard: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenMoodJournal: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenRoomTheme: () -> Unit
) {
    val pomodoroSubtitle = if (isPomodoroRunning) {
        val m = pomodoroRemainingSeconds / 60
        val s = pomodoroRemainingSeconds % 60
        String.format("Sedang berjalan • %02d:%02d", m, s)
    } else {
        "Timer fokus belajar bareng karakter"
    }

    val items = listOf(
        ActivityMenuItem(
            tag = "menu_item_pomodoro",
            emoji = "⏱️",
            title = "Timer Pomodoro",
            subtitle = pomodoroSubtitle,
            accentColor = Color(0xFF06B6D4),
            onClick = onOpenPomodoro
        ),
        ActivityMenuItem(
            tag = "menu_item_flashcard",
            emoji = "🃏",
            title = "Flashcard Belajar",
            subtitle = "Kartu hafalan otomatis materi sekolah",
            accentColor = Color(0xFFF59E0B),
            onClick = onOpenFlashcard
        ),
        ActivityMenuItem(
            tag = "menu_item_quiz",
            emoji = "🎮",
            title = "Kuis Cerdas AI",
            subtitle = "Tebak-tebakan & latihan soal seru",
            accentColor = Color(0xFF8B5CF6),
            onClick = onOpenQuiz
        ),
        ActivityMenuItem(
            tag = "menu_item_mood",
            emoji = "📓",
            title = "Jurnal Mood",
            subtitle = "Catat perasaan & curhat harian",
            accentColor = Color(0xFFEC4899),
            onClick = onOpenMoodJournal
        ),
        ActivityMenuItem(
            tag = "menu_item_wardrobe",
            emoji = "🧥",
            title = "Menu Kostum",
            subtitle = "Pilih kostum & aksesoris secara terpisah",
            accentColor = Color(0xFF6366F1),
            onClick = onOpenWardrobe
        ),
        ActivityMenuItem(
            tag = "menu_item_theme",
            emoji = "🛋️",
            title = "Tema Ruangan",
            subtitle = "Ganti suasana ruang tamu, kamar, atau dapur",
            accentColor = Color(0xFF14B8A6),
            onClick = onOpenRoomTheme
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("activity_menu_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Menu Aktivitas & Pendamping",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Pilih aktivitas belajar, hiburan, atau tampilan",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8)
                        )
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2,
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.2.dp, item.accentColor.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = item.onClick)
                            .testTag(item.tag)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(item.accentColor.copy(alpha = 0.2f))
                            ) {
                                Text(text = item.emoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
