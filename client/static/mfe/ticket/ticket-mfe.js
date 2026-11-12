import {callApi, emitMessage, escapeHtml, lines} from "../shared/api.js?v=mfe-3";

const template = `
<section class="panel">
    <h2><span class="section-star">✦</span> Афиша представлений</h2>
    <div class="filters">
        <label>Страница <input data-field="page" type="number" min="0" value="0"></label>
        <label>Размер <input data-field="size" type="number" min="1" max="100" value="10"></label>
        <label class="wide">Сортировка
            <textarea data-field="sort" rows="2" placeholder="price,desc&#10;name,asc"></textarea>
        </label>
        <label class="wide">Фильтры
            <textarea data-field="filter" rows="2" placeholder="price:gte:100&#10;type:eq:VIP"></textarea>
        </label>
        <button data-action="apply" class="primary" type="button">Найти билеты</button>
    </div>
    <div class="table-wrapper">
        <table>
            <thead><tr><th>ID</th><th>Название</th><th>Координаты</th><th>Дата создания</th><th>Цена</th><th>Комментарий</th><th>Тип</th><th>Мероприятие</th></tr></thead>
            <tbody data-view="tickets"></tbody>
        </table>
    </div>
    <div class="pagination">
        <button data-action="previous" type="button">Назад</button>
        <span data-view="page-label">Страница 0</span>
        <button data-action="next" type="button">Вперёд</button>
    </div>
</section>

<div class="grid">
    <section class="panel">
        <h2><span class="section-star">✦</span> Выпустить билет</h2>
        <form data-form="create">
            <label>JSON билета
                <textarea data-field="create-ticket" rows="11">{
  "name": "Билет в партер",
  "coordinates": {"x": 10.5, "y": 20},
  "price": 2500.5,
  "comment": "Место рядом со сценой",
  "type": "VIP",
  "event": null
}</textarea>
            </label>
            <button class="primary">Выпустить билет</button>
        </form>
    </section>

    <section class="panel">
        <h2><span class="section-star">✦</span> Найти билет</h2>
        <form data-form="get" class="compact-form">
            <label>ID билета <input data-field="get-id" type="number" min="1" required></label>
            <label>ETag (необязательно) <input data-field="get-etag" placeholder='"ticket-1-v1"'></label>
            <button>Получить</button>
        </form>
        <form data-form="delete" class="compact-form danger-zone">
            <label>ID билета <input data-field="delete-id" type="number" min="1" required></label>
            <div class="clown-hand-delete">
                <button class="danger">Удалить</button>
                <img class="clown-grab-hand" src="static/assets/clown-grab-hand.png" alt="" aria-hidden="true" draggable="false">
            </div>
        </form>
        <button data-action="options" type="button">Показать разрешённые методы</button>
    </section>

    <section class="panel">
        <h2><span class="section-star">✦</span> Полное обновление · PUT</h2>
        <form data-form="put">
            <label>ID билета <input data-field="put-id" type="number" min="1" required></label>
            <label>JSON билета
                <textarea data-field="put-ticket" rows="9">{
  "name": "Обновлённый билет",
  "coordinates": {"x": 15, "y": 25},
  "price": 3000,
  "comment": "Обновлено",
  "type": "USUAL",
  "event": null
}</textarea>
            </label>
            <button class="primary">Обновить</button>
        </form>
    </section>

    <section class="panel">
        <h2><span class="section-star">✦</span> Частичное обновление · PATCH</h2>
        <form data-form="patch">
            <label>ID билета <input data-field="patch-id" type="number" min="1" required></label>
            <label>JSON изменяемых полей
                <textarea data-field="patch-ticket" rows="9">{
  "price": 2800,
  "comment": "Новый комментарий"
}</textarea>
            </label>
            <button class="primary">Изменить</button>
        </form>
    </section>

    <section class="panel">
        <h2><span class="section-star">✦</span> Особые номера</h2>
        <button data-action="max-type" type="button">Билет с максимальным типом</button>
        <form data-form="comment" class="compact-form">
            <label>Подстрока комментария <input data-field="comment-substring" required></label>
            <button>Найти</button>
        </form>
        <form data-form="type" class="compact-form">
            <label>Тип
                <select data-field="greater-type"><option>VIP</option><option>USUAL</option><option>BUDGETARY</option><option>CHEAP</option></select>
            </label>
            <button>Найти типы больше</button>
        </form>
    </section>
</div>`;

class TicketMfe extends HTMLElement {
    connectedCallback() {
        if (this.initialized) return;
        this.initialized = true;
        this.innerHTML = template;
        this.refreshHandler = () => this.loadTickets();
        document.addEventListener("ticket:refresh", this.refreshHandler);
        this.bindEvents();
        this.loadTickets();
    }

    disconnectedCallback() {
        document.removeEventListener("ticket:refresh", this.refreshHandler);
    }

    get apiBase() {
        return this.getAttribute("api-base") || "/api/ticket";
    }

    field(name) {
        return this.querySelector(`[data-field="${name}"]`);
    }

    async loadTickets() {
        const params = new URLSearchParams();
        const page = this.field("page").value;
        params.set("page", page);
        params.set("size", this.field("size").value);
        lines(this.field("sort").value).forEach(value => params.append("sort", value));
        lines(this.field("filter").value).forEach(value => params.append("filter", value));

        try {
            const {data} = await callApi(`${this.apiBase}/tickets?${params}`);
            this.renderTickets(data);
            this.querySelector('[data-view="page-label"]').textContent = `Страница ${page}`;
        } catch (error) {
            emitMessage(this, error.message, "error");
        }
    }

