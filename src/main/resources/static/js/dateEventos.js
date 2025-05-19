document.addEventListener('DOMContentLoaded', () => {
    let currentPage = 0;
    const pageSize = 9;
    let searchQuery = '';

    // Función para mostrar una alerta estilizada
    function showAuthAlert() {
        // Crear el contenedor de la alerta
        const alertContainer = document.createElement('div');
        alertContainer.className = 'auth-alert';
        alertContainer.innerHTML = `
            <div class="auth-alert__content">
                <div class="auth-alert__icon">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                </div>
                <h3 class="auth-alert__title">Debes Iniciar Sesión</h3>
                <p class="auth-alert__message">Para comprar una entrada, necesitas estar registrado como cliente. Por favor, inicia sesión o regístrate.</p>
                <div class="auth-alert__buttons">
                    <a href="/login.html" class="auth-alert__button auth-alert__button--login">Iniciar Sesión</a>
                    <a href="/register.html" class="auth-alert__button auth-alert__button--register">Registrarse</a>
                </div>
            </div>
        `;

        // Añadir la alerta al cuerpo
        document.body.appendChild(alertContainer);

        // Cerrar la alerta al hacer clic fuera de ella
        alertContainer.addEventListener('click', (e) => {
            if (e.target === alertContainer) {
                document.body.removeChild(alertContainer);
            }
        });
    }

    // Fetch events from the API
    const fetchEvents = async (page, search = '') => {
        try {
            const url = `/rest/organizadores/public/eventos?page=${page}&size=${pageSize}${search ? `&search=${encodeURIComponent(search)}` : ''}`;
            console.log('Fetching events from:', url);
            const response = await fetch(url);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            console.log('API response:', data);

            const eventosContainer = document.getElementById('eventos-container');
            eventosContainer.innerHTML = '';

            if (data.eventos.length === 0) {
                eventosContainer.innerHTML = '<p>No se encontraron eventos.</p>';
                return;
            }

            data.eventos.forEach(evento => {
                const card = document.createElement('div');
                card.className = 'evento__card-unit';
                // Determinar si el aforo es 0 para mostrar "No disponible" o el botón "Obtener Entradas"
                const isAforoAgotado = evento.aforo <= 0;
                card.innerHTML = `
                    <img src="${evento.imagenUrl || '/img/eventos/default.jpg'}" alt="${evento.nombreEvento}">
                    <h3 class="evento__card-title">${evento.nombreEvento}</h3>
                    <p class="evento__card-parrafo">${evento.lugar}, ${evento.ciudad}</p>
                    <p class="evento__card-aforo"><strong>Aforo:</strong> ${evento.aforo}</p>
                    <span class="evento__card-more">Leer más</span>
                    <div class="evento__overlay">
                        <i class="fas fa-times overlay__close"></i>
                        <span class="evento__overlay-span">${evento.fecha}, ${evento.hora}</span>
                        <p><strong>Descripción:</strong> ${evento.descripcion}</p>
                        <p><strong>Requisitos:</strong> ${evento.requisitos}</p>
                        <p><strong>Organizador:</strong> ${evento.organizadorNombre}</p>
                    </div>
                    ${isAforoAgotado 
                        ? '<p class="evento__no-disponible">No disponible</p>' 
                        : `<button class="global__button" data-bs-toggle="modal" data-bs-target="#preciosModal" data-evento='${JSON.stringify(evento)}'>Obtener Entradas</button>`
                    }
                `;
                eventosContainer.appendChild(card);
            });

            // Update pagination
            const prevButton = document.getElementById('prev-page');
            const nextButton = document.getElementById('next-page');
            const pageInfo = document.getElementById('page-info');

            pageInfo.textContent = `Página ${data.currentPage + 1} de ${data.totalPages}`;
            prevButton.disabled = data.currentPage === 0;
            nextButton.disabled = data.currentPage >= data.totalPages - 1;

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

            // Add event listeners for "Obtener Entradas" buttons (solo para los eventos con aforo disponible)
            document.querySelectorAll('.global__button').forEach(button => {
                button.addEventListener('click', () => {
                    const evento = JSON.parse(button.getAttribute('data-evento'));
                    const modalBody = document.getElementById('preciosModalBody');
                    const modalTitle = document.getElementById('preciosModalLabel');
                    modalTitle.textContent = `Entradas para ${evento.nombreEvento}`;
                    modalBody.innerHTML = `
                        <h6 class="title-location">Ubicaciones y Precios</h6>
                        <ul>
                            ${evento.ubicacionesPrecios && evento.ubicacionesPrecios.length > 0
                                ? evento.ubicacionesPrecios.map(ubicacion => `
                                    <li>
                                        <strong>${ubicacion.ubicacion}</strong>: $${ubicacion.precio}
                                        <button class="btn-comprar--entrada btn btn-primary btn-lg ms-2 comprar-entrada" data-ubicacion="${ubicacion.ubicacion}" data-precio="${ubicacion.precio}">Comprar</button>
                                    </li>
                                `).join('')
                                : '<p>No hay ubicaciones disponibles.</p>'
                            }
                        </ul>
                    `;
                    // Add event listeners for "Comprar" buttons
                    document.querySelectorAll('.comprar-entrada').forEach(btn => {
                        btn.addEventListener('click', async () => {
                            // Verificar autenticación
                            try {
                                const authResponse = await fetch('/api/clientes/auth-status', {
                                    credentials: 'include'
                                });
                                const authData = await authResponse.json();

                                if (!authData.isAuthenticated || authData.role !== 'CLIENTE') {
                                    showAuthAlert();
                                    return;
                                }

                                // Si está autenticado y es cliente, proceder a la compra
                                const ubicacion = btn.getAttribute('data-ubicacion');
                                const precio = btn.getAttribute('data-precio');
                                window.location.href = `/paymentForm.html?ubicacion=${encodeURIComponent(ubicacion)}&precio=${encodeURIComponent(precio)}&eventoId=${evento.id}`;
                            } catch (error) {
                                console.error('Error al verificar autenticación:', error);
                                showAuthAlert();
                            }
                        });
                    });
                });
            });
        } catch (error) {
            console.error('Error fetching events:', error);
            const eventosContainer = document.getElementById('eventos-container');
            eventosContainer.innerHTML = '<p>Error al cargar los eventos. Por favor, intenta de nuevo.</p>';
        }
    };

    // Initial fetch
    fetchEvents(currentPage);

    // Pagination event listeners
    document.getElementById('prev-page').addEventListener('click', () => {
        if (currentPage > 0) {
            currentPage--;
            fetchEvents(currentPage, searchQuery);
        }
    });

    document.getElementById('next-page').addEventListener('click', () => {
        currentPage++;
        fetchEvents(currentPage, searchQuery);
    });

    // Search event listener
    const searchInput = document.getElementById('search-input');
    searchInput.addEventListener('input', () => {
        searchQuery = searchInput.value.trim();
        currentPage = 0;
        fetchEvents(currentPage, searchQuery);
    });
});