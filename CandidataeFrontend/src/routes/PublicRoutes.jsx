import { Routes, Route } from 'react-router-dom'
import PublicLayout from '../layouts/PublicLayout'
import LandingPage from '../pages/public/LandingPage'
import LoginPage from '../pages/public/LoginPage'
import RegisterPage from '../pages/public/RegisterPage'
import GuidePage from '../pages/public/GuidePage'
import ContributorsPage from '../pages/public/ContributorsPage'

export default function PublicRoutes() {
    return (
        <Routes>
            <Route element={<PublicLayout />}>
                <Route path="/" element={<LandingPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/registro" element={<RegisterPage />} />
                <Route path="/guia" element={<GuidePage />} />
                <Route path="/colaboradores" element={<ContributorsPage />} />
            </Route>
        </Routes>
    )
}