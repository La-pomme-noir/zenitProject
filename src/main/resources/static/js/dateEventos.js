document.addEventListener('DOMContentLoaded', () => {
    let currentPage = 0;
    const pageSize = 9;

    // Fetch events from the API
    const fetchEvents = async (page) => {
        try {
            const response = await fetch(`/rest/organizadores/public/eventos?page=${page}&size=${pageSize}`);
            const data = await response.json();

            const eventosContainer = document.getElementById('eventos-container');
            eventosContainer.innerHTML = ''; // Clear existing cards

            data.eventos.forEach(evento => {
                const card = document.createElement('div');
                card.className = 'evento__card-unit';
                card.innerHTML = `
                    <img src="./img/eventos/eventos${(Math.floor(Math.random() * 3) + 1)}.jpg" alt="${evento.nombreEvento}">
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
                    <button class="global__button">Obtener Entradas</button>
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
        } catch (error) {
            console.error('Error fetching events:', error);
        }
    };

    // Initial fetch
    fetchEvents(currentPage);

    // Pagination event listeners
    document.getElementById('prev-page').addEventListener('click', () => {
        if (currentPage > 0) {
            currentPage--;
            fetchEvents(currentPage);
        }
    });

    document.getElementById('next-page').addEventListener('click', () => {
        currentPage++;
        fetchEvents(currentPage);
    });
});