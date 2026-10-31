import { useState } from 'react'
import { api, tokenStore, ApiError } from '../api'
import type { User } from '../types'
import { Button, Card, ErrorNote, Field } from '../components/ui'

export default function Login({ onAuthenticated }: { onAuthenticated: (user: User) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('buyer@dollar.local')
  const [password, setPassword] = useState('Buyer@12345')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      if (mode === 'login') {
        const result = await api.login(email, password)
        tokenStore.set(result.accessToken)
        onAuthenticated(result.user)
      } else {
        const result = await api.register(name, email, password)
        setNotice(
          result.verification.token
            ? `تم إنشاء الحساب. رمز التحقق (بيئة تطوير): ${result.verification.token}`
            : 'تم إنشاء الحساب. تحقق من بريدك لتفعيل الحساب.'
        )
      }
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر تنفيذ الطلب')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-wrap">
      <Card className="login-card">
        <h1>دولار — منصة التجارة B2B</h1>
        <p className="sub">سجّل الدخول للوصول إلى لوحة دورك. الحساب لا يُفعّل قبل التحقق من البريد.</p>

        <ErrorNote message={error} />
        {notice && <p className="empty">{notice}</p>}

        <form
          onSubmit={(event) => {
            event.preventDefault()
            void submit()
          }}
        >
          {mode === 'register' && (
            <Field label="الاسم">
              <input value={name} onChange={(e) => setName(e.target.value)} required minLength={2} />
            </Field>
          )}
          <Field label="البريد الإلكتروني">
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </Field>
          <Field label="كلمة المرور">
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
          </Field>
          <div className="row">
            <Button type="submit" disabled={busy}>{busy ? '…' : mode === 'login' ? 'دخول' : 'إنشاء حساب'}</Button>
            <Button variant="ghost" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(null); setNotice(null) }}>
              {mode === 'login' ? 'حساب جديد' : 'لدي حساب'}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
