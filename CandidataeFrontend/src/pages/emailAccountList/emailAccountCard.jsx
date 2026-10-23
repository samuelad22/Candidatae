function EmailAccountCard({ emailCuenta = "", proveedor = "", estadoConexion = "", fechaConexion = null}) {

    const fechaFormateada = fechaConexion
        ? new Date(fechaConexion).toLocaleDateString("es-ES")
        : "-"

    return (
        <div className="cardCuentaCorreo">
            <h3 className="tituloCardCuentaCorreo">{emailCuenta}</h3>
            <div className="cuerpoCardCuentaCorreo">
                <p className="proveedorCardCuentaCorreo">Proveedor: {proveedor}</p>
                <p className="estadoCardCuentaCorreo">Estado: {estadoConexion}</p>
            </div>
            <p className="fechaCardCuentaCorreo">Conectada: {fechaFormateada}</p>
        </div>
    )
}

export default EmailAccountCard