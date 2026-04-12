<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-header">Admin</div>
      <nav class="menu">
        <router-link to="/admin/users" class="menu-item" active-class="active">用户</router-link>
        <router-link to="/admin/groups" class="menu-item" active-class="active">群管理</router-link>
        <router-link to="/admin/versions" class="menu-item" active-class="active">版本管理</router-link>
        <router-link to="/admin/orders" class="menu-item" active-class="active">订单管理</router-link>
      </nav>
    </aside>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
</script>

<style scoped>
.admin-layout {
  display: grid;
  grid-template-columns: 1fr 15fr;
  min-height: calc(100vh - 56px);
}

.sidebar {
  display: flex;
  flex-direction: column;
  padding: 1.5rem 1.25rem;
  border-right: 1px solid var(--color-border);
  background: #faf4f4;
}

.sidebar-header {
  font-weight: 600;
  font-size: 0.9rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #1d1d1f;
  margin-bottom: 1.5rem;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
}

.menu-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 0.6rem 0.85rem;
  border-radius: 10px;
  font-size: 0.9rem;
  color: var(--color-text-muted);
  border: none;
  background: transparent;
  text-decoration: none;
  white-space: nowrap;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.5);
  color: #1d1d1f;
}

.menu-item.active {
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  color: #1d1d1f;
  border-left: 3px solid #ff9f43;
}

.content {
  padding: 1.75rem 2rem 2.5rem;
  background: #f5f5f7;
}
</style>
