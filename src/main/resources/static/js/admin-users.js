(() => {
  let pageNumber = 0;
  const pageSize = 20;
  let editingUserId = null;
  const table = document.getElementById('userTable');
  const editor = bootstrap.Modal.getOrCreateInstance(document.getElementById('userEditorModal'));
  const details = bootstrap.Modal.getOrCreateInstance(document.getElementById('userDetailsModal'));

  function escapeHtml(value) {
    const node = document.createElement('span');
    node.textContent = value == null ? '' : String(value);
    return node.innerHTML;
  }

  async function loadUsers() {
    const error = document.getElementById('usersError');
    error.hidden = true;
    table.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Loading users...</td></tr>';
    const params = new URLSearchParams({
      page: String(pageNumber),
      size: String(pageSize),
      sort: document.getElementById('userSort').value,
      direction: document.getElementById('userDirection').value
    });
    const search = document.getElementById('userSearch').value.trim();
    if (search) params.set('search', search);
    try {
      const result = await API.get(`/api/admin/users?${params}`);
      if (!Array.isArray(result.content)) throw new Error('Invalid users response.');
      table.replaceChildren();
      if (!result.content.length) {
        table.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No users found.</td></tr>';
      } else {
        result.content.forEach(user => {
          const row = document.createElement('tr');
          const avatar = user.avatarUrl && /^https?:\/\//i.test(user.avatarUrl)
            ? `<img src="${escapeHtml(user.avatarUrl)}" class="rounded-circle me-2" width="36" height="36" alt="">`
            : '<span class="rounded-circle bg-secondary-subtle d-inline-flex justify-content-center align-items-center me-2" style="width:36px;height:36px"><i class="fa fa-user text-secondary"></i></span>';
          row.innerHTML = `<td>${user.id}</td><td>${avatar}${escapeHtml(user.fullname || user.nickname || 'Unnamed user')}</td><td>${escapeHtml(user.email)}</td><td><span class="badge text-bg-secondary">${escapeHtml(user.role)}</span></td><td><span class="badge ${user.status === 'ACTIVE' ? 'text-bg-success' : 'text-bg-warning'}">${escapeHtml(user.status)}</span></td><td>${user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'}</td><td class="text-end text-nowrap"><button class="btn btn-sm btn-outline-secondary" data-user-action="details" data-user-id="${user.id}">Details</button> <button class="btn btn-sm btn-outline-primary" data-user-action="edit" data-user-id="${user.id}">Edit</button> <button class="btn btn-sm btn-outline-danger" data-user-action="deactivate" data-user-id="${user.id}" ${user.status === 'BANNED' ? 'disabled' : ''}>Deactivate</button></td>`;
          table.appendChild(row);
        });
      }
      document.getElementById('userPageLabel').textContent = result.totalElements
        ? `Page ${result.page + 1} of ${result.totalPages} · ${result.totalElements} users`
        : 'No users';
      document.getElementById('previousUserPage').disabled = result.first;
      document.getElementById('nextUserPage').disabled = result.last || result.totalPages === 0;
    } catch (requestError) {
      error.textContent = requestError.message || 'Users could not be loaded.';
      error.hidden = false;
      table.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Unable to load users.</td></tr>';
    }
  }

  function openCreateForm() {
    editingUserId = null;
    document.getElementById('userEditorForm').reset();
    document.getElementById('userEditorTitle').textContent = 'Create user';
    document.getElementById('saveUserButton').textContent = 'Create user';
    document.getElementById('editorEmail').disabled = false;
    document.getElementById('editorPassword').required = true;
    document.getElementById('passwordField').hidden = false;
    document.getElementById('statusField').hidden = true;
    document.getElementById('roleAssignmentNote').hidden = false;
    document.getElementById('userEditorError').hidden = true;
    editor.show();
  }

  async function loadUser(id, edit) {
    try {
      const user = await API.get(`/api/admin/users/${encodeURIComponent(id)}`);
      if (edit) {
        editingUserId = user.id;
        document.getElementById('userEditorForm').reset();
        document.getElementById('userEditorTitle').textContent = 'Edit user';
        document.getElementById('saveUserButton').textContent = 'Save changes';
        document.getElementById('editorEmail').value = user.email || '';
        document.getElementById('editorEmail').disabled = true;
        document.getElementById('editorPassword').required = false;
        document.getElementById('passwordField').hidden = true;
        document.getElementById('editorFullname').value = user.fullname || '';
        document.getElementById('editorNickname').value = user.nickname || '';
        document.getElementById('editorStatus').value = user.status || 'ACTIVE';
        document.getElementById('statusField').hidden = false;
        document.getElementById('roleAssignmentNote').hidden = true;
        document.getElementById('userEditorError').hidden = true;
        editor.show();
      } else {
        document.getElementById('userDetailsBody').innerHTML = `<div class="d-flex align-items-center gap-3 mb-3"><strong class="fs-5">${escapeHtml(user.fullname || user.nickname || 'Unnamed user')}</strong><span class="badge text-bg-secondary">${escapeHtml(user.role)}</span></div><dl class="row mb-0"><dt class="col-4">ID</dt><dd class="col-8">${user.id}</dd><dt class="col-4">Email</dt><dd class="col-8">${escapeHtml(user.email)}</dd><dt class="col-4">Nickname</dt><dd class="col-8">${escapeHtml(user.nickname || '—')}</dd><dt class="col-4">Status</dt><dd class="col-8">${escapeHtml(user.status)}</dd><dt class="col-4">Registered</dt><dd class="col-8">${user.createdAt ? new Date(user.createdAt).toLocaleString() : '—'}</dd></dl>`;
        details.show();
      }
    } catch (error) {
      showModal('error', 'Could not load user', error.message || 'User details could not be loaded.');
    }
  }

  table.addEventListener('click', event => {
    const button = event.target.closest('[data-user-action]');
    if (!button) return;
    const id = button.dataset.userId;
    if (button.dataset.userAction === 'details') loadUser(id, false);
    if (button.dataset.userAction === 'edit') loadUser(id, true);
    if (button.dataset.userAction === 'deactivate') {
      showConfirm('Deactivate account', 'This will suspend this account without deleting its social data. Continue?', async () => {
        try {
          await API.del(`/api/admin/users/${encodeURIComponent(id)}`);
          await loadUsers();
        } catch (error) {
          showModal('error', 'Deactivation failed', error.message || 'The account could not be deactivated.');
        }
      });
    }
  });

  document.getElementById('userEditorForm').addEventListener('submit', async event => {
    event.preventDefault();
    const error = document.getElementById('userEditorError');
    error.hidden = true;
    const body = {
      fullname: document.getElementById('editorFullname').value.trim(),
      nickname: document.getElementById('editorNickname').value.trim()
    };
    const button = document.getElementById('saveUserButton');
    button.disabled = true;
    try {
      if (editingUserId) {
        body.status = document.getElementById('editorStatus').value;
        await API.put(`/api/admin/users/${encodeURIComponent(editingUserId)}`, body);
      } else {
        body.email = document.getElementById('editorEmail').value.trim();
        body.password = document.getElementById('editorPassword').value;
        await API.post('/api/admin/users', body);
      }
      editor.hide();
      await loadUsers();
    } catch (requestError) {
      error.textContent = requestError.message || 'User could not be saved.';
      error.hidden = false;
    } finally {
      button.disabled = false;
    }
  });

  document.getElementById('createUserButton').addEventListener('click', openCreateForm);
  document.getElementById('userSearchForm').addEventListener('submit', event => {
    event.preventDefault();
    pageNumber = 0;
    loadUsers();
  });
  document.getElementById('userSort').addEventListener('change', () => { pageNumber = 0; loadUsers(); });
  document.getElementById('userDirection').addEventListener('change', () => { pageNumber = 0; loadUsers(); });
  document.getElementById('previousUserPage').addEventListener('click', () => { pageNumber = Math.max(0, pageNumber - 1); loadUsers(); });
  document.getElementById('nextUserPage').addEventListener('click', () => { pageNumber += 1; loadUsers(); });
  document.addEventListener('DOMContentLoaded', loadUsers, { once: true });
})();
