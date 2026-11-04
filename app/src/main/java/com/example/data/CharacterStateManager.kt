package com.example.data

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.example.audio.VoiceEngineMode
import com.example.model.AppLanguage
import com.example.model.AvatarCustomization
import com.example.model.BackgroundTheme
import com.example.model.CharacterCatalog
import com.example.model.ChatMessage
import com.example.model.MoodJournalEntry
import com.example.model.PetCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Domain snapshot representing the restored character and session state on startup.
 */
data class RestoredCharacterState(
    val activeCharacter: PetCharacter,
    val customization: AvatarCustomization,
    val selectedLanguage: AppLanguage,
    val backgroundTheme: BackgroundTheme,
    val voiceEngineMode: VoiceEngineMode,
    val voicePitch: Float,
    val speechRate: Float,
    val affectionScore: Int,
    val coins: Int,
    val diamonds: Int,
    val playerLevel: Int,
    val levelProgress: Float,
    val hungerLevel: Float,
    val toiletLevel: Float,
    val energyLevel: Float,
    val funLevel: Float,
    val quizStreak: Int,
    val pomodoroCompletedSessions: Int,
    val messages: List<ChatMessage> = emptyList(),
    val moodJournalEntries: List<MoodJournalEntry> = emptyList()
)

/**
 * Repository / Manager for persisting and observing the current character's state,
 * appearance customizations (costume, accessory, fur color, eyes, ears, room theme),
 * voice settings, chat history, mood journal, and study companion progress between app sessions using Room.
 */
