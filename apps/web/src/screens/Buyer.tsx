import { useEffect, useMemo, useState } from 'react'
import { api, ApiError } from '../api'
import type { Order, Product, Statement } from '../types'
import { Badge, Button, Card, Empty, ErrorNote, Field, Stat } from '../components/ui'

type CartLine = { product: Product; quantity: number }

export default function Buyer() {
  const [products, setProducts] = useState<Product[]>([])
  const [query, setQuery] = useState('')
  const [cart, setCart] = useState<CartLine[]>([])
  const [orders, setOrders] = useState<Order[]>([])
  const [statement, setStatement] = useState<Statement | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = async () => {
    try {
      const [catalog, myOrders, myStatement] = await Promise.all([
        api.products({ q: query || undefined, limit: 24 }),
        api.orders({ limit: 20 }),
        api.myStatement()
      ])
      setProducts(catalog.data)
      setOrders(myOrders.data)
      setStatement(myStatement)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر تحميل البيانات')
    }
  }

  useEffect(() => {
    void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const cartTotal = useMemo(
    () => cart.reduce((sum, line) => sum + Number(line.product.price) * line.quantity, 0),
    [cart]
  )

  const addToCart = (product: Product) => {
    setCart((current) => {
      const existing = current.find((line) => line.product.id === product.id)
      if (existing) {
        return current.map((line) =>
          line.product.id === product.id ? { ...line, quantity: line.quantity + product.minimumOrderQuantity } : line
        )
      }
      return [...current, { product, quantity: product.minimumOrderQuantity }]
    })
  }

  const submitOrder = async () => {
    setBusy(true)
    setError(null)
    try {
      await api.createOrder(cart.map((line) => ({ productId: line.product.id, quantity: line.quantity })))
      setCart([])
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'تعذّر إنشاء الطلب')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>لوحة المشتري</h1>
      <ErrorNote message={error} />

      {statement && (
        <div className="stats">
          <Stat label="إجمالي المدين" value={`${statement.totals.debit} ${'USD'}`} />
          <Stat label="إجمالي المدفوع" value={statement.totals.credit} tone="green" />
          <Stat label="الرصيد الحالي" value={statement.totals.balance} tone={Number(statement.totals.balance) > 0 ? 'red' : 'green'} />
          <Stat label="فواتير مفتوحة" value={String(statement.outstanding.length)} />
        </div>
      )}

      <Card title="البحث في الكتالوج" actions={
        <div className="row">
          <input
            placeholder="اسم المنتج أو SKU أو الوكالة"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            style={{ background: '#0f0f15', border: '1px solid #26262f', borderRadius: 10, padding: '8px 12px', color: '#f7f7fa' }}
          />
          <Button onClick={() => void load()}>بحث</Button>
        </div>
      }>
        {products.length === 0 ? (
          <Empty text="لا توجد منتجات مطابقة." />
        ) : (
          <div className="cards-grid">
            {products.map((product) => (
              <div className="product-card" key={product.id}>
                <h3>{product.name}</h3>
                <small>{product.agency?.name ?? '—'} · {product.sku}</small>
                <span className="price">{product.price} {product.currency}</span>
                <small>الوحدة: {product.unit} · أقل كمية: {product.minimumOrderQuantity}</small>
                {product.countryOfOrigin && <small>بلد المنشأ: {product.countryOfOrigin}</small>}
                <Button onClick={() => addToCart(product)}>أضف إلى السلة</Button>
              </div>
            ))}
          </div>
        )}
      </Card>

      <div className="grid-2">
        <Card title={`السلة (${cart.length})`} actions={cart.length > 0 ? <Button variant="ghost" onClick={() => setCart([])}>تفريغ</Button> : undefined}>
          {cart.length === 0 ? (
            <Empty text="السلة فارغة." />
          ) : (
            <>
              {cart.map((line) => (
                <div className="cart-line" key={line.product.id}>
                  <span>{line.product.name}</span>
                  <span className="mono">{line.quantity} × {line.product.price} = {(Number(line.product.price) * line.quantity).toFixed(2)}</span>
                  <Button variant="ghost" onClick={() => setCart(cart.filter((l) => l.product.id !== line.product.id))}>حذف</Button>
                </div>
              ))}
              <p className="mono">الإجمالي التقديري: {cartTotal.toFixed(2)} USD</p>
              <Button onClick={() => void submitOrder()} disabled={busy}>تأكيد الطلب</Button>
            </>
          )}
        </Card>

        <Card title="الفواتير والمستحقات">
          {!statement || statement.outstanding.length === 0 ? (
            <Empty text="لا توجد مبالغ مستحقة." />
          ) : (
            <table>
              <thead><tr><th>الفاتورة</th><th>الإجمالي</th><th>المتبقي</th></tr></thead>
              <tbody>
                {statement.outstanding.map((invoice) => (
                  <tr key={invoice.invoiceId}>
                    <td className="mono">{invoice.invoiceNumber}</td>
                    <td className="mono">{invoice.total}</td>
                    <td className="mono">{invoice.remaining}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      <Card title="طلباتي">
        {orders.length === 0 ? (
          <Empty text="لا توجد طلبات بعد." />
        ) : (
          <table>
            <thead><tr><th>الرقم</th><th>التاريخ</th><th>الإجمالي</th><th>الحالة</th><th>التجهيز</th><th>الشحن</th></tr></thead>
            <tbody>
              {orders.map((order) => {
                const ordered = order.items.reduce((sum, item) => sum + item.quantity, 0)
                const prepared = order.items.reduce((sum, item) => sum + item.preparedQuantity, 0)
                return (
                  <tr key={order.id}>
                    <td className="mono">{order.orderNumber}</td>
                    <td>{new Date(order.createdAt).toLocaleDateString('ar')}</td>
                    <td className="mono">{order.totalAmount} {order.currency}</td>
                    <td><Badge status={order.status} /></td>
                    <td className="mono">{prepared}/{ordered}</td>
                    <td>{order.shippedAt ? `أُرسل · ${order.courierPhone ?? ''}` : '—'}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </Card>

      {statement && (
        <Card title="كشف الحساب">
          {statement.statement.length === 0 ? (
            <Empty text="لا توجد حركات مالية." />
          ) : (
            <table>
              <thead><tr><th>التاريخ</th><th>البيان</th><th>مدين</th><th>دائن</th><th>الرصيد</th></tr></thead>
              <tbody>
                {statement.statement.map((line) => (
                  <tr key={line.id}>
                    <td>{new Date(line.createdAt).toLocaleDateString('ar')}</td>
                    <td>{line.description ?? line.referenceType}</td>
                    <td className="mono">{line.type === 'DEBIT' ? line.amount : '—'}</td>
                    <td className="mono">{line.type === 'CREDIT' ? line.amount : '—'}</td>
                    <td className="mono">{line.runningBalance}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      )}
    </>
  )
}
