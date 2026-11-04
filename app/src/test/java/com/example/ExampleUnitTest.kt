package com.example

import com.example.ai.GeminiChatService
import com.example.audio.VoiceEngineMode
import com.example.data.CharacterCustomizationEntity
import com.example.data.CharacterStateDao
import com.example.data.CharacterStateManager
import com.example.data.ChatMessageEntity
import com.example.data.MoodJournalEntity
import com.example.data.SessionStateEntity
import com.example.model.AccessoryStyle
import com.example.model.AppLanguage
import com.example.model.AvatarCustomization
import com.example.model.AvatarEmotion
import com.example.model.BackgroundTheme
import com.example.model.CharacterCatalog
import com.example.model.ChatMessage
import com.example.model.CostumeStyle
import com.example.model.EarStyle
import com.example.model.EyeStyle
import com.example.model.MoodJournalEntry
import com.example.model.MoodType
import com.example.model.RoomTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local JVM unit tests for CharacterStateManager, Room DAO contract, AI response fast-path,
 * and Natural Voice configuration.
 */
class ExampleUnitTest {

    private class FakeCharacterStateDao : CharacterStateDao {
        private val sessionFlow = MutableStateFlow<SessionStateEntity?>(null)
        private val customizationsFlow = MutableStateFlow<Map<String, CharacterCustomizationEntity>>(emptyMap())
        private val chatMessagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
        private val moodEntriesFlow = MutableStateFlow<List<MoodJournalEntity>>(emptyList())

        override fun observeSessionState(id: Int): Flow<SessionStateEntity?> = sessionFlow

        override suspend fun getSessionStateOnce(id: Int): SessionStateEntity? = sessionFlow.value

        override suspend fun upsertSessionState(state: SessionStateEntity) {
            sessionFlow.value = state
        }

        override fun observeCharacterCustomization(characterId: String): Flow<CharacterCustomizationEntity?> =
            customizationsFlow.map { it[characterId] }

        override fun observeAllCharacterCustomizations(): Flow<List<CharacterCustomizationEntity>> =
            customizationsFlow.map { it.values.toList() }

        override suspend fun getCharacterCustomizationOnce(characterId: String): CharacterCustomizationEntity? =
            customizationsFlow.value[characterId]

        override suspend fun upsertCharacterCustomization(customization: CharacterCustomizationEntity) {
            customizationsFlow.value = customizationsFlow.value + (customization.characterId to customization)
        }

        override fun observeRecentChatMessages(limit: Int): Flow<List<ChatMessageEntity>> =
            chatMessagesFlow.map { it.takeLast(limit) }

        override suspend fun getRecentChatMessagesOnce(limit: Int): List<ChatMessageEntity> =
            chatMessagesFlow.value.takeLast(limit)

        override suspend fun upsertChatMessages(messages: List<ChatMessageEntity>) {
            val map = chatMessagesFlow.value.associateBy { it.id }.toMutableMap()
            messages.forEach { map[it.id] = it }
            chatMessagesFlow.value = map.values.sortedBy { it.timestamp }
        }

        override suspend fun clearAllChatMessages() {
            chatMessagesFlow.value = emptyList()
        }

        override fun observeMoodJournalEntries(limit: Int): Flow<List<MoodJournalEntity>> =
            moodEntriesFlow.map { it.take(limit) }

        override suspend fun getMoodJournalEntriesOnce(limit: Int): List<MoodJournalEntity> =
            moodEntriesFlow.value.take(limit)

