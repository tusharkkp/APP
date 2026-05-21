package com.example.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
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
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Converts a bitmap into Base64 format.
     */
    fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Analyzes an image with the given category intent using Gemini-3.5-Flash.
     * Returns a parsed AIAnalysisResult using strict JSON structure mode.
     */
    suspend fun analyzeImage(bitmap: Bitmap, mode: String): AIAnalysisResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured!")
            return AIAnalysisResult(
                type = mode,
                title = "Configuration Needed",
                confidence = 0f,
                explanation = "Please configure your GEMINI_API_KEY inside the Secrets panel of Google AI Studio.",
                narration = "Please configure your Google AI Studio API key to enable visual artificial intelligence features."
            )
        }

        // Custom prompt directive according to user selection
        val categoryContext = when (mode) {
            "Plant" -> "This is a plant search request. Identify the specific species, scientific name, key facts, watering/sunlight care guidelines, and status."
            "Animal" -> "This is an animal search request. Identify the breed or species, scientific name, habit, diet, interesting biological fun fact, and conservation status."
            "Landmark" -> "This is a landmark search request. Identify this historical site, monument, building or geography, geological coordinates, build period, architectural style, and quick fun fact."
            "Barcode" -> "This is a barcode product details extraction. Extract text, barcodes, ingredients list, or manufacturer details if visible."
            "OCR", "Text" -> "This is an optical text extraction. Transcribe the visible text perfectly, translate if needed, and summarize what is written."
            else -> "Identify objects, products, vehicles, text, or elements in this photo. Give details, name what you see, and describe features."
        }

        val prompt = """
            You are Vision AI, an intelligent, helpful Android visual explorer.
            Analyze the provided image frame with high accuracy. 
            
            $categoryContext
            
            You must return your output strictly matching the following JSON schema:
            {
              "type": "The category analyzed (e.g., Plant, Animal, Landmark, Object, Text, Barcode)",
              "title": "A short, precise title (e.g., Species name, site name, or object label, limit 5 words)",
              "confidence": 95.0,  // Floating number estimating confidence percentage from 0 to 100
              "explanation": "Detailed detailed analysis listing key characteristics, scientific naming, safety, nutritional, or architectural info depending on the target. Break down clearly in markdown format",
              "narration": "A highly readable, friendly, 1-2 sentence pleasant voice text summarizing what you identified for real-time acoustic speaking."
            }
            
            Do not include any extra text around the JSON, markdown code wrapping (like ```json), or header formatting. Return ONLY the raw valid JSON payload.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                maxOutputTokens = 1024,
                responseMimeType = "application/json"
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                // Parse the structured JSON output utilizing our Moshi adapter
                val adapter = moshi.adapter(AIAnalysisResult::class.java)
                adapter.fromJson(jsonText) ?: throw Exception("Moshi returned null adapter parse results")
            } else {
                throw Exception("Received empty text candidate from Gemini API response.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error during call: ${e.message}", e)
            
            // Graceful fallback for offline mode or network errors
            AIAnalysisResult(
                type = mode,
                title = "Analysis Error",
                confidence = 0f,
                explanation = "An error occurred while connecting to the visual intelligence engines. Please verify your internet connection.\n\nError details: ${e.localizedMessage ?: "Unknown network failure"}",
                narration = "Sorry, I am unable to connect to the cloud right now. Please check your connectivity and try again."
            )
        }
    }
}
