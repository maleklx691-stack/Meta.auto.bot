package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ProfileRepository
import com.example.ui.AppDestination
import com.example.ui.AutoCreateAccountScreen
import com.example.ui.GeneratorScreen
import com.example.ui.MetaIdentityViewModel
import com.example.ui.ToolsScreen
import com.example.ui.VaultScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = ProfileRepository(database.profileDao())

        setContent {
            MyApplicationTheme {
                val viewModel: MetaIdentityViewModel = viewModel(
                    factory = MetaIdentityViewModel.Factory(repository)
                )
                MetaIdentityApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaIdentityApp(viewModel: MetaIdentityViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedProfiles by viewModel.savedProfiles.collectAsStateWithLifecycle()
    val searchQuery by viewModel.vaultSearchQuery.collectAsStateWithLifecycle()
    val favoritesOnly by viewModel.vaultFavoritesOnly.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.AUTO_CREATE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isEn = uiState.useEnglishUiLabels

    if (currentDestination != AppDestination.AUTO_CREATE) {
        BackHandler {
            currentDestination = AppDestination.AUTO_CREATE
        }
    }

    val showToastMessage: (String) -> Unit = { msg ->
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    val copyToClipboard: (String, String) -> Unit = { label, text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        showToastMessage(
            if (isEn) "$label copied to clipboard!" else "$label ক্লিপবোর্ডে কপি হয়েছে!"
        )
    }

    val pasteEmailFromClipboard: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val pastedText = clipboard?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()
            .orEmpty()

        if (pastedText.isNotEmpty()) {
            viewModel.updateTempEmail(pastedText)
            showToastMessage(
                if (isEn) "Temp Email pasted!" else "টেম্প ইমেইল পেস্ট করা হয়েছে!"
            )
        } else {
            showToastMessage(
                if (isEn) "Clipboard is empty" else "ক্লিপবোর্ডে কোনো টেক্সট নেই"
            )
        }
    }

    val pasteOtpFromClipboard: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val pastedText = clipboard?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()
            .orEmpty()

        if (pastedText.isNotEmpty()) {
            val code = viewModel.pasteAndExtractOtpCode(pastedText)
            showToastMessage(
                if (isEn) "OTP Code '$code' extracted!" else "ইমেইল কোড '$code' বসানো হয়েছে!"
            )
        } else {
            showToastMessage(
                if (isEn) "Clipboard is empty" else "ক্লিপবোর্ডে কোনো কোড নেই"
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEn) "Meta AI Auto Studio" else "Meta AI অটো একাউন্ট স্টুডিও",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = { viewModel.toggleUiLanguage() },
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .testTag("language_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Switch Language"
                            )
                            Text(
                                text = if (isEn) " বাংলা" else " EN",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    DestinationIcon(
                                        destination = dest,
                                        selected = selected,
                                        vaultCount = savedProfiles.size
                                    )
                                },
                                label = {
                                    Text(if (isEn) dest.titleEn else dest.titleBn)
                                },
                                modifier = Modifier.testTag("nav_tab_${dest.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(modifier = Modifier.testTag("side_navigation_rail")) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    DestinationIcon(
                                        destination = dest,
                                        selected = selected,
                                        vaultCount = savedProfiles.size
                                    )
                                },
                                label = {
                                    Text(if (isEn) dest.titleEn else dest.titleBn)
                                },
                                modifier = Modifier.testTag("rail_tab_${dest.name.lowercase()}")
                            )
                        }
                    }
                }

                when (currentDestination) {
                    AppDestination.AUTO_CREATE -> {
                        AutoCreateAccountScreen(
                            uiState = uiState,
                            savedProfiles = savedProfiles,
                            onStartOneClickAutomation = {
                                viewModel.startOneClickMetaAiAutomation { created ->
                                    showToastMessage(
                                        if (isEn) {
                                            "Meta AI Account (${created.tempEmail}) Verified & Saved!"
                                        } else {
                                            "Meta AI একাউন্ট (${created.tempEmail}) ভেরিফাইড ও গুগল শিট টেবিলে সেভ হয়েছে!"
                                        }
                                    )
                                }
                            },
                            onStopAutomation = {
                                viewModel.stopMetaAiAutomation()
                                showToastMessage(
                                    if (isEn) "Automation stopped" else "অটোমেশন থামানো হয়েছে"
                                )
                            },
                            onToggleAutoLoop = viewModel::toggleAutoLoopMode,
                            onTopTempMailScraped = { email, otp ->
                                viewModel.onTopTempMailWebScraped(email, otp) { created ->
                                    showToastMessage(
                                        if (isEn) {
                                            "Auto-Verified OTP (${created.verificationCode}) & Saved!"
                                        } else {
                                            "অটো কোড (${created.verificationCode}) ভেরিফাইড ও শিটে সেভ হয়েছে!"
                                        }
                                    )
                                }
                            },
                            onMetaDomStageDetected = viewModel::onMetaWebViewDomStageDetected,
                            onManualOtpSubmit = { otp ->
                                if (otp.isBlank()) {
                                    showToastMessage(
                                        if (isEn) "Enter or wait for the OTP code first"
                                        else "প্রথমে মেইল-কি (OTP কোড) দিন অথবা অটো কোডের জন্য অপেক্ষা করুন"
                                    )
                                } else {
                                    viewModel.onRealOtpReceivedAndComplete(otp) { created ->
                                        showToastMessage(
                                            if (isEn) "Account Verified & Saved to Google Sheet!"
                                            else "কোড বসিয়ে একাউন্ট গুগল শিট টেবিলে সেভ হয়েছে!"
                                        )
                                    }
                                }
                            },
                            onOtpCodeChange = viewModel::updateOtpCode,
                            onPasteOtpClick = pasteOtpFromClipboard,
                            onRefreshInboxClick = {
                                viewModel.refreshInboxNow { created ->
                                    showToastMessage(
                                        if (isEn) "OTP Received (${created.verificationCode}) & Verified!"
                                        else "মেইল-কি (${created.verificationCode}) পাওয়া গেছে ও ভেরিফাইড হয়েছে!"
                                    )
                                }
                            },
                            onToggleUseCustomPassword = viewModel::toggleUseCustomFixedPassword,
                            onCustomFixedPasswordChange = viewModel::updateCustomFixedPassword,
                            onUpdateSavedPassword = { id, newPass ->
                                viewModel.updateSavedProfilePassword(id, newPass)
                                showToastMessage(
                                    if (isEn) "Password updated!" else "পাসওয়ার্ড নির্ধারণ সম্পন্ন হয়েছে!"
                                )
                            },
                            onTriggerManualAutoFill = {
                                viewModel.triggerManualWebAutoFill()
                                showToastMessage(
                                    if (isEn) "Auto-Fill & Next triggered!"
                                    else "অটো-ফিল ও Next ক্লিক করা হয়েছে!"
                                )
                            },
                            onCopyText = copyToClipboard,
                            onNavigateToVault = {
                                currentDestination = AppDestination.VAULT
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AppDestination.GENERATOR -> {
                        GeneratorScreen(
                            uiState = uiState,
                            savedVaultCount = savedProfiles.size,
                            onTempEmailChange = viewModel::updateTempEmail,
                            onPasteEmailClick = pasteEmailFromClipboard,
                            onApplySuggestedEmail = viewModel::applySuggestedEmail,
                            onScriptModeChange = viewModel::setScriptMode,
                            onCultureStyleChange = viewModel::setCultureStyle,
                            onGenderFilterChange = viewModel::setGenderFilter,
                            onPasswordLengthChange = viewModel::setPasswordLength,
                            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                            onGenerateNewProfile = viewModel::generateNewProfile,
                            onRegenerateNameOnly = viewModel::regenerateNameOnly,
                            onRegenerateDobOnly = viewModel::regenerateDobOnly,
                            onRegeneratePasswordOnly = viewModel::regeneratePasswordOnly,
                            onAccountNoteChange = viewModel::updateAccountNote,
                            onSaveToVault = {
                                viewModel.saveCurrentProfileToVault {
                                    showToastMessage(
                                        if (isEn) "Profile saved to Vault!" else "প্রোফাইলটি ভল্টে সংরক্ষণ করা হয়েছে!"
                                    )
                                }
                            },
                            onLaunchAutoCreate = {
                                currentDestination = AppDestination.AUTO_CREATE
                                viewModel.startOneClickMetaAiAutomation()
                            },
                            onCopyText = copyToClipboard,
                            onRestoreSessionProfile = viewModel::restoreFromSessionHistory,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AppDestination.VAULT -> {
                        VaultScreen(
                            isEn = isEn,
                            savedProfiles = savedProfiles,
                            googleWorkspaceUrl = uiState.googleWorkspaceUrl,
                            searchQuery = searchQuery,
                            favoritesOnly = favoritesOnly,
                            onGoogleWorkspaceUrlChange = viewModel::updateGoogleWorkspaceUrl,
                            onSearchQueryChange = viewModel::updateVaultSearchQuery,
                            onToggleFavoritesOnly = viewModel::toggleVaultFavoritesFilter,
                            onToggleProfileFavorite = viewModel::toggleProfileFavorite,
                            onUpdateNote = viewModel::updateSavedProfileNote,
                            onUpdatePassword = { id, newPass ->
                                viewModel.updateSavedProfilePassword(id, newPass)
                                showToastMessage(
                                    if (isEn) "Password updated in Sheet!"
                                    else "পাসওয়ার্ড আপডেট করা হয়েছে!"
                                )
                            },
                            onDeleteProfile = viewModel::deleteSavedProfile,
                            onCopyText = copyToClipboard,
                            onNavigateToGenerator = {
                                currentDestination = AppDestination.AUTO_CREATE
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AppDestination.TOOLS -> {
                        ToolsScreen(
                            uiState = uiState,
                            onCustomPasswordChange = viewModel::updateCustomTestPassword,
                            onGenerateBatchPasswords = viewModel::generateBatchPasswords,
                            onToggleChecklistItem = viewModel::toggleChecklistItem,
                            onResetChecklist = viewModel::resetChecklist,
                            onCopyText = copyToClipboard,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(
    destination: AppDestination,
    selected: Boolean,
    vaultCount: Int
) {
    when (destination) {
        AppDestination.AUTO_CREATE -> {
            Icon(
                imageVector = if (selected) Icons.Filled.Bolt else Icons.Outlined.Bolt,
                contentDescription = destination.titleEn
            )
        }
        AppDestination.GENERATOR -> {
            Icon(
                imageVector = if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                contentDescription = destination.titleEn
            )
        }
        AppDestination.VAULT -> {
            BadgedBox(
                badge = {
                    if (vaultCount > 0) {
                        Badge { Text(vaultCount.toString()) }
                    }
                }
            ) {
                Icon(
                    imageVector = if (selected) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks,
                    contentDescription = destination.titleEn
                )
            }
        }
        AppDestination.TOOLS -> {
            Icon(
                imageVector = if (selected) Icons.Filled.Security else Icons.Outlined.Security,
                contentDescription = destination.titleEn
            )
        }
    }
}
