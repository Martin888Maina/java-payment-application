export class ApiError extends Error {
  constructor(message, status, fieldErrors) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

async function request(path, options = {}) {
  const headers = options.body ? { 'Content-Type': 'application/json' } : {}

  let response
  try {
    response = await fetch(path, { ...options, headers })
  } catch {
    throw new ApiError('Could not reach the server', 0, [])
  }

  const body = await response.json().catch(() => null)
  if (!response.ok) {
    const message = body?.detail ?? `Request failed with status ${response.status}`
    throw new ApiError(message, response.status, body?.errors ?? [])
  }
  return body
}

export function listPayments(status) {
  const query = status ? `?status=${encodeURIComponent(status)}` : ''
  return request(`/api/v1/payments${query}`)
}

export function getPayment(reference) {
  return request(`/api/v1/payments/${encodeURIComponent(reference)}`)
}

export function createPayment(payment) {
  return request('/api/v1/payments', { method: 'POST', body: JSON.stringify(payment) })
}

export function simulateCallback(reference, status) {
  return request(`/api/v1/dev/payments/${encodeURIComponent(reference)}/simulate-callback`, {
    method: 'POST',
    body: JSON.stringify({ status }),
  })
}

export function reconcile(records) {
  return request('/api/v1/reconciliation', { method: 'POST', body: JSON.stringify({ records }) })
}
