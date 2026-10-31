import { Routes, Route } from 'react-router-dom'
import Home from '../pages/Home'
import Login from '../pages/Login'
import Register from '../pages/Register'
import TerminosPage from '../pages/TerminosPage'

export default function PublicRoutes() {
    return (
        <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/terminos" element={<TerminosPage />} />
            <Route path="/terminos-condiciones" element={<TerminosPage />} />
        </Routes>
    )
}
