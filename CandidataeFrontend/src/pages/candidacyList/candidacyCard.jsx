function getBadgeClass(estado) {
    switch (estado) {
        case 'ENTREVISTA':
            return 'bg-warning text-dark'
        case 'OFERTA':
            return 'bg-success text-white'
        case 'EN_PROCESO':
            return 'bg-info text-dark'
        case 'RECHAZADA':
        case 'DESCARTADA':
            return 'bg-danger text-white'
        case 'ENVIADA':
        default:
            return 'bg-primary text-white'
    }
}

function CandidacyCard({ puesto = '', empresa = '', estado = '', fecha = '' }) {
    const badgeClass = getBadgeClass(estado)

    return (
        <div className="card card-custom mb-3 border-0 shadow-sm p-3">
            <div className="card-body p-0">
                <div className="d-flex justify-content-between align-items-start mb-2">
                    <h5 className="card-title fw-bold mb-0 text-truncate" title={puesto}>
                        {puesto || 'Puesto no especificado'}
                    </h5>
                    <span className={`badge ${badgeClass} text-uppercase px-2 py-1`}>
                        {estado || 'ENVIADA'}
                    </span>
                </div>
                <div className="text-muted small mb-2">
                    <i className="fa fa-building me-1"></i>
                    <strong>{empresa}</strong>
                </div>
                {fecha && (
                    <div className="text-secondary small">
                        <i className="fa fa-calendar me-1"></i>
                        <span>{fecha}</span>
                    </div>
                )}
            </div>
        </div>
    )
}

export default CandidacyCard
