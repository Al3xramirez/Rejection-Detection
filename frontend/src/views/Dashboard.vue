<script setup>
/*
  Dashboard view.
  This view represents the dashboard page of the application.
  It includes a header, some placeholder content, and the FooterTabs component.
*/

import FooterTabs from '../components/FooterTabs.vue'
</script>

<template>
	<main class="page-shell">
		<section class="page-panel">
			<h1>Dashboard</h1>
			<p>Dashboard content will go here.</p>
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
</style>