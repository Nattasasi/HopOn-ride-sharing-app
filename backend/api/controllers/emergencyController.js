const { v4: uuidv4 } = require('uuid');
const EmergencyAlert = require('../models/EmergencyAlert');

const createEmergency = async (req, res, io) => {
  const alert = new EmergencyAlert({
    alert_id: uuidv4(),
    ...req.body
  });
  await alert.save();

  // Emit to admin room
  io.to('admin').emit('emergency', alert);

  res.status(201).json(alert);
};

const getEmergencies = async (req, res) => {
  const alerts = await EmergencyAlert.find({ resolved: false });
  res.json(alerts);
};

module.exports = { createEmergency, getEmergencies };