/**
 * Race-free document numbers. Sequences are created by the migrations, so two
 * concurrent requests can never receive the same invoice/receipt number.
 */
export async function nextDocumentNumber(tx, sequence, prefix) {
  const rows = await tx.$queryRawUnsafe(`SELECT nextval('${sequence}')::bigint AS value`)
  const value = rows?.[0]?.value ?? 1n
  const year = new Date().getUTCFullYear()
  return `${prefix}-${year}-${String(value).padStart(6, '0')}`
}

export const nextInvoiceNumber = (tx) => nextDocumentNumber(tx, 'invoice_number_seq', 'INV')
export const nextReceiptNumber = (tx) => nextDocumentNumber(tx, 'receipt_number_seq', 'RCP')
