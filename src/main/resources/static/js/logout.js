document.addEventListener('DOMContentLoaded', () => {
    // Lógica de cierre de sesión manual
    document.getElementById('logout-link').addEventListener('click', async () => {
        try {
            const response = await fetch('/logout', {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            if (response.ok) {
                window.location.href = '/index.html';
            } else {
                console.error('Error al cerrar sesión:', await response.text());
                alert('Error al cerrar sesión. Por favor, intenta de nuevo.');
            }
        } catch (error) {
            console.error('Error en la solicitud de logout:', error);
            alert('Error al cerrar sesión. Por favor, intenta de nuevo.');
        }
    });

});

/*// Cierre automático al cerrar la ventana
    window.addEventListener('beforeunload', async () => {
        try {
            await fetch('/logout', {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
        } catch (error) {
            console.error('Error al cerrar sesión automáticamente:', error);
        }
    });

    // Cierre automático por inactividad (15 minutos)
    let inactivityTime = 15 * 60 * 1000; // 15 minutos en milisegundos
    let inactivityTimer;

    const resetInactivityTimer = () => {
        clearTimeout(inactivityTimer);
        inactivityTimer = setTimeout(async () => {
            try {
                const response = await fetch('/logout', {
                    method: 'POST',
                    credentials: 'include',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                });
                if (response.ok) {
                    alert('Sesión cerrada por inactividad.');
                    window.location.href = '/index.html';
                }
            } catch (error) {
                console.error('Error al cerrar sesión por inactividad:', error);
            }
        }, inactivityTime);
    };

    // Reiniciar el temporizador con cualquier interacción del usuario
    document.addEventListener('mousemove', resetInactivityTimer);
    document.addEventListener('keypress', resetInactivityTimer);
    document.addEventListener('scroll', resetInactivityTimer);
    document.addEventListener('click', resetInactivityTimer);

    // Iniciar el temporizador al cargar la página
    resetInactivityTimer(); */