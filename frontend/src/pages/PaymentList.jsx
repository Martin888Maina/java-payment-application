import { useEffect, useState } from 'react'
import { listPayments } from '../api.js'
import { formatAmount, formatDate } from '../format.js'

function PaymentList() {
  const [payments, setPayments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    listPayments()
      .then(setPayments)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  return (
    <>
      <h2>Payments</h2>
      {loading && <p>Loading payments...</p>}
      {error && <p>Error: {error}</p>}
      {!loading && !error && payments.length === 0 && <p>No payments yet.</p>}
      {payments.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Reference</th>
              <th>Merchant reference</th>
              <th>Amount</th>
              <th>Phone</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {payments.map((payment) => (
              <tr key={payment.reference}>
                <td>{payment.reference}</td>
                <td>{payment.merchantReference}</td>
                <td>{formatAmount(payment.amount, payment.currency)}</td>
                <td>{payment.payerPhone}</td>
                <td>{payment.status}</td>
                <td>{formatDate(payment.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}

export default PaymentList
