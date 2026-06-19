const WS_BASE = 'ws://localhost:8080/ws'

export interface WsMessage {
  type: string
  payload: Record<string, any>
}

export type MessageHandler = (msg: WsMessage) => void

let task: UniApp.SocketTask | null = null
let handler: MessageHandler | null = null

export function connectMatch(token: string, onMessage: MessageHandler): void {
  if (task) disconnectMatch()

  handler = onMessage
  task = uni.connectSocket({
    url: `${WS_BASE}?token=${token}`,
    complete: () => {}
  })

  task.onMessage((res) => {
    try {
      const msg: WsMessage = JSON.parse(res.data as string)
      handler?.(msg)
    } catch (e) {
      console.error('[match.ts] 解析消息失败', e)
    }
  })

  task.onError((err) => {
    console.error('[match.ts] WebSocket 错误', err)
  })
}

export function sendMatch(type: string, payload: Record<string, any> = {}): void {
  if (!task) return
  task.send({ data: JSON.stringify({ type, payload }) })
}

export function setMessageHandler(onMessage: MessageHandler): void {
  handler = onMessage
}

export function disconnectMatch(): void {
  if (!task) return
  try { task.close({}) } catch (_) {}
  task = null
  handler = null
}
