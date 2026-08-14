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
    val temperature: Double = 0.7,
    val top_p: Double = 0.9,
    val max_tokens: Int = 4000
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
        private const val BASE_URL = "http://10.0.2.2:3000/" // Android Emulator maps localhost to 10.0.2.2

        fun create(): NvidiaService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
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
