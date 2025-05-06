const track = document.querySelector('.carousel__track');
const slides = document.querySelectorAll('.carousel__slide');
const nextBtn = document.querySelector('.carousel__btn--next');
const prevBtn = document.querySelector('.carousel__btn--prev');
let currentIndex = 0;
let autoSlideInterval;

function updateCarousel() {
    track.style.transform = `translateX(-${currentIndex * 100}%)`;
}

function startAutoSlide() {
    autoSlideInterval = setInterval(() => {
        currentIndex = (currentIndex + 1) % slides.length;
        updateCarousel();
    }, 5000); // Cambia cada 5 segundos
}

function stopAutoSlide() {
    clearInterval(autoSlideInterval);
}

nextBtn.addEventListener('click', () => {
    stopAutoSlide();
    currentIndex = (currentIndex + 1) % slides.length;
    updateCarousel();
    startAutoSlide();
});

prevBtn.addEventListener('click', () => {
    stopAutoSlide();
    currentIndex = (currentIndex - 1 + slides.length) % slides.length;
    updateCarousel();
    startAutoSlide();
});

// Pausar el carrusel al pasar el ratón
document.querySelector('.carousel').addEventListener('mouseenter', stopAutoSlide);
document.querySelector('.carousel').addEventListener('mouseleave', startAutoSlide);

// Iniciar el carrusel
updateCarousel();
startAutoSlide();