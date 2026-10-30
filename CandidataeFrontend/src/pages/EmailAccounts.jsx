import { useState } from "react"
import EmailAccountList from "./emailAccountList/emailAccountList"

function EmailAccounts() {
    // Contador que empieza en 0. Su trabajo es cambiar para avisar a la lista de que vuelva a pedir los datos
    const [reloadKey, setReloadKey] = useState(0)

    // Suma 1 al contador.
    const reload = () => setReloadKey((current) => current + 1)

    return (
        <main className="paginaCuentasCorreo">
            <h1>Cuentas de correo</h1>
            <p>Vincula las cuentas desde las que recibes las confirmaciones de tus inscripciones.</p>

            {/* El formulario irá aquí*/}

            <EmailAccountList reloadKey={reloadKey} onChanged={reload} />
        </main>
    )
}

export default EmailAccounts