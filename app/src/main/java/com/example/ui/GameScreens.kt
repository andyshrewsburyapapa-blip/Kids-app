package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.network.EducationalGame
import com.example.network.GameQuestion
import com.example.ui.theme.*
import com.example.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- Simple Particle Effect for Celebrating Kids' Wins ---
data class ConfettiParticle(
    val id: Int,
    val x: Float,
    val y: Float,
    val color: Color,
    val size: Float,
    val speedX: Float,
    val speedY: Float
)

@Composable
fun ConfettiRain(trigger: Boolean) {
    if (!trigger) return
    var particles by remember { mutableStateOf(emptyList<ConfettiParticle>()) }

    LaunchedEffect(trigger) {
        val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Magenta, Color.Cyan, Color.White, Color.Blue)
        particles = List(50) { id ->
            ConfettiParticle(
                id = id,
                x = Random.nextFloat() * 1000f,
                y = -50f,
                color = colors.random(),
                size = 15f + Random.nextFloat() * 20f,
                speedX = -5f + Random.nextFloat() * 10f,
                speedY = 10f + Random.nextFloat() * 15f
            )
        }

        val frameDuration = 16L
        val endTime = System.currentTimeMillis() + 2000L // Run for 2 seconds
        while (System.currentTimeMillis() < endTime) {
            particles = particles.map { p ->
                p.copy(
                    x = p.x + p.speedX,
                    y = p.y + p.speedY
                )
            }
            delay(frameDuration)
        }
        particles = emptyList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            Box(
                modifier = Modifier
                    .offset(x = p.x.dp, y = p.y.dp)
                    .size(p.size.dp)
                    .background(p.color, CircleShape)
            )
        }
    }
}

// --- Main Container Router ---
@Composable
fun KidsLearningAppContent(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val progress by viewModel.userProgress.collectAsState()
    val savedGames by viewModel.savedGames.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(KidsSkyBlue, KidsBackgroundLight)
                    )
                )
        ) {
            Crossfade(
                targetState = currentScreen,
                animationSpec = tween(durationMillis = 350),
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Welcome -> WelcomeScreen(
                        progress = progress,
                        onEnter = { name, avatar, grade ->
                            viewModel.saveProfile(name, avatar, grade)
                        }
                    )
                    is Screen.Hub -> HubScreen(
                        progress = progress,
                        savedGames = savedGames,
                        onNavigate = { viewModel.navigateTo(it) },
                        onPlaySaved = { viewModel.playSavedGame(it) },
                        onDeleteSaved = { viewModel.deleteSavedGame(it) }
                    )
                    is Screen.AIToyBox -> AIToyBoxScreen(
                        viewModel = viewModel,
                        progress = progress,
                        onBack = { viewModel.navigateTo(Screen.Hub) }
                    )
                    is Screen.MathBubbleQuest -> MathBubbleScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(Screen.Hub) }
                    )
                    is Screen.MemorySafari -> MemoryScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(Screen.Hub) }
                    )
                    is Screen.PatternSolver -> PatternScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(Screen.Hub) }
                    )
                    is Screen.TrophyRoom -> TrophyRoomScreen(
                        progress = progress,
                        onBack = { viewModel.navigateTo(Screen.Hub) }
                    )
                    is Screen.WorldDetail -> WorldDetailScreen(
                        viewModel = viewModel,
                        worldId = screen.worldId,
                        onBack = { viewModel.navigateTo(Screen.Hub) }
                    )
                    is Screen.PlayWorldGame -> {
                        when (screen.gameType) {
                            "QUIZ" -> WorldQuizScreen(
                                viewModel = viewModel,
                                worldId = screen.worldId,
                                gameId = screen.gameId,
                                onBack = { viewModel.navigateTo(Screen.WorldDetail(screen.worldId)) }
                            )
                            "MEMORY" -> MemoryScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Screen.WorldDetail(screen.worldId)) }
                            )
                            "MATH" -> MathBubbleScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Screen.WorldDetail(screen.worldId)) }
                            )
                            "PATTERN" -> PatternScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Screen.WorldDetail(screen.worldId)) }
                            )
                            else -> viewModel.navigateTo(Screen.WorldDetail(screen.worldId))
                        }
                    }
                }
            }
        }
    }
}

