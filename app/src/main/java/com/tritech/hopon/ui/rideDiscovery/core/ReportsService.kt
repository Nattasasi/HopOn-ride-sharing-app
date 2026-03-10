package com.tritech.hopon.ui.rideDiscovery.core

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit service for report endpoints.
 *
 * Base path: /api/v1/reports/
 */
interface ReportsService {
    @POST("reports")
    suspend fun createReport(@Body request: ApiCreateReportRequest): ApiReport

    @GET("reports/me")
    suspend fun getMyReports(): List<ApiReport>
}
