'use strict';

const API_BASE = 'http://localhost:8080/api';
const AUTO_REFRESH_MS = 30000;

let authToken = localStorage.getItem('authToken') || '';
let currentUser = null;
let selectedEmail = '';

const $ = (id) => document.getElementById(id);

/* ═══════════════════════════════════════════
   TOAST SYSTEM
   ═══════════════════════════════════════════ */

function toast(message, type = 'info') {
  const bar = $('statusBar');
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = message;

  const progress = document.createElement('div');
  progress.className = 'toast-progress';
  el.appendChild(progress);

  bar.appendChild(el);
  setTimeout(() => {
    el.style.opacity = '0';
    el.style.transform = 'translateX(20px)';
    el.style.transition = 'opacity 200ms, transform 200ms';
    setTimeout(() => el.remove(), 200);
  }, 4000);
}


/* ═══════════════════════════════════════════
   API HELPER
   ═══════════════════════════════════════════ */

async function api(path, opts = {}) {
  const headers = Object.assign({ 'Content-Type': 'application/json' }, opts.headers || {});
  if (authToken) headers['X-Auth-Token'] = authToken;
  const res = await fetch(`${API_BASE}${path}`, Object.assign({}, opts, { headers }));
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    return await res.json();
  }
  return await res.text();
}


/* ═══════════════════════════════════════════
   AUTH
   ═══════════════════════════════════════════ */

function setLoggedIn(user) {
  currentUser = user;
  $('loginView').classList.add('hidden');
  $('appShell').classList.remove('hidden');

  $('userName').textContent = user.username || '—';
  $('userRole').textContent = user.role || '—';
  $('userAvatar').textContent = (user.username || 'U')[0].toUpperCase();

  const isAdmin = user.role === 'ADMIN';
  $('deletePersonBtn').disabled = !isAdmin;
  $('downloadCsvBtn').disabled = !isAdmin;
  $('uploadCsvBtn').disabled = !isAdmin;
}

function setLoggedOut() {
  authToken = '';
  currentUser = null;
  selectedEmail = '';
  localStorage.removeItem('authToken');
  $('loginView').classList.remove('hidden');
  $('appShell').classList.add('hidden');
}

async function login() {
  const username = $('username').value.trim();
  const password = $('password').value;
  if (!username || !password) {
    toast('Username and password are required', 'error');
    return;
  }

  try {
    const data = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });

    if (!data.success) {
      toast(data.message || 'Login failed', 'error');
      return;
    }

    authToken = data.data.token;
    localStorage.setItem('authToken', authToken);
    setLoggedIn(data.data);
    toast('Welcome back', 'success');
    await refreshAll();
  } catch (e) {
    // API offline — load demo state
    loadDemoData();
    toast('Running in demo mode (API offline)', 'info');
  }
}

async function logout() {
  try {
    await api('/auth/logout', { method: 'POST' });
  } catch (_) {}
  setLoggedOut();
  toast('Signed out', 'info');
}

async function bootstrapSession() {
  if (!authToken) return;
  try {
    const data = await api('/auth/me');
    if (!data.success) {
      setLoggedOut();
      return;
    }
    setLoggedIn(data.data);
    await refreshAll();
  } catch (_) {
    setLoggedOut();
  }
}


/* ═══════════════════════════════════════════
   DEMO DATA FALLBACK
   ═══════════════════════════════════════════ */

function loadDemoData() {
  const demoUser = { username: 'admin', role: 'ADMIN', token: 'demo' };
  authToken = 'demo';
  localStorage.setItem('authToken', authToken);
  setLoggedIn(demoUser);

  // Populate stats with demo values
  animateCount($('statTotal'), 24502);
  animateCount($('statQueue'), 128);
  animateCount($('statViewed'), 18240);
  animateCount($('statFailed'), 3);

  // Populate demo table rows
  const demoRows = [
    { name: 'Alex Rivera', email: 'alex.r@wenxt.tech', provider: 'Gmail', status: 'Sent', sentTime: 'Mar 16, 10:30 AM' },
    { name: 'Sarah Chen', email: 'sarah@studio.io', provider: 'Outlook', status: 'Queue', sentTime: 'Mar 16, 02:00 PM' },
    { name: 'Mike Johnson', email: 'mjones@global.com', provider: 'Gmail', status: 'Viewed', sentTime: 'Mar 15, 09:15 AM' },
    { name: 'Anna White', email: 'aw@tech-corp.com', provider: 'Gmail', status: 'Sent', sentTime: 'Mar 15, 02:00 PM' },
    { name: 'Jordan Smith', email: 'j.smith@corp.com', provider: 'Outlook', status: 'Viewed', sentTime: 'Mar 14, 11:20 AM' },
  ];
  renderRows(demoRows, $('dashboardLogsBody'));
  renderRows(demoRows, $('logsBody'));
}


