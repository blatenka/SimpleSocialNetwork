const API = {
  token: () => localStorage.getItem('token'),
  headers: () => ({ 
    'Content-Type': 'application/json', 
    'Authorization': 'Bearer ' + (localStorage.getItem('token') || '') 
  }),
  
  async request(method, url, body) {
    const headers = this.headers();
    let requestBody = body;
    if (typeof FormData !== 'undefined' && body instanceof FormData) {
      delete headers['Content-Type'];
    } else if (body) {
      requestBody = JSON.stringify(body);
    }

    const res = await fetch(url, { 
      method, 
      headers,
      body: requestBody || null
    });
    
    // NẾU BỊ 401 (Token hết hạn hoặc sai) -> Xoá token, đá về login và DỪNG CHẠY
    if (res.status === 401) { 
      localStorage.removeItem('token'); 
      window.location.href = '/login'; 
      throw new Error('Unauthorized');
    }
    
    if (!res.ok) {
      const body = await res.text();
      let message = body;
      try {
        const error = JSON.parse(body);
        message = error.message || body;
        if (error.errors) {
          const details = Object.entries(error.errors)
            .flatMap(([field, messages]) => messages.map(detail => `${field}: ${detail}`))
            .join('; ');
          if (details) message += ` (${details})`;
        }
      } catch {
        // Keep a plain-text server error as the message.
      }
      throw new Error(message || `Request failed with status ${res.status}`);
    }
    
    const ct = res.headers.get('content-type') || '';
    return ct.includes('application/json') ? res.json() : res.text();
  },
  
  get: (url) => API.request('GET', url),
  post: (url, body) => API.request('POST', url, body),
  put: (url, body) => API.request('PUT', url, body),
  del: (url) => API.request('DELETE', url),
  upload: (url, formData) => API.request('POST', url, formData)
};

let currentUserPromise;
const notificationIds = new Set();

function getCurrentUser() {
  if (!currentUserPromise) currentUserPromise = API.get('/api/users/me');
  return currentUserPromise;
}

window.getCurrentUser = getCurrentUser;

async function login(e) {
  if (e) e.preventDefault(); // Ngăn trình duyệt reload lại trang
  const email = document.getElementById('email').value;
  const password = document.getElementById('password').value;
  const errorMessage = document.getElementById('loginError');
  if (errorMessage) errorMessage.hidden = true;
  
  try {
    const data = await fetch('/api/auth/login', { 
      method: 'POST', 
      headers: { 'Content-Type': 'application/json' }, 
      body: JSON.stringify({ email, password }) 
    });
    
    if (!data.ok) {
      if (errorMessage) {
        errorMessage.textContent = data.status === 401
          ? 'Sai email hoặc mật khẩu.'
          : 'Không thể đăng nhập lúc này. Vui lòng thử lại.';
        errorMessage.hidden = false;
      }
      return;
    }
    
    const json = await data.json();
    localStorage.setItem('token', json.token);
    window.location.href = '/home';
  } catch (err) { 
    if (errorMessage) {
      errorMessage.textContent = 'Không thể kết nối máy chủ. Vui lòng thử lại.';
      errorMessage.hidden = false;
    }
  }
}

async function logout() {
  try {
    await API.post('/api/auth/logout');
  } catch (error) {
    console.error('Logout request failed', error);
  } finally {
    currentUserPromise = null;
    localStorage.removeItem('token');
    window.location.href = '/login';
  }
}

async function searchGlobal(e) {
  if (e) e.preventDefault();
  const q = document.getElementById('globalSearch').value;
  window.location.href = '/search?q=' + encodeURIComponent(q);
}

function escapeHtml(text) {
  const element = document.createElement('div');
  element.textContent = text == null ? '' : String(text);
  return element.innerHTML;
}

function setNotificationCount(count) {
  const badge = document.getElementById('notifCount');
  if (badge) badge.textContent = String(Math.max(0, Number(count) || 0));
}

function renderNotification(notification) {
  const list = document.getElementById('notifList');
  if (!list || !notification || !notification.id || notificationIds.has(notification.id)) return;
  notificationIds.add(notification.id);
  const empty = list.querySelector('[data-empty-notifications]');
  if (empty) empty.remove();
  const item = document.createElement('li');
  const link = document.createElement('a');
  link.className = `dropdown-item${notification.isRead ? '' : ' fw-semibold'}`;
  link.href = notification.linkUrl || '#';
  link.dataset.read = String(Boolean(notification.isRead));
  link.dataset.notificationId = notification.id;
  link.dataset.notificationType = notification.type || '';
  link.dataset.actorId = notification.actor?.id ?? '';
  link.dataset.actorEmail = notification.actor?.email || '';
  link.dataset.createdAt = notification.createdAt || '';
  link.textContent = notification.content || '';
  item.appendChild(link);
  list.appendChild(item);
  sortNotificationsNewestFirst(list);
}

function sortNotificationsNewestFirst(list) {
  const items = Array.from(list.querySelectorAll('a[data-notification-id]'))
    .map(link => link.closest('li'))
    .filter(Boolean);
  items.sort((left, right) => {
    const leftLink = left.querySelector('[data-notification-id]');
    const rightLink = right.querySelector('[data-notification-id]');
    const byDate = Date.parse(rightLink?.dataset.createdAt || '') - Date.parse(leftLink?.dataset.createdAt || '');
    if (Number.isFinite(byDate) && byDate !== 0) return byDate;
    return (rightLink?.dataset.notificationId || '').localeCompare(leftLink?.dataset.notificationId || '');
  });
  items.forEach(item => list.appendChild(item));
}

