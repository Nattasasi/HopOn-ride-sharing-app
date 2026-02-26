const { v4: uuidv4 } = require('uuid');
const CarpoolPost = require('../models/CarpoolPost');
const User = require('../models/User');
const Booking = require('../models/Booking');
const Feedback = require('../models/Feedback');
const { body, validationResult } = require('express-validator');

const getPosts = async (req, res) => {
  try {
    const { lat, lng, radius } = req.query;
    console.log('getPosts received query:', { lat, lng, radius }); // Log received query params

    let posts;

    const DEFAULT_RADIUS_KM = 5;
    if (lat && lng) {
      const radiusKm = radius ? parseFloat(radius) : DEFAULT_RADIUS_KM;
      console.log('Performing geo search with:', { lat: parseFloat(lat), lng: parseFloat(lng), radiusKm });
      posts = await CarpoolPost.aggregate([
        {
          $geoNear: {
            near: { type: 'Point', coordinates: [parseFloat(lng), parseFloat(lat)] },
            distanceField: 'distance',
            maxDistance: radiusKm * 1000, // km to meters
            spherical: true,
            key: 'end_location' // Search by ride destination, not pickup point
          }
        },
        { $sort: { distance: 1 } },
        {
          $lookup: {
            from: 'users',
            localField: 'driver_id',
            foreignField: '_id',
            as: 'driver'
          }
        },
        { $unwind: '$driver' },
        {
          $project: {
            _id: 1,
            post_id: 1,
            start_location_name: 1,
            start_lat: 1,
            start_lng: 1,
            end_location_name: 1,
            end_lat: 1,
            end_lng: 1,
            departure_time: 1,
            total_seats: 1,
            available_seats: 1,
            price_per_seat: 1,
            status: 1,
            vehicle_info: 1,
            contact_info: 1,
            additional_notes: 1,
            wait_time_minutes: 1,
            distance: 1,
            // Rename driver → driver_id to match Android ApiCarpoolPost shape
            driver_id: {
              _id: '$driver._id',
              first_name: '$driver.first_name',
              last_name: '$driver.last_name',
              average_rating: '$driver.average_rating'
            }
          }
        }
      ]);
      console.log('Geo search results:', posts); // Log results of geo search
    } else {
      console.log('Fetching all posts.');
      posts = await CarpoolPost.find({})
        .populate('driver_id', 'first_name last_name average_rating') // Populate all fields needed by app
        .sort({ departure_time: -1 }); // Sort by newest first
      console.log('All posts results:', posts); // Log results of all posts
    }

    res.json(posts);
  } catch (error) {
    console.error('Error in getPosts:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
};

const createPost = [
  body('start_location_name').notEmpty().withMessage('Start location name is required'),
  body('start_lat').isFloat().withMessage('Start latitude must be a number'),
  body('start_lng').isFloat().withMessage('Start longitude must be a number'),
  body('end_location_name').notEmpty().withMessage('End location name is required'),
  body('end_lat').isFloat().withMessage('End latitude must be a number'),
  body('end_lng').isFloat().withMessage('End longitude must be a number'),
  body('departure_time').isISO8601().toDate().withMessage('Departure time must be a valid date and time'),
  body('total_seats').isInt({ min: 1 }).withMessage('Total seats must be an integer greater than 0'),
  body('price_per_seat').isFloat({ min: 0 }).withMessage('Price per seat must be a number greater than or equal to 0'),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ errors: errors.array() });

    const { start_lat, start_lng, end_lat, end_lng, total_seats, ...rest } = req.body;

    const post = new CarpoolPost({
      post_id: uuidv4(),
      driver_id: req.user.id,
      ...rest,
      start_lat,
      start_lng,
      end_lat,
      end_lng,
      location: {
        type: 'Point',
        coordinates: [parseFloat(start_lng), parseFloat(start_lat)] // GeoJSON [longitude, latitude]
      },
      end_location: {
        type: 'Point',
        coordinates: [parseFloat(end_lng), parseFloat(end_lat)] // GeoJSON [longitude, latitude]
      },
      available_seats: total_seats,
      total_seats: total_seats
    });
    await post.save();
    const populatedPost = await CarpoolPost.findById(post._id)
      .populate('driver_id', 'first_name last_name average_rating');
    res.status(201).json(populatedPost);
  }
];

const getPost = async (req, res) => {
  const post = await CarpoolPost.findById(req.params.id)
    .populate('driver_id', 'first_name last_name average_rating');
  if (!post) return res.status(404).json({ message: 'Post not found' });
  res.json(post);
};

const getMyPosts = async (req, res) => {
  try {
    const posts = await CarpoolPost.find({ driver_id: req.user.id })
      .populate('driver_id', 'first_name last_name average_rating')
      .sort({ departure_time: -1 });
    res.json(posts);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const updatePostStatus = async (req, res) => {
  const { status } = req.body;
  const post = await CarpoolPost.findByIdAndUpdate(req.params.id, { status }, { new: true });
  if (status === 'completed') {
    // Create pending feedback records
    const bookings = await Booking.find({ post_id: post._id, status: 'confirmed' });
    for (const booking of bookings) {
      // Driver to passenger
      await new Feedback({
        feedback_id: uuidv4(),
        post_id: post._id,
        reviewer_id: post.driver_id,
        reviewee_id: booking.passenger_id,
        rating: 0 // Pending
      }).save();
      // Passenger to driver
      await new Feedback({
        feedback_id: uuidv4(),
        post_id: post._id,
        reviewer_id: booking.passenger_id,
        reviewee_id: post.driver_id,
        rating: 0 // Pending
      }).save();
    }
  }
  res.json(post);
};

module.exports = { getPosts, createPost, getPost, updatePostStatus, getMyPosts };