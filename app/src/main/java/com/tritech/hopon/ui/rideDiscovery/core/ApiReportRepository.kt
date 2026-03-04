package com.tritech.hopon.ui.rideDiscovery.core

import android.content.Context
import android.util.Log

class ApiReportRepository(private val context: Context) {
    private val service: ReportsService by lazy {
        ApiClient.create(context)
    }

    suspend fun createReport(request: ApiCreateReportRequest): Result<ApiReport> =
        runCatching {
            service.createReport(request)
        }.onFailure { Log.e(TAG, "createReport failed", it) }

    companion object {
        private const val TAG = "ApiReportRepository"
    }
}
