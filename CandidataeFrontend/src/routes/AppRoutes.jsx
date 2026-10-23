import { Routes, Route } from 'react-router-dom'

import PublicRoutes from './PublicRoutes'
import PrivateRoutes from './PrivateRoutes'

export default function AppRoutes() {
    return (
        <Routes>
            <Route path="/*" element={<PublicRoutes />} />
            <Route path="/panel/*" element={<PrivateRoutes />} />
        </Routes>
    )
}