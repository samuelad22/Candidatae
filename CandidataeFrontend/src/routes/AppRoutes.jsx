import { Routes, Route } from 'react-router-dom'

import PublicRoutes from './PublicRoutes'
import PrivateRoutes from './PrivateRoutes'

export default function AppRoutes() {
    return (
        <Routes>
            <Route path="/dashboard/*" element={<PrivateRoutes />} />
            <Route path="/candidaturas/*" element={<PrivateRoutes />} />
            <Route path="/cuentas-correo/*" element={<PrivateRoutes />} />
            <Route path="/*" element={<PublicRoutes />} />
        </Routes>
    )
}
