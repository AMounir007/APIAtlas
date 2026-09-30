import React, { useState } from 'react';
import { Alert, Box, Button, Card, CardActions, CardContent, Grid, TextField, Typography } from '@mui/material';
import { api } from '../api.js';

const EXPORTS = [
  ['swagger', 'OpenAPI / Swagger', 'openapi.json', 'OpenAPI 3.0 specification'],
  ['postman', 'Postman collection', 'postman_collection.json', 'Collection v2.1 grouped by service'],
  ['excel', 'Excel', 'api-atlas.xlsx', 'APIs and security findings'],
  ['csv', 'CSV', 'api-atlas.csv', 'Flat catalog report'],
  ['json', 'JSON', 'api-atlas.json', 'Raw catalog data'],
  ['html', 'HTML documentation', 'api-atlas.html', 'Self-contained documentation'],
  ['pdf', 'PDF', 'api-atlas.pdf', 'Printable catalog'],
  ['tests', 'REST Assured + TestNG tests', 'api-atlas-tests.zip', 'Data-driven Maven test project'],
];

export default function Exports() {
  const [service, setService] = useState('');
  const [error, setError] = useState(null);

  const run = async (key, file) => {
    try {
      const qs = service && key !== 'security-report' ? `?service=${encodeURIComponent(service)}` : '';
      await api.download(`/export/${key}${qs}`, file);
      setError(null);
    } catch (e) { setError(e.message); }
  };

  return (
    <Box>
      <Typography variant="h5" gutterBottom>Export Center</Typography>
      <TextField size="small" label="Limit to service (host[:port]), optional" value={service}
        onChange={(e) => setService(e.target.value)} sx={{ mb: 2, minWidth: 360 }} />
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <Grid container spacing={2}>
        {EXPORTS.map(([key, title, file, desc]) => (
          <Grid item xs={12} sm={6} md={3} key={key}>
            <Card>
              <CardContent><Typography variant="subtitle1">{title}</Typography><Typography variant="body2" color="text.secondary">{desc}</Typography></CardContent>
              <CardActions><Button onClick={() => run(key, file)}>Download</Button></CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
