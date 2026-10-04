import { BrowserRouter, NavLink, Outlet, Route, Routes } from 'react-router'
import NewPayment from './pages/NewPayment.jsx'
import PaymentDetail from './pages/PaymentDetail.jsx'
import PaymentList from './pages/PaymentList.jsx'
import Reconciliation from './pages/Reconciliation.jsx'

function Layout() {
  return (
    <>
      <header className="topbar">
        <div className="container topbar-inner">
          <h1 className="brand">Payments</h1>
          <nav>
            <NavLink to="/" end>Payments</NavLink>
            <NavLink to="/payments/new">New payment</NavLink>
            <NavLink to="/reconciliation">Reconciliation</NavLink>
          </nav>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  )
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<PaymentList />} />
          <Route path="payments/new" element={<NewPayment />} />
          <Route path="payments/:reference" element={<PaymentDetail />} />
          <Route path="reconciliation" element={<Reconciliation />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
