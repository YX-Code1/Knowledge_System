<template>
  <div id="app">
    <router-view />
    <!-- 右下角聊天小窗：挂在 App 上，切换页面时跟随显示、聊天状态不丢失 -->
    <ChatWidget />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import ChatWidget from '@/components/ChatWidget.vue'

const userStore = useUserStore()

// 每次进入应用时拉取一次最新资料，避免 sessionStorage 里的登录快照过旧
// （例如设置昵称后刷新页面，头部昵称仍是登录时的旧值）
onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.refreshUserInfo().catch(() => {})
  }
})
</script>

<style>
#app {
  min-height: 100vh;
  background: #f5f7fa;
}
</style>
