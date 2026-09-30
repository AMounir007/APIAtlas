import React from 'react';
import { Box, Paper, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useFetch } from '../hooks.js';
import Async from '../Async.jsx';

const SECTIONS = [
  ['mostCalled', 'Most called'],
  ['slowest', 'Slowest (average response time)'],
  ['mostErrors', 'Most errors'],
];

export default function Traffic() {
  const state = useFetch('/statistics/traffic');
  return (
    <Async state={state}>
      {(data) => (
        <Box>
          <Typography variant="h5" gutterBottom>Traffic Analytics</Typography>
          {SECTIONS.map(([key, title]) => (
            <Paper key={key} sx={{ mb: 3 }}>
              <Typography variant="subtitle1" sx={{ p: 2 }}>{title}</Typography>
              <Table size="small">
                <TableHead><TableRow>
                  <TableCell>Endpoint</TableCell><TableCell align="right">Calls</TableCell><TableCell align="right">Errors</TableCell>
                  <TableCell align="right">Avg ms</TableCell><TableCell align="right">Max ms</TableCell>
                </TableRow></TableHead>
                <TableBody>
                  {data[key].map((r) => (
                    <TableRow key={r.endpointId}>
                      <TableCell>{r.endpoint}</TableCell><TableCell align="right">{r.calls}</TableCell>
                      <TableCell align="right">{r.errors}</TableCell><TableCell align="right">{r.avgResponseMs}</TableCell>
                      <TableCell align="right">{r.maxResponseMs}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Paper>
          ))}
        </Box>
      )}
    </Async>
  );
}
