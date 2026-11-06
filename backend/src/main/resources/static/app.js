'use strict';

/**
 * Access token хранится только в памяти вкладки. Refresh token браузер получает
 * в HttpOnly/SameSite cookie.
 */
const state = {
    accessToken: null,
    user: null,
    subscribers: [],
    requests: { page: 0, size: 25, totalPages: 0, query: '', content: [] },
    selectedConnectionRequestIds: new Set(),
    activeConnectionRequestForNotes: null,
    notes: [],
    userPage: 0,
    auditPage: 0,
    refreshPromise: null
};

const $ = (id) => document.getElementById(id);
const escapeHtml = (value) => String(value ?? '').replace(/[&<>'"]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[ch]));
const roles = () => new Set((state.user?.roles || []).map(String));
const isAdmin = () => roles().has('ADMIN');
const canEdit = () => roles().has('OPERATOR') || isAdmin();


/**
 * Коды ролей в API остаются английскими; в интерфейсе используются русские названия.
 */
function roleLabel(value) {
    return ({
        ADMIN: 'Администратор',
        OPERATOR: 'Оператор',
        VIEWER: 'Наблюдатель'
    })[value] || value;
}

/** Русские имена полей для ошибок Bean Validation. */
function fieldLabel(value) {
    return ({
        externalId: 'внешний ID',
        title: 'название заявки',
        description: 'описание',
        address: 'адрес',
        email: 'email',
        phone: 'телефон',
        dueDate: 'срок исполнения',
        subscriberId: 'абонент',
        status: 'статус',
        estimatedHours: 'оценка трудозатрат',
        urgent: 'срочность',
        tags: 'теги',
        version: 'версия'
    })[value] || value;
}

/** Приводит первую букву текста после двоеточия к нижнему регистру. */
function lowerFirst(value) {
    const text = String(value ?? '');
    return text ? text.charAt(0).toLocaleLowerCase('ru-RU') + text.slice(1) : text;
}

/**
 * Нормализует сообщения вида «поле: Ошибка» в «поле: ошибка».
 * Правило применяется и к сообщениям сервера.
 */
function normalizeAfterColon(value) {
    return String(value ?? '').replace(/:\s*([А-ЯЁA-Z])/g,
        (_, letter) => `: ${letter.toLocaleLowerCase('ru-RU')}`);
}

/**
 * Приводит ввод телефона к формату +7-999-999-99-99.
 * Если пользователь вставляет 8XXXXXXXXXX или 7XXXXXXXXXX, код страны
 * автоматически нормализуется к +7.
 */
function formatRussianPhone(value) {
    let digits = String(value ?? '').replace(/\D/g, '');

    if (digits.startsWith('8')) {
        digits = `7${digits.slice(1)}`;
    }

    if (digits.startsWith('7')) {
        digits = digits.slice(1);
    }

    digits = digits.slice(0, 10);

    let result = '+7-';
    if (!digits.length) return result;

    result += digits.slice(0, 3);
    if (digits.length > 3) result += `-${digits.slice(3, 6)}`;
    if (digits.length > 6) result += `-${digits.slice(6, 8)}`;
    if (digits.length > 8) result += `-${digits.slice(8, 10)}`;

    return result;
}

/**
 * Маска телефона:
 * - при фокусе сразу подставляет +7-;
 * - разрешает вводить только цифры и служебные клавиши;
 * - дефисы добавляет автоматически;
 * - вставленный текст очищает от букв и лишних знаков.
 */
function bindPhoneMask(input) {
    if (!input) return;

    input.addEventListener('focus', () => {
        if (!input.value) input.value = '+7-';
    });

    input.addEventListener('keydown', event => {
        const allowedKeys = new Set([
            'Backspace', 'Delete', 'ArrowLeft', 'ArrowRight',
            'Home', 'End', 'Tab', 'Enter'
        ]);

        if (event.ctrlKey || event.metaKey || allowedKeys.has(event.key) || /^\d$/.test(event.key)) {
            return;
        }

        event.preventDefault();
    });

    input.addEventListener('input', () => {
        input.value = formatRussianPhone(input.value);
    });

    input.addEventListener('blur', () => {
        if (input.value === '+7-') input.value = '';
    });
}

function toast(message, error = false) {
    const node = $('toast');
    node.textContent = message;
    node.className = `toast show${error ? ' error' : ''}`;
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => node.className = 'toast', 3500);
}


/**
 * Ошибки modal dialog выводятся внутри формы, а не через общий toast.
 */
function showFormError(form, message) {
    const box = form.querySelector('[data-form-error]');
    if (!box) {
        toast(message, true);
        return;
    }
    box.textContent = message;
    box.hidden = false;
}

/** Очищает локальное сообщение об ошибке перед новой отправкой формы. */
function clearFormError(form) {
    const box = form.querySelector('[data-form-error]');
    if (!box) return;
    box.textContent = '';
    box.hidden = true;
}

async function parseError(response) {
    try {
        const body = await response.json();
        const fields = body.validationErrors
            ? Object.entries(body.validationErrors)
                .map(([key, value]) => `${fieldLabel(key)}: ${lowerFirst(value)}`)
                .join('; ')
            : '';

        const message = `${body.message || response.statusText}${fields ? ` — ${fields}` : ''}${body.correlationId ? ` [${body.correlationId}]` : ''}`;
        return normalizeAfterColon(message);
    } catch (_) {
        return `${response.status} ${response.statusText}`;
    }
}

/**
 * Общая функция HTTP-вызова обрабатывает 401 и ограничивает повторный refresh.
 * Параллельные запросы используют один refreshPromise.
 */
async function api(path, options = {}, retry = true) {
    const headers = new Headers(options.headers || {});
    if (state.accessToken) headers.set('Authorization', `Bearer ${state.accessToken}`);
    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
    const response = await fetch(`/api${path}`, {...options, headers, credentials: 'include'});
    if (response.status === 401 && retry) {
        const refreshed = await refreshAccessToken();
        if (refreshed) return api(path, options, false);
    }
    if (!response.ok) throw new Error(await parseError(response));
    if (response.status === 204) return null;
    const type = response.headers.get('content-type') || '';
    return type.includes('application/json') ? response.json() : response;
}

async function refreshAccessToken() {
    if (!state.refreshPromise) {
        state.refreshPromise = fetch('/api/auth/refresh', {
            method: 'POST', credentials: 'include', headers: {'Content-Type':'application/json'}, body: '{}'
        }).then(async response => {
            if (!response.ok) return false;
            const result = await response.json();
            state.accessToken = result.accessToken;
            state.user = result.user;
            return true;
        }).finally(() => state.refreshPromise = null);
    }
    return state.refreshPromise;
}

async function boot() {
    bindEvents();
    if (await refreshAccessToken()) {
        await enterApplication();
    } else {
        showLogin();
    }
}

function showLogin() {
    $('app-view').hidden = true;
    $('login-view').hidden = false;
    state.accessToken = null;
    state.user = null;
}

async function enterApplication() {
    $('login-view').hidden = true;
    $('app-view').hidden = false;
    applyRoleVisibility();
    $('current-user').innerHTML = `<strong>${escapeHtml(state.user.displayName)}</strong><br>${escapeHtml(state.user.email)}<br>${escapeHtml(state.user.roles.map(roleLabel).join(', '))}`;

    if (state.user.passwordChangeRequired) {
        toast('Для продолжения необходимо сменить временный пароль', true);
        const changed = await openPasswordDialog(true);
        if (!changed) return;
    }

    /*
     * Ошибка загрузки аналитики или справочников не считается ошибкой входа.
     * Пользователь остаётся в приложении и получает отдельное уведомление.
     */
    try {
        await Promise.all([loadSubscribers(), loadDashboard()]);
    } catch (error) {
        console.error('[ИС интернет-провайдера] Initial data loading failed', error);
        toast(`Вход выполнен, но часть данных не загрузилась: ${error.message}`, true);
    }
}

function applyRoleVisibility() {
    document.querySelectorAll('.admin-only').forEach(node => node.hidden = !isAdmin());
    document.querySelectorAll('.operator-only').forEach(node => node.hidden = !canEdit());
}

function switchTab(name) {
    document.querySelectorAll('.tab').forEach(tab => tab.classList.toggle('active', tab.id === `tab-${name}`));
    document.querySelectorAll('.nav-item').forEach(button => button.classList.toggle('active', button.dataset.tab === name));
    const titles = {
        dashboard: ['Аналитика','Сводная картина по заявкам'],
        requests: ['Заявки','Умный поиск, карточки и массовые операции'],
        subscribers: ['Абоненты','Единый справочник абонентов'],
        users: ['Пользователи','Роли, блокировка и временные пароли'],
        audit: ['Журнал аудита','История действий пользователей']
    };
    [$('page-title').textContent, $('page-subtitle').textContent] = titles[name];
    if (name === 'requests') loadConnectionRequests();
    if (name === 'subscribers') renderSubscribers();
    if (name === 'users' && isAdmin()) loadUsers();
    if (name === 'audit' && isAdmin()) loadAudit();
    if (name === 'dashboard') loadDashboard();
}

async function loadDashboard() {
    try {
        const data = await api('/analytics/summary');
        const metrics = [
            ['Всего', data.total], ['Активные', data.active], ['В архиве', data.archived],
            ['С срочностью', data.urgent], ['Оценка трудозатрат', data.estimatedHours ?? '—']
        ];
        $('metric-grid').innerHTML = metrics.map(([name,value]) => `<article class="metric card"><span class="muted">${name}</span><strong>${value}</strong></article>`).join('');
        renderBars('chart-subscribers', data.bySubscriber);
        renderBars('chart-status', data.byStatus);
        renderBars('chart-priority', data.byPriority);
    } catch (error) { toast(error.message, true); }
}

function renderBars(id, values) {
    const max = Math.max(1, ...values.map(item => item.count));

    const displayName = value => {
        if (id === 'chart-status') return statusLabel(value);
        if (id === 'chart-priority') return priorityLabel(value);
        return value;
    };

    $(id).innerHTML = values.length
        ? values.map(item => `<div class="bar-row"><span>${escapeHtml(displayName(item.name))}</span><progress value="${item.count}" max="${max}"></progress><strong>${item.count}</strong></div>`).join('')
        : '<p class="muted">Нет данных</p>';
}

async function loadSubscribers() {
    try { state.subscribers = await api('/subscribers?activeOnly=false'); renderSubscribers(); }
    catch (error) { toast(error.message, true); }
}

function renderSubscribers() {
    const body = $('subscribers-body');
    if (!body) return;
    body.innerHTML = state.subscribers.map(subscriber => `<tr>
        <td><strong>${escapeHtml(subscriber.code)}</strong></td><td>${escapeHtml(subscriber.name || '—')}</td><td>${subscriber.registrationYear ?? '—'}</td>
        <td><span class="badge ${subscriber.active ? 'success' : ''}">${subscriber.active ? 'Да' : 'Нет'}</span></td>
        <td class="admin-only"><div class="actions"><button class="secondary" data-subscriber-edit="${subscriber.id}">Изменить</button><button class="secondary" data-subscriber-toggle="${subscriber.id}">${subscriber.active ? 'Отключить' : 'Включить'}</button></div></td>
    </tr>`).join('');
    applyRoleVisibility();
}

async function loadConnectionRequests(page = state.requests.page) {
    try {
        const q = encodeURIComponent(state.requests.query);
        const result = await api(`/requests?q=${q}&page=${page}&size=${state.requests.size}&sort=${encodeURIComponent($('request-sort').value)}`);
        state.requests = {...state.requests, ...result};
        state.selectedConnectionRequestIds.clear();
        renderConnectionRequests();
    } catch (error) { toast(error.message, true); }
}

function renderConnectionRequests() {
    $('requests-body').innerHTML = state.requests.content.map(request => `<tr class="${request.archived ? 'archived' : ''}">
        <td class="operator-only"><input type="checkbox" data-request-select="${request.id}"></td>
        <td><strong>${escapeHtml(request.title)}</strong><br><small class="muted">${escapeHtml(request.externalId || '')}</small></td>
        <td>${escapeHtml(request.subscriberCode || '—')}</td><td>${escapeHtml(statusLabel(request.status))}</td>
        <td>${request.estimatedHours ?? '—'}</td><td>${request.urgent ? '<span class="badge warning">Да</span>' : 'Нет'}</td>
        <td><span class="badge priority-${request.priorityLevel}">${escapeHtml(priorityLabel(request.priorityLevel))}</span></td>
        <td>${request.tags.map(tag => `<span class="badge">${escapeHtml(tag)}</span>`).join(' ') || '—'}</td>
        <td><div class="actions">
            <button class="secondary" data-request-view="${request.id}">Карточка</button><button class="secondary" data-request-notes="${request.id}">Заметки</button>
            ${canEdit() ? `<button class="secondary" data-request-edit="${request.id}">Изменить</button><button class="danger-outline" data-request-archive="${request.id}">${request.archived ? 'Восстановить' : 'В архив'}</button>` : ''}
        </div></td>
    </tr>`).join('') || '<tr><td colspan="9" class="muted">Заявки не найдены</td></tr>';
    $('requests-page').textContent = `Страница ${state.requests.page + 1} из ${Math.max(1,state.requests.totalPages)} · записей ${state.requests.totalElements}`;
    $('requests-prev').disabled = state.requests.first;
    $('requests-next').disabled = state.requests.last;
    $('select-all-requests').checked = false;
    applyRoleVisibility();
}

function requestForm(request = null, readOnly = false) {
    const subscriberOptions = ['<option value="">Выберите абонента</option>', ...state.subscribers.filter(g => g.active || g.id === request?.subscriberId).map(g => `<option value="${g.id}" ${g.id === request?.subscriberId ? 'selected' : ''}>${escapeHtml(g.code)}</option>`)].join('');
    return `<div class="dialog-head"><h3>${request ? (readOnly ? 'Карточка заявки' : 'Изменение заявки') : 'Новая заявка'}</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <div class="form-grid">
      <label>Описание<input name="description" required maxlength="1000" value="${escapeHtml(request?.description || '')}" ${readOnly?'disabled':''}></label>
      <label>Название заявки<input name="title" required maxlength="100" value="${escapeHtml(request?.title || '')}" ${readOnly?'disabled':''}></label>
      <label>Адрес<input name="address" maxlength="500" value="${escapeHtml(request?.address || '')}" ${readOnly?'disabled':''}></label>
      <label>External ID<input name="externalId" required maxlength="100" value="${escapeHtml(request?.externalId || '')}" ${readOnly?'disabled':''}></label>
      <label>Email<input name="email" type="email" maxlength="254" value="${escapeHtml(request?.email || '')}" ${readOnly?'disabled':''}></label>
      <label>Телефон<input name="phone" type="tel" inputmode="numeric" autocomplete="tel" maxlength="16" pattern="\\+7-[0-9]{3}-[0-9]{3}-[0-9]{2}-[0-9]{2}" title="Формат: +7-999-999-99-99" placeholder="+7-999-999-99-99" value="${escapeHtml(request?.phone ? formatRussianPhone(request.phone) : '')}" ${readOnly?'disabled':''}></label>
      <label>Срок исполнения<input name="dueDate" type="date" value="${request?.dueDate || ''}" ${readOnly?'disabled':''}></label>
      <label>Абонент<select name="subscriberId" required ${readOnly?'disabled':''}>${subscriberOptions}</select></label>
      <label>Тип заявки<select name="type" ${readOnly?'disabled':''}>${['NEW_CONNECTION','TARIFF_CHANGE','TECHNICAL_SUPPORT','DISCONNECTION','EQUIPMENT_REPLACEMENT'].map(v=>`<option value="${v}" ${v===(request?.type||'NEW_CONNECTION')?'selected':''}>${escapeHtml(typeLabel(v))}</option>`).join('')}</select></label>
      <label>Тариф<input name="tariffPlan" maxlength="100" value="${escapeHtml(request?.tariffPlan || '')}" ${readOnly?'disabled':''}></label>
      <label>Статус<select name="status" ${readOnly?'disabled':''}>${statusOptions(request?.status)}</select></label>
      <label>Оценка трудозатрат (0–100)<input name="estimatedHours" type="number" min="0" max="100" step="0.01" inputmode="decimal" placeholder="Например, 68.50" value="${request?.estimatedHours ?? ''}" ${readOnly?'disabled':''}></label>
      <label class="inline"><input name="urgent" type="checkbox" ${request?.urgent?'checked':''} ${readOnly?'disabled':''}> Срочная заявка</label>
      <label class="span-2">Теги через запятую<input name="tags" maxlength="1000" value="${escapeHtml((request?.tags || []).join(', '))}" ${readOnly?'disabled':''}></label>
    </div>
    ${readOnly ? '' : '<div class="form-message error" data-form-error hidden></div><button class="primary" type="submit">Сохранить</button>'}`;
}

async function openConnectionRequest(id = null, readOnly = false) {
    const request = id ? await api(`/requests/${id}`) : null;
    const form = $('entity-form');
    form.innerHTML = requestForm(request, readOnly);

    const phoneInput = form.elements.phone;
    if (phoneInput && !readOnly) {
        bindPhoneMask(phoneInput);
    }

    form.onsubmit = readOnly ? null : async event => {
        event.preventDefault();
        clearFormError(form);

        /*
         * reportValidity() запускает нативную browser-валидацию до похода в backend.
         * Так пользователь сразу видит проблему в конкретном поле.
         */
        if (!form.reportValidity()) return;

        const data = new FormData(form);
        const payload = {
            externalId: empty(data.get('externalId')), title: data.get('title'), description: data.get('description'), address: empty(data.get('address')),
            email: empty(data.get('email')), phone: empty(data.get('phone')), dueDate: empty(data.get('dueDate')),
            subscriberId: empty(data.get('subscriberId')), status: data.get('status'), type: data.get('type'), tariffPlan: empty(data.get('tariffPlan')), estimatedHours: empty(data.get('estimatedHours')) ? Number(data.get('estimatedHours')) : null,
            urgent: data.get('urgent') === 'on', tags: String(data.get('tags') || '').split(',').map(v => v.trim()).filter(Boolean), version: request?.version ?? null
        };
        try {
            await api(id ? `/requests/${id}` : '/requests', {method: id ? 'PUT' : 'POST', body: JSON.stringify(payload)});
            $('entity-dialog').close(); toast('Карточка сохранена'); await Promise.all([loadConnectionRequests(), loadDashboard()]);
        } catch (error) {
            showFormError(form, error.message);
        }
    };
    $('entity-dialog').showModal();
}

async function archiveConnectionRequest(id) {
    const request = state.requests.content.find(item => item.id === id);
    const action = request?.archived ? 'восстановить' : 'переместить в архив';
    if (!confirm(`Действительно ${action} карточку?`)) return;
    try {
        await api(request.archived ? `/requests/${id}/restore` : `/requests/${id}`, {method: request.archived ? 'POST' : 'DELETE'});
        toast('Операция выполнена'); await Promise.all([loadConnectionRequests(), loadDashboard()]);
    } catch (error) { toast(error.message, true); }
}

async function openNotes(requestId) {
    state.activeConnectionRequestForNotes = requestId;
    await renderNotes();
    $('notes-dialog').showModal();
}

async function renderNotes() {
    try {
        const list = await api(`/requests/${state.activeConnectionRequestForNotes}/notes`);
        state.notes = list;
        $('note-form').hidden = !canEdit();
        $('notes-list').innerHTML = list.map(note => `<article class="note"><div class="note-meta"><span>${escapeHtml(note.authorName)} · ${formatDate(note.createdAt)}</span><span>${note.pinned ? '📌' : ''}</span></div><p>${escapeHtml(note.body).replace(/\n/g,'<br>')}</p><div class="actions">${canEdit() && (isAdmin() || note.authorId === state.user.id) ? `<button class="secondary" data-note-edit="${note.id}">Изменить</button><button class="danger-outline" data-note-delete="${note.id}">Удалить</button>` : ''}</div></article>`).join('') || '<p class="muted">Заметок пока нет</p>';
    } catch (error) { toast(error.message, true); }
}

async function bulkAction() {
    if (!state.selectedConnectionRequestIds.size) return toast('Выберите хотя бы одну заявку', true);
    const action = prompt('Действие: ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG', 'ARCHIVE');
    if (!action) return;
    const payload = {ids: [...state.selectedConnectionRequestIds], action: action.toUpperCase(), status: null, tag: null};
    if (payload.action === 'SET_STATUS') payload.status = prompt('Статус: NEW, IN_PROGRESS, APPROVED, REJECTED, COMPLETED, CANCELLED', 'NEW');
    if (payload.action === 'ADD_TAG' || payload.action === 'REMOVE_TAG') payload.tag = prompt('Тег');
    try {
        const result = await api('/requests/bulk', {method:'POST', body:JSON.stringify(payload)});
        toast(`Успешно: ${result.succeeded}; ошибок: ${result.failures.length}`); await Promise.all([loadConnectionRequests(), loadDashboard()]);
    } catch (error) { toast(error.message, true); }
}

async function exportCsv() {
    try {
        const response = await api(`/requests/export?q=${encodeURIComponent(state.requests.query)}`);
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a'); link.href = url; link.download = `requests-${new Date().toISOString().slice(0,10)}.csv`; link.click();
        URL.revokeObjectURL(url);
    } catch (error) { toast(error.message, true); }
}

function subscriberForm(subscriber = null) {
    return `<div class="dialog-head"><h3>${subscriber?'Изменение':'Новая'} абонент</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <label>Код<input name="code" required maxlength="50" value="${escapeHtml(subscriber?.code || '')}"></label>
    <label>ФИО абонента<input name="name" required maxlength="255" value="${escapeHtml(subscriber?.name || '')}"></label>
    <label>Телефон<input name="phone" maxlength="32" value="${escapeHtml(subscriber?.phone || '')}"></label>
    <label>Email<input name="email" type="email" maxlength="254" value="${escapeHtml(subscriber?.email || '')}"></label>
    <label>Адрес<input name="address" maxlength="500" value="${escapeHtml(subscriber?.address || '')}"></label>
    <label>Год регистрации<input name="registrationYear" type="number" min="2000" max="2100" value="${subscriber?.registrationYear ?? ''}"></label>
    <label class="inline"><input name="active" type="checkbox" ${subscriber?.active !== false?'checked':''}> Активна</label>
    <button class="primary">Сохранить</button>`;
}

async function openSubscriber(id = null) {
    const subscriber = id ? state.subscribers.find(g => g.id === id) : null;
    const form = $('entity-form'); form.innerHTML = subscriberForm(subscriber);
    form.onsubmit = async event => {
        event.preventDefault(); const data = new FormData(form);
        const payload = {phone:empty(data.get('phone')), email:empty(data.get('email')), address:empty(data.get('address')), code:data.get('code'), name:empty(data.get('name')), registrationYear:empty(data.get('registrationYear'))?Number(data.get('registrationYear')):null, active:data.get('active')==='on', version:subscriber?.version??null};
        try { await api(id?`/subscribers/${id}`:'/subscribers',{method:id?'PUT':'POST',body:JSON.stringify(payload)}); $('entity-dialog').close(); toast('Абонент сохранён'); await loadSubscribers(); }
        catch (error) { toast(error.message,true); }
    }; $('entity-dialog').showModal();
}

async function toggleSubscriber(id) {
    const subscriber = state.subscribers.find(g => g.id === id);
    try { await api(`/subscribers/${id}/active?active=${!subscriber.active}&version=${subscriber.version}`,{method:'PATCH'}); await loadSubscribers(); }
    catch (error) { toast(error.message,true); }
}

async function loadUsers(pageNumber = state.userPage) {
    try {
        const page = await api(`/admin/users?page=${pageNumber}&size=25&sort=email,asc`);
        state.userPage = page.page; renderPager('users',page);
        $('users-body').innerHTML = page.content.map(user => `<tr><td>${escapeHtml(user.email)}</td><td>${escapeHtml(user.displayName)}</td><td>${user.roles.map(r=>`<span class="badge">${escapeHtml(roleLabel(r))}</span>`).join(' ')}</td><td>${user.enabled?'Да':'Нет'}</td><td>${user.passwordChangeRequired?'Да':'Нет'}</td><td><div class="actions"><button class="secondary" data-user-edit="${user.id}">Изменить</button><button class="secondary" data-user-reset="${user.id}">Сбросить пароль</button></div></td></tr>`).join('');
        state.users = page.content;
    } catch (error) { toast(error.message,true); }
}

function userForm(user = null) {
    const checked = role => user?.roles.includes(role) || (!user && role==='VIEWER');
    return `<div class="dialog-head"><h3>${user?'Изменение':'Новый'} пользователь</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <label>Email<input name="email" type="email" required maxlength="254" value="${escapeHtml(user?.email||'')}"></label>
    <label>Отображаемое имя<input name="displayName" required maxlength="160" value="${escapeHtml(user?.displayName||'')}"></label>
    ${user?'': '<label>Временный пароль<input name="temporaryPassword" type="password" required minlength="12" maxlength="128"></label>'}
    <fieldset><legend>Роли</legend>${['VIEWER','OPERATOR','ADMIN'].map(r=>`<label class="inline"><input type="checkbox" name="roles" value="${r}" ${checked(r)?'checked':''}> ${escapeHtml(roleLabel(r))}</label>`).join('')}</fieldset>
    ${user?`<label class="inline"><input type="checkbox" name="enabled" ${user.enabled?'checked':''}> Активен</label>`:''}
    <button class="primary">Сохранить</button>`;
}

async function openUser(id = null) {
    const user = id ? state.users.find(u=>u.id===id) : null;
    const form=$('entity-form'); form.innerHTML=userForm(user);
    form.onsubmit=async event=>{event.preventDefault(); const data=new FormData(form); const payload={email:data.get('email'),displayName:data.get('displayName'),roles:data.getAll('roles'),...(user?{enabled:data.get('enabled')==='on',version:user.version}:{temporaryPassword:data.get('temporaryPassword')})};
        try{await api(id?`/admin/users/${id}`:'/admin/users',{method:id?'PUT':'POST',body:JSON.stringify(payload)});$('entity-dialog').close();toast('Пользователь сохранён');await loadUsers();}catch(error){toast(error.message,true);}};
    $('entity-dialog').showModal();
}

async function resetUserPassword(id) {
    const password=prompt('Новый временный пароль (не менее 12 символов, регистры, цифра и спецсимвол)'); if(!password)return;
    try{await api(`/admin/users/${id}/reset-password`,{method:'POST',body:JSON.stringify({temporaryPassword:password})});toast('Пароль сброшен');await loadUsers();}catch(error){toast(error.message,true);}
}

async function loadAudit(pageNumber = state.auditPage) {
    try {
        const q=encodeURIComponent($('audit-search').value.trim()); const page=await api(`/admin/audit?q=${q}&page=${pageNumber}&size=25&sort=${encodeURIComponent($('audit-sort').value)}`);
        state.auditPage = page.page; renderPager('audit',page);
        $('audit-body').innerHTML=page.content.map(item=>`<tr><td>${formatDate(item.occurredAt)}</td><td>${escapeHtml(item.actorEmail||'system')}</td><td>${escapeHtml(item.eventType)}</td><td>${escapeHtml(item.entityType||'—')}</td><td>${escapeHtml(item.summary)}</td><td><code>${escapeHtml(item.correlationId||'—')}</code></td></tr>`).join('');
    } catch(error){toast(error.message,true);}
}

function openPasswordDialog(required=false) {
    return new Promise(resolve => {
        const dialog = $('entity-dialog');
        const form = $('entity-form');

        const currentPasswordField = required
            ? ''
            : '<label>Текущий пароль<input name="current" type="password" required maxlength="128" autocomplete="current-password"></label>';

        form.innerHTML = `<div class="dialog-head"><h3>${required ? 'Установка постоянного пароля' : 'Смена пароля'}</h3>${required ? '' : '<button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button>'}</div>
            ${currentPasswordField}
            <label>Новый пароль<input name="next" type="password" required minlength="12" maxlength="128" autocomplete="new-password"></label>
            <label>Повторите новый пароль<input name="confirm" type="password" required minlength="12" maxlength="128" autocomplete="new-password"></label>
            <p class="muted">Минимум 12 символов: верхний/нижний регистр, цифра и специальный символ.</p>
            <button class="primary">${required ? 'Установить пароль' : 'Изменить'}</button>`;

        let completed = false;

        const onClose = () => {
            dialog.removeEventListener('close', onClose);
            if (!completed) resolve(false);
        };
        dialog.addEventListener('close', onClose);

        form.onsubmit = async event => {
            event.preventDefault();

            const data = new FormData(form);
            const next = String(data.get('next') || '');
            const confirm = String(data.get('confirm') || '');

            if (next !== confirm) {
                toast('Новые пароли не совпадают', true);
                return;
            }

            try {
                if (required) {
                    await api('/auth/change-initial-password', {
                        method: 'POST',
                        body: JSON.stringify({newPassword: next})
                    });

                    /*
                     * Access JWT был выпущен до смены пароля и ещё содержит
                     * passwordChangeRequired=true. Refresh-cookie ротируется, после чего
                     * клиент получает новый access JWT с актуальным значением из БД.
                     */
                    const refreshed = await refreshAccessToken();
                    if (!refreshed) {
                        completed = true;
                        dialog.close();
                        showLogin();
                        toast('Пароль изменён, но сессию не удалось обновить. Войдите новым паролем.', true);
                        resolve(false);
                        return;
                    }

                    state.user.passwordChangeRequired = false;
                    completed = true;
                    dialog.close();
                    toast('Постоянный пароль установлен');
                    resolve(true);
                    return;
                }

                await api('/auth/change-password', {
                    method: 'POST',
                    body: JSON.stringify({
                        currentPassword: data.get('current'),
                        newPassword: next
                    })
                });

                completed = true;
                dialog.close();
                toast('Пароль изменён. Выполните вход заново.');
                resolve(true);
                await logout();
            } catch (error) {
                toast(error.message, true);
            }
        };

        dialog.showModal();
    });
}

async function logout() {
    try { await fetch('/api/auth/logout',{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:'{}'}); } catch (_) {}
    showLogin();
}

function bindEvents() {
    $('login-form').addEventListener('submit', async event => {
        event.preventDefault();

        const form = $('login-form');
        const button = form.querySelector('button[type="submit"]');
        const errorNode = $('login-error');

        errorNode.hidden = true;
        button.disabled = true;
        const originalCaption = button.textContent;
        button.textContent = 'Входим…';

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                credentials: 'include',
                headers: {'Content-Type':'application/json'},
                body: JSON.stringify({
                    email: $('login-email').value,
                    password: $('login-password').value,
                    clientType: 'WEB'
                })
            });

            if (!response.ok) {
                throw new Error(await parseError(response));
            }

            const result = await response.json();
            state.accessToken = result.accessToken;
            state.user = result.user;
            $('login-password').value = '';

            await enterApplication();
        } catch (error) {
            /*
             * Если ошибка возникает после успешного HTTP login, экран входа снова
             * открывается и сообщение выводится в видимой форме.
             */
            console.error('[ИС интернет-провайдера] Login flow failed', error);
            showLogin();
            errorNode.textContent = error.message || 'Не удалось выполнить вход';
            errorNode.hidden = false;
        } finally {
            button.disabled = false;
            button.textContent = originalCaption;
        }
    });
    $('navigation').addEventListener('click',event=>{const button=event.target.closest('[data-tab]');if(button&&!button.hidden)switchTab(button.dataset.tab);});
    $('logout').addEventListener('click',logout); $('change-password').addEventListener('click',()=>openPasswordDialog(false));
    $('search-requests').addEventListener('click',()=>{state.requests.query=$('request-search').value.trim();state.requests.page=0;loadConnectionRequests(0);});
    $('request-sort').addEventListener('change',()=>loadConnectionRequests(0));
    $('request-size').addEventListener('change',()=>{state.requests.size=Number($('request-size').value);loadConnectionRequests(0);});
    $('csv-file').addEventListener('change',()=>{$('csv-apply').disabled=true;});
    $('csv-mode').addEventListener('change',()=>{$('csv-apply').disabled=true;});
    $('request-search').addEventListener('keydown',event=>{if(event.key==='Enter')$('search-requests').click();});
    $('requests-prev').addEventListener('click',()=>loadConnectionRequests(state.requests.page-1)); $('requests-next').addEventListener('click',()=>loadConnectionRequests(state.requests.page+1));
    $('add-request').addEventListener('click',()=>openConnectionRequest()); $('bulk-action').addEventListener('click',bulkAction); $('export-csv').addEventListener('click',exportCsv);
    $('import-csv').addEventListener('click',()=>{$('csv-result').textContent='';$('csv-apply').disabled=true;$('csv-dialog').showModal();});
    $('add-subscriber').addEventListener('click',()=>openSubscriber()); $('add-user').addEventListener('click',()=>openUser()); $('search-audit').addEventListener('click',()=>loadAudit(0));
    $('audit-sort').addEventListener('change',()=>loadAudit(0));
    $('users-prev').addEventListener('click',()=>loadUsers(state.userPage-1));
    $('users-next').addEventListener('click',()=>loadUsers(state.userPage+1));
    $('audit-prev').addEventListener('click',()=>loadAudit(state.auditPage-1));
    $('audit-next').addEventListener('click',()=>loadAudit(state.auditPage+1));
    $('requests-body').addEventListener('click',event=>handleConnectionRequestTableClick(event));
    $('requests-body').addEventListener('change',event=>{const checkbox=event.target.closest('[data-request-select]');if(checkbox){checkbox.checked?state.selectedConnectionRequestIds.add(checkbox.dataset.requestSelect):state.selectedConnectionRequestIds.delete(checkbox.dataset.requestSelect);}});
    $('select-all-requests').addEventListener('change',event=>{document.querySelectorAll('[data-request-select]').forEach(cb=>{cb.checked=event.target.checked;event.target.checked?state.selectedConnectionRequestIds.add(cb.dataset.requestSelect):state.selectedConnectionRequestIds.delete(cb.dataset.requestSelect);});});
    $('subscribers-body').addEventListener('click',event=>{const edit=event.target.closest('[data-subscriber-edit]');const toggle=event.target.closest('[data-subscriber-toggle]');if(edit)openSubscriber(edit.dataset.subscriberEdit);if(toggle)toggleSubscriber(toggle.dataset.subscriberToggle);});
    $('users-body').addEventListener('click',event=>{const edit=event.target.closest('[data-user-edit]');const reset=event.target.closest('[data-user-reset]');if(edit)openUser(edit.dataset.userEdit);if(reset)resetUserPassword(reset.dataset.userReset);});
    $('note-form').addEventListener('submit',async event=>{event.preventDefault();try{await api(`/requests/${state.activeConnectionRequestForNotes}/notes`,{method:'POST',body:JSON.stringify({body:$('note-body').value,pinned:$('note-pinned').checked,version:null})});$('note-body').value='';$('note-pinned').checked=false;await renderNotes();}catch(error){toast(error.message,true);}});
    $('notes-list').addEventListener('click',async event=>{const edit=event.target.closest('[data-note-edit]');if(edit){const note=state.notes.find(n=>n.id===edit.dataset.noteEdit);const body=prompt('Текст заметки',note.body);if(body===null)return;const pinned=confirm('Закрепить заметку?');try{await api(`/requests/${state.activeConnectionRequestForNotes}/notes/${note.id}`,{method:'PUT',body:JSON.stringify({body,pinned,version:note.version})});await renderNotes();}catch(error){toast(error.message,true);}return;}const button=event.target.closest('[data-note-delete]');if(button&&confirm('Удалить заметку?')){try{await api(`/requests/${state.activeConnectionRequestForNotes}/notes/${button.dataset.noteDelete}`,{method:'DELETE'});await renderNotes();}catch(error){toast(error.message,true);}}});
    $('csv-preview').addEventListener('click',()=>runCsv(false)); $('csv-apply').addEventListener('click',()=>runCsv(true));
    document.body.addEventListener('click',event=>{const close=event.target.closest('[data-close-dialog]');if(close)$(close.dataset.closeDialog).close();});
}

