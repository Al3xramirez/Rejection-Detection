<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const email = ref('')
const password = ref('')
const rememberMe = ref(true)
const errorMessage = ref('')

function login() {
  if (!email.value || !password.value) {
    errorMessage.value = 'Please enter your email and password.'
    return
  }

  localStorage.setItem('rr_logged_in', 'true')
  localStorage.setItem('rr_user_email', email.value)

  router.push('/home')
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <div class="logo-wrapper">
        <div class="logo-icon">
          ✉
          <span>×</span>
        </div>
      </div>

      <h1>Rejection <span>Detection</span></h1>
      <p class="subtitle">Smarter insights from your job emails</p>

      <form class="login-form" @submit.prevent="login">
        <div class="field-group">
          <label>Email</label>
          <div class="input-box">
            <span>✉</span>
            <input
              v-model="email"
              type="email"
              placeholder="you@example.com"
            />
          </div>
        </div>

        <div class="field-group">
          <label>Password</label>
          <div class="input-box">
            <span class="lock-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" focusable="false">
                <path
                  d="M17 8h-1V6a4 4 0 0 0-8 0v2H7a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2zm-7-2a2 2 0 1 1 4 0v2h-4V6zm7 12H7v-8h10v8z"
                />
              </svg>
            </span>
            <input
              v-model="password"
              type="password"
              placeholder="••••••••"
            />
          </div>
        </div>

        <p v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </p>

        <div class="form-options">
          <label class="checkbox-label">
            <input v-model="rememberMe" type="checkbox" />
            <span>Remember me</span>
          </label>

          <a href="#">Forgot password?</a>
        </div>

        <button class="login-button" type="submit">
          Login
        </button>
      </form>

      <div class="divider">
        <span></span>
        <p>OR</p>
        <span></span>
      </div>

      <button class="google-button">
        <span>G</span>
        Login with Google
      </button>

      <p class="signup-text">
        Don’t have an account?
        <a href="#">Sign up</a>
      </p>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: white;
  background:
    radial-gradient(circle at left, rgba(22, 163, 74, 0.55), transparent 30%),
    radial-gradient(circle at top right, rgba(34, 197, 94, 0.25), transparent 25%),
    linear-gradient(135deg, #050807, #111514 45%, #0b0f0d);
}

.login-card {
  width: 100%;
  max-width: 430px;
  padding: 44px 32px;
  border-radius: 18px;
  background: rgba(22, 25, 24, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: 0 30px 90px rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(20px);
}

.logo-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 22px;
}

.logo-icon {
  position: relative;
  width: 74px;
  height: 58px;
  border: 4px solid #22c55e;
  border-radius: 8px;
  color: #22c55e;
  font-size: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-icon span {
  position: absolute;
  right: -13px;
  bottom: -13px;
  width: 28px;
  height: 28px;
  border: 3px solid #22c55e;
  border-radius: 50%;
  background: #111514;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

h1 {
  text-align: center;
  margin: 0;
  font-size: 31px;
  font-weight: 800;
}

h1 span {
  color: #22c55e;
}

.subtitle {
  text-align: center;
  color: #c7c7c7;
  margin: 10px 0 34px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field-group label {
  display: block;
  margin-bottom: 8px;
  color: #f1f1f1;
  font-size: 14px;
  font-weight: 700;
}

.input-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #242827;
  border: 1px solid #3d4240;
  border-radius: 6px;
  padding: 0 14px;
}

.input-box span {
  color: #a3a3a3;
}

.lock-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #8f9492;
}

.lock-icon svg {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

.input-box input {
  width: 100%;
  height: 48px;
  border: none;
  outline: none;
  background: transparent;
  color: white;
  font-size: 15px;
}

.input-box:focus-within {
  border-color: #22c55e;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
  font-size: 14px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #dedede;
}

.checkbox-label input {
  accent-color: #22c55e;
}

.form-options a,
.signup-text a {
  color: #22c55e;
  text-decoration: none;
}

.error-message {
  margin: 0;
  color: #ff8a8a;
  font-size: 14px;
}

.login-button {
  height: 50px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(135deg, #16a34a, #22c55e);
  color: white;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  margin-top: 6px;
}

.login-button:hover {
  filter: brightness(1.08);
}

.divider {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 26px 0;
}

.divider span {
  flex: 1;
  height: 1px;
  background: #3d4240;
}

.divider p {
  margin: 0;
  color: #bcbcbc;
  font-size: 13px;
}

.google-button {
  width: 100%;
  height: 48px;
  background: transparent;
  color: white;
  border: 1px solid #686d6a;
  border-radius: 0px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.google-button span {
  color: #22c55e;
  font-weight: 900;
  margin-right: 10px;
}

.google-button:hover {
  border-color: #22c55e;
}

.signup-text {
  margin: 32px 0 0;
  text-align: center;
  color: #c7c7c7;
  font-size: 14px;
}
</style>