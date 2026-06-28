package com.example.data

data class LearningWorld(
    val id: String,
    val name: String,
    val description: String,
    val characterName: String,
    val characterEmoji: String,
    val characterGreeting: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val backgroundColorHex: Long,
    val bannerEmoji: String,
    val games: List<WorldGame>
)

data class WorldGame(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val gameType: String // "QUIZ", "MEMORY", "MATH", "PATTERN"
)

data class WorldQuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val answer: String,
    val explanation: String
)

data class WorldPatternQuestion(
    val sequence: List<String>,
    val missingIndex: Int,
    val options: List<String>,
    val correctAnswer: String
)

val THEMED_WORLDS = listOf(
    LearningWorld(
        id = "dino_dig",
        name = "Dinosaur Dig Land",
        description = "Travel back in time to dig up fossils, count dino eggs, and learn paleontology secrets!",
        characterName = "Rexy the T-Rex",
        characterEmoji = "🦖",
        characterGreeting = "Roar! I'm Rexy! Let's explore the jungle and dig up fossil treasures together!",
        primaryColorHex = 0xFFFF7043, // Coral Orange
        secondaryColorHex = 0xFFFFEE58, // Sunshine Yellow
        backgroundColorHex = 0xFFF1F8E9, // Light Green/Safari
        bannerEmoji = "🌋",
        games = listOf(
            WorldGame(
                id = "dino_quiz",
                name = "Fossil Hunter Quiz",
                description = "Solve tricky fossil trivia to discover prehistoric monsters!",
                emoji = "🦴",
                gameType = "QUIZ"
            ),
            WorldGame(
                id = "dino_memory",
                name = "Dino Jungle Pairs",
                description = "Flip and match giant prehistoric beasts and plants!",
                emoji = "🦕",
                gameType = "MEMORY"
            ),
            WorldGame(
                id = "dino_math",
                name = "Egg Hatcher Math",
                description = "Pop math bubbles to help hatch baby dinosaur eggs!",
                emoji = "🥚",
                gameType = "MATH"
            )
        )
    ),
    LearningWorld(
        id = "outer_space",
        name = "Outer Space Adventure",
        description = "Blast off to cosmic galaxies, solve alien patterns, and capture glowing space star numbers!",
        characterName = "Cosmo the Alien",
        characterEmoji = "👽",
        characterGreeting = "Greetings, Earthling! I'm Cosmo. Help me power up my starship by learning astronomy!",
        primaryColorHex = 0xFFAB47BC, // Lavender Purple
        secondaryColorHex = 0xFFE3F2FD, // Sky Blue
        backgroundColorHex = 0xFFEDE7F6, // Deep Lavender Space light
        bannerEmoji = "🪐",
        games = listOf(
            WorldGame(
                id = "space_quiz",
                name = "Astro Voyager Quiz",
                description = "Unlock the cosmic secrets of stars, black holes, and the Red Planet!",
                emoji = "🚀",
                gameType = "QUIZ"
            ),
            WorldGame(
                id = "space_pattern",
                name = "Cosmic Pattern Codes",
                description = "Solve alien light chains and finish cosmic shape sequences!",
                emoji = "🛸",
                gameType = "PATTERN"
            ),
            WorldGame(
                id = "space_math",
                name = "Star Fuel Math",
                description = "Pop space bubbles to fuel Cosmo's galactic hyperdrive!",
                emoji = "🌟",
                gameType = "MATH"
            )
        )
    ),
    LearningWorld(
        id = "ocean_deep",
        name = "Deep Ocean Explorer",
        description = "Dive deep down under the sea to find pair matches and pop aquatic bubbles!",
        characterName = "Bubbles the Dolphin",
        characterEmoji = "🐬",
        characterGreeting = "Squeak! I'm Bubbles. Let's dive down to the magical coral reef and learn about blue whales!",
        primaryColorHex = 0xFF1E88E5, // Marine Blue
        secondaryColorHex = 0xFFE0F7FA, // Soft Cyan
        backgroundColorHex = 0xFFE0F2F1, // Soft Marine Teal
        bannerEmoji = "🪸",
        games = listOf(
            WorldGame(
                id = "ocean_quiz",
                name = "Coral Reef Quiz",
                description = "Discover the incredible creatures of the blue ocean deep!",
                emoji = "🐙",
                gameType = "QUIZ"
            ),
            WorldGame(
                id = "ocean_memory",
                name = "Sea Critter Pairs",
                description = "Match sea turtles, glowing jellyfish, and playful seals!",
                emoji = "🐚",
                gameType = "MEMORY"
            ),
            WorldGame(
                id = "ocean_math",
                name = "Submarine Pop Math",
                description = "Pop sub-aquatic oxygen bubbles to navigate the deep marine trenches!",
                emoji = "🫧",
                gameType = "MATH"
            )
        )
    )
)

