import { Navigate, Routes, Route } from 'react-router-dom'
import AppLayout from '../layouts/AppLayout'
import HomePage from '../pages/app/HomePage'
import OffersPage from '../pages/app/OffersPage'
import ProfilePage from '../pages/app/ProfilePage'
import ApplicationsPage from '../pages/app/ApplicationsPage'

export default function PrivateRoutes() {
    const isAuthenticated = Boolean(localStorage.getItem('token'))

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />
    }

    return (
        <Routes>
            <Route element={<AppLayout />}>
                <Route path="/" element={<HomePage />} />

                <Route path="/candidaturas" element={<ApplicationsPage />} />
                <Route path="/ofertas" element={<OffersPage />} />
                <Route path="/perfil" element={<ProfilePage />} />
            </Route>
        </Routes>
    )
}