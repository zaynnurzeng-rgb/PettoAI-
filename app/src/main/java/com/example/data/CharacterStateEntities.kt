package com.example.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.audio.VoiceEngineMode
import com.example.model.AccessoryStyle
import com.example.model.AppLanguage
import com.example.model.AvatarCustomization
import com.example.model.AvatarEmotion
import com.example.model.BackgroundTheme
import com.example.model.CharacterCatalog
import com.example.model.ChatMessage
import com.example.model.CostumeStyle
import com.example.model.CustomizationCatalog
import com.example.model.EarStyle
import com.example.model.EyeStyle
import com.example.model.MoodJournalEntry
import com.example.model.MoodType
import com.example.model.PetCharacter
import com.example.model.PetSpecies
import com.example.model.RoomTheme

/**
 * Room Entity storing global app session state & active character selection.
 */
@Entity(tableName = "session_state")
data class SessionStateEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val activeCharacterId: String = CharacterCatalog.characters.first().id,
    val selectedLanguageCode: String = AppLanguage.INDONESIAN.code,
    val backgroundThemeId: String = BackgroundTheme.LIVING_ROOM.id,
    val voiceEngineMode: String = VoiceEngineMode.GEMINI_NATURAL.name,
    val coins: Int = 1500,
    val diamonds: Int = 240,
    val playerLevel: Int = 12,
    val levelProgress: Float = 0.65f,
    val quizStreak: Int = 0,
    val pomodoroCompletedSessions: Int = 0,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 1
    }

    fun resolveActiveCharacter(): PetCharacter =
        CharacterCatalog.getById(activeCharacterId)

    fun resolveLanguage(): AppLanguage =
        AppLanguage.entries.firstOrNull { it.code == selectedLanguageCode } ?: AppLanguage.INDONESIAN

    fun resolveBackgroundTheme(): BackgroundTheme =
        BackgroundTheme.entries.firstOrNull { it.id == backgroundThemeId } ?: BackgroundTheme.LIVING_ROOM

    fun resolveVoiceEngineMode(): VoiceEngineMode =
        try {
            VoiceEngineMode.valueOf(voiceEngineMode)
        } catch (_: Exception) {
            VoiceEngineMode.GEMINI_NATURAL
        }
}

/**
 * Room Entity storing per-character appearance customizations (costume, accessory, fur color,
 * eye style, ear style, room theme) and character care/affection state across app sessions.
 */
