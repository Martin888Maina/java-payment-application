const HEADER = 'reference,amount,status'

// Each record keeps the line it came from so API errors can point back to it
export function parseProviderRecords(text) {
  const records = []
  const lines = []
  const errors = []

  text.split('\n').forEach((rawLine, index) => {
    const line = rawLine.trim()
    const lineNumber = index + 1
    if (line === '' || line.replace(/\s/g, '').toLowerCase() === HEADER) {
      return
    }

    const parts = line.split(',').map((part) => part.trim())
    if (parts.length !== 3) {
      errors.push(`Line ${lineNumber}: expected reference,amount,status`)
      return
    }

    const [reference, amount, status] = parts
    records.push({ reference, amount, status: status.toUpperCase() })
    lines.push(lineNumber)
  })

  return { records, lines, errors }
}
