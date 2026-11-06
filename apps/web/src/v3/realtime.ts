export type RealtimeEventType = 'order.created' | 'invoice.approved' | 'receipt.confirmed'

export interface RealtimeEvent {
  type: RealtimeEventType
  data: Record<string, unknown>
}

type EventListener = (event: RealtimeEvent) => void
type StatusListener = (connected: boolean) => void

const API_BASE = (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:4000/api'
const EVENT_TYPES: RealtimeEventType[] = ['order.created', 'invoice.approved', 'receipt.confirmed']
const eventListeners = new Set<EventListener>()
const statusListeners = new Set<StatusListener>()
let source: EventSource | null = null
let connected = false

function setConnected(value: boolean) {
  if (connected === value) return
  connected = value
  statusListeners.forEach((listener) => listener(connected))
}

export function subscribeRealtime(listener: EventListener) {
  eventListeners.add(listener)
  return () => { eventListeners.delete(listener) }
}

export function subscribeRealtimeStatus(listener: StatusListener) {
  statusListeners.add(listener)
  listener(connected)
  return () => { statusListeners.delete(listener) }
}

export function connectRealtime(token: string | null) {
  source?.close()
  source = null
  setConnected(false)
  if (!token) return () => undefined

  const endpoint = `${API_BASE.replace(/\/$/, '')}/v3/events?token=${encodeURIComponent(token)}`
  const current = new EventSource(endpoint)
  source = current
  current.onopen = () => setConnected(true)
  current.onerror = () => setConnected(false)

  for (const type of EVENT_TYPES) {
    current.addEventListener(type, (rawEvent) => {
      const message = rawEvent as MessageEvent<string>
      try {
        const event: RealtimeEvent = { type, data: JSON.parse(message.data) as Record<string, unknown> }
        eventListeners.forEach((listener) => listener(event))
      } catch {
        // Ignore malformed frames; EventSource will continue with later events.
      }
    })
  }

  return () => {
    if (source !== current) return
    current.close()
    source = null
    setConnected(false)
  }
}
