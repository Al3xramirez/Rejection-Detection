<script setup>
/**
 * Home.vue
 * This is the main landing page for the application
 */
import { useRouter } from 'vue-router'

const router = useRouter()

</script>

<template>
  <main class="home-page">
    <nav class="navbar">
      <div class="brand">
        <div class="brand-icon">✉</div>
        <h2>
          Rejection
          <span>Detection</span>
        </h2>
      </div>

      <div class="nav-links">
        <a href="#how-it-works">How It Works</a>
        <a href="#features">Features</a>
        <a href="#purpose">Purpose</a>
        <button @click="router.push('/login')">Log In</button>
      </div>
    </nav>

    <section class="hero">
      <div class="hero-text">
        
        <h1>
          Turn job rejection emails into
          <span>trackable insights.</span>
        </h1>

        <p class="description">
          Rejection Detection is designed to scan job-related
          emails, identify possible rejection messages, and organize the results in a clean
          dashboard. Simple, sweet, and effective.
        </p>

        <div class="hero-buttons">
          <a href="#how-it-works" class="primary-button">Learn How It Works</a>
          <a href="#features" class="secondary-button">View Features</a>
        </div>
      </div>

      <div class="hero-card">
        <div class="email-preview">
          <div class="email-top">
            <div class="circle">G</div>
            <div>
              <h3>GreenTech Solutions</h3>
              <p>Application Update</p>
            </div>
          </div>

          <p class="email-body">
            You were rejected from this application. LMAO Try again.
          </p>

          <div class="result-box">
            
          </div>
        </div>
      </div>
    </section>

    <section id="how-it-works" class="section">
      <div class="section-heading">
        <p>Process</p>
        <h2>How the website works</h2>
      </div>

      <div class="steps-grid">
        <article class="step-card">
          <div class="step-number">1</div>
          <h3>Email Input</h3>
          <p>
            A user can log in through Email, allowing the application to access their inbox and scan for job-related emails regarding rejections.
          </p>
        </article>

        <article class="step-card">
          <div class="step-number">2</div>
          <h3>Rejection Detection</h3>
          <p>
            Checks the email for keywords, phrases, and patterns that indicate a possible job rejection message.
          </p>
        </article>

        <article class="step-card">
          <div class="step-number">3</div>
          <h3>Dashboard Results</h3>
          <p>
            Results are stored and organized on a dashboard, showing your sick rejection stats.
          </p>
        </article>
      </div>
    </section>

    <section id="purpose" class="section purpose-section">
      <div class="purpose-card">
        <p class="section-label">Project Purpose</p>
        <h2>Why this project exists</h2>
        <p>
          Rejection Detection was made for the purpose of knowing rejection letters are soon to be coming my way,
          so why not make a funny system that alerts you when a rejection email is detected, adding a humorous twist to the often disheartening process.
        </p>
      </div>

      <div class="tech-card">
        <h3>Tech Stack</h3>

        <div class="tech-tags">
          <span>Java</span>
          <span>Spring Boot</span>
          <span>Vue</span>
          <span>MySQL</span>
          <span>REST API</span>
          <span>Vite Proxy</span>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(34, 197, 94, 0.28), transparent 30%),
    linear-gradient(135deg, #070908, #111514 45%, #080a09);
  color: white;
}

