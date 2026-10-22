import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, getMyBalance, getProfile } from '@/api'
import { useRouter } from 'vue-router'

export const useUserStore = defineStore('user', () => {
  // 登录态存 sessionStorage：每个标签页独立会话，互不干扰
  // （localStorage 全浏览器共享，第二个标签页登录会覆盖第一个的 token）
  const token = ref(sessionStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(sessionStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => ['ADMIN', 'SUPER_ADMIN'].includes(userInfo.value?.role))
  const isSuperAdmin = computed(() => userInfo.value?.role === 'SUPER_ADMIN')

  function setLoginData(data) {
    token.value = data.token
    userInfo.value = {
      userId: data.userId,
      username: data.username,
      nickname: data.nickname,
      role: data.role,
      avatar: data.avatar,
      balance: data.balance ?? 0
    }
    sessionStorage.setItem('token', data.token)
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  /** 刷新余额（购买/充值后调用），同步 ref 与 sessionStorage */
  async function refreshBalance() {
    const res = await getMyBalance()
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, balance: res.data }
      sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
  }

  /** 资料编辑后本地同步 userInfo（昵称/邮箱/头像等） */
  function updateUserInfo(data) {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...data }
      sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
  }

  /** 从服务端拉取最新资料并同步 userInfo 与 sessionStorage */
  async function refreshUserInfo() {
    const res = await getProfile()
    const d = res.data
    updateUserInfo({
      // 资料接口返回的主键字段是 id（登录接口才是 userId）
      userId: d.id,
      username: d.username,
      nickname: d.nickname,
      role: d.role,
      avatar: d.avatar,
      email: d.email,
      balance: d.balance
    })
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
  }

  async function login(form) {
    const res = await loginApi(form)
    setLoginData(res.data)
    return res
  }

  async function register(form) {
    return await registerApi(form)
  }

  return { token, userInfo, isLoggedIn, isAdmin, isSuperAdmin, login, register, logout, setLoginData, refreshBalance, refreshUserInfo, updateUserInfo }
})
