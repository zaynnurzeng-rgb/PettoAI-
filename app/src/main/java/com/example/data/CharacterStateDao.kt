package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for reactive observation and persistence of session state, character customizations,
 * chat messages, and mood journal entries.
 */
@Dao
interface CharacterStateDao {

    @Query("SELECT * FROM session_state WHERE id = :id LIMIT 1")
    fun observeSessionState(id: Int = SessionStateEntity.SINGLETON_ID): Flow<SessionStateEntity?>

    @Query("SELECT * FROM session_state WHERE id = :id LIMIT 1")
    suspend fun getSessionStateOnce(id: Int = SessionStateEntity.SINGLETON_ID): SessionStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionState(state: SessionStateEntity)

    @Query("SELECT * FROM character_customizations WHERE characterId = :characterId LIMIT 1")
    fun observeCharacterCustomization(characterId: String): Flow<CharacterCustomizationEntity?>

    @Query("SELECT * FROM character_customizations")
    fun observeAllCharacterCustomizations(): Flow<List<CharacterCustomizationEntity>>

    @Query("SELECT * FROM character_customizations WHERE characterId = :characterId LIMIT 1")
    suspend fun getCharacterCustomizationOnce(characterId: String): CharacterCustomizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCharacterCustomization(customization: CharacterCustomizationEntity)

    // Chat Messages Persistence
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC LIMIT :limit")
    fun observeRecentChatMessages(limit: Int = 60): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getRecentChatMessagesOnce(limit: Int = 60): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChatMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllChatMessages()

    // Mood Journal Persistence
    @Query("SELECT * FROM mood_journal_entries ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeMoodJournalEntries(limit: Int = 40): Flow<List<MoodJournalEntity>>

    @Query("SELECT * FROM mood_journal_entries ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun getMoodJournalEntriesOnce(limit: Int = 40): List<MoodJournalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMoodJournalEntries(entries: List<MoodJournalEntity>)
}
