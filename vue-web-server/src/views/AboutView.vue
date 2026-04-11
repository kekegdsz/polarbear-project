<template>
  <div class="about-page">
    <div class="about-card">
      <h1 class="title">关于我们</h1>
      <p class="lead">{{ appConfig.name }}</p>
      <p class="desc">{{ appConfig.description }}</p>
      <dl class="meta">
        <div v-if="appConfig.contact?.email" class="meta-row">
          <dt>联系邮箱</dt>
          <dd>
            <a :href="`mailto:${appConfig.contact.email}`">{{ appConfig.contact.email }}</a>
          </dd>
        </div>
        <div v-if="appConfig.contact?.website" class="meta-row">
          <dt>官网</dt>
          <dd>
            <a :href="appConfig.contact.website" rel="noopener noreferrer" target="_blank">{{
              appConfig.contact.website
            }}</a>
          </dd>
        </div>
      </dl>
    </div>
  </div>
</template>

<script setup>
import { appConfig } from '../config/app'
</script>

<style scoped>
.about-page {
  width: 100%;
  margin: 0;
  padding: 1.5rem 1rem;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 0;
}

.about-card {
  width: 100%;
  max-width: 36rem;
  background: #fff;
  border-radius: 16px;
  border: 1px solid rgba(210, 210, 215, 0.85);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  padding: 2rem 1.75rem;
}

.title {
  margin: 0 0 0.5rem;
  font-size: 1.5rem;
  font-weight: 700;
  color: #1d1d1f;
  letter-spacing: -0.02em;
}

.lead {
  margin: 0 0 1rem;
  font-size: 1.05rem;
  font-weight: 600;
  color: #0071e3;
}

.desc {
  margin: 0 0 1.5rem;
  font-size: 0.95rem;
  line-height: 1.6;
  color: #424245;
}

.meta {
  margin: 0;
}

.meta-row {
  display: grid;
  grid-template-columns: 5rem 1fr;
  gap: 0.75rem 1rem;
  padding: 0.65rem 0;
  border-top: 1px solid rgba(210, 210, 215, 0.6);
}

.meta-row:first-of-type {
  border-top: none;
  padding-top: 0;
}

.meta dt {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: #6e6e73;
}

.meta dd {
  margin: 0;
  font-size: 0.9rem;
  word-break: break-all;
}

.meta a {
  color: #0071e3;
  text-decoration: none;
}

.meta a:hover {
  text-decoration: underline;
}
</style>
