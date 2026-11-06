import { useState } from 'react'
import type { CSSProperties, FormEvent } from 'react'
import { ArrowRight, UserPlus } from 'lucide-react'
import { ApiError, api } from '../../api'
import type { Role } from '../../types'
import { Button } from '../components/ui/Button'
import '../v3.css'

const ROLES: { value: Role; label: string }[] = [
  { value: 'ADMIN', label: 'مدير' },
  { value: 'WAREHOUSE', label: 'مسؤول مستودع' },
  { value: 'COLLECTOR', label: 'محصّل' },
  { value: 'ACCOUNTANT', label: 'محاسب' },
  { value: 'BUYER', label: 'مشتري' }
]

const fieldStyle: CSSProperties = {
  width: '100%',
  minHeight: 44,
  padding: '10px 12px',
  borderRadius: 9,
  border: '1px solid var(--border)',
  background: 'var(--black)',
  color: 'var(--white)',
  font: 'inherit'
}

export default function AddUser({ onBack, onCreated }: { onBack: () => void; onCreated?: () => void }) {
  const [name, setName] = useState('')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<Role>('BUYER')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setSuccess(false)
    if (email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setError('أدخل عنوان بريد إلكتروني صحيحاً أو اترك الحقل فارغاً.')
      return
    }

    setBusy(true)
    try {
      await api.createUser({
        name: name.trim(),
        username: username.trim(),
        ...(email.trim() ? { email: email.trim() } : {}),
        password,
        role
      })
      setSuccess(true)
      onCreated?.()
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'تعذّر إنشاء المستخدم. حاول مرة أخرى.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="v3-page">
      <header className="v3-header">
        <div>
          <h1 className="v3-title">إضافة مستخدم</h1>
          <p className="v3-subtitle">أدخل بيانات المستخدم وحدد الدور المناسب له.</p>
        </div>
        <Button variant="outline" onClick={onBack}><ArrowRight size={16} /> العودة للوحة الإدارة</Button>
      </header>

      <section className="v3-panel" style={{ maxWidth: 720, margin: '0 auto' }}>
        <div className="v3-panel-head"><UserPlus size={18} color="var(--gold)" /><h2>بيانات الحساب</h2></div>
        <form onSubmit={(event) => void submit(event)} style={{ display: 'grid', gap: 18 }}>
          <label style={{ display: 'grid', gap: 8 }}>الاسم
            <input style={fieldStyle} value={name} onChange={(event) => setName(event.target.value)} autoComplete="name" required />
          </label>
          <label style={{ display: 'grid', gap: 8 }}>اسم المستخدم
            <input style={fieldStyle} value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" required />
          </label>
          <label style={{ display: 'grid', gap: 8 }}>البريد الإلكتروني <span className="v3-muted" style={{ display: 'inline', fontSize: 12 }}>(اختياري)</span>
            <input style={fieldStyle} type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" />
          </label>
          <label style={{ display: 'grid', gap: 8 }}>كلمة المرور
            <input style={fieldStyle} type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="new-password" minLength={8} required />
          </label>
          <label style={{ display: 'grid', gap: 8 }}>الدور
            <select style={fieldStyle} value={role} onChange={(event) => setRole(event.target.value as Role)}>
              {ROLES.map((option) => <option key={option.value} value={option.value}>{option.label} ({option.value})</option>)}
            </select>
          </label>

          {error && <p role="alert" style={{ margin: 0, color: 'var(--red)' }}>{error}</p>}
          {success && <p role="status" style={{ margin: 0, color: 'var(--gold)' }}>تم إنشاء المستخدم بنجاح.</p>}
          <div style={{ display: 'flex', justifyContent: 'flex-start', gap: 10 }}>
            <Button type="submit" disabled={busy}><UserPlus size={16} />{busy ? 'جارٍ الإنشاء...' : 'إنشاء المستخدم'}</Button>
            <Button variant="ghost" onClick={onBack} disabled={busy}>إلغاء</Button>
          </div>
        </form>
      </section>
    </div>
  )
}
