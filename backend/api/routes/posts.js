const express = require('express');
const router = express.Router();
const { getPosts, createPost, getPost, updatePostStatus, getMyPosts } = require('../controllers/postsController');

router.get('/', getPosts);
router.get('/me', getMyPosts);
router.post('/', createPost);
router.get('/:id', getPost);
router.patch('/:id/status', updatePostStatus);

module.exports = router;