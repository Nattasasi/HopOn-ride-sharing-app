package com.tritech.hopon.ui.rideDiscovery.core

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.tritech.hopon.utils.SessionManager

private const val TAG = "ApiRideRepository"

/**
 * Live implementation of [RideRepository] that calls the car_pool REST API via
 * [PostsService].  Falls back to an empty list / null on any network error so
 * calling code can fall back to mock data without crashing.
 *
 * @param context  Application context used to resolve [ApiClient] and [SessionManager].
 * @param currentUserLatLng  Supplier for the caller's current GPS position; used to
 *                           calculate [RideListItem.pickupDistanceMeters] client-side.
 */
class ApiRideRepository(
    private val context: Context,
    private val currentUserLatLng: () -> LatLng?
) : RideRepository {

    private val postsService: PostsService by lazy {
        ApiClient.create(context)
    }

    private val currentUserId: String?
        get() = SessionManager.getCurrentUserId(context)

    // ─── RideRepository impl ─────────────────────────────────────────────────

    override suspend fun getNearbyRides(
        lat: Double,
        lng: Double,
        radiusKm: Double?
    ): Result<List<RideListItem>> = runCatching {
        val posts = postsService.getNearbyPosts(lat, lng, radiusKm)
            .filter { it.status.equals("active", ignoreCase = true) }
            .filter { (isoToEpochMillis(it.departure_time) ?: Long.MIN_VALUE) > System.currentTimeMillis() }
            .filter { it.available_seats > 0 }
        val userLatLng = currentUserLatLng()
        posts.toRideListItems(userLatLng, currentUserId)
            .sortedBy { it.pickupDistanceMeters }
    }.onFailure { e ->
        Log.w(TAG, "getNearbyRides failed: ${e.message}")
    }

    override suspend fun getMyRides(): Result<List<RideListItem>> = runCatching {
        val posts = postsService.getMyPosts()
        val userLatLng = currentUserLatLng()
        posts.toRideListItems(userLatLng, currentUserId)
    }.onFailure { e ->
        Log.w(TAG, "getMyRides failed: ${e.message}")
    }

    override suspend fun createRide(submission: CreateRideSubmission): Result<RideListItem> =
        runCatching {
            val isoTime = meetupSubmissionToIso(submission.meetupDate, submission.meetupTime)
            val request = submission.toApiCreatePostRequest(isoTime)
            val post = postsService.createPost(request)
            val userLatLng = currentUserLatLng()
            post.toRideListItem(userLatLng, currentUserId)
        }.onFailure { e ->
            Log.w(TAG, "createRide failed: ${e.message}")
        }

    override suspend fun getRideDetail(postId: String): RideListItem? = runCatching {
        val post = postsService.getPost(postId)
        val userLatLng = currentUserLatLng()
        post.toRideListItem(userLatLng, currentUserId)
    }.getOrElse { e ->
        Log.w(TAG, "getRideDetail($postId) failed: ${e.message}")
        null
    }
}
