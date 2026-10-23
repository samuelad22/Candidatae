import { useEffect, useState } from "react"
import { listarCuentasCorreo } from "../../services/emailAccountService"
import EmailAccountCard from "./emailAccountCard"

function EmailAccountList() {
    const [accounts, setAccounts] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        let cancelado = false

        listarCuentasCorreo()
            .then((data) => {
                if (!cancelado) setAccounts(Array.isArray(data) ? data : [])
            })
            .catch((requestError) => {
                if (!cancelado) setError(requestError)
            })
            .finally(() => {
                if (!cancelado) setLoading(false)
            })

        return () => {
            cancelado = true
        }
    }, [])

    if (loading) return <p>Cargando cuentas de correo...</p>
    if (error) return <p role="alert">No se pudieron cargar las cuentas de correo.</p>
    if (accounts.length === 0) return <p>No tienes ninguna cuenta de correo vinculada todavía.</p>

    return (
        <section>
            {accounts.map(({ id, emailCuenta, proveedor, estadoConexion, fechaConexion }) => (
                <EmailAccountCard
                    key={id} // obligatoria para que React distinga unas cards de otras
                    emailCuenta={emailCuenta}
                    proveedor={proveedor}
                    estadoConexion={estadoConexion}
                    fechaConexion={fechaConexion}
                />
            ))}
        </section>
    )
}

export default EmailAccountList