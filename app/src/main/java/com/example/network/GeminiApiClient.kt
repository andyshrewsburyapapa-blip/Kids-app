package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun generateEducationalGame(
        subject: String,
        theme: String,
        gradeLevel: String
    ): EducationalGame? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key is missing or placeholder. Please configure your GEMINI_API_KEY in the Secrets Panel.")
        }

        // Build the structured prompt for Kids Learning
        val prompt = """
            Create a highly engaging, delightful, and educational game for a kid in the $gradeLevel group.
            Subject: $subject
            Visual Theme: $theme
            
            The game must consist of 3 cute interactive questions.
            Each question must have exactly 4 simple choices (options), a correct answer, and an encouraging, cheerful, kid-friendly explanation with lots of emojis (e.g. "Yay! 🌟 You did it!").
            Make sure the difficulty is appropriate for $gradeLevel kids. Keep explanations simple, supportive, and clear.
            Provide a playful story introduction that ties the visual theme to the learning subject.
            Provide a unique mini-badge reward title (like 'Space Cadet' or 'Dino Tracker') and a fitting emoji.
        """.trimIndent()

        val systemInstruction = """
            You are a bubbly, encouraging, and creative early childhood education designer. 
            You design beautiful mini-games and quizzes for kids that are fun, positive, and educational.
            You must format your entire response strictly as a JSON object matching the requested schema.
        """.trimIndent()

        // Configure the Response JSON Schema manually to guide Gemini to return EXACTLY the EducationalGame format
        val schema = ResponseSchema(
            type = "OBJECT",
            required = listOf("title", "story", "rewardBadgeTitle", "rewardBadgeEmoji", "questions"),
            properties = mapOf(
                "title" to SchemaProperty(type = "STRING", description = "Cute, playful title of the educational mini-game."),
                "story" to SchemaProperty(type = "STRING", description = "A delightful intro story (2-3 sentences) setting the educational mission in the selected visual theme."),
                "rewardBadgeTitle" to SchemaProperty(type = "STRING", description = "Playful mini-badge reward name for completing this specific game."),
                "rewardBadgeEmoji" to SchemaProperty(type = "STRING", description = "Single cute emoji representing the mini-badge."),
                "questions" to SchemaProperty(
                    type = "ARRAY",
                    description = "List of 3 interactive, educational questions.",
                    items = ResponseSchema(
                        type = "OBJECT",
                        required = listOf("id", "question", "options", "answer", "explanation"),
                        properties = mapOf(
                            "id" to SchemaProperty(type = "INTEGER"),
                            "question" to SchemaProperty(type = "STRING", description = "The question text, kid-friendly and clear."),
                            "options" to SchemaProperty(
                                type = "ARRAY",
                                items = ResponseSchema(type = "STRING"),
                                description = "Exactly 4 multiple choice options."
                            ),
                            "answer" to SchemaProperty(type = "STRING", description = "The exact correct option from the options list."),
                            "explanation" to SchemaProperty(type = "STRING", description = "A bubbly, warm, supportive feedback message explaining the answer with child-friendly emojis.")
                        )
                    )
                )
            )
        )

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = schema,
                temperature = 0.7f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = systemInstruction))
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                // Parse it back to EducationalGame object using Moshi
                val adapter = moshi.adapter(EducationalGame::class.java)
                adapter.fromJson(jsonText)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
