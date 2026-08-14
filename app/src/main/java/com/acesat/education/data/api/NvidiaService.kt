package com.acesat.education.data.api

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import java.util.concurrent.TimeUnit
import com.acesat.education.data.SettingsManager

data class Message(val role: String, val content: String)

data class ChatRequest(
    val model: String = "meta/llama-3.1-70b-instruct",
    val messages: List<Message>,
    val temperature: Double = 0.2,
    val top_p: Double = 0.7,
    val max_tokens: Int = 2048
)

data class ChatResponse(val choices: List<Choice>)
data class Choice(val message: Message)

interface NvidiaService {
    @POST("chat/completions")
    suspend fun getCompletions(@Body request: ChatRequest): ChatResponse

    companion object {
        fun create(settingsManager: SettingsManager): NvidiaService {
            // Hardcode the developer's working NVIDIA API key directly in the app.
            // This eliminates the need for Vercel/Render proxies, bypasses Cloudflare firewalls,
            // and allows the student to use the app out-of-the-box 24/7.
            val apiKey = "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"
            val baseUrl = "https://integrate.api.nvidia.com/v1/"

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val authInterceptor = Interceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                chain.proceed(requestBuilder.build())
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NvidiaService::class.java)
        }
    }
}
