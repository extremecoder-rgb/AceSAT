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
    val model: String = "nvidia/nemotron-3-super-120b-a12b",
    val messages: List<Message>,
    val temperature: Double = 0.8,
    val top_p: Double = 0.95,
    val max_tokens: Int = 8000
)

data class ChatResponse(val choices: List<Choice>)
data class Choice(val message: Message)

interface NvidiaService {
    @POST("chat/completions")
    suspend fun getCompletions(@Body request: ChatRequest): ChatResponse

    companion object {
        fun create(settingsManager: SettingsManager): NvidiaService {
            val apiKey = settingsManager.getApiKey()
            
            // If API key is present in settings, use NVIDIA direct URL. Otherwise, use proxy.
            val baseUrl = if (!apiKey.isNullOrBlank()) {
                "https://integrate.api.nvidia.com/v1/"
            } else {
                "http://10.180.70.162:3000/v1/"
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val authInterceptor = Interceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                if (!apiKey.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                }
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
