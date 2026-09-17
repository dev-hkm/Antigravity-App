// State
let accounts = [];
let activeAccountId = null;
let settings = {};
let selectedColor = '#3b82f6';

// DOM Elements
const accountsListEl = document.getElementById('accountsList');
const titleActiveAccountEl = document.getElementById('titleActiveAccount');
const titleBadgeDotEl = document.getElementById('titleBadgeDot');
const titleBadgeNameEl = document.getElementById('titleBadgeName');

// Window buttons
const btnMinimize = document.getElementById('btnMinimize');
const btnMaximize = document.getElementById('btnMaximize');
const btnClose = document.getElementById('btnClose');
const iconMaximize = document.getElementById('iconMaximize');
const iconRestore = document.getElementById('iconRestore');

// Nav buttons
const btnNavBack = document.getElementById('btnNavBack');
const btnNavForward = document.getElementById('btnNavForward');
const btnNavReload = document.getElementById('btnNavReload');
const iconReload = document.getElementById('iconReload');
const btnNavHome = document.getElementById('btnNavHome');
const urlDisplayEl = document.getElementById('urlDisplay');
const progressIndicator = document.getElementById('progressIndicator');
const navbarEl = document.getElementById('navbar');
const btnRestoreNavbar = document.getElementById('btnRestoreNavbar');
const btnToggleZen = document.getElementById('btnToggleZen');
const btnToggleDevTools = document.getElementById('btnToggleDevTools');
const btnOpenExternal = document.getElementById('btnOpenExternal');
const btnEditCurrentAccount = document.getElementById('btnEditCurrentAccount');

// Modals
const accountModal = document.getElementById('accountModal');
const modalTitle = document.getElementById('modalTitle');
const accountForm = document.getElementById('accountForm');
const editAccountIdInput = document.getElementById('editAccountId');
const accNameInput = document.getElementById('accName');
const accUrlInput = document.getElementById('accUrl');
const colorPalette = document.getElementById('colorPalette');
const btnDeleteAccount = document.getElementById('btnDeleteAccount');
const btnCancelAccountModal = document.getElementById('btnCancelAccountModal');
const btnCloseAccountModal = document.getElementById('btnCloseAccountModal');

// Settings modal
const settingsModal = document.getElementById('settingsModal');
const btnOpenSettings = document.getElementById('btnOpenSettings');
const btnCloseSettingsModal = document.getElementById('btnCloseSettingsModal');
const btnDoneSettings = document.getElementById('btnDoneSettings');
const toggleNavBarSetting = document.getElementById('toggleNavBarSetting');

// Add account button
const btnAddAccount = document.getElementById('btnAddAccount');

// Initialize application
async function init() {
  const state = await window.api.getInitialState();
  accounts = state.accounts || [];
  activeAccountId = state.activeAccountId;
  settings = state.settings || {};

  updateWindowState(state.isMaximized);
  applyNavBarVisibility(settings.showNavBar !== false);
  renderAccounts();
  updateActiveAccountUI();
  setupEventListeners();
}

function renderAccounts() {
  accountsListEl.innerHTML = '';

  accounts.forEach((acc, index) => {
    const isActive = acc.id === activeAccountId;
    const wrapper = document.createElement('div');
    wrapper.className = `account-item-wrapper ${isActive ? 'active' : ''}`;
    wrapper.dataset.id = acc.id;

    // Pill indicator on the edge
    const pill = document.createElement('div');
    pill.className = 'account-pill-indicator';
    if (isActive) {
      pill.style.backgroundColor = acc.color || '#3b82f6';
    }

    // Avatar button
    const btn = document.createElement('button');
    btn.className = 'account-avatar-btn';
    btn.style.backgroundColor = acc.color || '#3b82f6';
    btn.title = `${acc.name}\n(Ctrl + ${index + 1})`;

    // Get initials (up to 2 letters)
    const initials = acc.name
      .split(' ')
      .filter(Boolean)
      .map(part => part[0])
      .slice(0, 2)
      .join('')
      .toUpperCase() || 'A';
    btn.textContent = initials;

    // Number badge (Ctrl+1, Ctrl+2, etc.)
    if (index < 9) {
      const badge = document.createElement('span');
      badge.className = 'account-index-badge';
      badge.textContent = index + 1;
      btn.appendChild(badge);
    }

    // Click to switch account
    btn.addEventListener('click', () => {
      if (acc.id !== activeAccountId) {
        switchAccount(acc.id);
      }
    });

    // Right-click or double-click to edit account
    btn.addEventListener('contextmenu', (e) => {
      e.preventDefault();
      openAccountModal(acc);
    });

    wrapper.appendChild(pill);
    wrapper.appendChild(btn);
    accountsListEl.appendChild(wrapper);
  });
}

