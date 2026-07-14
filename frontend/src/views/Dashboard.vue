<script setup>
import { onMounted, ref } from 'vue'
import FooterTabs from '../components/FooterTabs.vue'

const isLoading = ref(true)
const errorMessage = ref('')
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

onMounted(loadDashboard)
</script>

<template>
  <main class="page-shell">
    <section class="page-panel">
      <h1>Analytics Data</h1>

      <p v-if="isLoading" class="helper-text">Loading dashboard...</p>
      <p v-else-if="errorMessage" class="helper-text error">{{ errorMessage }}</p>

      <div v-else class="simple-view">
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

.helper-text {
  margin: 0;
  color: #b7becb;
}

.helper-text.error {
  color: #ff9797;
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