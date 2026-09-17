const { app, BrowserWindow, WebContentsView, session, shell, ipcMain, Menu } = require('electron');
const path = require('path');
const SimpleStore = require('./store');

// Keep global references to prevent garbage collection
let mainWindow = null;
let store = null;
const accountViews = new Map(); // accountId -> WebContentsView
let currentAccountId = null;

// UI Layout dimensions (in pixels)
let layoutConfig = {
  sidebarWidth: 68,
  titleBarHeight: 38,
  navBarHeight: 46,
  showNavBar: true
};

function createMainWindow() {
  store = new SimpleStore();
  const savedBounds = store.get('windowBounds', { width: 1380, height: 880 });

  mainWindow = new BrowserWindow({
    width: savedBounds.width || 1380,
    height: savedBounds.height || 880,
    x: savedBounds.x,
    y: savedBounds.y,
    minWidth: 900,
    minHeight: 600,
    frame: false, // Frameless for modern custom titlebar
    backgroundColor: '#0c0e14',
    title: 'Antigravity',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    }
  });

  // Remove default native menu
  Menu.setApplicationMenu(null);

  // Load UI Shell
  mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'));

  // Save bounds on resize / move
  const saveBounds = () => {
    if (!mainWindow.isMaximized() && !mainWindow.isMinimized()) {
      store.set('windowBounds', mainWindow.getBounds());
    }
  };
  mainWindow.on('resize', () => {
    saveBounds();
    updateActiveViewBounds();
  });
  mainWindow.on('move', saveBounds);

  mainWindow.on('maximize', () => {
    mainWindow.webContents.send('window:state-change', true);
    updateActiveViewBounds();
  });
  mainWindow.on('unmaximize', () => {
    mainWindow.webContents.send('window:state-change', false);
    updateActiveViewBounds();
  });

  // Setup accounts once UI shell is ready
  mainWindow.webContents.once('did-finish-load', () => {
    initAccounts();
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
    accountViews.clear();
  });
}

function initAccounts() {
  const accounts = store.getAccounts();
  const activeId = store.getActiveAccountId() || (accounts[0] && accounts[0].id);

  if (accounts.length === 0) {
    const defaultAcc = {
      id: 'acc_' + Date.now(),
      name: 'Work Account',
      color: '#3b82f6',
      url: store.get('settings').defaultUrl || 'https://antigravity.google',
      createdAt: Date.now()
    };
    store.addAccount(defaultAcc);
    switchAccount(defaultAcc.id);
  } else {
    // Switch to last active account
    switchAccount(activeId);
  }
}

function calculateViewBounds() {
  if (!mainWindow) return { x: 0, y: 0, width: 0, height: 0 };
  const bounds = mainWindow.getContentBounds();
  const x = layoutConfig.sidebarWidth;
  const y = layoutConfig.titleBarHeight + (layoutConfig.showNavBar ? layoutConfig.navBarHeight : 0);
  const width = Math.max(0, bounds.width - x);
  const height = Math.max(0, bounds.height - y);
  return { x, y, width, height };
}

function updateActiveViewBounds() {
  if (!currentAccountId || !accountViews.has(currentAccountId)) return;
  const view = accountViews.get(currentAccountId);
  const bounds = calculateViewBounds();
  view.setBounds(bounds);
}

