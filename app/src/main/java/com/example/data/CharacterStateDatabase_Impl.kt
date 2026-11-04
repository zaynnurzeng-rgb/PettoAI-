package com.example.data

import android.annotation.SuppressLint
import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlin.reflect.KClass

@SuppressLint("RestrictedApi")
@Suppress("UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "RestrictedApi")
public class CharacterStateDatabase_Impl : CharacterStateDatabase() {
    private val _characterStateDao: Lazy<CharacterStateDao> = lazy {
        CharacterStateDao_Impl(this)
    }

    protected override fun createOpenDelegate(): RoomOpenDelegate {
        val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(
            2,
            "2b9759efb3fd43804e3efe745fecd2ad",
            "37ec750e6c0e9e313a7585f4ba30c1a5"
        ) {
            public override fun createAllTables(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `session_state` (`id` INTEGER NOT NULL, `activeCharacterId` TEXT NOT NULL, `selectedLanguageCode` TEXT NOT NULL, `backgroundThemeId` TEXT NOT NULL, `voiceEngineMode` TEXT NOT NULL, `coins` INTEGER NOT NULL, `diamonds` INTEGER NOT NULL, `playerLevel` INTEGER NOT NULL, `levelProgress` REAL NOT NULL, `quizStreak` INTEGER NOT NULL, `pomodoroCompletedSessions` INTEGER NOT NULL, `lastUpdatedMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                connection.execSQL("CREATE TABLE IF NOT EXISTS `character_customizations` (`characterId` TEXT NOT NULL, `furColorId` TEXT NOT NULL, `furColorArgb` INTEGER NOT NULL, `eyeStyleId` TEXT NOT NULL, `earStyleId` TEXT NOT NULL, `costumeId` TEXT NOT NULL, `accessoryId` TEXT NOT NULL, `roomThemeId` TEXT NOT NULL, `voicePitch` REAL NOT NULL, `speechRate` REAL NOT NULL, `affectionScore` INTEGER NOT NULL, `hungerLevel` REAL NOT NULL, `toiletLevel` REAL NOT NULL, `energyLevel` REAL NOT NULL, `funLevel` REAL NOT NULL, `lastUpdatedMillis` INTEGER NOT NULL, PRIMARY KEY(`characterId`))")
                connection.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` TEXT NOT NULL, `characterId` TEXT NOT NULL, `senderName` TEXT NOT NULL, `text` TEXT NOT NULL, `isUser` INTEGER NOT NULL, `emotionName` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                connection.execSQL("CREATE TABLE IF NOT EXISTS `mood_journal_entries` (`id` TEXT NOT NULL, `timestampMillis` INTEGER NOT NULL, `formattedDate` TEXT NOT NULL, `moodId` TEXT NOT NULL, `userNote` TEXT NOT NULL, `characterName` TEXT NOT NULL, `characterEmoji` TEXT NOT NULL, `petResponse` TEXT NOT NULL, PRIMARY KEY(`id`))")
                connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '2b9759efb3fd43804e3efe745fecd2ad')")
            }

            public override fun dropAllTables(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE IF EXISTS `session_state`")
                connection.execSQL("DROP TABLE IF EXISTS `character_customizations`")
                connection.execSQL("DROP TABLE IF EXISTS `chat_messages`")
                connection.execSQL("DROP TABLE IF EXISTS `mood_journal_entries`")
            }

            public override fun onCreate(connection: SQLiteConnection) {
            }

            public override fun onOpen(connection: SQLiteConnection) {
                internalInitInvalidationTracker(connection)
            }

            public override fun onPreMigrate(connection: SQLiteConnection) {
                dropFtsSyncTriggers(connection)
            }

            public override fun onPostMigrate(connection: SQLiteConnection) {
            }

            public override fun onValidateSchema(
                connection: SQLiteConnection
            ): RoomOpenDelegate.ValidationResult {
                val _columnsSessionState: MutableMap<String, TableInfo.Column> = mutableMapOf()
                _columnsSessionState["id"] = TableInfo.Column(
                    "id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsSessionState["activeCharacterId"] = TableInfo.Column(
                    "activeCharacterId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsSessionState["selectedLanguageCode"] = TableInfo.Column(
                    "selectedLanguageCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsSessionState["backgroundThemeId"] = TableInfo.Column(
                    "backgroundThemeId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsSessionState["voiceEngineMode"] = TableInfo.Column(
                    "voiceEngineMode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsSessionState["coins"] = TableInfo.Column(
                    "coins", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsSessionState["diamonds"] = TableInfo.Column(
                    "diamonds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsSessionState["playerLevel"] = TableInfo.Column(
                    "playerLevel", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsSessionState["levelProgress"] = TableInfo.Column(
                    "levelProgress", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsSessionState["quizStreak"] = TableInfo.Column(
                    "quizStreak", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsSessionState["pomodoroCompletedSessions"] = TableInfo.Column(
                    "pomodoroCompletedSessions", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsSessionState["lastUpdatedMillis"] = TableInfo.Column(
                    "lastUpdatedMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                val _foreignKeysSessionState: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
                val _indicesSessionState: MutableSet<TableInfo.Index> = mutableSetOf()
                val _infoSessionState = TableInfo(
                    "session_state", _columnsSessionState, _foreignKeysSessionState, _indicesSessionState
                )
                val _existingSessionState: TableInfo = read(connection, "session_state")
                if (_infoSessionState != _existingSessionState) {
                    return RoomOpenDelegate.ValidationResult(
                        false,
                        "session_state(com.example.data.SessionStateEntity).\n Expected:\n$_infoSessionState\n Found:\n$_existingSessionState"
                    )
                }

                val _columnsCharacterCustomizations: MutableMap<String, TableInfo.Column> = mutableMapOf()
                _columnsCharacterCustomizations["characterId"] = TableInfo.Column(
                    "characterId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["furColorId"] = TableInfo.Column(
                    "furColorId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["furColorArgb"] = TableInfo.Column(
                    "furColorArgb", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["eyeStyleId"] = TableInfo.Column(
                    "eyeStyleId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["earStyleId"] = TableInfo.Column(
                    "earStyleId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["costumeId"] = TableInfo.Column(
                    "costumeId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["accessoryId"] = TableInfo.Column(
                    "accessoryId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["roomThemeId"] = TableInfo.Column(
                    "roomThemeId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["voicePitch"] = TableInfo.Column(
                    "voicePitch", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["speechRate"] = TableInfo.Column(
                    "speechRate", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["affectionScore"] = TableInfo.Column(
                    "affectionScore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["hungerLevel"] = TableInfo.Column(
                    "hungerLevel", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["toiletLevel"] = TableInfo.Column(
                    "toiletLevel", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["energyLevel"] = TableInfo.Column(
                    "energyLevel", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["funLevel"] = TableInfo.Column(
                    "funLevel", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsCharacterCustomizations["lastUpdatedMillis"] = TableInfo.Column(
                    "lastUpdatedMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                val _foreignKeysCharacterCustomizations: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
                val _indicesCharacterCustomizations: MutableSet<TableInfo.Index> = mutableSetOf()
                val _infoCharacterCustomizations = TableInfo(
                    "character_customizations",
                    _columnsCharacterCustomizations,
                    _foreignKeysCharacterCustomizations,
                    _indicesCharacterCustomizations
                )
                val _existingCharacterCustomizations: TableInfo =
                    read(connection, "character_customizations")
                if (_infoCharacterCustomizations != _existingCharacterCustomizations) {
                    return RoomOpenDelegate.ValidationResult(
                        false,
                        "character_customizations(com.example.data.CharacterCustomizationEntity).\n Expected:\n$_infoCharacterCustomizations\n Found:\n$_existingCharacterCustomizations"
                    )
                }

                val _columnsChatMessages: MutableMap<String, TableInfo.Column> = mutableMapOf()
                _columnsChatMessages["id"] = TableInfo.Column(
                    "id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsChatMessages["characterId"] = TableInfo.Column(
                    "characterId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsChatMessages["senderName"] = TableInfo.Column(
                    "senderName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsChatMessages["text"] = TableInfo.Column(
                    "text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsChatMessages["isUser"] = TableInfo.Column(
                    "isUser", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsChatMessages["emotionName"] = TableInfo.Column(
                    "emotionName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsChatMessages["timestamp"] = TableInfo.Column(
                    "timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                val _foreignKeysChatMessages: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
                val _indicesChatMessages: MutableSet<TableInfo.Index> = mutableSetOf()
                val _infoChatMessages = TableInfo(
                    "chat_messages", _columnsChatMessages, _foreignKeysChatMessages, _indicesChatMessages
                )
                val _existingChatMessages: TableInfo = read(connection, "chat_messages")
                if (_infoChatMessages != _existingChatMessages) {
                    return RoomOpenDelegate.ValidationResult(
                        false,
                        "chat_messages(com.example.data.ChatMessageEntity).\n Expected:\n$_infoChatMessages\n Found:\n$_existingChatMessages"
                    )
                }

                val _columnsMoodJournalEntries: MutableMap<String, TableInfo.Column> = mutableMapOf()
                _columnsMoodJournalEntries["id"] = TableInfo.Column(
                    "id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsMoodJournalEntries["timestampMillis"] = TableInfo.Column(
                    "timestampMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsMoodJournalEntries["formattedDate"] = TableInfo.Column(
                    "formattedDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsMoodJournalEntries["moodId"] = TableInfo.Column(
                    "moodId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsMoodJournalEntries["userNote"] = TableInfo.Column(
                    "userNote", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsMoodJournalEntries["characterName"] = TableInfo.Column(
                    "characterName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsMoodJournalEntries["characterEmoji"] = TableInfo.Column(
                    "characterEmoji", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                _columnsMoodJournalEntries["petResponse"] = TableInfo.Column(
                    "petResponse", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY
                )
                val _foreignKeysMoodJournalEntries: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
                val _indicesMoodJournalEntries: MutableSet<TableInfo.Index> = mutableSetOf()
                val _infoMoodJournalEntries = TableInfo(
                    "mood_journal_entries",
                    _columnsMoodJournalEntries,
                    _foreignKeysMoodJournalEntries,
                    _indicesMoodJournalEntries
                )
                val _existingMoodJournalEntries: TableInfo =
                    read(connection, "mood_journal_entries")
                if (_infoMoodJournalEntries != _existingMoodJournalEntries) {
                    return RoomOpenDelegate.ValidationResult(
                        false,
                        "mood_journal_entries(com.example.data.MoodJournalEntity).\n Expected:\n$_infoMoodJournalEntries\n Found:\n$_existingMoodJournalEntries"
                    )
                }
                return RoomOpenDelegate.ValidationResult(true, null)
            }
        }
        return _openDelegate
    }

    protected override fun createInvalidationTracker(): InvalidationTracker {
        val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
        val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
        return InvalidationTracker(
            this,
            _shadowTablesMap,
            _viewTables,
            "session_state",
            "character_customizations",
            "chat_messages",
            "mood_journal_entries"
        )
    }

    public override fun clearAllTables() {
        super.performClear(
            false,
            "session_state",
            "character_customizations",
            "chat_messages",
            "mood_journal_entries"
        )
    }

    protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
        val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
        _typeConvertersMap[CharacterStateDao::class] = CharacterStateDao_Impl.getRequiredConverters()
        return _typeConvertersMap
    }

    public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
        return mutableSetOf()
    }

    public override fun createAutoMigrations(
        autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration> {
        return mutableListOf()
    }

    public override fun characterStateDao(): CharacterStateDao = _characterStateDao.value
}
