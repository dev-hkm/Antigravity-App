package com.hkm.antigravity.ui.components

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hkm.antigravity.browser.GeckoRuntimeHolder
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension

@Stable
class BrowserController(
    private val canGoBackProvider: () -> Boolean,
    private val goBackAction: () -> Unit,
    private val reloadAction: () -> Unit
) {
    fun canGoBack(): Boolean = canGoBackProvider()
    fun goBack() = goBackAction()
    fun reload() = reloadAction()
}

private class GeckoHolder {
    var view: GeckoView? = null
    var session: GeckoSession? = null
    var appliedDesktopMode: Boolean? = null
    var lastRequestedUrl: String? = null
}

private data class PopupWindowState(
    val session: GeckoSession,
    val title: String = "Sign in",
    val progress: Int = 0,
    val canGoBack: Boolean = false
)

private fun handleNonHttpUri(context: Context, session: GeckoSession?, rawUrl: String): Boolean {
    val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return true
    if (uri.scheme.equals("intent", ignoreCase = true)) {
        val intent = runCatching { Intent.parseUri(rawUrl, Intent.URI_INTENT_SCHEME) }.getOrNull()
        if (intent != null) {
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            val launched = runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (launched) return true

            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
            if (!fallbackUrl.isNullOrBlank()) {
                session?.loadUri(fallbackUrl)
            }
        }
        return true
    }

    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
    return true
}

private fun isSupportedNavigationScheme(uriString: String): Boolean {
    val scheme = runCatching { Uri.parse(uriString).scheme?.lowercase() }.getOrNull() ?: return true
    return scheme == "http" ||
        scheme == "https" ||
        scheme == "about" ||
        scheme == "blob" ||
        scheme == "data" ||
        scheme == "javascript"
}

private fun createSessionSettings(accountId: String, desktop: Boolean): GeckoSessionSettings {
    return GeckoSessionSettings.Builder()
        .contextId("account_$accountId")
        .userAgentMode(
            if (desktop) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        )
        .viewportMode(
            if (desktop) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        )
        .useTrackingProtection(false)
        .allowJavascript(true)
        .suspendMediaWhenInactive(false)
        .build()
}

private fun createPromptDelegate(
    context: Context,
    onLaunchFilePicker: (Intent, (Array<Uri>?) -> Unit) -> Boolean
): GeckoSession.PromptDelegate {
    return object : GeckoSession.PromptDelegate {
        override fun onFilePrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.FilePrompt
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                val accepted = prompt.mimeTypes?.filter { it.isNotBlank() } ?: emptyList()
                if (accepted.size == 1) {
                    type = accepted[0]
                } else {
                    type = "*/*"
                    if (accepted.size > 1) {
                        putExtra(Intent.EXTRA_MIME_TYPES, accepted.toTypedArray())
                    }
                }
                if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }
            val launched = onLaunchFilePicker(intent) { uris ->
                if (uris != null && uris.isNotEmpty()) {
                    runCatching {
                        val response =
                            if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
                                prompt.confirm(context, uris)
                            } else {
                                prompt.confirm(context, uris[0])
                            }
                        result.complete(response)
                    }.onFailure {
                        result.complete(prompt.dismiss())
                    }
                } else {
                    result.complete(prompt.dismiss())
                }
            }
            if (!launched) {
                result.complete(prompt.dismiss())
            }
            return result
        }

        override fun onPopupPrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.PopupPrompt
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
            return GeckoResult.fromValue(prompt.confirm(AllowOrDeny.ALLOW))
        }

        override fun onAlertPrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.AlertPrompt
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
            AlertDialog.Builder(context)
                .setTitle(prompt.title ?: "Alert")
                .setMessage(prompt.message.orEmpty())
                .setPositiveButton(android.R.string.ok) { _, _ -> result.complete(prompt.dismiss()) }
                .setOnCancelListener { result.complete(prompt.dismiss()) }
                .show()
            return result
        }

        override fun onButtonPrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.ButtonPrompt
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
            AlertDialog.Builder(context)
                .setTitle(prompt.title ?: "Confirm")
                .setMessage(prompt.message.orEmpty())
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    result.complete(prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE))
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    result.complete(prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE))
                }
                .setOnCancelListener { result.complete(prompt.dismiss()) }
                .show()
            return result
        }

        override fun onTextPrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.TextPrompt
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
            val input = EditText(context).apply {
                setText(prompt.defaultValue.orEmpty())
            }
            AlertDialog.Builder(context)
                .setTitle(prompt.title ?: "Input")
                .setMessage(prompt.message.orEmpty())
                .setView(input)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    result.complete(prompt.confirm(input.text?.toString().orEmpty()))
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    result.complete(prompt.dismiss())
                }
                .setOnCancelListener { result.complete(prompt.dismiss()) }
                .show()
            return result
        }

        override fun onChoicePrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.ChoicePrompt
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
            val flatChoices = prompt.choices.filter { !it.separator }
            if (flatChoices.isEmpty()) {
                return GeckoResult.fromValue(prompt.dismiss())
            }
            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
            val labels = flatChoices.map { it.label }.toTypedArray()

            when (prompt.type) {
                GeckoSession.PromptDelegate.ChoicePrompt.Type.MULTIPLE -> {
                    val checked = BooleanArray(flatChoices.size) { flatChoices[it].selected }
                    AlertDialog.Builder(context)
                        .setTitle(prompt.title ?: prompt.message ?: "Select")
                        .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                            checked[which] = isChecked
                        }
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val selectedIds = flatChoices
                                .filterIndexed { idx, _ -> checked[idx] }
                                .map { it.id }
                                .toTypedArray()
                            result.complete(prompt.confirm(selectedIds))
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            result.complete(prompt.dismiss())
                        }
                        .setOnCancelListener { result.complete(prompt.dismiss()) }
                        .show()
                }
                else -> {
                    val initialIdx = flatChoices.indexOfFirst { it.selected }
                    AlertDialog.Builder(context)
                        .setTitle(prompt.title ?: prompt.message)
                        .setSingleChoiceItems(labels, initialIdx) { dialog, which ->
                            dialog.dismiss()
                            result.complete(prompt.confirm(flatChoices[which].id))
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            result.complete(prompt.dismiss())
                        }
                        .setOnCancelListener { result.complete(prompt.dismiss()) }
                        .show()
                }
            }
            return result
        }
    }
}

