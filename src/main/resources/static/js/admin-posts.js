(() => {
  let pageNumber = 0;
  const pageSize = 12;
  let selectedPostId = null;
  const list = document.getElementById('postList');
  const detailsModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('postDetailsModal'));

  function safeImageUrl(value) {
    if (!value) return null;
    try {
      const url = new URL(value, window.location.origin);
      return ['http:', 'https:'].includes(url.protocol) ? url.href : null;
    } catch {
      return null;
    }
  }

  function addImage(container, url, className, alt, width, height) {
    const src = safeImageUrl(url);
    if (!src) return;
    const image = document.createElement('img');
    image.src = src;
    image.alt = alt;
    image.className = className;
    if (width && height) {
      image.width = width;
      image.height = height;
    }
    image.loading = 'lazy';
    image.referrerPolicy = 'no-referrer';
    container.appendChild(image);
  }

  function formatDate(value) {
    if (!value) return 'Date unavailable';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? 'Date unavailable' : date.toLocaleString();
  }

  async function loadPosts() {
    const error = document.getElementById('postsError');
    const loading = document.getElementById('postsLoading');
    error.hidden = true;
    loading.hidden = false;
    list.replaceChildren();
    const params = new URLSearchParams({
      page: String(pageNumber),
      size: String(pageSize),
      sort: document.getElementById('postSort').value,
      direction: document.getElementById('postDirection').value
    });
    const search = document.getElementById('postSearch').value.trim();
    if (search) params.set('search', search);
    try {
      const result = await API.get(`/api/admin/posts?${params}`);
      if (!Array.isArray(result.content)) throw new Error('Invalid posts response.');
      if (!result.content.length) {
        const empty = document.createElement('div');
        empty.className = 'col-12 text-center text-muted py-5';
        empty.textContent = 'No posts found.';
        list.appendChild(empty);
      } else {
        result.content.forEach(post => list.appendChild(createPostCard(post)));
      }
      document.getElementById('postPageLabel').textContent = result.totalElements
        ? `Page ${result.page + 1} of ${result.totalPages} · ${result.totalElements} posts`
        : 'No posts';
      document.getElementById('previousPostPage').disabled = result.first;
      document.getElementById('nextPostPage').disabled = result.last || result.totalPages === 0;
    } catch (requestError) {
      error.textContent = requestError.message || 'Posts could not be loaded.';
      error.hidden = false;
      const failure = document.createElement('div');
      failure.className = 'col-12 text-center text-muted py-5';
      failure.textContent = 'Unable to load posts.';
      list.appendChild(failure);
    } finally {
      loading.hidden = true;
    }
  }

  function createPostCard(post) {
    const column = document.createElement('div');
    column.className = 'col-12 col-xl-6';
    const card = document.createElement('article');
    card.className = 'card border-0 h-100';
    const body = document.createElement('div');
    body.className = 'card-body';

    const authorLine = document.createElement('div');
    authorLine.className = 'd-flex align-items-center gap-2 mb-3';
    addImage(authorLine, post.author && post.author.avatarUrl,
      'rounded-circle object-fit-cover flex-shrink-0', '', 40, 40);
    const authorText = document.createElement('div');
    const name = document.createElement('div');
    name.className = 'fw-semibold';
    name.textContent = post.author && (post.author.fullname || post.author.nickname) || 'Unknown author';
    const authorEmail = document.createElement('div');
    authorEmail.className = 'small text-muted';
    authorEmail.textContent = post.author && post.author.email || '';
    authorText.append(name, authorEmail);
    authorLine.appendChild(authorText);
    const date = document.createElement('time');
    date.className = 'small text-muted ms-auto';
    date.dateTime = post.createdAt || '';
    date.textContent = formatDate(post.createdAt);
    authorLine.appendChild(date);

    const content = document.createElement('p');
    content.className = 'mb-3 text-break';
    content.textContent = post.content || '';
    const media = document.createElement('div');
    media.className = 'd-flex gap-2 overflow-auto mb-3';
    (Array.isArray(post.mediaUrls) ? post.mediaUrls.slice(0, 3) : []).forEach(url =>
      addImage(media, url, 'rounded object-fit-cover flex-shrink-0 admin-post-thumbnail',
        'Post attachment', 96, 96));
    const meta = document.createElement('div');
    meta.className = 'small text-muted mb-3';
    meta.textContent = `${Number(post.likeCount || 0)} reactions · ${Number(post.commentCount || 0)} comments`;
    const actions = document.createElement('div');
    actions.className = 'd-flex gap-2';
    const details = document.createElement('button');
    details.type = 'button';
    details.className = 'btn btn-sm btn-outline-primary';
    details.dataset.postAction = 'details';
    details.dataset.postId = post.id;
    details.textContent = 'View details';
    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'btn btn-sm btn-outline-danger';
    remove.dataset.postAction = 'delete';
    remove.dataset.postId = post.id;
    remove.textContent = 'Delete';
    actions.append(details, remove);
    body.append(authorLine, content, media, meta, actions);
    card.appendChild(body);
    column.appendChild(card);
    return column;
  }

  async function openPostDetails(id) {
    selectedPostId = id;
    document.getElementById('postDetailsBody').innerHTML = '<div class="text-center text-muted py-4">Loading post details...</div>';
    detailsModal.show();
    try {
      const data = await API.get(`/api/admin/posts/${encodeURIComponent(id)}?page=0&size=50`);
      renderPostDetails(data);
    } catch (error) {
      const body = document.getElementById('postDetailsBody');
      body.textContent = error.message || 'Post details could not be loaded.';
    }
  }

  function renderPostDetails(data) {
    const body = document.getElementById('postDetailsBody');
    body.replaceChildren();
    const post = data.post;
    if (!post) {
      body.textContent = 'Post details are unavailable.';
      return;
    }
    const author = document.createElement('h3');
    author.className = 'h6';
    author.textContent = `${post.author && (post.author.fullname || post.author.nickname) || 'Unknown author'} · ${formatDate(post.createdAt)}`;
    const content = document.createElement('p');
    content.className = 'text-break';
    content.textContent = post.content || '';
    const media = document.createElement('div');
    media.className = 'd-flex flex-wrap gap-2 mb-3';
    (Array.isArray(post.mediaUrls) ? post.mediaUrls : []).forEach(url =>
      addImage(media, url, 'rounded object-fit-cover flex-shrink-0 admin-post-detail-image',
        'Post attachment', 144, 144));
    const engagement = document.createElement('p');
    engagement.className = 'small text-muted';
    engagement.textContent = `${Number(post.likeCount || 0)} reactions · ${Number(post.commentCount || 0)} comments`;
    const heading = document.createElement('h3');
    heading.className = 'h6 border-top pt-3';
    heading.textContent = 'Comments';
    const comments = document.createElement('div');
    const commentPage = data.comments;
    if (!commentPage || !Array.isArray(commentPage.content) || !commentPage.content.length) {
      const empty = document.createElement('p');
      empty.className = 'text-muted small';
      empty.textContent = 'No comments on this post.';
      comments.appendChild(empty);
    } else {
      commentPage.content.forEach(comment => {
        const item = document.createElement('div');
        item.className = 'border rounded p-2 mb-2';
        const name = comment.author && (comment.author.fullname || comment.author.nickname) || 'Unknown user';
        const byline = document.createElement('div');
        byline.className = 'small fw-semibold mb-1';
        byline.textContent = `${name} · ${formatDate(comment.createdAt)}`;
        const text = document.createElement('div');
        text.className = 'text-break';
        text.textContent = comment.content || '';
        item.append(byline, text);
        comments.appendChild(item);
      });
    }
    body.append(author, content, media, engagement, heading, comments);
  }

  function confirmDelete(id) {
    showConfirm('Delete post', 'Delete this post and its comments and reactions? This cannot be undone from the admin interface.', async () => {
      try {
        await API.del(`/api/admin/posts/${encodeURIComponent(id)}`);
        if (selectedPostId === id) detailsModal.hide();
        selectedPostId = null;
        await loadPosts();
      } catch (error) {
        showModal('error', 'Post deletion failed', error.message || 'The post could not be deleted.');
      }
    });
  }

  list.addEventListener('click', event => {
    const button = event.target.closest('[data-post-action]');
    if (!button) return;
    if (button.dataset.postAction === 'details') openPostDetails(button.dataset.postId);
    if (button.dataset.postAction === 'delete') confirmDelete(button.dataset.postId);
  });
  document.getElementById('deletePostFromDetails').addEventListener('click', () => {
    if (selectedPostId) confirmDelete(selectedPostId);
  });
  document.getElementById('postSearchForm').addEventListener('submit', event => {
    event.preventDefault();
    pageNumber = 0;
    loadPosts();
  });
  document.getElementById('postSort').addEventListener('change', () => { pageNumber = 0; loadPosts(); });
  document.getElementById('postDirection').addEventListener('change', () => { pageNumber = 0; loadPosts(); });
  document.getElementById('refreshPosts').addEventListener('click', loadPosts);
  document.getElementById('previousPostPage').addEventListener('click', () => { pageNumber = Math.max(0, pageNumber - 1); loadPosts(); });
  document.getElementById('nextPostPage').addEventListener('click', () => { pageNumber += 1; loadPosts(); });
  document.addEventListener('DOMContentLoaded', loadPosts, { once: true });
})();
