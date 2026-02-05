package com.expensetracker

import android.content.Context
import com.expensetracker.db.AppDatabase
import com.expensetracker.network.ExchangeRateApi
import com.expensetracker.repository.ExpenseRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

object ServiceLocator {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    private fun createApi(): ExchangeRateApi {
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.frankfurter.dev/v1/")
            .client(createOkHttpClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        return retrofit.create(ExchangeRateApi::class.java)
    }

    fun provideRepository(context: Context): ExpenseRepository {
        val db = AppDatabase.getInstance(context)
        return ExpenseRepository(db.expenseDao(), createApi())
    }
}
