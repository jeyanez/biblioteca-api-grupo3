'use strict';

const state = { section: 'libros', libros: [], autores: [], usuarios: [], prestamos: [], search: '', filter: 'todos', connected: false };
const config = {
  libros: { label: 'Libros', title: 'Catálogo de libros', description: 'Todos los títulos y su disponibilidad, en un mismo lugar.', singular: 'libro', search: 'Buscar por título, ISBN o autor…', columns: ['Título', 'Autor', 'ISBN', 'Disponibilidad', 'Acciones'], empty: 'El próximo capítulo empieza aquí.', hint: 'Registra primero un autor y después añade el primer libro a tu catálogo.' },
  prestamos: { label: 'Préstamos', title: 'Préstamos y devoluciones', description: 'Cada ejemplar, cada lector y su fecha de devolución.', singular: 'préstamo', search: 'Buscar por libro o lector…', columns: ['Libro', 'Lector', 'Fecha de préstamo', 'Devolución prevista', 'Estado', 'Acciones'], empty: 'Los libros están listos para salir.', hint: 'Registra un lector y un libro con ejemplares disponibles para comenzar.' },
  autores: { label: 'Autores', title: 'Autores del catálogo', description: 'Las personas detrás de cada historia.', singular: 'autor', search: 'Buscar por nombre o nacionalidad…', columns: ['Nombre', 'Nacionalidad', 'Libros', 'Acciones'], empty: 'Demos nombre a las historias.', hint: 'Añade un autor para empezar a organizar los libros de la biblioteca.' },
  usuarios: { label: 'Usuarios', title: 'Comunidad de lectores', description: 'Un espacio para quienes dan vida a la biblioteca.', singular: 'usuario', search: 'Buscar por nombre, cédula o correo…', columns: ['Nombre', 'Cédula', 'Correo electrónico', 'Pendientes', 'Acciones'], empty: 'Una comunidad por construir.', hint: 'Registra al primer lector para que pueda solicitar libros prestados.' }
};
const $ = (selector) => document.querySelector(selector);
const dialog = $('#record-dialog');
let modal = null;
let submitting = false;
let refreshTask = null;

