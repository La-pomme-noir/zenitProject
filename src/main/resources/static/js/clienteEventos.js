document.addEventListener('DOMContentLoaded', () => {
    // Función para mostrar una alerta de confirmación
    function showCancelConfirmation(eventoId, buttonElement) {
        // Crear el contenedor de la alerta
        const alertContainer = document.createElement('div');
        alertContainer.className = 'cancel-alert';
        alertContainer.innerHTML = `
            <div class="cancel-alert__content">
                <div class="cancel-alert__icon">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                </div>
                <h3 class="cancel-alert__title">¿Cancelar Asistencia?</h3>
                <p class="cancel-alert__message">¿Estás seguro de que deseas cancelar tu asistencia a este evento? Esta acción no se puede deshacer.</p>
                <div class="cancel-alert__buttons">
                    <button class="cancel-alert__button cancel-alert__button--confirm">Sí, Cancelar</button>
                    <button class="cancel-alert__button cancel-alert__button--cancel">No</button>
                </div>
            </div>
        `;
        // Añadir la alerta al cuerpo
        document.body.appendChild(alertContainer);

        // Manejar los botones
        const confirmButton = alertContainer.querySelector('.cancel-alert__button--confirm');
        const cancelButton = alertContainer.querySelector('.cancel-alert__button--cancel');

        confirmButton.addEventListener('click', async () => {
            try {
                const response = await fetch(`/api/clientes/cancel-event/${eventoId}`, {
                    method: 'DELETE',
                    credentials: 'include'
                });
                const data = await response.json();
                if (!response.ok) {
                    throw new Error(data.message || 'Error al cancelar la asistencia.');
                }
                alert('Asistencia cancelada con éxito.');
                // Remover la tarjeta del evento
                buttonElement.closest('.evento__card-unit').remove();
            } catch (error) {
                console.error('Error al cancelar:', error);
                alert('Error: ' + error.message);
            }
            document.body.removeChild(alertContainer);
        });

        cancelButton.addEventListener('click', () => {
            document.body.removeChild(alertContainer);
        });

        // Cerrar al hacer clic fuera
        alertContainer.addEventListener('click', (e) => {
            if (e.target === alertContainer) {
                document.body.removeChild(alertContainer);
            }
        });
    }

    // Fetch eventos registrados del cliente
    const fetchRegisteredEvents = async () => {
        try {
            const response = await fetch('/api/clientes/registered-events', {
                credentials: 'include'
            });
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const eventos = await response.json();
            console.log('Eventos registrados:', eventos);

            const eventosContainer = document.getElementById('eventos-container');
            eventosContainer.innerHTML = '';

            if (eventos.length === 0) {
                eventosContainer.innerHTML = '<p>No estás registrado en ningún evento.</p>';
                return;
            }

            eventos.forEach(evento => {
                const card = document.createElement('div');
                card.className = 'evento__card-unit';
                card.innerHTML = `
                    <img src="${evento.imagenUrl || '/img/eventos/default.jpg'}" alt="${evento.nombreEvento}">
                    <h3 class="evento__card-title">${evento.nombreEvento}</h3>
                    <p class="evento__card-parrafo">${evento.lugar}, ${evento.ciudad}</p>
                    <span class="evento__card-more">Leer más</span>
                    <div class="evento__overlay">
                        <i class="fas fa-times overlay__close"></i>
                        <span class="evento__overlay-span">${evento.fecha}, ${evento.hora}</span>
                        <p><strong>Descripción:</strong> ${evento.descripcion}</p>
                        <p><strong>Requisitos:</strong> ${evento.requisitos}</p>
                        <p><strong>Organizador:</strong> ${evento.organizadorNombre}</p>
                    </div>
                    <button class="global__button button__evento cancelar-asistencia" data-evento-id="${evento.id}">Cancelar Asistencia</button>
                `;
                eventosContainer.appendChild(card);
            });

            // Add event listeners for "Leer más" and close buttons
            document.querySelectorAll('.evento__card-more').forEach(button => {
                button.addEventListener('click', () => {
                    const overlay = button.nextElementSibling;
                    overlay.classList.add('active');
                });
            });

            document.querySelectorAll('.overlay__close').forEach(button => {
                button.addEventListener('click', () => {
                    button.parentElement.classList.remove('active');
                });
            });

            // Add event listeners for "Cancelar Asistencia" buttons
            document.querySelectorAll('.cancelar-asistencia').forEach(button => {
                button.addEventListener('click', () => {
                    const eventoId = button.getAttribute('data-evento-id');
                    showCancelConfirmation(eventoId, button);
                });
            });
        } catch (error) {
            console.error('Error fetching registered events:', error);
            const eventosContainer = document.getElementById('eventos-container');
            eventosContainer.innerHTML = '<p>Error al cargar los eventos. Por favor, intenta de nuevo.</p>';
        }
    };

    // Cargar los eventos al iniciar
    fetchRegisteredEvents();
});