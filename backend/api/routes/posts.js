const express = require('express');
const router = express.Router();
const { getPosts, createPost, getPost, updatePostStatus, startPostRide, getMyPosts } = require('../controllers/postsController');

router.get('/', getPosts);
router.get('/me', getMyPosts);
router.post('/', createPost);
router.patch('/:id/start', startPostRide);
router.get('/:id', getPost);
router.patch('/:id/status', updatePostStatus);

module.exports = router;