async function updateNotificationCount() {
  const data = await API.get('/api/notifications/unread-count');
  setNotificationCount(data.count);
}

async function loadNotifications(markDisplayedRead = false) {
  try {
    const page = await API.get('/api/notifications?page=0&size=100');
    const list = document.getElementById('notifList');
    if (!page || !Array.isArray(page.content)) {
      throw new Error('Invalid notification response');
    }
    if (list) {
      list.replaceChildren();
      notificationIds.clear();
      page.content.slice().sort((left, right) =>
        Date.parse(right.createdAt || '') - Date.parse(left.createdAt || '')
        || String(right.id || '').localeCompare(String(left.id || ''))
      ).forEach(renderNotification);
      if (!page.content.length) {
        list.innerHTML = '<li data-empty-notifications><span class="dropdown-item-text text-muted">No notifications</span></li>';
      }
    }

    if (markDisplayedRead) {
      const unreadIds = page.content.filter(notification => !notification.isRead).map(notification => notification.id);
      await Promise.all(unreadIds.map(id => API.post(`/api/notifications/${encodeURIComponent(id)}/read`, {})));
      unreadIds.forEach(id => {
        const link = list?.querySelector(`[data-notification-id="${CSS.escape(id)}"]`);
        if (link) {
          link.classList.remove('fw-semibold');
          link.dataset.read = 'true';
        }
      });
    }
    await updateNotificationCount();
  } catch (error) {
    console.error('Could not load or mark notifications as read', error);
    if (markDisplayedRead) showModal('error', 'Notifications unavailable', error.message || 'Could not mark notifications as read.');
  }
}

window.loadNotifications = loadNotifications;

window.addEventListener('aloute:notification-event', event => {
  const data = event.detail;
  if (!data) return;
  if (data.type === 'notification.created') renderNotification(data.notification);
  if (Number.isFinite(Number(data.unreadCount))) setNotificationCount(data.unreadCount);
});

document.addEventListener('DOMContentLoaded', () => {
  const dropdown = document.getElementById('notificationDropdown');
  dropdown?.addEventListener('shown.bs.dropdown', () => loadNotifications(true));
  document.getElementById('notifList')?.addEventListener('click', async event => {
    const link = event.target.closest('[data-notification-id]');
    if (!link || link.dataset.read === 'true') return;
    event.preventDefault();
    let target = link.href;
    try {
      await API.post(`/api/notifications/${encodeURIComponent(link.dataset.notificationId)}/read`, {});
      link.classList.remove('fw-semibold');
      link.dataset.read = 'true';
      await updateNotificationCount();
      if (link.dataset.notificationType === 'FRIEND_ACCEPT' && link.dataset.actorId) {
        const me = await getCurrentUser();
        if (Number(me.id) === Number(link.dataset.actorId)) {
          target = '/myprofile';
        } else if (link.dataset.actorEmail) {
          target = `/profile?email=${encodeURIComponent(link.dataset.actorEmail)}`;
        }
      }
      if (target && target !== window.location.href && !target.endsWith('#')) window.location.assign(target);
    } catch (error) {
      showModal('error', 'Notification could not be opened', error.message || 'Please try again.');
    }
  });
});

function showModal(type, title, message) {
  const modalEl = document.getElementById('appModal');
  if (!modalEl) { alert(message); return; }
  
  document.getElementById('appModalTitle').textContent = title || (type === 'error' ? 'Error' : 'Notice');
  document.getElementById('appModalBody').textContent = message;
  const footer = document.getElementById('appModalFooter');
  footer.innerHTML = '<button type="button" class="btn btn-primary" data-bs-dismiss="modal">OK</button>';
  
  const modal = new bootstrap.Modal(modalEl);
  modal.show();
}

function showConfirm(title, message, onConfirm) {
  const modalEl = document.getElementById('appModal');
  if (!modalEl) { if (confirm(message)) onConfirm(); return; }
  
  document.getElementById('appModalTitle').textContent = title || 'Confirm';
  document.getElementById('appModalBody').textContent = message;
  const footer = document.getElementById('appModalFooter');
  footer.innerHTML = '<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button><button type="button" class="btn btn-danger" id="confirmBtn">Confirm</button>';
  
  const modal = new bootstrap.Modal(modalEl);
  document.getElementById('confirmBtn').onclick = () => { modal.hide(); onConfirm(); };
  modal.show();
}

document.addEventListener('DOMContentLoaded', async () => {
  const publicPaths = ['/', '/login', '/register', '/forgot-password', '/reset-password'];
  const path = window.location.pathname;
  
  // 1. Nếu không có token và đang không ở trang Public -> Đá về trang Login
  if (!localStorage.getItem('token') && !publicPaths.includes(path)) {
    window.location.href = '/login';
    return;
  }
  
  // 2. Nếu có token -> Gọi API lấy thông tin người dùng
  if (localStorage.getItem('token')) {
    loadNotifications();
    try {
      const me = await getCurrentUser();
      const info = await API.get('/api/users/' + me.id + '/profile');
      
      const name = info.fullname || info.nickname || me.email;
      const el = document.getElementById('navUserName');
      if (el) el.textContent = name;
      if (me.role === 'ADMIN') {
        const adminSidebar = document.getElementById('adminSidebar');
        const adminLink = document.getElementById('adminLink');
        if (adminSidebar) adminSidebar.style.display = '';
        if (adminLink) adminLink.style.display = '';
      }
      
      if (path.startsWith('/admin') && me.role !== 'ADMIN') {
        window.location.href = '/home';
      }
    } catch (e) { 
      console.log("Lỗi load profile do Backend chưa có API");
    }
  }
});