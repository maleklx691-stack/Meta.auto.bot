package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.GenderFilter
import com.example.domain.GeneratedIdentityProfile
import com.example.domain.IdentityGeneratorEngine
import com.example.domain.NameCultureStyle
import com.example.domain.NameScriptMode
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratorScreen(
    uiState: GeneratorUiState,
    savedVaultCount: Int,
    onTempEmailChange: (String) -> Unit,
    onPasteEmailClick: () -> Unit,
    onApplySuggestedEmail: (String) -> Unit,
    onScriptModeChange: (NameScriptMode) -> Unit,
    onCultureStyleChange: (NameCultureStyle) -> Unit,
    onGenderFilterChange: (GenderFilter) -> Unit,
    onPasswordLengthChange: (Int) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onGenerateNewProfile: () -> Unit,
    onRegenerateNameOnly: () -> Unit,
    onRegenerateDobOnly: () -> Unit,
    onRegeneratePasswordOnly: () -> Unit,
    onAccountNoteChange: (String) -> Unit,
    onSaveToVault: () -> Unit,
    onLaunchAutoCreate: () -> Unit,
    onCopyText: (label: String, value: String) -> Unit,
    onRestoreSessionProfile: (GeneratedIdentityProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEn = uiState.useEnglishUiLabels
    val profile = uiState.currentProfile
    var showAdvancedSettings by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("generator_scroll_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Banner Card
            item {
                HeroBannerCard(
                    isEn = isEn,
                    generationCount = uiState.generationCount,
                    savedVaultCount = savedVaultCount,
                    onGenerateNew = onGenerateNewProfile
                )
            }

            // 2. Step 1: User's Temporary / Custom Email Input Card (Condition 4)
            item {
                TempEmailInputCard(
                    isEn = isEn,
                    emailInput = uiState.tempEmailInput,
                    isEmailValid = uiState.isEmailValid,
                    onTempEmailChange = onTempEmailChange,
                    onPasteEmailClick = onPasteEmailClick,
                    onApplySuggestedEmail = onApplySuggestedEmail,
                    onCopyEmail = {
                        val effective = uiState.tempEmailInput.ifBlank {
                            IdentityGeneratorEngine.suggestTempEmailForProfile(profile)
                        }
                        onCopyText(
                            if (isEn) "Temp Email" else "টেম্প ইমেইল",
                            effective
                        )
                    }
                )
            }

            // 3. Customization & Filter Bar (Script, Culture, Gender, Password Length)
            item {
                GeneratorPreferencesCard(
                    isEn = isEn,
                    uiState = uiState,
                    showAdvanced = showAdvancedSettings,
                    onToggleAdvanced = { showAdvancedSettings = !showAdvancedSettings },
                    onScriptModeChange = onScriptModeChange,
                    onCultureStyleChange = onCultureStyleChange,
                    onGenderFilterChange = onGenderFilterChange,
                    onPasswordLengthChange = onPasswordLengthChange
                )
            }

            // 4. Centerpiece: Generated Meta / Facebook Unique Profile Card
            item {
                GeneratedProfileMasterCard(
                    isEn = isEn,
                    profile = profile,
                    scriptMode = uiState.scriptMode,
                    tempEmailInput = uiState.tempEmailInput,
                    accountNoteInput = uiState.accountNoteInput,
                    isPasswordVisible = uiState.isPasswordVisible,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    onRegenerateNameOnly = onRegenerateNameOnly,
                    onRegenerateDobOnly = onRegenerateDobOnly,
                    onRegeneratePasswordOnly = onRegeneratePasswordOnly,
                    onGenerateNewProfile = onGenerateNewProfile,
                    onAccountNoteChange = onAccountNoteChange,
                    onSaveToVault = onSaveToVault,
                    onLaunchAutoCreate = onLaunchAutoCreate,
                    onApplySuggestedEmail = { onApplySuggestedEmail("temp-mail.org") },
                    onCopyText = onCopyText
                )
            }

            // 5. Recent Session Generated Profiles Strip
            if (uiState.recentSessionProfiles.size > 1) {
                item {
                    RecentSessionHistorySection(
                        isEn = isEn,
                        profiles = uiState.recentSessionProfiles,
                        currentProfile = profile,
                        onSelectProfile = onRestoreSessionProfile
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HeroBannerCard(
    isEn: Boolean,
    generationCount: Int,
    savedVaultCount: Int,
    onGenerateNew: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_banner_card"),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1791256610690),
                contentDescription = "Meta Identity Studio Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xEE061229),
                                Color(0xCC091D42),
                                Color(0x880B2554)
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF0866FF),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isEn) "Meta AI Ready Identity" else "Meta AI রেডি প্রোফাইল",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = if (isEn) {
                                    "Saved: $savedVaultCount | #${generationCount}"
                                } else {
                                    "ভল্ট: ${IdentityGeneratorEngine.toBengaliDigits(savedVaultCount)} | #${IdentityGeneratorEngine.toBengaliDigits(generationCount)}"
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isEn) {
                                "Unique Name, 18–35 DOB & Strong Password"
                            } else {
                                "ইউনিক নাম, ১৮-৩৫ বয়স ও স্ট্রং পাসওয়ার্ড"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEn) {
                                "Provide your Temp Email below and generate instant registration-ready details."
                            } else {
                                "আপনার টেম্প ইমেইল দিন এবং ১ ক্লিকেই একাউন্ট খোলার সম্পূর্ণ ইউনিক তথ্য তৈরি করুন।"
                            },
                            color = Color.White.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TempEmailInputCard(
    isEn: Boolean,
    emailInput: String,
    isEmailValid: Boolean,
    onTempEmailChange: (String) -> Unit,
    onPasteEmailClick: () -> Unit,
    onApplySuggestedEmail: (String) -> Unit,
    onCopyEmail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("temp_email_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (emailInput.isNotBlank() && isEmailValid) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (isEn) {
                                "4. Your Temp / Custom Email"
                            } else {
                                "শর্ত ৪: আপনার টেম্পোরারি / কাস্টম ইমেইল দিন"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn) {
                                "Paste your Temp Email or tap a domain chip below"
                            } else {
                                "আপনার Temp Email এখানে পেস্ট করুন অথবা নিচের ডোমেইন সিলেক্ট করুন"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (emailInput.isNotBlank()) {
                    Surface(
                        color = if (isEmailValid) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (isEmailValid) {
                                if (isEn) "✓ Ready" else "✓ সঠিক ইমেইল"
                            } else {
                                if (isEn) "Add @domain" else "@domain দিন"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isEmailValid) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = emailInput,
                onValueChange = onTempEmailChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("temp_email_input"),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                label = {
                    Text(
                        if (isEn) "Enter or Paste Temp / Custom Email"
                        else "টেম্পোরারি / কাস্টম ইমেইল লিখুন বা পেস্ট করুন"
                    )
                },
                placeholder = {
                    Text("example@temp-mail.org")
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (emailInput.isNotEmpty()) {
                            IconButton(
                                onClick = { onTempEmailChange("") },
                                modifier = Modifier.testTag("clear_email_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = if (isEn) "Clear email" else "ইমেইল মুছুন"
                                )
                            }
                        }
                        IconButton(
                            onClick = onPasteEmailClick,
                            modifier = Modifier.testTag("paste_email_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = if (isEn) "Paste email from clipboard" else "ক্লিপবোর্ড থেকে পেস্ট করুন",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )

            // Quick Action & Domain Helper Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onPasteEmailClick,
                    label = {
                        Text(if (isEn) "Paste Clipboard" else "পেস্ট করুন")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("chip_paste_clipboard")
                )

                AssistChip(
                    onClick = { onApplySuggestedEmail("temp-mail.org") },
                    label = {
                        Text(if (isEn) "Auto from Name" else "নাম দিয়ে অটো ইমেইল")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("chip_auto_suggest_email")
                )

                IdentityGeneratorEngine.popularTempDomains.forEach { domain ->
                    AssistChip(
                        onClick = { onApplySuggestedEmail(domain) },
                        label = { Text("@$domain") },
                        modifier = Modifier.testTag("domain_chip_${domain.replace('.', '_')}")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneratorPreferencesCard(
    isEn: Boolean,
    uiState: GeneratorUiState,
    showAdvanced: Boolean,
    onToggleAdvanced: () -> Unit,
    onScriptModeChange: (NameScriptMode) -> Unit,
    onCultureStyleChange: (NameCultureStyle) -> Unit,
    onGenderFilterChange: (GenderFilter) -> Unit,
    onPasswordLengthChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("generator_preferences_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Text(
                    text = if (isEn) "Name & Security Options" else "নাম ও পাসওয়ার্ড অপশন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = onToggleAdvanced,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("toggle_advanced_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showAdvanced) {
                            if (isEn) "Hide Options" else "সংক্ষেপ করুন"
                        } else {
                            if (isEn) "Customize" else "কাস্টমাইজ"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Script Mode Filter Chips (Both / Bengali / English)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NameScriptMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.scriptMode == mode,
                        onClick = { onScriptModeChange(mode) },
                        label = {
                            Text(if (isEn) mode.labelEn else mode.labelBn)
                        },
                        modifier = Modifier.testTag("script_mode_${mode.name.lowercase()}")
                    )
                }
            }

            // Gender Filter Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GenderFilter.entries.forEach { gender ->
                    FilterChip(
                        selected = uiState.genderFilter == gender,
                        onClick = { onGenderFilterChange(gender) },
                        label = {
                            Text(if (isEn) gender.labelEn else gender.labelBn)
                        },
                        modifier = Modifier.testTag("gender_filter_${gender.name.lowercase()}")
                    )
                }

                NameCultureStyle.entries.forEach { style ->
                    FilterChip(
                        selected = uiState.cultureStyle == style,
                        onClick = { onCultureStyleChange(style) },
                        label = {
                            Text(if (isEn) style.labelEn else style.labelBn)
                        },
                        modifier = Modifier.testTag("culture_style_${style.name.lowercase()}")
                    )
                }
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isEn) {
                                "Strong Password Length: ${uiState.passwordLength} chars (Min 8)"
                            } else {
                                "পাসওয়ার্ডের দৈর্ঘ্য: ${IdentityGeneratorEngine.toBengaliDigits(uiState.passwordLength)} অক্ষর (সর্বনিম্ন ৮)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Slider(
                        value = uiState.passwordLength.toFloat(),
                        onValueChange = { onPasswordLengthChange(it.toInt()) },
                        valueRange = 8f..24f,
                        steps = 15,
                        modifier = Modifier.testTag("password_length_slider")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneratedProfileMasterCard(
    isEn: Boolean,
    profile: GeneratedIdentityProfile,
    scriptMode: NameScriptMode,
    tempEmailInput: String,
    accountNoteInput: String,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onRegenerateNameOnly: () -> Unit,
    onRegenerateDobOnly: () -> Unit,
    onRegeneratePasswordOnly: () -> Unit,
    onGenerateNewProfile: () -> Unit,
    onAccountNoteChange: (String) -> Unit,
    onSaveToVault: () -> Unit,
    onLaunchAutoCreate: () -> Unit,
    onApplySuggestedEmail: () -> Unit,
    onCopyText: (label: String, value: String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("generated_profile_master_card"),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header + Regenerate All Primary CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEn) "Generated Meta AI Profile" else "জেনারেটেড Meta AI প্রোফাইল",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "@${profile.usernameHandle}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            onCopyText("Username", profile.usernameHandle)
                        }
                    )
                }

                Button(
                    onClick = onGenerateNewProfile,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("generate_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "New Profile" else "নতুন তথ্য",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider()

            // SECTION 1: First Name & Last Name (Bengali & English)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEn) {
                                "1. First Name & Last Name"
                            } else {
                                "১. সুন্দর নাম (First Name & Last Name)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onRegenerateNameOnly,
                        modifier = Modifier.testTag("regenerate_name_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isEn) "Regenerate Name" else "শুধু নাম পরিবর্তন করুন"
                        )
                    }
                }

                if (scriptMode == NameScriptMode.BOTH || scriptMode == NameScriptMode.BENGALI) {
                    NamePairRow(
                        badgeLabel = "বাংলা নাম",
                        firstLabel = if (isEn) "First Name (BN)" else "ফার্স্ট নেম (বাংলা)",
                        firstValue = profile.firstNameBn,
                        lastLabel = if (isEn) "Last Name (BN)" else "লাস্ট নেম (বাংলা)",
                        lastValue = profile.lastNameBn,
                        fullLabel = if (isEn) "Copy Full BN Name" else "পুরো বাংলা নাম কপি: ${profile.fullNameBn}",
                        firstTestTag = "copy_first_name_bn_button",
                        lastTestTag = "copy_last_name_bn_button",
                        fullTestTag = "copy_full_name_bn_button",
                        onCopyFirst = { onCopyText("First Name (BN)", profile.firstNameBn) },
                        onCopyLast = { onCopyText("Last Name (BN)", profile.lastNameBn) },
                        onCopyFull = { onCopyText("Full Name (BN)", profile.fullNameBn) }
                    )
                }

                if (scriptMode == NameScriptMode.BOTH || scriptMode == NameScriptMode.ENGLISH) {
                    NamePairRow(
                        badgeLabel = "English Name",
                        firstLabel = "First Name (EN)",
                        firstValue = profile.firstNameEn,
                        lastLabel = "Last Name (EN)",
                        lastValue = profile.lastNameEn,
                        fullLabel = if (isEn) "Copy Full EN Name: ${profile.fullNameEn}" else "পুরো ইংরেজি নাম কপি: ${profile.fullNameEn}",
                        firstTestTag = "copy_first_name_en_button",
                        lastTestTag = "copy_last_name_en_button",
                        fullTestTag = "copy_full_name_en_button",
                        onCopyFirst = { onCopyText("First Name (EN)", profile.firstNameEn) },
                        onCopyLast = { onCopyText("Last Name (EN)", profile.lastNameEn) },
                        onCopyFull = { onCopyText("Full Name (EN)", profile.fullNameEn) }
                    )
                }
            }

            // SECTION 2: Realistic Date of Birth (Age 18 to 35)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
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
                            imageVector = Icons.Default.Cake,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = if (isEn) {
                                    "2. Realistic Date of Birth (18–35 yrs)"
                                } else {
                                    "২. বাস্তবসম্মত জন্ম তারিখ (১৮-৩৫ বছর)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEn) {
                                    "Verified Age: ${profile.dob.ageYears} years (${profile.dob.dayOfWeekEn})"
                                } else {
                                    "বর্তমান বয়স: ${IdentityGeneratorEngine.toBengaliDigits(profile.dob.ageYears)} বছর (${profile.dob.ageYears} yrs • ${profile.dob.dayOfWeekBn})"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(
                            onClick = onRegenerateDobOnly,
                            modifier = Modifier.testTag("regenerate_dob_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isEn) "New Date of Birth" else "নতুন জন্ম তারিখ"
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { onCopyText("Date of Birth", profile.dob.numericSlash) },
                            modifier = Modifier.testTag("copy_dob_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = if (isEn) "Copy DOB" else "জন্ম তারিখ কপি করুন"
                            )
                        }
                    }
                }

                // Separate Day / Month / Year Boxes for easy Meta dropdown selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DobPartBox(
                        label = if (isEn) "Day" else "দিন (Day)",
                        valueMain = String.format("%02d", profile.dob.day),
                        valueSub = IdentityGeneratorEngine.toBengaliDigits(profile.dob.day),
                        modifier = Modifier.weight(1f)
                    )
                    DobPartBox(
                        label = if (isEn) "Month" else "মাস (Month)",
                        valueMain = profile.dob.monthNameEn,
                        valueSub = "${profile.dob.monthNameBn} (${profile.dob.monthNumber})",
                        modifier = Modifier.weight(1.35f)
                    )
                    DobPartBox(
                        label = if (isEn) "Year" else "বছর (Year)",
                        valueMain = profile.dob.year.toString(),
                        valueSub = IdentityGeneratorEngine.toBengaliDigits(profile.dob.year),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${profile.dob.formattedBn}  •  ${profile.dob.numericSlash}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (isEn) "18–35 Verified" else "✓ ১৮-৩৫ শর্ত পূরণ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // SECTION 3: Cryptographically Strong Password (8+ chars, Upper, Lower, Digit, Special)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
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
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Column {
                            Text(
                                text = if (isEn) {
                                    "3. Strong Password (${profile.password.length} chars)"
                                } else {
                                    "৩. স্ট্রং পাসওয়ার্ড (${IdentityGeneratorEngine.toBengaliDigits(profile.password.length)} অক্ষর)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEn) {
                                    "${profile.passwordAnalysis.strengthLabelEn} • ${profile.passwordAnalysis.entropyBits}-bit entropy"
                                } else {
                                    "${profile.passwordAnalysis.strengthLabelBn} • ${profile.passwordAnalysis.entropyBits}-bit এনট্রপি"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onTogglePasswordVisibility,
                            modifier = Modifier.testTag("toggle_password_visibility_button")
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isEn) "Toggle password visibility" else "পাসওয়ার্ড দেখুন বা লুকান"
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onRegeneratePasswordOnly,
                            modifier = Modifier.testTag("regenerate_password_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isEn) "Regenerate Password" else "নতুন পাসওয়ার্ড তৈরি করুন"
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { onCopyText("Password", profile.password) },
                            modifier = Modifier.testTag("copy_password_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = if (isEn) "Copy Password" else "পাসওয়ার্ড কপি করুন"
                            )
                        }
                    }
                }

                // Monospace Password Box
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            MaterialTheme.shapes.small
                        )
                        .clickable { onCopyText("Password", profile.password) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPasswordVisible) {
                                profile.password
                            } else {
                                "•".repeat(profile.password.length)
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("generated_password_text")
                        )
                        Text(
                            text = if (isEn) "TAP TO COPY" else "কপি করুন",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { profile.passwordAnalysis.strengthScore },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // 5-Rule Verification Badges
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PasswordRuleBadge(
                        passed = profile.passwordAnalysis.hasMinLength,
                        text = if (isEn) "8+ Chars" else "৮+ অক্ষর"
                    )
                    PasswordRuleBadge(
                        passed = profile.passwordAnalysis.hasUppercase,
                        text = if (isEn) "A–Z Upper" else "বড় হাতের (A-Z)"
                    )
                    PasswordRuleBadge(
                        passed = profile.passwordAnalysis.hasLowercase,
                        text = if (isEn) "a–z Lower" else "ছোট হাতের (a-z)"
                    )
                    PasswordRuleBadge(
                        passed = profile.passwordAnalysis.hasDigit,
                        text = if (isEn) "0–9 Digit" else "সংখ্যা (0-9)"
                    )
                    PasswordRuleBadge(
                        passed = profile.passwordAnalysis.hasSpecial,
                        text = if (isEn) "!@#$ Symbol" else "স্পেশাল (!@#$)"
                    )
                }
            }

            // SECTION 4: Bound Temp / Custom Email Summary inside the Profile Card
            val effectiveEmail = tempEmailInput.trim()
            Surface(
                color = if (effectiveEmail.isNotEmpty()) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) {
                                "4. Linked Temp / Custom Email"
                            } else {
                                "৪. যুক্তকৃত টেম্পোরারি / কাস্টম ইমেইল"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = effectiveEmail.ifEmpty {
                                if (isEn) "Not entered yet — type above or tap Auto-Fill"
                                else "এখনো দেওয়া হয়নি — উপরে পেস্ট করুন বা 'অটো ইমেইল' চাপুন"
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = if (effectiveEmail.isNotEmpty()) JetBrainsMonoFontFamily else MaterialTheme.typography.bodyLarge.fontFamily
                            ),
                            fontWeight = if (effectiveEmail.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (effectiveEmail.isNotEmpty()) {
                        FilledTonalIconButton(
                            onClick = { onCopyText("Temp Email", effectiveEmail) },
                            modifier = Modifier.testTag("copy_email_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = if (isEn) "Copy Email" else "ইমেইল কপি করুন"
                            )
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onApplySuggestedEmail,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("auto_fill_email_button")
                        ) {
                            Text(if (isEn) "Auto-Fill" else "অটো ইমেইল")
                        }
                    }
                }
            }

            // Optional Account Note before saving to Vault
            OutlinedTextField(
                value = accountNoteInput,
                onValueChange = onAccountNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_note_input"),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                label = {
                    Text(
                        if (isEn) "Optional Note (e.g., Main Page Admin, Ad Account 1)"
                        else "ঐচ্ছিক নোট (যেমন: পেজ এডমিন, বিজনেস একাউন্ট ১)"
                    )
                }
            )

            // Bottom Action Buttons: Copy All + Save to Vault + Generate New
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val fullText = profile.formatFullClipboardText(
                            scriptMode = scriptMode,
                            customEmailOverride = tempEmailInput.trim()
                        )
                        onCopyText(
                            if (isEn) "Full Profile" else "সম্পূর্ণ প্রোফাইল তথ্য",
                            fullText
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("copy_full_profile_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Copy All" else "সব কপি করুন",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onSaveToVault,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("save_to_vault_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Save to Vault" else "ভল্টে সেভ করুন",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Full-width Auto Account Creator Launcher Button
            Button(
                onClick = onLaunchAutoCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag("launch_auto_create_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEn) {
                        "Auto Create Account (Only Enter Email & OTP)"
                    } else {
                        "এই তথ্য দিয়ে অটো একাউন্ট তৈরি করুন (শুধু ইমেইল ও কোড দিন)"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NamePairRow(
    badgeLabel: String,
    firstLabel: String,
    firstValue: String,
    lastLabel: String,
    lastValue: String,
    fullLabel: String,
    firstTestTag: String,
    lastTestTag: String,
    fullTestTag: String,
    onCopyFirst: () -> Unit,
    onCopyLast: () -> Unit,
    onCopyFull: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First Name Box
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                        MaterialTheme.shapes.small
                    )
                    .clickable(onClick = onCopyFirst)
                    .testTag(firstTestTag)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = firstLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = firstValue,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy $firstLabel",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Last Name Box
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                        MaterialTheme.shapes.small
                    )
                    .clickable(onClick = onCopyLast)
                    .testTag(lastTestTag)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lastLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lastValue,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy $lastLabel",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Full Name subtle copy pill
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopyFull)
                .testTag(fullTestTag)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fullLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DobPartBox(
    label: String,
    valueMain: String,
    valueSub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            MaterialTheme.shapes.small
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueMain,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = valueSub,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PasswordRuleBadge(
    passed: Boolean,
    text: String
) {
    Surface(
        color = if (passed) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (passed) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (passed) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}

@Composable
private fun RecentSessionHistorySection(
    isEn: Boolean,
    profiles: List<GeneratedIdentityProfile>,
    currentProfile: GeneratedIdentityProfile,
    onSelectProfile: (GeneratedIdentityProfile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (isEn) "Recent Generated in This Session" else "এই সেশনে তৈরি সাম্প্রতিক নামসমূহ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(profiles) { item ->
                val isSelected =
                    item.firstNameEn == currentProfile.firstNameEn &&
                        item.lastNameEn == currentProfile.lastNameEn &&
                        item.dob.numericSlash == currentProfile.dob.numericSlash

                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .clickable { onSelectProfile(item) }
                        .testTag("session_history_item_${item.firstNameEn.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = item.fullNameBn,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.fullNameEn} • ${item.dob.ageYears}y",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
