(() => {
  const form = document.getElementById('createPostForm');
  if (!form) return;

  const contentField = document.getElementById('postContent');
  const mediaField = document.getElementById('postMedia');
  const submitButton = document.getElementById('submitPost');
  const mediaPreview = document.getElementById('postMediaPreview');
  const errorMessage = document.getElementById('createPostError');
  const successMessage = document.getElementById('createPostSuccess');
  const MAX_MEDIA_FILES = 10;
  const MAX_MEDIA_SIZE = 25 * 1024 * 1024;
  const ALLOWED_TYPES = new Set([
    'image/jpeg', 'image/png', 'image/gif', 'image/webp',
    'video/mp4', 'video/webm', 'video/ogg'
  ]);

  function showError(message) {
    errorMessage.textContent = message;
    errorMessage.hidden = false;
    successMessage.hidden = true;
  }

  function renderSelection() {
    const files = Array.from(mediaField.files || []);
    mediaPreview.textContent = files.length
      ? `${files.length} file${files.length === 1 ? '' : 's'} selected: ${files.map(file => file.name).join(', ')}`
      : '';
  }

  async function loadComposerAuthor() {
    try {
      const currentUser = await getCurrentUser();
      const profile = await API.get(`/api/users/${encodeURIComponent(currentUser.id)}/profile`);
      const name = profile.fullname || profile.nickname || currentUser.email;
      if (name) document.getElementById('composerUserName').textContent = name;
      if (profile.avatarUrl) {
        const avatar = document.getElementById('composerAvatar');
        avatar.src = profile.avatarUrl;
        avatar.hidden = false;
        document.getElementById('composerAvatarFallback').hidden = true;
      }
    } catch (error) {
      console.warn('Could not load post composer profile', error);
    }
  }

  mediaField.addEventListener('change', () => {
    renderSelection();
    const files = Array.from(mediaField.files || []);
    if (files.length > MAX_MEDIA_FILES) {
      mediaField.value = '';
      renderSelection();
      showError(`You can attach up to ${MAX_MEDIA_FILES} photos or videos.`);
      return;
    }
    const invalidFile = files.find(file =>
      !ALLOWED_TYPES.has(file.type) || file.size > MAX_MEDIA_SIZE || file.size === 0);
    if (invalidFile) {
      mediaField.value = '';
      renderSelection();
      showError('Choose valid JPEG, PNG, GIF, or WebP images, or MP4, WebM, or Ogg videos. Each file must be 25 MB or smaller.');
    } else {
      errorMessage.hidden = true;
    }
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();
    errorMessage.hidden = true;
    successMessage.hidden = true;
    const content = contentField.value.trim();
    const files = Array.from(mediaField.files || []);
    if (!content) {
      showError('Write something before posting.');
      contentField.focus();
      return;
    }
    if (content.length > 5000 || files.length > MAX_MEDIA_FILES
        || files.some(file => !ALLOWED_TYPES.has(file.type) || file.size > MAX_MEDIA_SIZE || file.size === 0)) {
      showError('Check your post text and attached media before posting.');
      return;
    }

    submitButton.disabled = true;
    const originalButtonText = submitButton.innerHTML;
    submitButton.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span>Posting...';
    try {
      const mediaUrls = [];
      for (const file of files) {
        const data = new FormData();
        data.append('file', file);
        const uploaded = await API.upload('/api/media/post-media', data);
        if (!uploaded || !uploaded.url) throw new Error('An uploaded file did not return a URL.');
        mediaUrls.push(uploaded.url);
      }

      const post = await API.post('/api/posts', {
        content,
        mediaUrls,
        privacy: document.getElementById('postPrivacy').value
      });
      if (!post || !post.id) throw new Error('The post was created but the response was invalid.');

      const feed = document.getElementById('feed');
      feed.prepend(createFeedPost(post));
      document.getElementById('feedEmpty').hidden = true;
      contentField.value = '';
      mediaField.value = '';
      document.getElementById('postPrivacy').value = 'PUBLIC';
      renderSelection();
      successMessage.textContent = 'Your post was published.';
      successMessage.hidden = false;
      window.setTimeout(() => { successMessage.hidden = true; }, 4000);
    } catch (error) {
      showError(error.message || 'Your post could not be published. Please try again.');
    } finally {
      submitButton.disabled = false;
      submitButton.innerHTML = originalButtonText;
    }
  });

  loadComposerAuthor();
})();
