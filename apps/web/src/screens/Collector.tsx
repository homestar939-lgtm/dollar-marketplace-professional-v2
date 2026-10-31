import { useEffect, useState } from 'react'
import { api, ApiError } from '../api'
import type { BuyerRow, Receipt } from '../types'
import { Badge, Button, Card, Empty, ErrorNote, Field, Stat } from '../components/ui'

export default function Collector() {
  const [buyers, setBuyers] = useState<BuyerRow[]>([])
  const [receipts, setReceipts] = useState<Receipt[]>([])
  const [commission, setCommission] = useState<{ collected: string; commission: string; receipts: number; effectiveRate: string } | null>(null)
  const [form, setForm] = useState({ buyerId: '', amount: '', paymentMethod: 'CASH', notes: '' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = async () => {
    try {
      const [buyersPage, receiptsPage, myCommission] = await Promise.all([
        api.buyers({ limit: 50 }),
        api.receipts({ limit: 50 }),
        api.myCommission()
      ])
      setBuyers(buyersPage.data)
      setReceipts(receiptsPage.data)
      setCommission(myCommission.commission)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر تحميل البيانات')
    }
  }

  useEffect(() => {
    void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const createReceipt = async () => {
    setBusy(true)
    setError(null)
    try {
      const { receipt } = await api.createReceipt({
        buyerId: form.buyerId,
        amount: form.amount,
        currency: 'USD',
        paymentMethod: form.paymentMethod,
        notes: form.notes || undefined
      })
      await api.sendReceipt(receipt.id)
      setForm({ buyerId: '', amount: '', paymentMethod: 'CASH', notes: '' })
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر إنشاء سند القبض')
    } finally {
      setBusy(false)
    }
  }

  const selectedBuyer = buyers.find((buyer) => buyer.id === form.buyerId)

  return (
    <>
      <h1>لوحة المحصل</h1>
      <ErrorNote message={error} />

      {commission && (
        <div className="stats">
          <Stat label="المحصَّل المعتمد" value={commission.collected} />
          <Stat label="العمولة" value={commission.commission} tone="green" />
          <Stat label="سندات معتمدة" value={String(commission.receipts)} />
          <Stat label="نسبة العمولة" value={commission.effectiveRate} />
        </div>
      )}

      <div className="grid-2">
        <Card title="المشترون والأرصدة">
          {buyers.length === 0 ? (
            <Empty text="لا يوجد مشترون." />
          ) : (
            <table>
              <thead><tr><th>المشتري</th><th>مدين</th><th>مدفوع</th><th>مستحق</th></tr></thead>
              <tbody>
                {buyers.map((buyer) => (
                  <tr key={buyer.id}>
                    <td>{buyer.name}<br /><small className="empty">{buyer.email}</small></td>
                    <td className="mono">{buyer.debit}</td>
                    <td className="mono">{buyer.credit}</td>
                    <td className="mono">{buyer.outstanding}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>

        <Card title="سند قبض جديد">
          <Field label="المشتري">
            <select value={form.buyerId} onChange={(e) => setForm({ ...form, buyerId: e.target.value })}>
              <option value="">— اختر —</option>
              {buyers.map((buyer) => (
                <option key={buyer.id} value={buyer.id}>{buyer.name} — مستحق {buyer.outstanding}</option>
              ))}
            </select>
          </Field>
          <Field label="المبلغ المستلم">
            <input value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} placeholder="200.0000" />
          </Field>
          <Field label="وسيلة الدفع">
            <select value={form.paymentMethod} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}>
              {['CASH', 'BANK_TRANSFER', 'CHEQUE', 'MOBILE_WALLET', 'CARD', 'OTHER'].map((method) => (
                <option key={method} value={method}>{method}</option>
              ))}
            </select>
          </Field>
          {selectedBuyer && <p className="empty">المستحق الحالي: {selectedBuyer.outstanding} — تجاوزه يحتاج موافقة المدير.</p>}
          <Button onClick={() => void createReceipt()} disabled={busy || !form.buyerId || !form.amount}>تسجيل وإرسال للمحاسب</Button>
        </Card>
      </div>

      <Card title="أرشيف التحصيل">
        {receipts.length === 0 ? (
          <Empty text="لا توجد سندات." />
        ) : (
          <table>
            <thead><tr><th>السند</th><th>المشتري</th><th>المبلغ</th><th>العمولة</th><th>الحالة</th><th>التاريخ</th></tr></thead>
            <tbody>
              {receipts.map((receipt) => (
                <tr key={receipt.id}>
                  <td className="mono">{receipt.receiptNumber}</td>
                  <td>{receipt.buyer?.name ?? '—'}</td>
                  <td className="mono">{receipt.amount}</td>
                  <td className="mono">{receipt.commissionAmount ?? '—'}</td>
                  <td><Badge status={receipt.status} /></td>
                  <td>{new Date(receipt.createdAt).toLocaleDateString('ar')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  )
}
