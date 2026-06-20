const WS_BASE = 'ws://localhost:8080/ws'

export interface WsMessage {
  type: string
  payload: Record<string, any>
}

export type MessageHandler = (msg: WsMessage) => void

let task: UniApp.SocketTask | null = null
let handler: MessageHandler | null = null
let socketReady = false

export function connectMatch(
  token: string,
  onMessage: MessageHandler,
  onOpen?: () => void,
): void {
  if (task) disconnectMatch()

  socketReady = false
  handler = onMessage

  task = uni.connectSocket({
    url: `${WS_BASE}?token=${token}`,
    complete: () => {},
  })

  task.onOpen(() => {
    socketReady = true
    onOpen?.()
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
    socketReady = false
  })

  task.onClose(() => {
    socketReady = false
  })
}

export function sendMatch(type: string, payload: Record<string, any> = {}): void {
  if (!task || !socketReady) return
  try {
    task.send({ data: JSON.stringify({ type, payload }) })
  } catch (e) {
    console.error('[match.ts] 发送失败', e)
  }
}

export function setMessageHandler(onMessage: MessageHandler): void {
  handler = onMessage
}

export function disconnectMatch(): void {
  if (!task) return
  socketReady = false
  const t = task
  task = null
  handler = null
  try { t.close({}) } catch (_) {}
}
