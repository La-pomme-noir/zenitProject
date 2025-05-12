document.querySelectorAll('.evento__card-more').forEach(button => {
    button.addEventListener('click', () => {
        const overlay = button.nextElementSibling;
        overlay.classList.toggle('active');
    });
});
document.querySelectorAll('.overlay__close').forEach(closeIcon => {
    closeIcon.addEventListener('click', () => {
        const overlay = closeIcon.parentElement;
        overlay.classList.remove('active');
    });
});