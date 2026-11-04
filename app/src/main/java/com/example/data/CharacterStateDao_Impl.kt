package com.example.data

import android.annotation.SuppressLint
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@SuppressLint("RestrictedApi")
@Suppress("UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "RestrictedApi")
public class CharacterStateDao_Impl(
    __db: RoomDatabase,
) : CharacterStateDao {
    private val __db: RoomDatabase

    private val __insertAdapterOfSessionStateEntity: EntityInsertAdapter<SessionStateEntity>

    private val __insertAdapterOfCharacterCustomizationEntity:
        EntityInsertAdapter<CharacterCustomizationEntity>

    private val __insertAdapterOfChatMessageEntity: EntityInsertAdapter<ChatMessageEntity>

    private val __insertAdapterOfMoodJournalEntity: EntityInsertAdapter<MoodJournalEntity>

    init {
        this.__db = __db
        this.__insertAdapterOfSessionStateEntity = object : EntityInsertAdapter<SessionStateEntity>() {
            protected override fun createQuery(): String =
                "INSERT OR REPLACE INTO `session_state` (`id`,`activeCharacterId`,`selectedLanguageCode`,`backgroundThemeId`,`voiceEngineMode`,`coins`,`diamonds`,`playerLevel`,`levelProgress`,`quizStreak`,`pomodoroCompletedSessions`,`lastUpdatedMillis`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"

            protected override fun bind(statement: SQLiteStatement, entity: SessionStateEntity) {
                statement.bindLong(1, entity.id.toLong())
                statement.bindText(2, entity.activeCharacterId)
                statement.bindText(3, entity.selectedLanguageCode)
                statement.bindText(4, entity.backgroundThemeId)
                statement.bindText(5, entity.voiceEngineMode)
                statement.bindLong(6, entity.coins.toLong())
                statement.bindLong(7, entity.diamonds.toLong())
                statement.bindLong(8, entity.playerLevel.toLong())
                statement.bindDouble(9, entity.levelProgress.toDouble())
                statement.bindLong(10, entity.quizStreak.toLong())
                statement.bindLong(11, entity.pomodoroCompletedSessions.toLong())
                statement.bindLong(12, entity.lastUpdatedMillis)
            }
        }
        this.__insertAdapterOfCharacterCustomizationEntity = object :
            EntityInsertAdapter<CharacterCustomizationEntity>() {
            protected override fun createQuery(): String =
                "INSERT OR REPLACE INTO `character_customizations` (`characterId`,`furColorId`,`furColorArgb`,`eyeStyleId`,`earStyleId`,`costumeId`,`accessoryId`,`roomThemeId`,`voicePitch`,`speechRate`,`affectionScore`,`hungerLevel`,`toiletLevel`,`energyLevel`,`funLevel`,`lastUpdatedMillis`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

            protected override fun bind(
                statement: SQLiteStatement,
                entity: CharacterCustomizationEntity
            ) {
                statement.bindText(1, entity.characterId)
                statement.bindText(2, entity.furColorId)
                statement.bindLong(3, entity.furColorArgb.toLong())
                statement.bindText(4, entity.eyeStyleId)
                statement.bindText(5, entity.earStyleId)
                statement.bindText(6, entity.costumeId)
                statement.bindText(7, entity.accessoryId)
                statement.bindText(8, entity.roomThemeId)
                statement.bindDouble(9, entity.voicePitch.toDouble())
                statement.bindDouble(10, entity.speechRate.toDouble())
                statement.bindLong(11, entity.affectionScore.toLong())
                statement.bindDouble(12, entity.hungerLevel.toDouble())
                statement.bindDouble(13, entity.toiletLevel.toDouble())
                statement.bindDouble(14, entity.energyLevel.toDouble())
                statement.bindDouble(15, entity.funLevel.toDouble())
                statement.bindLong(16, entity.lastUpdatedMillis)
            }
        }
        this.__insertAdapterOfChatMessageEntity = object : EntityInsertAdapter<ChatMessageEntity>() {
            protected override fun createQuery(): String =
                "INSERT OR REPLACE INTO `chat_messages` (`id`,`characterId`,`senderName`,`text`,`isUser`,`emotionName`,`timestamp`) VALUES (?,?,?,?,?,?,?)"

            protected override fun bind(statement: SQLiteStatement, entity: ChatMessageEntity) {
                statement.bindText(1, entity.id)
                statement.bindText(2, entity.characterId)
                statement.bindText(3, entity.senderName)
                statement.bindText(4, entity.text)
                val _tmp: Int = if (entity.isUser) 1 else 0
                statement.bindLong(5, _tmp.toLong())
                statement.bindText(6, entity.emotionName)
                statement.bindLong(7, entity.timestamp)
            }
        }
        this.__insertAdapterOfMoodJournalEntity = object : EntityInsertAdapter<MoodJournalEntity>() {
            protected override fun createQuery(): String =
                "INSERT OR REPLACE INTO `mood_journal_entries` (`id`,`timestampMillis`,`formattedDate`,`moodId`,`userNote`,`characterName`,`characterEmoji`,`petResponse`) VALUES (?,?,?,?,?,?,?,?)"

            protected override fun bind(statement: SQLiteStatement, entity: MoodJournalEntity) {
                statement.bindText(1, entity.id)
                statement.bindLong(2, entity.timestampMillis)
                statement.bindText(3, entity.formattedDate)
                statement.bindText(4, entity.moodId)
                statement.bindText(5, entity.userNote)
                statement.bindText(6, entity.characterName)
                statement.bindText(7, entity.characterEmoji)
                statement.bindText(8, entity.petResponse)
            }
        }
    }

    public override suspend fun upsertSessionState(state: SessionStateEntity): Unit =
        performSuspending(__db, false, true) { _connection ->
            __insertAdapterOfSessionStateEntity.insert(_connection, state)
        }

    public override suspend fun upsertCharacterCustomization(
        customization: CharacterCustomizationEntity
    ): Unit = performSuspending(__db, false, true) { _connection ->
        __insertAdapterOfCharacterCustomizationEntity.insert(_connection, customization)
    }

    public override suspend fun upsertChatMessages(messages: List<ChatMessageEntity>): Unit =
        performSuspending(__db, false, true) { _connection ->
            __insertAdapterOfChatMessageEntity.insert(_connection, messages)
        }

    public override suspend fun upsertMoodJournalEntries(entries: List<MoodJournalEntity>): Unit =
        performSuspending(__db, false, true) { _connection ->
            __insertAdapterOfMoodJournalEntity.insert(_connection, entries)
        }

    public override fun observeSessionState(id: Int): Flow<SessionStateEntity?> {
        val _sql: String = "SELECT * FROM session_state WHERE id = ? LIMIT 1"
        return createFlow(__db, false, arrayOf("session_state")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                var _argIndex: Int = 1
                _stmt.bindLong(_argIndex, id.toLong())
                val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfActiveCharacterId: Int =
                    getColumnIndexOrThrow(_stmt, "activeCharacterId")
                val _columnIndexOfSelectedLanguageCode: Int =
                    getColumnIndexOrThrow(_stmt, "selectedLanguageCode")
                val _columnIndexOfBackgroundThemeId: Int =
                    getColumnIndexOrThrow(_stmt, "backgroundThemeId")
                val _columnIndexOfVoiceEngineMode: Int =
                    getColumnIndexOrThrow(_stmt, "voiceEngineMode")
                val _columnIndexOfCoins: Int = getColumnIndexOrThrow(_stmt, "coins")
                val _columnIndexOfDiamonds: Int = getColumnIndexOrThrow(_stmt, "diamonds")
                val _columnIndexOfPlayerLevel: Int = getColumnIndexOrThrow(_stmt, "playerLevel")
                val _columnIndexOfLevelProgress: Int = getColumnIndexOrThrow(_stmt, "levelProgress")
                val _columnIndexOfQuizStreak: Int = getColumnIndexOrThrow(_stmt, "quizStreak")
                val _columnIndexOfPomodoroCompletedSessions: Int =
                    getColumnIndexOrThrow(_stmt, "pomodoroCompletedSessions")
                val _columnIndexOfLastUpdatedMillis: Int =
                    getColumnIndexOrThrow(_stmt, "lastUpdatedMillis")
                val _result: SessionStateEntity?
                if (_stmt.step()) {
                    val _tmpId: Int = _stmt.getLong(_columnIndexOfId).toInt()
                    val _tmpActiveCharacterId: String =
                        _stmt.getText(_columnIndexOfActiveCharacterId)
                    val _tmpSelectedLanguageCode: String =
                        _stmt.getText(_columnIndexOfSelectedLanguageCode)
                    val _tmpBackgroundThemeId: String =
                        _stmt.getText(_columnIndexOfBackgroundThemeId)
                    val _tmpVoiceEngineMode: String = _stmt.getText(_columnIndexOfVoiceEngineMode)
                    val _tmpCoins: Int = _stmt.getLong(_columnIndexOfCoins).toInt()
                    val _tmpDiamonds: Int = _stmt.getLong(_columnIndexOfDiamonds).toInt()
                    val _tmpPlayerLevel: Int = _stmt.getLong(_columnIndexOfPlayerLevel).toInt()
                    val _tmpLevelProgress: Float =
                        _stmt.getDouble(_columnIndexOfLevelProgress).toFloat()
                    val _tmpQuizStreak: Int = _stmt.getLong(_columnIndexOfQuizStreak).toInt()
                    val _tmpPomodoroCompletedSessions: Int =
                        _stmt.getLong(_columnIndexOfPomodoroCompletedSessions).toInt()
                    val _tmpLastUpdatedMillis: Long = _stmt.getLong(_columnIndexOfLastUpdatedMillis)
                    _result = SessionStateEntity(
                        _tmpId,
                        _tmpActiveCharacterId,
                        _tmpSelectedLanguageCode,
                        _tmpBackgroundThemeId,
                        _tmpVoiceEngineMode,
                        _tmpCoins,
                        _tmpDiamonds,
                        _tmpPlayerLevel,
                        _tmpLevelProgress,
                        _tmpQuizStreak,
                        _tmpPomodoroCompletedSessions,
                        _tmpLastUpdatedMillis
                    )
                } else {
                    _result = null
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getSessionStateOnce(id: Int): SessionStateEntity? {
        val _sql: String = "SELECT * FROM session_state WHERE id = ? LIMIT 1"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                var _argIndex: Int = 1
                _stmt.bindLong(_argIndex, id.toLong())
                val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfActiveCharacterId: Int =
                    getColumnIndexOrThrow(_stmt, "activeCharacterId")
                val _columnIndexOfSelectedLanguageCode: Int =
                    getColumnIndexOrThrow(_stmt, "selectedLanguageCode")
                val _columnIndexOfBackgroundThemeId: Int =
                    getColumnIndexOrThrow(_stmt, "backgroundThemeId")
                val _columnIndexOfVoiceEngineMode: Int =
                    getColumnIndexOrThrow(_stmt, "voiceEngineMode")
                val _columnIndexOfCoins: Int = getColumnIndexOrThrow(_stmt, "coins")
                val _columnIndexOfDiamonds: Int = getColumnIndexOrThrow(_stmt, "diamonds")
                val _columnIndexOfPlayerLevel: Int = getColumnIndexOrThrow(_stmt, "playerLevel")
                val _columnIndexOfLevelProgress: Int = getColumnIndexOrThrow(_stmt, "levelProgress")
                val _columnIndexOfQuizStreak: Int = getColumnIndexOrThrow(_stmt, "quizStreak")
                val _columnIndexOfPomodoroCompletedSessions: Int =
                    getColumnIndexOrThrow(_stmt, "pomodoroCompletedSessions")
                val _columnIndexOfLastUpdatedMillis: Int =
                    getColumnIndexOrThrow(_stmt, "lastUpdatedMillis")
                val _result: SessionStateEntity?
                if (_stmt.step()) {
                    val _tmpId: Int = _stmt.getLong(_columnIndexOfId).toInt()
                    val _tmpActiveCharacterId: String =
                        _stmt.getText(_columnIndexOfActiveCharacterId)
                    val _tmpSelectedLanguageCode: String =
                        _stmt.getText(_columnIndexOfSelectedLanguageCode)
                    val _tmpBackgroundThemeId: String =
                        _stmt.getText(_columnIndexOfBackgroundThemeId)
                    val _tmpVoiceEngineMode: String = _stmt.getText(_columnIndexOfVoiceEngineMode)
                    val _tmpCoins: Int = _stmt.getLong(_columnIndexOfCoins).toInt()
                    val _tmpDiamonds: Int = _stmt.getLong(_columnIndexOfDiamonds).toInt()
                    val _tmpPlayerLevel: Int = _stmt.getLong(_columnIndexOfPlayerLevel).toInt()
                    val _tmpLevelProgress: Float =
                        _stmt.getDouble(_columnIndexOfLevelProgress).toFloat()
                    val _tmpQuizStreak: Int = _stmt.getLong(_columnIndexOfQuizStreak).toInt()
                    val _tmpPomodoroCompletedSessions: Int =
                        _stmt.getLong(_columnIndexOfPomodoroCompletedSessions).toInt()
                    val _tmpLastUpdatedMillis: Long = _stmt.getLong(_columnIndexOfLastUpdatedMillis)
                    _result = SessionStateEntity(
                        _tmpId,
                        _tmpActiveCharacterId,
                        _tmpSelectedLanguageCode,
                        _tmpBackgroundThemeId,
                        _tmpVoiceEngineMode,
                        _tmpCoins,
                        _tmpDiamonds,
                        _tmpPlayerLevel,
                        _tmpLevelProgress,
                        _tmpQuizStreak,
                        _tmpPomodoroCompletedSessions,
                        _tmpLastUpdatedMillis
                    )
                } else {
                    _result = null
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override fun observeCharacterCustomization(
        characterId: String
    ): Flow<CharacterCustomizationEntity?> {
        val _sql: String = "SELECT * FROM character_customizations WHERE characterId = ? LIMIT 1"
        return createFlow(__db, false, arrayOf("character_customizations")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                var _argIndex: Int = 1
                _stmt.bindText(_argIndex, characterId)
                val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
                val _columnIndexOfFurColorId: Int = getColumnIndexOrThrow(_stmt, "furColorId")
                val _columnIndexOfFurColorArgb: Int = getColumnIndexOrThrow(_stmt, "furColorArgb")
                val _columnIndexOfEyeStyleId: Int = getColumnIndexOrThrow(_stmt, "eyeStyleId")
                val _columnIndexOfEarStyleId: Int = getColumnIndexOrThrow(_stmt, "earStyleId")
                val _columnIndexOfCostumeId: Int = getColumnIndexOrThrow(_stmt, "costumeId")
                val _columnIndexOfAccessoryId: Int = getColumnIndexOrThrow(_stmt, "accessoryId")
                val _columnIndexOfRoomThemeId: Int = getColumnIndexOrThrow(_stmt, "roomThemeId")
                val _columnIndexOfVoicePitch: Int = getColumnIndexOrThrow(_stmt, "voicePitch")
                val _columnIndexOfSpeechRate: Int = getColumnIndexOrThrow(_stmt, "speechRate")
                val _columnIndexOfAffectionScore: Int =
                    getColumnIndexOrThrow(_stmt, "affectionScore")
                val _columnIndexOfHungerLevel: Int = getColumnIndexOrThrow(_stmt, "hungerLevel")
                val _columnIndexOfToiletLevel: Int = getColumnIndexOrThrow(_stmt, "toiletLevel")
                val _columnIndexOfEnergyLevel: Int = getColumnIndexOrThrow(_stmt, "energyLevel")
                val _columnIndexOfFunLevel: Int = getColumnIndexOrThrow(_stmt, "funLevel")
                val _columnIndexOfLastUpdatedMillis: Int =
                    getColumnIndexOrThrow(_stmt, "lastUpdatedMillis")
                val _result: CharacterCustomizationEntity?
                if (_stmt.step()) {
                    val _tmpCharacterId: String = _stmt.getText(_columnIndexOfCharacterId)
                    val _tmpFurColorId: String = _stmt.getText(_columnIndexOfFurColorId)
                    val _tmpFurColorArgb: Int = _stmt.getLong(_columnIndexOfFurColorArgb).toInt()
                    val _tmpEyeStyleId: String = _stmt.getText(_columnIndexOfEyeStyleId)
                    val _tmpEarStyleId: String = _stmt.getText(_columnIndexOfEarStyleId)
                    val _tmpCostumeId: String = _stmt.getText(_columnIndexOfCostumeId)
                    val _tmpAccessoryId: String = _stmt.getText(_columnIndexOfAccessoryId)
                    val _tmpRoomThemeId: String = _stmt.getText(_columnIndexOfRoomThemeId)
                    val _tmpVoicePitch: Float = _stmt.getDouble(_columnIndexOfVoicePitch).toFloat()
                    val _tmpSpeechRate: Float = _stmt.getDouble(_columnIndexOfSpeechRate).toFloat()
                    val _tmpAffectionScore: Int =
                        _stmt.getLong(_columnIndexOfAffectionScore).toInt()
                    val _tmpHungerLevel: Float =
                        _stmt.getDouble(_columnIndexOfHungerLevel).toFloat()
                    val _tmpToiletLevel: Float =
                        _stmt.getDouble(_columnIndexOfToiletLevel).toFloat()
                    val _tmpEnergyLevel: Float =
                        _stmt.getDouble(_columnIndexOfEnergyLevel).toFloat()
                    val _tmpFunLevel: Float = _stmt.getDouble(_columnIndexOfFunLevel).toFloat()
                    val _tmpLastUpdatedMillis: Long = _stmt.getLong(_columnIndexOfLastUpdatedMillis)
                    _result = CharacterCustomizationEntity(
                        _tmpCharacterId,
                        _tmpFurColorId,
                        _tmpFurColorArgb,
                        _tmpEyeStyleId,
                        _tmpEarStyleId,
                        _tmpCostumeId,
                        _tmpAccessoryId,
                        _tmpRoomThemeId,
                        _tmpVoicePitch,
                        _tmpSpeechRate,
                        _tmpAffectionScore,
                        _tmpHungerLevel,
                        _tmpToiletLevel,
                        _tmpEnergyLevel,
                        _tmpFunLevel,
                        _tmpLastUpdatedMillis
                    )
                } else {
                    _result = null
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override fun observeAllCharacterCustomizations():
        Flow<List<CharacterCustomizationEntity>> {
        val _sql: String = "SELECT * FROM character_customizations"
        return createFlow(__db, false, arrayOf("character_customizations")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
                val _columnIndexOfFurColorId: Int = getColumnIndexOrThrow(_stmt, "furColorId")
                val _columnIndexOfFurColorArgb: Int = getColumnIndexOrThrow(_stmt, "furColorArgb")
                val _columnIndexOfEyeStyleId: Int = getColumnIndexOrThrow(_stmt, "eyeStyleId")
                val _columnIndexOfEarStyleId: Int = getColumnIndexOrThrow(_stmt, "earStyleId")
                val _columnIndexOfCostumeId: Int = getColumnIndexOrThrow(_stmt, "costumeId")
                val _columnIndexOfAccessoryId: Int = getColumnIndexOrThrow(_stmt, "accessoryId")
                val _columnIndexOfRoomThemeId: Int = getColumnIndexOrThrow(_stmt, "roomThemeId")
                val _columnIndexOfVoicePitch: Int = getColumnIndexOrThrow(_stmt, "voicePitch")
                val _columnIndexOfSpeechRate: Int = getColumnIndexOrThrow(_stmt, "speechRate")
                val _columnIndexOfAffectionScore: Int =
                    getColumnIndexOrThrow(_stmt, "affectionScore")
                val _columnIndexOfHungerLevel: Int = getColumnIndexOrThrow(_stmt, "hungerLevel")
                val _columnIndexOfToiletLevel: Int = getColumnIndexOrThrow(_stmt, "toiletLevel")
                val _columnIndexOfEnergyLevel: Int = getColumnIndexOrThrow(_stmt, "energyLevel")
                val _columnIndexOfFunLevel: Int = getColumnIndexOrThrow(_stmt, "funLevel")
                val _columnIndexOfLastUpdatedMillis: Int =
                    getColumnIndexOrThrow(_stmt, "lastUpdatedMillis")
                val _result: MutableList<CharacterCustomizationEntity> = mutableListOf()
                while (_stmt.step()) {
                    val _tmpCharacterId: String = _stmt.getText(_columnIndexOfCharacterId)
                    val _tmpFurColorId: String = _stmt.getText(_columnIndexOfFurColorId)
                    val _tmpFurColorArgb: Int = _stmt.getLong(_columnIndexOfFurColorArgb).toInt()
                    val _tmpEyeStyleId: String = _stmt.getText(_columnIndexOfEyeStyleId)
                    val _tmpEarStyleId: String = _stmt.getText(_columnIndexOfEarStyleId)
                    val _tmpCostumeId: String = _stmt.getText(_columnIndexOfCostumeId)
                    val _tmpAccessoryId: String = _stmt.getText(_columnIndexOfAccessoryId)
                    val _tmpRoomThemeId: String = _stmt.getText(_columnIndexOfRoomThemeId)
                    val _tmpVoicePitch: Float = _stmt.getDouble(_columnIndexOfVoicePitch).toFloat()
                    val _tmpSpeechRate: Float = _stmt.getDouble(_columnIndexOfSpeechRate).toFloat()
                    val _tmpAffectionScore: Int =
                        _stmt.getLong(_columnIndexOfAffectionScore).toInt()
                    val _tmpHungerLevel: Float =
                        _stmt.getDouble(_columnIndexOfHungerLevel).toFloat()
                    val _tmpToiletLevel: Float =
                        _stmt.getDouble(_columnIndexOfToiletLevel).toFloat()
                    val _tmpEnergyLevel: Float =
                        _stmt.getDouble(_columnIndexOfEnergyLevel).toFloat()
                    val _tmpFunLevel: Float = _stmt.getDouble(_columnIndexOfFunLevel).toFloat()
                    val _tmpLastUpdatedMillis: Long = _stmt.getLong(_columnIndexOfLastUpdatedMillis)
                    val _item = CharacterCustomizationEntity(
                        _tmpCharacterId,
                        _tmpFurColorId,
                        _tmpFurColorArgb,
                        _tmpEyeStyleId,
                        _tmpEarStyleId,
                        _tmpCostumeId,
                        _tmpAccessoryId,
                        _tmpRoomThemeId,
                        _tmpVoicePitch,
                        _tmpSpeechRate,
                        _tmpAffectionScore,
                        _tmpHungerLevel,
                        _tmpToiletLevel,
                        _tmpEnergyLevel,
                        _tmpFunLevel,
                        _tmpLastUpdatedMillis
                    )
                    _result.add(_item)
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getCharacterCustomizationOnce(
        characterId: String
    ): CharacterCustomizationEntity? {
        val _sql: String = "SELECT * FROM character_customizations WHERE characterId = ? LIMIT 1"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                var _argIndex: Int = 1
                _stmt.bindText(_argIndex, characterId)
                val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
                val _columnIndexOfFurColorId: Int = getColumnIndexOrThrow(_stmt, "furColorId")
                val _columnIndexOfFurColorArgb: Int = getColumnIndexOrThrow(_stmt, "furColorArgb")
                val _columnIndexOfEyeStyleId: Int = getColumnIndexOrThrow(_stmt, "eyeStyleId")
                val _columnIndexOfEarStyleId: Int = getColumnIndexOrThrow(_stmt, "earStyleId")
                val _columnIndexOfCostumeId: Int = getColumnIndexOrThrow(_stmt, "costumeId")
                val _columnIndexOfAccessoryId: Int = getColumnIndexOrThrow(_stmt, "accessoryId")
                val _columnIndexOfRoomThemeId: Int = getColumnIndexOrThrow(_stmt, "roomThemeId")
                val _columnIndexOfVoicePitch: Int = getColumnIndexOrThrow(_stmt, "voicePitch")
                val _columnIndexOfSpeechRate: Int = getColumnIndexOrThrow(_stmt, "speechRate")
                val _columnIndexOfAffectionScore: Int =
                    getColumnIndexOrThrow(_stmt, "affectionScore")
                val _columnIndexOfHungerLevel: Int = getColumnIndexOrThrow(_stmt, "hungerLevel")
                val _columnIndexOfToiletLevel: Int = getColumnIndexOrThrow(_stmt, "toiletLevel")
                val _columnIndexOfEnergyLevel: Int = getColumnIndexOrThrow(_stmt, "energyLevel")
                val _columnIndexOfFunLevel: Int = getColumnIndexOrThrow(_stmt, "funLevel")
                val _columnIndexOfLastUpdatedMillis: Int =
                    getColumnIndexOrThrow(_stmt, "lastUpdatedMillis")
                val _result: CharacterCustomizationEntity?
                if (_stmt.step()) {
                    val _tmpCharacterId: String = _stmt.getText(_columnIndexOfCharacterId)
                    val _tmpFurColorId: String = _stmt.getText(_columnIndexOfFurColorId)
                    val _tmpFurColorArgb: Int = _stmt.getLong(_columnIndexOfFurColorArgb).toInt()
                    val _tmpEyeStyleId: String = _stmt.getText(_columnIndexOfEyeStyleId)
                    val _tmpEarStyleId: String = _stmt.getText(_columnIndexOfEarStyleId)
                    val _tmpCostumeId: String = _stmt.getText(_columnIndexOfCostumeId)
                    val _tmpAccessoryId: String = _stmt.getText(_columnIndexOfAccessoryId)
                    val _tmpRoomThemeId: String = _stmt.getText(_columnIndexOfRoomThemeId)
                    val _tmpVoicePitch: Float = _stmt.getDouble(_columnIndexOfVoicePitch).toFloat()
                    val _tmpSpeechRate: Float = _stmt.getDouble(_columnIndexOfSpeechRate).toFloat()
                    val _tmpAffectionScore: Int =
                        _stmt.getLong(_columnIndexOfAffectionScore).toInt()
                    val _tmpHungerLevel: Float =
                        _stmt.getDouble(_columnIndexOfHungerLevel).toFloat()
                    val _tmpToiletLevel: Float =
                        _stmt.getDouble(_columnIndexOfToiletLevel).toFloat()
                    val _tmpEnergyLevel: Float =
                        _stmt.getDouble(_columnIndexOfEnergyLevel).toFloat()
                    val _tmpFunLevel: Float = _stmt.getDouble(_columnIndexOfFunLevel).toFloat()
                    val _tmpLastUpdatedMillis: Long = _stmt.getLong(_columnIndexOfLastUpdatedMillis)
                    _result = CharacterCustomizationEntity(
                        _tmpCharacterId,
                        _tmpFurColorId,
                        _tmpFurColorArgb,
                        _tmpEyeStyleId,
                        _tmpEarStyleId,
                        _tmpCostumeId,
                        _tmpAccessoryId,
                        _tmpRoomThemeId,
                        _tmpVoicePitch,
                        _tmpSpeechRate,
                        _tmpAffectionScore,
                        _tmpHungerLevel,
                        _tmpToiletLevel,
                        _tmpEnergyLevel,
                        _tmpFunLevel,
                        _tmpLastUpdatedMillis
                    )
                } else {
                    _result = null
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override fun observeRecentChatMessages(limit: Int): Flow<List<ChatMessageEntity>> {
        val _sql: String = "SELECT * FROM chat_messages ORDER BY timestamp ASC LIMIT ?"
        return createFlow(__db, false, arrayOf("chat_messages")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                var _argIndex: Int = 1
                _stmt.bindLong(_argIndex, limit.toLong())
                val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
                val _columnIndexOfSenderName: Int = getColumnIndexOrThrow(_stmt, "senderName")
                val _columnIndexOfText: Int = getColumnIndexOrThrow(_stmt, "text")
                val _columnIndexOfIsUser: Int = getColumnIndexOrThrow(_stmt, "isUser")
                val _columnIndexOfEmotionName: Int = getColumnIndexOrThrow(_stmt, "emotionName")
                val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
                val _result: MutableList<ChatMessageEntity> = mutableListOf()
                while (_stmt.step()) {
                    val _tmpId: String = _stmt.getText(_columnIndexOfId)
                    val _tmpCharacterId: String = _stmt.getText(_columnIndexOfCharacterId)
                    val _tmpSenderName: String = _stmt.getText(_columnIndexOfSenderName)
                    val _tmpText: String = _stmt.getText(_columnIndexOfText)
                    val _tmp: Int = _stmt.getLong(_columnIndexOfIsUser).toInt()
                    val _tmpIsUser: Boolean = _tmp != 0
                    val _tmpEmotionName: String = _stmt.getText(_columnIndexOfEmotionName)
                    val _tmpTimestamp: Long = _stmt.getLong(_columnIndexOfTimestamp)
                    val _item = ChatMessageEntity(
                        _tmpId,
                        _tmpCharacterId,
                        _tmpSenderName,
                        _tmpText,
                        _tmpIsUser,
                        _tmpEmotionName,
                        _tmpTimestamp
                    )
                    _result.add(_item)
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getRecentChatMessagesOnce(limit: Int): List<ChatMessageEntity> {
        val _sql: String = "SELECT * FROM chat_messages ORDER BY timestamp ASC LIMIT ?"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                var _argIndex: Int = 1
                _stmt.bindLong(_argIndex, limit.toLong())
                val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
                val _columnIndexOfSenderName: Int = getColumnIndexOrThrow(_stmt, "senderName")
                val _columnIndexOfText: Int = getColumnIndexOrThrow(_stmt, "text")
                val _columnIndexOfIsUser: Int = getColumnIndexOrThrow(_stmt, "isUser")
                val _columnIndexOfEmotionName: Int = getColumnIndexOrThrow(_stmt, "emotionName")
                val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
                val _result: MutableList<ChatMessageEntity> = mutableListOf()
                while (_stmt.step()) {
                    val _tmpId: String = _stmt.getText(_columnIndexOfId)
                    val _tmpCharacterId: String = _stmt.getText(_columnIndexOfCharacterId)
                    val _tmpSenderName: String = _stmt.getText(_columnIndexOfSenderName)
                    val _tmpText: String = _stmt.getText(_columnIndexOfText)
                    val _tmp: Int = _stmt.getLong(_columnIndexOfIsUser).toInt()
                    val _tmpIsUser: Boolean = _tmp != 0
                    val _tmpEmotionName: String = _stmt.getText(_columnIndexOfEmotionName)
                    val _tmpTimestamp: Long = _stmt.getLong(_columnIndexOfTimestamp)
                    val _item = ChatMessageEntity(
                        _tmpId,
                        _tmpCharacterId,
                        _tmpSenderName,
                        _tmpText,
                        _tmpIsUser,
                        _tmpEmotionName,
                        _tmpTimestamp
                    )
                    _result.add(_item)
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override fun observeMoodJournalEntries(limit: Int): Flow<List<MoodJournalEntity>> {
        val _sql: String =
            "SELECT * FROM mood_journal_entries ORDER BY timestampMillis DESC LIMIT ?"
        return createFlow(__db, false, arrayOf("mood_journal_entries")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                var _argIndex: Int = 1
                _stmt.bindLong(_argIndex, limit.toLong())
                val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfTimestampMillis: Int =
                    getColumnIndexOrThrow(_stmt, "timestampMillis")
                val _columnIndexOfFormattedDate: Int =
                    getColumnIndexOrThrow(_stmt, "formattedDate")
                val _columnIndexOfMoodId: Int = getColumnIndexOrThrow(_stmt, "moodId")
                val _columnIndexOfUserNote: Int = getColumnIndexOrThrow(_stmt, "userNote")
                val _columnIndexOfCharacterName: Int =
                    getColumnIndexOrThrow(_stmt, "characterName")
                val _columnIndexOfCharacterEmoji: Int =
                    getColumnIndexOrThrow(_stmt, "characterEmoji")
                val _columnIndexOfPetResponse: Int = getColumnIndexOrThrow(_stmt, "petResponse")
                val _result: MutableList<MoodJournalEntity> = mutableListOf()
                while (_stmt.step()) {
                    val _tmpId: String = _stmt.getText(_columnIndexOfId)
                    val _tmpTimestampMillis: Long = _stmt.getLong(_columnIndexOfTimestampMillis)
                    val _tmpFormattedDate: String = _stmt.getText(_columnIndexOfFormattedDate)
                    val _tmpMoodId: String = _stmt.getText(_columnIndexOfMoodId)
                    val _tmpUserNote: String = _stmt.getText(_columnIndexOfUserNote)
                    val _tmpCharacterName: String = _stmt.getText(_columnIndexOfCharacterName)
                    val _tmpCharacterEmoji: String = _stmt.getText(_columnIndexOfCharacterEmoji)
                    val _tmpPetResponse: String = _stmt.getText(_columnIndexOfPetResponse)
                    val _item = MoodJournalEntity(
                        _tmpId,
                        _tmpTimestampMillis,
                        _tmpFormattedDate,
                        _tmpMoodId,
                        _tmpUserNote,
                        _tmpCharacterName,
                        _tmpCharacterEmoji,
                        _tmpPetResponse
                    )
                    _result.add(_item)
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getMoodJournalEntriesOnce(limit: Int): List<MoodJournalEntity> {
        val _sql: String =
            "SELECT * FROM mood_journal_entries ORDER BY timestampMillis DESC LIMIT ?"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                var _argIndex: Int = 1
                _stmt.bindLong(_argIndex, limit.toLong())
                val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfTimestampMillis: Int =
                    getColumnIndexOrThrow(_stmt, "timestampMillis")
                val _columnIndexOfFormattedDate: Int =
                    getColumnIndexOrThrow(_stmt, "formattedDate")
                val _columnIndexOfMoodId: Int = getColumnIndexOrThrow(_stmt, "moodId")
                val _columnIndexOfUserNote: Int = getColumnIndexOrThrow(_stmt, "userNote")
                val _columnIndexOfCharacterName: Int =
                    getColumnIndexOrThrow(_stmt, "characterName")
                val _columnIndexOfCharacterEmoji: Int =
                    getColumnIndexOrThrow(_stmt, "characterEmoji")
                val _columnIndexOfPetResponse: Int = getColumnIndexOrThrow(_stmt, "petResponse")
                val _result: MutableList<MoodJournalEntity> = mutableListOf()
                while (_stmt.step()) {
                    val _tmpId: String = _stmt.getText(_columnIndexOfId)
                    val _tmpTimestampMillis: Long = _stmt.getLong(_columnIndexOfTimestampMillis)
                    val _tmpFormattedDate: String = _stmt.getText(_columnIndexOfFormattedDate)
                    val _tmpMoodId: String = _stmt.getText(_columnIndexOfMoodId)
                    val _tmpUserNote: String = _stmt.getText(_columnIndexOfUserNote)
                    val _tmpCharacterName: String = _stmt.getText(_columnIndexOfCharacterName)
                    val _tmpCharacterEmoji: String = _stmt.getText(_columnIndexOfCharacterEmoji)
                    val _tmpPetResponse: String = _stmt.getText(_columnIndexOfPetResponse)
                    val _item = MoodJournalEntity(
                        _tmpId,
                        _tmpTimestampMillis,
                        _tmpFormattedDate,
                        _tmpMoodId,
                        _tmpUserNote,
                        _tmpCharacterName,
                        _tmpCharacterEmoji,
                        _tmpPetResponse
                    )
                    _result.add(_item)
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun clearAllChatMessages() {
        val _sql: String = "DELETE FROM chat_messages"
        return performSuspending(__db, false, true) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.step()
            } finally {
                _stmt.close()
            }
        }
    }

    public companion object {
        public fun getRequiredConverters(): List<KClass<*>> = emptyList()
    }
}
