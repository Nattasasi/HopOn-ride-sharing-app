/**
 * expireRides.js
 *
 * Scheduled job: auto-cancels active CarpoolPosts that were never started
 * when 1 hour has passed beyond their scheduled departure_time.
 *
 * On cancellation:
 *   - Sets status → "cancelled"
 *   - Emits real-time socket events to the post room and every participant's
 *     user room (ride_cancelled + ride_status_changed)
 *   - Sends push notifications to the driver and all active passengers
 *
 * Runs every minute via node-cron.
 * Call startExpireRidesJob(io) with the Socket.IO server instance.
 */

const cron = require('node-cron');
const CarpoolPost = require('../models/CarpoolPost');
const Booking = require('../models/Booking');
const { sendToUsers } = require('./pushNotifications');

const EXPIRE_GRACE_HOURS = 1;

const cancelStaleRides = async (io) => {
  const cutoff = new Date(Date.now() - EXPIRE_GRACE_HOURS * 60 * 60 * 1000);

  const stalePosts = await CarpoolPost.find({
    status: 'active',
    departure_time: { $lt: cutoff }
  }).lean();

  if (stalePosts.length === 0) return;

  const postIds = stalePosts.map((p) => p._id);

  await CarpoolPost.updateMany(
    { _id: { $in: postIds } },
    { $set: { status: 'cancelled' } }
  );

  console.log(`[expireRides] Auto-cancelled ${stalePosts.length} unstarted post(s):`, postIds.map(String));

  for (const post of stalePosts) {
    try {
      const activeBookings = await Booking.find({
        post_id: post.post_id,
        status: { $in: ['pending', 'accepted', 'confirmed'] }
      })
        .select('passenger_id')
        .lean();

      const participantUserIds = [
        post.driver_id?.toString(),
        ...activeBookings.map((b) => b.passenger_id?.toString())
      ].filter(Boolean);

      const postMongoId = post._id.toString();
      const payload = {
        post_id: postMongoId,
        post_uuid: post.post_id,
        status: 'cancelled',
        cancelled_by: 'system'
      };

      // Real-time: post room + each participant's user room
      if (io) {
        io.to(`post_${postMongoId}`).emit('ride_cancelled', payload);
        io.to(`post_${postMongoId}`).emit('ride_status_changed', payload);
        const uniqueIds = [...new Set(participantUserIds)];
        uniqueIds.forEach((userId) => {
          io.to(`user_${userId}`).emit('ride_cancelled', payload);
          io.to(`user_${userId}`).emit('ride_status_changed', payload);
        });
      }

      if (participantUserIds.length === 0) continue;

      sendToUsers({
        userIds: participantUserIds,
        notification: {
          title: 'Ride cancelled',
          body: `The ride from ${post.start_location_name} to ${post.end_location_name} was not started and has been automatically cancelled.`
        },
        data: { type: 'ride_cancelled', ...payload }
      }).catch((err) => console.error('[expireRides] Push notification error:', err.message));
    } catch (err) {
      console.error('[expireRides] Error processing post', post._id, err.message);
    }
  }
};

const startExpireRidesJob = (io) => {
  cron.schedule('* * * * *', async () => {
    try {
      await cancelStaleRides(io);
    } catch (err) {
      console.error('[expireRides] Job error:', err.message);
    }
  });
  console.log(`[expireRides] Auto-cancel job started (grace period: ${EXPIRE_GRACE_HOURS}h).`);
};

module.exports = { startExpireRidesJob, cancelStaleRides };
