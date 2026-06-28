package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.EducationalGame
import com.example.network.GeminiApiClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface Screen {
    object Welcome : Screen
    object Hub : Screen
    object AIToyBox : Screen
    object MathBubbleQuest : Screen
    object MemorySafari : Screen
    object PatternSolver : Screen
    object TrophyRoom : Screen
    data class WorldDetail(val worldId: String) : Screen
    data class PlayWorldGame(val worldId: String, val gameId: String, val gameType: String) : Screen
}

sealed interface AIGameState {
    object Idle : AIGameState
    object Loading : AIGameState
    data class Success(val game: EducationalGame) : AIGameState
    data class Error(val message: String) : AIGameState
}

// Built-in Game States
data class MathBubbleState(
    val num1: Int = 0,
    val num2: Int = 0,
    val operator: String = "+",
    val correctAnswer: Int = 0,
    val questionText: String = "",
    val bubbles: List<Bubble> = emptyList(),
    val poppedCount: Int = 0,
    val currentStreak: Int = 0,
    val activeWorldId: String? = null
)

data class Bubble(
    val id: Int,
    val value: Int,
    val xOffset: Float, // 0f to 1f representation
    val speed: Float,
    val colorHex: Long,
    val size: Int
)

data class MemoryCard(
    val id: Int,
    val emoji: String,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)

data class MemorySafariState(
    val cards: List<MemoryCard> = emptyList(),
    val selectedIndices: List<Int> = emptyList(),
    val matchesFound: Int = 0,
    val totalFlips: Int = 0,
    val isGameWon: Boolean = false
)

data class PatternQuestion(
    val sequence: List<String>,
    val missingIndex: Int,
    val options: List<String>,
    val correctAnswer: String
)

