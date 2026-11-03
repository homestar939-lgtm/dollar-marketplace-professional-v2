// apps/web/src/v3/screens/DemoLogin.tsx
// بوابة الدخول التجريبي V3 — خمسة أدوار، بلا كلمة مرور.
// ملف مستقل تماماً ولا يمسّ `src/screens/Login.tsx` (المسار الأصلي للبريد/كلمة المرور).
// يعتمد على عميل الـ API الموجود (`src/api.ts`) و`/api/v3/demo/login` (المسار نسبي لأن الأساس في api.ts يحتوي /api).
import { useState } from 'react'
import type { LucideIcon } from 'lucide-react'
import { Calculator, Package, ShieldCheck, Store, Wallet } from 'lucide-react'
import { ApiError, request, tokenStore } from '../../api'
import type { Role, User } from '../../types'
import { ErrorNote } from '../../components/ui'
import { Button } from '../components/ui/Button'
import '../v3.css'

type DemoRole = {
  role: Role
  label: string
  description: string
  icon: LucideIcon
}

const DEMO_ROLES: DemoRole[] = [
  { role: 'ADMIN', label: 'مدير المنصة', description: 'إدارة المستخدمين والوكالات والإعدادات وسجل الإجراءات.', icon: ShieldCheck },
  { role: 'WAREHOUSE', label: 'مسؤول المخزن', description: 'تجهيز الطلبات وشحنها وتتبّع المخزون.', icon: Package },
  { role: 'COLLECTOR', label: 'محصل ميداني', description: 'تسجيل التحصيلات ومتابعة العمولات.', icon: Wallet },
  { role: 'ACCOUNTANT', label: 'المحاسب', description: 'الفواتير والمطابقة والتقارير المالية.', icon: Calculator },
  { role: 'BUYER', label: 'مؤسسة تجارية', description: 'تصفّح الكتالوج وإنشاء الطلبات ومتابعة الكشف.', icon: Store }
]

export default function DemoLogin({ onAuthenticated }: { onAuthenticated: (user: User) => void }) {
  const [busy, setBusy] = useState<Role | null>(null)
  const [error, setError] = useState<string | null>(null)

  const loginAs = async (role: Role) => {
    setBusy(role)
    setError(null)
    try {
      const result = await request<{ user: User; accessToken: string }>('/v3/demo/login', { method: 'POST', body: { role } })
      tokenStore.set(result.accessToken)
      onAuthenticated(result.user)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر تسجيل الدخول التجريبي')
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="v3-page">
      <header className="v3-header">
        <div>
          <h1 className="v3-title">الدخول التجريبي</h1>
          <p className="v3-subtitle">اختر دوراً للدخول مباشرة بلا كلمة مرور — نسخة تجريبية.</p>
        </div>
      </header>

      <ErrorNote message={error} />

      <section className="v3-panel">
        <div className="v3-panel-head">
          <Store size={18} color="var(--gold)" />
          <h2>الأدوار المتاحة</h2>
        </div>

        <div className="v3-grid">
          {DEMO_ROLES.map(({ role, label, description, icon: Icon }) => {
            const loading = busy === role
            return (
              <Button
                key={role}
                variant="outline"
                size="lg"
                onClick={() => void loginAs(role)}
                disabled={busy !== null}
                aria-label={label}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  gap: '8px',
                  width: '100%',
                  height: 'auto',
                  padding: '18px',
                  textAlign: 'right'
                }}
              >
                <span className="v3-icon-box">
                  <Icon size={22} />
                </span>
                <span style={{ fontSize: '16px', fontWeight: 700 }}>{label}</span>
                <span style={{ fontSize: '13px', fontWeight: 400, color: 'var(--muted)', lineHeight: 1.6 }}>{description}</span>
                <span className="v3-muted" style={{ fontSize: '12px' }}>
                  {loading ? 'جارٍ الدخول...' : busy !== null ? '...' : role}
                </span>
              </Button>
            )
          })}
        </div>

        <p className="v3-subtitle" style={{ marginBottom: 0 }}>
          الدخول التجريبي مخصّص للعرض فقط. أوقف تفعيله بإزالة VITE_DEMO_MODE.
        </p>
      </section>
    </div>
  )
}
