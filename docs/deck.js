const slides = Array.from(document.querySelectorAll(".slide"));
const counter = document.querySelector("#counter");
const prevButton = document.querySelector("#prev");
const nextButton = document.querySelector("#next");
let current = 0;

function showSlide(index) {
  current = Math.max(0, Math.min(index, slides.length - 1));
  slides.forEach((slide, slideIndex) => {
    slide.classList.toggle("is-active", slideIndex === current);
  });
  counter.textContent = `${current + 1} / ${slides.length}`;
  prevButton.disabled = current === 0;
  nextButton.disabled = current === slides.length - 1;
}

function nextSlide() {
  showSlide(current + 1);
}

function previousSlide() {
  showSlide(current - 1);
}

prevButton.addEventListener("click", previousSlide);
nextButton.addEventListener("click", nextSlide);

document.addEventListener("keydown", (event) => {
  if (event.key === "ArrowRight" || event.key === " " || event.key === "PageDown") {
    event.preventDefault();
    nextSlide();
  }
  if (event.key === "ArrowLeft" || event.key === "PageUp") {
    event.preventDefault();
    previousSlide();
  }
  if (event.key === "Home") {
    showSlide(0);
  }
  if (event.key === "End") {
    showSlide(slides.length - 1);
  }
});

showSlide(0);
