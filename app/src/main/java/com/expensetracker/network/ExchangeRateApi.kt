package com.expensetracker.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class ExchangeRateResponse(
    val amount: Double? = null,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

interface ExchangeRateApi {
    @GET("{date}")
    suspend fun getUsdCopRate(
        @Path("date") date: String,
        @Query("base") base: String = "USD",
        @Query("symbols") symbols: String = "COP"
    ): ExchangeRateResponse
}
