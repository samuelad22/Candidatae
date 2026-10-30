import { useState } from "react"

function EmailAccountCard({ id, emailCuenta = "", proveedor = "", estadoConexion = "", fechaConexion = null, onRemove }) {
    // confirming: el usuario ha pulsado "Quitar" y falta que confirme
    const [confirming, setConfirming] = useState(false)
    // removing: ya confirmó y estamos esperando la respuesta del backend
    const [removing, setRemoving] = useState(false)

    const fechaFormateada = fechaConexion
        ? new Date(fechaConexion).toLocaleDateString("es-ES")
        : "-"

    // El backend no borra la cuenta: la deja como DESCONECTADA. Si ya lo está, no tiene sentido quitarla otra vez
    const isDisconnected = estadoConexion === "DESCONECTADA"

    const handleConfirm = async () => {
        setRemoving(true)
        try {
            await onRemove(id)
        } finally {
            setRemoving(false)
            setConfirming(false)
        }
    }

    return (
        <div className="cardCuentaCorreo">
            <h3 className="tituloCardCuentaCorreo">{emailCuenta}</h3>
            <div className="cuerpoCardCuentaCorreo">
                <p className="proveedorCardCuentaCorreo">Proveedor: {proveedor}</p>
                <p className="estadoCardCuentaCorreo">Estado: {estadoConexion}</p>
            </div>
            <p className="fechaCardCuentaCorreo">Conectada: {fechaFormateada}</p>

            {!isDisconnected && (
                confirming ? (
                    <div className="accionesCardCuentaCorreo">
                        <button type="button" onClick={handleConfirm} disabled={removing}>
                            {removing ? "Quitando..." : "Sí, quitar"}
                        </button>
                        <button type="button" onClick={() => setConfirming(false)} disabled={removing}>
                            Cancelar
                        </button>
                    </div>
                ) : (
                    <button type="button" onClick={() => setConfirming(true)}>
                        Quitar cuenta
                    </button>
                )
            )}
        </div>
    )
}

export default EmailAccountCard