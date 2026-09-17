const fs = require('fs');
const path = require('path');
const { app } = require('electron');

class SimpleStore {
  constructor(filename = 'antigravity-config.json') {
    const userDataPath = app.getPath('userData');
    this.filePath = path.join(userDataPath, filename);
    this.data = this.load();
  }

  getDefaultData() {
    return {
      accounts: [
        {
          id: 'acc_default_1',
          name: 'Primary Account',
          color: '#3b82f6',
          url: 'https://antigravity.google',
          createdAt: Date.now()
        }
      ],
      activeAccountId: 'acc_default_1',
      settings: {
        defaultUrl: 'https://antigravity.google',
        showNavBar: true,
        zenMode: false,
        hardwareAcceleration: true
      },
      windowBounds: {
        width: 1360,
        height: 860,
        x: undefined,
        y: undefined
      }
    };
  }

  load() {
    try {
      if (fs.existsSync(this.filePath)) {
        const raw = fs.readFileSync(this.filePath, 'utf8');
        const parsed = JSON.parse(raw);
        return { ...this.getDefaultData(), ...parsed };
      }
    } catch (err) {
      console.error('[Store] Error loading config:', err);
    }
    return this.getDefaultData();
  }

  save() {
    try {
      const dir = path.dirname(this.filePath);
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
      }
      fs.writeFileSync(this.filePath, JSON.stringify(this.data, null, 2), 'utf8');
    } catch (err) {
      console.error('[Store] Error saving config:', err);
    }
  }

  get(key, defaultValue) {
    return this.data[key] !== undefined ? this.data[key] : defaultValue;
  }

  set(key, value) {
    this.data[key] = value;
    this.save();
  }

  getAccounts() {
    return this.data.accounts || [];
  }

  getActiveAccountId() {
    return this.data.activeAccountId || (this.data.accounts[0] && this.data.accounts[0].id);
  }

  setActiveAccountId(id) {
    this.data.activeAccountId = id;
    this.save();
  }

  addAccount(account) {
    if (!this.data.accounts) {
      this.data.accounts = [];
    }
    this.data.accounts.push(account);
    this.save();
    return account;
  }

  updateAccount(id, updates) {
    const idx = this.data.accounts.findIndex(a => a.id === id);
    if (idx !== -1) {
      this.data.accounts[idx] = { ...this.data.accounts[idx], ...updates };
      this.save();
      return this.data.accounts[idx];
    }
    return null;
  }

  deleteAccount(id) {
    if (this.data.accounts.length <= 1) {
      throw new Error('Cannot delete the only remaining account');
    }
    this.data.accounts = this.data.accounts.filter(a => a.id !== id);
    if (this.data.activeAccountId === id) {
      this.data.activeAccountId = this.data.accounts[0].id;
    }
    this.save();
    return this.data.activeAccountId;
  }
}

module.exports = SimpleStore;