// --- 1. Welcome Screen ---
@Composable
fun WelcomeScreen(
    progress: UserProgress,
    onEnter: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(if (progress.name == "Sparky") "" else progress.name) }
    var selectedAvatar by remember { mutableStateOf(progress.avatar) }
    var selectedGrade by remember { mutableStateOf(progress.gradeLevel) }

    val avatars = listOf("🦁", "🐼", "🦄", "🦖", "🦊", "🐯", "🐨", "🐸")
    val grades = listOf("Preschool", "Early Elementary", "Late Elementary")
    val gradeLabels = mapOf(
        "Preschool" to "🎒 Preschool (Ages 3-5)",
        "Early Elementary" to "🏫 Early Grades (Ages 6-8)",
        "Late Elementary" to "🧠 Brainy Explorer (Ages 9-11)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Kids Learning Land! ✨",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = KidsCoralOrange,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Play dynamic, infinite educational games and earn stars!",
                fontSize = 16.sp,
                color = Color(0xFF556B2F),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Pick your animal buddy!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1C3144)
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        avatars.forEach { emoji ->
                            val isSelected = selectedAvatar == emoji
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) KidsSunshineYellow else KidsSkyBlue)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) KidsCoralOrange else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAvatar = emoji }
                                    .testTag("avatar_$emoji"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 36.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. What is your name?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1C3144)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Write your name...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KidsCoralOrange,
                            unfocusedBorderColor = KidsSkyBlue
                        )
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. Select your learning land!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1C3144)
                    )

                    grades.forEach { grade ->
                        val isSelected = selectedGrade == grade
                        Button(
                            onClick = { selectedGrade = grade },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) KidsCoralOrange else KidsSkyBlue,
                                contentColor = if (isSelected) Color.White else Color(0xFF1C3144)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("grade_$grade"),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = gradeLabels[grade] ?: grade,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onEnter(name, selectedAvatar, selectedGrade) },
                colors = ButtonDefaults.buttonColors(containerColor = KidsMeadowGreen),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("enter_btn"),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "✨ Let's Play! ✨", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- 2. Main Hub Dashboard ---
