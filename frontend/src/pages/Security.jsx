import React from 'react';
import { Box, Button, Chip, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { api } from '../api.js';
import { useFetch } from '../hooks.js';
import Async from '../Async.jsx';

const COLOR = { CRITICAL: 'error', HIGH: 'error', MEDIUM: 'warning', LOW: 'info', INFO: 'default' };

export default function Security() {
  const state = useFetch('/security');
  return (
    <Async state={state}>
      {(r) => (
        <Box>
          <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 2 }}>
            <Typography variant="h5" sx={{ flexGrow: 1 }}>Security Findings ({r.totalOpen} open)</Typography>
            <Button variant="outlined" onClick={() => api.download('/export/security-report', 'security-report.html')}>
              Download report
            </Button>
          </Stack>
          <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
            {Object.entries(r.bySeverity).map(([k, v]) => <Chip key={k} color={COLOR[k]} label={`${k}: ${v}`} />)}
          </Stack>
          <Paper>
            <Table size="small">
              <TableHead><TableRow>
                <TableCell>Severity</TableCell><TableCell>Endpoint</TableCell><TableCell>OWASP</TableCell><TableCell>Finding</TableCell>
              </TableRow></TableHead>
              <TableBody>
                {r.findings.map((f) => (
                  <TableRow key={f.id}>
                    <TableCell><Chip size="small" color={COLOR[f.severity]} label={f.severity} /></TableCell>
                    <TableCell>{f.endpointLabel}</TableCell>
                    <TableCell>{f.owasp}</TableCell>
                    <TableCell><b>{f.title}</b><br />{f.description}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        </Box>
      )}
    </Async>
  );
}