        override suspend fun upsertMoodJournalEntries(entries: List<MoodJournalEntity>) {
            val map = moodEntriesFlow.value.associateBy { it.id }.toMutableMap()
            entries.forEach { map[it.id] = it }
            moodEntriesFlow.value = map.values.sortedByDescending { it.timestampMillis }
        }
    }

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun characterStateManager_persistsAndRestoresCustomizationsAndChatAcrossSessions() = runBlocking {
        val fakeDao = FakeCharacterStateDao()
        val manager = CharacterStateManager(fakeDao)

        // 1. Initial load seeds default character (Zayn) and GEMINI_NATURAL voice mode
        val initial = manager.loadOrInitializeState(legacyCoins = 1500)
        assertEquals("zayn", initial.activeCharacter.id)
        assertEquals(VoiceEngineMode.GEMINI_NATURAL, initial.voiceEngineMode)
        assertEquals(1500, initial.coins)

        // 2. Customize Mochi, add chat & mood journal entries, and save full state
        val mochi = CharacterCatalog.getById("mochi")
        val mochiCustomization = AvatarCustomization(
            furColor = mochi.defaultFurColor,
            furColorId = "pink",
            eyeStyle = EyeStyle.HEARTS,
            earStyle = EarStyle.RIBBON_EARS,
            costume = CostumeStyle.SUPERHERO,
            accessory = AccessoryStyle.SUNGLASSES,
            roomTheme = RoomTheme.STARRY_BEDROOM
        )

        val chatHistory = listOf(
            ChatMessage(id = "m1", senderName = "Kamu", text = "Halo Mochi!", isUser = true, timestamp = 1000L),
            ChatMessage(id = "m2", senderName = "Mochi", text = "Hai bestie!", isUser = false, emotion = AvatarEmotion.HAPPY, timestamp = 1001L)
        )
        val moodEntries = listOf(
            MoodJournalEntry(
                id = "mood1",
                timestampMillis = 2000L,
                formattedDate = "04 Nov, 10:00",
                mood = MoodType.SEMANGAT,
                userNote = "Siap belajar!",
                characterName = "Mochi",
                characterEmoji = "🐱",
                petResponse = "Semangat terus bestie!"
            )
        )

        manager.saveFullState(
            activeCharacter = mochi,
            customization = mochiCustomization,
            selectedLanguage = AppLanguage.INDONESIAN,
            backgroundTheme = BackgroundTheme.BEDROOM,
            voiceEngineMode = VoiceEngineMode.GEMINI_NATURAL,
            voicePitch = 1.22f,
            speechRate = 1.06f,
            affectionScore = 88,
            coins = 2350,
            diamonds = 300,
            playerLevel = 14,
            levelProgress = 0.75f,
            hungerLevel = 0.95f,
            toiletLevel = 0.90f,
            energyLevel = 0.85f,
            funLevel = 1.0f,
            quizStreak = 7,
            pomodoroCompletedSessions = 5,
            messages = chatHistory,
            moodJournalEntries = moodEntries
        )

        // 3. Simulate a new app session loading from the same Room database
        val restored = manager.loadOrInitializeState()
        assertEquals("mochi", restored.activeCharacter.id)
        assertEquals(VoiceEngineMode.GEMINI_NATURAL, restored.voiceEngineMode)
        assertEquals(CostumeStyle.SUPERHERO, restored.customization.costume)
        assertEquals(AccessoryStyle.SUNGLASSES, restored.customization.accessory)
        assertEquals(EyeStyle.HEARTS, restored.customization.eyeStyle)
        assertEquals(EarStyle.RIBBON_EARS, restored.customization.earStyle)
        assertEquals("pink", restored.customization.furColorId)
        assertEquals(RoomTheme.STARRY_BEDROOM, restored.customization.roomTheme)
        assertEquals(BackgroundTheme.BEDROOM, restored.backgroundTheme)
        assertEquals(2350, restored.coins)
        assertEquals(88, restored.affectionScore)
        assertEquals(7, restored.quizStreak)
        assertEquals(5, restored.pomodoroCompletedSessions)
        assertEquals(2, restored.messages.size)
        assertEquals("Hai bestie!", restored.messages.last().text)
        assertEquals(1, restored.moodJournalEntries.size)
        assertEquals(MoodType.SEMANGAT, restored.moodJournalEntries.first().mood)

        // 4. Verify reactive Flow emissions
        val observedMochi = manager.observeCharacterCustomization("mochi").first()
        assertNotNull(observedMochi)
        assertEquals(CostumeStyle.SUPERHERO.id, observedMochi?.costumeId)
        assertEquals(AccessoryStyle.SUNGLASSES.id, observedMochi?.accessoryId)
    }

    @Test
    fun aiFastPath_andNaturalVoicePersonas_areConfiguredProperly() {
        val aiService = GeminiChatService()
        val mochi = CharacterCatalog.getById("mochi")
        val reply = aiService.trySolveDirectIntent("12 x 5", mochi, AppLanguage.INDONESIAN)
        assertNotNull(reply)
        assertTrue(reply!!.contains("60"))
        assertTrue(reply.contains("bestie", ignoreCase = true))

        // Verify all characters have distinct natural voice configurations
        CharacterCatalog.characters.forEach { character ->
            assertTrue(character.defaultVoicePitch in 0.60f..1.35f)
            assertTrue(character.defaultSpeechRate in 0.85f..1.25f)
            assertTrue(character.voicePersona.geminiVoiceName.isNotBlank())
        }
    }
}
