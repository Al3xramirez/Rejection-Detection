<script setup>
import { computed, onMounted, ref } from 'vue'
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

const maxCount = computed(() => {
	if (!dashboard.value.monthlyResults.length) return 1
	return Math.max(
		...dashboard.value.monthlyResults.flatMap((point) => [
			point.rejectionCount,
			point.possibleRejectionCount,
			point.acceptanceCount,
		]),
		1,
	)
})

function makeChartPoints(selector) {
	const points = dashboard.value.monthlyResults
  if (!points.length) return ''

  const width = 640
  const height = 220
  const xStep = points.length > 1 ? width / (points.length - 1) : width

  return points
    .map((point, index) => {
      const x = Math.round(index * xStep)
			const y = Math.round(height - (selector(point) / maxCount.value) * height)
      return `${x},${y}`
    })
    .join(' ')
}

const rejectionPoints = computed(() => makeChartPoints((point) => point.rejectionCount))
const possiblePoints = computed(() => makeChartPoints((point) => point.possibleRejectionCount))
const acceptancePoints = computed(() => makeChartPoints((point) => point.acceptanceCount))

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
			<h1>Dashboard</h1>

			<p v-if="isLoading" class="helper-text">Loading dashboard...</p>
			<p v-else-if="errorMessage" class="helper-text error">{{ errorMessage }}</p>

			<div v-else class="dashboard-grid">
				<article class="metric-card rejection">
					<p class="metric-label">Logged Rejections</p>
					<p class="metric-value">{{ dashboard.rejectionCount }}</p>
				</article>

				<article class="metric-card possible">
					<p class="metric-label">Possible Rejections</p>
					<p class="metric-value">{{ dashboard.possibleRejectionCount }}</p>
				</article>

				<article class="metric-card accepted">
					<p class="metric-label">Acceptances</p>
					<p class="metric-value">{{ dashboard.acceptanceCount }}</p>
				</article>

				<article class="chart-card">
					<div class="chart-header">
						<h2>Monthly Outcomes</h2>
						<p class="chart-legend">
							<span class="legend-dot rejection"></span>Rejections
							<span class="legend-dot possible"></span>Possible
							<span class="legend-dot accepted"></span>Acceptances
						</p>
					</div>

					<div v-if="dashboard.monthlyResults.length" class="chart-shell">
						<svg viewBox="0 0 640 220" preserveAspectRatio="none" class="chart-svg">
							<polyline :points="rejectionPoints" fill="none" stroke="#ef4444" stroke-width="3" />
							<polyline :points="possiblePoints" fill="none" stroke="#f59e0b" stroke-width="3" />
							<polyline :points="acceptancePoints" fill="none" stroke="#22c55e" stroke-width="3" />
						</svg>
						<div class="axis-labels">
							<span
								v-for="point in dashboard.monthlyResults"
								:key="point.monthLabel"
							>{{ point.monthLabel }}</span>
						</div>
					</div>

					<p v-else class="helper-text">No monthly data yet.</p>
				</article>

				<article class="list-card">
					<h2>Recent Results</h2>
					<ul v-if="dashboard.results.length" class="rejection-list">
						<li v-for="item in dashboard.results" :key="item.id" class="rejection-item">
							<div class="row-top">
								<p class="company">{{ item.companyName || 'Unknown Company' }}</p>
								<span class="status" :class="item.status">{{ item.status }}</span>
							</div>
							<p class="subject">{{ item.emailSubject || 'No subject available' }}</p>
							<p class="summary">{{ item.summary || 'No summary available yet.' }}</p>
						</li>
					</ul>
					<p v-else class="helper-text">No email results logged yet.</p>
				</article>
			</div>
		</section>

		<FooterTabs />
	</main>
</template>

<style scoped>
.page-shell {
	min-height: 100vh;
	padding: 48px 40px 92px;
	background:
		radial-gradient(circle at top left, rgba(34, 197, 94, 0.28), transparent 30%),
		linear-gradient(135deg, #070908, #111514 45%, #080a09);
	color: white;
}

.page-panel {
	max-width: 980px;
	margin: 0 auto;
	border: 1px solid #313834;
	background: rgba(22, 25, 24, 0.9);
	padding: 28px;
}

.page-panel h1 {
	margin: 0 0 12px;
	color: #22c55e;
}

.page-panel p {
	margin: 0;
	color: #c4cbc7;
}

.helper-text {
	color: #c4cbc7;
}

.helper-text.error {
	color: #fca5a5;
}

.dashboard-grid {
	display: grid;
	gap: 16px;
	margin-top: 12px;
	grid-template-columns: repeat(3, minmax(0, 1fr));
}

.metric-card,
.chart-card,
.list-card {
	border: 1px solid #313834;
	background: rgba(17, 20, 19, 0.95);
	padding: 18px;
}

.metric-card.rejection .metric-value {
	color: #ef4444;
}

.metric-card.possible .metric-value {
	color: #f59e0b;
}

.metric-card.accepted .metric-value {
	color: #22c55e;
}

.chart-card,
.list-card {
	grid-column: 1 / -1;
}

.metric-label {
	font-size: 0.95rem;
	color: #a9b3ad;
	margin-bottom: 6px;
}

.metric-value {
	font-size: 2rem;
	font-weight: 800;
	color: #22c55e;
}

.chart-header h2,
.list-card h2 {
	margin: 0 0 10px;
	color: #22c55e;
	font-size: 1.15rem;
}

.chart-legend {
	display: flex;
	align-items: center;
	gap: 14px;
	font-size: 0.85rem;
	color: #b6c1bb;
	margin: 0 0 8px;
}

.legend-dot {
	display: inline-block;
	width: 10px;
	height: 10px;
	margin-right: 6px;
	vertical-align: middle;
}

.legend-dot.rejection {
	background: #ef4444;
}

.legend-dot.possible {
	background: #f59e0b;
}

.legend-dot.accepted {
	background: #22c55e;
}

.chart-shell {
	display: grid;
	gap: 10px;
}

.chart-svg {
	width: 100%;
	height: 220px;
	background: rgba(7, 10, 9, 0.7);
	border: 1px solid #252b28;
}

.axis-labels {
	display: flex;
	justify-content: space-between;
	gap: 8px;
	font-size: 0.8rem;
	color: #9ca9a1;
}

.rejection-list {
	list-style: none;
	margin: 0;
	padding: 0;
	display: grid;
	gap: 10px;
}

.rejection-item {
	border: 1px solid #252b28;
	background: rgba(7, 10, 9, 0.7);
	padding: 12px;
}

.row-top {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 10px;
}

.company {
	font-weight: 700;
	color: #e6ece8;
}

.status {
	font-size: 0.75rem;
	padding: 2px 8px;
	border: 1px solid #94a3b8;
	color: #94a3b8;
}

.status.REJECTION {
	border-color: #ef4444;
	color: #ef4444;
}

.status.POSSIBLE_REJECTION {
	border-color: #f59e0b;
	color: #f59e0b;
}

.status.NOT_REJECTION {
	border-color: #22c55e;
	color: #22c55e;
}

.subject {
	margin-top: 6px;
	font-size: 0.9rem;
	color: #b6c1bb;
}

.summary {
	margin-top: 6px;
	line-height: 1.5;
	color: #d0d8d3;
}

@media (max-width: 900px) {
	.dashboard-grid {
		grid-template-columns: 1fr;
	}
}
</style>