import { useCallback, useEffect, useState } from 'react'
import { api, tokenStore } from './api'
import type { User } from './types'
import Login from './screens/Login'
import DemoLogin from './v3/screens/DemoLogin'
import Buyer from './screens/Buyer'
import Warehouse from './screens/Warehouse'
import Collector from './screens/Collector'
import Accountant from './screens/Accountant'
import Admin from './screens/Admin'
import { Button } from './components/ui'

const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === 'true'

const ROLE_LABEL: Record<string, string> = {
  ADMIN: 'المدير', WAREHOUSE: 'المخزن', COLLECTOR: 'المحصل', ACCOUNTANT: 'المحاسب', BUYER: 'المشتري', SELLER: 'البائع', SUPPORT: 'الدعم'
}

export default function App() {
  const [user, setUser] = useState<User | null>(null)
  const [booting, setBooting] = useState(true)

  const bootstrap = useCallback(async () => {
    if (!tokenStore.get()) {
      setBooting(false)
      return
    }
    try {
      const { user } = await api.me()
      setUser(user)
    } catch {
      tokenStore.clear()
    } finally {
      setBooting(false)
    }
  }, [])

  useEffect(() => {
    void bootstrap()
  }, [bootstrap])

  const logout = async () => {
    try {
      await api.logout()
    } catch {
      /* the session may already be gone */
    }
    tokenStore.clear()
    setUser(null)
  }

  if (booting) return <div className="boot">جارٍ التحميل…</div>

  if (!user) {
    return DEMO_MODE ? <DemoLogin onAuthenticated={setUser} /> : <Login onAuthenticated={setUser} />
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">$</span>
          <div>
            <strong>دولار</strong>
            <small>Dollar Marketplace</small>
          </div>
        </div>
        <nav>
          <span className="nav-item nav-active">{ROLE_LABEL[user.role] ?? user.role}</span>
        </nav>
        <div className="sidebar-foot">
          <span className="who">{user.name}</span>
          <small>{user.email}</small>
          <Button variant="ghost" onClick={() => void logout()}>خروج</Button>
        </div>
      </aside>

      <main className="content">
        {user.role === 'BUYER' && <Buyer />}
        {user.role === 'WAREHOUSE' && <Warehouse />}
        {user.role === 'COLLECTOR' && <Collector />}
        {user.role === 'ACCOUNTANT' && <Accountant />}
        {user.role === 'ADMIN' && <Admin />}
        {['SELLER', 'SUPPORT'].includes(user.role) && <p className="empty">لا توجد لوحة مخصصة لهذا الدور بعد.</p>}
      </main>
    </div>
  )
}
