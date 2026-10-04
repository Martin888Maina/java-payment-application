const amountFormat = new Intl.NumberFormat('en-KE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const dateFormat = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' })

export function formatAmount(amount, currency) {
  return `${currency} ${amountFormat.format(amount)}`
}

export function formatDate(isoDate) {
  return dateFormat.format(new Date(isoDate))
}
