package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    fun getUserProgress(): Flow<UserProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProgress(progress: UserProgress)

    @Update
    suspend fun updateUserProgress(progress: UserProgress)

    @Query("SELECT * FROM saved_games ORDER BY timestamp DESC")
    fun getAllSavedGames(): Flow<List<SavedGame>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedGame(game: SavedGame): Long

    @Query("DELETE FROM saved_games WHERE id = :gameId")
    suspend fun deleteSavedGame(gameId: Int)

    @Query("DELETE FROM saved_games")
    suspend fun deleteAllSavedGames()
}
