const amountFormat = new Intl.NumberFormat('en-KE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const dateFormat = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' })

export function formatNumber(amount) {
  return amountFormat.format(amount)
}

export function formatAmount(amount, currency) {
  return `${currency} ${formatNumber(amount)}`
}

export function formatDate(isoDate) {
  return dateFormat.format(new Date(isoDate))
}
