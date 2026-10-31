import EmailAccountList from "./emailAccountList"
import EmailAddForm from "./emailAddForm"

function EmailAccountPage() {
    return (
        <main className="emailAccountPage">
            <header className="emailAccountPageHeader">
                <div>
                    <h1 className="emailAccountPageTitle">Cuentas de correo</h1>
                    <p className="emailAccountPageDescription">
                        Gestiona las cuentas de correo vinculadas a tu perfil.
                    </p>
                </div>
                <EmailAddForm />
            </header>

            <EmailAccountList />
        </main>
    )
}

export default EmailAccountPage