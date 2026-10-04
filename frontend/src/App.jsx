import { BrowserRouter, NavLink, Outlet, Route, Routes } from 'react-router'
import NewPayment from './pages/NewPayment.jsx'
import PaymentList from './pages/PaymentList.jsx'
import Reconciliation from './pages/Reconciliation.jsx'

function Layout() {
  return (
    <>
      <header>
        <h1>Payments</h1>
        <nav>
          <NavLink to="/" end>Payments</NavLink>
          <NavLink to="/payments/new">New payment</NavLink>
          <NavLink to="/reconciliation">Reconciliation</NavLink>
        </nav>
      </header>
      <main>
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
          <Route path="reconciliation" element={<Reconciliation />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
