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
    throw new ApiError(errorMessage(response.status, body), response.status, body?.errors ?? [])
  }
  return body
}

function errorMessage(status, body) {
  if (body?.detail) {
    return body.detail
  }
  // The development proxy answers with an empty 502 when the API is not running
  if (status >= 500) {
    return 'Could not reach the server'
  }
  return `Request failed with status ${status}`
}

export const PAGE_SIZE = 10

// The page number is 0-based, as the API expects
export function listPayments(status, page) {
  const params = new URLSearchParams({ page, size: PAGE_SIZE })
  if (status) {
    params.set('status', status)
  }
  return request(`/api/v1/payments?${params}`)
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

export function resetPayments() {
  return request('/api/v1/dev/payments', { method: 'DELETE' })
}

export function reconcile(records) {
  return request('/api/v1/reconciliation', { method: 'POST', body: JSON.stringify({ records }) })
}
