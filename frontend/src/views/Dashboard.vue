<script setup>
import { computed, onMounted, ref } from 'vue'
import FooterTabs from '../components/FooterTabs.vue'

import {
  Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler,
} from 'chart.js'

import { Line } from 'vue-chartjs'

ChartJS.register( CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler)

const isLoading = ref(true)
const isSyncing = ref(false)

const errorMessage = ref('')
const syncMessage = ref('')

const lastScannedCount = ref(0)

const dashboard = ref({rejectionCount: 0, possibleRejectionCount: 0, acceptanceCount: 0, monthlyResults: [], results: [],})

/* -----------------------------
   Load Dashboard
------------------------------ */

async function loadDashboard() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await fetch('/api/dashboard', {
      credentials: 'include',
    })

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

/* -----------------------------
   Gmail Sync
------------------------------ */

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
      errorMessage.value =
        payload.error || 'Could not run manual sync.'

      return
    }

    lastScannedCount.value = payload.scannedCount ?? 0

    syncMessage.value =
      `Sync complete. Scanned ${payload.scannedCount ?? 0} emails ` +
      `and stored ${payload.persistedCount ?? 0} results.`

    if (payload.dashboard) {
      dashboard.value = payload.dashboard
    } else {
      await loadDashboard()
    }
  } catch {
    errorMessage.value =
      'Network error while running manual sync.'
  } finally {
    isSyncing.value = false
  }
}

/* -----------------------------
   Recent Result Categories
------------------------------ */

const recentRejections = computed(() => {
  return dashboard.value.results
    .filter(item => item.status === 'REJECTION')
    .slice(0, 3)
})

const recentPossibleRejections = computed(() => {
  return dashboard.value.results
    .filter(item => item.status === 'POSSIBLE_REJECTION')
    .slice(0, 3)
})

const recentAcceptances = computed(() => {
  return dashboard.value.results
    .filter(item =>
      item.status === 'NOT_REJECTION' ||
      item.status === 'ACCEPTANCE'
    )
    .slice(0, 3)
})

/* -----------------------------
   Chart
------------------------------ */

const chartData = computed(() => ({
  labels: dashboard.value.monthlyResults.map(point => point.monthLabel),

  datasets: [
    {
      label: 'Rejections',
      data: dashboard.value.monthlyResults.map(point => point.rejectionCount),
      borderColor: '#ff4d57',
      backgroundColor: 'rgba(255, 77, 87, 0.08)',
      pointBackgroundColor: '#ff4d57',
      pointBorderColor: '#ff4d57',
      tension: 0.35,
      borderWidth: 2,
      pointRadius: 4,
      pointHoverRadius: 6,
      fill: false,
    },
    {
      label: 'Possible Rejections',
      data: dashboard.value.monthlyResults.map(point => point.possibleRejectionCount),
      borderColor: '#ff9f2f',
      backgroundColor: 'rgba(255, 159, 47, 0.08)',
      pointBackgroundColor: '#ff9f2f',
      pointBorderColor: '#ff9f2f',
      tension: 0.35,
      borderWidth: 2,
      pointRadius: 4,
      pointHoverRadius: 6,
      fill: false,
    },
    {
      label: 'Acceptances',
      data: dashboard.value.monthlyResults.map(point => point.acceptanceCount),
      borderColor: '#2ed573',
      backgroundColor: 'rgba(46, 213, 115, 0.08)',
      pointBackgroundColor: '#2ed573',
      pointBorderColor: '#2ed573',
      tension: 0.35,
      borderWidth: 2,
      pointRadius: 4,
      pointHoverRadius: 6,
      fill: false,
    },
  ],
}))

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,

  interaction: {mode: 'index', intersect: false},
  plugins: {
    legend: {
      position: 'top',
      align: 'start',

      labels: {
        color: '#b7becb',
        boxWidth: 10,
        boxHeight: 10,
        usePointStyle: true,
        pointStyle: 'circle',
        padding: 22,
      },
    },

    tooltip: {
      backgroundColor: '#171b23',
      borderColor: '#2b303b',
      borderWidth: 1,
      titleColor: '#ffffff',
      bodyColor: '#d6dbe5',
      padding: 12,
    },
  },

  scales: {
    x: {
      border: {
        display: false,
      },

      grid: {
        display: false,
      },

      ticks: {
        color: '#8f98a7',
      },
    },

    y: {
      beginAtZero: true,

      border: {
        display: false,
      },

      grid: {
        color: 'rgba(255, 255, 255, 0.06)',
      },

      ticks: {
        color: '#8f98a7',
        precision: 0,
      },
    },
  },
}

