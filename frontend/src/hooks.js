import { useEffect, useState } from 'react';
import { api } from './api.js';

/** Loads JSON from the API; re-runs when `path` changes. */
export function useFetch(path) {
  const [state, setState] = useState({ data: null, error: null, loading: true });
  useEffect(() => {
    let cancelled = false;
    setState((s) => ({ ...s, loading: true }));
    api.get(path)
      .then((data) => !cancelled && setState({ data, error: null, loading: false }))
      .catch((error) => !cancelled && setState({ data: null, error: error.message, loading: false }));
    return () => { cancelled = true; };
  }, [path]);
  return state;
}
