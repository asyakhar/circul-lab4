import {callApi, emitMessage} from "../shared/api.js?v=mfe-3";

const template = `
<section class="panel booking-panel">
    <h2><span class="section-star">✦</span> Цирковая касса</h2>
    <form data-form="sell" class="compact-form">
        <label>ID билета <input data-field="ticket-id" type="number" min="1" required></label>
        <label>ID человека <input data-field="person-id" type="number" min="1" required></label>
        <label>Цена продажи <input data-field="price" type="number" min="0" step="any" required></label>
        <button class="primary">Продать</button>
    </form>
    <form data-form="cancel" class="compact-form danger-zone">
        <label>ID человека <input data-field="cancel-person-id" type="number" min="1" required></label>
        <button class="danger">Отменить все бронирования</button>
    </form>
</section>

<div data-view="ticket-modal" class="ticket-modal" aria-hidden="true">
    <div class="ticket-modal-backdrop" data-action="close"></div>
    <section class="printed-ticket" role="dialog" aria-modal="true" aria-labelledby="ticketHeading">
        <button class="ticket-close" type="button" aria-label="Закрыть билет" data-action="close">×</button>
        <div class="ticket-topline"><span>ЦИРКуль</span><span>ВХОДНОЙ БИЛЕТ</span></div>
        <div class="ticket-roof" aria-hidden="true"><span>✦</span></div>
        <p class="ticket-kicker">Сегодня вы — часть представления</p>
        <h2 id="ticketHeading" class="ticket-event-name" data-receipt="heading">Билет оформлен!</h2>
        <div class="ticket-number">№ <span data-receipt="ticket-id">—</span></div>
        <div class="ticket-perforation"><span></span><span></span></div>
        <div class="ticket-details">
            <div><small>ГОСТЬ</small><strong data-receipt="person">—</strong></div>
            <div><small>ТИП БИЛЕТА</small><strong data-receipt="type">—</strong></div>
            <div><small>МЕРОПРИЯТИЕ</small><strong data-receipt="event">—</strong></div>
            <div><small>ЦЕНА</small><strong data-receipt="price">—</strong></div>
            <div><small>МЕСТО · КООРДИНАТЫ</small><strong data-receipt="coordinates">—</strong></div>
            <div><small>ВЫПИСАН</small><strong data-receipt="date">—</strong></div>
        </div>
        <div class="ticket-barcode" aria-hidden="true"></div>
        <p class="ticket-footer">Очерти круг своих проблем — и до встречи под куполом!</p>
        <div class="ticket-actions">
            <button data-action="print" class="primary" type="button">Распечатать билет</button>
            <button data-action="replay-tiger" class="ticket-dismiss" type="button">Ещё раз позвать тигра</button>
            <button data-action="close" class="ticket-dismiss" type="button">Готово</button>
        </div>
    </section>
</div>
<div data-view="tiger" class="tiger-show" aria-hidden="true">
    <img src="static/assets/tiger-fire-ring.png" alt="Тигр прыгает через огненное кольцо">
</div>
<div data-view="confetti" class="confetti-layer" aria-hidden="true"></div>
<div data-view="curtain" class="booking-curtain" aria-hidden="true">
    <div class="curtain-panel curtain-panel-left"></div><div class="curtain-panel curtain-panel-right"></div>
    <div class="curtain-message">Цирка не будет</div>
</div>`;

class BookingMfe extends HTMLElement {
    connectedCallback() {
        if (this.initialized) return;
        this.initialized = true;
        this.innerHTML = template;
        this.keyHandler = event => {
            if (event.key === "Escape" && this.view("ticket-modal").classList.contains("active")) {
                this.closeReceipt();
            }
        };
        document.addEventListener("keydown", this.keyHandler);
        this.bindEvents();
    }

    disconnectedCallback() {
        document.removeEventListener("keydown", this.keyHandler);
        [this.tigerTimer, this.confettiTimer, this.curtainTimer].forEach(clearTimeout);
    }

    get apiBase() { return this.getAttribute("api-base") || "/api/booking"; }
    get ticketApiBase() { return this.getAttribute("ticket-api-base") || "/api/ticket"; }
    field(name) { return this.querySelector(`[data-field="${name}"]`); }
    view(name) { return this.querySelector(`[data-view="${name}"]`); }
    receipt(name) { return this.querySelector(`[data-receipt="${name}"]`); }

    bindEvents() {
        this.querySelector('[data-form="sell"]').addEventListener("submit", event => {
            event.preventDefault();
            this.sell();
        });
        this.querySelector('[data-form="cancel"]').addEventListener("submit", event => {
            event.preventDefault();
            this.cancel();
        });
        this.querySelectorAll('[data-action="close"]').forEach(button => {
            button.addEventListener("click", () => this.closeReceipt());
        });
        this.querySelector('[data-action="print"]').addEventListener("click", () => window.print());
        this.querySelector('[data-action="replay-tiger"]').addEventListener("click", async () => {
            if (!this.currentReceipt) return;
            const receipt = this.currentReceipt;
            this.closeReceipt();
            this.showConfetti();
            await this.showTiger();
            this.openReceipt(receipt.ticketId, receipt.personId, receipt.price, receipt.ticket);
        });
    }

