package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: ResponseSchema? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    val type: String,
    val description: String? = null,
    val properties: Map<String, SchemaProperty>? = null,
    val required: List<String>? = null,
    val items: ResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class SchemaProperty(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null,
    val items: ResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- Dynamic Kids Game Format (Structured JSON returned by Gemini) ---

@JsonClass(generateAdapter = true)
data class EducationalGame(
    val title: String,
    val story: String, // Cute introductory story for kids
    val rewardBadgeTitle: String, // Mini-reward badge title
    val rewardBadgeEmoji: String, // Mini-reward badge emoji
    val questions: List<GameQuestion>
)

@JsonClass(generateAdapter = true)
data class GameQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val answer: String,
    val explanation: String // Kid-friendly supportive explanation
)
