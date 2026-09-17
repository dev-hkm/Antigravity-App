package com.hkm.antigravity.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hkm.antigravity.data.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("antigravity_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _accountsFlow = MutableStateFlow<List<Account>>(emptyList())
    val accountsFlow: StateFlow<List<Account>> = _accountsFlow.asStateFlow()

    private val _activeAccountFlow = MutableStateFlow<Account?>(null)
    val activeAccountFlow: StateFlow<Account?> = _activeAccountFlow.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val json = prefs.getString(KEY_ACCOUNTS, null)
        val list: MutableList<Account> = if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<Account>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } else {
            mutableListOf()
        }

        if (list.isEmpty()) {
            val defaultAcc = Account(
                id = "acc_" + System.currentTimeMillis(),
                name = "Primary Account",
                colorHex = "#3b82f6",
                url = "https://antigravity.google"
            )
            list.add(defaultAcc)
            saveAccountsList(list)
        }

        _accountsFlow.value = list

        val savedActiveId = prefs.getString(KEY_ACTIVE_ID, null)
        val active = list.find { it.id == savedActiveId } ?: list.first()
        _activeAccountFlow.value = active
    }

    private fun saveAccountsList(list: List<Account>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_ACCOUNTS, json).apply()
    }

    fun getActiveAccount(): Account? = _activeAccountFlow.value

    fun getAccounts(): List<Account> = _accountsFlow.value

    fun saveCurrentAccountCookies(url: String) {
        val current = _activeAccountFlow.value ?: return
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(url) ?: ""
        updateAccountCookies(current.id, cookies)
    }

    private fun updateAccountCookies(accountId: String, cookies: String) {
        val currentList = _accountsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == accountId }
        if (index != -1) {
            val updated = currentList[index].copy(cookies = cookies)
            currentList[index] = updated
            _accountsFlow.value = currentList
            if (_activeAccountFlow.value?.id == accountId) {
                _activeAccountFlow.value = updated
            }
            saveAccountsList(currentList)
        }
    }

    fun applyAccountCookies(account: Account, targetUrl: String) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        // Clear existing cookies to avoid cross-account contamination
        cookieManager.removeAllCookies {
            if (account.cookies.isNotEmpty()) {
                val parts = account.cookies.split(";")
                for (part in parts) {
                    val trimmed = part.trim()
                    if (trimmed.isNotEmpty()) {
                        cookieManager.setCookie(targetUrl, trimmed)
                    }
                }
            }
            cookieManager.flush()
        }
    }

    fun switchAccount(accountId: String, currentUrl: String): Account? {
        saveCurrentAccountCookies(currentUrl)

        val target = _accountsFlow.value.find { it.id == accountId } ?: return null
        prefs.edit().putString(KEY_ACTIVE_ID, target.id).apply()
        _activeAccountFlow.value = target

        applyAccountCookies(target, target.url)
        return target
    }

    fun createAccount(name: String, colorHex: String, url: String): Account {
        val newAcc = Account(
            id = "acc_" + System.currentTimeMillis(),
            name = name.ifBlank { "Account " + (_accountsFlow.value.size + 1) },
            colorHex = colorHex.ifBlank { "#6366f1" },
            url = url.ifBlank { "https://antigravity.google" }
        )

        val updated = _accountsFlow.value.toMutableList().apply { add(newAcc) }
        _accountsFlow.value = updated
        saveAccountsList(updated)

        switchAccount(newAcc.id, _activeAccountFlow.value?.url ?: newAcc.url)
        return newAcc
    }

    fun updateAccount(id: String, name: String, colorHex: String, url: String): Account? {
        val currentList = _accountsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = currentList[index].copy(
                name = name.ifBlank { currentList[index].name },
                colorHex = colorHex.ifBlank { currentList[index].colorHex },
                url = url.ifBlank { currentList[index].url }
            )
            currentList[index] = updated
            _accountsFlow.value = currentList
            if (_activeAccountFlow.value?.id == id) {
                _activeAccountFlow.value = updated
            }
            saveAccountsList(currentList)
            return updated
        }
        return null
    }

    fun deleteAccount(id: String): Boolean {
        val currentList = _accountsFlow.value.toMutableList()
        if (currentList.size <= 1) return false

        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            currentList.removeAt(index)
            _accountsFlow.value = currentList
            saveAccountsList(currentList)

            if (_activeAccountFlow.value?.id == id) {
                val nextActive = currentList.first()
                _activeAccountFlow.value = nextActive
                prefs.edit().putString(KEY_ACTIVE_ID, nextActive.id).apply()
                applyAccountCookies(nextActive, nextActive.url)
            }
            return true
        }
        return false
    }

    companion object {
        private const val KEY_ACCOUNTS = "key_accounts_json"
        private const val KEY_ACTIVE_ID = "key_active_account_id"
    }
}
