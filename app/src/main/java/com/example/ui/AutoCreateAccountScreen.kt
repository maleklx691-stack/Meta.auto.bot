package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.SavedProfileEntity
import com.example.domain.IdentityGeneratorEngine
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONObject

private const val CHROME_MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.127 Mobile Safari/537.36"

@Composable
fun AutoCreateAccountScreen(
    uiState: GeneratorUiState,
    savedProfiles: List<SavedProfileEntity>,
    onStartOneClickAutomation: () -> Unit,
    onStopAutomation: () -> Unit,
    onToggleAutoLoop: () -> Unit,
    onTopTempMailScraped: (email: String, otp: String) -> Unit,
    onMetaDomStageDetected: (stage: String, url: String) -> Unit,
    onManualOtpSubmit: (String) -> Unit,
    onOtpCodeChange: (String) -> Unit,
    onPasteOtpClick: () -> Unit,
    onRefreshInboxClick: () -> Unit,
    onToggleUseCustomPassword: () -> Unit,
    onCustomFixedPasswordChange: (String) -> Unit,
    onUpdateSavedPassword: (Long, String) -> Unit,
    onTriggerManualAutoFill: () -> Unit,
    onCopyText: (String, String) -> Unit,
    onNavigateToVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEn = uiState.useEnglishUiLabels
    val profile = uiState.currentProfile

    // Collapsed by default so the Meta AI Browser is immediately visible right below START!
    var showTopTempMailWebView by rememberSaveable { mutableStateOf(false) }
    var topTempMailUrl by rememberSaveable { mutableStateOf("https://temp-mail.io") }
    var topTempMailWebViewRef by remember { mutableStateOf<WebView?>(null) }
    var isTopMailLoading by remember { mutableStateOf(false) }

    var showBottomMetaAiWebView by rememberSaveable { mutableStateOf(true) }
    var metaAiWebUrl by rememberSaveable { mutableStateOf("https://www.meta.ai/") }
    var metaAiWebViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMetaAiWebLoading by remember { mutableStateOf(false) }

    var showPasswordSetter by rememberSaveable { mutableStateOf(false) }
    var editingPasswordProfile by remember { mutableStateOf<SavedProfileEntity?>(null) }

    val runAutoFillInMetaAiWebView: (Boolean) -> Unit = { submitOtp ->
        val wv = metaAiWebViewRef
        val activeEmail = uiState.tempEmailInput.ifBlank {
            uiState.lastCompletedEmail.ifBlank { profile.tempEmail }
        }
        if (wv != null && activeEmail.isNotBlank()) {
            val js = IdentityGeneratorEngine.buildMetaWebViewAutoFillJs(
                profile = profile,
                email = activeEmail,
                otpCode = uiState.otpCodeInput.ifBlank { uiState.lastCompletedOtp },
                preferBengaliName = uiState.preferBengaliInAutoFill,
                triggerSubmitOtp = submitOtp,
                autoClickNext = true
            )
            wv.evaluateJavascript(js) { rawResult ->
                if (!rawResult.isNullOrBlank() && rawResult != "null") {
                    runCatching {
                        val unescaped = rawResult
                            .removeSurrounding("\"")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                        val obj = JSONObject(unescaped)
                        val stage = obj.optString("stage")
                        val url = obj.optString("url")
                        if (stage.isNotBlank()) {
                            onMetaDomStageDetected(stage, url)
                        }
                    }
                }
            }
        }
    }

    // When START is clicked, load https://auth.meta.com/ so the signup/login form fills live
    LaunchedEffect(uiState.shouldLoadMetaAuthPortalCount) {
        if (uiState.shouldLoadMetaAuthPortalCount > 0) {
            metaAiWebUrl = "https://auth.meta.com/"
            metaAiWebViewRef?.loadUrl("https://auth.meta.com/")
            delay(600L)
            runAutoFillInMetaAiWebView(false)
        }
    }

    // When Step 6 completes, inject the verified OTP and open https://www.meta.ai/ logged in!
    LaunchedEffect(uiState.shouldRedirectToMetaAiChatCount) {
        if (uiState.shouldRedirectToMetaAiChatCount > 0) {
            runAutoFillInMetaAiWebView(true)
            delay(900L)
            metaAiWebUrl = "https://www.meta.ai/"
            metaAiWebViewRef?.loadUrl("https://www.meta.ai/")
            delay(1000L)
            runAutoFillInMetaAiWebView(true)
        }
    }

    // Continuous DOM & Top Temp-Mail synchronization loop
    LaunchedEffect(
        uiState.isAutoRunning,
        uiState.tempEmailInput,
        uiState.otpCodeInput,
        uiState.currentProfile,
        showTopTempMailWebView
    ) {
        while (isActive) {
            if (showTopTempMailWebView) {
                topTempMailWebViewRef?.evaluateJavascript(
                    IdentityGeneratorEngine.buildTopTempMailScraperJs()
                ) { rawJson ->
                    if (!rawJson.isNullOrBlank() && rawJson != "null") {
                        runCatching {
                            val unescaped = rawJson
                                .removeSurrounding("\"")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                            val obj = JSONObject(unescaped)
                            val scrapedEmail = obj.optString("email")
                            val scrapedOtp = obj.optString("otp")
                            if (scrapedEmail.isNotBlank() || scrapedOtp.isNotBlank()) {
                                onTopTempMailScraped(scrapedEmail, scrapedOtp)
                            }
                        }
                    }
                }
            }
            if (uiState.isAutoRunning || uiState.tempEmailInput.isNotBlank() || uiState.isMetaAiLoggedIn) {
                runAutoFillInMetaAiWebView(uiState.otpCodeInput.isNotBlank())
            }
            delay(1300L)
        }
    }

    LaunchedEffect(uiState.autoInjectionTriggerCount) {
        if (uiState.autoInjectionTriggerCount > 0) {
            runAutoFillInMetaAiWebView(uiState.shouldTriggerOtpSubmitInWeb)
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Using Column + verticalScroll so the Meta AI WebView is ALWAYS mounted and never disposed!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("auto_create_scroll_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. GIANT 1-CLICK START CONTROL CARD
            OneClickStartMasterCard(
                isEn = isEn,
                uiState = uiState,
                showPasswordSetter = showPasswordSetter,
                onTogglePasswordSetter = { showPasswordSetter = !showPasswordSetter },
                onStartOneClickAutomation = onStartOneClickAutomation,
                onStopAutomation = onStopAutomation,
                onToggleAutoLoop = onToggleAutoLoop,
                onToggleUseCustomPassword = onToggleUseCustomPassword,
                onCustomFixedPasswordChange = onCustomFixedPasswordChange
            )

            // 2. ACTIVE LOGGED-IN META AI ACCOUNT BANNER (Shows immediately when an account is created & logged in!)
            if (uiState.isMetaAiLoggedIn || uiState.lastCompletedEmail.isNotBlank() || savedProfiles.isNotEmpty()) {
                val latestSaved = savedProfiles.firstOrNull()
                ActiveMetaAiLoggedInBanner(
                    isEn = isEn,
                    nameBn = uiState.lastCompletedNameBn.ifBlank { latestSaved?.fullNameBn ?: profile.fullNameBn },
                    nameEn = uiState.lastCompletedNameEn.ifBlank { latestSaved?.fullNameEn ?: profile.fullNameEn },
                    email = uiState.lastCompletedEmail.ifBlank { latestSaved?.tempEmail ?: uiState.tempEmailInput },
                    password = uiState.lastCompletedPassword.ifBlank { latestSaved?.password ?: profile.password },
                    dob = uiState.lastCompletedDob.ifBlank { latestSaved?.dobNumeric ?: profile.dob.numericSlash },
                    otp = uiState.lastCompletedOtp.ifBlank { latestSaved?.verificationCode ?: uiState.otpCodeInput },
                    onCopyText = onCopyText,
                    onOpenMetaAiChat = {
                        metaAiWebUrl = "https://www.meta.ai/"
                        metaAiWebViewRef?.loadUrl("https://www.meta.ai/")
                    }
                )
            }

            // 3. META AI SIGN-UP & LOGIN BROWSER (Immediately visible right below START!)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("embedded_meta_webview_card"),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (isEn) {
                                        "Meta AI Auto-Account & Login Browser"
                                    } else {
                                        "Meta AI অটো একাউন্ট ও লগইন ব্রাউজার (ফেসবুক মুক্ত)"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isEn) {
                                        "Auto-fills Email, 18–35 DOB, Name, Password & logs into meta.ai"
                                    } else {
                                        "অটো মেইল, জন্ম তারিখ, নাম, পাসওয়ার্ড ও কোড বসিয়ে Meta AI লগইন করে"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    onTriggerManualAutoFill()
                                    runAutoFillInMetaAiWebView(true)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .testTag("webview_reinject_autofill_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEn) "Fill & Login" else "অটো-ফিল ও লগইন")
                            }

                            IconButton(
                                onClick = { showBottomMetaAiWebView = !showBottomMetaAiWebView }
                            ) {
                                Icon(
                                    imageVector = if (showBottomMetaAiWebView) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Meta AI Browser"
                                )
                            }
                        }
                    }

                    // Direct Switcher between Meta AI Chat (meta.ai) and Meta Auth Portal (auth.meta.com)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = metaAiWebUrl.contains("www.meta.ai"),
                            onClick = {
                                metaAiWebUrl = "https://www.meta.ai/"
                                metaAiWebViewRef?.loadUrl(metaAiWebUrl)
                            },
                            label = {
                                Text(if (isEn) "1. Meta AI Chat (meta.ai)" else "১. Meta AI মেইন চ্যাট (meta.ai)")
                            }
                        )
                        FilterChip(
                            selected = metaAiWebUrl.contains("auth.meta.com"),
                            onClick = {
                                metaAiWebUrl = "https://auth.meta.com/"
                                metaAiWebViewRef?.loadUrl(metaAiWebUrl)
                            },
                            label = {
                                Text(if (isEn) "2. Meta AI Auth Form (auth.meta.com)" else "২. Meta AI সাইন-আপ ফর্ম (auth.meta.com)")
                            }
                        )
                        IconButton(
                            onClick = { metaAiWebViewRef?.reload() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload Meta AI",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isMetaAiWebLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    AnimatedVisibility(visible = showBottomMetaAiWebView) {
                        EmbeddedBrowserWebView(
                            url = metaAiWebUrl,
                            testTagName = "meta_registration_webview",
                            blockFacebookRedirects = true,
                            onWebViewCreated = { metaAiWebViewRef = it },
                            onPageLoadingChange = { isMetaAiWebLoading = it },
                            onPageFinished = {
                                runAutoFillInMetaAiWebView(uiState.otpCodeInput.isNotBlank())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                        )
                    }
                }
            }

            // 4. INLINE GOOGLE WORKSPACE PUBLIC EDITING TABLE & CREATED ACCOUNTS
            // Always visible so the user immediately sees created accounts and can set/edit passwords!
            InlineGoogleSheetAccountsCard(
                isEn = isEn,
                currentProfile = profile,
                currentEmail = uiState.tempEmailInput,
                currentOtp = uiState.otpCodeInput,
                profiles = savedProfiles.take(8),
                totalCount = savedProfiles.size,
                onEditPassword = { editingPasswordProfile = it },
                onOpenPasswordSetterForCurrent = { showPasswordSetter = true },
                onCopyAllForSheet = {
                    val sheetRows = buildString {
                        appendLine("Name\tTemp Email\tPassword\tDOB\tMail Key (OTP)\tStatus")
                        if (savedProfiles.isEmpty()) {
                            val mail = uiState.tempEmailInput.ifBlank { "auto@mail.tm" }
                            appendLine("${profile.fullNameEn}\t$mail\t${profile.password}\t${profile.dob.numericSlash}\t${uiState.otpCodeInput.ifBlank { "AUTO" }}\tMeta AI Ready")
                        } else {
                            savedProfiles.forEach { p ->
                                appendLine("${p.fullNameEn}\t${p.tempEmail}\t${p.password}\t${p.dobNumeric}\t${p.verificationCode}\tMeta AI Logged In")
                            }
                        }
                    }.trim()
                    onCopyText("Google Sheet Table", sheetRows)
                },
                onOpenFullSheetScreen = onNavigateToVault
            )

            // 5. TOP TEMP-MAIL WEBSITE & REAL INBOX OTP BAR (উপরে টেম্প-মেইল ওয়েবসাইট ও অটো মেইল-কি)
            TopTempMailWebsiteCard(
                isEn = isEn,
                uiState = uiState,
                showTopWebView = showTopTempMailWebView,
                onToggleTopWebView = { showTopTempMailWebView = !showTopTempMailWebView },
                topTempMailUrl = topTempMailUrl,
                onSelectTopTempMailUrl = { url ->
                    topTempMailUrl = url
                    topTempMailWebViewRef?.loadUrl(url)
                },
                isTopMailLoading = isTopMailLoading,
                onTopWebViewCreated = { topTempMailWebViewRef = it },
                onTopMailLoadingChange = { isTopMailLoading = it },
                onOtpCodeChange = onOtpCodeChange,
                onPasteOtpClick = onPasteOtpClick,
                onRefreshInboxClick = onRefreshInboxClick,
                onManualOtpSubmit = {
                    onManualOtpSubmit(uiState.otpCodeInput)
                    runAutoFillInMetaAiWebView(true)
                },
                onCopyText = onCopyText
            )

            // 6. LIVE 6-STEP VISUAL AUTOMATION TRACKER
            LiveSixStepPipelineCard(
                isEn = isEn,
                uiState = uiState,
                onCopyText = onCopyText
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Password Edit Dialog ("পাসওয়ার্ড নির্ধারণ করার জন্য")
    editingPasswordProfile?.let { target ->
        var draftPassword by rememberSaveable(target.id) { mutableStateOf(target.password) }
        AlertDialog(
            onDismissRequest = { editingPasswordProfile = null },
            title = {
                Text(
                    if (isEn) "Set / Change Meta AI Password"
                    else "Meta AI একাউন্টের পাসওয়ার্ড নির্ধারণ করুন"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${target.fullNameBn} (${target.tempEmail})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = draftPassword,
                        onValueChange = { draftPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text(if (isEn) "New Password" else "নতুন পাসওয়ার্ড লিখুন")
                        }
                    )
                    FilledTonalButton(
                        onClick = {
                            draftPassword = IdentityGeneratorEngine.generateStrongPassword(14)
                        }
                    ) {
                        Text(if (isEn) "Auto Strong Password" else "অটো স্ট্রং পাসওয়ার্ড নিন")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateSavedPassword(target.id, draftPassword)
                        editingPasswordProfile = null
                    }
                ) {
                    Text(if (isEn) "Save Password" else "পাসওয়ার্ড সেভ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPasswordProfile = null }) {
                    Text(if (isEn) "Cancel" else "বাতিল")
                }
            }
        )
    }
}

@Composable
private fun ActiveMetaAiLoggedInBanner(
    isEn: Boolean,
    nameBn: String,
    nameEn: String,
    email: String,
    password: String,
    dob: String,
    otp: String,
    onCopyText: (String, String) -> Unit,
    onOpenMetaAiChat: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_meta_ai_logged_in_banner"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (isEn) {
                                "✓ Meta AI Account Created & Logged In!"
                            } else {
                                "✓ Meta AI একাউন্ট তৈরি ও লগইন সম্পন্ন হয়েছে!"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "$nameBn ($nameEn) • DOB: $dob",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Button(
                    onClick = onOpenMetaAiChat,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 38.dp)
                ) {
                    Text(if (isEn) "Open Meta AI" else "Meta AI চ্যাট")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniInfoPill(
                    label = if (isEn) "Logged-In Email" else "লগইন ইমেইল",
                    value = email,
                    onClick = { onCopyText("Email", email) },
                    modifier = Modifier.weight(1.2f)
                )
                MiniInfoPill(
                    label = if (isEn) "Password" else "পাসওয়ার্ড",
                    value = password,
                    onClick = { onCopyText("Password", password) },
                    modifier = Modifier.weight(1f)
                )
                MiniInfoPill(
                    label = if (isEn) "OTP Key" else "মেইল-কি",
                    value = otp.ifBlank { "VERIFIED" },
                    onClick = { onCopyText("OTP", otp) },
                    modifier = Modifier.weight(0.7f)
                )
            }
        }
    }
}

