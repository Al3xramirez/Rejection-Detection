<script setup>
import { onMounted, ref } from 'vue'
import FooterTabs from '../components/FooterTabs.vue'

const isLoading = ref(true)
const isSyncing = ref(false)
const errorMessage = ref('')
const syncMessage = ref('')
const dashboard = ref({
  rejectionCount: 0,
  possibleRejectionCount: 0,
  acceptanceCount: 0,
  monthlyResults: [],
  results: [],
})

async function loadDashboard() {
  isLoading.value = true
  errorMessage.value = ''
  syncMessage.value = ''

  try {
    const response = await fetch('/api/dashboard', { credentials: 'include' })
    if (!response.ok) {
      errorMessage.value = 'Could not load dashboard data.'
      return
    }

    dashboard.value = await response.json()
  } catch {
    errorMessage.value = 'Network error while loading dashboard.'
  } finally {
    isLoading.value = false
  }
}

async function runManualSync() {
  isSyncing.value = true
  errorMessage.value = ''
  syncMessage.value = ''

  try {
    const response = await fetch('/api/dashboard/sync', {
      method: 'POST',
      credentials: 'include',
    })

    const payload = await response.json().catch(() => ({}))

    if (!response.ok) {
      errorMessage.value = payload.error || 'Could not run manual sync.'
      return
    }

    syncMessage.value = `Sync complete. Scanned ${payload.scannedCount ?? 0} emails and stored ${payload.persistedCount ?? 0} results.`

    if (payload.dashboard) {
      dashboard.value = payload.dashboard
    } else {
      await loadDashboard()
    }
  } catch {
    errorMessage.value = 'Network error while running manual sync.'
  } finally {
    isSyncing.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <main class="page-shell">
    <section class="page-panel">
      <div class="header-row">
        <div>
          <h1>Analytics Data</h1>
          <p class="subtitle">Review rejection history and trigger Gmail sync when needed.</p>
        </div>

        <button class="sync-button" type="button" :disabled="isLoading || isSyncing" @click="runManualSync">
          {{ isSyncing ? 'Syncing...' : 'Sync Now' }}
        </button>
      </div>

      <p v-if="isLoading" class="helper-text">Loading dashboard...</p>
      <p v-if="errorMessage" class="helper-text error">{{ errorMessage }}</p>
      <p v-if="syncMessage" class="helper-text success">{{ syncMessage }}</p>

      <div v-if="!isLoading && !errorMessage" class="simple-view">
        <section class="data-block">
          <h2>Totals</h2>
          <p>rejectionCount: {{ dashboard.rejectionCount }}</p>
          <p>possibleRejectionCount: {{ dashboard.possibleRejectionCount }}</p>
          <p>acceptanceCount: {{ dashboard.acceptanceCount }}</p>
        </section>

        <section class="data-block">
          <h2>Monthly Results</h2>
          <p v-if="!dashboard.monthlyResults.length">No monthly data yet.</p>
          <div v-else class="line-list">
            <div v-for="(point, index) in dashboard.monthlyResults" :key="point.monthLabel + index" class="line-item">
              <p>monthLabel: {{ point.monthLabel }}</p>
              <p>rejectionCount: {{ point.rejectionCount }}</p>
              <p>possibleRejectionCount: {{ point.possibleRejectionCount }}</p>
              <p>acceptanceCount: {{ point.acceptanceCount }}</p>
            </div>
          </div>
        </section>

        <section class="data-block">
          <h2>Results</h2>
          <p v-if="!dashboard.results.length">No email results logged yet.</p>
          <div v-else class="line-list">
            <div v-for="item in dashboard.results" :key="item.id" class="line-item">
              <p>id: {{ item.id }}</p>
              <p>companyName: {{ item.companyName || 'Unknown Company' }}</p>
              <p>status: {{ item.status }}</p>
              <p>summary: {{ item.summary || 'No summary available' }}</p>
              <p>emailSubject: {{ item.emailSubject || 'No subject available' }}</p>
              <p>receivedAt: {{ item.receivedAt || 'Unknown' }}</p>
            </div>
          </div>
        </section>
      </div>
    </section>

    <FooterTabs />
  </main>
</template>

<style scoped>
.page-shell {
  min-height: 100vh;
  padding: 24px 20px 92px;
  background: #0f1115;
  color: #e4e8ef;
}

.page-panel {
  max-width: 860px;
  margin: 0 auto;
  border: 1px solid #2b303b;
  background: #171b23;
  padding: 16px;
}

.page-panel h1 {
  margin: 0 0 10px;
  font-size: 1.3rem;
}

.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.subtitle {
  margin: 0;
  color: #b7becb;
  font-size: 0.95rem;
}

.sync-button {
  border: 1px solid #3d6df2;
  background: linear-gradient(180deg, #4977ff 0%, #2e5cf0 100%);
  color: #fff;
  padding: 10px 14px;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  min-width: 112px;
}

.sync-button:disabled {
  opacity: 0.65;
  cursor: wait;
}

.helper-text {
  margin: 0;
  color: #b7becb;
}

.helper-text.error {
  color: #ff9797;
}

.helper-text.success {
  color: #91d9a7;
}

.simple-view {
  display: grid;
  gap: 12px;
  margin-top: 10px;
}

.data-block {
  border: 1px solid #2b303b;
  padding: 12px;
  background: #10141b;
}

.data-block h2 {
  margin: 0 0 8px;
  font-size: 1rem;
}

.line-list {
  display: grid;
  gap: 8px;
}

.line-item {
  border: 1px solid #2b303b;
  padding: 8px;
  background: #0c1016;
}

.line-item p {
  margin: 0 0 4px;
  font-size: 0.93rem;
}
</style>