(() => {
  const metrics = [
    ['totalUsers', 'Registered users', 'fa-users', 'primary'],
    ['usersLastWeek', 'Registrations · 7 days', 'fa-user-plus', 'success'],
    ['usersLastMonth', 'Registrations · 30 days', 'fa-user-plus', 'success'],
    ['activeUsers', 'Active accounts', 'fa-user-check', 'success'],
    ['bannedUsers', 'Suspended accounts', 'fa-user-lock', 'warning'],
    ['totalPosts', 'All posts', 'fa-newspaper', 'primary'],
    ['activePosts', 'Active posts', 'fa-file-lines', 'info'],
    ['postsLastWeek', 'Posts · 7 days', 'fa-chart-line', 'info'],
    ['postsLastMonth', 'Posts · 30 days', 'fa-chart-line', 'info'],
    ['totalComments', 'Comments and replies', 'fa-comments', 'secondary'],
    ['totalReplies', 'Replies', 'fa-reply', 'secondary'],
    ['commentsLastWeek', 'Comments · 7 days', 'fa-comment', 'secondary'],
    ['commentsLastMonth', 'Comments · 30 days', 'fa-comment', 'secondary'],
    ['totalReactions', 'Reactions', 'fa-heart', 'danger'],
    ['acceptedFriendships', 'Accepted friendships', 'fa-user-group', 'success'],
    ['pendingFriendRequests', 'Pending requests', 'fa-user-clock', 'warning']
  ];
  const trends = [
    ['Registrations', 'usersLastWeek', 'usersLastMonth', 'primary'],
    ['Posts', 'postsLastWeek', 'postsLastMonth', 'success'],
    ['Comments', 'commentsLastWeek', 'commentsLastMonth', 'info']
  ];
  let latestStats = null;

  function makeCard(label, value, icon, color) {
    const column = document.createElement('div');
    column.className = 'col-6 col-md-4 col-xl-3';
    const card = document.createElement('div');
    card.className = 'card border-0 h-100';
    const body = document.createElement('div');
    body.className = 'card-body';
    const title = document.createElement('div');
    title.className = 'd-flex justify-content-between align-items-start gap-2';
    const labelElement = document.createElement('span');
    labelElement.className = 'text-muted small';
    labelElement.textContent = label;
    const iconElement = document.createElement('i');
    iconElement.className = `fa ${icon} text-${color}`;
    iconElement.setAttribute('aria-hidden', 'true');
    const amount = document.createElement('div');
    amount.className = 'h3 mt-3 mb-0';
    amount.textContent = Number(value || 0).toLocaleString();
    title.append(labelElement, iconElement);
    body.append(title, amount);
    card.appendChild(body);
    column.appendChild(card);
    return column;
  }

  function makeHealthCard(name, state) {
    const column = document.createElement('div');
    column.className = 'col-12 col-md-6';
    const card = document.createElement('div');
    card.className = 'card border-0';
    const body = document.createElement('div');
    body.className = 'card-body d-flex justify-content-between align-items-center';
    const label = document.createElement('span');
    label.className = 'fw-semibold';
    label.textContent = name;
    const status = document.createElement('span');
    const isUp = state === 'UP';
    status.className = `badge text-bg-${isUp ? 'success' : 'danger'}`;
    status.textContent = isUp ? 'Connected' : 'Unavailable';
    status.setAttribute('aria-label', `${name}: ${status.textContent}`);
    body.append(label, status);
    card.appendChild(body);
    column.appendChild(card);
    return column;
  }

  function renderActivityChart(stats, range) {
    const chart = document.getElementById('activityChart');
    chart.replaceChildren();
    const legend = document.createElement('p');
    legend.className = 'small text-muted';
    legend.textContent = range === 'compare'
      ? 'Database counts for the last 7 and 30 days.'
      : `Database counts for the last ${range} days.`;
    chart.appendChild(legend);
    trends.forEach(([label, weekKey, monthKey, color]) => {
      const week = Number(stats[weekKey] || 0);
      const month = Number(stats[monthKey] || 0);
      const periods = range === '7'
        ? [[week, '7 days']]
        : range === '30' ? [[month, '30 days']] : [[week, '7 days'], [month, '30 days']];
      const scale = Math.max(...periods.map(([value]) => value), 1);
      const group = document.createElement('div');
      group.className = 'mb-3';
      const title = document.createElement('div');
      title.className = 'fw-semibold small mb-2';
      title.textContent = label;
      group.appendChild(title);
      periods.forEach(([value, period]) => {
        const row = document.createElement('div');
        row.className = 'd-flex align-items-center gap-2 mb-1';
        const periodLabel = document.createElement('span');
        periodLabel.className = 'small text-muted';
        periodLabel.style.width = '4.5rem';
        periodLabel.textContent = period;
        const progress = document.createElement('div');
        progress.className = 'progress flex-grow-1';
        progress.setAttribute('role', 'progressbar');
        progress.setAttribute('aria-label', `${label} in the last ${period}`);
        progress.setAttribute('aria-valuemin', '0');
        progress.setAttribute('aria-valuemax', String(scale));
        progress.setAttribute('aria-valuenow', String(value));
        const bar = document.createElement('div');
        bar.className = `progress-bar bg-${color}`;
        bar.style.width = `${Math.max(0, Math.min(100, value / scale * 100))}%`;
        const count = document.createElement('span');
        count.className = 'small text-nowrap';
        count.textContent = value.toLocaleString();
        progress.appendChild(bar);
        row.append(periodLabel, progress, count);
        group.appendChild(row);
      });
      chart.appendChild(group);
    });
  }

  async function loadSystemReport() {
    const loading = document.getElementById('systemLoading');
    const error = document.getElementById('systemError');
    const statsContainer = document.getElementById('systemStats');
    const healthContainer = document.getElementById('healthCards');
    loading.hidden = false;
    error.hidden = true;
    try {
      const stats = await API.get('/api/admin/analytics');
      latestStats = stats;
      statsContainer.replaceChildren(...metrics.map(([key, label, icon, color]) =>
        makeCard(label, stats[key], icon, color)));
      renderActivityChart(stats, document.getElementById('activityRange').value);
      healthContainer.replaceChildren(
        makeHealthCard('MySQL', stats.mysqlHealth),
        makeHealthCard('MongoDB', stats.mongoHealth)
      );
    } catch (requestError) {
      error.textContent = requestError.message || 'System statistics could not be loaded.';
      error.hidden = false;
    } finally {
      loading.hidden = true;
    }
  }

  document.getElementById('refreshSystem').addEventListener('click', loadSystemReport);
  document.getElementById('activityRange').addEventListener('change', event => {
    if (latestStats) renderActivityChart(latestStats, event.target.value);
  });
  document.addEventListener('DOMContentLoaded', loadSystemReport, { once: true });
})();
