import { useState } from 'react'
import { useNavigate } from 'react-router'
import { createPayment } from '../api.js'

const emptyForm = { merchantReference: '', amount: '', currency: 'KES', payerPhone: '' }

function NewPayment() {
  const navigate = useNavigate()
  const [form, setForm] = useState(emptyForm)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  function update(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  async function submit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await createPayment({
        merchantReference: form.merchantReference.trim(),
        amount: form.amount.trim(),
        currency: form.currency.trim() || null,
        payerPhone: form.payerPhone.trim(),
      })
      navigate('/')
    } catch (e) {
      setError(e.message)
      setSubmitting(false)
    }
  }

  return (
    <>
      <h2>New payment</h2>
      <form onSubmit={submit}>
        <p>
          <label htmlFor="merchantReference">Merchant reference</label>
          <input id="merchantReference" name="merchantReference" value={form.merchantReference} onChange={update} />
        </p>
        <p>
          <label htmlFor="amount">Amount</label>
          <input id="amount" name="amount" inputMode="decimal" value={form.amount} onChange={update} />
        </p>
        <p>
          <label htmlFor="currency">Currency</label>
          <input id="currency" name="currency" maxLength={3} value={form.currency} onChange={update} />
        </p>
        <p>
          <label htmlFor="payerPhone">Payer phone (2547XXXXXXXX)</label>
          <input id="payerPhone" name="payerPhone" inputMode="numeric" value={form.payerPhone} onChange={update} />
        </p>
        <p>
          <button type="submit" className="primary" disabled={submitting}>
            {submitting ? 'Creating...' : 'Create payment'}
          </button>
        </p>
      </form>
      {error && <p>Error: {error}</p>}
    </>
  )
}

export default NewPayment
