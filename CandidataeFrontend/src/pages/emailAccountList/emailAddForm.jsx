import { autorizarGmailOAuth } from "../../services/emailAccountService"

function emailAddForm (){

    const iniciarSesion = async () => {
        const { authorizationUrl } = await autorizarGmailOAuth()
        window.location.assign(authorizationUrl)
    }

    return (
        <button type="button" className="btn-primary emailAddButton" onClick={iniciarSesion}>
            <i className="fa-solid fa-plus" aria-hidden="true"></i>
            Vincular correo
        </button>
    )
}

export default emailAddForm