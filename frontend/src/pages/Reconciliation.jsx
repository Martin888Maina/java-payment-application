import { useState } from 'react'
import { reconcile } from '../api.js'
import { formatNumber } from '../format.js'
import { parseProviderRecords } from '../providerRecords.js'

function lineErrors(fieldErrors, lines) {
  return fieldErrors.map(({ field, message }) => {
    const match = field.match(/^records\[(\d+)\]\.(\w+)$/)
    return match ? `Line ${lines[Number(match[1])]}: ${match[2]} ${message}` : `${field} ${message}`
  })
}

function Reconciliation() {
  const [text, setText] = useState('')
  const [running, setRunning] = useState(false)
  const [errors, setErrors] = useState([])
  const [result, setResult] = useState(null)

  async function run(event) {
    event.preventDefault()
    setResult(null)
    const { records, lines, errors: parseErrors } = parseProviderRecords(text)
    if (parseErrors.length > 0) {
      setErrors(parseErrors)
      return
    }
    if (records.length === 0) {
      setErrors(['Enter at least one record.'])
      return
    }

    setRunning(true)
    setErrors([])
    try {
      setResult(await reconcile(records))
    } catch (e) {
      setErrors(e.fieldErrors.length > 0 ? lineErrors(e.fieldErrors, lines) : [e.message])
    } finally {
      setRunning(false)
    }
  }

  return (
    <>
      <h2>Reconciliation</h2>
      <form onSubmit={run}>
        <p>
          <label htmlFor="records">Provider records, one per line: reference,amount,status</label>
          <textarea
            id="records"
            rows={8}
            spellCheck={false}
            value={text}
            onChange={(event) => setText(event.target.value)}
          />
        </p>
        <p>Example: PAY-7F3K9Q2M8XWD,1500.00,SUCCESSFUL</p>
        <p>
          <button type="submit" className="primary" disabled={running}>
            {running ? 'Running...' : 'Run reconciliation'}
          </button>
        </p>
      </form>
      {errors.map((error) => (
        <p key={error}>Error: {error}</p>
      ))}
      {result && (
        <>
          <h3>Summary</h3>
          <table className="summary">
            <tbody>
              <tr>
                <th>Provider records</th>
                <td>{result.summary.providerRecords}</td>
              </tr>
              <tr>
                <th>Matched</th>
                <td>{result.summary.matched}</td>
              </tr>
              <tr>
                <th>Amount mismatches</th>
                <td>{result.summary.amountMismatches}</td>
              </tr>
              <tr>
                <th>Status mismatches</th>
                <td>{result.summary.statusMismatches}</td>
              </tr>
              <tr>
                <th>Missing on our side</th>
                <td>{result.summary.missingOnOurSide}</td>
              </tr>
              <tr>
                <th>Missing on provider side</th>
                <td>{result.summary.missingOnProviderSide}</td>
              </tr>
              <tr>
                <th>Matched amount</th>
                <td>{formatNumber(result.summary.matchedAmount)}</td>
              </tr>
            </tbody>
          </table>
        </>
      )}
    </>
  )
}

export default Reconciliation