function handleConnectionRequestTableClick(event) {
    const view=event.target.closest('[data-request-view]'), edit=event.target.closest('[data-request-edit]'), notes=event.target.closest('[data-request-notes]'), archive=event.target.closest('[data-request-archive]');
    if(view)openConnectionRequest(view.dataset.requestView,true); if(edit)openConnectionRequest(edit.dataset.requestEdit,false); if(notes)openNotes(notes.dataset.requestNotes); if(archive)archiveConnectionRequest(archive.dataset.requestArchive);
}

async function runCsv(apply) {
    const file=$('csv-file').files[0]; if(!file)return toast('Выберите CSV-файл',true);
    const form=new FormData();form.append('file',file);
    try{const result=await api(`/requests/import/${apply?'apply':'preview'}?mode=${$('csv-mode').value}`,{method:'POST',body:form});$('csv-result').textContent=JSON.stringify(result,null,2);$('csv-apply').disabled=apply?true:result.invalidRows>0;if(apply){toast('Импорт завершён');await Promise.all([loadConnectionRequests(),loadDashboard()]);}}catch(error){toast(error.message,true);$('csv-result').textContent=error.message;}
}

function statusOptions(selected='NEW'){return ['NEW','IN_PROGRESS','APPROVED','REJECTED','COMPLETED','CANCELLED'].map(v=>`<option value="${v}" ${v===selected?'selected':''}>${statusLabel(v)}</option>`).join('');}
function statusLabel(value){return ({NEW:'Новая',IN_PROGRESS:'В работе',APPROVED:'Согласована',REJECTED:'Отклонена',COMPLETED:'Выполнена',CANCELLED:'Отменена'})[value]||value;}
function priorityLabel(value){return ({NOT_APPLICABLE:'Не применяется',LOW:'Низкий',MEDIUM:'Средний',HIGH:'Высокий',CRITICAL:'Критический'})[value]||value;}
function formatDate(value){return value?new Intl.DateTimeFormat('ru-RU',{dateStyle:'short',timeStyle:'short'}).format(new Date(value)):'—';}
function empty(value){const normalized=String(value??'').trim();return normalized||null;}

boot().catch(error => {
    console.error('[ИС интернет-провайдера] Boot failed', error);
    showLogin();
    toast(error.message || 'Не удалось инициализировать приложение', true);
});

function typeLabel(v){return ({NEW_CONNECTION:'Подключение',TARIFF_CHANGE:'Смена тарифа',TECHNICAL_SUPPORT:'Техподдержка',DISCONNECTION:'Отключение',EQUIPMENT_REPLACEMENT:'Замена оборудования'})[v]||v;}

function renderPager(prefix,page) {
    $(prefix+'-page').textContent = `Страница ${page.page+1} из ${Math.max(1,page.totalPages)} · записей ${page.totalElements}`;
    $(prefix+'-prev').disabled = page.first;
    $(prefix+'-next').disabled = page.last;
}
