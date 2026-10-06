package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Database(entities = [SavedProfileEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meta_identity_vault.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ProfileRepository(private val profileDao: ProfileDao) {
    val allProfiles: Flow<List<SavedProfileEntity>> = profileDao.getAllProfiles()

    suspend fun insert(profile: SavedProfileEntity): Long = profileDao.insertProfile(profile)

    suspend fun update(profile: SavedProfileEntity) = profileDao.updateProfile(profile)

    suspend fun deleteById(id: Long) = profileDao.deleteProfileById(id)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        profileDao.setFavorite(id, isFavorite)

    suspend fun updateNote(id: Long, note: String) = profileDao.updateNote(id, note)

    suspend fun updatePassword(id: Long, password: String) =
        profileDao.updatePassword(id, password)
}
