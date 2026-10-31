import { Navigate, Routes, Route } from 'react-router-dom'
import CandidacyList from '../pages/candidacyList/candidacyList'

export default function PrivateRoutes() {
    const isAuthenticated = Boolean(localStorage.getItem('token'))

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />
    }

    return (
        <Routes>
            <Route index element={<CandidacyList />} />
            <Route path="candidaturas" element={<CandidacyList />} />
            <Route path="cuentas-correo" element={<CandidacyList />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
    )
}