function node(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
}
function normalized(value) { return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase(); }
function lookup(collection, id, field) { return state[collection].find(item => item.id === id)?.[field] ?? `#${id}`; }
function dateLabel(value) { const [year, month, day] = value.split('-'); return `${day}/${month}/${year}`; }
function isoDate(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
function notify(message, error = false) {
  const area = $('#notification');
  area.textContent = message;
  area.className = `notification${error ? ' error' : ''}`;
  area.hidden = false;
}
async function api(path, options = {}) {
  let response;
  try {
    response = await fetch(path, { ...options, headers: { 'Accept': 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers } });
  } catch (_) { throw new Error('No se pudo conectar con la biblioteca. Comprueba que la aplicación siga en ejecución.'); }
  if (response.status === 204) return null;
  let data;
  try { data = await response.json(); } catch (_) { data = null; }
  if (!response.ok) {
    const details = data?.errors ? Object.values(data.errors).join('. ') : '';
    throw new Error(details || data?.message || `No se pudo completar la operación (HTTP ${response.status}).`);
  }
  return data;
}
function refresh(showSuccess = false) {
  // Una escritura debe esperar la carga anterior y solicitar datos nuevos después.
  if (refreshTask) return refreshTask.then(() => refresh(showSuccess), () => refresh(showSuccess));
  refreshTask = refreshData(showSuccess);
  const running = refreshTask;
  const clear = () => { if (refreshTask === running) refreshTask = null; };
  running.then(clear, clear);
  return running;
}
async function refreshData(showSuccess) {
  $('#refresh').disabled = true;
  try {
    const collections = ['libros', 'autores', 'usuarios', 'prestamos'];
    const lists = await Promise.all(collections.map(key => api(`/api/${key}`)));
    collections.forEach((key, index) => { state[key] = lists[index]; });
    state.connected = true;
    $('#connection-dot').className = 'connection-dot online';
    $('#connection-text').textContent = 'Biblioteca conectada';
    render();
    if (showSuccess) notify('La biblioteca está actualizada.');
  } catch (error) {
    state.connected = false;
    $('#connection-dot').className = 'connection-dot offline';
    $('#connection-text').textContent = 'Sin conexión';
    notify(error.message, true);
    if (!state.libros.length && !state.usuarios.length && !state.autores.length && !state.prestamos.length) {
      const empty = node('div', 'empty-state');
      empty.append(node('h3', '', 'No pudimos cargar la biblioteca.'), node('p', '', 'Comprueba la conexión y pulsa Actualizar para volver a intentarlo.'));
      $('#records').replaceChildren(empty);
      $('#record-count').textContent = 'Datos no disponibles';
    }
    throw error;
  } finally { $('#refresh').disabled = false; }
}
function selectSection(section) {
  state.section = section;
  state.search = '';
  $('#search').value = '';
  document.querySelectorAll('[data-section]').forEach(button => {
    const active = button.dataset.section === section;
    button.classList.toggle('active', active);
    if (active) button.setAttribute('aria-current', 'page'); else button.removeAttribute('aria-current');
  });
  render();
}
function render() {
  $('#stat-books').textContent = state.libros.length;
  $('#stat-stock').textContent = state.libros.reduce((total, book) => total + book.stockDisponible, 0);
  $('#stat-loans').textContent = state.prestamos.filter(loan => loan.estado === 'PENDIENTE').length;
  $('#stat-users').textContent = state.usuarios.length;
  const current = config[state.section];
  $('#breadcrumb').textContent = current.label;
  $('#section-title').textContent = current.title;
  $('#section-description').textContent = current.description;
  $('#create-record').textContent = `＋ Nuevo ${current.singular}`;
  $('#search').placeholder = current.search;
  $('#loan-filter-wrapper').hidden = state.section !== 'prestamos';
  const rows = state[state.section].filter(item => {
    if (state.section === 'prestamos' && state.filter !== 'todos' && item.estado !== state.filter) return false;
    let searchText = Object.values(item).join(' ');
    if (state.section === 'libros') searchText += ` ${lookup('autores', item.autorId, 'nombre')}`;
    if (state.section === 'prestamos') searchText += ` ${lookup('libros', item.libroId, 'titulo')} ${lookup('usuarios', item.usuarioId, 'nombre')}`;
    return normalized(searchText).includes(normalized(state.search));
  });
  $('#record-count').textContent = `${rows.length} ${rows.length === 1 ? 'registro' : 'registros'}${rows.length !== state[state.section].length ? ` de ${state[state.section].length}` : ''}`;
  const container = $('#records');
  if (!rows.length) {
    const filtered = Boolean(state.search) || (state.section === 'prestamos' && state.filter !== 'todos');
    const empty = node('div', 'empty-state');
    empty.append(node('div', 'empty-illustration', filtered ? '⌕' : '▤'), node('h3', '', filtered ? 'No hay coincidencias.' : current.empty), node('p', '', filtered ? 'Prueba con otra búsqueda o cambia el filtro para ver más registros.' : current.hint));
    if (!filtered) {
      const button = node('button', 'button secondary', `＋ Añadir ${current.singular}`);
      button.addEventListener('click', () => openForm(state.section));
      empty.append(button);
    }
    container.replaceChildren(empty);
    return;
  }
  const wrap = node('div', 'table-wrap');
  const table = node('table');
  const head = node('thead');
  const headRow = node('tr');
  current.columns.forEach(label => { const th = node('th', '', label); th.scope = 'col'; headRow.append(th); });
  head.append(headRow);
  const body = node('tbody');
  rows.forEach(item => body.append(renderRow(item)));
  table.append(head, body); wrap.append(table); container.replaceChildren(wrap);
}
function cell(text, className = '') { return node('td', className, text); }
function badge(text, className = '') { return node('span', `badge ${className}`, text); }
function action(label, callback, className = '') {
  const button = node('button', `text-button ${className}`, label);
  button.type = 'button'; button.addEventListener('click', callback); return button;
}
function renderRow(item) {
  const row = node('tr');
  const actions = node('div', 'row-actions');
  if (state.section === 'libros') {
    const titleCell = cell(); const titleWrap = node('div', 'book-title-wrap');
    const title = node('div', 'main-cell', item.titulo); title.append(node('small', 'sub-cell', `LIBRO ${String(item.id).padStart(3, '0')}`));
    titleWrap.append(node('span', 'book-cover', item.titulo.slice(0, 1).toUpperCase()), title); titleCell.append(titleWrap);
    const stock = cell(); stock.append(badge(item.stockDisponible > 0 ? `${item.stockDisponible} disponibles` : 'Sin ejemplares', item.stockDisponible > 0 ? '' : 'warning'));
    row.append(titleCell, cell(lookup('autores', item.autorId, 'nombre')), cell(item.isbn), stock);
  } else if (state.section === 'autores') {
    row.append(cell(item.nombre, 'main-cell'), cell(item.nacionalidad), cell(state.libros.filter(book => book.autorId === item.id).length));
  } else if (state.section === 'usuarios') {
    row.append(cell(item.nombre, 'main-cell'), cell(item.cedula), cell(item.email), cell(state.prestamos.filter(loan => loan.usuarioId === item.id && loan.estado === 'PENDIENTE').length));
  } else {
    const status = cell(); status.append(badge(item.estado === 'PENDIENTE' ? 'Pendiente' : 'Devuelto', item.estado === 'PENDIENTE' ? 'warning' : 'neutral'));
    row.append(cell(lookup('libros', item.libroId, 'titulo'), 'main-cell'), cell(lookup('usuarios', item.usuarioId, 'nombre')), cell(dateLabel(item.fechaPrestamo)), cell(dateLabel(item.fechaDevolucion)), status);
    if (item.estado === 'PENDIENTE') actions.append(action('Registrar devolución ↗', () => returnLoan(item)));
    else actions.append(node('span', 'sub-cell', 'Completado'));
  }
  if (state.section !== 'prestamos') {
    const kind = state.section;
    actions.append(action('Editar', () => openForm(kind, item)), action('Eliminar', () => deleteRecord(kind, item), 'delete'));
  }
  const actionCell = cell(); actionCell.append(actions); row.append(actionCell); return row;
}
function addField(name, label, { type = 'text', value = '', full = true, options, min, maxLength = 255, hint } = {}) {
  const wrapper = node('label', `form-field${full ? ' full' : ''}`);
  wrapper.append(node('span', '', label));
  const input = node(options ? 'select' : 'input');
  input.name = name; input.id = `field-${name}`; input.required = true;
  if (options) {
    const placeholder = node('option', '', 'Selecciona una opción'); placeholder.value = ''; placeholder.disabled = true; placeholder.selected = !value; input.append(placeholder);
    options.forEach(option => { const element = node('option', '', option.label); element.value = option.id; input.append(element); });
  } else { input.type = type; if (type === 'text' || type === 'email') input.maxLength = maxLength; if (min !== undefined) input.min = min; if (type === 'number') input.step = '1'; }
  input.value = value;
  wrapper.append(input);
  if (hint) wrapper.append(node('span', 'field-hint', hint));
  $('#form-fields').append(wrapper);
  return input;
}
function openForm(kind, item = null) {
  if (!state.connected) { notify('Conecta con la biblioteca antes de registrar información. Pulsa Actualizar.', true); return; }
  if (kind === 'libros' && !state.autores.length) { selectSection('autores'); notify('Añade primero un autor para poder registrar el libro.'); openForm('autores'); return; }
  if (kind === 'prestamos' && !state.usuarios.length) { selectSection('usuarios'); notify('Registra primero un usuario para realizar el préstamo.'); openForm('usuarios'); return; }
  if (kind === 'prestamos' && !state.libros.some(book => book.stockDisponible > 0)) { selectSection('libros'); notify('Necesitas un libro con ejemplares disponibles para registrar un préstamo.', true); return; }
  modal = { kind, id: item?.id };
  $('#dialog-title').textContent = `${item ? 'Editar' : 'Nuevo'} ${config[kind].singular}`;
  $('#dialog-description').textContent = kind === 'prestamos' ? 'El préstamo descontará un ejemplar disponible. La devolución prevista debe ser posterior a la fecha del préstamo.' : kind === 'libros' && item ? 'Actualiza los datos del libro. El stock representa los ejemplares disponibles actualmente.' : 'Completa los datos para guardar el registro en la biblioteca.';
  $('#form-fields').replaceChildren(); $('#form-error').hidden = true;
  $('#save-record').textContent = item ? 'Guardar cambios' : kind === 'prestamos' ? 'Registrar préstamo' : 'Guardar registro';
  if (kind === 'autores') {
    addField('nombre', 'Nombre del autor', { value: item?.nombre });
    addField('nacionalidad', 'Nacionalidad', { value: item?.nacionalidad });
  } else if (kind === 'usuarios') {
    addField('nombre', 'Nombre completo', { value: item?.nombre });
    addField('cedula', 'Cédula', { value: item?.cedula });
    addField('email', 'Correo electrónico', { type: 'email', value: item?.email });
  } else if (kind === 'libros') {
    addField('titulo', 'Título del libro', { value: item?.titulo });
    addField('autorId', 'Autor', { value: item?.autorId ?? '', options: state.autores.map(author => ({ id: author.id, label: author.nombre })) });
    addField('isbn', 'ISBN', { value: item?.isbn, full: false, maxLength: 32 });
    addField('stockDisponible', 'Ejemplares disponibles', { type: 'number', value: item?.stockDisponible ?? 1, min: 0, full: false });
  } else {
    addField('libroId', 'Libro', { options: state.libros.filter(book => book.stockDisponible > 0).map(book => ({ id: book.id, label: `${book.titulo} · ${book.stockDisponible} disponibles` })) });
    addField('usuarioId', 'Lector', { options: state.usuarios.map(user => ({ id: user.id, label: `${user.nombre} · ${user.cedula}` })) });
    const today = new Date(); const nextWeek = new Date(today); nextWeek.setDate(nextWeek.getDate() + 7);
    const loanDate = addField('fechaPrestamo', 'Fecha de préstamo', { type: 'date', value: isoDate(today), full: false });
    const dueDate = addField('fechaDevolucion', 'Devolución prevista', { type: 'date', value: isoDate(nextWeek), full: false });
    const updateMinimum = () => { if (!loanDate.value) return; const minimum = new Date(`${loanDate.value}T12:00:00`); minimum.setDate(minimum.getDate() + 1); dueDate.min = isoDate(minimum); };
    loanDate.addEventListener('change', updateMinimum); updateMinimum();
  }
  dialog.showModal();
}
function closeForm() { if (!submitting) { dialog.close(); modal = null; } }
async function saveForm(event) {
  event.preventDefault();
  if (submitting || !modal) return;
  submitting = true;
  $('#save-record').disabled = true; $('#cancel-dialog').disabled = true; $('#close-dialog').disabled = true; $('#form-error').hidden = true;
  const { kind, id } = modal;
  const data = Object.fromEntries(new FormData(event.currentTarget));
  ['autorId', 'libroId', 'usuarioId', 'stockDisponible'].forEach(key => { if (key in data) data[key] = Number(data[key]); });
  try {
    await api(`/api/${kind}${id ? `/${id}` : ''}`, { method: id ? 'PUT' : 'POST', body: JSON.stringify(data) });
    dialog.close(); modal = null; selectSection(kind);
    try { await refresh(); notify(`${kind === 'prestamos' ? 'Préstamo registrado' : id ? 'Cambios guardados' : 'Registro creado'} correctamente.`); }
    catch (_) { notify('El registro se guardó, pero no se pudo actualizar la lista. Pulsa Actualizar.', true); }
  } catch (error) { $('#form-error').textContent = error.message; $('#form-error').hidden = false; }
  finally { submitting = false; $('#save-record').disabled = false; $('#cancel-dialog').disabled = false; $('#close-dialog').disabled = false; }
}
async function deleteRecord(kind, item) {
  if (!window.confirm(`¿Eliminar ${item.titulo || item.nombre}? Esta acción no se puede deshacer.`)) return;
  try {
    await api(`/api/${kind}/${item.id}`, { method: 'DELETE' });
    try { await refresh(); notify('Registro eliminado correctamente.'); }
    catch (_) { notify('El registro se eliminó. Pulsa Actualizar para recuperar la lista.', true); }
  } catch (error) { notify(error.message, true); }
}
async function returnLoan(loan) {
  if (!window.confirm(`¿Confirmas la devolución de “${lookup('libros', loan.libroId, 'titulo')}”? Se repondrá un ejemplar.`)) return;
  try {
    await api(`/api/prestamos/${loan.id}/devolucion`, { method: 'PUT' });
    try { await refresh(); notify('Devolución registrada. El ejemplar vuelve a estar disponible.'); }
    catch (_) { notify('La devolución se registró. Pulsa Actualizar para recuperar el stock.', true); }
  } catch (error) { notify(error.message, true); }
}

document.querySelectorAll('[data-section]').forEach(button => button.addEventListener('click', () => selectSection(button.dataset.section)));
$('#search').addEventListener('input', event => { state.search = event.target.value; render(); });
$('#loan-filter').addEventListener('change', event => { state.filter = event.target.value; render(); });
$('#refresh').addEventListener('click', () => refresh(true).catch(() => {}));
$('#quick-loan').addEventListener('click', () => openForm('prestamos'));
$('#create-record').addEventListener('click', () => openForm(state.section));
$('#record-form').addEventListener('submit', saveForm);
$('#close-dialog').addEventListener('click', closeForm);
$('#cancel-dialog').addEventListener('click', closeForm);
dialog.addEventListener('cancel', event => { if (submitting) event.preventDefault(); });
refresh().catch(() => {});
