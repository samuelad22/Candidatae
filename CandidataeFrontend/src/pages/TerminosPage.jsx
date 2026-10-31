import { useState } from 'react';
import TerminosCondiciones from '../components/TerminosCondiciones';
import { updateTermsAcceptance } from '../services/termsService';
import '../styles/terminosCondiciones.css';

export default function TerminosPage() {
    const [status, setStatus] = useState(() => localStorage.getItem('terminosCondiciones') || 'sin_responder');
    const [dbSyncStatus, setDbSyncStatus] = useState(null);
    const [key, setKey] = useState(0);

    const handleAceptar = async () => {
        setStatus('aceptado');
        const token = localStorage.getItem('token');
        if (token) {
            try {
                await updateTermsAcceptance(true, '1.0');
                setDbSyncStatus('Registrado con éxito en PostgreSQL (RGPD Art. 5.2)');
            } catch (err) {
                setDbSyncStatus('Guardado en local (inicia sesión para registrar en BD)');
            }
        } else {
            setDbSyncStatus('Guardado en LocalStorage (sin sesión activa)');
        }
    };

    const handleDenegar = async () => {
        setStatus('denegado');
        const token = localStorage.getItem('token');
        if (token) {
            try {
                await updateTermsAcceptance(false, null);
                setDbSyncStatus('Revocado en PostgreSQL');
            } catch (err) {
                setDbSyncStatus('Guardado en local');
            }
        } else {
            setDbSyncStatus('Guardado en LocalStorage');
        }
    };

    const handleReset = () => {
        localStorage.removeItem('terminosCondiciones');
        setStatus('sin_responder (1ª visita)');
        setDbSyncStatus(null);
        setKey(prev => prev + 1);
    };

    const handleForzarApertura = () => {
        localStorage.removeItem('terminosCondiciones');
        setStatus('abierto (pendiente)');
        setDbSyncStatus(null);
        setKey(prev => prev + 1);
    };

    return (
        <div style={{ minHeight: '100vh', padding: '40px 20px', backgroundColor: '#0F0F0F', color: '#F8F7F0', fontFamily: 'Inter, sans-serif' }}>
            <div style={{ maxWidth: '800px', margin: '0 auto', background: '#182126', border: '1px solid #303B40', borderRadius: '14px', padding: '28px', boxShadow: '0 12px 36px rgba(0,0,0,0.4)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                    <span style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 700, color: '#D9FF6B' }}>
                        Simulador y Conexión Backend
                    </span>
                </div>
                <h1 style={{ color: '#F8F7F0', margin: '0 0 10px 0', fontSize: '24px', fontWeight: 700 }}>
                    Términos y Condiciones (RGPD / LOPDGDD)
                </h1>
                <p style={{ color: '#C1C8CC', marginBottom: '24px', fontSize: '14px', lineHeight: 1.6 }}>
                    Esta vista previa permite probar el modal y la persistencia auditada tanto en el cliente como en el backend (<code>POST /api/users/terms</code>).
                </p>

                <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginBottom: '24px' }}>
                    <button 
                        type="button" 
                        onClick={handleForzarApertura}
                        style={{ padding: '10px 20px', backgroundColor: '#D9FF6B', color: '#0F0F0F', border: 'none', borderRadius: '6px', fontWeight: 700, cursor: 'pointer', fontSize: '14px' }}
                    >
                        Abrir modal
                    </button>
                    <button 
                        type="button" 
                        onClick={handleReset}
                        style={{ padding: '10px 20px', backgroundColor: 'transparent', color: '#F8F7F0', border: '1px solid #303B40', borderRadius: '6px', cursor: 'pointer', fontSize: '14px', fontWeight: 600 }}
                    >
                        Resetear decisión
                    </button>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    <div style={{ padding: '14px 18px', background: '#11191D', border: '1px solid #303B40', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <span style={{ color: '#8F9BA2', fontSize: '13.5px' }}>Estado en LocalStorage:</span>
                        <strong style={{ 
                            color: status === 'aceptado' ? '#42C98A' : status === 'denegado' ? '#EF4444' : '#FFBB38',
                            fontSize: '14px',
                            fontFamily: 'monospace'
                        }}>
                            {status}
                        </strong>
                    </div>

                    {dbSyncStatus && (
                        <div style={{ padding: '12px 18px', background: 'rgba(217, 255, 107, 0.08)', border: '1px solid rgba(217, 255, 107, 0.3)', borderRadius: '8px', fontSize: '13px', color: '#D9FF6B' }}>
                            <strong>Registro Backend: </strong> {dbSyncStatus}
                        </div>
                    )}
                </div>
            </div>

            <TerminosCondiciones 
                key={key} 
                onAceptar={handleAceptar} 
                onDenegar={handleDenegar} 
            />
        </div>
    );
}
