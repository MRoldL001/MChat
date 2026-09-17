package com.mroldl001.mimochat.di

import com.mroldl001.mimochat.data.api.MiMoApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // BODY 会消费完整的 SSE 响应后再交给上层，导致流式输出被缓冲。
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiServiceFactory(okHttpClient: OkHttpClient): ApiServiceFactory {
        return ApiServiceFactory(okHttpClient)
    }
}

class ApiServiceFactory(private val okHttpClient: OkHttpClient) {
    private val serviceCache = ConcurrentHashMap<String, MiMoApiService>()

    fun getService(baseUrl: String): MiMoApiService {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val retrofitBaseUrl = if (normalizedBaseUrl.endsWith("/v1")) {
            normalizedBaseUrl.removeSuffix("/v1") + "/"
        } else {
            "$normalizedBaseUrl/"
        }
        return serviceCache.getOrPut(retrofitBaseUrl) {
            val retrofit = Retrofit.Builder()
                .baseUrl(retrofitBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            retrofit.create(MiMoApiService::class.java)
        }
    }
}
