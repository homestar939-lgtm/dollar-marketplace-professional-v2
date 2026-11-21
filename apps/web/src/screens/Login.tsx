import { useState } from 'react'
import { api, tokenStore, ApiError } from '../api'
import type { User } from '../types'
import { Button, Card, ErrorNote, Field } from '../components/ui'

export default function Login({ onAuthenticated }: { onAuthenticated: (user: User) => void }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    setBusy(true)
    setError(null)
    try {
      const result = await api.login(username, password)
      tokenStore.set(result.accessToken)
      onAuthenticated(result.user)
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
        <p className="sub">سجّل الدخول باسم المستخدم وكلمة المرور للوصول إلى لوحة دورك.</p>

        <ErrorNote message={error} />

        <form
          onSubmit={(event) => {
            event.preventDefault()
            void submit()
          }}
        >
          <Field label="اسم المستخدم">
            <input autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} required />
          </Field>
          <Field label="كلمة المرور">
            <input autoComplete="current-password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </Field>
          <div className="row">
            <Button type="submit" disabled={busy}>{busy ? '…' : 'دخول'}</Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
