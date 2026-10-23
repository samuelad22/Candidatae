import { Outlet } from 'react-router-dom'
import Navbar from '../components/layout/public/Navbar'

export default function PublicLayout() {
    return (
        <div className="min-vh-100 d-flex flex-column bg-main text-body">
            <Navbar />

            <main className="flex-grow-1 d-flex flex-column">
                <Outlet />
            </main>
        </div>
    )
}