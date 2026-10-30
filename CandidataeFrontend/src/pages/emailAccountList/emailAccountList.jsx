import { useEffect, useState } from "react"
import { listarCuentasCorreo, desconectarCuentaCorreo } from "../../services/emailAccountService"
import EmailAccountCard from "./emailAccountCard"

function EmailAccountList({ reloadKey, onChanged }) {
    const [accounts, setAccounts] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    // Error de una acción (quitar), distinto del error de carga de la lista
    const [actionError, setActionError] = useState(null)

    useEffect(() => {
        let cancelado = false

        listarCuentasCorreo()
            .then((data) => {
                if (!cancelado) {
                    setAccounts(Array.isArray(data) ? data : [])
                    setError(null)
                }
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
    }, [reloadKey])

    const handleRemove = async (id) => {
        setActionError(null)
        try {
            await desconectarCuentaCorreo(id)
            onChanged()
        } catch {
            setActionError("No se pudo quitar la cuenta. Inténtalo de nuevo.")
        }
    }

    if (loading) return <p>Cargando cuentas de correo...</p>
    if (error) return <p role="alert">No se pudieron cargar las cuentas de correo.</p>
    if (accounts.length === 0) return <p>No tienes ninguna cuenta de correo vinculada todavía.</p>

    return (
        <section>
            {actionError && <p role="alert">{actionError}</p>}
            {accounts.map(({ id, emailCuenta, proveedor, estadoConexion, fechaConexion }) => (
                <EmailAccountCard
                    key={id}
                    id={id}
                    emailCuenta={emailCuenta}
                    proveedor={proveedor}
                    estadoConexion={estadoConexion}
                    fechaConexion={fechaConexion}
                    onRemove={handleRemove}
                />
            ))}
        </section>
    )
}

export default EmailAccountList