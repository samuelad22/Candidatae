import { Navigate, Routes, Route } from 'react-router-dom'

// import Dashboard from '../pages/Candidacies'

export default function PrivateRoutes() {
    const isAuthenticated = Boolean(localStorage.getItem('token'))

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />
    }

    return (
        <Routes>
            {/* Ejemplo: */}
            {/* <Route path="/candidaturas" element={<Candidacies />} /> */}
        </Routes>
    )
}