/* ═══════════════════════════════════════════
   NUMBER COUNT-UP ANIMATION
   ═══════════════════════════════════════════ */

function animateCount(el, target) {
  const duration = 600;
  const start = performance.now();
  const from = 0;

  function tick(now) {
    const elapsed = now - start;
    const progress = Math.min(elapsed / duration, 1);
    // ease-out cubic
    const ease = 1 - Math.pow(1 - progress, 3);
    const current = Math.round(from + (target - from) * ease);
    el.textContent = current.toLocaleString();
    if (progress < 1) requestAnimationFrame(tick);
  }

  requestAnimationFrame(tick);
}


/* ═══════════════════════════════════════════
   STATS
   ═══════════════════════════════════════════ */

async function refreshStats() {
  try {
    const data = await api('/stats');
    if (!data.success || !data.data) return;
    animateCount($('statTotal'), data.data.total ?? 0);
    animateCount($('statQueue'), data.data.queue ?? 0);
    animateCount($('statViewed'), data.data.viewed ?? 0);
    animateCount($('statFailed'), data.data.failed ?? 0);
  } catch (_) {
    // stats unavailable — keep current values
  }
}


/* ═══════════════════════════════════════════
   TABLE RENDERING
   ═══════════════════════════════════════════ */

function esc(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function statusBadgeClass(status) {
  const s = (status || '').toLowerCase();
  if (s === 'queue') return 'badge--queue';
  if (s === 'sent') return 'badge--sent';
  if (s === 'viewed') return 'badge--viewed';
  if (s === 'failed') return 'badge--failed';
  return '';
}

function providerDotClass(provider) {
  const p = (provider || '').toLowerCase();
  if (p === 'gmail') return 'provider-dot--gmail';
  if (p === 'outlook') return 'provider-dot--outlook';
  return '';
}

function renderRows(rows, targetBody) {
  const body = targetBody || $('logsBody');
  if (!rows || rows.length === 0) {
    body.innerHTML = '<tr><td colspan="5" style="text-align:center;color:#A1A1AA;padding:24px;">No records found</td></tr>';
    return;
  }

  body.innerHTML = rows.map((p) => {
    const email = esc(p.email || '');
    const providerLower = (p.provider || '').toLowerCase();
    const statusLower = (p.status || '').toLowerCase();
    return `<tr data-email="${email}">
      <td><span style="font-weight:500">${esc(p.name || '')}</span></td>
      <td style="color:#71717A">${email}</td>
      <td><span class="provider-cell"><span class="provider-dot ${providerDotClass(p.provider)}"></span>${esc(p.provider || '')}</span></td>
      <td><span class="badge ${statusBadgeClass(p.status)}">${esc(p.status || '')}</span></td>
      <td style="color:#71717A;font-size:12px">${esc(p.sentTime || '')}</td>
    </tr>`;
  }).join('');

  body.querySelectorAll('tr[data-email]').forEach((tr) => {
    tr.addEventListener('click', () => {
      const email = tr.getAttribute('data-email');
      const row = rows.find((r) => (r.email || '').toLowerCase() === email.toLowerCase());
      if (!row) return;
      selectedEmail = row.email;
      $('personName').value = row.name || '';
      $('personEmail').value = row.email || '';
      $('personProvider').value = row.provider || 'Gmail';
      $('personMessage').value = row.message || '';
      $('personSchedule').value = row.schedule || '0';
      $('personStatus').value = row.status || 'Queue';
      // Switch to contacts view
      navigateTo('contacts');
    });
  });
}


/* ═══════════════════════════════════════════
   LOGS
   ═══════════════════════════════════════════ */

async function refreshLogs() {
  try {
    const data = await api('/logs');
    if (!data.success) {
      toast(data.message || 'Failed to load logs', 'error');
      return;
    }
    const rows = data.data || [];
    renderRows(rows, $('dashboardLogsBody'));
    renderRows(rows, $('logsBody'));
  } catch (_) {
    // logs unavailable
  }
}


/* ═══════════════════════════════════════════
   SEARCH
   ═══════════════════════════════════════════ */

async function search() {
  const query = $('searchInput').value.trim();
  const path = query ? `/search?query=${encodeURIComponent(query)}` : '/persons';
  try {
    const data = await api(path);
    if (!data.success) {
      renderRows([], $('logsBody'));
      toast(data.message || 'No result', 'info');
      return;
    }
    renderRows(data.data || [], $('logsBody'));
  } catch (_) {
    // search unavailable
  }
}


/* ═══════════════════════════════════════════
   COMPOSE / SEND
   ═══════════════════════════════════════════ */

function gatherComposePayload() {
  return {
    name: $('sendName').value.trim(),
    email: $('sendEmail').value.trim(),
    provider: $('sendProvider').value,
    message: $('sendMessage').value.trim(),
    schedule: $('sendSchedule').value
  };
}

async function send(addAndSend = false) {
  const payload = gatherComposePayload();
  if (!payload.email || !payload.message) {
    toast('Email and message are required', 'error');
    return;
  }
  try {
    const path = addAndSend ? '/add-and-send' : '/send';
    const data = await api(path, { method: 'POST', body: JSON.stringify(payload) });
    if (!data.success) {
      toast(data.message || 'Send failed', 'error');
      return;
    }
    toast(addAndSend ? 'Added and sent' : 'Sent successfully', 'success');
    await refreshAll();
  } catch (_) {
    toast('Send failed — API offline', 'error');
  }
}


/* ═══════════════════════════════════════════
   CONTACTS CRUD
   ═══════════════════════════════════════════ */

function gatherPersonPayload() {
  return {
    name: $('personName').value.trim(),
    email: $('personEmail').value.trim(),
    provider: $('personProvider').value,
    message: $('personMessage').value.trim(),
    aiGenerated: 'false',
    schedule: $('personSchedule').value,
    status: $('personStatus').value,
    sentTime: ''
  };
}

async function savePerson() {
  const payload = gatherPersonPayload();
  if (!payload.email) {
    toast('Email is required for contact', 'error');
    return;
  }

  const isUpdate = !!selectedEmail;
  const path = isUpdate ? `/persons/${encodeURIComponent(selectedEmail)}` : '/persons';
  const method = isUpdate ? 'PUT' : 'POST';

  try {
    const data = await api(path, { method, body: JSON.stringify(payload) });
    if (!data.success) {
      toast(data.message || 'Save failed', 'error');
      return;
    }
    selectedEmail = data.data?.email || payload.email;
    toast(isUpdate ? 'Contact updated' : 'Contact created', 'success');
    await refreshAll();
  } catch (_) {
    toast('Save failed — API offline', 'error');
  }
}

async function deletePerson() {
  const email = selectedEmail || $('personEmail').value.trim();
  if (!email) {
    toast('Select a contact first', 'error');
    return;
  }

  try {
    const data = await api(`/persons/${encodeURIComponent(email)}`, { method: 'DELETE' });
    if (!data.success) {
      toast(data.message || 'Delete failed', 'error');
      return;
    }
    clearPersonForm();
    toast('Contact deleted', 'success');
    await refreshAll();
  } catch (_) {
    toast('Delete failed — API offline', 'error');
  }
}

function clearPersonForm() {
  selectedEmail = '';
  $('personName').value = '';
  $('personEmail').value = '';
  $('personProvider').value = 'Gmail';
  $('personMessage').value = '';
  $('personSchedule').value = '0';
  $('personStatus').value = 'Queue';
}


/* ═══════════════════════════════════════════
   CSV
   ═══════════════════════════════════════════ */

async function downloadCsv() {
  try {
    const text = await api('/export/csv', { headers: { 'Content-Type': 'text/plain' } });
    if (typeof text !== 'string' || text.startsWith('error')) {
      toast('Export failed (admin only)', 'error');
      return;
    }
    const blob = new Blob([text], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `contacts-${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    toast('CSV downloaded', 'success');
  } catch (_) {
    toast('Export failed — API offline', 'error');
  }
}

async function uploadCsv() {
  const file = $('csvFile').files[0];
  if (!file) {
    toast('Choose a CSV file first', 'error');
    return;
  }
  try {
    const content = await file.text();
    const data = await api('/import/csv', {
      method: 'POST',
      headers: { 'Content-Type': 'text/csv' },
      body: content
    });
    if (!data.success) {
      toast(data.message || 'Import failed', 'error');
      return;
    }
    toast(`Imported ${data.data.imported}, skipped ${data.data.skipped}`, 'success');
    await refreshAll();
  } catch (_) {
    toast('Import failed — API offline', 'error');
  }
}


/* ═══════════════════════════════════════════
   NAVIGATION
   ═══════════════════════════════════════════ */

const viewTitles = {
  dashboard: 'Dashboard',
  compose: 'Send Email',
  contacts: 'Contacts',
  csv: 'CSV Tools'
};

function navigateTo(viewName) {
  // Update nav items
  document.querySelectorAll('.nav-item').forEach((item) => {
    item.classList.toggle('active', item.dataset.view === viewName);
  });

  // Show correct view
  document.querySelectorAll('.view').forEach((v) => v.classList.remove('active-view'));
  const target = $('view-' + viewName);
  if (target) target.classList.add('active-view');

  // Update topbar
  $('topbarTitle').textContent = viewTitles[viewName] || viewName;

  // Close mobile sidebar
  document.querySelector('.sidebar').classList.remove('open');
}


/* ═══════════════════════════════════════════
   REFRESH
   ═══════════════════════════════════════════ */

async function refreshAll() {
  await Promise.all([refreshStats(), refreshLogs()]);
}


/* ═══════════════════════════════════════════
   SEGMENTED CONTROL
   ═══════════════════════════════════════════ */

function setupSegmentedControl() {
  const segments = document.querySelectorAll('.segment');
  segments.forEach((seg) => {
    seg.addEventListener('click', () => {
      segments.forEach((s) => s.classList.remove('active'));
      seg.classList.add('active');
      $('sendSchedule').value = seg.dataset.value;
    });
  });
}


/* ═══════════════════════════════════════════
   INIT
   ═══════════════════════════════════════════ */

document.addEventListener('DOMContentLoaded', async () => {
  // Auth
  $('loginBtn').addEventListener('click', login);
  $('logoutBtn').addEventListener('click', logout);
  $('refreshBtn').addEventListener('click', refreshAll);

  // Allow Enter key on login
  $('password').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') login();
  });
  $('username').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') $('password').focus();
  });

  // Compose
  $('sendBtn').addEventListener('click', () => send(false));
  $('addAndSendBtn').addEventListener('click', () => send(true));

  // Contacts
  $('searchInput').addEventListener('input', search);
  $('savePersonBtn').addEventListener('click', savePerson);
  $('clearPersonBtn').addEventListener('click', clearPersonForm);
  $('deletePersonBtn').addEventListener('click', deletePerson);

  // CSV
  $('downloadCsvBtn').addEventListener('click', downloadCsv);
  $('uploadCsvBtn').addEventListener('click', uploadCsv);

  // Navigation
  document.querySelectorAll('.nav-item').forEach((item) => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      navigateTo(item.dataset.view);
    });
  });

  // Mobile menu
  $('mobileMenuBtn').addEventListener('click', () => {
    document.querySelector('.sidebar').classList.toggle('open');
  });

  // Segmented control
  setupSegmentedControl();

  // Bootstrap
  await bootstrapSession();

  // Auto refresh
  setInterval(() => {
    if (authToken) refreshAll();
  }, AUTO_REFRESH_MS);
});
