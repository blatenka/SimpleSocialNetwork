const feedElement = document.getElementById('feed');
const feedLoadingElement = document.getElementById('feedLoading');
const feedErrorElement = document.getElementById('feedError');
const feedErrorMessageElement = document.getElementById('feedErrorMessage');
const feedEmptyElement = document.getElementById('feedEmpty');
const loadMoreFeedButton = document.getElementById('loadMoreFeed');
let feedPage = 0;
let feedLastPage = false;
let feedIsLoading = false;

function safeMediaUrl(value) {
  try {
    const url = new URL(value, window.location.origin);
    if (url.protocol === 'https:' || url.protocol === 'http:') return url.href;
    if (url.origin === window.location.origin && url.pathname.startsWith('/uploads/')) return url.pathname;
  } catch (error) {
    console.warn('Skipping invalid post media URL', error);
  }
  return null;
}

function createFeedPost(post) {
  const author = post.author || {};
  const card = document.createElement('article');
  card.className = 'card post-card p-3 mb-3';
  card.dataset.postId = post.id;

  const header = document.createElement('div');
  header.className = 'd-flex align-items-center gap-2';
  header.appendChild(SocialInteractions.createAuthor(author, 44));
  const authorInfo = document.createElement('div');
  const createdAt = document.createElement('small');
  createdAt.className = 'd-block text-muted';
  createdAt.textContent = post.createdAt ? new Date(post.createdAt).toLocaleString() : '';
  authorInfo.appendChild(createdAt);
  header.appendChild(authorInfo);
  card.appendChild(header);

  const content = document.createElement('p');
  content.className = 'mt-3 mb-2 post-content';
  content.textContent = post.content || '';
  card.appendChild(content);

  (post.mediaUrls || []).forEach(media => {
    const url = safeMediaUrl(media);
    if (!url) return;
    const isVideo = /\.(mp4|webm|ogg)(?:$|[?#])/i.test(url);
    const element = document.createElement(isVideo ? 'video' : 'img');
    element.src = url;
    element.className = `post-media${isVideo ? ' post-video' : ' post-image'} rounded`;
    if (isVideo) element.controls = true;
    else {
      element.alt = 'Post attachment';
      element.loading = 'lazy';
    }
    if (isVideo) element.preload = 'metadata';
    const mediaContainer = document.createElement('div');
    mediaContainer.className = `post-media-container mb-2${isVideo ? ' post-video-container' : ''}`;
    mediaContainer.appendChild(element);
    card.appendChild(mediaContainer);
  });

  const actions = document.createElement('div');
  actions.className = 'd-flex flex-wrap justify-content-between align-items-center gap-2 border-top pt-2';
  actions.appendChild(SocialInteractions.createReactionControl('POST', post.id, post.reactionSummary));
  const comments = document.createElement('a');
  comments.className = 'btn btn-sm btn-outline-secondary';
  comments.href = `/posts/${encodeURIComponent(post.id)}#comments`;
  comments.innerHTML = `<i class="fa fa-comment" aria-hidden="true"></i> <span data-post-comment-count>${Number(post.commentCount || 0)}</span> comments`;
  card.appendChild(actions);
  actions.appendChild(comments);

  const commentArea = document.createElement('div');
  commentArea.className = 'mt-2';
  commentArea.dataset.comments = '';
  commentArea.hidden = true;
  card.appendChild(commentArea);
  comments.addEventListener('click', event => {
    if (window.location.pathname === `/posts/${post.id}`) {
      event.preventDefault();
      commentArea.hidden = !commentArea.hidden;
      if (!commentArea.hidden) SocialInteractions.renderComments(post.id, commentArea, card);
    }
  });
  return card;
}

async function loadFeed(reset = false) {
  if (feedIsLoading) return;
  feedIsLoading = true;
  feedErrorElement.hidden = true;
  feedEmptyElement.hidden = true;
  loadMoreFeedButton.hidden = true;
  feedLoadingElement.hidden = false;
  if (reset) {
    feedPage = 0;
    feedLastPage = false;
    feedElement.replaceChildren();
  }

  try {
    const page = await API.get(`/api/posts/newsfeed?page=${feedPage}&size=10`);
    if (!page || !Array.isArray(page.content)) throw new Error('The newsfeed response was invalid.');
    page.content.forEach(post => feedElement.appendChild(createFeedPost(post)));
    feedLastPage = Boolean(page.last);
    feedPage += 1;
    feedEmptyElement.hidden = feedElement.childElementCount > 0;
    loadMoreFeedButton.hidden = feedLastPage || feedElement.childElementCount === 0;
  } catch (error) {
    feedErrorMessageElement.textContent = error.message || 'The newsfeed could not be loaded.';
    feedErrorElement.hidden = false;
  } finally {
    feedLoadingElement.hidden = true;
    feedIsLoading = false;
  }
}

feedElement.addEventListener('click', async event => {
  const link = event.target.closest('a[href$="#comments"]');
  if (!link || window.location.pathname !== link.pathname) return;
  event.preventDefault();
  const post = link.closest('[data-post-id]');
  const container = post?.querySelector('[data-comments]');
  if (container) {
    container.hidden = !container.hidden;
    if (!container.hidden) SocialInteractions.renderComments(post.dataset.postId, container, post);
  }
});

document.getElementById('retryFeed').addEventListener('click', () => loadFeed(true));
loadMoreFeedButton.addEventListener('click', () => loadFeed());
document.addEventListener('DOMContentLoaded', () => loadFeed(true));
