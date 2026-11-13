const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
const header = document.querySelector("[data-header]");
const menu = document.querySelector("[data-menu]");
const menuToggle = document.querySelector("[data-menu-toggle]");

function closeMenu() {
  menu.classList.remove("is-open");
  menuToggle.setAttribute("aria-expanded", "false");
  menuToggle.setAttribute("aria-label", "Abrir menu");
  document.body.classList.remove("menu-open");
}

menuToggle.addEventListener("click", () => {
  const opening = !menu.classList.contains("is-open");
  menu.classList.toggle("is-open", opening);
  menuToggle.setAttribute("aria-expanded", String(opening));
  menuToggle.setAttribute("aria-label", opening ? "Fechar menu" : "Abrir menu");
  document.body.classList.toggle("menu-open", opening);
});

menu.querySelectorAll("a").forEach((link) => link.addEventListener("click", closeMenu));
window.addEventListener("resize", () => { if (window.innerWidth > 820) closeMenu(); });
window.addEventListener("scroll", () => header.classList.toggle("is-scrolled", window.scrollY > 18), { passive: true });

const revealItems = [...document.querySelectorAll("[data-reveal]")];
if (reduceMotion || !("IntersectionObserver" in window)) {
  revealItems.forEach((item) => item.classList.add("is-revealed"));
} else {
  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add("is-revealed");
      observer.unobserve(entry.target);
    });
  }, { threshold: 0.14, rootMargin: "0px 0px -45px" });
  revealItems.forEach((item) => revealObserver.observe(item));
}

const counters = [...document.querySelectorAll("[data-counter]")];
function animateCounter(element) {
  const target = Number(element.dataset.counter);
  if (reduceMotion) { element.textContent = String(target); return; }
  const startedAt = performance.now();
  function update(now) {
    const progress = Math.min((now - startedAt) / 900, 1);
    element.textContent = String(Math.round(target * (1 - Math.pow(1 - progress, 3))));
    if (progress < 1) requestAnimationFrame(update);
  }
  requestAnimationFrame(update);
}

if ("IntersectionObserver" in window && !reduceMotion) {
  const counterObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      animateCounter(entry.target);
      observer.unobserve(entry.target);
    });
  }, { threshold: .7 });
  counters.forEach((counter) => { counter.textContent = "0"; counterObserver.observe(counter); });
} else {
  counters.forEach(animateCounter);
}

const scenes = [...document.querySelectorAll("[data-scene]")];
const replayButtons = [...document.querySelectorAll("[data-replay]")];
const instagramButton = document.querySelector(".app-icon--instagram");
const timer = document.querySelector("[data-timer]");
let sequenceId = 0;
let timerInterval;
let manualDemoUntil = 0;

function showScene(name) {
  scenes.forEach((scene) => {
    const active = scene.dataset.scene === name;
    scene.classList.toggle("is-active", active);
    scene.setAttribute("aria-hidden", String(!active));
  });
}

function wait(ms, id) {
  return new Promise((resolve) => window.setTimeout(() => resolve(id === sequenceId), reduceMotion ? 80 : ms));
}

function startTimer() {
  window.clearInterval(timerInterval);
  let seconds = 300;
  timer.textContent = "00:05:00";
  if (reduceMotion) return;
  timerInterval = window.setInterval(() => {
    seconds = Math.max(0, seconds - 1);
    timer.textContent = `00:${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
  }, 1000);
}

async function playSequence({ immediate = false } = {}) {
  const id = ++sequenceId;
  manualDemoUntil = Date.now() + 4500;
  window.clearInterval(timerInterval);
  showScene("home");
  if (!immediate && !(await wait(1700, id))) return;
  showScene("instagram");
  if (!(await wait(immediate ? 1050 : 950, id)) || id !== sequenceId) return;
  showScene("blocked");
  startTimer();
}

instagramButton.addEventListener("click", () => playSequence({ immediate: true }));
replayButtons.forEach((button) => button.addEventListener("click", () => playSequence()));

// Build the scrollytelling section from the existing editable phone DOM.
const phoneVisual = document.querySelector(".hero__visual");
const mainContent = document.querySelector("main#conteudo");
const numbersSection = mainContent?.querySelector(".numbers.section");
if (phoneVisual && mainContent && numbersSection) {
  const story = document.createElement("section");
  story.className = "phone-story";
  story.setAttribute("aria-label", "Demonstração do ciclo automático do InstBlock");

  const sticky = document.createElement("div");
  sticky.className = "phone-story__sticky";
  const slot = document.createElement("div");
  slot.className = "phone-story__slot";
  sticky.append(slot);
  story.append(sticky);

  const chapters = document.createElement("div");
  chapters.className = "phone-story__chapters";
  const storyCopy = [
    ["home", "01 / Perceba o impulso", "Um intervalo entre você e o feed", "Toque no Instagram dentro do telefone para ver a demonstração. O InstBlock detecta a abertura do aplicativo oficial."],
    ["instagram", "02 / Interrompa o automático", "O impulso não precisa virar rolagem", "Uma tentativa distinta é registrada e o bloqueio entra em ação, sem ler o conteúdo da tela."],
    ["blocked", "03 / Deixe o ciclo agir", "Seu tempo de foco, protegido", "A espera usa o tempo interno do Android. Ao zerar, uma notificação avisa e libera dez minutos; depois, a proteção volta sozinha."],
  ];
  storyCopy.forEach(([scene, eyebrow, title, description], index) => {
    const chapter = document.createElement("article");
    chapter.className = `phone-story__chapter${index === 0 ? " is-current" : ""}`;
    chapter.dataset.phoneChapter = scene;
    const label = document.createElement("span");
    label.className = "section-label";
    label.textContent = eyebrow;
    const heading = document.createElement("h2");
    heading.textContent = title;
    const paragraph = document.createElement("p");
    paragraph.textContent = description;
    chapter.append(label, heading, paragraph);
    chapters.append(chapter);
  });
  story.append(chapters);
  mainContent.insertBefore(story, numbersSection);

  phoneVisual.classList.add("phone-story__visual");
  phoneVisual.removeAttribute("data-reveal");
  phoneVisual.closest(".hero__grid")?.classList.add("hero__grid--phone-moved");
  slot.append(phoneVisual);

  const chaptersInStory = [...chapters.querySelectorAll("[data-phone-chapter]")];
  if ("IntersectionObserver" in window && !reduceMotion) {
    const storyObserver = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        chaptersInStory.forEach((chapter) => chapter.classList.toggle("is-current", chapter === entry.target));
        if (Date.now() >= manualDemoUntil) showScene(entry.target.dataset.phoneChapter);
      });
    }, { threshold: 0.62 });
    chaptersInStory.forEach((chapter) => storyObserver.observe(chapter));
  }
}
