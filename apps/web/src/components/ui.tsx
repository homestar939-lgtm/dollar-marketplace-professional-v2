import type { ReactNode } from 'react'

export function Card({ title, actions, children, className = '' }: { title?: string; actions?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <header className="card-head">
          {title && <h2>{title}</h2>}
          {actions && <div className="card-actions">{actions}</div>}
        </header>
      )}
      {children}
    </section>
  )
}

export function Stat({ label, value, tone = 'gold' }: { label: string; value: string; tone?: 'gold' | 'green' | 'red' }) {
  return (
    <div className={`stat stat-${tone}`}>
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{value}</strong>
    </div>
  )
}

const STATUS_TONES: Record<string, string> = {
  PENDING: 'warn', CONFIRMED: 'info', PREPARED: 'info', INVOICED: 'info', SHIPPED: 'ok', DELIVERED: 'ok',
  DRAFT: 'muted', SENT: 'warn', APPROVED: 'ok', REJECTED: 'bad', VOIDED: 'muted', CANCELLED: 'bad'
}

export function Badge({ status }: { status: string }) {
  return <span className={`badge badge-${STATUS_TONES[status] ?? 'muted'}`}>{status}</span>
}

export function Button({ children, onClick, variant = 'primary', disabled, type = 'button' }: {
  children: ReactNode
  onClick?: () => void
  variant?: 'primary' | 'ghost' | 'danger'
  disabled?: boolean
  type?: 'button' | 'submit'
}) {
  return (
    <button className={`btn btn-${variant}`} onClick={onClick} disabled={disabled} type={type}>
      {children}
    </button>
  )
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
    </label>
  )
}

export function Empty({ text }: { text: string }) {
  return <p className="empty">{text}</p>
}

export function ErrorNote({ message }: { message: string | null }) {
  if (!message) return null
  return <p className="error-note">{message}</p>
}