function updateActiveAccountUI() {
  const currentAcc = accounts.find(a => a.id === activeAccountId);
  if (currentAcc) {
    titleBadgeDotEl.style.backgroundColor = currentAcc.color || '#3b82f6';
    titleBadgeDotEl.style.boxShadow = `0 0 8px ${currentAcc.color || '#3b82f6'}`;
    titleBadgeNameEl.textContent = currentAcc.name;
    document.title = `${currentAcc.name} - Antigravity`;
  }
}

async function switchAccount(id) {
  activeAccountId = id;
  renderAccounts();
  updateActiveAccountUI();
  await window.api.switchAccount(id);
}

function openAccountModal(accountToEdit = null) {
  if (accountToEdit) {
    modalTitle.textContent = 'Chỉnh sửa tài khoản';
    editAccountIdInput.value = accountToEdit.id;
    accNameInput.value = accountToEdit.name;
    accUrlInput.value = accountToEdit.url || 'https://antigravity.google';
    selectedColor = accountToEdit.color || '#3b82f6';

    // Show delete button only if more than 1 account exists
    if (accounts.length > 1) {
      btnDeleteAccount.classList.remove('hidden');
    } else {
      btnDeleteAccount.classList.add('hidden');
    }
  } else {
    modalTitle.textContent = 'Thêm tài khoản mới';
    editAccountIdInput.value = '';
    accNameInput.value = '';
    accUrlInput.value = settings.defaultUrl || 'https://antigravity.google';
    selectedColor = '#3b82f6';
    btnDeleteAccount.classList.add('hidden');
  }

  // Select color dot
  document.querySelectorAll('.color-dot').forEach(dot => {
    dot.classList.toggle('active', dot.dataset.color === selectedColor);
  });

  accountModal.classList.remove('hidden');
  accNameInput.focus();
}

function closeAccountModal() {
  accountModal.classList.add('hidden');
}

function applyNavBarVisibility(show) {
  settings.showNavBar = show;
  toggleNavBarSetting.checked = show;
  if (show) {
    navbarEl.classList.remove('hidden');
    btnRestoreNavbar.classList.add('hidden');
  } else {
    navbarEl.classList.add('hidden');
    btnRestoreNavbar.classList.remove('hidden');
  }
  // Notify main process to recalculate webContentsView bounds
  window.api.updateSettings({ showNavBar: show });
}

function updateWindowState(isMaximized) {
  if (isMaximized) {
    iconMaximize.classList.add('hidden');
    iconRestore.classList.remove('hidden');
  } else {
    iconMaximize.classList.remove('hidden');
    iconRestore.classList.add('hidden');
  }
}