private fun createPermissionDelegate(): GeckoSession.PermissionDelegate {
    return object : GeckoSession.PermissionDelegate {
        override fun onContentPermissionRequest(
            session: GeckoSession,
            perm: GeckoSession.PermissionDelegate.ContentPermission
        ): GeckoResult<Int> {
            val value = when (perm.permission) {
                GeckoSession.PermissionDelegate.PERMISSION_STORAGE_ACCESS,
                GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE,
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE,
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE ->
                    GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                else -> GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
            }
            return GeckoResult.fromValue(value)
        }

        override fun onAndroidPermissionsRequest(
            session: GeckoSession,
            permissions: Array<out String>?,
            callback: GeckoSession.PermissionDelegate.Callback
        ) {
            callback.reject()
        }
    }
}

@Composable
fun GeckoBrowserContainer(
    modifier: Modifier = Modifier,
    accountId: String,
    url: String,
    appThemeMode: String = "system",
    webTheme: String = "dark",
    isDesktopMode: Boolean = false,
    autoConnect: Boolean = true,
    onControllerReady: (BrowserController) -> Unit,
    onProgressChange: (Int) -> Unit,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onRendererCrashed: () -> Unit,
    onLaunchFilePicker: (Intent, (Array<Uri>?) -> Unit) -> Boolean
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isWebDark = when (webTheme) {
        "dark" -> true
        "light" -> false
        else -> when (appThemeMode) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }
    }

    val currentIsWebDark by rememberUpdatedState(isWebDark)
    val currentIsDesktopMode by rememberUpdatedState(isDesktopMode)
    val currentAutoConnect by rememberUpdatedState(autoConnect)
    val currentOnProgressChange by rememberUpdatedState(onProgressChange)
    val currentOnTitleChange by rememberUpdatedState(onTitleChange)
    val currentOnUrlChange by rememberUpdatedState(onUrlChange)
    val currentOnRendererCrashed by rememberUpdatedState(onRendererCrashed)
    val currentOnLaunchFilePicker by rememberUpdatedState(onLaunchFilePicker)

    val activePorts = remember { mutableListOf<WebExtension.Port>() }
    val holder = remember { GeckoHolder() }
    var canGoBackState by remember { mutableStateOf(false) }
    var popupState by remember { mutableStateOf<PopupWindowState?>(null) }

    fun buildConfigJson(): JSONObject = JSONObject().apply {
        put("isWebDark", currentIsWebDark)
        put("isDesktopMode", currentIsDesktopMode)
        put("autoConnect", currentAutoConnect)
    }

    fun pushConfigToPorts() {
        val msg = buildConfigJson()
        activePorts.forEach { port ->
            runCatching { port.postMessage(msg) }
        }
    }

    fun attachExtensionBridge(session: GeckoSession) {
        GeckoRuntimeHolder.withExtension { ext ->
            val msgDelegate = object : WebExtension.MessageDelegate {
                override fun onMessage(
                    nativeApp: String,
                    message: Any,
                    sender: WebExtension.MessageSender
                ): GeckoResult<Any>? {
                    return GeckoResult.fromValue(buildConfigJson())
                }

                override fun onConnect(port: WebExtension.Port) {
                    activePorts.add(port)
                    port.setDelegate(object : WebExtension.PortDelegate {
                        override fun onPortMessage(message: Any, port: WebExtension.Port) {
                            runCatching { port.postMessage(buildConfigJson()) }
                        }

                        override fun onDisconnect(port: WebExtension.Port) {
                            activePorts.remove(port)
                        }
                    })
                    runCatching { port.postMessage(buildConfigJson()) }
                }
            }
            session.webExtensionController.setMessageDelegate(ext, msgDelegate, "antigravity")
        }
    }

    val coverColor = remember(isWebDark) {
        if (isWebDark) AndroidColor.parseColor("#0C0E14") else AndroidColor.WHITE
    }

    LaunchedEffect(isWebDark) {
        GeckoRuntimeHolder.updatePreferredColorScheme(isWebDark)
        pushConfigToPorts()
    }

    LaunchedEffect(autoConnect) {
        pushConfigToPorts()
    }

    LaunchedEffect(isDesktopMode) {
        val session = holder.session ?: return@LaunchedEffect
        if (holder.appliedDesktopMode == isDesktopMode) return@LaunchedEffect
        holder.appliedDesktopMode = isDesktopMode
        session.settings.userAgentMode = if (isDesktopMode) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        session.settings.viewportMode = if (isDesktopMode) {
            GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        }
        pushConfigToPorts()
        session.reload()
    }

    LaunchedEffect(url) {
        val session = holder.session ?: return@LaunchedEffect
        if (url != holder.lastRequestedUrl) {
            holder.lastRequestedUrl = url
            session.loadUri(url)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            val runtime = GeckoRuntimeHolder.getOrCreate(viewContext, currentIsWebDark)
            val session = GeckoSession(createSessionSettings(accountId, currentIsDesktopMode))
            holder.session = session
            holder.appliedDesktopMode = currentIsDesktopMode

            val gv = GeckoView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                coverUntilFirstPaint(coverColor)
            }
            holder.view = gv

            val promptDelegate = createPromptDelegate(viewContext) { intent, cb ->
                currentOnLaunchFilePicker(intent, cb)
            }
            val permissionDelegate = createPermissionDelegate()

            session.promptDelegate = promptDelegate
            session.permissionDelegate = permissionDelegate

            session.progressDelegate = object : GeckoSession.ProgressDelegate {
                override fun onPageStart(session: GeckoSession, url: String) {
                    currentOnUrlChange(url)
                    currentOnProgressChange(15)
                }

                override fun onProgressChange(session: GeckoSession, progress: Int) {
                    currentOnProgressChange(progress)
                }

                override fun onPageStop(session: GeckoSession, success: Boolean) {
                    currentOnProgressChange(100)
                    pushConfigToPorts()
                }
            }

            session.contentDelegate = object : GeckoSession.ContentDelegate {
                override fun onTitleChange(session: GeckoSession, title: String?) {
                    if (!title.isNullOrEmpty()) currentOnTitleChange(title)
                }

                override fun onCrash(session: GeckoSession) {
                    Toast.makeText(
                        viewContext,
                        "Browser engine restarted after a crash.",
                        Toast.LENGTH_LONG
                    ).show()
                    currentOnRendererCrashed()
                }

                override fun onKill(session: GeckoSession) {
                    currentOnRendererCrashed()
                }
            }

            session.navigationDelegate = object : GeckoSession.NavigationDelegate {
                override fun onLocationChange(
                    session: GeckoSession,
                    url: String?,
                    perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                    hasUserGesture: Boolean
                ) {
                    if (!url.isNullOrEmpty()) {
                        currentOnUrlChange(url)
                    }
                }

                override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                    canGoBackState = canGoBack
                }

                override fun onLoadRequest(
                    session: GeckoSession,
                    request: GeckoSession.NavigationDelegate.LoadRequest
                ): GeckoResult<AllowOrDeny> {
                    if (!isSupportedNavigationScheme(request.uri)) {
                        handleNonHttpUri(viewContext, session, request.uri)
                        return GeckoResult.fromValue(AllowOrDeny.DENY)
                    }
                    return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                }

                override fun onLoadError(
                    session: GeckoSession,
                    uri: String?,
                    error: org.mozilla.geckoview.WebRequestError
                ): GeckoResult<String>? {
                    Toast.makeText(
                        viewContext,
                        "Unable to load page. Try Reload.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return null
                }

                override fun onNewSession(
                    session: GeckoSession,
                    uri: String
                ): GeckoResult<GeckoSession> {
                    if (!isSupportedNavigationScheme(uri)) {
                        handleNonHttpUri(viewContext, session, uri)
                        return GeckoResult.fromValue(null)
                    }

                    val child = GeckoSession(createSessionSettings(accountId, currentIsDesktopMode))
                    child.promptDelegate = promptDelegate
                    child.permissionDelegate = permissionDelegate

                    child.progressDelegate = object : GeckoSession.ProgressDelegate {
                        override fun onProgressChange(s: GeckoSession, progress: Int) {
                            popupState = popupState?.takeIf { it.session === child }?.copy(progress = progress)
                        }

                        override fun onPageStop(s: GeckoSession, success: Boolean) {
                            popupState = popupState?.takeIf { it.session === child }?.copy(progress = 100)
                        }
                    }

                    child.contentDelegate = object : GeckoSession.ContentDelegate {
                        override fun onTitleChange(s: GeckoSession, title: String?) {
                            if (!title.isNullOrBlank()) {
                                popupState = popupState?.takeIf { it.session === child }?.copy(title = title)
                            }
                        }

                        override fun onCloseRequest(s: GeckoSession) {
                            if (popupState?.session === child) {
                                popupState = null
                            }
                            child.close()
                        }
                    }

                    child.navigationDelegate = object : GeckoSession.NavigationDelegate {
                        override fun onCanGoBack(s: GeckoSession, canGoBack: Boolean) {
                            popupState = popupState?.takeIf { it.session === child }?.copy(canGoBack = canGoBack)
                        }

                        override fun onLoadRequest(
                            s: GeckoSession,
                            request: GeckoSession.NavigationDelegate.LoadRequest
                        ): GeckoResult<AllowOrDeny> {
                            if (!isSupportedNavigationScheme(request.uri)) {
                                handleNonHttpUri(viewContext, s, request.uri)
                                return GeckoResult.fromValue(AllowOrDeny.DENY)
                            }
                            return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                        }
                    }

                    attachExtensionBridge(child)
                    popupState = PopupWindowState(session = child, title = uri)
                    return GeckoResult.fromValue(child)
                }
            }

            attachExtensionBridge(session)
            session.open(runtime)
            gv.setSession(session)

            onControllerReady(
                BrowserController(
                    canGoBackProvider = { canGoBackState },
                    goBackAction = { session.goBack() },
                    reloadAction = { session.reload() }
                )
            )

            holder.lastRequestedUrl = url
            session.loadUri(url)
            gv
        },
        update = { /* Managed via state & LaunchedEffect */ }
    )

    popupState?.let { activePopup ->
        PopupBrowserDialog(
            state = activePopup,
            isWebDark = isWebDark,
            onDismiss = {
                popupState = null
                runCatching { activePopup.session.close() }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            activePorts.toList().forEach { port ->
                runCatching { port.disconnect() }
            }
            activePorts.clear()
            popupState?.let {
                runCatching { it.session.close() }
                popupState = null
            }
            holder.view?.let { gv ->
                runCatching { gv.releaseSession() }
            }
            holder.session?.let { s ->
                runCatching { s.close() }
            }
            holder.session = null
            holder.view = null
        }
    }
}

@Composable
private fun PopupBrowserDialog(
    state: PopupWindowState,
    isWebDark: Boolean,
    onDismiss: () -> Unit
) {
    val coverColor = if (isWebDark) AndroidColor.parseColor("#0C0E14") else AndroidColor.WHITE
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.canGoBack) {
                            IconButton(onClick = { state.session.goBack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                if (state.progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    var popupGvRef by remember { mutableStateOf<GeckoView?>(null) }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            GeckoView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                coverUntilFirstPaint(coverColor)
                                setSession(state.session)
                                popupGvRef = this
                            }
                        }
                    )
                    DisposableEffect(state.session) {
                        onDispose {
                            runCatching { popupGvRef?.releaseSession() }
                        }
                    }
                }
            }
        }
    }
}