@Composable
fun HubScreen(
    progress: UserProgress,
    savedGames: List<SavedGame>,
    onNavigate: (Screen) -> Unit,
    onPlaySaved: (SavedGame) -> Unit,
    onDeleteSaved: (Int) -> Unit
) {
    var showSavedGamesDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Kids Header Status Row
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(KidsSkyBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = progress.avatar, fontSize = 32.sp)
                        }

                        Column {
                            Text(
                                text = progress.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF1C3144)
                            )
                            Box(
                                modifier = Modifier
                                    .background(KidsSunshineYellow, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LVL ${progress.level}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "⭐", fontSize = 22.sp)
                            Text(
                                text = "${progress.stars}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFE65100)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🪙", fontSize = 22.sp)
                            Text(
                                text = "${progress.coins}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
        }

        // Custom generated illustration banner
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box {
                    Image(
                        painter = painterResource(id = R.drawable.img_kids_hero_banner),
                        contentDescription = "Cartoon Adventure Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0x99000000))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Infinite Educational Magic Map!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Ask the AI wizard for any game or play offline!",
                            fontSize = 12.sp,
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }
            }
        }

        // Quick Navigation shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onNavigate(Screen.TrophyRoom) },
                    colors = ButtonDefaults.buttonColors(containerColor = KidsLavenderPurple),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("trophy_btn"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = "Trophies", tint = Color.White)
                        Text("Trophy Room", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Button(
                    onClick = { showSavedGamesDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = KidsBubblegumPink),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("history_btn"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.List, contentDescription = "History", tint = Color.White)
                        Text("Game History", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Section: Themed Learning Worlds
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🗺️ Magic Adventure Worlds 🗺️",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color(0xFF2E3E14),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                THEMED_WORLDS.forEach { world ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .width(280.dp)
                            .height(210.dp)
                            .clickable { onNavigate(Screen.WorldDetail(world.id)) }
                            .testTag("world_card_${world.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color(world.backgroundColorHex)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        border = BorderStroke(3.dp, Color(world.primaryColorHex))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = world.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF1C3144),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(text = world.bannerEmoji, fontSize = 28.sp)
                                }
                                Text(
                                    text = world.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF424242),
                                    lineHeight = 15.sp,
                                    maxLines = 3
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = world.characterEmoji, fontSize = 14.sp)
                                    Text(
                                        text = world.characterName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        color = Color(0xFF1C3144)
                                    )
                                }

                                Button(
                                    onClick = { onNavigate(Screen.WorldDetail(world.id)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(world.primaryColorHex)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Enter! 🚀", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title 1: AI Endless Game Generator
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✨ Dynamic AI Toy Box ✨",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color(0xFF2E3E14),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // Magical AI Entry Portal Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(3.dp, KidsSunshineYellow),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Screen.AIToyBox) }
                    .testTag("ai_toy_box_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(KidsSunshineYellow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🪄", fontSize = 42.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Infinite Game Wizard",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = KidsCoralOrange
                        )
                        Text(
                            text = "Choose Space, Dinosaurs, Safari, Math, or Spelling to generate custom puzzle quests!",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Go",
                        tint = KidsCoralOrange,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Section Title 2: Built-in Games
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🫧 Offline Playground Zones 🫧",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color(0xFF2E3E14),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
        }

        // 3 Offline Games grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Math Bubbles
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = KidsSkyBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.MathBubbleQuest) }
                        .testTag("math_bubbles_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "🫧", fontSize = 36.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Math Bubble Quest", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D47A1))
                            Text(text = "Pop answer bubbles to solve addition & subtraction!", fontSize = 12.sp, color = Color(0xFF1565C0))
                        }
                    }
                }

                // Memory Safari
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.MemorySafari) }
                        .testTag("memory_safari_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "🦁", fontSize = 36.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Animal Memory Safari", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1B5E20))
                            Text(text = "Find pair matches of smiling animals to train focus!", fontSize = 12.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }

                // Pattern Solver
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.PatternSolver) }
                        .testTag("pattern_solver_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "🧩", fontSize = 36.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Magic Pattern Solver", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFE65100))
                            Text(text = "Solve logical sequences and complete shape chains!", fontSize = 12.sp, color = Color(0xFFEF6C00))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Saved Games History Sheet/Dialog
    if (showSavedGamesDialog) {
        AlertDialog(
            onDismissRequest = { showSavedGamesDialog = false },
            confirmButton = {
                TextButton(onClick = { showSavedGamesDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = KidsCoralOrange)
                }
            },
            title = {
                Text(
                    text = "Your Magic Game Scrolls 📜",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF1C3144)
                )
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 350.dp)) {
                    if (savedGames.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🐪", fontSize = 48.sp)
                            Text(
                                "No saved games yet!",
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Play an AI game to save it in your infinite inventory scrolls!",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(savedGames) { game ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = KidsSkyBlue)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = game.theme,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF0D47A1)
                                            )
                                            Text(
                                                text = "Completed & Saved",
                                                fontSize = 11.sp,
                                                color = Color(0xFF1565C0)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = { onPlaySaved(game) }) {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = KidsMeadowGreen)
                                            }
                                            IconButton(onClick = { onDeleteSaved(game.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// --- 3. Dynamic AI Game Generator Screen ---
@Composable
fun AIToyBoxScreen(
    viewModel: GameViewModel,
    progress: UserProgress,
    onBack: () -> Unit
) {
    val aiGameState by viewModel.aiGameState.collectAsState()
    val activeIdx by viewModel.currentAIQuestionIndex.collectAsState()
    val selectedOpt by viewModel.selectedAIOption.collectAsState()
    val isCorrect by viewModel.isAIOptionCorrect.collectAsState()
    val showFeedback by viewModel.showAIFeedback.collectAsState()

    var selectedSubject by remember { mutableStateOf("Math") }
    var selectedTheme by remember { mutableStateOf("Dinosaurs") }

    val subjects = listOf("Math", "Spelling", "Science", "Trivia")
    val subjectIcons = mapOf(
        "Math" to "🧮",
        "Spelling" to "🔤",
        "Science" to "🔬",
        "Trivia" to "🧠"
    )

    val themes = listOf("Space", "Dinosaurs", "Safari Animals", "Knights & Castles", "Deep Ocean")
    val themeIcons = mapOf(
        "Space" to "🚀",
        "Dinosaurs" to "🦖",
        "Safari Animals" to "🦁",
        "Knights & Castles" to "🏰",
        "Deep Ocean" to "🪸"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Simple Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    viewModel.resetAIGame()
                    onBack()
                },
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .testTag("ai_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KidsCoralOrange)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "AI Infinite Quest",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF2E3E14)
            )
        }

        Crossfade(targetState = aiGameState, label = "AI_State_Crossfade") { state ->
            when (state) {
                is AIGameState.Idle -> {
                    // Selection Setup screen
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "🪄 Magical AI Game Forge 🪄",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = KidsCoralOrange,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Combine subjects and cartoon themes. Our AI will build a personalized puzzle game instantly! With millions of variations, you'll never run out!",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Subject Pickers
                        item {
                            Text(text = "1. Pick Game Topic", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1C3144))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                subjects.forEach { sub ->
                                    val isSel = selectedSubject == sub
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSel) KidsCoralOrange else Color.White
                                        ),
                                        border = BorderStroke(2.dp, if (isSel) Color.Transparent else KidsSkyBlue),
                                        modifier = Modifier
                                            .width(120.dp)
                                            .clickable { selectedSubject = sub }
                                            .testTag("sub_$sub")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(text = subjectIcons[sub] ?: "⭐", fontSize = 36.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = sub,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isSel) Color.White else Color(0xFF1C3144)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Theme Pickers
                        item {
                            Text(text = "2. Pick Visual Theme", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1C3144))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                themes.forEach { th ->
                                    val isSel = selectedTheme == th
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSel) KidsLavenderPurple else Color.White
                                        ),
                                        border = BorderStroke(2.dp, if (isSel) Color.Transparent else KidsSkyBlue),
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clickable { selectedTheme = th }
                                            .testTag("theme_$th")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(text = themeIcons[th] ?: "🏰", fontSize = 36.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = th,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isSel) Color.White else Color(0xFF1C3144),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Generate Button
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.generateAIGame(selectedSubject, selectedTheme) },
                                colors = ButtonDefaults.buttonColors(containerColor = KidsMeadowGreen),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .testTag("generate_game_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🪄 Conjure Game! 🪄", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                is AIGameState.Loading -> {
                    // AI Generating screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(KidsSkyBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🧙‍♂️", fontSize = 64.sp)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Consulting the Game Wizard...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = KidsLavenderPurple,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Crafting custom quiz-puzzles for $selectedSubject in the world of $selectedTheme!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        CircularProgressIndicator(color = KidsCoralOrange)
                    }
                }
                is AIGameState.Success -> {
                    // Active Gameplay Screen!
                    val game = state.game
                    val currentQuestion = game.questions.getOrNull(activeIdx)

                    if (currentQuestion != null) {
                        // Confetti Trigger
                        val isCelebrating = showFeedback && (isCorrect == true)
                        ConfettiRain(trigger = isCelebrating)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Badge Target Preview Header
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = KidsSunshineYellow.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = game.rewardBadgeEmoji, fontSize = 24.sp)
                                        Column {
                                            Text(
                                                text = "Quest Reward: ${game.rewardBadgeTitle}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFFE65100)
                                            )
                                        }
                                    }
                                }
                            }

                            // Intro story bubble
                            if (activeIdx == 0) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "📖 Mission Intro",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = KidsLavenderPurple
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = game.story,
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp,
                                                color = Color(0xFF374151)
                                            )
                                        }
                                    }
                                }
                            }

                            // Dynamic Question Progress
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Challenge ${activeIdx + 1} of ${game.questions.size}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1C3144)
                                        )
                                        Text(
                                            text = "${(activeIdx * 100) / game.questions.size}% Complete",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = KidsMeadowGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (activeIdx + 1).toFloat() / game.questions.size.toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        color = KidsCoralOrange,
                                        trackColor = KidsSkyBlue
                                    )
                                }
                            }

                            // Question Card
                            item {
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = currentQuestion.question,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = Color(0xFF1C3144),
                                            lineHeight = 24.sp
                                        )

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            currentQuestion.options.forEachIndexed { optIdx, option ->
                                                val isSelected = selectedOpt == option
                                                val isThisCorrect = option == currentQuestion.answer
                                                val btnColor = when {
                                                    selectedOpt != null && isThisCorrect -> KidsMeadowGreen
                                                    isSelected && !isThisCorrect -> KidsCoralOrange
                                                    else -> KidsSkyBlue
                                                }

                                                Button(
                                                    onClick = { viewModel.selectAIOption(option, currentQuestion.answer) },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = btnColor,
                                                        contentColor = if (selectedOpt != null && (isThisCorrect || isSelected)) Color.White else Color(0xFF1C3144)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(54.dp)
                                                        .testTag("ai_option_$optIdx"),
                                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                                ) {
                                                    Text(text = option, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Feedback and Explanation Card
                            if (showFeedback) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCorrect == true) Color(0xFFE8F5E9) else Color(0xFFFBE9E7)
                                        ),
                                        border = BorderStroke(2.dp, if (isCorrect == true) KidsMeadowGreen else KidsCoralOrange),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = if (isCorrect == true) "🎉 Stellar! That's Correct!" else "💡 Not quite, but you can try again!",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = if (isCorrect == true) Color(0xFF1B5E20) else Color(0xFFD84315)
                                            )
                                            Text(
                                                text = currentQuestion.explanation,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center,
                                                color = Color(0xFF374151)
                                            )

                                            if (isCorrect == true) {
                                                Button(
                                                    onClick = { viewModel.nextAIQuestion() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = KidsMeadowGreen),
                                                    shape = RoundedCornerShape(16.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.6f)
                                                        .testTag("ai_next_btn")
                                                ) {
                                                    Text("Next Match ➡️", fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    }
                }
                is AIGameState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🏜️", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Magical Forest Signal Weak",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = KidsCoralOrange,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.resetAIGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = KidsCoralOrange),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Try Again", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- 4. Trophy Room Screen ---