function setupEventListeners() {
  // Window controls
  btnMinimize.addEventListener('click', () => window.api.minimizeWindow());
  btnMaximize.addEventListener('click', () => window.api.maximizeWindow());
  btnClose.addEventListener('click', () => window.api.closeWindow());

  // Navigation controls
  btnNavBack.addEventListener('click', () => window.api.goBack());
  btnNavForward.addEventListener('click', () => window.api.goForward());
  btnNavReload.addEventListener('click', () => window.api.reload());
  btnNavHome.addEventListener('click', () => window.api.goHome());
  btnOpenExternal.addEventListener('click', () => window.api.openExternal());
  btnToggleDevTools.addEventListener('click', () => window.api.toggleDevTools());

  // Zen Mode toggle
  btnToggleZen.addEventListener('click', () => {
    applyNavBarVisibility(false);
  });
  btnRestoreNavbar.addEventListener('click', () => {
    applyNavBarVisibility(true);
  });

  // Edit current account
  btnEditCurrentAccount.addEventListener('click', () => {
    const currentAcc = accounts.find(a => a.id === activeAccountId);
    if (currentAcc) openAccountModal(currentAcc);
  });

  // Add Account Button
  btnAddAccount.addEventListener('click', () => {
    openAccountModal(null);
  });

  // Color picker
  colorPalette.addEventListener('click', (e) => {
    const dot = e.target.closest('.color-dot');
    if (dot) {
      document.querySelectorAll('.color-dot').forEach(d => d.classList.remove('active'));
      dot.classList.add('active');
      selectedColor = dot.dataset.color;
    }
  });

  // Save Account Form
  accountForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const editId = editAccountIdInput.value.trim();
    const name = accNameInput.value.trim() || 'Tài khoản mới';
    const url = accUrlInput.value.trim() || 'https://antigravity.google';

    if (editId) {
      // Update
      const res = await window.api.updateAccount(editId, { name, url, color: selectedColor });
      accounts = res.accounts;
      renderAccounts();
      updateActiveAccountUI();
    } else {
      // Create new
      const res = await window.api.createAccount({ name, url, color: selectedColor });
      accounts = res.accounts;
      activeAccountId = res.activeAccountId;
      renderAccounts();
      updateActiveAccountUI();
    }
    closeAccountModal();
  });

  // Delete Account
  btnDeleteAccount.addEventListener('click', async () => {
    const editId = editAccountIdInput.value.trim();
    if (!editId) return;

    if (confirm('Bạn có chắc chắn muốn xóa tài khoản này và dữ liệu phiên làm việc của nó không?')) {
      const res = await window.api.deleteAccount(editId);
      if (res.success) {
        accounts = res.accounts;
        activeAccountId = res.activeAccountId;
        renderAccounts();
        updateActiveAccountUI();
        closeAccountModal();
      } else {
        alert(res.error || 'Không thể xóa tài khoản');
      }
    }
  });

  btnCancelAccountModal.addEventListener('click', closeAccountModal);
  btnCloseAccountModal.addEventListener('click', closeAccountModal);

  // Settings modal
  btnOpenSettings.addEventListener('click', () => {
    settingsModal.classList.remove('hidden');
  });
  btnCloseSettingsModal.addEventListener('click', () => {
    settingsModal.classList.add('hidden');
  });
  btnDoneSettings.addEventListener('click', () => {
    settingsModal.classList.add('hidden');
  });

  toggleNavBarSetting.addEventListener('change', (e) => {
    applyNavBarVisibility(e.target.checked);
  });

  // Modal backdrop click to close
  window.addEventListener('click', (e) => {
    if (e.target === accountModal) closeAccountModal();
    if (e.target === settingsModal) settingsModal.classList.add('hidden');
  });

  // Main process event subscribers
  window.api.onNavStateChange((data) => {
    btnNavBack.disabled = !data.canGoBack;
    btnNavForward.disabled = !data.canGoForward;
    if (data.url) {
      urlDisplayEl.textContent = data.url;
    }
  });

  window.api.onLoadingChange((isLoading) => {
    if (isLoading) {
      progressIndicator.classList.add('active');
      iconReload.classList.add('spinning');
    } else {
      progressIndicator.classList.remove('active');
      iconReload.classList.remove('spinning');
    }
  });

  window.api.onAccountSwitched((data) => {
    activeAccountId = data.accountId;
    renderAccounts();
    updateActiveAccountUI();
    btnNavBack.disabled = !data.canGoBack;
    btnNavForward.disabled = !data.canGoForward;
    if (data.url) {
      urlDisplayEl.textContent = data.url;
    }
  });

  window.api.onWindowStateChange((isMax) => {
    updateWindowState(isMax);
  });
}

// Start
init();
