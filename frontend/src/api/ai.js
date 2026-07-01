import request from './request'

/**
 * AI 仓库管理员 — API 封装
 */

/** 获取最新预警结果 */
export function getLatestAlertsApi(alertType) {
  return request.get('/ai/alerts/latest', { params: { alertType } })
}

/** 手动刷新预警分析 */
export function refreshAlertsApi() {
  return request.post('/ai/alerts/refresh')
}

/** AI 对话 — 发送消息给 DeepSeek */
export function chatApi(messages) {
  return request.post('/ai/chat', { messages })
}

/** 获取全量库存快照 */
export function getStocksDataApi() {
  return request.get('/ai/data/stocks')
}

/** 获取出库历史 */
export function getOutboundHistoryDataApi(materialCode) {
  return request.get('/ai/data/outbound-history', { params: { materialCode } })
}

/** 获取入库历史 */
export function getInboundHistoryDataApi(materialCode) {
  return request.get('/ai/data/inbound-history', { params: { materialCode } })
}
