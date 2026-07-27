<script setup>
import { computed, onMounted, ref } from 'vue'
import FooterTabs from '../components/FooterTabs.vue'

const isLoading = ref(true)
const errorMessage = ref('')

const dashboard = ref({
  results: [],
})

const selectedStatus = ref('ALL')

async function loadResults() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await fetch('/api/dashboard', {
      credentials: 'include',
    })

    if (!response.ok) {
      errorMessage.value = 'Could not load results.'
      return
    }

    dashboard.value = await response.json()
  } catch {
    errorMessage.value = 'Network error while loading results.'
  } finally {
    isLoading.value = false
  }
}

const filteredResults = computed(() => {
  if (selectedStatus.value === 'ALL') {
    return dashboard.value.results
  }

  if (selectedStatus.value === 'ACCEPTANCE') {
    return dashboard.value.results.filter(
      item =>
        item.status === 'ACCEPTANCE' ||
        item.status === 'NOT_REJECTION'
    )
  }

  return dashboard.value.results.filter(
    item => item.status === selectedStatus.value
  )
})

function formatDate(value) {
  if (!value) {
    return 'Unknown date'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString([], {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

function statusLabel(status) {
  if (status === 'REJECTION') {
    return 'Rejection'
  }

  if (status === 'POSSIBLE_REJECTION') {
    return 'Possible Rejection'
  }

  if (
    status === 'ACCEPTANCE' ||
    status === 'NOT_REJECTION'
  ) {
    return 'Acceptance'
  }

  return status
}

function statusClass(status) {
  if (status === 'REJECTION') {
    return 'rejection'
  }

  if (status === 'POSSIBLE_REJECTION') {
    return 'possible'
  }

  return 'acceptance'
}

onMounted(loadResults)
</script>

<template>
  <main class="results-page">

    <header class="results-header">
      <div>
        <h1>All Results</h1>
        <p>
          All hiring decisions detected from your Gmail account.
        </p>
      </div>

      <select
        v-model="selectedStatus"
        class="status-filter"
      >
        <option value="ALL">
          All Results
        </option>

        <option value="REJECTION">
          Rejections
        </option>

        <option value="POSSIBLE_REJECTION">
          Possible Rejections
        </option>

        <option value="ACCEPTANCE">
          Acceptances
        </option>
      </select>
    </header>

    <div
      v-if="errorMessage"
      class="error-box"
    >
      {{ errorMessage }}
    </div>

    <div
      v-if="isLoading"
      class="loading-state"
    >
      Loading results...
    </div>

    <section
      v-else
      class="results-container"
    >

      <div class="results-count">
        {{ filteredResults.length }} results
      </div>

      <div
        v-if="!filteredResults.length"
        class="empty-state"
      >
        No results found.
      </div>

      <article
        v-for="item in filteredResults"
        :key="item.id"
        class="result-row"
      >
        <div class="result-content">

          <div class="result-heading">
            <div>
              <h2>
                {{ item.companyName || 'Unknown Company' }}
              </h2>

              <p class="subject">
                {{ item.emailSubject || 'No subject' }}
              </p>
            </div>

            <span
              class="status-pill"
              :class="statusClass(item.status)"
            >
              {{ statusLabel(item.status) }}
            </span>
          </div>

          <p class="summary">
            {{ item.summary || 'No summary available.' }}
          </p>

          <div class="result-bottom">
            <span>
              {{ item.emailSender || 'Unknown sender' }}
            </span>

            <span>
              {{ formatDate(item.receivedAt) }}
            </span>
          </div>

        </div>
      </article>

    </section>

    <FooterTabs />
  </main>
</template>

<style scoped>

.results-page {
  min-height: 100vh;

  background:
    radial-gradient(
      circle at top center,
      rgba(2, 243, 18, 0.13),
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

.results-header {
  max-width: 1000px;

  margin:
    0
    auto
    30px;

  display: flex;
  align-items: center;
  justify-content: space-between;

  gap: 20px;
}

.results-header h1 {
  margin: 0 0 7px;

  font-size: 2rem;

  color: #f5f7fb;
}

.results-header p {
  margin: 0;

  color: #8f98a7;

  font-size: 0.9rem;
}

.status-filter {
  background: #151617;

  border:
    1px
    solid
    #292c32;

  color: #e4e8ef;

  border-radius: 8px;

  padding:
    10px
    14px;
}

.results-container {
  max-width: 1000px;

  margin: 0 auto;
}

.results-count {
  color: #808997;

  font-size: 0.85rem;

  margin-bottom: 12px;
}

.result-row {
  background: #151617;

  border:
    1px
    solid
    #24272d;

  border-radius: 10px;

  padding: 20px;

  margin-bottom: 12px;

  transition:
    border-color 0.15s ease,
    transform 0.15s ease;
}

.result-row:hover {
  border-color: #3a3f48;

  transform: translateY(-1px);
}

.result-heading {
  display: flex;

  justify-content: space-between;

  align-items: flex-start;

  gap: 20px;
}

.result-heading h2 {
  margin: 0;

  font-size: 1rem;

  color: #f0f2f7;
}

.subject {
  margin: 5px 0 0;

  color: #9ba4b3;

  font-size: 0.82rem;
}

.summary {
  color: #b9c1cd;

  font-size: 0.85rem;

  line-height: 1.5;

  margin:
    16px
    0;
}

.result-bottom {
  display: flex;

  justify-content: space-between;

  gap: 15px;

  color: #777f8c;

  font-size: 0.75rem;
}

.status-pill {
  flex-shrink: 0;

  border-radius: 20px;

  padding:
    5px
    9px;

  font-size: 0.67rem;

  font-weight: 600;

  text-transform: uppercase;
}

.status-pill.rejection {
  color: #ff686f;

  background:
    rgba(
      255,
      77,
      87,
      0.1
    );

  border:
    1px
    solid
    rgba(
      255,
      77,
      87,
      0.35
    );
}

.status-pill.possible {
  color: #ffa640;

  background:
    rgba(
      255,
      159,
      47,
      0.1
    );

  border:
    1px
    solid
    rgba(
      255,
      159,
      47,
      0.35
    );
}

.status-pill.acceptance {
  color: #54e493;

  background:
    rgba(
      46,
      213,
      115,
      0.1
    );

  border:
    1px
    solid
    rgba(
      46,
      213,
      115,
      0.35
    );
}

.error-box,
.loading-state,
.empty-state {
  max-width: 1000px;

  margin:
    0
    auto
    20px;

  padding: 18px;

  background: #151617;

  border:
    1px
    solid
    #292c32;

  border-radius: 9px;

  color: #929ba9;
}

.error-box {
  color: #ff8e95;

  border-color:
    rgba(
      255,
      77,
      87,
      0.4
    );
}

.empty-state {
  text-align: center;

  padding:
    60px
    20px;
}

@media (max-width: 650px) {

  .results-page {
    padding:
      24px
      16px
      100px;
  }

  .results-header {
    flex-direction: column;

    align-items: flex-start;
  }

  .status-filter {
    width: 100%;
  }

  .result-heading {
    flex-direction: column;
  }

  .result-bottom {
    flex-direction: column;

    gap: 5px;
  }
}

</style>