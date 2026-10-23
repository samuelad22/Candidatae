import { Outlet, Navigate } from 'react-router-dom'
import Navbar from '../components/layout/app/Navbar'

export default function AppLayout() {
    const token = localStorage.getItem('token')

    if (!token) {
        return <Navigate to="/login" replace />
    }

    return (
        <div className="min-vh-100 d-flex flex-column bg-main text-body">
            <Navbar />

            <main className="container py-4 flex-grow-1">
                <Outlet />
            </main>
        </div>
    )
}