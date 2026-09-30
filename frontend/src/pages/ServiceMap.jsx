import React from 'react';
import { Box, Paper, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useFetch } from '../hooks.js';
import Async from '../Async.jsx';

const SIZE = 520;

/** Service Map / Dependency Map: services on a circle, arrows show observed call sequences. */
export default function ServiceMap() {
  const state = useFetch('/map/services');
  return (
    <Async state={state}>
      {({ nodes, edges }) => {
        const c = SIZE / 2;
        const r = c - 90;
        const pos = Object.fromEntries(nodes.map((n, i) => {
          const a = (2 * Math.PI * i) / Math.max(nodes.length, 1);
          return [n.id, { x: c + r * Math.cos(a), y: c + r * Math.sin(a) }];
        }));
        return (
          <Box>
            <Typography variant="h5" gutterBottom>Service Map</Typography>
            <Paper sx={{ p: 2, mb: 2, overflow: 'auto' }}>
              <svg width={SIZE} height={SIZE} role="img" aria-label="Service dependency graph">
                <defs>
                  <marker id="arrow" markerWidth="8" markerHeight="8" refX="8" refY="4" orient="auto">
                    <path d="M0,0 L8,4 L0,8 z" fill="#888" />
                  </marker>
                </defs>
                {edges.map((e) => pos[e.source] && pos[e.target] && (
                  <line key={`${e.source}>${e.target}`} x1={pos[e.source].x} y1={pos[e.source].y}
                    x2={pos[e.target].x} y2={pos[e.target].y} stroke="#888"
                    strokeWidth={Math.min(1 + Math.log2(e.weight + 1), 6)} markerEnd="url(#arrow)" />
                ))}
                {nodes.map((n) => (
                  <g key={n.id}>
                    <circle cx={pos[n.id].x} cy={pos[n.id].y} r={10 + Math.min(n.endpoints, 20)}
                      fill={n.thirdParty ? '#ed6c02' : '#1e5eff'} opacity="0.85" />
                    <text x={pos[n.id].x} y={pos[n.id].y - 16 - Math.min(n.endpoints, 20)} textAnchor="middle" fontSize="11">{n.id}</text>
                  </g>
                ))}
              </svg>
              <Typography variant="caption">Blue: first-party service, orange: third-party. Circle size = number of APIs.</Typography>
            </Paper>
            <Paper>
              <Table size="small">
                <TableHead><TableRow><TableCell>From</TableCell><TableCell>To</TableCell><TableCell>Observed calls</TableCell></TableRow></TableHead>
                <TableBody>
                  {edges.map((e) => (
                    <TableRow key={`${e.source}>${e.target}`}><TableCell>{e.source}</TableCell><TableCell>{e.target}</TableCell><TableCell>{e.weight}</TableCell></TableRow>
                  ))}
                </TableBody>
              </Table>
            </Paper>
          </Box>
        );
      }}
    </Async>
  );
}