function getOrCreateView(account) {
  if (accountViews.has(account.id)) {
    return accountViews.get(account.id);
  }

  // Create isolated persistent partition session for this account
  const partitionName = `persist:account_${account.id}`;
  
  // Create view
  const view = new WebContentsView({
    webPreferences: {
      partition: partitionName,
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      spellcheck: true
    }
  });

  const wc = view.webContents;

  // Custom User-Agent to ensure full compatibility with modern Google services
  const defaultUA = wc.getUserAgent();
  const cleanUA = defaultUA.replace(/Electron\/[0-9\.]+\s/, '');
  wc.setUserAgent(cleanUA);

function canGoBack(wc) {
  return wc.navigationHistory ? wc.navigationHistory.canGoBack() : wc.canGoBack();
}
function canGoForward(wc) {
  return wc.navigationHistory ? wc.navigationHistory.canGoForward() : wc.canGoForward();
}
function goBack(wc) {
  if (wc.navigationHistory) {
    if (wc.navigationHistory.canGoBack()) wc.navigationHistory.goBack();
  } else if (wc.canGoBack()) {
    wc.goBack();
  }
}
function goForward(wc) {
  if (wc.navigationHistory) {
    if (wc.navigationHistory.canGoForward()) wc.navigationHistory.goForward();
  } else if (wc.canGoForward()) {
    wc.goForward();
  }
}

  // Attach navigation & loading event listeners to send status to shell UI
  const notifyNavState = () => {
    if (account.id === currentAccountId && mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('nav:state-change', {
        canGoBack: canGoBack(wc),
        canGoForward: canGoForward(wc),
        url: wc.getURL(),
        title: wc.getTitle()
      });
    }
  };

  wc.on('did-start-loading', () => {
    if (account.id === currentAccountId && mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('nav:loading-change', true);
    }
  });

  wc.on('did-stop-loading', () => {
    if (account.id === currentAccountId && mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('nav:loading-change', false);
      notifyNavState();
    }
  });

  wc.on('did-navigate', notifyNavState);
  wc.on('did-navigate-in-page', notifyNavState);
  wc.on('page-title-updated', notifyNavState);

  // Handle popups (OAuth login like accounts.google.com vs external web links)
  wc.setWindowOpenHandler(({ url }) => {
    try {
      const parsed = new URL(url);
      const isAuthOrGoogle = parsed.hostname.includes('google.com') ||
                             parsed.hostname.includes('accounts.google') ||
                             parsed.hostname.includes('antigravity');

      if (isAuthOrGoogle) {
        // Open in a dedicated auth dialog window sharing this account's session
        return {
          action: 'allow',
          overrideBrowserWindowOptions: {
            parent: mainWindow,
            modal: true,
            width: 540,
            height: 680,
            autoHideMenuBar: true,
            webPreferences: {
              partition: partitionName,
              contextIsolation: true
            }
          }
        };
      } else {
        // Open standard external links in the user's default OS browser
        shell.openExternal(url);
        return { action: 'deny' };
      }
    } catch (e) {
      shell.openExternal(url);
      return { action: 'deny' };
    }
  });

  // Load the initial account URL
  const targetUrl = account.url || store.get('settings').defaultUrl || 'https://antigravity.google';
  wc.loadURL(targetUrl);

  accountViews.set(account.id, view);
  return view;
}

function switchAccount(accountId) {
  const accounts = store.getAccounts();
  const targetAccount = accounts.find(a => a.id === accountId);
  if (!targetAccount) return false;

  // If already active, just bring to focus
  if (currentAccountId === accountId && accountViews.has(accountId)) {
    const existingView = accountViews.get(accountId);
    existingView.webContents.focus();
    return true;
  }

  // Hide or detach current view
  if (currentAccountId && accountViews.has(currentAccountId)) {
    const oldView = accountViews.get(currentAccountId);
    mainWindow.contentView.removeChildView(oldView);
  }

  // Get or initialize new view
  const newView = getOrCreateView(targetAccount);
  mainWindow.contentView.addChildView(newView);
  currentAccountId = accountId;
  store.setActiveAccountId(accountId);

  updateActiveViewBounds();
  newView.webContents.focus();

  // Notify UI Shell
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('account:switched', {
      accountId,
      account: targetAccount,
      canGoBack: canGoBack(newView.webContents),
      canGoForward: canGoForward(newView.webContents),
      url: newView.webContents.getURL(),
      title: newView.webContents.getTitle()
    });
  }

  return true;
}

