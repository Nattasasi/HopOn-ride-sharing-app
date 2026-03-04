package com.tritech.hopon.ui.rideDiscovery.core

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface EmergencyService {
    @POST("emergency")
    suspend fun createEmergency(@Body request: ApiCreateEmergencyRequest): ApiEmergencyAlert

    @GET("emergency")
    suspend fun getEmergencies(): List<ApiEmergencyAlert>
}

