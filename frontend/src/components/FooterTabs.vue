<script setup>
/*
  FooterTabs component.
  This component renders the footer navigation tabs for the application.
  It includes links to the Dashboard, Profile, and Settings pages.
*/

</script>

<template>
  <footer class="footer-tabs">
    <router-link to="/dashboard" class="tab" active-class="active">Dashboard</router-link>
    <router-link to="/settings" class="tab" active-class="active">Settings</router-link>
  </footer>
</template>

<style scoped>
.footer-tabs {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  border-top: 1px solid #252b28;
  background: rgba(10, 13, 12, 0.94);
  backdrop-filter: blur(10px);
  z-index: 15;
}

.tab {
  text-align: center;
  text-decoration: none;
  color: #cdd4cf;
  padding: 14px 10px;
  font-weight: 700;
  border-right: 1px solid #252b28;
}

.tab:last-child {
  border-right: none;
}

.tab:hover {
  color: #22c55e;
}

.tab.active {
  background: #22c55e;
  color: #06100b;
}
</style>
