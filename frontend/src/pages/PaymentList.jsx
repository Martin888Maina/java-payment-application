import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { listPayments } from '../api.js'
import { formatAmount, formatDate } from '../format.js'

function PaymentList() {
  const [searchParams, setSearchParams] = useSearchParams()
  const status = searchParams.get('status') ?? ''
  const [result, setResult] = useState({ status: null, payments: [], error: null })

  useEffect(() => {
    // Ignores a slow response that arrives after the filter has changed again
    let ignore = false
    listPayments(status)
      .then((payments) => !ignore && setResult({ status, payments, error: null }))
      .catch((e) => !ignore && setResult({ status, payments: [], error: e.message }))
    return () => {
      ignore = true
    }
  }, [status])

  const loading = result.status !== status
  const { payments, error } = result

  function changeStatus(event) {
    const value = event.target.value
    setSearchParams(value ? { status: value } : {})
  }

  return (
    <>
      <div className="page-header">
        <h2>Payments</h2>
        <div className="filter">
          <label htmlFor="status">Status</label>
          <select id="status" value={status} onChange={changeStatus}>
            <option value="">All</option>
            <option value="PENDING">Pending</option>
            <option value="SUCCESSFUL">Successful</option>
            <option value="FAILED">Failed</option>
          </select>
        </div>
      </div>
      {loading && <p>Loading payments...</p>}
      {!loading && error && <p className="error">Error: {error}</p>}
      {!loading && !error && payments.length === 0 && (
        <p>{status ? 'No payments with this status.' : 'No payments yet.'}</p>
      )}
      {!loading && !error && payments.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Reference</th>
                <th>Merchant reference</th>
                <th className="num">Amount</th>
                <th>Phone</th>
                <th>Status</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((payment) => (
                <tr key={payment.reference}>
                  <td>
                    <Link to={`/payments/${payment.reference}`}>{payment.reference}</Link>
                  </td>
                  <td>{payment.merchantReference}</td>
                  <td className="num">{formatAmount(payment.amount, payment.currency)}</td>
                  <td>{payment.payerPhone}</td>
                  <td>{payment.status}</td>
                  <td>{formatDate(payment.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}

export default PaymentList
