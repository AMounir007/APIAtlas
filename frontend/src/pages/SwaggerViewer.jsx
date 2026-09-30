import React from 'react';
import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';
import { Box, Typography } from '@mui/material';
import { useFetch } from '../hooks.js';
import Async from '../Async.jsx';

/** Renders the generated OpenAPI document of the whole catalog. */
export default function SwaggerViewer() {
  const state = useFetch('/export/swagger');
  return (
    <Box>
      <Typography variant="h5" gutterBottom>Swagger Viewer</Typography>
      <Async state={state}>{(spec) => <SwaggerUI spec={spec} docExpansion="list" />}</Async>
    </Box>
  );
}
