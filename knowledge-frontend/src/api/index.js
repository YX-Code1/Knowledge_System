import request from '@/utils/request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const sendCode = (data) => request.post('/auth/send-code', data)
export const sendResetCode = (data) => request.post('/auth/reset-code', data)
export const resetPassword = (data) => request.post('/auth/reset-password', data)

// 文章
export const getArticles = (params) => request.get('/articles', { params })
export const getArticle = (id) => request.get(`/articles/${id}`)
export const getMyArticles = (params) => request.get('/articles/my', { params })
export const createArticle = (data) => request.post('/articles', data)
export const updateArticle = (id, data) => request.put(`/articles/${id}`, data)
export const deleteArticle = (id) => request.delete(`/articles/${id}`)
export const likeArticle = (id) => request.post(`/articles/${id}/like`)
export const purchaseArticle = (id) => request.post(`/articles/${id}/purchase`)

// 分类
export const getCategories = () => request.get('/categories')
export const createCategory = (data) => request.post('/categories', data)
export const updateCategory = (id, data) => request.put(`/categories/${id}`, data)
export const deleteCategory = (id) => request.delete(`/categories/${id}`)

// 标签
export const getTags = () => request.get('/tags')
export const createTag = (data) => request.post('/tags', data)
export const updateTag = (id, data) => request.put(`/tags/${id}`, data)
export const deleteTag = (id) => request.delete(`/tags/${id}`)

// 评论
export const getComments = (articleId) => request.get(`/comments/article/${articleId}`)
export const createComment = (data) => request.post('/comments', data)
export const deleteComment = (id) => request.delete(`/comments/${id}`)

// 收藏
export const toggleFavorite = (articleId) => request.post(`/favorites/toggle/${articleId}`)
export const checkFavorite = (articleId) => request.get(`/favorites/check/${articleId}`)
export const getMyFavorites = () => request.get('/favorites/my')

// 用户管理（超级管理员）
export const getUsers = (params) => request.get('/users', { params })
export const updateUserRole = (id, data) => request.put(`/users/${id}/role`, data)
export const updateUserStatus = (id, data) => request.put(`/users/${id}/status`, data)

// 个人资料
export const getProfile = () => request.get('/profile')
export const updateProfile = (data) => request.put('/profile', data)

// 通知
export const getNotifications = (params) => request.get('/notifications', { params })
export const getNotificationUnreadCount = () => request.get('/notifications/unread-count')
export const markNotificationRead = (id) => request.put(`/notifications/${id}/read`)
export const markAllNotificationsRead = () => request.put('/notifications/read-all')

// 私信
export const getConversations = () => request.get('/messages/conversations')
export const getConversationMessages = (peerId, params) => request.get(`/messages/conversation/${peerId}`, { params })
export const sendMessage = (data) => request.post('/messages', data)
export const getMessageUnreadCount = () => request.get('/messages/unread-count')
export const blockUser = (id) => request.post(`/messages/block/${id}`)
export const unblockUser = (id) => request.delete(`/messages/block/${id}`)

// 余额 / 充值
export const getMyBalance = () => request.get('/recharges/balance')
export const getMyBalanceLogs = (params) => request.get('/recharges/logs', { params })
export const recharge = (data) => request.post('/recharges', data)
export const getMyRecharges = (params) => request.get('/recharges', { params })
export const adjustUserBalance = (id, data) => request.put(`/users/${id}/balance`, data)

// 合集
export const getCollections = (params) => request.get('/collections', { params })
export const getCollection = (id) => request.get(`/collections/${id}`)
export const getMyCollections = () => request.get('/collections/my')
export const createCollection = (data) => request.post('/collections', data)
export const updateCollection = (id, data) => request.put(`/collections/${id}`, data)
export const deleteCollection = (id) => request.delete(`/collections/${id}`)
export const addCollectionArticle = (id, data) => request.post(`/collections/${id}/articles`, data)
export const removeCollectionArticle = (id, articleId) => request.delete(`/collections/${id}/articles/${articleId}`)
export const purchaseCollection = (id) => request.post(`/collections/${id}/purchase`)

// AI 能力（模型调用耗时较长，覆盖 request.js 的默认 10s 超时）
export const generateSummary = (data) => request.post('/ai/summary', data, { timeout: 60000 })
export const recommendTags = (data) => request.post('/ai/tags', data, { timeout: 60000 })
