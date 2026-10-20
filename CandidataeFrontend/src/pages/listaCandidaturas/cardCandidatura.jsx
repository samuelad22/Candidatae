function cardCandidatura({candidatura}){

    const {puesto = "", empresa = "", estado = "", fecha = ""} = candidatura
    
    return(
        <div id="cardCandidatura">
            <h3 className="tituloCardCandidatura">{puesto}</h3>
            <div className="cuerpoCardCandidatura">
                <p className="empresaCardCandidatura">Empresa: {empresa}</p>
                <p className="estadoCardCandidatura">Estado: {estado}</p>
            </div>
            <p className="fechaCardCandidatura">Fecha: {fecha}</p>
        </div>
    )
}