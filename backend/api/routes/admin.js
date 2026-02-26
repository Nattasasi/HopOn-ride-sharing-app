const express = require('express');
const router = express.Router();
const { 
  getUsers, 
  banUser, 
  getPosts, 
  deletePost,
  getBookings,
  updateBookingStatus,
  getDashboard 
} = require('../controllers/adminController');

router.get('/users', getUsers);
router.patch('/users/:id/ban', banUser);
router.get('/posts', getPosts);
router.delete('/posts/:id', deletePost);
router.get('/bookings', getBookings);
router.patch('/bookings/:id/status', updateBookingStatus);
router.get('/dashboard', getDashboard);

module.exports = router;