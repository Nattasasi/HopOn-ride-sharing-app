const express = require('express');
const router = express.Router();
const requireRole = require('../middleware/role');
const { 
  getUsers, 
  banUser, 
  getPosts, 
  deletePost,
  getBookings,
  updateBookingStatus,
  getDashboard,
  getVerifications,
  approveVerification,
  rejectVerification
} = require('../controllers/adminController');
const { getAdminReports, updateReportStatus } = require('../controllers/reportsController');

router.use(requireRole('admin'));

router.get('/users', getUsers);
router.patch('/users/:id/ban', banUser);
router.get('/posts', getPosts);
router.delete('/posts/:id', deletePost);
router.get('/bookings', getBookings);
router.patch('/bookings/:id/status', updateBookingStatus);
router.get('/dashboard', getDashboard);
router.get('/verifications', getVerifications);
router.patch('/verifications/:id/approve', approveVerification);
router.patch('/verifications/:id/reject', rejectVerification);
router.patch('/verifications/:id/cancel', rejectVerification);
router.get('/reports', getAdminReports);
router.patch('/reports/:id/status', updateReportStatus);

module.exports = router;
