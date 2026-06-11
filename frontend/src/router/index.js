import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'basic',
        meta: { title: '基础信息', roles: ['admin', 'manager'] },
        children: [
          {
            path: 'material',
            name: 'Material',
            component: () => import('@/views/basic/Material.vue'),
            meta: { title: '物料管理' }
          },
          {
            path: 'packaging',
            name: 'Packaging',
            component: () => import('@/views/basic/Packaging.vue'),
            meta: { title: '包装管理' }
          },
          {
            path: 'supplier',
            name: 'Supplier',
            component: () => import('@/views/basic/Supplier.vue'),
            meta: { title: '供应商管理' }
          }
        ]
      },
      {
        path: 'inbound',
        meta: { title: '入库管理' },
        children: [
          {
            path: 'order',
            name: 'InboundOrder',
            component: () => import('@/views/inbound/Order.vue'),
            meta: { title: '入库单管理' }
          },
          {
            path: 'manual',
            name: 'InboundManual',
            component: () => import('@/views/inbound/Manual.vue'),
            meta: { title: '手工入库' }
          },
          {
            path: 'scan',
            name: 'InboundScan',
            component: () => import('@/views/inbound/Scan.vue'),
            meta: { title: '扫码入库' }
          }
        ]
      },
      {
        path: 'outbound',
        meta: { title: '出库管理' },
        children: [
          {
            path: 'order',
            name: 'OutboundOrder',
            component: () => import('@/views/outbound/Order.vue'),
            meta: { title: '出库单管理' }
          },
          {
            path: 'manual',
            name: 'OutboundManual',
            component: () => import('@/views/outbound/Manual.vue'),
            meta: { title: '手工出库' }
          }
        ]
      },
      {
        path: 'inventory',
        meta: { title: '库存管理' },
        children: [
          {
            path: 'report',
            name: 'InventoryReport',
            component: () => import('@/views/inventory/Report.vue'),
            meta: { title: '库存报表' }
          },
          {
            path: 'import',
            name: 'InventoryImport',
            component: () => import('@/views/inventory/ImportReq.vue'),
            meta: { title: '需求导入', roles: ['admin', 'manager'] }
          }
        ]
      },
      {
        path: 'demand/list',
        name: 'DemandList',
        component: () => import('@/views/demand/List.vue'),
        meta: { title: '物料需求' }
      },
      {
        path: 'alert/threshold',
        name: 'AlertThreshold',
        component: () => import('@/views/alert/Threshold.vue'),
        meta: { title: '高低储预警' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const whiteList = ['/login']

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - WMS 仓库管理系统` : 'WMS 仓库管理系统'

  const hasToken = getToken()

  if (hasToken) {
    if (to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
