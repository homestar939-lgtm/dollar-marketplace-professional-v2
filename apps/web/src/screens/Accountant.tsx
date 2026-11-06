import { useEffect, useState } from 'react'
import { api, ApiError } from '../api'
import type { Invoice, Receipt } from '../types'
import { Badge, Button, Card, Empty, ErrorNote, Stat } from '../components/ui'
import { subscribeRealtime } from '../v3/realtime'

export default function Accountant() {
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [receipts, setReceipts] = useState<Receipt[]>([])
  const [report, setReport] = useState<Awaited<ReturnType<typeof api.financialReport>> | null>(null)
  const [collectionReport, setCollectionReport] = useState<Awaited<ReturnType<typeof api.collections>> | null>(null)
  const [period, setPeriod] = useState<'WEEKLY' | 'MONTHLY' | 'YEARLY'>('MONTHLY')
  const [periodDate, setPeriodDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [matching, setMatching] = useState<Record<string, unknown[]>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = async () => {
    try {
      const [invoicePage, receiptPage, financial, collections, matchQueue] = await Promise.all([
        api.invoices({ status: 'SENT', limit: 50 }),
        api.receipts({ status: 'SENT', limit: 50 }),
        api.financialReport(),
        api.collections({ period, date: periodDate }),
        api.matching()
      ])
      setInvoices(invoicePage.data)
      setReceipts(receiptPage.data)
      setReport(financial)
      setCollectionReport(collections)
      setMatching(matchQueue)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر تحميل البيانات')
    }
  }

  useEffect(() => {
    void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period, periodDate])

  useEffect(() => subscribeRealtime(() => { void load() }), [period, periodDate])

  const act = async (fn: () => Promise<unknown>) => {
    setBusy(true)
    setError(null)
    try {
      await fn()
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر تنفيذ العملية')
    } finally {
      setBusy(false)
    }
  }

  const reject = (kind: 'invoice' | 'receipt', id: string) => {
    const reason = window.prompt('سبب الرفض (إعادة للتصحيح):')
    if (!reason) return
    void act(() => (kind === 'invoice' ? api.rejectInvoice(id, reason) : api.rejectReceipt(id, reason)))
  }

  return (
    <>
      <h1>لوحة المحاسب</h1>
      <ErrorNote message={error} />

      <Card title="تحصيلات وعمولات الفترة">
        <div className="row" style={{ alignItems: 'end', flexWrap: 'wrap', gap: 12 }}>
          <label>
            الفترة
            <select value={period} onChange={(event) => setPeriod(event.target.value as typeof period)}>
              <option value="WEEKLY">أسبوعية</option>
              <option value="MONTHLY">شهرية</option>
              <option value="YEARLY">سنوية</option>
            </select>
          </label>
          <label>
            تاريخ داخل الفترة
            <input type="date" value={periodDate} onChange={(event) => setPeriodDate(event.target.value)} />
          </label>
        </div>
        {collectionReport && (
          <>
            <p className="empty">النطاق: {collectionReport.period.from?.slice(0, 10) ?? '—'} – {collectionReport.period.to?.slice(0, 10) ?? '—'}</p>
            <div className="stats">
              <Stat label="إجمالي التحصيل" value={collectionReport.totals.collected} tone="green" />
              <Stat label="إجمالي العمولات" value={collectionReport.totals.commission} />
              <Stat label="سندات معتمدة" value={String(collectionReport.totals.receipts)} />
            </div>
          </>
        )}
      </Card>

      {report && (
        <div className="stats">
          <Stat label="مدين" value={report.ledger.debit} />
          <Stat label="دائن" value={report.ledger.credit} tone="green" />
          <Stat label="المستحق الكلي" value={report.outstanding} tone="red" />
          <Stat label="فواتير معتمدة" value={String(report.invoices.APPROVED?.count ?? 0)} />
        </div>
      )}

      <Card title={`فواتير بانتظار المراجعة (${invoices.length})`} actions={<Button variant="ghost" onClick={() => void load()}>تحديث</Button>}>
        {invoices.length === 0 ? (
          <Empty text="لا توجد فواتير معلقة." />
        ) : (
          <table>
            <thead><tr><th>الفاتورة</th><th>المبلغ</th><th>التاريخ</th><th>إجراء</th></tr></thead>
            <tbody>
              {invoices.map((invoice) => (
                <tr key={invoice.id}>
                  <td className="mono">{invoice.invoiceNumber}</td>
                  <td className="mono">{invoice.totalAmount} {invoice.currency}</td>
                  <td>{new Date(invoice.createdAt).toLocaleDateString('ar')}</td>
                  <td className="row">
                    <Button disabled={busy} onClick={() => void act(() => api.approveInvoice(invoice.id))}>اعتماد</Button>
                    <Button variant="danger" disabled={busy} onClick={() => reject('invoice', invoice.id)}>رفض</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      <Card title={`سندات قبض بانتظار المراجعة (${receipts.length})`}>
        {receipts.length === 0 ? (
          <Empty text="لا توجد سندات معلقة." />
        ) : (
          <table>
            <thead><tr><th>السند</th><th>المشتري</th><th>المحصل</th><th>المبلغ</th><th>إجراء</th></tr></thead>
            <tbody>
              {receipts.map((receipt) => (
                <tr key={receipt.id}>
                  <td className="mono">{receipt.receiptNumber}</td>
                  <td>{receipt.buyer?.name ?? '—'}</td>
                  <td>{receipt.collector?.name ?? '—'}</td>
                  <td className="mono">{receipt.amount} {receipt.currency}</td>
                  <td className="row">
                    <Button disabled={busy} onClick={() => void act(() => api.approveReceipt(receipt.id))}>اعتماد</Button>
                    <Button variant="danger" disabled={busy} onClick={() => reject('receipt', receipt.id)}>رفض</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      <Card title="المطابقة">
        {Object.entries(matching).every(([, rows]) => (rows as unknown[]).length === 0) ? (
          <p className="empty">كل الحركات مطابقة ✓</p>
        ) : (
          <table>
            <thead><tr><th>القائمة</th><th>عدد العناصر</th></tr></thead>
            <tbody>
              {Object.entries(matching).map(([key, rows]) => (
                <tr key={key}>
                  <td>{key}</td>
                  <td className="mono">{String((rows as unknown[]).length)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {report && (
        <Card title="الملخص المالي">
          <table>
            <thead><tr><th>الحالة</th><th>العدد</th><th>الإجمالي</th></tr></thead>
            <tbody>
              {Object.entries(report.invoices).map(([status, value]) => (
                <tr key={`inv-${status}`}>
                  <td><Badge status={status} /></td>
                  <td className="mono">{value.count}</td>
                  <td className="mono">{value.total}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}
