import { useEffect, useState } from 'react'
import { api, ApiError } from '../api'
import type { Agency, User } from '../types'
import { Badge, Button, Card, Empty, ErrorNote, Field } from '../components/ui'

export default function Admin() {
  const [users, setUsers] = useState<User[]>([])
  const [agencies, setAgencies] = useState<Agency[]>([])
  const [settings, setSettings] = useState<Record<string, unknown> | null>(null)
  const [staff, setStaff] = useState({ name: '', email: '', password: '', role: 'WAREHOUSE', commissionRate: '' })
  const [agency, setAgency] = useState({ name: '', slug: '' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = async () => {
    try {
      const [userPage, agencyPage, configuration] = await Promise.all([
        api.users({ limit: 50 }),
        api.agencies({ limit: 50 }),
        api.settings()
      ])
      setUsers(userPage.data)
      setAgencies(agencyPage.data)
      setSettings(configuration.settings)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر تحميل البيانات')
    }
  }

  useEffect(() => {
    void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const createStaff = async () => {
    setBusy(true)
    setError(null)
    try {
      await api.createStaff({
        name: staff.name,
        email: staff.email,
        password: staff.password,
        role: staff.role,
        ...(staff.role === 'COLLECTOR' && staff.commissionRate ? { commissionRate: Number(staff.commissionRate) } : {})
      })
      setStaff({ name: '', email: '', password: '', role: 'WAREHOUSE', commissionRate: '' })
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر إنشاء الحساب')
    } finally {
      setBusy(false)
    }
  }

  const createAgency = async () => {
    setBusy(true)
    setError(null)
    try {
      await api.createAgency({ name: agency.name, slug: agency.slug })
      setAgency({ name: '', slug: '' })
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر إنشاء الوكالة')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>لوحة المدير</h1>
      <ErrorNote message={error} />

      <div className="grid-2">
        <Card title="إنشاء حساب موظف">
          <Field label="الاسم"><input value={staff.name} onChange={(e) => setStaff({ ...staff, name: e.target.value })} /></Field>
          <Field label="البريد"><input value={staff.email} onChange={(e) => setStaff({ ...staff, email: e.target.value })} /></Field>
          <Field label="كلمة المرور"><input type="password" value={staff.password} onChange={(e) => setStaff({ ...staff, password: e.target.value })} /></Field>
          <Field label="الدور">
            <select value={staff.role} onChange={(e) => setStaff({ ...staff, role: e.target.value })}>
              {['WAREHOUSE', 'COLLECTOR', 'ACCOUNTANT', 'SUPPORT'].map((role) => (
                <option key={role} value={role}>{role}</option>
              ))}
            </select>
          </Field>
          {staff.role === 'COLLECTOR' && (
            <Field label="نسبة العمولة (0.025 = 2.5%)">
              <input value={staff.commissionRate} onChange={(e) => setStaff({ ...staff, commissionRate: e.target.value })} />
            </Field>
          )}
          <Button onClick={() => void createStaff()} disabled={busy || !staff.name || !staff.email || !staff.password}>إنشاء</Button>
        </Card>

        <Card title="إنشاء وكالة">
          <Field label="اسم الوكالة"><input value={agency.name} onChange={(e) => setAgency({ ...agency, name: e.target.value })} /></Field>
          <Field label="المعرّف (slug)"><input value={agency.slug} onChange={(e) => setAgency({ ...agency, slug: e.target.value })} placeholder="al-noor" /></Field>
          <Button onClick={() => void createAgency()} disabled={busy || !agency.name || !agency.slug}>إنشاء</Button>

          {settings && (
            <p className="empty" style={{ marginTop: 12 }}>
              المؤسسة: {String(settings.companyName ?? '—')} · العملة: {String(settings.defaultCurrency ?? '—')} · فترة السداد: {String(settings.paymentTerms ?? '—')} يوم
            </p>
          )}
        </Card>
      </div>

      <Card title="المستخدمون">
        {users.length === 0 ? (
          <Empty text="لا يوجد مستخدمون." />
        ) : (
          <table>
            <thead><tr><th>الاسم</th><th>البريد</th><th>الدور</th><th>الحالة</th></tr></thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.name}</td>
                  <td>{user.email}</td>
                  <td>{user.role}</td>
                  <td><Badge status={user.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      <Card title="الوكالات">
        {agencies.length === 0 ? (
          <Empty text="لا توجد وكالات." />
        ) : (
          <table>
            <thead><tr><th>الاسم</th><th>المعرّف</th><th>الحالة</th><th>المنتجات</th></tr></thead>
            <tbody>
              {agencies.map((item) => (
                <tr key={item.id}>
                  <td>{item.name}</td>
                  <td className="mono">{item.slug}</td>
                  <td><Badge status={item.status} /></td>
                  <td className="mono">{item._count?.products ?? 0}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  )
}
