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
                id = "acc_primary",
                name = "Primary Account",
                colorHex = "#3b82f6",
                url = DEFAULT_REMOTE_URL
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

    fun switchAccount(accountId: String): Account? {
        val target = _accountsFlow.value.find { it.id == accountId } ?: return null
        prefs.edit().putString(KEY_ACTIVE_ID, target.id).apply()
        _activeAccountFlow.value = target
        CookieManager.getInstance().flush()
        return target
    }

    fun createAccount(name: String, colorHex: String, url: String): Account {
        val newAcc = Account(
            id = "acc_" + System.currentTimeMillis(),
            name = name.ifBlank { "Account " + (_accountsFlow.value.size + 1) },
            colorHex = colorHex.ifBlank { "#6366f1" },
            url = url.ifBlank { DEFAULT_REMOTE_URL }
        )

        val updated = _accountsFlow.value.toMutableList().apply { add(newAcc) }
        _accountsFlow.value = updated
        saveAccountsList(updated)

        switchAccount(newAcc.id)
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
            }
            return true
        }
        return false
    }

    companion object {
        const val DEFAULT_REMOTE_URL = "https://antigravity.google/remote"
        private const val KEY_ACCOUNTS = "key_accounts_json"
        private const val KEY_ACTIVE_ID = "key_active_account_id"
    }
}