@Composable
private fun OneClickStartMasterCard(
    isEn: Boolean,
    uiState: GeneratorUiState,
    showPasswordSetter: Boolean,
    onTogglePasswordSetter: () -> Unit,
    onStartOneClickAutomation: () -> Unit,
    onStopAutomation: () -> Unit,
    onToggleAutoLoop: () -> Unit,
    onToggleUseCustomPassword: () -> Unit,
    onCustomFixedPasswordChange: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("one_click_start_master_card"),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEn) {
                            "1-Click Auto Meta AI Account & Login"
                        } else {
                            "১-ক্লিকে অটো Meta AI একাউন্ট ও লগইন"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isEn) {
                            "Tap START — Auto-creates Temp-Mail, fills DOB/Name/Password & logs into Meta AI"
                        } else {
                            "স্টার্ট (START) চাপলেই অটো মেইল, জন্ম তারিখ, নাম ও পাসওয়ার্ড দিয়ে Meta AI লগইন হবে"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.totalAutoCreatedInSession > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (isEn) {
                                "Logged In: ${uiState.totalAutoCreatedInSession}"
                            } else {
                                "তৈরি হয়েছে: ${IdentityGeneratorEngine.toBengaliDigits(uiState.totalAutoCreatedInSession)}টি"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (!uiState.isAutoRunning) {
                Button(
                    onClick = onStartOneClickAutomation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .testTag("one_click_start_button"),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0866FF)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEn) {
                            "START — AUTO CREATE & LOGIN META AI"
                        } else {
                            "স্টার্ট (START) — অটো Meta AI একাউন্ট ও লগইন"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onStopAutomation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .testTag("one_click_stop_button"),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) {
                            "CREATING & LOGGING IN... (TAP TO STOP)"
                        } else {
                            "অটো Meta AI একাউন্ট তৈরি হচ্ছে... (থামাতে ক্লিক করুন)"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LinearProgressIndicator(
                progress = { uiState.autoProgressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEn) uiState.mailboxStatusEn else uiState.mailboxStatusBn,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = uiState.autoLoopEnabled,
                        onCheckedChange = { onToggleAutoLoop() },
                        modifier = Modifier.testTag("auto_loop_switch")
                    )
                    Text(
                        text = if (isEn) "Continuous Auto-Loop" else "একটার পর একটা অটো একাউন্ট",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(
                    onClick = onTogglePasswordSetter,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEn) "Set Password" else "পাসওয়ার্ড নির্ধারণ")
                }
            }

            AnimatedVisibility(visible = showPasswordSetter) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    FilterChip(
                        selected = uiState.useCustomFixedPassword,
                        onClick = onToggleUseCustomPassword,
                        label = {
                            Text(
                                if (isEn) "Use My Predetermined Password for All Accounts"
                                else "সকল Meta AI একাউন্টের জন্য আমার নির্ধারিত পাসওয়ার্ড দিন"
                            )
                        }
                    )
                    if (uiState.useCustomFixedPassword) {
                        OutlinedTextField(
                            value = uiState.customFixedPassword,
                            onValueChange = onCustomFixedPasswordChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_fixed_password_input"),
                            singleLine = true,
                            label = {
                                Text(
                                    if (isEn) "Predetermined Password"
                                    else "নির্ধারিত পাসওয়ার্ড লিখুন"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopTempMailWebsiteCard(
    isEn: Boolean,
    uiState: GeneratorUiState,
    showTopWebView: Boolean,
    onToggleTopWebView: () -> Unit,
    topTempMailUrl: String,
    onSelectTopTempMailUrl: (String) -> Unit,
    isTopMailLoading: Boolean,
    onTopWebViewCreated: (WebView) -> Unit,
    onTopMailLoadingChange: (Boolean) -> Unit,
    onOtpCodeChange: (String) -> Unit,
    onPasteOtpClick: () -> Unit,
    onRefreshInboxClick: () -> Unit,
    onManualOtpSubmit: () -> Unit,
    onCopyText: (String, String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_temp_mail_panel"),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column {
                        Text(
                            text = if (isEn) {
                                "Temp-Mail Inbox & Auto Mail-Key (OTP)"
                            } else {
                                "টেম্প-মেইল ইনবক্স ও অটো মেইল-কি (OTP)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.tempEmailInput.ifBlank {
                                if (isEn) "Tap START to generate live email" else "স্টার্ট চাপলে অটো ইমেইল তৈরি হবে"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                if (uiState.tempEmailInput.isNotBlank()) {
                                    onCopyText("Email", uiState.tempEmailInput)
                                }
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onRefreshInboxClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Text(if (isEn) "Check OTP" else "কোড চেক")
                    }
                    IconButton(onClick = onToggleTopWebView) {
                        Icon(
                            imageVector = if (showTopWebView) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Top Temp-Mail Site"
                        )
                    }
                }
            }

            // Live OTP Box + 1-Tap Verify & Save Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.otpCodeInput,
                    onValueChange = onOtpCodeChange,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("auto_create_otp_input"),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    label = {
                        Text(if (isEn) "Meta AI OTP Code" else "মেইল-কি / কোড (OTP)")
                    },
                    trailingIcon = {
                        IconButton(onClick = onPasteOtpClick) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste OTP",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                Button(
                    onClick = onManualOtpSubmit,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("submit_otp_complete_button"),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEn) "Verify & Login" else "কোড বসিয়ে লগইন",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.inboxMessages.isNotEmpty()) {
                val latest = uiState.inboxMessages.first()
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "📩 ${latest.subject} (${latest.fromAddress})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (latest.extractedOtp.isNotBlank()) {
                            Text(
                                text = "✓ Auto OTP Verified: ${latest.extractedOtp}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showTopWebView) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = topTempMailUrl.contains("temp-mail.io"),
                            onClick = { onSelectTopTempMailUrl("https://temp-mail.io") },
                            label = { Text("Temp-Mail.io") }
                        )
                        FilterChip(
                            selected = topTempMailUrl.contains("10minutemail"),
                            onClick = { onSelectTopTempMailUrl("https://10minutemail.net") },
                            label = { Text("10MinuteMail") }
                        )
                        FilterChip(
                            selected = topTempMailUrl.contains("guerrillamail"),
                            onClick = { onSelectTopTempMailUrl("https://www.guerrillamail.com") },
                            label = { Text("GuerrillaMail") }
                        )
                    }

                    if (isTopMailLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    EmbeddedBrowserWebView(
                        url = topTempMailUrl,
                        testTagName = "top_temp_mail_webview",
                        blockFacebookRedirects = false,
                        onWebViewCreated = onTopWebViewCreated,
                        onPageLoadingChange = onTopMailLoadingChange,
                        onPageFinished = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                MaterialTheme.shapes.medium
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveSixStepPipelineCard(
    isEn: Boolean,
    uiState: GeneratorUiState,
    onCopyText: (String, String) -> Unit
) {
    val profile = uiState.currentProfile

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_six_step_pipeline_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isEn) {
                    "Auto-Filled Identity & Live 6-Step Progress"
                } else {
                    "অটো বসানো তথ্য ও ৬-ধাপের লাইভ অগ্রগতি"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniInfoPill(
                    label = if (isEn) "Auto Name" else "অটো নাম",
                    value = "${profile.fullNameBn} (${profile.fullNameEn})",
                    onClick = { onCopyText("Name", profile.fullNameEn) },
                    modifier = Modifier.weight(1.1f)
                )
                MiniInfoPill(
                    label = if (isEn) "Auto DOB (18–35)" else "অটো জন্ম তারিখ",
                    value = "${profile.dob.numericSlash} (${profile.dob.ageYears}y)",
                    onClick = { onCopyText("DOB", profile.dob.numericSlash) },
                    modifier = Modifier.weight(0.9f)
                )
                MiniInfoPill(
                    label = if (isEn) "Password" else "পাসওয়ার্ড",
                    value = profile.password,
                    onClick = { onCopyText("Password", profile.password) },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            uiState.autoSteps.forEach { step ->
                Surface(
                    color = when {
                        step.isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        step.isDone -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (step.isCurrent && uiState.isAutoRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (step.isDone) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (step.isDone) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEn) step.titleEn else step.titleBn,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (step.isCurrent || step.isDone) FontWeight.Bold else FontWeight.Normal
                            )
                            if (step.detailText.isNotBlank()) {
                                Text(
                                    text = step.detailText,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineGoogleSheetAccountsCard(
    isEn: Boolean,
    currentProfile: com.example.domain.GeneratedIdentityProfile,
    currentEmail: String,
    currentOtp: String,
    profiles: List<SavedProfileEntity>,
    totalCount: Int,
    onEditPassword: (SavedProfileEntity) -> Unit,
    onOpenPasswordSetterForCurrent: () -> Unit,
    onCopyAllForSheet: () -> Unit,
    onOpenFullSheetScreen: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inline_google_sheet_accounts_card"),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Column {
                        Text(
                            text = if (isEn) {
                                "Google Workspace Public Editing Table ($totalCount)"
                            } else {
                                "গুগল ওয়ার্কস্পেস পাবলিক এডিটিং টেবিল ও পাসওয়ার্ড শিট (${IdentityGeneratorEngine.toBengaliDigits(totalCount)}টি)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEn) {
                                "Accounts auto-save here when START completes — tap any password to edit"
                            } else {
                                "START চাপলে একাউন্ট খুলে অটোমেটিক এই টেবিলে যুক্ত হয় — পাসওয়ার্ডে ট্যাপ করে পরিবর্তন করুন"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 7.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Meta AI নাম", fontWeight = FontWeight.Bold, modifier = Modifier.width(125.dp))
                    Text("অটো মেইল (Temp Mail)", fontWeight = FontWeight.Bold, modifier = Modifier.width(175.dp))
                    Text("পাসওয়ার্ড (এডিট করুন)", fontWeight = FontWeight.Bold, modifier = Modifier.width(145.dp))
                    Text("জন্ম তারিখ", fontWeight = FontWeight.Bold, modifier = Modifier.width(95.dp))
                    Text("মেইল-কি (OTP)", fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                    Text("স্ট্যাটাস", fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp))
                }

                if (profiles.isEmpty()) {
                    // Show the active profile row ready to be created
                    Row(
                        modifier = Modifier
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            .padding(vertical = 7.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentProfile.fullNameBn,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(125.dp)
                        )
                        Text(
                            text = currentEmail.ifBlank { "START চাপলে তৈরি হবে" },
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(175.dp)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .width(145.dp)
                                .clickable { onOpenPasswordSetterForCurrent() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = currentProfile.password,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Edit Password",
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text(
                            text = currentProfile.dob.numericSlash,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(95.dp)
                        )
                        Text(
                            text = currentOtp.ifBlank { "অটো কোড" },
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(90.dp)
                        )
                        Text(
                            text = if (isEn) "Tap START" else "প্রস্তুত (START চাপুন)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                } else {
                    profiles.forEach { item ->
                        Row(
                            modifier = Modifier
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                .padding(vertical = 7.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.fullNameBn,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(125.dp)
                            )
                            Text(
                                text = item.tempEmail,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(175.dp)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier
                                    .width(145.dp)
                                    .clickable { onEditPassword(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.password,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "Edit Password",
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.dobNumeric,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(95.dp)
                            )
                            Text(
                                text = item.verificationCode.ifBlank { "-" },
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(90.dp)
                            )
                            Text(
                                text = "✓ Meta AI লগইন",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCopyAllForSheet,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEn) "Copy Sheet Table" else "শিট টেবিল কপি করুন")
                }

                Button(
                    onClick = onOpenFullSheetScreen,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("open_google_sheet_vault_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEn) "Google Workspace Editor" else "গুগল ওয়ার্কস্পেস এডিটর")
                }
            }
        }
    }
}

@Composable
private fun MiniInfoPill(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbeddedBrowserWebView(
    url: String,
    testTagName: String,
    blockFacebookRedirects: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    onPageLoadingChange: (Boolean) -> Unit,
    onPageFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .testTag(testTagName),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadsImagesAutomatically = true
                    javaScriptCanOpenWindowsAutomatically = true
                    setSupportMultipleWindows(true)
                    userAgentString = CHROME_MOBILE_USER_AGENT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?
                    ): Boolean {
                        val parentWebView = view ?: return false
                        val tempWebView = WebView(parentWebView.context)
                        tempWebView.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                v: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val targetUrl = request?.url?.toString().orEmpty()
                                val isBlocked = blockFacebookRedirects &&
                                    (targetUrl.contains("facebook.com", ignoreCase = true) ||
                                        targetUrl.contains("instagram.com", ignoreCase = true))
                                if (targetUrl.isNotBlank() && !isBlocked) {
                                    parentWebView.loadUrl(targetUrl)
                                }
                                tempWebView.destroy()
                                return true
                            }
                        }
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        transport?.webView = tempWebView
                        resultMsg?.sendToTarget()
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onPageLoadingChange(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onPageLoadingChange(false)
                        onPageFinished()
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val targetUrl = request?.url?.toString().orEmpty()
                        if (blockFacebookRedirects &&
                            (targetUrl.contains("facebook.com", ignoreCase = true) ||
                                targetUrl.contains("instagram.com", ignoreCase = true))
                        ) {
                            // Never allow redirecting to Facebook or Instagram; keep user on Meta AI
                            return true
                        }
                        return false
                    }
                }
                onWebViewCreated(this)
                loadUrl(url)
            }
        }
    )
}
