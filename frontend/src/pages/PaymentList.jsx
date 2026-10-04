import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { listPayments, resetPayments } from '../api.js'
import { formatAmount, formatDate } from '../format.js'

// Page numbers in the address bar start at 1; anything else falls back to the first page
function pageFrom(searchParams) {
  const page = Number(searchParams.get('page'))
  return Number.isInteger(page) && page >= 1 ? page : 1
}

function PaymentList() {
  const [searchParams, setSearchParams] = useSearchParams()
  const status = searchParams.get('status') ?? ''
  const page = pageFrom(searchParams)
  const [reloads, setReloads] = useState(0)
  const query = `${status}|${page}|${reloads}`
  const [result, setResult] = useState({ query: null, data: null, error: null })
  const [confirming, setConfirming] = useState(false)
  const [resetting, setResetting] = useState(false)
  const [resetError, setResetError] = useState(null)

  useEffect(() => {
    // Ignores a slow response that arrives after the filter or page has changed again
    let ignore = false
    const current = `${status}|${page}|${reloads}`
    listPayments(status, page - 1)
      .then((data) => !ignore && setResult({ query: current, data, error: null }))
      .catch((e) => !ignore && setResult({ query: current, data: null, error: e.message }))
    return () => {
      ignore = true
    }
  }, [status, page, reloads])

  const loading = result.query !== query
  const { data, error } = result
  const payments = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const hasPayments = (data?.totalElements ?? 0) > 0

  function changeStatus(event) {
    const value = event.target.value
    setSearchParams(value ? { status: value } : {})
  }

  function goToPage(number) {
    const params = status ? { status } : {}
    if (number > 1) {
      params.page = String(number)
    }
    setSearchParams(params)
    window.scrollTo(0, 0)
  }

  function startReset() {
    setResetError(null)
    setConfirming(true)
  }

  async function reset() {
    setResetting(true)
    try {
      await resetPayments()
      setSearchParams({})
      setReloads((count) => count + 1)
    } catch (e) {
      // The reset route only exists when the back end runs with the dev profile
      const missing = e.status === 404 && e.message.startsWith('No endpoint matches')
      setResetError(missing ? 'Resetting demo data is only available in the dev profile.' : e.message)
    } finally {
      setResetting(false)
      setConfirming(false)
    }
  }

  return (
    <>
      <div className="page-header">
        <h2>Payments</h2>
        <div className="toolbar">
          <div className="filter">
            <label htmlFor="status">Status</label>
            <select id="status" value={status} onChange={changeStatus}>
              <option value="">All</option>
              <option value="PENDING">Pending</option>
              <option value="SUCCESSFUL">Successful</option>
              <option value="FAILED">Failed</option>
            </select>
          </div>
          {hasPayments && (
            <button type="button" disabled={confirming} onClick={startReset}>
              Reset demo data
            </button>
          )}
        </div>
      </div>
      {confirming && (
        <div className="confirm">
          <p>Delete all payments? This cannot be undone.</p>
          <div className="actions">
            <button type="button" className="primary" disabled={resetting} onClick={reset}>
              {resetting ? 'Deleting...' : 'Yes, delete all'}
            </button>
            <button type="button" disabled={resetting} onClick={() => setConfirming(false)}>
              Cancel
            </button>
          </div>
        </div>
      )}
      {resetError && <p className="error">Error: {resetError}</p>}
      {loading && <p>Loading payments...</p>}
      {!loading && error && <p className="error">Error: {error}</p>}
      {!loading && !error && !hasPayments && <p>{status ? 'No payments with this status.' : 'No payments yet.'}</p>}
      {!loading && !error && hasPayments && payments.length === 0 && (
        <p>
          No payments on this page. <Link to={status ? `?status=${status}` : '.'}>Go to the first page</Link>
        </p>
      )}
      {!loading && !error && payments.length > 0 && (
        <>
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
          {totalPages > 1 && (
            <nav className="pagination" aria-label="Pages">
              <button type="button" disabled={page <= 1} onClick={() => goToPage(page - 1)}>
                Previous
              </button>
              <p>
                Page {page} of {totalPages}, {data.totalElements} payments
              </p>
              <button type="button" disabled={page >= totalPages} onClick={() => goToPage(page + 1)}>
                Next
              </button>
            </nav>
          )}
        </>
      )}
    </>
  )
}

export default PaymentList
