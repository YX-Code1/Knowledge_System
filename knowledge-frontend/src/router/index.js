import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/ResetPassword.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('@/views/ArticleDetail.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/article/create',
    name: 'ArticleCreate',
    component: () => import('@/views/ArticleCreate.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/article/edit/:id',
    name: 'ArticleEdit',
    component: () => import('@/views/ArticleEdit.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/category',
    name: 'CategoryManage',
    component: () => import('@/views/CategoryManage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/tag',
    name: 'TagManage',
    component: () => import('@/views/TagManage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user',
    name: 'UserManage',
    component: () => import('@/views/UserManage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'MessageCenter',
    component: () => import('@/views/MessageCenter.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/recharge',
    name: 'Recharge',
    component: () => import('@/views/Recharge.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/collections',
    name: 'Collections',
    component: () => import('@/views/Collections.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/collection/:id',
    name: 'CollectionDetail',
    component: () => import('@/views/CollectionDetail.vue'),
    meta: { requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  // sessionStorage：每个标签页独立的登录态
  const token = sessionStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.guest && token) {
    next('/')
  } else {
    next()
  }
})

export default router
