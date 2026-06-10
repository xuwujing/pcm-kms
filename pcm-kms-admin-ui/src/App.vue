<template>
  <main class="app-shell">
    <section class="panel">
      <p class="eyebrow">PCM-KMS</p>
      <h1>Phase 1 Bootstrap Ready</h1>
      <p class="summary">
        Backend and frontend skeletons are in place. The next step is implementing
        key management, client onboarding, and local SQLite flow.
      </p>
    </section>
  </main>
</template>

<style>
:root {
  color: #122117;
  background: linear-gradient(135deg, #f7f2e8 0%, #d9e7d0 100%);
  font-family: "Segoe UI", sans-serif;
}

body {
  margin: 0;
}

.app-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.panel {
  max-width: 720px;
  padding: 40px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 60px rgba(27, 57, 39, 0.12);
}

.eyebrow {
  margin: 0 0 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  font-size: 12px;
}

h1 {
  margin: 0 0 12px;
  font-size: 42px;
}

.summary {
  margin: 0;
  line-height: 1.6;
}
</style>
