package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database managing persistent character customizations, companion session state,
 * chat history, and mood journal entries.
 */
@Database(
    entities = [
        SessionStateEntity::class,
        CharacterCustomizationEntity::class,
        ChatMessageEntity::class,
        MoodJournalEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CharacterStateDatabase : RoomDatabase() {

    abstract fun characterStateDao(): CharacterStateDao

    companion object {
        private const val DATABASE_NAME = "pawpal_character_state.db"

        @Volatile
        private var INSTANCE: CharacterStateDatabase? = null

        fun getInstance(context: Context): CharacterStateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CharacterStateDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
