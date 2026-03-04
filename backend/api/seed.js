/**
 * seed.js — Populates MongoDB with test users and carpool posts.
 *
 * Usage:
 *   node seed.js            — inserts seed data (skips existing emails)
 *   node seed.js --fresh    — drops carpoolposts + test users first, then inserts
 *
 * Test accounts created:
 *   driver@hopon.test  / password123
 *   rider@hopon.test   / password123
 */

require('dotenv').config();
const mongoose = require('mongoose');
const { v4: uuidv4 } = require('uuid');

const User = require('./models/User');
const CarpoolPost = require('./models/CarpoolPost');

const FRESH = process.argv.includes('--fresh');

// ─── Seed data ────────────────────────────────────────────────────────────────

const TEST_USERS = [
  {
    email: 'driver@hopon.test',
    first_name: 'Somchai',
    last_name: 'Meechai',
    dob: new Date('1990-05-15'),
    password: 'password123',
    phone_number: '0812345678',
    role: 'rider',           // no separate driver role at registration
    average_rating: 4.8,
  },
  {
    email: 'rider@hopon.test',
    first_name: 'Nattapon',
    last_name: 'Sukjai',
    dob: new Date('1998-11-20'),
    password: 'password123',
    phone_number: '0898765432',
    role: 'rider',
    average_rating: 4.5,
  },
];

// Bangkok-area seed posts — coordinates match the existing mock data
const buildPosts = (driverId) => {
  const now = new Date();
  const future = (hoursFromNow) => new Date(now.getTime() + hoursFromNow * 60 * 60 * 1000);

  const post = (
    startName, startLat, startLng,
    endName,   endLat,   endLng,
    departureTime, totalSeats, pricePerSeat,
    vehicleInfo = 'Sedan', notes = ''
  ) => ({
    post_id:            uuidv4(),
    driver_id:          driverId,
    start_location_name: startName,
    start_lat:          startLat,
    start_lng:          startLng,
    location: {
      type: 'Point',
      coordinates: [startLng, startLat],   // GeoJSON: [lng, lat]
    },
    end_location_name:  endName,
    end_lat:            endLat,
    end_lng:            endLng,
    end_location: {
      type: 'Point',
      coordinates: [endLng, endLat],       // GeoJSON: [lng, lat]
    },
    departure_time:     departureTime,
    total_seats:        totalSeats,
    available_seats:    totalSeats,
    price_per_seat:     pricePerSeat,
    status:             'active',
    vehicle_info:       vehicleInfo,
    additional_notes:   notes,
    wait_time_minutes:  5,
  });

  return [
    post('Sukhumvit Soi 11', 13.7429, 100.5559,
         'Terminal 21',       13.7373, 100.5607,
         future(2), 3, 40, 'White Honda Jazz',
         'Meet near BTS Asok exit 3'),

    post('On Nut',     13.7053, 100.5997,
         'Terminal 21', 13.7373, 100.5607,
         future(3), 2, 35, 'Silver Toyota Vios',
         'Meet at the taxi stand'),

    post('Ekkamai',   13.7197, 100.5850,
         'Terminal 21', 13.7373, 100.5607,
         future(4), 4, 30, 'Blue Honda City',
         'Running 5 minutes late is okay'),

    post('Silom Complex', 13.7296, 100.5349,
         'Siam Paragon',   13.7466, 100.5347,
         future(6), 3, 50, 'Black Toyota Camry',
         'Morning commute, quiet ride preferred'),

    post('Ari',          13.7794, 100.5381,
         'Siam Paragon',  13.7466, 100.5347,
         future(8), 2, 45, 'Grey Mazda 3',
         'Space for one small bag'),

    post('Benjasiri Park', 13.7308, 100.5680,
         'Terminal 21',     13.7373, 100.5607,
         future(1), 6, 25, 'White MPV',
         'Plate ends 19'),
  ];
};

// ─── Main ─────────────────────────────────────────────────────────────────────

async function run() {
  console.log('Connecting to MongoDB…');
  await mongoose.connect(process.env.MONGO_URI);
  console.log('Connected.');

  if (FRESH) {
    console.log('--fresh: dropping existing carpoolposts and test users…');
    await CarpoolPost.deleteMany({});
    await User.deleteMany({ email: { $in: TEST_USERS.map(u => u.email) } });
    console.log('Dropped.');
  }

  // ── Upsert test users ──────────────────────────────────────────────────────
  const userIds = {};
  for (const u of TEST_USERS) {
    let user = await User.findOne({ email: u.email });
    if (!user) {
      user = await User.create({
        user_id: uuidv4(),
        first_name: u.first_name,
        last_name:  u.last_name,
        email:      u.email,
        dob:        u.dob,
        // Let User pre-save middleware hash this exactly once.
        password_hash: u.password,
        phone_number:   u.phone_number,
        role:           u.role,
        average_rating: u.average_rating,
      });
      console.log(`Created user: ${u.email}  _id=${user._id}`);
    } else {
      console.log(`Skipped existing user: ${u.email}  _id=${user._id}`);
    }
    userIds[u.email] = user._id;
  }

  // ── Insert posts (driver = driver@hopon.test) ──────────────────────────────
  const driverId = userIds['driver@hopon.test'];
  const posts = buildPosts(driverId);
  const inserted = await CarpoolPost.insertMany(posts);
  console.log(`Inserted ${inserted.length} posts.`);

  inserted.forEach(p => {
    console.log(`  [${p._id}]  ${p.start_location_name} → ${p.end_location_name}  (${p.departure_time.toISOString()})`);
  });

  console.log('\n── Test credentials ──────────────────────────────────');
  console.log('  Host account : driver@hopon.test / password123');
  console.log('  Rider account: rider@hopon.test  / password123');
  console.log('─────────────────────────────────────────────────────\n');

  await mongoose.disconnect();
  console.log('Done.');
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
