const messageBox = document.getElementById("message");
const clownScare = document.getElementById("clownScare");
const balloonTriggers = document.querySelectorAll(".balloon-trigger");
let clownTimer = null;
let lastBalloonTrigger = null;

function showMessage(text, type = "success") {
    messageBox.className = `message mascot-speech ${type}`;
    messageBox.textContent = text;
    window.scrollTo({top: 0, behavior: "smooth"});
}

function hideClowns() {
    clownScare.classList.remove("active");
    clownScare.setAttribute("aria-hidden", "true");
    document.body.classList.remove("scare-open");
    clearTimeout(clownTimer);
    lastBalloonTrigger?.focus();
}

function showClowns(trigger) {
    clearTimeout(clownTimer);
    lastBalloonTrigger = trigger;
    clownScare.classList.remove("active");
    void clownScare.offsetWidth;
    clownScare.classList.add("active");
    clownScare.setAttribute("aria-hidden", "false");
    document.body.classList.add("scare-open");
    clownTimer = setTimeout(hideClowns, 1800);
}

document.addEventListener("circus:message", event => {
    showMessage(event.detail.text, event.detail.type);
});

document.getElementById("reloadButton").addEventListener("click", () => {
    document.dispatchEvent(new CustomEvent("ticket:refresh"));
});

balloonTriggers.forEach(trigger => {
    trigger.addEventListener("click", () => showClowns(trigger));
});

clownScare.addEventListener("click", event => {
    if (event.target === clownScare) hideClowns();
});

document.addEventListener("keydown", event => {
    if (event.key === "Escape" && clownScare.classList.contains("active")) {
        hideClowns();
    }
});
