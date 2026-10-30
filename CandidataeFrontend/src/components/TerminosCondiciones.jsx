import { useState } from 'react';

import { TERMINOS_CONDICIONES } from '../constants/terminosCondiciones';
import '../styles/terminosCondiciones.css';

function TerminosCondiciones({ onAceptar, onDenegar }) {
    const [visible, setVisible] = useState(
        () => localStorage.getItem('terminosCondiciones') === null,
    );

    const guardarDecision = (decision, callback) => {
        localStorage.setItem('terminosCondiciones', decision);
        setVisible(false);

        if (callback) {
            callback();
        }
    };

    const aceptarTerminos = () => {
        guardarDecision('aceptado', onAceptar);
    };

    const denegarTerminos = () => {
        guardarDecision('denegado', onDenegar);
    };

    if (!visible) {
        return null;
    }

    return (
        <div className="terminos-condiciones" role="presentation">
            <section
                className="terminos-condiciones__dialogo"
                role="dialog"
                aria-modal="true"
                aria-labelledby="terminos-condiciones-titulo"
            >
                <header className="terminos-condiciones__encabezado">
                    <p className="terminos-condiciones__etiqueta">Información legal</p>
                    <h1 id="terminos-condiciones-titulo">Términos y condiciones</h1>
                    <p className="terminos-condiciones__introduccion">
                        Lea atentamente las condiciones de uso antes de continuar.
                    </p>
                </header>

                <div className="terminos-condiciones__contenido">
                    {TERMINOS_CONDICIONES.map(({ titulo, parrafos }) => (
                        <section className="terminos-condiciones__seccion" key={titulo}>
                            <h2>{titulo}</h2>
                            {parrafos.map((parrafo) => (
                                <p key={parrafo}>{parrafo}</p>
                            ))}
                        </section>
                    ))}
                </div>

                <footer className="terminos-condiciones__acciones">
                    <button
                        className="terminos-condiciones__boton terminos-condiciones__boton--secundario"
                        type="button"
                        onClick={denegarTerminos}
                    >
                        Denegar
                    </button>
                    <button
                        className="terminos-condiciones__boton terminos-condiciones__boton--primario"
                        type="button"
                        onClick={aceptarTerminos}
                    >
                        Aceptar
                    </button>
                </footer>
            </section>
        </div>
    );
}

export default TerminosCondiciones;