@Entity(tableName = "character_customizations")
data class CharacterCustomizationEntity(
    @PrimaryKey
    val characterId: String,
    val furColorId: String,
    val furColorArgb: Int,
    val eyeStyleId: String,
    val earStyleId: String,
    val costumeId: String,
    val accessoryId: String,
    val roomThemeId: String,
    val voicePitch: Float,
    val speechRate: Float,
    val affectionScore: Int = 20,
    val hungerLevel: Float = 0.85f,
    val toiletLevel: Float = 0.70f,
    val energyLevel: Float = 0.60f,
    val funLevel: Float = 0.90f,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    fun toAvatarCustomization(character: PetCharacter): AvatarCustomization {
        val matchedColorOption = CustomizationCatalog.furColors.firstOrNull { it.id == furColorId }
        val resolvedColor = matchedColorOption?.color
            ?: if (furColorArgb != 0) Color(furColorArgb) else character.defaultFurColor

        val resolvedEye = EyeStyle.entries.firstOrNull { it.id == eyeStyleId } ?: EyeStyle.SPARKLING
        val defaultEar = if (character.species == PetSpecies.DOG) EarStyle.DOG_FLOPPY else EarStyle.CAT_POINTY
        val resolvedEar = EarStyle.entries.firstOrNull { it.id == earStyleId } ?: defaultEar
        val resolvedCostume = CostumeStyle.entries.firstOrNull { it.id == costumeId } ?: CostumeStyle.NONE
        val resolvedAccessory = AccessoryStyle.entries.firstOrNull { it.id == accessoryId } ?: AccessoryStyle.BELL_COLLAR
        val resolvedRoomTheme = RoomTheme.entries.firstOrNull { it.id == roomThemeId } ?: RoomTheme.WARM_LIVING_ROOM

        return AvatarCustomization(
            furColor = resolvedColor,
            furColorId = furColorId,
            eyeStyle = resolvedEye,
            earStyle = resolvedEar,
            costume = resolvedCostume,
            accessory = resolvedAccessory,
            roomTheme = resolvedRoomTheme
        )
    }

    companion object {
        fun defaultForCharacter(
            character: PetCharacter,
            customization: AvatarCustomization? = null
        ): CharacterCustomizationEntity {
            val defaultEar = if (character.species == PetSpecies.DOG) EarStyle.DOG_FLOPPY else EarStyle.CAT_POINTY
            val custom = customization ?: AvatarCustomization(
                furColor = character.defaultFurColor,
                furColorId = CustomizationCatalog.furColors.firstOrNull {
                    it.color == character.defaultFurColor
                }?.id ?: "orange",
                earStyle = defaultEar
            )
            return CharacterCustomizationEntity(
                characterId = character.id,
                furColorId = custom.furColorId,
                furColorArgb = custom.furColor.toArgb(),
                eyeStyleId = custom.eyeStyle.id,
                earStyleId = custom.earStyle.id,
                costumeId = custom.costume.id,
                accessoryId = custom.accessory.id,
                roomThemeId = custom.roomTheme.id,
                voicePitch = character.defaultVoicePitch,
                speechRate = character.defaultSpeechRate
            )
        }
    }
}

/**
 * Room Entity storing chat messages per character so conversations persist across sessions.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val characterId: String,
    val senderName: String,
    val text: String,
    val isUser: Boolean,
    val emotionName: String = AvatarEmotion.IDLE.name,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): ChatMessage {
        val resolvedEmotion = try {
            AvatarEmotion.valueOf(emotionName)
        } catch (_: Exception) {
            AvatarEmotion.IDLE
        }
        return ChatMessage(
            id = id,
            senderName = senderName,
            text = text,
            isUser = isUser,
            emotion = resolvedEmotion,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomainModel(message: ChatMessage, characterId: String): ChatMessageEntity {
            return ChatMessageEntity(
                id = message.id,
                characterId = characterId,
                senderName = message.senderName,
                text = message.text,
                isUser = message.isUser,
                emotionName = message.emotion.name,
                timestamp = message.timestamp
            )
        }
    }
}

/**
 * Room Entity storing daily mood journal entries across app sessions.
 */
@Entity(tableName = "mood_journal_entries")
data class MoodJournalEntity(
    @PrimaryKey
    val id: String,
    val timestampMillis: Long,
    val formattedDate: String,
    val moodId: String,
    val userNote: String,
    val characterName: String,
    val characterEmoji: String,
    val petResponse: String
) {
    fun toDomainModel(): MoodJournalEntry {
        val resolvedMood = MoodType.entries.firstOrNull { it.id == moodId } ?: MoodType.SENANG
        return MoodJournalEntry(
            id = id,
            timestampMillis = timestampMillis,
            formattedDate = formattedDate,
            mood = resolvedMood,
            userNote = userNote,
            characterName = characterName,
            characterEmoji = characterEmoji,
            petResponse = petResponse
        )
    }

    companion object {
        fun fromDomainModel(entry: MoodJournalEntry): MoodJournalEntity {
            return MoodJournalEntity(
                id = entry.id,
                timestampMillis = entry.timestampMillis,
                formattedDate = entry.formattedDate,
                moodId = entry.mood.id,
                userNote = entry.userNote,
                characterName = entry.characterName,
                characterEmoji = entry.characterEmoji,
                petResponse = entry.petResponse
            )
        }
    }
}
