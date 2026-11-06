// apps/web/src/v3/screens/AdminDashboard.tsx
// لوحة المدير V3 — سجل التدقيق الشامل + الإحصائيات.
// يعتمد على عميل الـ API الموجود (`src/api.ts`) و`/api/v3/audit-logs` (المسار نسبي هنا لأن الأساس في api.ts).
import { useCallback, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { Activity, Plus, RefreshCw, TrendingUp, Users } from 'lucide-react'
import { request } from '../../api'
import { Button } from '../components/ui/Button'
import AddUser from './AddUser'
import '../v3.css'

interface AuditActor {
  id: string
  name: string
  role: string
}

interface AuditLog {
  id: string
  // الخادم يعيد `actor` (سكيما AuditLog الحقيقية)؛ `user` مدعوم للتوافقية فقط.
  actor?: AuditActor | null
  user?: AuditActor | null
  action: string
  entityType: string
  createdAt: string
}

interface AuditLogsResponse {
  logs: AuditLog[]
  pagination: { page: number; limit: number; total: number; totalPages: number }
}

interface AuditStats {
  totalLogs: number
  todayLogs: number
  topActions: { action: string; count: number }[]
  topUsers: { user: AuditActor | null; count: number }[]
}

const actorOf = (log: AuditLog): AuditActor | null => log.actor ?? log.user ?? null

export default function AdminDashboardV3() {
  const [showAddUser, setShowAddUser] = useState(false)
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([])
  const [stats, setStats] = useState<AuditStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchAuditLogs = useCallback(async () => {
    try {
      const data = await request<AuditLogsResponse>('/v3/audit-logs', { query: { limit: 50 } })
      setAuditLogs(data.logs ?? [])
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'فشل تحميل سجل الإجراءات')
    }
  }, [])

  const fetchStats = useCallback(async () => {
    try {
      const data = await request<AuditStats>('/v3/audit-logs/stats')
      setStats(data)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'فشل تحميل الإحصائيات')
    }
  }, [])

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    await Promise.all([fetchAuditLogs(), fetchStats()])
    setLoading(false)
  }, [fetchAuditLogs, fetchStats])

  useEffect(() => {
    void refresh()
  }, [refresh])

  if (showAddUser) {
    return <AddUser onBack={() => setShowAddUser(false)} onCreated={() => setShowAddUser(false)} />
  }

  if (loading) {
    return (
      <div className="v3-page v3-loading">
        <span>جاري التحميل...</span>
      </div>
    )
  }

  return (
    <div className="v3-page">
      <header className="v3-header">
        <div>
          <h1 className="v3-title">لوحة المدير</h1>
          <p className="v3-subtitle">إدارة كاملة للمنصة وسجل الإجراءات</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <Button variant="outline" size="md" onClick={() => setShowAddUser(true)}>
            <Plus size={16} /> إضافة مستخدم
          </Button>
          <Button variant="outline" size="md" onClick={() => void refresh()}>
            <RefreshCw size={16} />
            تحديث
          </Button>
        </div>
      </header>

      {error && (
        <p className="v3-subtitle" style={{ color: 'var(--red)' }}>
          {error}
        </p>
      )}

      {stats && (
        <div className="v3-grid">
          <StatCard icon={<Users size={22} />} label="إجمالي الإجراءات" value={stats.totalLogs} />
          <StatCard icon={<Activity size={22} />} label="إجراءات اليوم" value={stats.todayLogs} />
          <StatCard icon={<TrendingUp size={22} />} label="أكثر إجراء" value={stats.topActions[0]?.action ?? '-'} />
          <StatCard icon={<Users size={22} />} label="أكثر مستخدم" value={stats.topUsers[0]?.user?.name ?? '-'} />
        </div>
      )}

      <section className="v3-panel">
        <div className="v3-panel-head">
          <Activity size={18} color="var(--gold)" />
          <h2>سجل الإجراءات الشامل</h2>
        </div>

        <div className="v3-log-list">
          {auditLogs.length === 0 && <p className="v3-empty">لا توجد إجراءات مسجّلة</p>}
          {auditLogs.map((log) => {
            const actor = actorOf(log)
            return (
              <article key={log.id} className="v3-log">
                <div>
                  <div className="v3-log-user">
                    <span>{actor?.name ?? 'النظام'}</span>
                    <span className="v3-role-chip">{actor?.role ?? 'SYSTEM'}</span>
                  </div>
                  <p className="v3-log-action">{log.action}</p>
                </div>
                <div className="v3-log-meta">
                  <span>{new Date(log.createdAt).toLocaleString('ar')}</span>
                  <p>{log.entityType}</p>
                </div>
              </article>
            )
          })}
        </div>
      </section>
    </div>
  )
}

function StatCard({ icon, label, value }: { icon: ReactNode; label: string; value: string | number }) {
  return (
    <div className="v3-stat">
      <div className="v3-icon-box">{icon}</div>
      <div>
        <p className="v3-stat-value">{value}</p>
        <p className="v3-stat-label">{label}</p>
      </div>
    </div>
  )
}
