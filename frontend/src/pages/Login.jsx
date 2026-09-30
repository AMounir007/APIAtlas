import React, { useState } from 'react';
import { Alert, Box, Button, Paper, TextField, Typography } from '@mui/material';
import { api, setToken } from '../api.js';

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);

  const submit = async (e) => {
    e.preventDefault();
    try {
      const { token } = await api.login(username, password);
      setToken(token);
      onLogin();
    } catch {
      setError('Invalid credentials');
    }
  };

  return (
    <Box sx={{ display: 'grid', placeItems: 'center', minHeight: '100vh' }}>
      <Paper component="form" onSubmit={submit} sx={{ p: 4, width: 340, display: 'grid', gap: 2 }}>
        <Typography variant="h5">API Atlas</Typography>
        <Typography variant="caption">Discover. Map. Document.</Typography>
        {error && <Alert severity="error">{error}</Alert>}
        <TextField label="Username" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
        <TextField label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        <Button type="submit" variant="contained">Sign in</Button>
      </Paper>
    </Box>
  );
}
