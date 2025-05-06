const testimonialsCarousel = document.querySelector('.testimonials__carousel');
const testimonials = document.querySelectorAll('.testimonial');
const nextBtn = document.querySelector('.testimonials__btn--next');
const prevBtn = document.querySelector('.testimonials__btn--prev');

let currentIndex = 0;

function updateTestimonials() {
  testimonialsCarousel.style.transform = `translateX(-${currentIndex * 100}%)`;
}

nextBtn.addEventListener('click', () => {
  currentIndex = (currentIndex + 1) % testimonials.length;
  updateTestimonials();
});

prevBtn.addEventListener('click', () => {
  currentIndex = (currentIndex - 1 + testimonials.length) % testimonials.length;
  updateTestimonials();
});

updateTestimonials();
