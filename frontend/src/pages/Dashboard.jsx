import React from 'react';
import { Box, Card, CardContent, Chip, Grid, Stack, Typography } from '@mui/material';
import { useFetch } from '../hooks.js';
import Async from '../Async.jsx';

const KPIS = [
  ['totalApis', 'Total APIs'],
  ['activeApis', 'Active APIs'],
  ['deprecatedApis', 'Deprecated APIs'],
  ['duplicateApis', 'Duplicate APIs'],
  ['unusedApis', 'Unused APIs'],
  ['thirdPartyApis', 'Third-party APIs'],
  ['discoverySessions', 'Discovery sessions'],
  ['openFindings', 'Open security findings'],
];

function Breakdown({ title, map }) {
  return (
    <Card>
      <CardContent>
        <Typography variant="subtitle1" gutterBottom>{title}</Typography>
        <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
          {Object.entries(map || {}).map(([k, v]) => <Chip key={k} label={`${k}: ${v}`} />)}
          {Object.keys(map || {}).length === 0 && <Typography variant="body2">No data yet</Typography>}
        </Stack>
      </CardContent>
    </Card>
  );
}

export default function Dashboard() {
  const state = useFetch('/statistics');
  return (
    <Async state={state}>
      {(s) => (
        <Box>
          <Typography variant="h5" gutterBottom>Dashboard</Typography>
          <Grid container spacing={2}>
            {KPIS.map(([key, label]) => (
              <Grid item xs={6} md={3} key={key}>
                <Card><CardContent>
                  <Typography variant="h4">{s[key]}</Typography>
                  <Typography color="text.secondary">{label}</Typography>
                </CardContent></Card>
              </Grid>
            ))}
            <Grid item xs={12} md={6}><Breakdown title="Authentication types" map={s.authTypes} /></Grid>
            <Grid item xs={12} md={6}><Breakdown title="Protocols" map={s.protocols} /></Grid>
            <Grid item xs={12} md={6}><Breakdown title="Findings by severity" map={s.findingsBySeverity} /></Grid>
            <Grid item xs={12} md={6}><Breakdown title="APIs per service" map={s.services} /></Grid>
          </Grid>
        </Box>
      )}
    </Async>
  );
}
