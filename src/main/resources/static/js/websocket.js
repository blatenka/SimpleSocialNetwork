const realtimeSockets = new Map();
let reconnectTimeouts = new Map();
let reconnectAttempts = new Map();
let realtimePageClosing = false;

function websocketUrl(path) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}${path}`;
}

function connectRealtimeChannel(channel, path) {
  if (realtimePageClosing || !localStorage.getItem('token')) return;
  const existing = realtimeSockets.get(channel);
  if (existing && (existing.readyState === WebSocket.OPEN || existing.readyState === WebSocket.CONNECTING)) return;

  const socket = new WebSocket(websocketUrl(path));
  realtimeSockets.set(channel, socket);
  socket.addEventListener('open', () => {
    reconnectAttempts.set(channel, 0);
    if (channel === 'notifications') {
      window.loadNotifications?.();
    } else {
      window.dispatchEvent(new CustomEvent('aloute:chat-reconnected'));
    }
  });
  socket.addEventListener('message', event => {
    try {
      const payload = JSON.parse(event.data);
      if (channel === 'chat') {
        window.dispatchEvent(new CustomEvent('aloute:chat-event', { detail: payload }));
      } else {
        window.dispatchEvent(new CustomEvent('aloute:notification-event', { detail: payload }));
      }
    } catch (error) {
      console.error(`Invalid ${channel} WebSocket event`, error);
    }
  });
  socket.addEventListener('error', () => {
    socket.close();
  });
  socket.addEventListener('close', () => {
    if (realtimeSockets.get(channel) === socket) realtimeSockets.delete(channel);
    if (realtimePageClosing || !localStorage.getItem('token')) return;
    const attempt = (reconnectAttempts.get(channel) || 0) + 1;
    reconnectAttempts.set(channel, attempt);
    const delay = Math.min(30000, 500 * (2 ** Math.min(attempt - 1, 6)));
    const timeout = window.setTimeout(() => connectRealtimeChannel(channel, path), delay);
    reconnectTimeouts.set(channel, timeout);
  });
}

async function connectWebSockets() {
  if (!localStorage.getItem('token')) return;
  try {
    await getCurrentUser();
    connectRealtimeChannel('chat', '/ws/chat');
    connectRealtimeChannel('notifications', '/ws/notifications');
  } catch (error) {
    console.error('Could not authenticate WebSocket connections', error);
  }
}

function disconnectWebSockets() {
  realtimePageClosing = true;
  reconnectTimeouts.forEach(timeout => window.clearTimeout(timeout));
  reconnectTimeouts.clear();
  realtimeSockets.forEach(socket => socket.close(1000, 'Page closed'));
  realtimeSockets.clear();
}

document.addEventListener('DOMContentLoaded', connectWebSockets);
window.addEventListener('pagehide', disconnectWebSockets);
window.addEventListener('pageshow', () => {
  realtimePageClosing = false;
  connectWebSockets();
});
