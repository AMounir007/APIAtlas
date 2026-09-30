import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Chip, MenuItem, Paper, Stack, Table, TableBody, TableCell, TableHead, TablePagination, TableRow, TextField, Typography,
} from '@mui/material';
import { useFetch } from '../hooks.js';
import Async from '../Async.jsx';

const METHODS = ['', 'GET', 'POST', 'PUT', 'PATCH', 'DELETE'];
const AUTH = ['', 'NONE', 'BASIC', 'JWT', 'OAUTH2', 'API_KEY'];
const STATUS = ['', 'ACTIVE', 'DEPRECATED', 'UNUSED'];

/** API Inventory + Search: filterable, paginated catalog. Click a row to open the API Explorer. */
export default function Inventory() {
  const nav = useNavigate();
  const [q, setQ] = useState('');
  const [method, setMethod] = useState('');
  const [auth, setAuth] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const params = new URLSearchParams({ page, size });
  if (q) params.set('q', q);
  if (method) params.set('method', method);
  if (auth) params.set('auth', auth);
  if (status) params.set('status', status);
  const state = useFetch(`/apis?${params}`);

  const select = (label, value, set, options) => (
    <TextField select size="small" label={label} value={value} sx={{ minWidth: 130 }}
      onChange={(e) => { set(e.target.value); setPage(0); }}>
      {options.map((o) => <MenuItem key={o} value={o}>{o || 'All'}</MenuItem>)}
    </TextField>
  );

  return (
    <Box>
      <Typography variant="h5" gutterBottom>API Inventory</Typography>
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField size="small" label="Search" value={q} sx={{ flexGrow: 1 }}
          onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        {select('Method', method, setMethod, METHODS)}
        {select('Auth', auth, setAuth, AUTH)}
        {select('Status', status, setStatus, STATUS)}
      </Stack>
      <Async state={state}>
        {(p) => (
          <Paper>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Method</TableCell><TableCell>Service</TableCell><TableCell>Endpoint</TableCell>
                  <TableCell>Auth</TableCell><TableCell>Status</TableCell><TableCell>Last seen</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {p.content.map((a) => (
                  <TableRow key={a.id} hover sx={{ cursor: 'pointer' }} onClick={() => nav(`/apis/${a.id}`)}>
                    <TableCell><Chip size="small" label={a.method} /></TableCell>
                    <TableCell>{a.service}</TableCell>
                    <TableCell>{a.url}</TableCell>
                    <TableCell>{a.authType}</TableCell>
                    <TableCell>{a.status}</TableCell>
                    <TableCell>{new Date(a.lastSeen).toLocaleString()}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <TablePagination component="div" count={p.totalElements} page={page} rowsPerPage={size}
              onPageChange={(_, n) => setPage(n)}
              onRowsPerPageChange={(e) => { setSize(Number(e.target.value)); setPage(0); }} />
          </Paper>
        )}
      </Async>
    </Box>
  );
}
