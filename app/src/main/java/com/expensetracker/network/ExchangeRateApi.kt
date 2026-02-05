package com.expensetracker.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class ExchangeRateResponse(
    val result: String,
    @SerialName("error-type") val errorType: String? = null,
    @SerialName("time_last_update_unix") val timeLastUpdateUnix: Long,
    @SerialName("time_next_update_unix") val timeNextUpdateUnix: Long,
    @SerialName("base_code") val baseCode: String,
    @SerialName("conversion_rates") val conversionRates: Map<String, Double> = emptyMap(),
    @SerialName("rates") val rates: Map<String, Double> = emptyMap()
)

interface ExchangeRateApi {
    @GET("latest/{base}")
    suspend fun latestRates(
        @Path("base") base: String = "USD"
    ): ExchangeRateResponse
}
