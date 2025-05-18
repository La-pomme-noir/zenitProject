document.addEventListener('DOMContentLoaded', () => {
    let currentPage = 0;
    const pageSize = 9;
    let searchQuery = '';

    // Fetch events from the API
    const fetchEvents = async (page, search = '') => {
        try {
            const url = `/rest/organizadores/public/eventos?page=${page}&size=${pageSize}${search ? `&search=${encodeURIComponent(search)}` : ''}`;
            console.log('Fetching events from:', url); // Debug
            const response = await fetch(url);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            console.log('API response:', data); // Debug

            const eventosContainer = document.getElementById('eventos-container');
            eventosContainer.innerHTML = ''; // Clear existing cards

            if (data.eventos.length === 0) {
                eventosContainer.innerHTML = '<p>No se encontraron eventos.</p>';
                return;
            }

            data.eventos.forEach(evento => {
                const card = document.createElement('div');
                card.className = 'evento__card-unit';
                card.innerHTML = `
                    <img src="${evento.imagenUrl || './uploads/default.jpg'}" alt="${evento.nombreEvento}">
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
                    <button class="global__button" data-bs-toggle="modal" data-bs-target="#preciosModal" data-evento='${JSON.stringify(evento)}'>Obtener Entradas</button>
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

            // Add event listeners for "Obtener Entradas" buttons
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
                    // Add event listeners for "Comprar" buttons (placeholder for PayPal integration)
                    document.querySelectorAll('.comprar-entrada').forEach(btn => {
                        btn.addEventListener('click', () => {
                            const ubicacion = btn.getAttribute('data-ubicacion');
                            const precio = btn.getAttribute('data-precio');
                            alert(`Iniciando compra para ${ubicacion} a $${precio}. (Integración con PayPal pendiente)`);
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
        currentPage = 0; // Reset to first page on search
        fetchEvents(currentPage, searchQuery);
    });
});