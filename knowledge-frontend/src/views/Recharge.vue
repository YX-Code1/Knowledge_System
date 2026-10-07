<template>
  <div>
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">充值中心</h1>
        <el-button @click="$router.back()" icon="ArrowLeft">返回</el-button>
      </div>
    </div>

    <div class="container" style="padding-top: 20px;">
      <!-- 余额卡片 -->
      <div class="balance-card">
        <div class="balance-label">当前余额</div>
        <div class="balance-amount">¥{{ Number(userStore.userInfo?.balance || 0).toFixed(2) }}</div>
      </div>

      <div class="recharge-row">
        <!-- 二维码（演示用途：扫码打开本页，输入金额即模拟支付成功） -->
        <div class="qr-card">
          <h3 class="card-title">扫码充值</h3>
          <img v-if="qrUrl" :src="qrUrl" class="qr-img" alt="充值二维码" />
          <p class="qr-tip">手机扫码打开本页，登录后输入金额即可充值（演示流程，无需真实支付）</p>
          <p class="qr-tip">提示：手机需能访问本机地址（localhost 不可达，请以局域网 IP 打开本站）</p>
        </div>

        <div class="recharge-card">
          <h3 class="card-title">立即充值</h3>
          <el-input-number
            v-model="amount"
            :min="0.01"
            :max="10000"
            :precision="2"
            :step="10"
            style="width: 100%"
            placeholder="请输入充值金额"
          />
          <el-button type="primary" size="large" style="width: 100%; margin-top: 16px" :loading="recharging" @click="handleRecharge">
            立即充值
          </el-button>
        </div>
      </div>

      <!-- 记录 -->
      <div class="manage-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="充值记录" name="recharges">
            <el-table :data="rechargeRecords" v-loading="recordsLoading">
              <el-table-column label="金额" width="150">
                <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
              </el-table-column>
              <el-table-column label="时间" prop="createTime">
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default><el-tag type="success" size="small">成功</el-tag></template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrapper" v-if="rechargeTotal > 0">
              <el-pagination
                v-model:current-page="rechargePage"
                v-model:page-size="rechargeSize"
                :total="rechargeTotal"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                @current-change="loadRechargeRecords"
                @size-change="loadRechargeRecords"
              />
            </div>
          </el-tab-pane>
          <el-tab-pane label="资金明细" name="logs">
            <el-table :data="logs" v-loading="logsLoading">
              <el-table-column label="类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="logTypeTag(row.type)" size="small">{{ logTypeName(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="变动金额" width="150">
                <template #default="{ row }">
                  <span :class="row.changeAmount >= 0 ? 'amount-in' : 'amount-out'">
                    {{ row.changeAmount >= 0 ? '+' : '' }}¥{{ Number(row.changeAmount).toFixed(2) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="变动后余额" width="150">
                <template #default="{ row }">¥{{ Number(row.balanceAfter).toFixed(2) }}</template>
              </el-table-column>
              <el-table-column label="备注" prop="remark" show-overflow-tooltip />
              <el-table-column label="时间" width="160">
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrapper" v-if="logTotal > 0">
              <el-pagination
                v-model:current-page="logPage"
                v-model:page-size="logSize"
                :total="logTotal"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                @current-change="loadLogs"
                @size-change="loadLogs"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import QRCode from 'qrcode'
import { recharge as rechargeApi, getMyRecharges, getMyBalanceLogs } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const qrUrl = ref('')
const amount = ref(100)
const recharging = ref(false)

const activeTab = ref('recharges')
const rechargeRecords = ref([])
const recordsLoading = ref(false)
const rechargePage = ref(1)
const rechargeSize = ref(10)
const rechargeTotal = ref(0)
const logs = ref([])
const logsLoading = ref(false)
const logPage = ref(1)
const logSize = ref(10)
const logTotal = ref(0)

function formatDateTime(date) {
  if (!date) return ''
  return date.substring(0, 16)
}

function logTypeName(type) {
  return { RECHARGE: '充值', PURCHASE: '购买支出', INCOME: '售出收入', ADMIN: '管理员调整' }[type] || type
}

function logTypeTag(type) {
  return { RECHARGE: 'success', PURCHASE: 'danger', INCOME: 'warning', ADMIN: 'info' }[type] || 'info'
}

async function handleRecharge() {
  if (!amount.value || amount.value <= 0) {
    ElMessage.warning('请输入充值金额')
    return
  }
  recharging.value = true
  try {
    const res = await rechargeApi({ amount: amount.value })
    await userStore.refreshBalance()
    ElMessage.success(`充值成功，当前余额 ¥${Number(res.data).toFixed(2)}`)
    loadRechargeRecords()
  } finally {
    recharging.value = false
  }
}

async function loadRechargeRecords() {
  recordsLoading.value = true
  try {
    const res = await getMyRecharges({ page: rechargePage.value, size: rechargeSize.value })
    rechargeRecords.value = res.data.records
    rechargeTotal.value = res.data.total
  } finally {
    recordsLoading.value = false
  }
}

async function loadLogs() {
  logsLoading.value = true
  try {
    const res = await getMyBalanceLogs({ page: logPage.value, size: logSize.value })
    logs.value = res.data.records
    logTotal.value = res.data.total
  } finally {
    logsLoading.value = false
  }
}

onMounted(async () => {
  // 二维码内容为当前站点地址 + /recharge，手机扫码后登录即可为该账号充值
  QRCode.toDataURL(window.location.origin + '/recharge', { width: 220 })
    .then(url => { qrUrl.value = url })
    .catch(() => {})
  loadRechargeRecords()
  loadLogs()
})
</script>

<style scoped>
.balance-card {
  background: linear-gradient(135deg, #409eff, #2b7cd9);
  border-radius: 12px;
  padding: 24px 32px;
  color: #fff;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}
.balance-label {
  font-size: 14px;
  opacity: 0.9;
}
.balance-amount {
  font-size: 32px;
  font-weight: 700;
  margin-top: 4px;
}
.recharge-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}
.qr-card, .recharge-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.qr-card {
  flex: 1;
  text-align: center;
}
.recharge-card {
  flex: 1;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}
.qr-img {
  width: 220px;
  height: 220px;
}
.qr-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
.manage-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
.amount-in {
  color: #67c23a;
  font-weight: 600;
}
.amount-out {
  color: #f56c6c;
  font-weight: 600;
}
</style>
