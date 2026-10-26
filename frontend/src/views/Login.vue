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
      <p class="signin-note">
        Google sign-in is required so we can securely scan incoming emails for rejection letters.
      </p>

      <a class="google-button" href="/api/auth/google">
        <span>G</span>
        Sign in with Google
      </a>
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
  margin: 10px 0 14px;
}

.signin-note {
  margin: 0 0 24px;
  text-align: center;
  color: #aeb7b2;
  font-size: 14px;
  line-height: 1.45;
}

.google-button {
  width: 100%;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: white;
  border: 1px solid #686d6a;
  border-radius: 0px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  text-decoration: none;
}

.google-button span {
  color: #22c55e;
  font-weight: 900;
  margin-right: 10px;
}

.google-button:hover {
  border-color: #22c55e;
}
</style>