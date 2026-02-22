package com.tritech.hopon.ui.rideDiscovery.core

object MockRideRepository {
    fun getUserById(userId: String): MockUser? =
        MockData.mockUserRideRelations
            .find { it.user.id == userId }
            ?.user

    fun getUserRideRelation(userId: String): MockUserRideRelation? =
        MockData.mockUserRideRelations.find { it.user.id == userId }
}