/* -----------------------------
   Helpers
------------------------------ */

function formatDate(value) {
  if (!value) {
    return 'Unknown date'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString([], {month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit',})
}

function truncateSummary(summary) {
  if (!summary) {
    return 'No summary available.'
  }

  const maxLength = 125

  if (summary.length <= maxLength) {
    return summary
  }

  return summary.substring(0, maxLength) + '...'
}

onMounted(loadDashboard)
</script>

<template>
  <main class="dashboard-page">
    <!-- HEADER -->
    <header class="dashboard-header">
      <div>
        <h1>Analytics Dashboard</h1>
        <p
          v-if="syncMessage"
          class="sync-success"
        >
          {{ syncMessage }}
        </p>
      </div>

      <div class="header-actions">
        <span
          v-if="lastScannedCount"
          class="last-sync"
          >
          Last scan: {{ lastScannedCount }} emails
        </span>

        <button
          class="sync-button"
          type="button"
          :disabled="isLoading || isSyncing"
          @click="runManualSync"
        >
          <span class="sync-icon">
            ↻
          </span>
          {{ isSyncing ? 'Syncing...' : 'Sync Now' }}
        </button>
      </div>
    </header>
    <!-- ERROR -->
    <div
      v-if="errorMessage"
      class="error-box"
    >
      {{ errorMessage }}
    </div>
    <!-- LOADING -->
    <div
      v-if="isLoading"
      class="loading-state"
    >
      Loading dashboard...
    </div>

    <template v-else>
      <!-- STAT CARDS -->
      <section class="stats-grid">

        <!-- REJECTIONS -->
        <article class="stat-card rejection-card">
          <div>
            <p class="stat-title">
              Rejections
            </p>

            <p class="stat-number rejection-number">
              {{ dashboard.rejectionCount }}
            </p>

            <p class="stat-description">
              Detected rejection emails
            </p>
          </div>
          <div class="stat-icon rejection-icon">
            ✕
          </div>
        </article>

        <!-- POSSIBLE -->
        <article class="stat-card possible-card">
          <div>
            <p class="stat-title">
              Possible Rejections
            </p>
            <p class="stat-number possible-number">
              {{ dashboard.possibleRejectionCount }}
            </p>
            <p class="stat-description">
              Emails requiring review
            </p>
          </div>
          <div class="stat-icon possible-icon">
            !
          </div>
        </article>

        <!-- ACCEPTANCES -->
        <article class="stat-card acceptance-card">
          <div>
            <p class="stat-title">
              Acceptances
            </p>
            <p class="stat-number acceptance-number">
              {{ dashboard.acceptanceCount }}
            </p>
            <p class="stat-description">
              Positive hiring results
            </p>
          </div>
          <div class="stat-icon acceptance-icon">
            ✓
          </div>
        </article>

        <!-- SCANNED -->
        <article class="stat-card scanned-card">
          <div>
            <p class="stat-title">
              Emails Scanned
            </p>
            <p class="stat-number scanned-number">
              {{ lastScannedCount || '—' }}
            </p>
            <p class="stat-description">
              Most recent Gmail sync
            </p>
          </div>
          <div class="stat-icon scanned-icon">
            ✉
          </div>
        </article>
      </section>

      <!-- CHART -->
      <section class="chart-card">
        <div class="section-header">
          <div>
            <h2>Monthly Trends</h2>
            <p>
              Hiring email results over time
            </p>
          </div>
          <select class="chart-filter">
            <option>Last 6 Months</option>
          </select>
        </div>
        <div
          v-if="dashboard.monthlyResults.length"
          class="chart-container"
        >
          <Line
            :data="chartData"
            :options="chartOptions"
          />
        </div>
        <div
          v-else
          class="empty-chart"
        >
          No monthly data available yet.
        </div>
      </section>

      <!-- RECENT RESULTS -->
      <section class="recent-section">
        <div class="section-header">
          <div>
            <h2>Recent Results</h2>
            <p>
              Latest hiring decisions detected from Gmail
            </p>
          </div>
          <button class="view-all-button">
            View all results →
          </button>
        </div>

        <div class="recent-grid">

          <!-- RECENT REJECTIONS -->
          <section class="result-column">
            <div class="result-column-heading">
              <h3 class="red-heading">
                Recent Rejections
              </h3>
              <span class="result-count red-count">
                {{ recentRejections.length }}
              </span>
            </div>
            <div
              v-if="!recentRejections.length"
              class="empty-result"
            >
              <div class="empty-icon red-empty">
                ✕
              </div>
              <strong>No recent rejections</strong>
              <span>
                You're all caught up.
              </span>
            </div>

            <article
              v-for="item in recentRejections"
              :key="item.id"
              class="result-card rejection-result"
            >
              <div class="result-top">
                <div>
                  <h4>
                    {{ item.companyName || 'Unknown Company' }}
                  </h4>
                  <p>
                    {{ item.emailSubject || 'No subject' }}
                  </p>
                </div>
                <span class="status-pill rejection-pill">
                  Rejection
                </span>
              </div>
              <p class="result-summary">
                {{ truncateSummary(item.summary) }}
              </p>
              <div class="result-footer">
                <span>
                  {{ formatDate(item.receivedAt) }}
                </span>
                <button>
                  View Details
                </button>
              </div>
            </article>
          </section>

          <!-- POSSIBLE -->
          <section class="result-column">
            <div class="result-column-heading">
              <h3 class="orange-heading">
                Possible Rejections
              </h3>
              <span class="result-count orange-count">
                {{ recentPossibleRejections.length }}
              </span>
            </div>
            <div
              v-if="!recentPossibleRejections.length"
              class="empty-result"
            >
              <div class="empty-icon orange-empty">
                !
              </div>
              <strong>
                No possible rejections
              </strong>
              <span>
                Nothing currently needs review.
              </span>
            </div>
            <article
              v-for="item in recentPossibleRejections"
              :key="item.id"
              class="result-card possible-result"
            >
              <div class="result-top">
                <div>
                  <h4>
                    {{ item.companyName || 'Unknown Company' }}
                  </h4>
                  <p>
                    {{ item.emailSubject || 'No subject' }}
                  </p>
                </div>
                <span class="status-pill possible-pill">
                  Possible
                </span>
              </div>
              <p class="result-summary">
                {{ truncateSummary(item.summary) }}
              </p>
              <div class="result-footer">
                <span>
                  {{ formatDate(item.receivedAt) }}
                </span>
                <button>
                  View Details
                </button>
              </div>
            </article>
          </section>

          <!-- ACCEPTANCES -->
          <section class="result-column">
            <div class="result-column-heading">
              <h3 class="green-heading">
                Acceptances
              </h3>
              <span class="result-count green-count">
                {{ recentAcceptances.length }}
              </span>
            </div>
            <div
              v-if="!recentAcceptances.length"
              class="empty-result"
            >
              <div class="empty-icon green-empty">
                ✓
              </div>
              <strong>
                No recent acceptances
              </strong>
              <span>
                New results will appear here.
              </span>
            </div>
            <article
              v-for="item in recentAcceptances"
              :key="item.id"
              class="result-card acceptance-result"
            >
              <div class="result-top">
                <div>
                  <h4>
                    {{ item.companyName || 'Unknown Company' }}
                  </h4>
                  <p>
                    {{ item.emailSubject || 'No subject' }}
                  </p>
                </div>
                <span class="status-pill acceptance-pill">
                  Acceptance
                </span>
              </div>
              <p class="result-summary">
                {{ truncateSummary(item.summary) }}
              </p>
              <div class="result-footer">
                <span>
                  {{ formatDate(item.receivedAt) }}
                </span>
                <button>
                  View Details
                </button>
              </div>
            </article>
          </section>
        </div>
      </section>
    </template>
    <FooterTabs />

  </main>
</template>
<style scoped>

/* =================================
   PAGE
================================= */

.dashboard-page {
  min-height: 100vh;
  background:
    radial-gradient(
      circle at top center,
      rgba(2, 243, 18, 0.164),
      transparent 35%
    ),
    #0f1115;
  color: #e4e8ef;
  padding:
    42px
    56px
    110px;
  box-sizing: border-box;
}

/* =================================
   HEADER
================================= */

.dashboard-header {
  max-width: 1250px;
  margin: 0 auto 28px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}


.dashboard-header h1 {
  margin: 0 0 8px;
  font-size: 2rem;
  font-weight: 700;
  color: #f5f7fb;
}


.subtitle {
  margin: 0;
  color: #9ca5b4;
}

.sync-success {
  margin: 7px 0 0;
  color: #4be38b;
  font-size: 0.92rem;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.last-sync {
  color: #909aaa;
  font-size: 0.85rem;
}

.sync-button {
  min-width: 130px;
  border: 1px solid #4fff5b;
  background:
    linear-gradient(
      180deg,
      #4fff5b,
      #28e858
    );
  color: rgb(0, 0, 0);
  border-radius: 9px;
  padding: 11px 18px;
  font-size: 0.94rem;
  font-weight: 600;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.sync-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow:
    0 8px 28px
    rgba(79, 255, 79, 0.28);
}

.sync-button:disabled {
  opacity: 0.6;
  cursor: wait;
}

.sync-icon {
  margin-right: 6px;
}

/* =================================
   ERROR / LOADING
================================= */

.error-box,
.loading-state {
  max-width: 1250px;
  margin: 0 auto 20px;
  padding: 14px 16px;
  border-radius: 8px;
  background: #171b23;
  border: 1px solid #2b303b;
}


.error-box {
  border-color: rgba(255, 77, 87, 0.4);
  color: #ff8e95;
}

/* =================================
   STAT CARDS
================================= */

.stats-grid {
  max-width: 1250px;
  margin: 0 auto 22px;
  display: grid;
  grid-template-columns:
    repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  min-height: 135px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px;
  border-radius: 10px;
  background:
    linear-gradient(
      145deg,
      #171d27,
      #111720
    );

  border: 1px solid #2a3240;
  box-shadow:
    0 8px 26px
    rgba(0, 0, 0, 0.12);
}

.stat-title {
  margin: 0 0 6px;
  color: #c9d0dc;
  font-size: 0.9rem;
}

.stat-number {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
}

.stat-description {
  margin: 8px 0 0;
  color: #8893a3;
  font-size: 0.78rem;
}

.rejection-number {
  color: #ff565f;
}

.possible-number {
  color: #ff9c32;
}

.acceptance-number {
  color: #3bd782;
}

.scanned-number {
  color: #4f83ff;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 1.4rem;
  font-weight: 600;
}

.rejection-icon {
  color: #ff565f;
  border: 1px solid #ff565f;
  background: rgba(255, 86, 95, 0.08);
}

.possible-icon {
  color: #ff9c32;
  border: 1px solid #ff9c32;
  background: rgba(255, 156, 50, 0.08);
}

.acceptance-icon {
  color: #3bd782;
  border: 1px solid #3bd782;
  background: rgba(59, 215, 130, 0.08);
}

.scanned-icon {
  color: #4f83ff;
  border: 1px solid #4f83ff;
  background: rgba(79, 131, 255, 0.08);
}

/* =================================
   CHART
================================= */

.chart-card {
  max-width: 1250px;
  margin: 0 auto 24px;
  padding: 22px;
  border-radius: 10px;
  background:
    linear-gradient(
      145deg,
      #151b25,
      #101720
    );
  border: 1px solid #2a3240;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.section-header h2 {
  margin: 0 0 4px;
  font-size: 1.05rem;
}

.section-header p {
  margin: 0;
  color: #7f8998;
  font-size: 0.82rem;
}

.chart-filter {
  padding: 8px 12px;
  background: #111720;
  color: #cbd2dc;
  border: 1px solid #303846;
  border-radius: 7px;
}

.chart-container {
  height: 330px;
}

.empty-chart {
  height: 250px;
  display: grid;
  place-items: center;
  color: #7f8998;
}

/* =================================
   RECENT
================================= */

.recent-section {
  max-width: 1250px;
  margin: 0 auto;
  padding: 22px;
  border-radius: 10px;
  background:
    linear-gradient(
      145deg,
      #151b25,
      #101720
    );
  border: 1px solid #2a3240;
}

.view-all-button {
  border: none;
  background: transparent;
  color: #4f83ff;
  cursor: pointer;
  font-size: 0.85rem;
}

.recent-grid {
  display: grid;
  grid-template-columns:
    repeat(3, minmax(0, 1fr));
  gap: 17px;
}

.result-column {
  min-width: 0;
}

.result-column-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 11px;
}

.result-column-heading h3 {
  margin: 0;
  font-size: 0.95rem;
}

.red-heading {
  color: #ff565f;
}

.orange-heading {
  color: #ff9c32;
}

.green-heading {
  color: #3bd782;
}

.result-count {
  min-width: 24px;
  text-align: center;
  padding: 3px 7px;
  border-radius: 6px;
  font-size: 0.75rem;
}

.red-count {
  background: rgba(255, 86, 95, 0.12);
  color: #ff747c;
}

.orange-count {
  background: rgba(255, 156, 50, 0.12);
  color: #ffad58;
}

.green-count {
  background: rgba(59, 215, 130, 0.12);
  color: #54e493;
}

/* =================================
   RESULT CARDS
================================= */

.result-card,
.empty-result {
  padding: 16px;
  margin-bottom: 11px;
  border-radius: 9px;
  background:
    linear-gradient(
      145deg,
      #171d27,
      #121821
    );
}

.rejection-result {
  border: 1px solid
    rgba(255, 77, 87, 0.42);
}

.possible-result {
  border: 1px solid
    rgba(255, 159, 47, 0.42);
}

.acceptance-result {
  border: 1px solid
    rgba(46, 213, 115, 0.42);
}

.result-top {
  display: flex;

  align-items: flex-start;
  justify-content: space-between;

  gap: 10px;
}

.result-top h4 {
  margin: 0;
  color: #f0f2f7;
  font-size: 1rem;
}

.result-top p {
  margin: 4px 0 0;
  color: #9ba4b3;
  font-size: 0.8rem;
}

.status-pill {
  flex-shrink: 0;
  padding: 5px 8px;
  border-radius: 20px;
  font-size: 0.67rem;
  font-weight: 600;
  text-transform: uppercase;
}

.rejection-pill {
  color: #ff686f;
  background: rgba(255, 77, 87, 0.1);
  border: 1px solid rgba(255, 77, 87, 0.35);
}

.possible-pill {
  color: #ffa640;
  background: rgba(255, 159, 47, 0.1);
  border: 1px solid rgba(255, 159, 47, 0.35);
}

.acceptance-pill {
  color: #44db86;
  background: rgba(46, 213, 115, 0.1);
  border: 1px solid rgba(46, 213, 115, 0.35);
}

.result-summary {
  margin: 16px 0;
  min-height: 58px;
  color: #b9c1cd;
  font-size: 0.82rem;
  line-height: 1.5;
}

.result-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #818b9a;
  font-size: 0.72rem;
}

.result-footer button {
  padding: 7px 10px;
  border-radius: 6px;
  border: 1px solid #323b49;
  background: #171e28;
  color: #cbd2dc;
  cursor: pointer;
}

.empty-result {
  min-height: 140px;
  border: 1px solid #29313d;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #8c96a5;
}

.empty-result strong {
  color: #bfc6d1;
  margin: 8px 0 4px;
}

.empty-result span {
  font-size: 0.78rem;
}

.empty-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
}

.red-empty {
  color: #ff565f;
  background: rgba(255, 86, 95, 0.08);
}

.orange-empty {
  color: #ff9c32;
  background: rgba(255, 156, 50, 0.08);
}

.green-empty {
  color: #3bd782;
  background: rgba(59, 215, 130, 0.08);
}

/* =================================
   RESPONSIVE
================================= */

@media (max-width: 1000px) {
  .stats-grid {
    grid-template-columns:
      repeat(2, 1fr);
  }
  .recent-grid {
    grid-template-columns: 1fr;
  }

}

@media (max-width: 650px) {
  .dashboard-page {
    padding:
      24px
      16px
      100px;
  }
  .dashboard-header {
    flex-direction: column;
  }
  .header-actions {
    width: 100%;
  }
  .sync-button {
    margin-left: auto;
  }
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .chart-container {
    height: 260px;
  }
}

</style>