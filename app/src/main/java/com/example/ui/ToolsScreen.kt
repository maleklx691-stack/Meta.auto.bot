package com.example.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.domain.IdentityGeneratorEngine
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsScreen(
    uiState: GeneratorUiState,
    onCustomPasswordChange: (String) -> Unit,
    onGenerateBatchPasswords: () -> Unit,
    onToggleChecklistItem: (Int) -> Unit,
    onResetChecklist: () -> Unit,
    onCopyText: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEn = uiState.useEnglishUiLabels
    val analysis = IdentityGeneratorEngine.analyzePassword(uiState.customTestPassword)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("tools_scroll_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Password Analyzer & Tester Card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_analyzer_card"),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = if (isEn) {
                                        "Strong Password Tester & Customizer"
                                    } else {
                                        "স্ট্রং পাসওয়ার্ড পরীক্ষক ও কাস্টমাইজার"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isEn) {
                                        "Verify 8+ chars, Uppercase, Lowercase, Digit & Symbol rules"
                                    } else {
                                        "৮+ অক্ষর, বড় হাতের, ছোট হাতের, সংখ্যা ও স্পেশাল ক্যারেক্টার যাচাই করুন"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.customTestPassword,
                            onValueChange = onCustomPasswordChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_password_test_input"),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            label = {
                                Text(if (isEn) "Test or Edit Password" else "পাসওয়ার্ড লিখে পরীক্ষা করুন")
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onCopyText("Password", uiState.customTestPassword) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Tested Password"
                                    )
                                }
                            }
                        )

                        LinearProgressIndicator(
                            progress = { analysis.strengthScore },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (analysis.meetsAllRules) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isEn) analysis.strengthLabelEn else analysis.strengthLabelBn,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (analysis.meetsAllRules) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = if (isEn) {
                                    "Length: ${analysis.length} | Entropy: ${analysis.entropyBits} bits"
                                } else {
                                    "দৈর্ঘ্য: ${IdentityGeneratorEngine.toBengaliDigits(analysis.length)} | এনট্রপি: ${analysis.entropyBits} bits"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RuleStatusChip(
                                passed = analysis.hasMinLength,
                                label = if (isEn) "8+ Chars (${analysis.length})" else "৮+ অক্ষর (${IdentityGeneratorEngine.toBengaliDigits(analysis.length)})"
                            )
                            RuleStatusChip(
                                passed = analysis.hasUppercase,
                                label = if (isEn) "Uppercase (${analysis.uppercaseCount})" else "বড় হাতের (${IdentityGeneratorEngine.toBengaliDigits(analysis.uppercaseCount)})"
                            )
                            RuleStatusChip(
                                passed = analysis.hasLowercase,
                                label = if (isEn) "Lowercase (${analysis.lowercaseCount})" else "ছোট হাতের (${IdentityGeneratorEngine.toBengaliDigits(analysis.lowercaseCount)})"
                            )
                            RuleStatusChip(
                                passed = analysis.hasDigit,
                                label = if (isEn) "Digits (${analysis.digitCount})" else "সংখ্যা (${IdentityGeneratorEngine.toBengaliDigits(analysis.digitCount)})"
                            )
                            RuleStatusChip(
                                passed = analysis.hasSpecial,
                                label = if (isEn) "Special (${analysis.specialCount})" else "স্পেশাল (${IdentityGeneratorEngine.toBengaliDigits(analysis.specialCount)})"
                            )
                        }
                    }
                }
            }

            // 2. Batch Strong Password Generator Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_passwords_card"),
                    shape = MaterialTheme.shapes.large
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
                                    text = if (isEn) "Batch Strong Passwords" else "একাধিক স্ট্রং পাসওয়ার্ড পুল",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isEn) "Tap any password to copy immediately" else "যেকোনো পাসওয়ার্ডে ট্যাপ করে সাথে সাথে কপি করুন",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onGenerateBatchPasswords,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("refresh_batch_passwords_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEn) "Refresh" else "নতুন সেট")
                            }
                        }

                        uiState.batchPasswords.forEachIndexed { idx, item ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCopyText("Password", item.password) }
                                    .testTag("batch_password_item_$idx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.password,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy password",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Interactive Meta / Facebook Account Creation Checklist
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("meta_checklist_card"),
                    shape = MaterialTheme.shapes.large
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
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isEn) {
                                        "Meta / Facebook Sign-Up Checklist"
                                    } else {
                                        "মেটা / ফেসবুক একাউন্ট খোলার ধাপসমূহ"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = onResetChecklist,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("reset_checklist_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEn) "Reset" else "রিসেট")
                            }
                        }

                        HorizontalDivider()

                        uiState.checklistItems.forEach { step ->
                            Surface(
                                color = if (step.isChecked) {
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleChecklistItem(step.id) }
                                    .testTag("checklist_step_${step.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = step.isChecked,
                                        onCheckedChange = { onToggleChecklistItem(step.id) }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEn) step.titleEn else step.titleBn,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (step.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                        Text(
                                            text = if (isEn) step.subtitleEn else step.subtitleBn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleStatusChip(
    passed: Boolean,
    label: String
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
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
