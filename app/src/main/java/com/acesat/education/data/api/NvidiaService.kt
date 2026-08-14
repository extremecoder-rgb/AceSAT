package com.acesat.education.data.api

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

data class Message(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String = "nvidia/nemotron-3-super-120b-a12b",
    val messages: List<Message>,
    val temperature: Double = 0.8,
    val top_p: Double = 0.95,
    val max_tokens: Int = 8000
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

interface NvidiaService {
    @POST("v1/chat/completions")
    suspend fun getCompletions(@Body request: ChatRequest): ChatResponse

    companion object {
        // Change this to your backend URL (local IP for phone, or deployed URL)
        private const val BASE_URL = "http://10.180.70.162:3000/"

        fun create(): NvidiaService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NvidiaService::class.java)
        }
    }
}