data class PatternSolverState(
    val currentQuestion: PatternQuestion? = null,
    val selectedOption: String? = null,
    val isCorrect: Boolean? = null,
    val solvedCount: Int = 0
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = GameRepository(database.gameDao())
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // Screen State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Welcome)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // User Progress State
    val userProgress: StateFlow<UserProgress> = repository.userProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProgress()
        )

    // Saved AI Games State
    val savedGames: StateFlow<List<SavedGame>> = repository.savedGames
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // AI Game Generator State
    private val _aiGameState = MutableStateFlow<AIGameState>(AIGameState.Idle)
    val aiGameState: StateFlow<AIGameState> = _aiGameState.asStateFlow()

    private val _currentAIQuestionIndex = MutableStateFlow(0)
    val currentAIQuestionIndex: StateFlow<Int> = _currentAIQuestionIndex.asStateFlow()

    private val _selectedAIOption = MutableStateFlow<String?>(null)
    val selectedAIOption: StateFlow<String?> = _selectedAIOption.asStateFlow()

    private val _isAIOptionCorrect = MutableStateFlow<Boolean?>(null)
    val isAIOptionCorrect: StateFlow<Boolean?> = _isAIOptionCorrect.asStateFlow()

    private val _showAIFeedback = MutableStateFlow(false)
    val showAIFeedback: StateFlow<Boolean> = _showAIFeedback.asStateFlow()

    // Built-in game states
    private val _mathState = MutableStateFlow(MathBubbleState())
    val mathState: StateFlow<MathBubbleState> = _mathState.asStateFlow()

    private val _memoryState = MutableStateFlow(MemorySafariState())
    val memoryState: StateFlow<MemorySafariState> = _memoryState.asStateFlow()

    private val _patternState = MutableStateFlow(PatternSolverState())
    val patternState: StateFlow<PatternSolverState> = _patternState.asStateFlow()

    // World Quiz States
    private val _currentWorldQuizGameId = MutableStateFlow("")
    val currentWorldQuizGameId: StateFlow<String> = _currentWorldQuizGameId.asStateFlow()

    private val _currentWorldQuizIndex = MutableStateFlow(0)
    val currentWorldQuizIndex: StateFlow<Int> = _currentWorldQuizIndex.asStateFlow()

    private val _selectedWorldQuizOption = MutableStateFlow<String?>(null)
    val selectedWorldQuizOption: StateFlow<String?> = _selectedWorldQuizOption.asStateFlow()

    private val _isWorldQuizOptionCorrect = MutableStateFlow<Boolean?>(null)
    val isWorldQuizOptionCorrect: StateFlow<Boolean?> = _isWorldQuizOptionCorrect.asStateFlow()

    private val _showWorldQuizFeedback = MutableStateFlow(false)
    val showWorldQuizFeedback: StateFlow<Boolean> = _showWorldQuizFeedback.asStateFlow()

    private val _worldQuizScore = MutableStateFlow(0)
    val worldQuizScore: StateFlow<Int> = _worldQuizScore.asStateFlow()

    private val _isWorldQuizCompleted = MutableStateFlow(false)
    val isWorldQuizCompleted: StateFlow<Boolean> = _isWorldQuizCompleted.asStateFlow()

    // Loading overlay
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Decide start screen based on if user profile is already created
        viewModelScope.launch {
            userProgress.collect { progress ->
                if (progress.name != "Sparky" || progress.stars > 0) {
                    if (_currentScreen.value == Screen.Welcome) {
                        _currentScreen.value = Screen.Hub
                    }
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        // Initialize states on transition
        when (screen) {
            is Screen.MathBubbleQuest -> generateMathQuestion()
            is Screen.MemorySafari -> startMemoryGame()
            is Screen.PatternSolver -> generatePatternQuestion()
            is Screen.PlayWorldGame -> {
                when (screen.gameType) {
                    "QUIZ" -> startWorldQuiz(screen.gameId)
                    "MEMORY" -> startWorldMemoryGame(screen.gameId)
                    "MATH" -> generateMathQuestion(screen.worldId)
                    "PATTERN" -> generateWorldPatternQuestion(screen.gameId)
                }
            }
            else -> {}
        }
    }

    // --- Themed Learning Worlds Helpers ---
    fun startWorldQuiz(gameId: String) {
        _currentWorldQuizGameId.value = gameId
        _currentWorldQuizIndex.value = 0
        _selectedWorldQuizOption.value = null
        _isWorldQuizOptionCorrect.value = null
        _showWorldQuizFeedback.value = false
        _worldQuizScore.value = 0
        _isWorldQuizCompleted.value = false
    }

    fun selectWorldQuizOption(option: String, correctAnswer: String) {
        if (_selectedWorldQuizOption.value != null) return
        _selectedWorldQuizOption.value = option
        val correct = option == correctAnswer
        _isWorldQuizOptionCorrect.value = correct
        _showWorldQuizFeedback.value = true
        if (correct) {
            _worldQuizScore.value += 1
            awardRewards(starsEarned = 5, coinsEarned = 2)
        }
    }

    fun nextWorldQuizQuestion(worldId: String) {
        _selectedWorldQuizOption.value = null
        _isWorldQuizOptionCorrect.value = null
        _showWorldQuizFeedback.value = false
        
        val questions = WORLD_QUIZ_QUESTIONS[_currentWorldQuizGameId.value] ?: emptyList()
        val nextIdx = _currentWorldQuizIndex.value + 1
        if (nextIdx < questions.size) {
            _currentWorldQuizIndex.value = nextIdx
        } else {
            // Completed! Reward extra bonus for completing world game
            _isWorldQuizCompleted.value = true
            awardRewards(starsEarned = 10, coinsEarned = 5)
            // Save completed world game as a badge and milestone
            manuallyUnlockBadge("first_step")
            if (worldId == "dino_dig") {
                manuallyUnlockBadge("science_star")
            } else if (worldId == "outer_space") {
                manuallyUnlockBadge("pattern_pro")
            } else if (worldId == "ocean_deep") {
                manuallyUnlockBadge("bubble_popper")
            }
        }
    }

    fun startWorldMemoryGame(gameId: String) {
        val emojis = WORLD_MEMORY_EMOJIS[gameId] ?: listOf("🦁", "🐼", "🐨", "🦊", "🐸", "🐙", "🦖", "🦄")
        val pairs = (emojis + emojis).shuffled()
        
        val cards = pairs.mapIndexed { index, emoji ->
            MemoryCard(id = index, emoji = emoji)
        }

        _memoryState.value = MemorySafariState(
            cards = cards,
            selectedIndices = emptyList(),
            matchesFound = 0,
            totalFlips = 0,
            isGameWon = false
        )
    }

    fun generateWorldPatternQuestion(gameId: String) {
        val questions = WORLD_PATTERN_QUESTIONS[gameId] ?: listOf(
            WorldPatternQuestion(listOf("🍎", "🍌", "🍎", "🍌", "🍎"), 5, listOf("🍎", "🍌", "🍒", "🍇"), "🍌")
        )
        val selected = questions.random()
        _patternState.value = PatternSolverState(
            currentQuestion = PatternQuestion(
                sequence = selected.sequence,
                missingIndex = selected.missingIndex,
                options = selected.options,
                correctAnswer = selected.correctAnswer
            ),
            selectedOption = null,
            isCorrect = null,
            solvedCount = _patternState.value.solvedCount
        )
    }

    // --- Profile Management ---
    fun saveProfile(name: String, avatar: String, grade: String) {
        viewModelScope.launch {
            val current = userProgress.value
            val updated = current.copy(
                name = name.ifBlank { "Sparky" },
                avatar = avatar,
                gradeLevel = grade
            )
            repository.saveProgress(updated)
            navigateTo(Screen.Hub)
        }
    }

    // --- Star & Badges Rewards System ---
    private fun awardRewards(starsEarned: Int, coinsEarned: Int) {
        viewModelScope.launch {
            val current = userProgress.value
            val totalStars = current.stars + starsEarned
            val totalCoins = current.coins + coinsEarned
            val newCompletedCount = current.completedCount + 1
            
            // Level up every 25 stars
            val newLevel = (totalStars / 25) + 1

            var updated = current.copy(
                stars = totalStars,
                coins = totalCoins,
                completedCount = newCompletedCount,
                level = if (newLevel > current.level) newLevel else current.level
            )

            // Dynamic Badge Checks
            val currentBadges = updated.unlockedBadges.split(",").filter { it.isNotEmpty() }.toMutableSet()
            
            if (newCompletedCount >= 1) currentBadges.add("first_step")
            if (totalStars >= 50) currentBadges.add("star_collector")
            if (newCompletedCount >= 20) currentBadges.add("super_kid")

            updated = updated.copy(unlockedBadges = currentBadges.joinToString(","))
            repository.saveProgress(updated)
        }
    }

    fun manuallyUnlockBadge(badgeId: String) {
        viewModelScope.launch {
            val current = userProgress.value
            val currentBadges = current.unlockedBadges.split(",").filter { it.isNotEmpty() }.toMutableSet()
            if (!currentBadges.contains(badgeId)) {
                currentBadges.add(badgeId)
                val updated = current.copy(unlockedBadges = currentBadges.joinToString(","))
                repository.saveProgress(updated)
            }
        }
    }

    // --- AI Dynamic Games (Gemini) ---
    fun generateAIGame(subject: String, theme: String) {
        viewModelScope.launch {
            _aiGameState.value = AIGameState.Loading
            _currentAIQuestionIndex.value = 0
            _selectedAIOption.value = null
            _isAIOptionCorrect.value = null
            _showAIFeedback.value = false

            try {
                val game = GeminiApiClient.generateEducationalGame(
                    subject = subject,
                    theme = theme,
                    gradeLevel = userProgress.value.gradeLevel
                )
                if (game != null) {
                    _aiGameState.value = AIGameState.Success(game)
                } else {
                    _aiGameState.value = AIGameState.Error("Oops! The magical AI forest is asleep. Try again in a minute!")
                }
            } catch (e: Exception) {
                _aiGameState.value = AIGameState.Error(e.message ?: "Could not connect to the magical AI realm. Please verify your internet connection and API Key!")
            }
        }
    }

    fun selectAIOption(option: String, correctAnswer: String) {
        if (_selectedAIOption.value != null) return // Already answered
        _selectedAIOption.value = option
        val correct = option == correctAnswer
        _isAIOptionCorrect.value = correct
        _showAIFeedback.value = true

        if (correct) {
            awardRewards(starsEarned = 5, coinsEarned = 2)
            // Increment category completion badge counts
            viewModelScope.launch {
                val current = userProgress.value
                val currentBadges = current.unlockedBadges.split(",").filter { it.isNotEmpty() }.toMutableSet()
                
                // Track specific subjects
                val state = _aiGameState.value
                if (state is AIGameState.Success) {
                    // Check badge increments if needed, or simply unlock custom badges
                    currentBadges.add("first_step")
                }
                
                val updated = current.copy(unlockedBadges = currentBadges.joinToString(","))
                repository.saveProgress(updated)
            }
        }
    }

    fun nextAIQuestion() {
        _selectedAIOption.value = null
        _isAIOptionCorrect.value = null
        _showAIFeedback.value = false
        
        val state = _aiGameState.value
        if (state is AIGameState.Success) {
            val nextIndex = _currentAIQuestionIndex.value + 1
            if (nextIndex < state.game.questions.size) {
                _currentAIQuestionIndex.value = nextIndex
            } else {
                // Game Over! Earned final badge
                manuallyUnlockBadge("first_step")
                
                // Reward for complete game
                awardRewards(starsEarned = 10, coinsEarned = 5)
                
                // Check customized subject tags for Badges
                val titleLower = state.game.title.lowercase()
                if (titleLower.contains("math") || titleLower.contains("count") || titleLower.contains("number")) {
                    manuallyUnlockBadge("math_magician")
                } else if (titleLower.contains("word") || titleLower.contains("spell") || titleLower.contains("read")) {
                    manuallyUnlockBadge("word_wizard")
                } else if (titleLower.contains("science") || titleLower.contains("nature") || titleLower.contains("animal")) {
                    manuallyUnlockBadge("science_star")
                } else {
                    manuallyUnlockBadge("trivia_titan")
                }
                
                // Save game automatically to history
                saveCurrentGameToDb(state.game)
                
                // Return to hub
                navigateTo(Screen.Hub)
            }
        }
    }

    private fun saveCurrentGameToDb(game: EducationalGame) {
        viewModelScope.launch {
            val gameJson = moshi.adapter(EducationalGame::class.java).toJson(game)
            val savedGame = SavedGame(
                subject = "AI Generator",
                theme = game.title,
                gameJson = gameJson,
                isCompleted = true
            )
            repository.saveGame(savedGame)
        }
    }

    fun playSavedGame(saved: SavedGame) {
        viewModelScope.launch {
            _aiGameState.value = AIGameState.Loading
            _currentAIQuestionIndex.value = 0
            _selectedAIOption.value = null
            _isAIOptionCorrect.value = null
            _showAIFeedback.value = false
            _currentScreen.value = Screen.AIToyBox

            try {
                val game = moshi.adapter(EducationalGame::class.java).fromJson(saved.gameJson)
                if (game != null) {
                    _aiGameState.value = AIGameState.Success(game)
                } else {
                    _aiGameState.value = AIGameState.Error("This saved game scroll is unreadable!")
                }
            } catch (e: Exception) {
                _aiGameState.value = AIGameState.Error("Could not decode the game scroll.")
            }
        }
    }

    fun deleteSavedGame(savedId: Int) {
        viewModelScope.launch {
            repository.deleteSavedGame(savedId)
        }
    }

    fun resetAIGame() {
        _aiGameState.value = AIGameState.Idle
        _currentAIQuestionIndex.value = 0
        _selectedAIOption.value = null
        _isAIOptionCorrect.value = null
        _showAIFeedback.value = false
    }


    // --- Built-in Game 1: Math Bubble Quest ---
    fun generateMathQuestion(worldId: String? = null) {
        val grade = userProgress.value.gradeLevel
        val (num1, num2, operator) = when (grade) {
            "Preschool" -> {
                // Simple addition under 10
                val n1 = Random.nextInt(1, 6)
                val n2 = Random.nextInt(1, 5)
                Triple(n1, n2, "+")
            }
            "Early Elementary" -> {
                // Addition or subtraction under 20
                val op = if (Random.nextBoolean()) "+" else "-"
                val n1 = Random.nextInt(5, 15)
                val n2 = if (op == "-") Random.nextInt(1, n1) else Random.nextInt(1, 10)
                Triple(n1, n2, op)
            }
            else -> {
                // Multiplication or advanced addition/subtraction
                val op = if (Random.nextBoolean()) "×" else if (Random.nextBoolean()) "+" else "-"
                val n1 = if (op == "×") Random.nextInt(2, 10) else Random.nextInt(10, 30)
                val n2 = if (op == "×") Random.nextInt(2, 9) else if (op == "-") Random.nextInt(1, n1) else Random.nextInt(5, 20)
                Triple(n1, n2, op)
            }
        }

        val correctAnswer = when (operator) {
            "+" -> num1 + num2
            "-" -> num1 - num2
            "×" -> num1 * num2
            else -> num1 + num2
        }

        // Generate options (correct + 3 wrong)
        val optionSet = mutableSetOf(correctAnswer)
        while (optionSet.size < 4) {
            val offset = Random.nextInt(-5, 6)
            val wrongAns = correctAnswer + offset
            if (wrongAns > 0 && wrongAns != correctAnswer) {
                optionSet.add(wrongAns)
            }
        }

        val optionsList = optionSet.toList().shuffled()
        
        // Generate bubbles with random colors and positions
        val colors = listOf(0xFF80DEEA, 0xFFFF8A80, 0xFFCE93D8, 0xFFFFF59D, 0xFFA5D6A7, 0xFFFFCC80)
        val bubbles = optionsList.mapIndexed { idx, value ->
            Bubble(
                id = idx,
                value = value,
                xOffset = 0.15f + (idx * 0.22f), // Distribute evenly
                speed = 1.5f + Random.nextFloat() * 1.5f,
                colorHex = colors[idx % colors.size],
                size = 70 + Random.nextInt(0, 25)
            )
        }

        val questionText = when (worldId) {
            "dino_dig" -> {
                when (operator) {
                    "+" -> "Help Rexy count dinosaur eggs! $num1 + $num2 = ? 🥚"
                    "-" -> "Rexy has $num1 eggs and $num2 hatched. How many are left? 🦖"
                    "×" -> "Count the fossil patterns! $num1 × $num2 = ? 🦴"
                    else -> "$num1 $operator $num2 = ?"
                }
            }
            "outer_space" -> {
                when (operator) {
                    "+" -> "Power up Cosmo's rocket fuel! Solve $num1 + $num2! 🚀"
                    "-" -> "There are $num1 stars. Cosmo captured $num2. How many stars left? 🌟"
                    "×" -> "Galaxy pattern solver! $num1 × $num2 = ? 🪐"
                    else -> "$num1 $operator $num2 = ?"
                }
            }
            "ocean_deep" -> {
                when (operator) {
                    "+" -> "Bubbles dolphin has blown bubbles! Solve $num1 + $num2! 🫧"
                    "-" -> "There are $num1 sea shells. $num2 floated away. How many left? 🐚"
                    "×" -> "Schools of fish math! $num1 × $num2 = ? 🐠"
                    else -> "$num1 $operator $num2 = ?"
                }
            }
            else -> {
                when (operator) {
                    "+" -> "How many is $num1 plus $num2? 🍎"
                    "-" -> "What is $num1 minus $num2? 🍪"
                    "×" -> "What is $num1 times $num2? ⭐"
                    else -> "$num1 $operator $num2 = ?"
                }
            }
        }

        _mathState.value = _mathState.value.copy(
            num1 = num1,
            num2 = num2,
            operator = operator,
            correctAnswer = correctAnswer,
            questionText = questionText,
            bubbles = bubbles,
            activeWorldId = worldId
        )
    }

    fun popBubble(bubble: Bubble) {
        val isCorrect = bubble.value == _mathState.value.correctAnswer
        if (isCorrect) {
            // Popped correct answer!
            val newPopped = _mathState.value.poppedCount + 1
            val newStreak = _mathState.value.currentStreak + 1
            
            _mathState.value = _mathState.value.copy(
                poppedCount = newPopped,
                currentStreak = newStreak
            )
            
            awardRewards(starsEarned = 3, coinsEarned = 1)
            manuallyUnlockBadge("bubble_popper")
            
            // Check milestones
            if (newStreak >= 5) {
                manuallyUnlockBadge("math_magician")
            }
            
            generateMathQuestion(_mathState.value.activeWorldId) // Next question preserving world
        } else {
            // Popped incorrect answer
            _mathState.value = _mathState.value.copy(
                currentStreak = 0
            )
        }
    }


    // --- Built-in Game 2: Memory Safari Match ---
    fun startMemoryGame() {
        val emojis = listOf("🦁", "🐼", "🐨", "🦊", "🐸", "🐙", "🦖", "🦄")
        val pairs = (emojis + emojis).shuffled()
        
        val cards = pairs.mapIndexed { index, emoji ->
            MemoryCard(id = index, emoji = emoji)
        }

        _memoryState.value = MemorySafariState(
            cards = cards,
            selectedIndices = emptyList(),
            matchesFound = 0,
            totalFlips = 0,
            isGameWon = false
        )
    }

    fun selectCard(index: Int) {
        val state = _memoryState.value
        if (state.selectedIndices.size >= 2) return // Wait for reset
        if (state.cards[index].isFlipped || state.cards[index].isMatched) return

        // Flip selected card
        val updatedCards = state.cards.mapIndexed { idx, card ->
            if (idx == index) card.copy(isFlipped = true) else card
        }

        val newSelection = state.selectedIndices + index
        val isTwoSelected = newSelection.size == 2

        _memoryState.value = state.copy(
            cards = updatedCards,
            selectedIndices = newSelection,
            totalFlips = state.totalFlips + 1
        )

        if (isTwoSelected) {
            val idx1 = newSelection[0]
            val idx2 = newSelection[1]
            val card1 = updatedCards[idx1]
            val card2 = updatedCards[idx2]

            viewModelScope.launch {
                kotlinx.coroutines.delay(1000) // Keep visible for 1 sec
                
                val match = card1.emoji == card2.emoji
                val finalCards = _memoryState.value.cards.mapIndexed { idx, card ->
                    if (idx == idx1 || idx == idx2) {
                        card.copy(
                            isFlipped = match, // Keep face up if matched, else face down
                            isMatched = match
                        )
                    } else {
                        card
                    }
                }

                val matches = if (match) _memoryState.value.matchesFound + 1 else _memoryState.value.matchesFound
                val won = matches == 8

                _memoryState.value = _memoryState.value.copy(
                    cards = finalCards,
                    selectedIndices = emptyList(),
                    matchesFound = matches,
                    isGameWon = won
                )

                if (match) {
                    awardRewards(starsEarned = 4, coinsEarned = 2)
                    manuallyUnlockBadge("memory_master")
                }
                
                if (won) {
                    awardRewards(starsEarned = 10, coinsEarned = 5)
                    manuallyUnlockBadge("super_kid")
                }
            }
        }
    }


    // --- Built-in Game 3: Magic Pattern Solver ---
    fun generatePatternQuestion() {
        val questions = listOf(
            PatternQuestion(listOf("🍎", "🍌", "🍎", "🍌", "🍎"), 5, listOf("🍎", "🍌", "🍒", "🍇"), "🍌"),
            PatternQuestion(listOf("🚗", "✈️", "🚗", "✈️", "🚗"), 5, listOf("🚗", "✈️", "🚢", "🚲"), "✈️"),
            PatternQuestion(listOf("⭐", "🌙", "⭐", "🌙", "⭐"), 5, listOf("⭐", "🌙", "☀️", "☁️"), "🌙"),
            PatternQuestion(listOf("🦁", "🐼", "🦁", "🐼", "🦁"), 5, listOf("🦁", "🐼", "🦊", "🐸"), "🐼"),
            PatternQuestion(listOf("🔴", "🔵", "🔴", "🔵", "🔴"), 5, listOf("🔴", "🔵", "🟢", "🟡"), "🔵"),
            PatternQuestion(listOf("🍀", "🌸", "🍀", "🌸", "🍀"), 5, listOf("🍀", "🌸", "🍁", "🍄"), "🌸"),
            PatternQuestion(listOf("🎈", "🎁", "🎈", "🎁", "🎈"), 5, listOf("🎈", "🎁", "🎂", "🎉"), "🎁"),
            PatternQuestion(listOf("🐶", "🐱", "🐶", "🐱", "🐶"), 5, listOf("🐶", "🐱", "🐹", "🐰"), "🐱")
        )

        val selected = questions.random()
        _patternState.value = PatternSolverState(
            currentQuestion = selected,
            selectedOption = null,
            isCorrect = null,
            solvedCount = _patternState.value.solvedCount
        )
    }

    fun selectPatternOption(option: String) {
        val state = _patternState.value
        if (state.selectedOption != null) return // Already answered
        
        val correct = option == state.currentQuestion?.correctAnswer
        val solvedCount = if (correct) state.solvedCount + 1 else state.solvedCount

        _patternState.value = state.copy(
            selectedOption = option,
            isCorrect = correct,
            solvedCount = solvedCount
        )

        if (correct) {
            awardRewards(starsEarned = 3, coinsEarned = 1)
            manuallyUnlockBadge("pattern_pro")
            if (solvedCount >= 3) {
                manuallyUnlockBadge("super_kid")
            }
        }
    }
}
