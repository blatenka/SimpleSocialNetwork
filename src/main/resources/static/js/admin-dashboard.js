(() => {
  const cards = [
    ['totalUsers', 'Registered users', 'fa-users', 'primary'],
    ['activeUsers', 'Active users', 'fa-user-check', 'success'],
    ['bannedUsers', 'Suspended users', 'fa-user-lock', 'warning'],
    ['activePosts', 'Active posts', 'fa-newspaper', 'info'],
    ['totalComments', 'Comments', 'fa-comments', 'secondary'],
    ['totalReactions', 'Reactions', 'fa-heart', 'danger'],
    ['pendingFriendRequests', 'Pending requests', 'fa-user-plus', 'primary'],
    ['acceptedFriendships', 'Friendships', 'fa-user-group', 'success']
  ];

  async function loadDashboard() {
    const loading = document.getElementById('dashboardLoading');
    const error = document.getElementById('dashboardError');
    const container = document.getElementById('dashboardCards');
    loading.hidden = false;
    error.hidden = true;
    try {
      const stats = await API.get('/api/admin/analytics');
      container.replaceChildren();
      cards.forEach(([key, label, icon, color]) => {
        const col = document.createElement('div');
        col.className = 'col-6 col-md-4 col-xl-3';
        col.innerHTML = `<div class="card border-0 h-100"><div class="card-body"><div class="d-flex justify-content-between align-items-start"><span class="text-muted small">${label}</span><i class="fa ${icon} text-${color}" aria-hidden="true"></i></div><div class="h3 mt-3 mb-0">${Number(stats[key] || 0).toLocaleString()}</div></div></div>`;
        container.appendChild(col);
      });
    } catch (requestError) {
      error.textContent = requestError.message || 'Statistics could not be loaded.';
      error.hidden = false;
    } finally {
      loading.hidden = true;
    }
  }

  document.getElementById('refreshDashboard').addEventListener('click', loadDashboard);
  document.addEventListener('DOMContentLoaded', loadDashboard, { once: true });
})();
