package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM saved_profiles ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllProfiles(): Flow<List<SavedProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SavedProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: SavedProfileEntity)

    @Query("DELETE FROM saved_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query("UPDATE saved_profiles SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE saved_profiles SET accountNote = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Query("UPDATE saved_profiles SET password = :password WHERE id = :id")
    suspend fun updatePassword(id: Long, password: String)
}
