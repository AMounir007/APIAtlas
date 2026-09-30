import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { Alert, Box, Button, Chip, Paper, Stack, Typography } from '@mui/material';
import { api } from '../api.js';
import { useFetch } from '../hooks.js';
import Async from '../Async.jsx';

const Block = ({ title, text }) => text ? (
  <Box sx={{ mb: 2 }}>
    <Typography variant="subtitle2">{title}</Typography>
    {/* React escapes text content, so untrusted captured data cannot inject markup */}
    <Paper variant="outlined" sx={{ p: 1, whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 13, overflow: 'auto' }}>{text}</Paper>
  </Box>
) : null;

/** API Explorer: full detail of one endpoint. */
export default function Explorer() {
  const { id } = useParams();
  const [version, setVersion] = useState(0);
  const [msg, setMsg] = useState(null);
  const state = useFetch(`/apis/${id}?v=${version}`);

  const analyze = async () => {
    try { await api.post(`/apis/${id}/analyze`); setVersion((v) => v + 1); setMsg(null); }
    catch (e) { setMsg(e.message); }
  };

  return (
    <Async state={state}>
      {({ endpoint: e, tags, findings }) => (
        <Box>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
            <Chip color="primary" label={e.method} />
            <Typography variant="h6" sx={{ wordBreak: 'break-all' }}>{e.url}</Typography>
          </Stack>
          <Stack direction="row" spacing={1} sx={{ mb: 2 }} useFlexGap flexWrap="wrap">
            <Chip label={`Auth: ${e.authType}`} /><Chip label={e.status} /><Chip label={e.protocol} />
            {e.version && <Chip label={e.version} />}
            {e.thirdParty && <Chip color="warning" label="Third-party" />}
            {tags.map((t) => <Chip key={t} variant="outlined" label={t} />)}
            <Button size="small" onClick={analyze}>Re-run AI analysis</Button>
          </Stack>
          {msg && <Alert severity="error" sx={{ mb: 2 }}>{msg}</Alert>}
          <Block title="Description" text={e.description} />
          <Block title="Business purpose" text={e.businessPurpose} />
          <Block title="Request description" text={e.requestDescription} />
          <Block title="Response description" text={e.responseDescription} />
          <Block title="Sample request" text={e.sampleRequest} />
          <Block title="Sample response" text={e.sampleResponse} />
          <Block title="Test recommendations" text={e.testRecommendations} />
          {findings.length > 0 && (
            <Box>
              <Typography variant="subtitle2">Security findings</Typography>
              {findings.map((f) => (
                <Alert key={f.id} severity={['HIGH', 'CRITICAL'].includes(f.severity) ? 'error' : 'warning'} sx={{ mb: 1 }}>
                  <b>{f.title}</b> ({f.owasp}) - {f.description}
                </Alert>
              ))}
            </Box>
          )}
        </Box>
      )}
    </Async>
  );
}
