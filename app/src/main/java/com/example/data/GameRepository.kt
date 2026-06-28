package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val gameDao: GameDao) {

    val userProgress: Flow<UserProgress> = gameDao.getUserProgress().map { 
        it ?: UserProgress(
            id = 1,
            name = "Sparky",
            avatar = "🦖",
            gradeLevel = "Preschool",
            stars = 0,
            coins = 0,
            level = 1,
            completedCount = 0,
            unlockedBadges = ""
        )
    }

    val savedGames: Flow<List<SavedGame>> = gameDao.getAllSavedGames()

    suspend fun saveProgress(progress: UserProgress) {
        gameDao.insertUserProgress(progress)
    }

    suspend fun saveGame(game: SavedGame): Long {
        return gameDao.insertSavedGame(game)
    }

    suspend fun deleteSavedGame(gameId: Int) {
        gameDao.deleteSavedGame(gameId)
    }

    suspend fun clearAllGames() {
        gameDao.deleteAllSavedGames()
    }
}