// Questions databases for offline/world games
val WORLD_QUIZ_QUESTIONS = mapOf(
    "dino_quiz" to listOf(
        WorldQuizQuestion(
            id = 1,
            question = "Which dinosaur was the 'King of the Lizards' with giant sharp teeth?",
            options = listOf("T-Rex 🦖", "Triceratops 🦕", "Stegosaurus 🛡️", "Brachiosaurus 🦕"),
            answer = "T-Rex 🦖",
            explanation = "The Tyrannosaurus Rex was one of the largest meat-eating dinosaurs with a bite force of over 12,000 pounds!"
        ),
        WorldQuizQuestion(
            id = 2,
            question = "Which dinosaur had three giant horns on its face to defend itself?",
            options = listOf("Velociraptor 🦖", "Triceratops 🦕", "Diplodocus 🦕", "Ankylosaurus 🔨"),
            answer = "Triceratops 🦕",
            explanation = "Triceratops means 'three-horned face'. They used their strong horns to protect their families from predators!"
        ),
        WorldQuizQuestion(
            id = 3,
            question = "What do paleontologists dig up to learn about dinosaurs?",
            options = listOf("Treasure Chests 🏴‍☠️", "Fossil Bones 🦴", "Plastic Toys 🧸", "Space Rocks ☄️"),
            answer = "Fossil Bones 🦴",
            explanation = "Fossils are ancient animal remains or bones that turned into stone over millions of years!"
        )
    ),
    "space_quiz" to listOf(
        WorldQuizQuestion(
            id = 1,
            question = "Which planet is known as the 'Red Planet' because of its rusty iron soil?",
            options = listOf("Mars 🔴", "Venus 🟡", "Jupiter 🟠", "Neptune 🔵"),
            answer = "Mars 🔴",
            explanation = "Mars looks red because of iron oxide (like rust) in its dust and rocky landscape!"
        ),
        WorldQuizQuestion(
            id = 2,
            question = "What is the name of our home galaxy shaped like a sparkling spiral?",
            options = listOf("Andromeda 🌌", "The Milky Way 🥛", "Black Hole 🌀", "Star Cluster ✨"),
            answer = "The Milky Way 🥛",
            explanation = "Our galaxy is called the Milky Way because it looks like a soft, milky band of starlight stretching across the night sky!"
        ),
        WorldQuizQuestion(
            id = 3,
            question = "Which is the closest star to Earth that keeps us warm and bright?",
            options = listOf("The Moon 🌙", "Polaris 🌟", "The Sun ☀️", "Sirius 💎"),
            answer = "The Sun ☀️",
            explanation = "The Sun is actually a giant star! It's a massive, glowing ball of gas at the center of our solar system!"
        )
    ),
    "ocean_quiz" to listOf(
        WorldQuizQuestion(
            id = 1,
            question = "What is the largest animal ever known to live on Earth, even bigger than the dinosaurs?",
            options = listOf("Great White Shark 🦈", "Blue Whale 🐳", "Giant Squid 🦑", "Elephant 🐘"),
            answer = "Blue Whale 🐳",
            explanation = "The Blue Whale can grow up to 100 feet long and weigh as much as 33 elephants! Its tongue alone weighs as much as an elephant!"
        ),
        WorldQuizQuestion(
            id = 2,
            question = "Which marine creature has eight wiggly arms and three hearts?",
            options = listOf("Jellyfish 🪼", "Octopus 🐙", "Seahorse 🐴", "Starfish ⭐"),
            answer = "Octopus 🐙",
            explanation = "An octopus has three hearts, blue blood, and can camouflage itself by changing color in less than a second!"
        ),
        WorldQuizQuestion(
            id = 3,
            question = "What underwater structure is built by millions of tiny animals and looks like a colorful garden?",
            options = listOf("Sand Castle 🏰", "Coral Reef 🪸", "Kelp Forest 🌿", "Sunken Ship 🚢"),
            answer = "Coral Reef 🪸",
            explanation = "Coral reefs are built by tiny polyps. They are often called the 'rainforests of the sea' because so many sea creatures live in them!"
        )
    )
)

val WORLD_PATTERN_QUESTIONS = mapOf(
    "space_pattern" to listOf(
        WorldPatternQuestion(
            sequence = listOf("🚀", "🪐", "🚀", "🪐", "?"),
            missingIndex = 4,
            options = listOf("🚀", "🪐", "👽"),
            correctAnswer = "🚀"
        ),
        WorldPatternQuestion(
            sequence = listOf("🛸", "🛸", "🌟", "🛸", "🛸", "?"),
            missingIndex = 5,
            options = listOf("🌟", "🛸", "🪐"),
            correctAnswer = "🌟"
        ),
        WorldPatternQuestion(
            sequence = listOf("🌙", "☄️", "🛰️", "🌙", "☄️", "?"),
            missingIndex = 5,
            options = listOf("🛰️", "🌙", "☄️"),
            correctAnswer = "🛰️"
        )
    )
)

val WORLD_MEMORY_EMOJIS = mapOf(
    "dino_memory" to listOf("🦖", "🦕", "🐊", "🌋", "🥚", "🦴", "🌴", "🪵"),
    "space_memory" to listOf("🚀", "🛰️", "🛸", "🪐", "🌟", "🌙", "☄️", "👽"),
    "ocean_memory" to listOf("🐬", "🐳", "🦈", "🐙", "🐚", "🦀", "🐠", "🦞")
)
