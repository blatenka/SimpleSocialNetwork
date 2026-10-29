(() => {
  const reactionTypes = ['LIKE', 'LOVE', 'HAHA', 'SAD', 'ANGRY'];
  const reactionLabels = {
    LIKE: 'Like',
    LOVE: 'Love',
    HAHA: 'Haha',
    SAD: 'Sad',
    ANGRY: 'Angry'
  };
  let currentUserPromise;

  function getCurrentUser() {
    if (!currentUserPromise) currentUserPromise = API.get('/api/users/me');
    return currentUserPromise;
  }

  function authorLink(author, content) {
    const link = document.createElement('a');
    link.className = 'text-decoration-none text-reset';
    link.href = author?.email ? `/profile?email=${encodeURIComponent(author.email)}` : '#';
    if (!author?.email) link.setAttribute('aria-disabled', 'true');
    link.appendChild(content);
    if (author?.id != null) {
      link.addEventListener('click', event => {
        event.preventDefault();
        getCurrentUser().then(me => {
          link.href = Number(me.id) === Number(author.id)
            ? '/myprofile'
            : `/profile?email=${encodeURIComponent(author.email || '')}`;
          if (author.email || Number(me.id) === Number(author.id)) {
            window.location.assign(link.href);
          }
        }).catch(error => {
          console.error('Could not resolve current user for profile link', error);
          if (author.email) window.location.assign(link.href);
        });
      });
    }
    return link;
  }

  function createAuthor(author, size = 40) {
    const user = author || {};
    const name = user.fullname || user.nickname || `User ${user.id || ''}`;
    const wrapper = document.createElement('div');
    wrapper.className = 'd-flex align-items-center gap-2';
    const avatar = document.createElement(user.avatarUrl ? 'img' : 'span');
    avatar.className = 'rounded-circle border';
    avatar.style.width = `${size}px`;
    avatar.style.height = `${size}px`;
    avatar.style.objectFit = 'cover';
    avatar.alt = user.avatarUrl ? `${name}'s avatar` : '';
    if (user.avatarUrl) avatar.src = user.avatarUrl;
    else {
      avatar.classList.add('bg-light', 'd-inline-flex', 'align-items-center', 'justify-content-center');
      avatar.textContent = name.trim().charAt(0).toUpperCase() || '?';
    }
    const label = document.createElement('span');
    label.className = 'fw-semibold';
    label.textContent = name;
    wrapper.append(authorLink(user, avatar), authorLink(user, label));
    return wrapper;
  }

  function reactionRoute(targetType, targetId) {
    const id = encodeURIComponent(targetId);
    return targetType === 'POST'
      ? `/api/posts/${id}/like`
      : `/api/comments/${id}/like`;
  }

  function normalizeSummary(summary) {
    const counts = Object.fromEntries(reactionTypes.map(type => [
      type,
      Number(summary?.counts?.[type] || 0)
    ]));
    return { counts, currentUserReaction: summary?.currentUserReaction || null };
  }

  function totalReactions(summary) {
    return Object.values(summary.counts).reduce((total, count) => total + count, 0);
  }

  function openReactors(targetType, targetId) {
    const modalId = 'reactionUsersModal';
    let modalElement = document.getElementById(modalId);
    if (!modalElement) {
      modalElement = document.createElement('div');
      modalElement.id = modalId;
      modalElement.className = 'modal fade';
      modalElement.tabIndex = -1;
      modalElement.innerHTML = '<div class="modal-dialog modal-dialog-scrollable"><div class="modal-content"><div class="modal-header"><h5 class="modal-title">Reactions</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><div class="modal-body" data-reactors></div></div></div>';
      document.body.appendChild(modalElement);
    }
    const body = modalElement.querySelector('[data-reactors]');
    body.textContent = 'Loading reactions...';
    bootstrap.Modal.getOrCreateInstance(modalElement).show();
    const endpoint = targetType === 'POST'
      ? `/api/posts/${encodeURIComponent(targetId)}/reactions`
      : `/api/comments/${encodeURIComponent(targetId)}/reactions`;
    API.get(endpoint).then(users => {
      body.replaceChildren();
      if (!Array.isArray(users) || users.length === 0) {
        body.textContent = 'No reactions yet.';
        return;
      }
      users.forEach(reaction => {
        const row = document.createElement('div');
        row.className = 'd-flex align-items-center justify-content-between border-bottom py-2';
        row.append(createAuthor(reaction.author, 36));
        const type = document.createElement('span');
        type.className = 'badge text-bg-light';
        type.textContent = reactionLabels[reaction.reactionType] || reaction.reactionType;
        row.appendChild(type);
        body.appendChild(row);
      });
    }).catch(error => {
      body.textContent = error.message || 'Could not load reactions.';
    });
  }

  function createReactionControl(targetType, targetId, initialSummary) {
    const wrapper = document.createElement('div');
    wrapper.className = 'd-inline-flex align-items-center gap-1';
    let summary = normalizeSummary(initialSummary);
    const action = document.createElement('button');
    action.type = 'button';
    action.className = 'btn btn-sm btn-outline-primary';
    action.setAttribute('aria-label', 'Toggle your reaction');
    const selector = document.createElement('select');
    selector.className = 'form-select form-select-sm';
    selector.setAttribute('aria-label', 'Choose a reaction');
    selector.style.width = 'auto';
    selector.innerHTML = '<option value="">React...</option>' + reactionTypes
      .map(type => `<option value="${type}">${reactionLabels[type]}</option>`).join('');
    const count = document.createElement('button');
    count.type = 'button';
    count.className = 'btn btn-sm btn-link text-decoration-none';
    count.setAttribute('aria-label', 'View people who reacted');
    const paint = () => {
      const selected = summary.currentUserReaction;
      action.textContent = selected ? `${reactionLabels[selected]} ✓` : 'Like';
      action.classList.toggle('btn-primary', Boolean(selected));
      action.classList.toggle('btn-outline-primary', !selected);
      selector.value = selected || '';
      count.textContent = String(totalReactions(summary));
      count.hidden = totalReactions(summary) === 0;
    };
    const send = async type => {
      action.disabled = true;
      selector.disabled = true;
      try {
        summary = normalizeSummary(await API.post(
          `${reactionRoute(targetType, targetId)}?type=${encodeURIComponent(type)}`, {}));
        paint();
      } catch (error) {
        showModal('error', 'Reaction failed', error.message || 'The reaction could not be saved.');
        paint();
      } finally {
        action.disabled = false;
        selector.disabled = false;
      }
    };
    action.addEventListener('click', () => send(summary.currentUserReaction || 'LIKE'));
    selector.addEventListener('change', () => {
      if (selector.value) send(selector.value);
    });
    count.addEventListener('click', () => openReactors(targetType, targetId));
    wrapper.append(action, selector, count);
    paint();
    return wrapper;
  }

  function updatePostCommentCount(postCard, delta) {
    const count = postCard?.querySelector('[data-post-comment-count]');
    if (!count) return;
    count.textContent = String(Math.max(0, Number(count.textContent) + delta));
  }

  function createReplyForm(postId, commentId, postCard, repliesContainer, replyButton) {
    const form = document.createElement('form');
    form.className = 'input-group input-group-sm mt-2';
    const input = document.createElement('input');
    input.className = 'form-control';
    input.maxLength = 2000;
    input.required = true;
    input.setAttribute('aria-label', 'Write a reply');
    input.placeholder = 'Write a reply...';
    const submit = document.createElement('button');
    submit.className = 'btn btn-outline-primary';
    submit.type = 'submit';
    submit.textContent = 'Reply';
    form.append(input, submit);
    form.addEventListener('submit', async event => {
      event.preventDefault();
      const content = input.value.trim();
      if (!content) return;
      submit.disabled = true;
      try {
        await API.post(`/api/comments/reply/${encodeURIComponent(commentId)}`, { content });
        input.value = '';
        updatePostCommentCount(postCard, 1);
        replyButton.dataset.replyCount = String(Number(replyButton.dataset.replyCount || 0) + 1);
        replyButton.textContent = `Replies (${replyButton.dataset.replyCount})`;
        repliesContainer.hidden = false;
        await loadReplies(postId, commentId, repliesContainer, postCard);
      } catch (error) {
        showModal('error', 'Reply failed', error.message || 'Your reply could not be saved.');
      } finally {
        submit.disabled = false;
      }
    });
    return form;
  }

  function createComment(postId, comment, postCard) {
    const item = document.createElement('div');
    item.className = 'border rounded p-2 mb-2';
    item.dataset.commentId = comment.id;
    const top = document.createElement('div');
    top.className = 'd-flex align-items-start justify-content-between gap-2';
    top.appendChild(createAuthor(comment.author, 36));
    const text = document.createElement('p');
    text.className = 'mb-1 mt-2';
    text.textContent = comment.content || '';
    const controls = document.createElement('div');
    controls.className = 'd-flex flex-wrap align-items-center gap-2';
    controls.appendChild(createReactionControl('COMMENT', comment.id, comment.reactionSummary));
    const replyButton = document.createElement('button');
    replyButton.type = 'button';
    replyButton.className = 'btn btn-sm btn-link px-1';
    replyButton.textContent = 'Reply';
    const repliesButton = document.createElement('button');
    repliesButton.type = 'button';
    repliesButton.className = 'btn btn-sm btn-link px-1';
    repliesButton.dataset.replyCount = String(comment.replyCount || 0);
    repliesButton.textContent = `Replies (${repliesButton.dataset.replyCount})`;
    repliesButton.hidden = Number(repliesButton.dataset.replyCount) === 0;
    controls.append(replyButton, repliesButton);
    const replyFormContainer = document.createElement('div');
    replyFormContainer.hidden = true;
    replyButton.addEventListener('click', () => {
      replyFormContainer.hidden = !replyFormContainer.hidden;
      if (!replyFormContainer.hidden && !replyFormContainer.childElementCount) {
        replyFormContainer.appendChild(createReplyForm(postId, comment.id, postCard, repliesContainer, repliesButton));
      }
    });
    const repliesContainer = document.createElement('div');
    repliesContainer.className = 'ms-4 mt-2';
    repliesContainer.hidden = true;
    repliesButton.addEventListener('click', async () => {
      repliesContainer.hidden = !repliesContainer.hidden;
      if (!repliesContainer.hidden) await loadReplies(postId, comment.id, repliesContainer, postCard);
    });
    item.append(top, text, controls, replyFormContainer, repliesContainer);
    return item;
  }

  async function loadReplies(postId, parentId, container, postCard) {
    container.replaceChildren();
    try {
      const page = await API.get(`/api/comments/replies/${encodeURIComponent(parentId)}?page=0&size=100`);
      if (!page || !Array.isArray(page.content)) throw new Error('Invalid replies response.');
      page.content.forEach(comment => container.appendChild(createComment(postId, comment, postCard)));
      if (!page.content.length) {
        const empty = document.createElement('small');
        empty.className = 'text-muted';
        empty.textContent = 'No replies yet.';
        container.appendChild(empty);
      }
    } catch (error) {
      const message = document.createElement('small');
      message.className = 'text-danger';
      message.textContent = error.message || 'Replies could not be loaded.';
      container.appendChild(message);
    }
  }

  async function renderComments(postId, container, postCard) {
    container.replaceChildren();
    try {
      const page = await API.get(`/api/comments/post/${encodeURIComponent(postId)}?page=0&size=100`);
      if (!page || !Array.isArray(page.content)) throw new Error('Invalid comments response.');
      page.content.forEach(comment => container.appendChild(createComment(postId, comment, postCard)));
      if (!page.content.length) {
        const empty = document.createElement('p');
        empty.className = 'text-muted';
        empty.textContent = 'No comments yet.';
        container.appendChild(empty);
      }
      const form = document.createElement('form');
      form.className = 'input-group mt-2';
      const input = document.createElement('input');
      input.className = 'form-control';
      input.maxLength = 2000;
      input.required = true;
      input.placeholder = 'Write a comment...';
      input.setAttribute('aria-label', 'Write a comment');
      const submit = document.createElement('button');
      submit.className = 'btn btn-primary';
      submit.type = 'submit';
      submit.textContent = 'Comment';
      form.append(input, submit);
      form.addEventListener('submit', async event => {
        event.preventDefault();
        const content = input.value.trim();
        if (!content) return;
        submit.disabled = true;
        try {
          await API.post(`/api/comments/post/${encodeURIComponent(postId)}`, { content });
          updatePostCommentCount(postCard, 1);
          await renderComments(postId, container, postCard);
        } catch (error) {
          showModal('error', 'Comment failed', error.message || 'The comment could not be saved.');
        } finally {
          submit.disabled = false;
        }
      });
      container.appendChild(form);
    } catch (error) {
      const message = document.createElement('p');
      message.className = 'text-danger small';
      message.textContent = error.message || 'Comments could not be loaded.';
      container.appendChild(message);
    }
  }

  window.SocialInteractions = { createAuthor, createReactionControl, openReactors, renderComments };
})();