.navbar {
  height: 82px;
  padding: 0 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #252b28;
  background: rgba(10, 13, 12, 0.85);
  backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-icon {
  width: 46px;
  height: 38px;
  border: 3px solid #22c55e;
  border-radius: 0px;
  color: #22c55e;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand h2 {
  margin: 0;
  font-size: 19px;
  line-height: 1.1;
}

.brand h2 span {
  color: #22c55e;
  display: block;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav-links a {
  color: #d2d8d4;
  text-decoration: none;
  font-size: 14px;
}

.nav-links a:hover {
  color: #22c55e;
}

.nav-links button {
  background: transparent;
  color: white;
  border: 1px solid #3c4540;
  border-radius: 0px;
  padding: 10px 14px;
  cursor: pointer;
}

.nav-links button:hover {
  border-color: #22c55e;
  color: #22c55e;
}

.hero {
  min-height: calc(100vh - 82px);
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 48px;
  align-items: center;
  padding: 72px 72px;
}

.hero h1 {
  font-size: clamp(42px, 6vw, 78px);
  line-height: 0.95;
  margin: 22px 0;
  max-width: 850px;
}

.hero h1 span {
  color: #22c55e;
  display: block;
}

.description {
  max-width: 720px;
  color: #c8cfca;
  font-size: 18px;
  line-height: 1.7;
}

.hero-buttons {
  display: flex;
  gap: 16px;
  margin-top: 32px;
}

.primary-button,
.secondary-button {
  padding: 14px 20px;
  border-radius: 0px;
  font-weight: 800;
  text-decoration: none;
}

.primary-button {
  background: #22c55e;
  color: #06100b;
}

.secondary-button {
  border: 1px solid #3c4540;
  color: white;
}

.secondary-button:hover {
  border-color: #22c55e;
  color: #22c55e;
}

.hero-card {
  background: rgba(24, 28, 26, 0.9);
  border: 1px solid #333b36;
  border-radius: 0px;
  padding: 28px;
  box-shadow: 0 30px 90px rgba(0, 0, 0, 0.45);
}

.email-preview {
  background: #111514;
  border: 1px solid #303833;
  border-radius: 0px;
  padding: 24px;
}

.email-top {
  display: flex;
  align-items: center;
  gap: 16px;
}

.circle {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #d1d5db;
  color: #111827;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}

.email-top h3 {
  margin: 0;
}

.email-top p,
.email-body {
  color: #bfc7c2;
}

.email-body {
  line-height: 1.6;
  margin: 24px 0;
}

.section {
  padding: 80px 72px;
  border-top: 1px solid #252b28;
}

.section-heading {
  text-align: center;
  margin-bottom: 38px;
}

.section-heading p,
.section-label {
  color: #22c55e;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 13px;
}

.section h2 {
  font-size: 40px;
  margin: 10px 0 0;
}

.steps-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  max-width: 1100px;
  margin: 0 auto;
}

.step-card {
  background: #171b19;
  border: 1px solid #303833;
  border-radius: 0px;
  padding: 24px;
}

.step-number {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #22c55e;
  color: #06100b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  margin-bottom: 18px;
}

.step-card h3 {
  margin: 0 0 10px;
}

.step-card p,
.section-description,
.purpose-card p {
  color: #c6cec9;
  line-height: 1.7;
}

.two-column {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 48px;
  align-items: start;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  background: #171b19;
  border: 1px solid #303833;
  border-radius: 0px;
  padding: 18px;
  display: flex;
  gap: 14px;
  align-items: center;
}

.feature-item span {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #22c55e;
  color: #06100b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}

.feature-item p {
  margin: 0;
  color: #e5ebe7;
}

.purpose-section {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 28px;
}

.purpose-card,
.tech-card {
  background: #171b19;
  border: 1px solid #303833;
  border-radius: 0px;
  padding: 30px;
}

.tech-card h3 {
  font-size: 26px;
  margin: 0 0 22px;
}

.tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tech-tags span {
  background: rgba(34, 197, 94, 0.12);
  border: 1px solid rgba(34, 197, 94, 0.35);
  color: #8df5b2;
  padding: 10px 14px;
  border-radius: 0px;
  font-weight: 700;
}

@media (max-width: 950px) {
  .navbar {
    padding: 0 24px;
  }

  .nav-links a {
    display: none;
  }

  .hero,
  .two-column,
  .purpose-section {
    grid-template-columns: 1fr;
  }

  .hero,
  .section {
    padding: 56px 24px;
  }

  .steps-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .steps-grid {
    grid-template-columns: 1fr;
  }

  .hero-buttons {
    flex-direction: column;
  }
}
</style>