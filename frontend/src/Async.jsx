import React from 'react';
import { Alert, CircularProgress } from '@mui/material';

/** Renders loading / error states and the children once data is present. */
export default function Async({ state, children }) {
  if (state.loading) return <CircularProgress />;
  if (state.error) return <Alert severity="error">{state.error}</Alert>;
  return children(state.data);
}
