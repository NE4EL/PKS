'use strict';

/**
 * Access token хранится только в памяти вкладки. Refresh token браузер получает
 * в HttpOnly/SameSite cookie.
 */
const state = {
    accessToken: null,
    user: null,
    groups: [],
    students: { page: 0, size: 25, totalPages: 0, query: '', content: [] },
    selectedStudentIds: new Set(),
    activeStudentForNotes: null,
    refreshPromise: null
};

const $ = (id) => document.getElementById(id);
const escapeHtml = (value) => String(value ?? '').replace(/[&<>'"]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[ch]));
const roles = () => new Set((state.user?.roles || []).map(String));
const isAdmin = () => roles().has('ADMIN');
const canEdit = () => roles().has('TEACHER') || isAdmin();


/**
 * Коды ролей в API остаются английскими; в интерфейсе используются русские названия.
 */
function roleLabel(value) {
    return ({
        ADMIN: 'Администратор',
        TEACHER: 'Преподаватель',
        VIEWER: 'Наблюдатель'
    })[value] || value;
}

/** Русские имена полей для ошибок Bean Validation. */
function fieldLabel(value) {
    return ({
        externalId: 'внешний ID',
        firstName: 'имя',
        lastName: 'фамилия',
        middleName: 'отчество',
        email: 'email',
        phone: 'телефон',
        birthDate: 'дата рождения',
        groupId: 'группа',
        status: 'статус',
        averageGrade: 'средний балл',
        hasDebt: 'задолженность',
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
        await Promise.all([loadGroups(), loadDashboard()]);
    } catch (error) {
        console.error('[Student Hub] Initial data loading failed', error);
        toast(`Вход выполнен, но часть данных не загрузилась: ${error.message}`, true);
    }
}

function applyRoleVisibility() {
    document.querySelectorAll('.admin-only').forEach(node => node.hidden = !isAdmin());
    document.querySelectorAll('.teacher-only').forEach(node => node.hidden = !canEdit());
}

function switchTab(name) {
    document.querySelectorAll('.tab').forEach(tab => tab.classList.toggle('active', tab.id === `tab-${name}`));
    document.querySelectorAll('.nav-item').forEach(button => button.classList.toggle('active', button.dataset.tab === name));
    const titles = {
        dashboard: ['Аналитика','Сводная картина по студентам'],
        students: ['Студенты','Умный поиск, карточки и массовые операции'],
        groups: ['Учебные группы','Единый справочник групп'],
        users: ['Пользователи','Роли, блокировка и временные пароли'],
        audit: ['Журнал аудита','История действий пользователей']
    };
    [$('page-title').textContent, $('page-subtitle').textContent] = titles[name];
    if (name === 'students') loadStudents();
    if (name === 'groups') renderGroups();
    if (name === 'users' && isAdmin()) loadUsers();
    if (name === 'audit' && isAdmin()) loadAudit();
    if (name === 'dashboard') loadDashboard();
}

async function loadDashboard() {
    try {
        const data = await api('/analytics/summary');
        const metrics = [
            ['Всего', data.total], ['Активные', data.active], ['В архиве', data.archived],
            ['С задолженностью', data.withDebt], ['Средний балл', data.averageGrade ?? '—']
        ];
        $('metric-grid').innerHTML = metrics.map(([name,value]) => `<article class="metric card"><span class="muted">${name}</span><strong>${value}</strong></article>`).join('');
        renderBars('chart-groups', data.byGroup);
        renderBars('chart-status', data.byStatus);
        renderBars('chart-risk', data.byRisk);
    } catch (error) { toast(error.message, true); }
}

function renderBars(id, values) {
    const max = Math.max(1, ...values.map(item => item.count));

    const displayName = value => {
        if (id === 'chart-status') return statusLabel(value);
        if (id === 'chart-risk') return riskLabel(value);
        return value;
    };

    $(id).innerHTML = values.length
        ? values.map(item => `<div class="bar-row"><span>${escapeHtml(displayName(item.name))}</span><progress value="${item.count}" max="${max}"></progress><strong>${item.count}</strong></div>`).join('')
        : '<p class="muted">Нет данных</p>';
}

async function loadGroups() {
    try { state.groups = await api('/groups?activeOnly=false'); renderGroups(); }
    catch (error) { toast(error.message, true); }
}

function renderGroups() {
    const body = $('groups-body');
    if (!body) return;
    body.innerHTML = state.groups.map(group => `<tr>
        <td><strong>${escapeHtml(group.code)}</strong></td><td>${escapeHtml(group.name || '—')}</td><td>${group.admissionYear ?? '—'}</td>
        <td><span class="badge ${group.active ? 'success' : ''}">${group.active ? 'Да' : 'Нет'}</span></td>
        <td class="admin-only"><div class="actions"><button class="secondary" data-group-edit="${group.id}">Изменить</button><button class="secondary" data-group-toggle="${group.id}">${group.active ? 'Отключить' : 'Включить'}</button></div></td>
    </tr>`).join('');
    applyRoleVisibility();
}

async function loadStudents(page = state.students.page) {
    try {
        const q = encodeURIComponent(state.students.query);
        const result = await api(`/students?q=${q}&page=${page}&size=${state.students.size}&sort=name,asc`);
        state.students = {...state.students, ...result};
        state.selectedStudentIds.clear();
        renderStudents();
    } catch (error) { toast(error.message, true); }
}

function renderStudents() {
    $('students-body').innerHTML = state.students.content.map(student => `<tr class="${student.archived ? 'archived' : ''}">
        <td class="teacher-only"><input type="checkbox" data-student-select="${student.id}"></td>
        <td><strong>${escapeHtml(student.fullName)}</strong><br><small class="muted">${escapeHtml(student.externalId || '')}</small></td>
        <td>${escapeHtml(student.groupCode || '—')}</td><td>${escapeHtml(statusLabel(student.status))}</td>
        <td>${student.averageGrade ?? '—'}</td><td>${student.hasDebt ? '<span class="badge warning">Да</span>' : 'Нет'}</td>
        <td><span class="badge risk-${student.riskLevel}">${escapeHtml(riskLabel(student.riskLevel))}</span></td>
        <td>${student.tags.map(tag => `<span class="badge">${escapeHtml(tag)}</span>`).join(' ') || '—'}</td>
        <td><div class="actions">
            <button class="secondary" data-student-view="${student.id}">Карточка</button>
            ${canEdit() ? `<button class="secondary" data-student-edit="${student.id}">Изменить</button><button class="secondary" data-student-notes="${student.id}">Заметки</button><button class="danger-outline" data-student-archive="${student.id}">${student.archived ? 'Восстановить' : 'В архив'}</button>` : ''}
        </div></td>
    </tr>`).join('') || '<tr><td colspan="9" class="muted">Студенты не найдены</td></tr>';
    $('students-page').textContent = `Страница ${state.students.page + 1} из ${Math.max(1,state.students.totalPages)} · записей ${state.students.totalElements}`;
    $('students-prev').disabled = state.students.first;
    $('students-next').disabled = state.students.last;
    $('select-all-students').checked = false;
    applyRoleVisibility();
}

function studentForm(student = null, readOnly = false) {
    const groupOptions = ['<option value="">Без группы</option>', ...state.groups.filter(g => g.active || g.id === student?.groupId).map(g => `<option value="${g.id}" ${g.id === student?.groupId ? 'selected' : ''}>${escapeHtml(g.code)}</option>`)].join('');
    return `<div class="dialog-head"><h3>${student ? (readOnly ? 'Карточка студента' : 'Изменение студента') : 'Новый студент'}</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <div class="form-grid">
      <label>Фамилия<input name="lastName" required maxlength="100" value="${escapeHtml(student?.lastName || '')}" ${readOnly?'disabled':''}></label>
      <label>Имя<input name="firstName" required maxlength="100" value="${escapeHtml(student?.firstName || '')}" ${readOnly?'disabled':''}></label>
      <label>Отчество<input name="middleName" maxlength="100" value="${escapeHtml(student?.middleName || '')}" ${readOnly?'disabled':''}></label>
      <label>External ID<input name="externalId" maxlength="100" value="${escapeHtml(student?.externalId || '')}" ${readOnly?'disabled':''}></label>
      <label>Email<input name="email" type="email" maxlength="254" value="${escapeHtml(student?.email || '')}" ${readOnly?'disabled':''}></label>
      <label>Телефон<input name="phone" type="tel" inputmode="numeric" autocomplete="tel" maxlength="16" pattern="\\+7-[0-9]{3}-[0-9]{3}-[0-9]{2}-[0-9]{2}" title="Формат: +7-999-999-99-99" placeholder="+7-999-999-99-99" value="${escapeHtml(student?.phone ? formatRussianPhone(student.phone) : '')}" ${readOnly?'disabled':''}></label>
      <label>Дата рождения<input name="birthDate" type="date" value="${student?.birthDate || ''}" ${readOnly?'disabled':''}></label>
      <label>Группа<select name="groupId" ${readOnly?'disabled':''}>${groupOptions}</select></label>
      <label>Статус<select name="status" ${readOnly?'disabled':''}>${statusOptions(student?.status)}</select></label>
      <label>Средний балл (0–100)<input name="averageGrade" type="number" min="0" max="100" step="0.01" inputmode="decimal" placeholder="Например, 68.50" value="${student?.averageGrade ?? ''}" ${readOnly?'disabled':''}></label>
      <label class="inline"><input name="hasDebt" type="checkbox" ${student?.hasDebt?'checked':''} ${readOnly?'disabled':''}> Есть задолженность</label>
      <label class="span-2">Теги через запятую<input name="tags" maxlength="1000" value="${escapeHtml((student?.tags || []).join(', '))}" ${readOnly?'disabled':''}></label>
    </div>
    ${readOnly ? '' : '<div class="form-message error" data-form-error hidden></div><button class="primary" type="submit">Сохранить</button>'}`;
}

async function openStudent(id = null, readOnly = false) {
    const student = id ? await api(`/students/${id}`) : null;
    const form = $('entity-form');
    form.innerHTML = studentForm(student, readOnly);

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
            externalId: empty(data.get('externalId')), firstName: data.get('firstName'), lastName: data.get('lastName'), middleName: empty(data.get('middleName')),
            email: empty(data.get('email')), phone: empty(data.get('phone')), birthDate: empty(data.get('birthDate')),
            groupId: empty(data.get('groupId')), status: data.get('status'), averageGrade: empty(data.get('averageGrade')) ? Number(data.get('averageGrade')) : null,
            hasDebt: data.get('hasDebt') === 'on', tags: String(data.get('tags') || '').split(',').map(v => v.trim()).filter(Boolean), version: student?.version ?? null
        };
        try {
            await api(id ? `/students/${id}` : '/students', {method: id ? 'PUT' : 'POST', body: JSON.stringify(payload)});
            $('entity-dialog').close(); toast('Карточка сохранена'); await Promise.all([loadStudents(), loadDashboard()]);
        } catch (error) {
            showFormError(form, error.message);
        }
    };
    $('entity-dialog').showModal();
}

async function archiveStudent(id) {
    const student = state.students.content.find(item => item.id === id);
    const action = student?.archived ? 'восстановить' : 'переместить в архив';
    if (!confirm(`Действительно ${action} карточку?`)) return;
    try {
        await api(student.archived ? `/students/${id}/restore` : `/students/${id}`, {method: student.archived ? 'POST' : 'DELETE'});
        toast('Операция выполнена'); await Promise.all([loadStudents(), loadDashboard()]);
    } catch (error) { toast(error.message, true); }
}

async function openNotes(studentId) {
    state.activeStudentForNotes = studentId;
    await renderNotes();
    $('notes-dialog').showModal();
}

async function renderNotes() {
    try {
        const list = await api(`/students/${state.activeStudentForNotes}/notes`);
        $('notes-list').innerHTML = list.map(note => `<article class="note"><div class="note-meta"><span>${escapeHtml(note.authorName)} · ${formatDate(note.createdAt)}</span><span>${note.pinned ? '📌' : ''}</span></div><p>${escapeHtml(note.body).replace(/\n/g,'<br>')}</p><div class="actions"><button class="danger-outline" data-note-delete="${note.id}">Удалить</button></div></article>`).join('') || '<p class="muted">Заметок пока нет</p>';
    } catch (error) { toast(error.message, true); }
}

async function bulkAction() {
    if (!state.selectedStudentIds.size) return toast('Выберите хотя бы одного студента', true);
    const action = prompt('Действие: ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG', 'ARCHIVE');
    if (!action) return;
    const payload = {ids: [...state.selectedStudentIds], action: action.toUpperCase(), status: null, tag: null};
    if (payload.action === 'SET_STATUS') payload.status = prompt('Статус: ENROLLED, ACADEMIC_LEAVE, EXPELLED, GRADUATED, TRANSFERRED', 'ENROLLED');
    if (payload.action === 'ADD_TAG' || payload.action === 'REMOVE_TAG') payload.tag = prompt('Тег');
    try {
        const result = await api('/students/bulk', {method:'POST', body:JSON.stringify(payload)});
        toast(`Успешно: ${result.succeeded}; ошибок: ${result.failures.length}`); await Promise.all([loadStudents(), loadDashboard()]);
    } catch (error) { toast(error.message, true); }
}

async function exportCsv() {
    try {
        const response = await api(`/students/export?q=${encodeURIComponent(state.students.query)}`);
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a'); link.href = url; link.download = `students-${new Date().toISOString().slice(0,10)}.csv`; link.click();
        URL.revokeObjectURL(url);
    } catch (error) { toast(error.message, true); }
}

function groupForm(group = null) {
    return `<div class="dialog-head"><h3>${group?'Изменение':'Новая'} группа</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <label>Код<input name="code" required maxlength="50" value="${escapeHtml(group?.code || '')}"></label>
    <label>Название<input name="name" maxlength="200" value="${escapeHtml(group?.name || '')}"></label>
    <label>Год поступления<input name="admissionYear" type="number" min="2000" max="2100" value="${group?.admissionYear ?? ''}"></label>
    <label class="inline"><input name="active" type="checkbox" ${group?.active !== false?'checked':''}> Активна</label>
    <button class="primary">Сохранить</button>`;
}

async function openGroup(id = null) {
    const group = id ? state.groups.find(g => g.id === id) : null;
    const form = $('entity-form'); form.innerHTML = groupForm(group);
    form.onsubmit = async event => {
        event.preventDefault(); const data = new FormData(form);
        const payload = {code:data.get('code'), name:empty(data.get('name')), admissionYear:empty(data.get('admissionYear'))?Number(data.get('admissionYear')):null, active:data.get('active')==='on', version:group?.version??null};
        try { await api(id?`/groups/${id}`:'/groups',{method:id?'PUT':'POST',body:JSON.stringify(payload)}); $('entity-dialog').close(); toast('Группа сохранена'); await loadGroups(); }
        catch (error) { toast(error.message,true); }
    }; $('entity-dialog').showModal();
}

async function toggleGroup(id) {
    const group = state.groups.find(g => g.id === id);
    try { await api(`/groups/${id}/active?active=${!group.active}&version=${group.version}`,{method:'PATCH'}); await loadGroups(); }
    catch (error) { toast(error.message,true); }
}

async function loadUsers() {
    try {
        const page = await api('/admin/users?page=0&size=100&sort=email,asc');
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
    <fieldset><legend>Роли</legend>${['VIEWER','TEACHER','ADMIN'].map(r=>`<label class="inline"><input type="checkbox" name="roles" value="${r}" ${checked(r)?'checked':''}> ${escapeHtml(roleLabel(r))}</label>`).join('')}</fieldset>
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

async function loadAudit() {
    try {
        const q=encodeURIComponent($('audit-search').value.trim()); const page=await api(`/admin/audit?q=${q}&page=0&size=100&sort=occurred,desc`);
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
            console.error('[Student Hub] Login flow failed', error);
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
    $('search-students').addEventListener('click',()=>{state.students.query=$('student-search').value.trim();state.students.page=0;loadStudents(0);});
    $('student-search').addEventListener('keydown',event=>{if(event.key==='Enter')$('search-students').click();});
    $('students-prev').addEventListener('click',()=>loadStudents(state.students.page-1)); $('students-next').addEventListener('click',()=>loadStudents(state.students.page+1));
    $('add-student').addEventListener('click',()=>openStudent()); $('bulk-action').addEventListener('click',bulkAction); $('export-csv').addEventListener('click',exportCsv);
    $('import-csv').addEventListener('click',()=>{$('csv-result').textContent='';$('csv-apply').disabled=true;$('csv-dialog').showModal();});
    $('add-group').addEventListener('click',()=>openGroup()); $('add-user').addEventListener('click',()=>openUser()); $('search-audit').addEventListener('click',loadAudit);
    $('students-body').addEventListener('click',event=>handleStudentTableClick(event));
    $('students-body').addEventListener('change',event=>{const checkbox=event.target.closest('[data-student-select]');if(checkbox){checkbox.checked?state.selectedStudentIds.add(checkbox.dataset.studentSelect):state.selectedStudentIds.delete(checkbox.dataset.studentSelect);}});
    $('select-all-students').addEventListener('change',event=>{document.querySelectorAll('[data-student-select]').forEach(cb=>{cb.checked=event.target.checked;event.target.checked?state.selectedStudentIds.add(cb.dataset.studentSelect):state.selectedStudentIds.delete(cb.dataset.studentSelect);});});
    $('groups-body').addEventListener('click',event=>{const edit=event.target.closest('[data-group-edit]');const toggle=event.target.closest('[data-group-toggle]');if(edit)openGroup(edit.dataset.groupEdit);if(toggle)toggleGroup(toggle.dataset.groupToggle);});
    $('users-body').addEventListener('click',event=>{const edit=event.target.closest('[data-user-edit]');const reset=event.target.closest('[data-user-reset]');if(edit)openUser(edit.dataset.userEdit);if(reset)resetUserPassword(reset.dataset.userReset);});
    $('note-form').addEventListener('submit',async event=>{event.preventDefault();try{await api(`/students/${state.activeStudentForNotes}/notes`,{method:'POST',body:JSON.stringify({body:$('note-body').value,pinned:$('note-pinned').checked,version:null})});$('note-body').value='';$('note-pinned').checked=false;await renderNotes();}catch(error){toast(error.message,true);}});
    $('notes-list').addEventListener('click',async event=>{const button=event.target.closest('[data-note-delete]');if(button&&confirm('Удалить заметку?')){try{await api(`/students/${state.activeStudentForNotes}/notes/${button.dataset.noteDelete}`,{method:'DELETE'});await renderNotes();}catch(error){toast(error.message,true);}}});
    $('csv-preview').addEventListener('click',()=>runCsv(false)); $('csv-apply').addEventListener('click',()=>runCsv(true));
    document.body.addEventListener('click',event=>{const close=event.target.closest('[data-close-dialog]');if(close)$(close.dataset.closeDialog).close();});
}

function handleStudentTableClick(event) {
    const view=event.target.closest('[data-student-view]'), edit=event.target.closest('[data-student-edit]'), notes=event.target.closest('[data-student-notes]'), archive=event.target.closest('[data-student-archive]');
    if(view)openStudent(view.dataset.studentView,true); if(edit)openStudent(edit.dataset.studentEdit,false); if(notes)openNotes(notes.dataset.studentNotes); if(archive)archiveStudent(archive.dataset.studentArchive);
}

async function runCsv(apply) {
    const file=$('csv-file').files[0]; if(!file)return toast('Выберите CSV-файл',true);
    const form=new FormData();form.append('file',file);
    try{const result=await api(`/students/import/${apply?'apply':'preview'}?mode=${$('csv-mode').value}`,{method:'POST',body:form});$('csv-result').textContent=JSON.stringify(result,null,2);$('csv-apply').disabled=apply?true:result.invalidRows>0;if(apply){toast('Импорт завершён');await Promise.all([loadStudents(),loadDashboard()]);}}catch(error){toast(error.message,true);$('csv-result').textContent=error.message;}
}

function statusOptions(selected='ENROLLED'){return ['ENROLLED','ACADEMIC_LEAVE','EXPELLED','GRADUATED','TRANSFERRED'].map(v=>`<option value="${v}" ${v===selected?'selected':''}>${statusLabel(v)}</option>`).join('');}
function statusLabel(value){return ({ENROLLED:'Обучается',ACADEMIC_LEAVE:'Академический отпуск',EXPELLED:'Отчислен',GRADUATED:'Выпускник',TRANSFERRED:'Переведён'})[value]||value;}
function riskLabel(value){return ({NOT_APPLICABLE:'Не применяется',LOW:'Низкий',MEDIUM:'Средний',HIGH:'Высокий',CRITICAL:'Критический'})[value]||value;}
function formatDate(value){return value?new Intl.DateTimeFormat('ru-RU',{dateStyle:'short',timeStyle:'short'}).format(new Date(value)):'—';}
function empty(value){const normalized=String(value??'').trim();return normalized||null;}

boot().catch(error => {
    console.error('[Student Hub] Boot failed', error);
    showLogin();
    toast(error.message || 'Не удалось инициализировать приложение', true);
});
