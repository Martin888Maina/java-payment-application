import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { getPayment, simulateCallback } from '../api.js'
import { formatAmount, formatDate } from '../format.js'

function PaymentDetail() {
  const { reference } = useParams()
  const [payment, setPayment] = useState(null)
  const [error, setError] = useState(null)
  const [simulating, setSimulating] = useState(false)
  const [simulateError, setSimulateError] = useState(null)

  useEffect(() => {
    let ignore = false
    setPayment(null)
    setError(null)
    getPayment(reference)
      .then((data) => !ignore && setPayment(data))
      .catch((e) => !ignore && setError(e.message))
    return () => {
      ignore = true
    }
  }, [reference])

  async function simulate(status) {
    setSimulating(true)
    setSimulateError(null)
    try {
      setPayment(await simulateCallback(reference, status))
    } catch (e) {
      // The simulator route only exists when the back end runs with the dev profile
      const missing = e.status === 404 && e.message.startsWith('No endpoint matches')
      setSimulateError(missing ? 'The callback simulator is only available in the dev profile.' : e.message)
    } finally {
      setSimulating(false)
    }
  }

  return (
    <>
      <h2>Payment {reference}</h2>
      {!payment && !error && <p>Loading payment...</p>}
      {error && <p>Error: {error}</p>}
      {payment && (
        <dl>
          <dt>Status</dt>
          <dd>{payment.status}</dd>
          <dt>Merchant reference</dt>
          <dd>{payment.merchantReference}</dd>
          <dt>Amount</dt>
          <dd>{formatAmount(payment.amount, payment.currency)}</dd>
          <dt>Payer phone</dt>
          <dd>{payment.payerPhone}</dd>
          <dt>Provider reference</dt>
          <dd>{payment.providerReference ?? 'None yet'}</dd>
          <dt>Created</dt>
          <dd>{formatDate(payment.createdAt)}</dd>
          <dt>Updated</dt>
          <dd>{formatDate(payment.updatedAt)}</dd>
        </dl>
      )}
      {payment?.status === 'PENDING' && (
        <>
          <p>Send a signed test callback, as a payment provider would.</p>
          <p>
            <button type="button" className="primary" disabled={simulating} onClick={() => simulate('SUCCESSFUL')}>
              Simulate success
            </button>{' '}
            <button type="button" disabled={simulating} onClick={() => simulate('FAILED')}>
              Simulate failure
            </button>
          </p>
        </>
      )}
      {simulateError && <p>Error: {simulateError}</p>}
      <p>
        <Link to="/">Back to payments</Link>
      </p>
    </>
  )
}

export default PaymentDetail
