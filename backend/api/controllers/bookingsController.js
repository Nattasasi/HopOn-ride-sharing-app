const { v4: uuidv4 } = require('uuid');
const Booking = require('../models/Booking');
const CarpoolPost = require('../models/CarpoolPost');
const { validationResult } = require('express-validator');

// @desc    Request to join a ride
// @route   POST /api/v1/bookings
// @access  Private
const requestBooking = async (req, res) => {
  const { post_id, seats_booked } = req.body;
  const seats = seats_booked || 1;

  try {
    // Search by the custom post_id string (UUID) instead of MongoDB _id
    const post = await CarpoolPost.findOne({ post_id: post_id });
    if (!post) return res.status(404).json({ message: 'Ride post not found' });

    if (post.driver_id.toString() === req.user.id) {
      return res.status(400).json({ message: 'You cannot book your own ride' });
    }

    if (post.available_seats < seats) {
      return res.status(400).json({ message: 'Not enough seats available' });
    }

    // Check if user already has a pending or confirmed booking for this post
    const existingBooking = await Booking.findOne({ 
      post_id, 
      passenger_id: req.user.id,
      status: { $in: ['pending', 'accepted', 'confirmed'] }
    });

    if (existingBooking) {
      return res.status(400).json({ message: 'You already have an active booking for this ride' });
    }

    const booking = new Booking({
      booking_id: uuidv4(),
      post_id,
      passenger_id: req.user.id,
      seats_booked: seats,
      status: 'pending'
    });

    await booking.save();
    
    // TODO: Notify driver via socket/push
    
    res.status(201).json(booking);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// @desc    Driver responds to a booking request
// @route   PATCH /api/v1/bookings/:id/respond
// @access  Private (Driver only)
const respondToBooking = async (req, res) => {
  const { status } = req.body;
  
  try {
    // Look up booking by MongoDB _id (what Android sends from ApiBooking.id)
    const booking = await Booking.findById(req.params.id);
    if (!booking) return res.status(404).json({ message: 'Booking not found' });

    // Find post by UUID string post_id
    const post = await CarpoolPost.findOne({ post_id: booking.post_id });
    if (!post) return res.status(404).json({ message: 'Ride post not found' });

    // 3. Security check: Is this user the driver?
    if (post.driver_id.toString() !== req.user.id) {
      return res.status(403).json({ message: 'Only the driver can respond' });
    }

    // ... (rest of your logic)
    booking.status = status === 'accepted' ? 'confirmed' : 'rejected';
    
    if (status === 'accepted') {
        if (post.available_seats < booking.seats_booked) {
            return res.status(400).json({ message: 'Not enough seats' });
        }
        post.available_seats -= booking.seats_booked;
        await post.save();
    }

    await booking.save();
    res.json(booking);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// @desc    Cancel a booking
// @route   PATCH /api/v1/bookings/:id/cancel
// @access  Private
const cancelBooking = async (req, res) => {
  try {
    // Look up booking by MongoDB _id (what Android sends from ApiBooking.id)
    const booking = await Booking.findById(req.params.id);
    
    if (!booking) {
      return res.status(404).json({ message: 'Booking not found' });
    }

    // 2. Manually find the associated post using the UUID string
    // We do this instead of .populate() to avoid the "Cast to ObjectId" error
    const post = await CarpoolPost.findOne({ post_id: booking.post_id });
    
    if (!post) {
      return res.status(404).json({ message: 'Associated ride post not found' });
    }

    // 3. Authorization check
    // Passenger can cancel their own, or Driver can cancel a passenger's booking
    const isPassenger = booking.passenger_id.toString() === req.user.id;
    const isDriver = post.driver_id.toString() === req.user.id;

    if (!isPassenger && !isDriver) {
      return res.status(403).json({ message: 'Not authorized to cancel this booking' });
    }

    // 4. Check if already cancelled
    if (booking.status === 'cancelled') {
      return res.status(400).json({ message: 'Booking is already cancelled' });
    }

    const oldStatus = booking.status;
    booking.status = 'cancelled';
    await booking.save();

    // 5. If the booking was confirmed/accepted, return the seats to the post
    if (oldStatus === 'confirmed' || oldStatus === 'accepted') {
      // Use the internal _id of the post for the update
      await CarpoolPost.findByIdAndUpdate(post._id, { 
        $inc: { available_seats: booking.seats_booked } 
      });
    }

    res.json({ 
      message: 'Booking cancelled successfully', 
      booking: {
        ...booking._doc,
        post_id: post // Return the full post object to match expected frontend format
      } 
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
}; 

// @desc    Get current user's bookings (as passenger)
// @route   GET /api/v1/bookings/me
// @access  Private
const getUserBookings = async (req, res) => {
  try {
    // 1. Get the bookings for the user
    const bookings = await Booking.find({ passenger_id: req.user.id })
      .sort({ booked_at: -1 })
      .lean(); // Use .lean() to get plain JS objects so we can modify them

    // 2. Manually "populate" the posts
    const populatedBookings = await Promise.all(
      bookings.map(async (booking) => {
        const post = await CarpoolPost.findOne({ post_id: booking.post_id })
          .populate('driver_id', 'first_name last_name average_rating');
        return { ...booking, post_id: post }; // Replace the ID string with the post object
      })
    );

    res.json(populatedBookings);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// @desc    Get bookings for a specific ride post (for driver)
// @route   GET /api/v1/bookings/posts/:id
// @access  Private (Driver only)
const getRideBookings = async (req, res) => {
  try {
   const post = await CarpoolPost.findOne({ post_id: req.params.id });
    if (!post) return res.status(404).json({ message: 'Ride post not found' });

// if (post.driver_id.toString() !== req.user.id) {
//       return res.status(403).json({ message: 'Access denied' });
//     }
   const bookings = await Booking.find({ post_id: post.post_id })
      .populate('passenger_id', 'first_name last_name email phone_number')
      .sort({ booked_at: 1 });
      
    res.json(bookings);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = { 
  requestBooking, 
  respondToBooking, 
  cancelBooking, 
  getUserBookings, 
  getRideBookings 
};