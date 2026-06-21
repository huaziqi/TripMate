"use strict";
const utils_useApi = require("../utils/useApi.js");
const { get, post, del } = utils_useApi.useApi();
function fetchPosts(params) {
  return get("/api/posts", params);
}
function fetchPostDetail(id) {
  return get(`/api/posts/${id}`);
}
function createPost(data) {
  return post("/api/posts", data);
}
function toggleLike(id) {
  return post(`/api/posts/${id}/like`);
}
function toggleFavorite(id) {
  return post(`/api/posts/${id}/favorite`);
}
function fetchComments(id, params) {
  return get(`/api/posts/${id}/comments`, params);
}
function createComment(id, content, parentId) {
  return post(`/api/posts/${id}/comments`, { content, parentId });
}
function fetchMyPosts(params) {
  return get("/api/posts/my", params);
}
function fetchMyFavorites(params) {
  return get("/api/posts/my/favorites", params);
}
function searchPosts(q, page = 0, size = 10) {
  return get(`/api/posts/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`);
}
exports.createComment = createComment;
exports.createPost = createPost;
exports.fetchComments = fetchComments;
exports.fetchMyFavorites = fetchMyFavorites;
exports.fetchMyPosts = fetchMyPosts;
exports.fetchPostDetail = fetchPostDetail;
exports.fetchPosts = fetchPosts;
exports.searchPosts = searchPosts;
exports.toggleFavorite = toggleFavorite;
exports.toggleLike = toggleLike;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/post.js.map
