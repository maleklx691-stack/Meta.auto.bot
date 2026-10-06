package com.example.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.SavedProfileEntity
import com.example.domain.IdentityGeneratorEngine
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun VaultScreen(
    isEn: Boolean,
    savedProfiles: List<SavedProfileEntity>,
    googleWorkspaceUrl: String,
    searchQuery: String,
    favoritesOnly: Boolean,
    onGoogleWorkspaceUrlChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onToggleProfileFavorite: (SavedProfileEntity) -> Unit,
    onUpdateNote: (Long, String) -> Unit,
    onUpdatePassword: (Long, String) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onCopyText: (String, String) -> Unit,
    onNavigateToGenerator: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingNoteProfile by remember { mutableStateOf<SavedProfileEntity?>(null) }
    var editingPasswordProfile by remember { mutableStateOf<SavedProfileEntity?>(null) }
    var showGoogleWorkspaceEditor by rememberSaveable { mutableStateOf(false) }
    var showSpreadsheetTableMode by rememberSaveable { mutableStateOf(true) }
    var workspaceWebViewRef by remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("vault_profiles_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // 1. Google Workspace Public Editing Sheet & Table Export Header Card
            item {
                GoogleWorkspaceSheetCard(
                    isEn = isEn,
                    savedProfiles = savedProfiles,
                    googleWorkspaceUrl = googleWorkspaceUrl,
                    showGoogleWorkspaceEditor = showGoogleWorkspaceEditor,
                    showSpreadsheetTableMode = showSpreadsheetTableMode,
                    onGoogleWorkspaceUrlChange = onGoogleWorkspaceUrlChange,
                    onToggleGoogleWorkspaceEditor = {
                        showGoogleWorkspaceEditor = !showGoogleWorkspaceEditor
                    },
                    onToggleSpreadsheetTableMode = {
                        showSpreadsheetTableMode = !showSpreadsheetTableMode
                    },
                    onLoadWorkspaceUrl = { url ->
                        onGoogleWorkspaceUrlChange(url)
                        workspaceWebViewRef?.loadUrl(url)
                    },
                    onWorkspaceWebViewCreated = { workspaceWebViewRef = it },
                    onCopyText = onCopyText
                )
            }

            // 2. Live Editable Accounts Spreadsheet Table (for setting/editing passwords & copying rows)
            if (showSpreadsheetTableMode && savedProfiles.isNotEmpty()) {
                item {
                    AccountsSpreadsheetTableCard(
                        isEn = isEn,
                        profiles = savedProfiles,
                        onEditPassword = { editingPasswordProfile = it },
                        onCopyRow = { item ->
                            val rowTsv =
                                "${item.fullNameEn}\t${item.tempEmail}\t${item.password}\t${item.dobNumeric}\t${item.verificationCode}"
                            onCopyText("Sheet Row", rowTsv)
                        }
                    )
                }
            }

            // 3. Search & Filter Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vault_search_input"),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        placeholder = {
                            Text(
                                if (isEn) "Search name, email, OTP, or note..."
                                else "নাম, ইমেইল, কোড বা নোট দিয়ে খুঁজুন..."
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = if (isEn) "Clear search" else "সার্চ মুছুন"
                                    )
                                }
                            }
                        }
                    )

                    FilterChip(
                        selected = favoritesOnly,
                        onClick = onToggleFavoritesOnly,
                        label = {
                            Text(if (isEn) "Starred" else "প্রিয়")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (favoritesOnly) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("vault_favorites_filter_chip")
                    )
                }
            }

            if (savedProfiles.isEmpty()) {
                item {
                    EmptyVaultPlaceholder(
                        isEn = isEn,
                        hasFilter = searchQuery.isNotBlank() || favoritesOnly,
                        onNavigateToGenerator = onNavigateToGenerator
                    )
                }
            } else {
                items(savedProfiles, key = { it.id }) { item ->
                    SavedProfileVaultCard(
                        isEn = isEn,
                        profile = item,
                        onToggleFavorite = { onToggleProfileFavorite(item) },
                        onEditNote = { editingNoteProfile = item },
                        onEditPassword = { editingPasswordProfile = item },
                        onDelete = { onDeleteProfile(item.id) },
                        onCopyText = onCopyText
                    )
                }
            }
        }
    }

    // Dialog 1: Edit Account Note
    editingNoteProfile?.let { target ->
        var draftNote by rememberSaveable(target.id) { mutableStateOf(target.accountNote) }
        AlertDialog(
            onDismissRequest = { editingNoteProfile = null },
            title = {
                Text(if (isEn) "Edit Account Note / Group" else "একাউন্ট নোট বা গ্রুপ সম্পাদনা করুন")
            },
            text = {
                OutlinedTextField(
                    value = draftNote,
                    onValueChange = { draftNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(if (isEn) "Note / Group Name" else "নোট বা গ্রুপের নাম")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateNote(target.id, draftNote)
                        editingNoteProfile = null
                    }
                ) {
                    Text(if (isEn) "Save" else "সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNoteProfile = null }) {
                    Text(if (isEn) "Cancel" else "বাতিল")
                }
            }
        )
    }

    // Dialog 2: Set / Edit Password for Account ("পাসওয়ার্ড নির্ধারণ করার জন্য")
    editingPasswordProfile?.let { target ->
        var draftPassword by rememberSaveable(target.id) { mutableStateOf(target.password) }
        AlertDialog(
            onDismissRequest = { editingPasswordProfile = null },
            title = {
                Text(
                    if (isEn) "Set / Edit Account Password"
                    else "একাউন্টের পাসওয়ার্ড নির্ধারণ বা পরিবর্তন করুন"
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
                            Text(if (isEn) "New Password" else "নতুন পাসওয়ার্ড দিন")
                        }
                    )
                    FilledTonalButton(
                        onClick = {
                            draftPassword = IdentityGeneratorEngine.generateStrongPassword(14)
                        }
                    ) {
                        Text(if (isEn) "Generate Strong Password" else "অটো স্ট্রং পাসওয়ার্ড তৈরি করুন")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdatePassword(target.id, draftPassword)
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GoogleWorkspaceSheetCard(
    isEn: Boolean,
    savedProfiles: List<SavedProfileEntity>,
    googleWorkspaceUrl: String,
    showGoogleWorkspaceEditor: Boolean,
    showSpreadsheetTableMode: Boolean,
    onGoogleWorkspaceUrlChange: (String) -> Unit,
    onToggleGoogleWorkspaceEditor: () -> Unit,
    onToggleSpreadsheetTableMode: () -> Unit,
    onLoadWorkspaceUrl: (String) -> Unit,
    onWorkspaceWebViewCreated: (WebView) -> Unit,
    onCopyText: (String, String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_workspace_sheet_card"),
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
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = if (isEn) {
                                "Google Workspace Public Editing Tools & Sheet"
                            } else {
                                "গুগল ওয়ার্কস্পেস পাবলিক এডিটিং টুলস ও পাসওয়ার্ড শিট"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEn) {
                                "Copy all accounts for Google Sheets or open your Public Sheet link"
                            } else {
                                "খোলা একাউন্টগুলোর মেইল ও পাসওয়ার্ড গুগল শিটে বসান ও এডিট করুন"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val sheetRows = buildString {
                            appendLine("Name (BN)\tName (EN)\tTemp Email\tPassword\tDOB\tOTP Code\tNote")
                            savedProfiles.forEach { p ->
                                appendLine("${p.fullNameBn}\t${p.fullNameEn}\t${p.tempEmail}\t${p.password}\t${p.dobNumeric}\t${p.verificationCode}\t${p.accountNote}")
                            }
                        }.trim()
                        onCopyText(
                            if (isEn) "Google Sheet Table" else "গুগল শিট টেবিল",
                            sheetRows
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .testTag("copy_all_for_google_sheets_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEn) "Copy Sheet Rows" else "গুগল শিটের জন্য সব কপি")
                }

                FilledTonalButton(
                    onClick = onToggleGoogleWorkspaceEditor,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .testTag("toggle_google_workspace_webview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (showGoogleWorkspaceEditor) {
                            if (isEn) "Hide Google Sheet" else "গুগল শিট লুকান"
                        } else {
                            if (isEn) "Open Public Sheet" else "পাবলিক গুগল শিট খুলুন"
                        }
                    )
                }
            }

            AnimatedVisibility(visible = showGoogleWorkspaceEditor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    OutlinedTextField(
                        value = googleWorkspaceUrl,
                        onValueChange = onGoogleWorkspaceUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_workspace_url_input"),
                        singleLine = true,
                        label = {
                            Text(
                                if (isEn) "Paste Public Google Sheet / Docs Editing URL"
                                else "আপনার পাবলিক গুগল শিট বা ডক লিংক দিন"
                            )
                        },
                        trailingIcon = {
                            TextButton(onClick = { onLoadWorkspaceUrl(googleWorkspaceUrl) }) {
                                Text(if (isEn) "Open" else "লোড")
                            }
                        }
                    )

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                MaterialTheme.shapes.medium
                            ),
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
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                }
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()
                                onWorkspaceWebViewCreated(this)
                                loadUrl(googleWorkspaceUrl)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountsSpreadsheetTableCard(
    isEn: Boolean,
    profiles: List<SavedProfileEntity>,
    onEditPassword: (SavedProfileEntity) -> Unit,
    onCopyRow: (SavedProfileEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("accounts_spreadsheet_table_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isEn) {
                    "Live Editable Accounts Sheet (Tap Password to Change)"
                } else {
                    "একাউন্টস ও পাসওয়ার্ড নির্ধারণ টেবিল (পাসওয়ার্ডে ট্যাপ করে পরিবর্তন করুন)"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("নাম (Name)", fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp))
                    Text("ইমেইল (Temp Mail)", fontWeight = FontWeight.Bold, modifier = Modifier.width(180.dp))
                    Text("পাসওয়ার্ড (Tap to Edit)", fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp))
                    Text("জন্ম তারিখ (DOB)", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
                    Text("কোড (OTP)", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                    Text("কপি", fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                }

                profiles.forEach { item ->
                    Row(
                        modifier = Modifier
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.fullNameBn,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(130.dp)
                        )
                        Text(
                            text = item.tempEmail,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(180.dp)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .width(150.dp)
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
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            text = item.verificationCode.ifBlank { "-" },
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(80.dp)
                        )
                        IconButton(
                            onClick = { onCopyRow(item) },
                            modifier = Modifier
                                .width(60.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Row",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyVaultPlaceholder(
    isEn: Boolean,
    hasFilter: Boolean,
    onNavigateToGenerator: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                text = if (hasFilter) {
                    if (isEn) "No matching profiles found" else "কোনো মিল খুঁজে পাওয়া যায়নি"
                } else {
                    if (isEn) "Your Google Sheet & Local Vault is Empty" else "আপনার শিট ও সংরক্ষিত ভল্ট এখনো খালি"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isEn) {
                    "Auto-created Meta accounts and saved profiles will appear here in an editable spreadsheet table."
                } else {
                    "অটো মেটা একাউন্ট ট্যাব থেকে একাউন্ট তৈরি করলেই এখানে টেবিল আকারে নাম, ইমেইল, জন্ম তারিখ ও পাসওয়ার্ড চলে আসবে।"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!hasFilter) {
                OutlinedButton(
                    onClick = onNavigateToGenerator,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("empty_vault_go_generator_button")
                ) {
                    Text(if (isEn) "Generate & Create Account" else "নতুন একাউন্ট তৈরি করুন")
                }
            }
        }
    }
}

@Composable
private fun SavedProfileVaultCard(
    isEn: Boolean,
    profile: SavedProfileEntity,
    onToggleFavorite: () -> Unit,
    onEditNote: () -> Unit,
    onEditPassword: () -> Unit,
    onDelete: () -> Unit,
    onCopyText: (String, String) -> Unit
) {
    var showPassword by rememberSaveable(profile.id) { mutableStateOf(true) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_card_${profile.id}"),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
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
                        text = "${profile.fullNameBn} (${profile.fullNameEn})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${profile.usernameHandle}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (profile.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isEn) "Toggle Favorite" else "প্রিয় তালিকায় যোগ করুন",
                            tint = if (profile.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onEditPassword) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = if (isEn) "Set Password" else "পাসওয়ার্ড পরিবর্তন"
                        )
                    }
                    IconButton(onClick = onEditNote) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = if (isEn) "Edit Note" else "নোট সম্পাদনা"
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_vault_profile_${profile.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = if (isEn) "Delete profile" else "মুছে ফেলুন",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (profile.verificationCode.isNotBlank() || profile.accountStatus == "VERIFIED") {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isEn) {
                            "✓ Auto-Created & Verified (OTP: ${profile.verificationCode})"
                        } else {
                            "✓ অটো-ক্রিয়েটেড ও ভেরিফাইড (ইমেইল কোড: ${profile.verificationCode})"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            if (profile.accountNote.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "📌 ${profile.accountNote}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider()

            // DOB & Age Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) {
                        "DOB: ${profile.dobFormattedEn} (${profile.ageYears} yrs)"
                    } else {
                        "জন্ম তারিখ: ${profile.dobFormattedBn} (${profile.dobNumeric} • ${IdentityGeneratorEngine.toBengaliDigits(profile.ageYears)} বছর)"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = { onCopyText("DOB", profile.dobNumeric) }
                ) {
                    Text(if (isEn) "Copy DOB" else "কপি")
                }
            }

            // Temp Email Row
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyText("Temp Email", profile.tempEmail) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "Temp / Custom Email" else "টেম্প / কাস্টম ইমেইল",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = profile.tempEmail,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Email",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Password Row (with Edit Password button)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "Password (Tap Key to Edit)" else "নির্ধারিত পাসওয়ার্ড (পরিবর্তনযোগ্য)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (showPassword) profile.password else "•".repeat(profile.password.length),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row {
                        IconButton(onClick = onEditPassword) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Edit Password",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Show/Hide Password"
                            )
                        }
                        IconButton(onClick = { onCopyText("Password", profile.password) }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Password",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Copy Full Saved Profile Button
            OutlinedButton(
                onClick = {
                    val summary = buildString {
                        appendLine("First Name: ${profile.firstNameBn} (${profile.firstNameEn})")
                        appendLine("Last Name: ${profile.lastNameBn} (${profile.lastNameEn})")
                        appendLine("DOB: ${profile.dobFormattedEn} (${profile.dobNumeric}) - Age ${profile.ageYears}")
                        appendLine("Email: ${profile.tempEmail}")
                        appendLine("Password: ${profile.password}")
                        if (profile.verificationCode.isNotBlank()) {
                            appendLine("OTP Code: ${profile.verificationCode}")
                        }
                        if (profile.accountNote.isNotBlank()) {
                            appendLine("Note: ${profile.accountNote}")
                        }
                    }.trim()
                    onCopyText("Saved Profile", summary)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isEn) "Copy Full Credentials" else "সম্পূর্ণ একাউন্ট তথ্য কপি করুন")
            }
        }
    }
}
