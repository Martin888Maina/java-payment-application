import { useState } from 'react'
import { useNavigate } from 'react-router'
import { createPayment } from '../api.js'

const emptyForm = { merchantReference: '', amount: '', currency: 'KES', payerPhone: '' }

function Field({ name, label, error, ...inputProps }) {
  return (
    <p>
      <label htmlFor={name}>{label}</label>
      <input
        id={name}
        name={name}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${name}-error` : undefined}
        {...inputProps}
      />
      {error && (
        <span id={`${name}-error`} className="field-error">
          {error}
        </span>
      )}
    </p>
  )
}

function NewPayment() {
  const navigate = useNavigate()
  const [form, setForm] = useState(emptyForm)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  function update(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  function fieldError(name) {
    const messages = error?.fieldErrors.filter((e) => e.field === name).map((e) => e.message) ?? []
    return messages.join(', ')
  }

  async function submit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const payment = await createPayment({
        merchantReference: form.merchantReference.trim(),
        amount: form.amount.trim(),
        currency: form.currency.trim() || null,
        payerPhone: form.payerPhone.trim(),
      })
      navigate(`/payments/${payment.reference}`)
    } catch (e) {
      setError({ message: e.message, fieldErrors: e.fieldErrors })
      setSubmitting(false)
    }
  }

  return (
    <>
      <h2>New payment</h2>
      <form onSubmit={submit}>
        <Field
          name="merchantReference"
          label="Merchant reference"
          value={form.merchantReference}
          onChange={update}
          error={fieldError('merchantReference')}
        />
        <Field
          name="amount"
          label="Amount"
          inputMode="decimal"
          value={form.amount}
          onChange={update}
          error={fieldError('amount')}
        />
        <Field
          name="currency"
          label="Currency"
          maxLength={3}
          value={form.currency}
          onChange={update}
          error={fieldError('currency')}
        />
        <Field
          name="payerPhone"
          label="Payer phone (2547XXXXXXXX)"
          inputMode="numeric"
          value={form.payerPhone}
          onChange={update}
          error={fieldError('payerPhone')}
        />
        <p>
          <button type="submit" className="primary" disabled={submitting}>
            {submitting ? 'Creating...' : 'Create payment'}
          </button>
        </p>
      </form>
      {error && <p>Error: {error.message}</p>}
    </>
  )
}

export default NewPayment
