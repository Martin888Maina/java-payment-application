import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { getPayment } from '../api.js'
import { formatAmount, formatDate } from '../format.js'

function PaymentDetail() {
  const { reference } = useParams()
  const [payment, setPayment] = useState(null)
  const [error, setError] = useState(null)

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
      <p>
        <Link to="/">Back to payments</Link>
      </p>
    </>
  )
}

export default PaymentDetail