    async sell() {
        const ticketId = this.field("ticket-id").value;
        const personId = this.field("person-id").value;
        const price = this.field("price").value;
        try {
            await callApi(`${this.apiBase}/sell/${ticketId}/${personId}/${price}`, {method: "POST"});
            const ticketPromise = callApi(`${this.ticketApiBase}/tickets/${ticketId}`)
                .then(result => result.data)
                .catch(() => null);
            this.showConfetti();
            await this.showTiger();
            this.openReceipt(ticketId, personId, price, await ticketPromise);
            emitMessage(this, `Билет ${ticketId} продан человеку ${personId}.`);
        } catch (error) {
            emitMessage(this, error.message, "error");
        }
    }

    async cancel() {
        const personId = this.field("cancel-person-id").value;
        try {
            await callApi(`${this.apiBase}/person/${personId}/cancel`, {method: "DELETE"});
            this.showCurtain();
            emitMessage(this, `Все бронирования человека ${personId} отменены.`);
        } catch (error) {
            emitMessage(this, error.message, "error");
        }
    }

    openReceipt(ticketId, personId, price, ticket) {
        this.currentReceipt = {ticketId, personId, price, ticket};
        this.receipt("heading").textContent = ticket?.event?.name || ticket?.name || "Цирковое представление";
        this.receipt("ticket-id").textContent = ticketId;
        this.receipt("person").textContent = personId;
        this.receipt("type").textContent = ticket?.type || "Обычный";
        this.receipt("event").textContent = ticket?.event?.name || "Основное представление";
        this.receipt("price").textContent = new Intl.NumberFormat("ru-RU", {
            style: "currency", currency: "RUB", maximumFractionDigits: 2,
        }).format(Number(price));
        this.receipt("coordinates").textContent = ticket?.coordinates
            ? `(${ticket.coordinates.x}; ${ticket.coordinates.y})`
            : "Место уточняется";
        this.receipt("date").textContent = new Date().toLocaleString("ru-RU");
        this.view("ticket-modal").classList.add("active");
        this.view("ticket-modal").setAttribute("aria-hidden", "false");
        document.body.classList.add("scare-open");
        this.querySelector('[data-action="print"]').focus();
    }

    closeReceipt() {
        this.view("ticket-modal").classList.remove("active");
        this.view("ticket-modal").setAttribute("aria-hidden", "true");
        document.body.classList.remove("scare-open");
    }

    showTiger() {
        clearTimeout(this.tigerTimer);
        const tiger = this.view("tiger");
        tiger.classList.remove("active");
        void tiger.offsetWidth;
        tiger.classList.add("active");
        tiger.setAttribute("aria-hidden", "false");
        const duration = window.matchMedia("(prefers-reduced-motion: reduce)").matches ? 300 : 2400;
        return new Promise(resolve => {
            this.tigerTimer = setTimeout(() => {
                tiger.classList.remove("active");
                tiger.setAttribute("aria-hidden", "true");
                resolve();
            }, duration);
        });
    }

    showConfetti() {
        const layer = this.view("confetti");
        const colors = ["#f5cf68", "#d94455", "#fff3d3", "#4d9b93", "#8d5ca6"];
        layer.replaceChildren();
        layer.classList.add("active");
        clearTimeout(this.confettiTimer);
        for (let index = 0; index < 64; index += 1) {
            const piece = document.createElement("span");
            piece.className = "confetti-piece";
            piece.style.setProperty("--x", `${Math.random() * 100}vw`);
            piece.style.setProperty("--delay", `${Math.random() * 450}ms`);
            piece.style.setProperty("--duration", `${1300 + Math.random() * 1000}ms`);
            piece.style.setProperty("--color", colors[index % colors.length]);
            layer.appendChild(piece);
        }
        this.confettiTimer = setTimeout(() => {
            layer.classList.remove("active");
            layer.replaceChildren();
        }, 2700);
    }

    showCurtain() {
        const curtain = this.view("curtain");
        clearTimeout(this.curtainTimer);
        curtain.classList.remove("active");
        void curtain.offsetWidth;
        curtain.classList.add("active");
        curtain.setAttribute("aria-hidden", "false");
        this.curtainTimer = setTimeout(() => {
            curtain.classList.remove("active");
            curtain.setAttribute("aria-hidden", "true");
        }, 2300);
    }
}

customElements.define("booking-mfe", BookingMfe);
