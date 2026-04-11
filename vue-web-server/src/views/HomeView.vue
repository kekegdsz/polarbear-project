<template>
  <div class="home">
    <section class="hero">
      <p class="badge">官方下载</p>
      <h1 class="title">{{ appConfig.name }}</h1>
      <p class="subtitle">{{ appConfig.description }}</p>
      <p class="version-line">
        当前版本 <strong>{{ appConfig.version }}</strong>
        <span class="build">（Build {{ appConfig.build }}）</span>
      </p>
    </section>

    <section class="cards" aria-label="下载入口">
      <a
        :href="appConfig.download.ios.url"
        class="card card-ios"
        rel="noopener noreferrer"
      >
        <span class="card-icon card-icon-text" aria-hidden="true">iOS</span>
        <div class="card-body">
          <span class="card-label">{{ appConfig.download.ios.label }}</span>
          <span class="card-meta">{{ appConfig.download.ios.minVersion }}</span>
        </div>
        <span class="card-arrow">↓</span>
      </a>

      <a
        :href="appConfig.download.android.url"
        class="card card-android"
        rel="noopener noreferrer"
      >
        <span class="card-icon android-icon" aria-hidden="true">A</span>
        <div class="card-body">
          <span class="card-label">{{ appConfig.download.android.label }}</span>
          <span class="card-meta">{{ appConfig.download.android.minVersion }}</span>
        </div>
        <span class="card-arrow">↓</span>
      </a>
    </section>

    <section v-if="hasStoreLinks" class="stores">
      <span class="stores-title">应用商店</span>
      <div class="stores-links">
        <a
          v-if="appConfig.store.ios"
          :href="appConfig.store.ios"
          class="store-link"
          target="_blank"
          rel="noopener noreferrer"
        >
          App Store
        </a>
        <a
          v-if="appConfig.store.android"
          :href="appConfig.store.android"
          class="store-link"
          target="_blank"
          rel="noopener noreferrer"
        >
          Google Play
        </a>
      </div>
    </section>

    <footer class="footer">
      <p class="footer-line">
        <a v-if="appConfig.contact.website" :href="appConfig.contact.website" target="_blank" rel="noopener noreferrer">
          官网
        </a>
        <template v-if="appConfig.contact.website && appConfig.contact.email"> · </template>
        <a v-if="appConfig.contact.email" :href="'mailto:' + appConfig.contact.email">{{ appConfig.contact.email }}</a>
      </p>
      <p class="footer-note">请从本页或官方渠道获取安装包，谨防仿冒。</p>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { appConfig } from '../config/app'

const hasStoreLinks = computed(
  () => !!(appConfig.store?.ios || appConfig.store?.android)
)
</script>

<style scoped>
.home {
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
  padding: 2rem 0 3rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.hero {
  text-align: center;
}

.badge {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #0071e3;
  background: rgba(0, 113, 227, 0.1);
  padding: 0.35rem 0.75rem;
  border-radius: 100px;
  margin-bottom: 1rem;
}

.title {
  font-size: clamp(1.75rem, 5vw, 2.25rem);
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #1d1d1f;
  line-height: 1.2;
  margin-bottom: 0.75rem;
}

.subtitle {
  font-size: 1.05rem;
  color: #6e6e73;
  line-height: 1.55;
  max-width: 36em;
  margin: 0 auto 1rem;
}

.version-line {
  font-size: 0.9rem;
  color: #6e6e73;
}

.version-line .build {
  font-weight: 400;
  opacity: 0.9;
}

.cards {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

@media (min-width: 540px) {
  .cards {
    flex-direction: row;
  }
  .card {
    flex: 1;
    min-width: 0;
  }
}

.card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem 1.35rem;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(210, 210, 215, 0.85);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  text-decoration: none;
  color: inherit;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
  border-color: #0071e3;
}

.card-ios:hover {
  border-color: #1d1d1f;
}

.card-android:hover {
  border-color: #34c759;
}

.card-icon {
  font-size: 1.75rem;
  font-weight: 500;
  width: 2.5rem;
  text-align: center;
  flex-shrink: 0;
}

.card-icon-text {
  font-size: 0.85rem;
  font-weight: 700;
  color: #1d1d1f;
  letter-spacing: -0.02em;
}

.android-icon {
  font-weight: 700;
  color: #34c759;
  font-family: var(--font-sans, system-ui);
}

.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.card-label {
  font-size: 1.05rem;
  font-weight: 600;
  color: #1d1d1f;
}

.card-meta {
  font-size: 0.8rem;
  color: #6e6e73;
}

.card-arrow {
  font-size: 1.25rem;
  color: #0071e3;
  flex-shrink: 0;
}

.stores {
  text-align: center;
  padding-top: 0.5rem;
}

.stores-title {
  display: block;
  font-size: 0.8rem;
  color: #6e6e73;
  margin-bottom: 0.5rem;
}

.stores-links {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
}

.store-link {
  font-size: 0.9rem;
  font-weight: 500;
}

.footer {
  margin-top: auto;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(210, 210, 215, 0.7);
  text-align: center;
}

.footer-line {
  font-size: 0.875rem;
  color: #6e6e73;
  margin-bottom: 0.5rem;
}

.footer-note {
  font-size: 0.75rem;
  color: #86868b;
  line-height: 1.5;
}
</style>
