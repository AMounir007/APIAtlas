const TOKEN_KEY = 'atlas_token';

export const getToken = () => sessionStorage.getItem(TOKEN_KEY);
export const setToken = (t) => (t ? sessionStorage.setItem(TOKEN_KEY, t) : sessionStorage.removeItem(TOKEN_KEY));

async function request(path, options = {}) {
  const token = getToken();
  const res = await fetch(`/api${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  if (res.status === 401 && path !== '/auth/login') {
    setToken(null);
    window.location.assign('/');
    throw new Error('Session expired');
  }
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      detail = body.detail || body.error || detail;
    } catch { /* not json */ }
    throw new Error(detail);
  }
  return res;
}

export const api = {
  login: (username, password) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }).then((r) => r.json()),
  get: (path) => request(path).then((r) => r.json()),
  post: (path, body) =>
    request(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }).then((r) => r.json()),
  download: async (path, filename) => {
    const blob = await request(path).then((r) => r.blob());
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  },
};
