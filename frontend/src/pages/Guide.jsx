import { Link } from 'react-router'

function Guide() {
  return (
    <div className="guide">
      <div className="page-header">
        <h2>How to use</h2>
      </div>
      <p className="lead">
        Create payments, follow their status and check them against the payment provider's records in four steps.
      </p>

      <ol className="steps">
        <li>
          <h3>Create a payment</h3>
          <p>
            Enter a merchant reference, an amount, a currency (KES by default) and the payer's phone number in the
            format 2547XXXXXXXX. Sending the same merchant reference again returns the same payment instead of a
            new one.
          </p>
          <Link to="/payments/new">Open New payment</Link>
        </li>
        <li>
          <h3>Find a payment</h3>
          <p>
            Payments are listed newest first. Filter the list by status, then click a reference to see the full
            details.
          </p>
          <Link to="/">Open Payments</Link>
        </li>
        <li>
          <h3>Complete a payment</h3>
          <p>
            On a pending payment, click Simulate success or Simulate failure to send a signed test callback, as a
            payment provider would. This is only available when the back end runs with the dev profile.
          </p>
        </li>
        <li>
          <h3>Reconcile</h3>
          <p>Compare the provider's statement with our payments to find anything that does not agree.</p>
          <ol className="substeps">
            <li>Take the list of transactions the provider processed, for example from their daily statement.</li>
            <li>
              Paste one record per line in the box, in the order reference,amount,status. The reference is our
              payment reference, the amount has up to two decimal places and the status is SUCCESSFUL or FAILED.
            </li>
            <li>Click Run reconciliation.</li>
            <li>Read the summary first, then check each group of differences below it.</li>
          </ol>
          <pre className="example">
            {'reference,amount,status\nPAY-7F3K9Q2M8XWD,1500.00,SUCCESSFUL\nPAY-B4D3CKQR6E6Z,250.50,FAILED'}
          </pre>
          <p className="hint">
            A header line and blank lines are ignored. Each reference may appear only once. If a line is wrong, the
            error message names the line number to fix.
          </p>
          <Link to="/reconciliation">Open Reconciliation</Link>
        </li>
      </ol>

      <section className="guide-section">
        <h3>Payment statuses</h3>
        <dl className="details">
          <dt>PENDING</dt>
          <dd>Created and waiting for the result from the provider.</dd>
          <dt>SUCCESSFUL</dt>
          <dd>The provider confirmed the payment. It cannot change again.</dd>
          <dt>FAILED</dt>
          <dd>The provider reported a failure. It cannot change again.</dd>
        </dl>
      </section>

      <section className="guide-section">
        <h3>Reconciliation results</h3>
        <dl className="details">
          <dt>Matched</dt>
          <dd>Same amount and status on both sides. Nothing to do.</dd>
          <dt>Amount mismatch</dt>
          <dd>The amounts differ. Check which amount was really charged.</dd>
          <dt>Status mismatch</dt>
          <dd>
            The statuses differ, often because a callback was missed. Confirm the outcome with the provider.
          </dd>
          <dt>Missing on our side</dt>
          <dd>The provider has a record that we do not. Find out where the payment came from.</dd>
          <dt>Missing on provider side</dt>
          <dd>We have a finished payment that the provider did not list. Ask the provider to confirm it.</dd>
          <dt>Pending payments</dt>
          <dd>Left out of the check, because they are still waiting for a result.</dd>
        </dl>
      </section>
    </div>
  )
}

export default Guide