class CharacterStateManager(
    private val dao: CharacterStateDao
) {
    /**
     * Reactive stream of the global session state.
     */
    val sessionStateFlow: Flow<SessionStateEntity?> = dao.observeSessionState()

    /**
     * Reactive stream of all saved character customizations.
     */
    val allCharacterCustomizationsFlow: Flow<List<CharacterCustomizationEntity>> =
        dao.observeAllCharacterCustomizations()

    /**
     * Reactive stream of saved chat messages.
     */
    val chatMessagesFlow: Flow<List<ChatMessage>> =
        dao.observeRecentChatMessages().map { list -> list.map { it.toDomainModel() } }

    /**
     * Reactive stream of saved mood journal entries.
     */
    val moodJournalEntriesFlow: Flow<List<MoodJournalEntry>> =
        dao.observeMoodJournalEntries().map { list -> list.map { it.toDomainModel() } }

    /**
     * Reactive stream of a specific character's customization and care state.
     */
    fun observeCharacterCustomization(characterId: String): Flow<CharacterCustomizationEntity?> =
        dao.observeCharacterCustomization(characterId)

    /**
     * Loads the saved character state from Room, or seeds initial defaults (migrating legacy
     * SharedPreferences values if present).
     */
    suspend fun loadOrInitializeState(
        legacyCoins: Int = 1500,
        legacyVoiceMode: VoiceEngineMode = VoiceEngineMode.GEMINI_NATURAL,
        legacyMoodEntries: List<MoodJournalEntry> = emptyList()
    ): RestoredCharacterState {
        val existingSession = dao.getSessionStateOnce() ?: SessionStateEntity(
            activeCharacterId = CharacterCatalog.characters.first().id,
            selectedLanguageCode = AppLanguage.INDONESIAN.code,
            backgroundThemeId = BackgroundTheme.LIVING_ROOM.id,
            voiceEngineMode = legacyVoiceMode.name,
            coins = legacyCoins
        ).also { seeded ->
            dao.upsertSessionState(seeded)
        }

        val activeChar = existingSession.resolveActiveCharacter()
        val existingCharEntity = dao.getCharacterCustomizationOnce(activeChar.id)
            ?: CharacterCustomizationEntity.defaultForCharacter(activeChar).also { seededChar ->
                dao.upsertCharacterCustomization(seededChar)
            }

        val savedChatEntities = dao.getRecentChatMessagesOnce()
        val existingMoodEntities = dao.getMoodJournalEntriesOnce()
        val finalMoodEntries = if (existingMoodEntities.isEmpty() && legacyMoodEntries.isNotEmpty()) {
            val entities = legacyMoodEntries.map { MoodJournalEntity.fromDomainModel(it) }
            dao.upsertMoodJournalEntries(entities)
            legacyMoodEntries
        } else {
            existingMoodEntities.map { it.toDomainModel() }
        }

        // Enforce natural voice if previously unset or migrated
        val resolvedVoiceMode = existingSession.resolveVoiceEngineMode()

        return RestoredCharacterState(
            activeCharacter = activeChar,
            customization = existingCharEntity.toAvatarCustomization(activeChar),
            selectedLanguage = existingSession.resolveLanguage(),
            backgroundTheme = existingSession.resolveBackgroundTheme(),
            voiceEngineMode = resolvedVoiceMode,
            voicePitch = existingCharEntity.voicePitch,
            speechRate = existingCharEntity.speechRate,
            affectionScore = existingCharEntity.affectionScore,
            coins = existingSession.coins,
            diamonds = existingSession.diamonds,
            playerLevel = existingSession.playerLevel,
            levelProgress = existingSession.levelProgress,
            hungerLevel = existingCharEntity.hungerLevel,
            toiletLevel = existingCharEntity.toiletLevel,
            energyLevel = existingCharEntity.energyLevel,
            funLevel = existingCharEntity.funLevel,
            quizStreak = existingSession.quizStreak,
            pomodoroCompletedSessions = existingSession.pomodoroCompletedSessions,
            messages = savedChatEntities.map { it.toDomainModel() },
            moodJournalEntries = finalMoodEntries
        )
    }

    /**
     * Loads a specific character's saved customization and care stats when switching characters,
     * creating a default entry for that character if none exists yet.
     */
    suspend fun getOrCreateCharacterCustomization(character: PetCharacter): CharacterCustomizationEntity {
        return dao.getCharacterCustomizationOnce(character.id)
            ?: CharacterCustomizationEntity.defaultForCharacter(character).also { seeded ->
                dao.upsertCharacterCustomization(seeded)
            }
    }

    /**
     * Saves the full current UI state (both global session state and active character's
     * appearance customization & care state) to Room.
     */
    suspend fun saveFullState(
        activeCharacter: PetCharacter,
        customization: AvatarCustomization,
        selectedLanguage: AppLanguage,
        backgroundTheme: BackgroundTheme,
        voiceEngineMode: VoiceEngineMode,
        voicePitch: Float,
        speechRate: Float,
        affectionScore: Int,
        coins: Int,
        diamonds: Int,
        playerLevel: Int,
        levelProgress: Float,
        hungerLevel: Float,
        toiletLevel: Float,
        energyLevel: Float,
        funLevel: Float,
        quizStreak: Int,
        pomodoroCompletedSessions: Int,
        messages: List<ChatMessage>? = null,
        moodJournalEntries: List<MoodJournalEntry>? = null
    ) {
        val now = System.currentTimeMillis()
        dao.upsertSessionState(
            SessionStateEntity(
                id = SessionStateEntity.SINGLETON_ID,
                activeCharacterId = activeCharacter.id,
                selectedLanguageCode = selectedLanguage.code,
                backgroundThemeId = backgroundTheme.id,
                voiceEngineMode = voiceEngineMode.name,
                coins = coins,
                diamonds = diamonds,
                playerLevel = playerLevel,
                levelProgress = levelProgress,
                quizStreak = quizStreak,
                pomodoroCompletedSessions = pomodoroCompletedSessions,
                lastUpdatedMillis = now
            )
        )

        dao.upsertCharacterCustomization(
            CharacterCustomizationEntity(
                characterId = activeCharacter.id,
                furColorId = customization.furColorId,
                furColorArgb = customization.furColor.toArgb(),
                eyeStyleId = customization.eyeStyle.id,
                earStyleId = customization.earStyle.id,
                costumeId = customization.costume.id,
                accessoryId = customization.accessory.id,
                roomThemeId = customization.roomTheme.id,
                voicePitch = voicePitch,
                speechRate = speechRate,
                affectionScore = affectionScore,
                hungerLevel = hungerLevel,
                toiletLevel = toiletLevel,
                energyLevel = energyLevel,
                funLevel = funLevel,
                lastUpdatedMillis = now
            )
        )

        if (messages != null) {
            val trimmed = messages.takeLast(60).map { ChatMessageEntity.fromDomainModel(it, activeCharacter.id) }
            dao.upsertChatMessages(trimmed)
        }

        if (moodJournalEntries != null) {
            val trimmedMoods = moodJournalEntries.take(40).map { MoodJournalEntity.fromDomainModel(it) }
            dao.upsertMoodJournalEntries(trimmedMoods)
        }
    }

    suspend fun clearChatHistory() {
        dao.clearAllChatMessages()
    }

    companion object {
        @Volatile
        private var INSTANCE: CharacterStateManager? = null

        fun getInstance(context: Context): CharacterStateManager {
            return INSTANCE ?: synchronized(this) {
                val database = CharacterStateDatabase.getInstance(context)
                val manager = CharacterStateManager(database.characterStateDao())
                INSTANCE = manager
                manager
            }
        }
    }
}