// Setup IPC handlers
function setupIpc() {
  ipcMain.handle('app:get-initial-state', () => {
    return {
      accounts: store.getAccounts(),
      activeAccountId: currentAccountId || store.getActiveAccountId(),
      settings: store.get('settings'),
      isMaximized: mainWindow ? mainWindow.isMaximized() : false
    };
  });

  ipcMain.handle('account:switch', (event, accountId) => {
    return switchAccount(accountId);
  });

  ipcMain.handle('account:create', (event, data) => {
    const newAcc = {
      id: 'acc_' + Date.now(),
      name: data.name || 'Account ' + (store.getAccounts().length + 1),
      color: data.color || '#6366f1',
      url: data.url || store.get('settings').defaultUrl || 'https://antigravity.google',
      createdAt: Date.now()
    };
    store.addAccount(newAcc);
    switchAccount(newAcc.id);
    return { accounts: store.getAccounts(), activeAccountId: newAcc.id };
  });

  ipcMain.handle('account:update', (event, id, data) => {
    const updated = store.updateAccount(id, data);
    return { accounts: store.getAccounts(), updated };
  });

  ipcMain.handle('account:delete', async (event, id) => {
    try {
      // If view is loaded in memory, remove it
      if (accountViews.has(id)) {
        const view = accountViews.get(id);
        if (currentAccountId === id) {
          mainWindow.contentView.removeChildView(view);
        }
        // Destroy session data for partition
        const sess = session.fromPartition(`persist:account_${id}`);
        await sess.clearStorageData();
        accountViews.delete(id);
      }

      const nextActiveId = store.deleteAccount(id);
      switchAccount(nextActiveId);
      return { success: true, accounts: store.getAccounts(), activeAccountId: nextActiveId };
    } catch (err) {
      return { success: false, error: err.message };
    }
  });

  // Navigation handlers
  ipcMain.handle('nav:go-back', () => {
    if (currentAccountId && accountViews.has(currentAccountId)) {
      const wc = accountViews.get(currentAccountId).webContents;
      goBack(wc);
    }
  });

  ipcMain.handle('nav:go-forward', () => {
    if (currentAccountId && accountViews.has(currentAccountId)) {
      const wc = accountViews.get(currentAccountId).webContents;
      goForward(wc);
    }
  });

  ipcMain.handle('nav:reload', () => {
    if (currentAccountId && accountViews.has(currentAccountId)) {
      accountViews.get(currentAccountId).webContents.reload();
    }
  });

  ipcMain.handle('nav:go-home', () => {
    if (currentAccountId && accountViews.has(currentAccountId)) {
      const accounts = store.getAccounts();
      const current = accounts.find(a => a.id === currentAccountId);
      const homeUrl = (current && current.url) || store.get('settings').defaultUrl || 'https://antigravity.google';
      accountViews.get(currentAccountId).webContents.loadURL(homeUrl);
    }
  });

  ipcMain.handle('nav:open-external', (event, url) => {
    const targetUrl = url || (currentAccountId && accountViews.has(currentAccountId) ? accountViews.get(currentAccountId).webContents.getURL() : null);
    if (targetUrl) shell.openExternal(targetUrl);
  });

  ipcMain.handle('nav:toggle-devtools', () => {
    if (currentAccountId && accountViews.has(currentAccountId)) {
      const wc = accountViews.get(currentAccountId).webContents;
      wc.toggleDevTools();
    }
  });

  ipcMain.handle('app:update-layout', (event, newLayout) => {
    layoutConfig = { ...layoutConfig, ...newLayout };
    updateActiveViewBounds();
    return layoutConfig;
  });

  ipcMain.handle('settings:update', (event, newSettings) => {
    const current = store.get('settings');
    const updated = { ...current, ...newSettings };
    store.set('settings', updated);
    if (updated.showNavBar !== undefined) {
      layoutConfig.showNavBar = updated.showNavBar;
      updateActiveViewBounds();
    }
    return updated;
  });

  // Window handlers
  ipcMain.handle('window:minimize', () => {
    if (mainWindow) mainWindow.minimize();
  });

  ipcMain.handle('window:maximize', () => {
    if (mainWindow) {
      if (mainWindow.isMaximized()) {
        mainWindow.unmaximize();
      } else {
        mainWindow.maximize();
      }
    }
  });

  ipcMain.handle('window:close', () => {
    if (mainWindow) mainWindow.close();
  });

  ipcMain.handle('window:is-maximized', () => {
    return mainWindow ? mainWindow.isMaximized() : false;
  });
}

// Global Keyboard Shortcuts via Window WebContents before-input-event
function setupShortcuts() {
  mainWindow.webContents.on('before-input-event', (event, input) => {
    if (input.type !== 'keyDown') return;

    // Ctrl + 1..9 to switch accounts
    if (input.control && !input.alt && !input.shift && input.key >= '1' && input.key <= '9') {
      const index = parseInt(input.key, 10) - 1;
      const accounts = store.getAccounts();
      if (accounts[index]) {
        event.preventDefault();
        switchAccount(accounts[index].id);
      }
    }

    // Ctrl + Tab to cycle accounts
    if (input.control && input.key === 'Tab') {
      event.preventDefault();
      const accounts = store.getAccounts();
      if (accounts.length > 1) {
        const curIdx = accounts.findIndex(a => a.id === currentAccountId);
        const nextIdx = input.shift
          ? (curIdx - 1 + accounts.length) % accounts.length
          : (curIdx + 1) % accounts.length;
        switchAccount(accounts[nextIdx].id);
      }
    }

    // Ctrl + R or F5 to reload webContents
    if ((input.control && input.key.toLowerCase() === 'r') || input.key === 'F5') {
      if (currentAccountId && accountViews.has(currentAccountId)) {
        event.preventDefault();
        accountViews.get(currentAccountId).webContents.reload();
      }
    }

    // Ctrl + Shift + I to toggle devtools on active web contents
    if (input.control && input.shift && input.key.toLowerCase() === 'i') {
      if (currentAccountId && accountViews.has(currentAccountId)) {
        event.preventDefault();
        accountViews.get(currentAccountId).webContents.toggleDevTools();
      }
    }
  });
}

// App lifecycle
app.whenReady().then(() => {
  setupIpc();
  createMainWindow();
  setupShortcuts();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createMainWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
