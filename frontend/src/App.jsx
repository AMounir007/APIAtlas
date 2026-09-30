import React, { useState } from 'react';
import { Link as RouterLink, Route, Routes, useLocation } from 'react-router-dom';
import {
  AppBar, Box, Button, Drawer, List, ListItemButton, ListItemIcon, ListItemText, Toolbar, Typography,
} from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import ApiIcon from '@mui/icons-material/Api';
import HubIcon from '@mui/icons-material/Hub';
import QueryStatsIcon from '@mui/icons-material/QueryStats';
import SecurityIcon from '@mui/icons-material/Security';
import DescriptionIcon from '@mui/icons-material/Description';
import DownloadIcon from '@mui/icons-material/Download';
import TravelExploreIcon from '@mui/icons-material/TravelExplore';
import { getToken, setToken } from './api.js';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Inventory from './pages/Inventory.jsx';
import Explorer from './pages/Explorer.jsx';
import ServiceMap from './pages/ServiceMap.jsx';
import Traffic from './pages/Traffic.jsx';
import Security from './pages/Security.jsx';
import SwaggerViewer from './pages/SwaggerViewer.jsx';
import Exports from './pages/Exports.jsx';
import Discovery from './pages/Discovery.jsx';

const NAV = [
  ['/', 'Dashboard', <DashboardIcon key="d" />],
  ['/apis', 'API Inventory', <ApiIcon key="a" />],
  ['/discovery', 'Discovery', <TravelExploreIcon key="t" />],
  ['/map', 'Service Map', <HubIcon key="h" />],
  ['/traffic', 'Traffic Analytics', <QueryStatsIcon key="q" />],
  ['/security', 'Security Findings', <SecurityIcon key="s" />],
  ['/swagger', 'Swagger Viewer', <DescriptionIcon key="w" />],
  ['/exports', 'Export Center', <DownloadIcon key="e" />],
];
const WIDTH = 240;

export default function App() {
  const [authed, setAuthed] = useState(!!getToken());
  const { pathname } = useLocation();

  if (!authed) return <Login onLogin={() => setAuthed(true)} />;

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            API Atlas <Typography component="span" variant="caption">Discover. Map. Document.</Typography>
          </Typography>
          <Button color="inherit" onClick={() => { setToken(null); setAuthed(false); }}>Sign out</Button>
        </Toolbar>
      </AppBar>
      <Drawer variant="permanent" sx={{ width: WIDTH, '& .MuiDrawer-paper': { width: WIDTH, boxSizing: 'border-box' } }}>
        <Toolbar />
        <List>
          {NAV.map(([to, label, icon]) => (
            <ListItemButton key={to} component={RouterLink} to={to} selected={to === '/' ? pathname === '/' : pathname.startsWith(to)}>
              <ListItemIcon>{icon}</ListItemIcon>
              <ListItemText primary={label} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: 3, minWidth: 0 }}>
        <Toolbar />
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/apis" element={<Inventory />} />
          <Route path="/apis/:id" element={<Explorer />} />
          <Route path="/discovery" element={<Discovery />} />
          <Route path="/map" element={<ServiceMap />} />
          <Route path="/traffic" element={<Traffic />} />
          <Route path="/security" element={<Security />} />
          <Route path="/swagger" element={<SwaggerViewer />} />
          <Route path="/exports" element={<Exports />} />
        </Routes>
      </Box>
    </Box>
  );
}
