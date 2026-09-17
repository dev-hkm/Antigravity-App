const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
  // Accounts
  getInitialState: () => ipcRenderer.invoke('app:get-initial-state'),
  switchAccount: (accountId) => ipcRenderer.invoke('account:switch', accountId),
  createAccount: (data) => ipcRenderer.invoke('account:create', data),
  updateAccount: (id, data) => ipcRenderer.invoke('account:update', id, data),
  deleteAccount: (id) => ipcRenderer.invoke('account:delete', id),

  // Navigation
  goBack: () => ipcRenderer.invoke('nav:go-back'),
  goForward: () => ipcRenderer.invoke('nav:go-forward'),
  reload: () => ipcRenderer.invoke('nav:reload'),
  goHome: () => ipcRenderer.invoke('nav:go-home'),
  openExternal: (url) => ipcRenderer.invoke('nav:open-external', url),
  toggleDevTools: () => ipcRenderer.invoke('nav:toggle-devtools'),

  // Settings & View layout
  updateLayout: (layout) => ipcRenderer.invoke('app:update-layout', layout),
  updateSettings: (settings) => ipcRenderer.invoke('settings:update', settings),

  // Window Controls
  minimizeWindow: () => ipcRenderer.invoke('window:minimize'),
  maximizeWindow: () => ipcRenderer.invoke('window:maximize'),
  closeWindow: () => ipcRenderer.invoke('window:close'),
  isMaximized: () => ipcRenderer.invoke('window:is-maximized'),

  // Events from Main process
  onNavStateChange: (callback) => {
    const sub = (event, data) => callback(data);
    ipcRenderer.on('nav:state-change', sub);
    return () => ipcRenderer.removeListener('nav:state-change', sub);
  },
  onLoadingChange: (callback) => {
    const sub = (event, data) => callback(data);
    ipcRenderer.on('nav:loading-change', sub);
    return () => ipcRenderer.removeListener('nav:loading-change', sub);
  },
  onAccountSwitched: (callback) => {
    const sub = (event, data) => callback(data);
    ipcRenderer.on('account:switched', sub);
    return () => ipcRenderer.removeListener('account:switched', sub);
  },
  onShortcutTriggered: (callback) => {
    const sub = (event, data) => callback(data);
    ipcRenderer.on('shortcut:action', sub);
    return () => ipcRenderer.removeListener('shortcut:action', sub);
  },
  onWindowStateChange: (callback) => {
    const sub = (event, isMax) => callback(isMax);
    ipcRenderer.on('window:state-change', sub);
    return () => ipcRenderer.removeListener('window:state-change', sub);
  }
});
