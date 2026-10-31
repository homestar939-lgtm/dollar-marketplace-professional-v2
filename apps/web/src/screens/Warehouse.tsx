import { useEffect, useState } from 'react'
import { api, ApiError } from '../api'
import type { Order } from '../types'
import { Badge, Button, Card, Empty, ErrorNote, Field, Stat } from '../components/ui'

export default function Warehouse() {
  const [orders, setOrders] = useState<Order[]>([])
  const [selected, setSelected] = useState<Order | null>(null)
  const [quantities, setQuantities] = useState<Record<string, number>>({})
  const [courier, setCourier] = useState({ courierPhone: '', courierName: '', courierChannel: 'WhatsApp' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = async () => {
    try {
      const result = await api.orders({ limit: 50 })
      setOrders(result.data)
      if (selected) {
        const fresh = result.data.find((order) => order.id === selected.id) ?? null
        setSelected(fresh)
      }
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر تحميل الطلبات')
    }
  }

  useEffect(() => {
    void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const open = (order: Order) => {
    setSelected(order)
    const initial: Record<string, number> = {}
    for (const item of order.items) initial[item.id] = item.preparedQuantity
    setQuantities(initial)
  }

  const prepare = async () => {
    if (!selected) return
    setBusy(true)
    setError(null)
    try {
      await api.prepareOrder(selected.id, Object.entries(quantities).map(([orderItemId, preparedQuantity]) => ({ orderItemId, preparedQuantity })))
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر تحديث التجهيز')
    } finally {
      setBusy(false)
    }
  }

  const createInvoice = async () => {
    if (!selected) return
    setBusy(true)
    setError(null)
    try {
      const { invoice } = await api.createInvoice(selected.id)
      await api.sendInvoice(invoice.id)
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر إنشاء الفاتورة')
    } finally {
      setBusy(false)
    }
  }

  const ship = async () => {
    if (!selected) return
    setBusy(true)
    setError(null)
    try {
      await api.shipOrder(selected.id, courier)
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر تسجيل الإرسال')
    } finally {
      setBusy(false)
    }
  }

  const pending = orders.filter((order) => ['PENDING', 'CONFIRMED', 'PREPARED', 'INVOICED'].includes(order.status)).length

  return (
    <>
      <h1>لوحة المخزن</h1>
      <ErrorNote message={error} />
      <div className="stats">
        <Stat label="طلبات قيد التنفيذ" value={String(pending)} />
        <Stat label="إجمالي الطلبات" value={String(orders.length)} tone="green" />
      </div>

      <Card title="الطلبات" actions={<Button variant="ghost" onClick={() => void load()}>تحديث</Button>}>
        {orders.length === 0 ? (
          <Empty text="لا توجد طلبات." />
        ) : (
          <table>
            <thead><tr><th>الرقم</th><th>المشتري</th><th>الإجمالي</th><th>الحالة</th><th></th></tr></thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id}>
                  <td className="mono">{order.orderNumber}</td>
                  <td>{order.buyer?.name ?? '—'}</td>
                  <td className="mono">{order.totalAmount}</td>
                  <td><Badge status={order.status} /></td>
                  <td><Button variant="ghost" onClick={() => open(order)}>فتح</Button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {selected && (
        <div className="grid-2">
          <Card title={`تجهيز الطلب ${selected.orderNumber}`}>
            <table>
              <thead><tr><th>المنتج</th><th>المطلوب</th><th>المحضّر</th></tr></thead>
              <tbody>
                {selected.items.map((item) => (
                  <tr key={item.id}>
                    <td>{item.productName}</td>
                    <td className="mono">{item.quantity}</td>
                    <td>
                      <input
                        type="number"
                        min={0}
                        max={item.quantity}
                        value={quantities[item.id] ?? 0}
                        onChange={(e) => setQuantities({ ...quantities, [item.id]: Number(e.target.value) })}
                        style={{ width: 90, background: '#0f0f15', border: '1px solid #26262f', borderRadius: 8, padding: '6px 8px', color: '#f7f7fa' }}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="row" style={{ marginTop: 12 }}>
              <Button onClick={() => void prepare()} disabled={busy}>حفظ التجهيز</Button>
              <Button variant="ghost" onClick={() => void createInvoice()} disabled={busy}>إنشاء فاتورة وإرسالها</Button>
            </div>
          </Card>

          <Card title="تسجيل إرسال الطلبية">
            <Field label="رقم المندوب">
              <input value={courier.courierPhone} onChange={(e) => setCourier({ ...courier, courierPhone: e.target.value })} placeholder="0555123456" />
            </Field>
            <Field label="اسم المندوب (اختياري)">
              <input value={courier.courierName} onChange={(e) => setCourier({ ...courier, courierName: e.target.value })} />
            </Field>
            <Field label="وسيلة التواصل">
              <input value={courier.courierChannel} onChange={(e) => setCourier({ ...courier, courierChannel: e.target.value })} />
            </Field>
            <Button onClick={() => void ship()} disabled={busy || !courier.courierPhone}>تأكيد الإرسال</Button>
            {selected.shippedAt && <p className="empty">أُرسل في {new Date(selected.shippedAt).toLocaleString('ar')}</p>}
          </Card>
        </div>
      )}
    </>
  )
}
