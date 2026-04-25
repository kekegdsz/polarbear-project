import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/HomeView.vue'),
    meta: { title: '苍穹之下' }
  },
  {
    path: '/api-doc',
    name: 'ApiDoc',
    component: () => import('../views/ApiDocView.vue'),
    meta: { title: 'API 接入文档' }
  },
  {
    path: '/json-tool',
    name: 'JsonTool',
    component: () => import('../views/JsonToolView.vue'),
    meta: { title: 'JSON 格式化工具' }
  },
  {
    path: '/icon-maker',
    name: 'IconMaker',
    component: () => import('../views/IconMakerView.vue'),
    meta: { title: 'ICON 制作工具' }
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('../views/AboutView.vue'),
    meta: { title: '关于我们' }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/AdminLoginView.vue'),
    meta: { title: '管理后台登录' }
  },
  {
    path: '/admin',
    component: () => import('../views/AdminLayout.vue'),
    meta: { title: '管理后台', requiresAdmin: true },
    redirect: '/admin/users',
    children: [
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('../views/AdminUsersView.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'versions',
        name: 'AdminVersions',
        component: () => import('../views/AdminVersionsView.vue'),
        meta: { title: '版本管理' }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('../views/AdminOrdersView.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'groups',
        name: 'AdminGroups',
        component: () => import('../views/AdminGroupsView.vue'),
        meta: { title: '群管理' }
      },
      {
        path: 'groups/:id',
        name: 'AdminGroupDetail',
        component: () => import('../views/AdminGroupDetailView.vue'),
        meta: { title: '群详情' }
      },
      {
        path: 'orders/logs',
        name: 'AdminOrderLogs',
        component: () => import('../views/AdminLogsView.vue'),
        meta: { title: '日志管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? to.meta.title : '苍穹之下'
  const requiresAdmin = to.meta.requiresAdmin
  if (requiresAdmin) {
    const token = localStorage.getItem('admin_token')
    const role = localStorage.getItem('admin_role')
    if (!token || role !== 'admin') {
      return next({ path: '/admin/login', query: { redirect: to.fullPath } })
    }
  }
  next()
})

export default router