@Composable
fun TrophyRoomScreen(
    progress: UserProgress,
    onBack: () -> Unit
) {
    val unlockedSet = remember(progress.unlockedBadges) {
        progress.unlockedBadges.split(",").filter { it.isNotEmpty() }.toSet()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .testTag("trophy_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KidsCoralOrange)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Trophy Room",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF2E3E14)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ALL_BADGES) { badge ->
                val isUnlocked = unlockedSet.contains(badge.id)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) Color.White else Color(0xFFE0E0E0).copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(2.dp, if (isUnlocked) KidsSunshineYellow else Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 4.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(if (isUnlocked) KidsSkyBlue else Color(0xFFBDBDBD), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badge.iconEmoji,
                                fontSize = 36.sp,
                                modifier = Modifier.graphicsLayer(alpha = if (isUnlocked) 1f else 0.4f)
                            )
                        }

                        Text(
                            text = badge.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isUnlocked) Color(0xFF1C3144) else Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = badge.description,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp,
                            modifier = Modifier.height(42.dp)
                        )

                        Box(
                            modifier = Modifier
                                .background(
                                    if (isUnlocked) KidsMeadowGreen else Color.LightGray,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isUnlocked) "UNLOCKED 🏆" else badge.requirementText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 5. Math Bubble Quest Screen (Offline Interactive Game) ---
@Composable
fun MathBubbleScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.mathState.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Draw playful wave lines on background
                drawCircle(
                    color = Color(0x1F80DEEA),
                    radius = 300f,
                    center = Offset(100f, 150f)
                )
                drawCircle(
                    color = Color(0x1FCE93D8),
                    radius = 450f,
                    center = Offset(800f, 1000f)
                )
            }
    ) {
        // Game Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.White, CircleShape)
                        .testTag("math_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KidsCoralOrange)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Bubble Quest",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF0D47A1)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = "🔥 Streak:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                Text(text = "${state.currentStreak}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFFE65100))
            }
        }

        // Question board
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(3.dp, KidsSkyBlue),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("math_question_card"),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tap the Bubble with the correct answer! 🫧",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.questionText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1565C0),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Floating Bubbles Box Arena
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            state.bubbles.forEach { bubble ->
                // Infinite gentle bobbing bounce effect for each bubble
                val infiniteTransition = rememberInfiniteTransition(label = "bubble_bounce_${bubble.id}")
                val verticalOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = (screenHeight / 3).toFloat() * 0.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = (1500 / bubble.speed).toInt() + 1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "v_offset_${bubble.id}"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = (LocalConfiguration.current.screenWidthDp * bubble.xOffset).dp,
                            y = (verticalOffset + 50f).dp
                        )
                        .size(bubble.size.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(bubble.colorHex), Color(bubble.colorHex).copy(alpha = 0.5f))
                            )
                        )
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { viewModel.popBubble(bubble) }
                        .testTag("bubble_${bubble.value}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${bubble.value}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// --- 6. Memory Safari Game Screen (Offline Interactive Game) ---
@Composable
fun MemoryScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.memoryState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.White, CircleShape)
                        .testTag("memory_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KidsCoralOrange)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Memory Safari",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF1B5E20)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Flips: ${state.totalFlips}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Matches: ${state.matchesFound}/8",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = KidsMeadowGreen
                )
            }
        }

        // Victory celebration card
        if (state.isGameWon) {
            ConfettiRain(trigger = true)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                border = BorderStroke(3.dp, KidsMeadowGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "👑 VICTORY! 👑", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    Text(
                        text = "Outstanding safari track! You successfully matched all sleeping animals! You earned 10 stars and 5 coins!",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF2E7D32)
                    )
                    Button(
                        onClick = { viewModel.startMemoryGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = KidsMeadowGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Safari Reset 🔄", color = Color.White)
                    }
                }
            }
        }

        // Card Deck Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.cards.size) { idx ->
                    val card = state.cards[idx]
                    val flipped = card.isFlipped || card.isMatched

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (flipped) Color.White else KidsLavenderPurple)
                            .border(2.dp, if (card.isMatched) KidsMeadowGreen else KidsSkyBlue, RoundedCornerShape(16.dp))
                            .clickable { viewModel.selectCard(idx) }
                            .testTag("memory_card_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (flipped) {
                            Text(text = card.emoji, fontSize = 36.sp)
                        } else {
                            Text(text = "❓", fontSize = 28.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- 7. Pattern Solver Game Screen (Offline Interactive Game) ---
@Composable
fun PatternScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.patternState.collectAsState()
    val isWinCelebrating = state.isCorrect == true

    ConfettiRain(trigger = isWinCelebrating)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .testTag("pattern_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KidsCoralOrange)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Pattern Solver",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFFE65100)
            )
        }

        val q = state.currentQuestion
        if (q != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "What comes next in this pattern? 🧩",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }

                // Sequence card display
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(2.dp, KidsSkyBlue)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            q.sequence.forEachIndexed { index, emoji ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (index > 0) {
                                        Text(text = " ➔ ", fontSize = 16.sp, color = Color.Gray)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(KidsSkyBlue, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 28.sp)
                                    }
                                }
                            }

                            Text(text = " ➔ ", fontSize = 16.sp, color = Color.Gray)

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(KidsSunshineYellow, CircleShape)
                                    .border(2.dp, KidsCoralOrange, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (state.selectedOption != null) state.selectedOption!! else "❓",
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }
                }

                // Options
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        q.options.forEachIndexed { oIdx, opt ->
                            val isChosen = state.selectedOption == opt
                            val isThisCorrect = opt == q.correctAnswer
                            val col = when {
                                state.selectedOption != null && isThisCorrect -> KidsMeadowGreen
                                isChosen && !isThisCorrect -> KidsCoralOrange
                                else -> Color.White
                            }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = col),
                                border = BorderStroke(2.dp, KidsSkyBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { viewModel.selectPatternOption(opt) }
                                    .testTag("pattern_option_$oIdx")
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = opt, fontSize = 32.sp)
                                }
                            }
                        }
                    }
                }

                // Feedback
                if (state.selectedOption != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.isCorrect == true) Color(0xFFE8F5E9) else Color(0xFFFBE9E7)
                            ),
                            border = BorderStroke(2.dp, if (state.isCorrect == true) KidsMeadowGreen else KidsCoralOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (state.isCorrect == true) "🎉 Magical! That completes the chain!" else "💡 Almost! Let's think, what is repeating?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (state.isCorrect == true) Color(0xFF1B5E20) else Color(0xFFD84315),
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = { viewModel.generatePatternQuestion() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (state.isCorrect == true) KidsMeadowGreen else KidsCoralOrange
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = if (state.isCorrect == true) "Next Pattern ➡️" else "Try Again 🔄",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 5. Themed World Detail Screen ---
@Composable
fun WorldDetailScreen(
    viewModel: GameViewModel,
    worldId: String,
    onBack: () -> Unit
) {
    val world = THEMED_WORLDS.find { it.id == worldId } ?: return
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(world.backgroundColorHex))
    ) {
        // Custom Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(world.primaryColorHex))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .size(40.dp)
                    .testTag("world_detail_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Hub Map",
                    tint = Color(world.primaryColorHex),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${world.bannerEmoji} ${world.name}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color.White
            )
        }

        // Main List Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mascot Speech bubble section
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(3.dp, Color(world.primaryColorHex)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(world.secondaryColorHex), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = world.characterEmoji, fontSize = 44.sp)
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = world.characterName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(world.primaryColorHex)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${world.characterGreeting}\"",
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color.DarkGray,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Games list heading
            item {
                Text(
                    text = "🌟 World Challenges 🌟",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Display games
            items(world.games) { game ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, Color(world.primaryColorHex)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.navigateTo(
                                Screen.PlayWorldGame(
                                    worldId = world.id,
                                    gameId = game.id,
                                    gameType = game.gameType
                                )
                            )
                        }
                        .testTag("world_game_item_${game.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(world.secondaryColorHex).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = game.emoji, fontSize = 32.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = game.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1F2937)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = game.description,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280),
                                lineHeight = 15.sp
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.navigateTo(
                                    Screen.PlayWorldGame(
                                        worldId = world.id,
                                        gameId = game.id,
                                        gameType = game.gameType
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(world.primaryColorHex)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Play!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- 6. Themed World Quiz Screen ---
@Composable
fun WorldQuizScreen(
    viewModel: GameViewModel,
    worldId: String,
    gameId: String,
    onBack: () -> Unit
) {
    val world = THEMED_WORLDS.find { it.id == worldId } ?: return
    val questions = WORLD_QUIZ_QUESTIONS[gameId] ?: emptyList()
    
    val currentIdx by viewModel.currentWorldQuizIndex.collectAsState()
    val selectedOption by viewModel.selectedWorldQuizOption.collectAsState()
    val isCorrect by viewModel.isWorldQuizOptionCorrect.collectAsState()
    val showFeedback by viewModel.showWorldQuizFeedback.collectAsState()
    val score by viewModel.worldQuizScore.collectAsState()
    val isCompleted by viewModel.isWorldQuizCompleted.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(world.backgroundColorHex))
    ) {
        // Custom Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(world.primaryColorHex))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .size(40.dp)
                    .testTag("world_quiz_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(world.primaryColorHex),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${world.characterEmoji} ${world.characterName}'s Quiz",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color.White
            )
        }

        if (isCompleted || questions.isEmpty()) {
            // Quest complete screen!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(4.dp, Color(world.primaryColorHex)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "🏆", fontSize = 72.sp)
                        
                        Text(
                            text = "Quest Completed!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(world.primaryColorHex),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Amazing job! You scored $score out of ${questions.size} on the trivia quest!",
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        // Mascot cheer
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(world.secondaryColorHex).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(text = world.characterEmoji, fontSize = 36.sp)
                                Text(
                                    text = "${world.characterName} says: \"Roar-tastic! You're an absolute star! Here are your treasures!\"",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF374151),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Rewards row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "⭐", fontSize = 24.sp)
                                Text(text = "+10 Stars", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "🪙", fontSize = 24.sp)
                                Text(text = "+5 Coins", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(world.primaryColorHex)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("world_quiz_complete_back")
                        ) {
                            Text("Back to World Map 🗺️", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
            }
        } else {
            val q = questions.getOrNull(currentIdx)!!
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header status
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${currentIdx + 1} of ${questions.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Score: $score ⭐",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(world.primaryColorHex)
                        )
                    }
                }

                // Question card
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(2.dp, Color(world.primaryColorHex))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = q.question,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1F2937),
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                // Options list
                items(q.options) { option ->
                    val isSelected = selectedOption == option
                    val isCorrectOption = option == q.answer
                    val containerColor = when {
                        selectedOption == null -> Color.White
                        isCorrectOption -> Color(0xFFE8F5E9)
                        isSelected -> Color(0xFFFBE9E7)
                        else -> Color.White
                    }
                    val borderStrokeColor = when {
                        selectedOption == null -> Color(world.primaryColorHex).copy(alpha = 0.4f)
                        isCorrectOption -> Color(0xFF2E7D32)
                        isSelected -> Color(0xFFC62828)
                        else -> Color.LightGray
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        border = BorderStroke(2.dp, borderStrokeColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = selectedOption == null) {
                                viewModel.selectWorldQuizOption(option, q.answer)
                            }
                            .testTag("world_quiz_option_$option")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            if (selectedOption != null) {
                                if (isCorrectOption) {
                                    Text("✅", fontSize = 18.sp)
                                } else if (isSelected) {
                                    Text("❌", fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }

                // Mascot Feedback bubble
                if (showFeedback) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect == true) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            ),
                            border = BorderStroke(2.dp, if (isCorrect == true) Color(0xFF2E7D32) else Color(0xFFEF6C00)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = world.characterEmoji, fontSize = 28.sp)
                                    Text(
                                        text = if (isCorrect == true) "🎉 Stellar Job! Correct!" else "💡 Encouragement!",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (isCorrect == true) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                                    )
                                }

                                Text(
                                    text = q.explanation,
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { viewModel.nextWorldQuizQuestion(worldId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCorrect == true) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = if (currentIdx + 1 < questions.size) "Next Question ➡️" else "Finish Quest 🏁",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
