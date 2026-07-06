<script setup>
import { onMounted, ref } from 'vue'
import FooterTabs from '../components/FooterTabs.vue'

const user = ref(null)
const isLoading = ref(true)
const errorMessage = ref('')

async function loadProfile() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await fetch('/api/auth/me', { credentials: 'include' })
    if (!response.ok) {
      errorMessage.value = 'Unable to load your profile right now.'
      return
    }

    user.value = await response.json()
  } catch {
    errorMessage.value = 'Network error while loading profile.'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <main class="page-shell">
    <section class="page-panel">
      <h1>Profile</h1>

      <p v-if="isLoading" class="helper-text">Loading profile...</p>
      <p v-else-if="errorMessage" class="helper-text error">{{ errorMessage }}</p>

      <div v-else-if="user" class="profile-card">
        <img
          v-if="user.picture"
          :src="user.picture"
          alt="Google profile picture"
          class="avatar"
        />
        <div class="profile-meta">
          <h2>{{ user.name || 'Google User' }}</h2>
          <p>{{ user.email || 'No email provided' }}</p>
        </div>
      </div>

      <p v-else class="helper-text">No profile data found.</p>
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

.profile-card {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #313834;
  background: rgba(17, 20, 19, 0.95);
  padding: 16px;
}

.avatar {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 50%;
  border: 2px solid #22c55e;
}

.profile-meta h2 {
  margin: 0 0 4px;
  color: #22c55e;
  font-size: 1.2rem;
}

.profile-meta p {
  margin: 0;
  color: #c4cbc7;
}
</style>
