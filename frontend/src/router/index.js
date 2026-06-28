import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'

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
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人信息' }
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
          },
          {
            path: 'warehouse-area',
            name: 'WarehouseArea',
            component: () => import('@/views/basic/WarehouseArea.vue'),
            meta: { title: '库区管理' }
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
          },
          {
            path: 'history',
            name: 'InboundHistory',
            component: () => import('@/views/inbound/History.vue'),
            meta: { title: '入库历史' }
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
          },
          {
            path: 'scan',
            name: 'OutboundScan',
            component: () => import('@/views/outbound/Scan.vue'),
            meta: { title: '扫码出库' }
          },
          {
            path: 'history',
            name: 'OutboundHistory',
            component: () => import('@/views/outbound/History.vue'),
            meta: { title: '出库历史' }
          },
          {
            path: 'return-scan',
            name: 'OutboundReturnScan',
            component: () => import('@/views/outbound/ReturnScan.vue'),
            meta: { title: '扫码退库' }
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
        path: 'check',
        name: 'CheckTask',
        component: () => import('@/views/check/TaskList.vue'),
        meta: { title: '盘点任务' }
      },
      {
        path: 'alert/threshold',
        name: 'AlertThreshold',
        component: () => import('@/views/alert/Threshold.vue'),
        meta: { title: '高低储预警' }
      },
      {
        path: 'seal',
        name: 'SealManagement',
        component: () => import('@/views/seal/SealManagement.vue'),
        meta: { title: '封存管理' }
      },
      // 操作日志
      {
        path: 'audit',
        name: 'AuditLog',
        component: () => import('@/views/audit/AuditLog.vue'),
        meta: { title: '操作日志', roles: ['admin', 'manager'] }
      },
      // AI 助手
      {
        path: 'ai/chat',
        name: 'AiChat',
        component: () => import('@/views/ai/Chat.vue'),
        meta: { title: 'AI 助手' }
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
      // 角色权限检查
      const userInfo = JSON.parse(localStorage.getItem('wms_user_info') || '{}')
      const userRole = userInfo.role
      const requiredRoles = to.meta.roles
      if (requiredRoles && !requiredRoles.includes(userRole)) {
        ElMessage.warning('无权限访问该页面')
        next('/dashboard')
      } else {
        next()
      }
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