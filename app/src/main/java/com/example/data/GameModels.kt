package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val name: String = "Sparky",
    val avatar: String = "🦖", // Emoji avatar like 🦁, 🐼, 🦄, 🦖, 🦊
    val gradeLevel: String = "Preschool", // Preschool, Early Elementary, Late Elementary
    val stars: Int = 0,
    val coins: Int = 0,
    val level: Int = 1,
    val completedCount: Int = 0,
    val unlockedBadges: String = "" // Comma-separated list of badge keys
)

@Entity(tableName = "saved_games")
data class SavedGame(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val theme: String,
    val gameJson: String,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

// In-Memory Data representations for Badges
data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val requirementText: String,
    val starCost: Int = 0
)

val ALL_BADGES = listOf(
    Badge("first_step", "First Explorer", "Completed your very first game challenge!", "🎒", "Complete 1 game"),
    Badge("math_magician", "Math Magician", "Solved 5 Math challenges!", "🧮", "Complete 5 Math games"),
    Badge("word_wizard", "Word Wizard", "Solved 5 Words & Spelling challenges!", "🔤", "Complete 5 Word games"),
    Badge("science_star", "Science Star", "Solved 5 Science & Nature puzzles!", "🔬", "Complete 5 Science games"),
    Badge("trivia_titan", "Trivia Champ", "Solved 5 Trivia & Quiz games!", "🧠", "Complete 5 Trivia games"),
    Badge("bubble_popper", "Bubble Popper", "Popped answers in Math Bubble Quest!", "🫧", "Play Math Bubble Quest"),
    Badge("memory_master", "Memory Safari Master", "Matched all cards in Memory Safari!", "🦁", "Play Memory Safari"),
    Badge("pattern_pro", "Pattern Solver", "Completed 3 Magic Patterns!", "🧩", "Play Pattern Solver"),
    Badge("star_collector", "Star Collector", "Earned 50 shining stars!", "⭐", "Accumulate 50 stars"),
    Badge("super_kid", "Super Scholar", "Completed 20 games of any type!", "👑", "Complete 20 games")
)
