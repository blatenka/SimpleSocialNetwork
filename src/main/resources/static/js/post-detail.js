(() => {
  const postId = decodeURIComponent(window.location.pathname.split('/').pop());
  const root = document.getElementById('postDetail');

  function appendMedia(card, post) {
    (post.mediaUrls || []).forEach(value => {
      try {
        const url = new URL(value, window.location.origin);
        if (!['http:', 'https:'].includes(url.protocol)) return;
        const isVideo = /\.(mp4|webm|ogg)(?:$|[?#])/i.test(url.href);
        const media = document.createElement(isVideo ? 'video' : 'img');
        media.src = url.href;
        media.className = 'img-fluid rounded mb-2';
        if (isVideo) media.controls = true;
        else {
          media.alt = 'Post attachment';
          media.loading = 'lazy';
        }
        card.appendChild(media);
      } catch (error) {
        console.warn('Skipping invalid post media URL', error);
      }
    });
  }

  async function loadPost() {
    try {
      const post = await API.get(`/api/posts/${encodeURIComponent(postId)}`);
      root.replaceChildren();
      const card = document.createElement('article');
      card.className = 'card border-0';
      card.dataset.postId = post.id;
      const author = document.createElement('div');
      author.className = 'mb-3';
      author.appendChild(SocialInteractions.createAuthor(post.author, 52));
      const date = document.createElement('small');
      date.className = 'text-muted';
      date.textContent = post.createdAt ? new Date(post.createdAt).toLocaleString() : '';
      author.appendChild(date);
      const content = document.createElement('p');
      content.className = 'mb-3';
      content.textContent = post.content || '';
      card.append(author, content);
      appendMedia(card, post);

      const actions = document.createElement('div');
      actions.className = 'd-flex flex-wrap justify-content-between align-items-center gap-2 border-top pt-3';
      actions.appendChild(SocialInteractions.createReactionControl('POST', post.id, post.reactionSummary));
      const commentsButton = document.createElement('button');
      commentsButton.type = 'button';
      commentsButton.className = 'btn btn-sm btn-outline-secondary';
      commentsButton.innerHTML = `<i class="fa fa-comment" aria-hidden="true"></i> <span data-post-comment-count>${Number(post.commentCount || 0)}</span> comments`;
      actions.appendChild(commentsButton);
      const comments = document.createElement('section');
      comments.id = 'comments';
      comments.className = 'mt-3';
      comments.setAttribute('aria-label', 'Comments');
      commentsButton.addEventListener('click', () => {
        comments.hidden = false;
        SocialInteractions.renderComments(post.id, comments, card);
        comments.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
      card.append(actions, comments);
      root.appendChild(card);
      commentsButton.click();
    } catch (error) {
      root.replaceChildren();
      const message = document.createElement('div');
      message.className = 'alert alert-danger';
      message.setAttribute('role', 'alert');
      message.textContent = error.message || 'This post could not be loaded.';
      root.appendChild(message);
    }
  }

  document.addEventListener('DOMContentLoaded', loadPost, { once: true });
})();