    renderTickets(tickets) {
        const body = this.querySelector('[data-view="tickets"]');
        const list = Array.isArray(tickets) ? tickets : tickets ? [tickets] : [];
        if (list.length === 0) {
            body.innerHTML = '<tr><td colspan="8" class="empty">Билеты не найдены</td></tr>';
            return;
        }
        body.innerHTML = list.map(ticket => `
            <tr>
                <td>${escapeHtml(ticket.id)}</td><td>${escapeHtml(ticket.name)}</td>
                <td>${escapeHtml(ticket.coordinates?.x)}, ${escapeHtml(ticket.coordinates?.y)}</td>
                <td>${escapeHtml(ticket.creationDate)}</td><td>${escapeHtml(ticket.price)}</td>
                <td>${escapeHtml(ticket.comment)}</td><td><span class="badge">${escapeHtml(ticket.type)}</span></td>
                <td>${escapeHtml(this.eventText(ticket.event))}</td>
            </tr>`).join("");
    }

    eventText(event) {
        return event ? `${event.name}, билетов: ${event.ticketsCount}, ${event.eventType ?? "без типа"}` : "—";
    }

    parseJson(field) {
        try {
            return JSON.parse(this.field(field).value);
        } catch (_) {
            throw new Error("Введён некорректный JSON.");
        }
    }

    ticketUrl(id, ticket) {
        const params = new URLSearchParams({ticket: JSON.stringify(ticket)});
        return `${this.apiBase}/tickets${id ? `/${id}` : ""}?${params}`;
    }

    bindEvents() {
        this.querySelector('[data-action="apply"]').addEventListener("click", () => this.loadTickets());
        this.querySelector('[data-action="previous"]').addEventListener("click", () => {
            this.field("page").value = Math.max(0, Number(this.field("page").value) - 1);
            this.loadTickets();
        });
        this.querySelector('[data-action="next"]').addEventListener("click", () => {
            this.field("page").value = Number(this.field("page").value) + 1;
            this.loadTickets();
        });

        this.querySelector('[data-form="create"]').addEventListener("submit", async event => {
            event.preventDefault();
            await this.run(async () => {
                const ticket = this.parseJson("create-ticket");
                const {data} = await callApi(this.ticketUrl(null, ticket), {method: "POST"});
                emitMessage(this, `Билет создан, id: ${data.id}.`);
                await this.loadTickets();
            });
        });

        this.querySelector('[data-form="get"]').addEventListener("submit", async event => {
            event.preventDefault();
            await this.run(async () => {
                const id = this.field("get-id").value;
                const etag = this.field("get-etag").value.trim();
                const {response, data} = await callApi(`${this.apiBase}/tickets/${id}`, {
                    headers: etag ? {"If-None-Match": etag} : {},
                });
                if (response.status === 304) {
                    emitMessage(this, "Билет не изменился: сервер вернул 304.");
                    return;
                }
                this.renderTickets(data);
                emitMessage(this, `Билет получен. ETag: ${response.headers.get("ETag") || "не указан"}.`);
            });
        });

        this.querySelector('[data-form="delete"]').addEventListener("submit", async event => {
            event.preventDefault();
            await this.run(async () => {
                const id = this.field("delete-id").value;
                await callApi(`${this.apiBase}/tickets/${id}`, {method: "DELETE"});
                emitMessage(this, `Билет ${id} удалён или уже отсутствовал.`);
                await this.loadTickets();
            });
        });

        this.querySelector('[data-form="put"]').addEventListener("submit", async event => {
            event.preventDefault();
            await this.run(async () => {
                const id = this.field("put-id").value;
                const {data} = await callApi(this.ticketUrl(id, this.parseJson("put-ticket")), {method: "PUT"});
                this.renderTickets(data);
                emitMessage(this, `Билет ${id} полностью обновлён.`);
            });
        });

        this.querySelector('[data-form="patch"]').addEventListener("submit", async event => {
            event.preventDefault();
            await this.run(async () => {
                const id = this.field("patch-id").value;
                const {data} = await callApi(this.ticketUrl(id, this.parseJson("patch-ticket")), {method: "PATCH"});
                this.renderTickets(data);
                emitMessage(this, `Билет ${id} частично обновлён.`);
            });
        });

        this.querySelector('[data-action="options"]').addEventListener("click", () => this.run(async () => {
            const {response} = await callApi(`${this.apiBase}/tickets`, {method: "OPTIONS"});
            emitMessage(this, `Разрешённые методы: ${response.headers.get("Allow") || "не указаны"}.`);
        }));

        this.querySelector('[data-action="max-type"]').addEventListener("click", () => this.run(async () => {
            const {data} = await callApi(`${this.apiBase}/tickets/max-type`);
            this.renderTickets(data);
            emitMessage(this, "Найден билет с максимальным типом.");
        }));

        this.querySelector('[data-form="comment"]').addEventListener("submit", event => {
            event.preventDefault();
            this.run(async () => {
                const params = new URLSearchParams({substring: this.field("comment-substring").value});
                const {data} = await callApi(`${this.apiBase}/tickets/comment-contains?${params}`);
                this.renderTickets(data);
                emitMessage(this, "Поиск по комментарию выполнен.");
            });
        });

        this.querySelector('[data-form="type"]').addEventListener("submit", event => {
            event.preventDefault();
            this.run(async () => {
                const params = new URLSearchParams({type: this.field("greater-type").value});
                const {data} = await callApi(`${this.apiBase}/tickets/type-greater-than?${params}`);
                this.renderTickets(data);
                emitMessage(this, "Поиск по типу выполнен.");
            });
        });
    }

    async run(action) {
        try {
            await action();
        } catch (error) {
            emitMessage(this, error.message, "error");
        }
    }
}

customElements.define("ticket-mfe", TicketMfe);
