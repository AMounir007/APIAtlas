import React, { useState } from 'react';
import {
  Alert, Box, Button, Chip, FormControlLabel, MenuItem, Paper, Stack, Switch, Table, TableBody, TableCell, TableHead, TableRow,
  TextField, Typography,
} from '@mui/material';
import { api } from '../api.js';
import { useFetch } from '../hooks.js';
import Async from '../Async.jsx';

export default function Discovery() {
  const [form, setForm] = useState({ type: 'WEB', target: '', browser: 'chromium', platform: 'android', submitForms: false });
  const [error, setError] = useState(null);
  const [refresh, setRefresh] = useState(0);
  const state = useFetch(`/discovery/sessions?v=${refresh}`);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const start = async () => {
    try {
      await api.post('/discovery/start', {
        type: form.type, target: form.target,
        browser: form.type === 'WEB' ? form.browser : undefined,
        platform: form.type === 'MOBILE' ? form.platform : undefined,
        submitForms: form.type === 'WEB' ? form.submitForms : undefined,
      });
      setError(null); setRefresh((r) => r + 1);
    } catch (e) { setError(e.message); }
  };
  const stop = async (id) => {
    try { await api.post('/discovery/stop', { sessionId: id }); setRefresh((r) => r + 1); }
    catch (e) { setError(e.message); }
  };

  return (
    <Box>
      <Typography variant="h5" gutterBottom>Discovery</Typography>
      <Paper sx={{ p: 2, mb: 3 }}>
        <Stack direction="row" spacing={2} alignItems="center" useFlexGap flexWrap="wrap">
          <TextField select size="small" label="Type" value={form.type} onChange={set('type')} sx={{ minWidth: 110 }}>
            <MenuItem value="WEB">Web</MenuItem><MenuItem value="MOBILE">Mobile</MenuItem>
          </TextField>
          <TextField size="small" label={form.type === 'WEB' ? 'Start URL (https://...)' : 'App path / package id'} value={form.target}
            onChange={set('target')} sx={{ flexGrow: 1, minWidth: 260 }} />
          {form.type === 'WEB' ? (
            <>
              <TextField select size="small" label="Browser" value={form.browser} onChange={set('browser')} sx={{ minWidth: 130 }}>
                {['chromium', 'chrome', 'msedge', 'firefox', 'webkit'].map((b) => <MenuItem key={b} value={b}>{b}</MenuItem>)}
              </TextField>
              <FormControlLabel control={<Switch checked={form.submitForms} onChange={(e) => setForm({ ...form, submitForms: e.target.checked })} />}
                label="Submit forms" />
            </>
          ) : (
            <TextField select size="small" label="Platform" value={form.platform} onChange={set('platform')} sx={{ minWidth: 130 }}>
              <MenuItem value="android">Android</MenuItem><MenuItem value="ios">iOS</MenuItem>
            </TextField>
          )}
          <Button variant="contained" disabled={!form.target} onClick={start}>Start</Button>
        </Stack>
        <Typography variant="caption" color="text.secondary">
          Only scan applications you are authorized to test. Mobile traffic is captured through the MITM proxy.
        </Typography>
      </Paper>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <Async state={state}>
        {(sessions) => (
          <Paper>
            <Table size="small">
              <TableHead><TableRow>
                <TableCell>ID</TableCell><TableCell>Name</TableCell><TableCell>Type</TableCell><TableCell>Status</TableCell>
                <TableCell>Endpoints</TableCell><TableCell>Started</TableCell><TableCell />
              </TableRow></TableHead>
              <TableBody>
                {sessions.map((s) => (
                  <TableRow key={s.id}>
                    <TableCell>{s.id}</TableCell><TableCell>{s.name}</TableCell><TableCell>{s.type}</TableCell>
                    <TableCell><Chip size="small" label={s.status} color={s.status === 'FAILED' ? 'error' : s.status === 'RUNNING' ? 'primary' : 'default'} /></TableCell>
                    <TableCell>{s.endpointsFound}</TableCell>
                    <TableCell>{new Date(s.startedAt).toLocaleString()}</TableCell>
                    <TableCell>{s.status === 'RUNNING' && <Button size="small" color="error" onClick={() => stop(s.id)}>Stop</Button>}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Box sx={{ p: 1 }}><Button size="small" onClick={() => setRefresh((r) => r + 1)}>Refresh</Button></Box>
          </Paper>
        )}
      </Async>
    </Box>
  );
}
