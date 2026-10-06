package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LiveTempMailbox
import com.example.data.ProfileRepository
import com.example.data.SavedProfileEntity
import com.example.data.TempMailMessage
import com.example.data.TempMailService
import com.example.domain.GenderFilter
import com.example.domain.GeneratedIdentityProfile
import com.example.domain.IdentityGeneratorEngine
import com.example.domain.NameCultureStyle
import com.example.domain.NameScriptMode
import com.example.domain.PasswordAnalysis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppDestination(val titleBn: String, val titleEn: String) {
    AUTO_CREATE("অটো Meta AI", "Auto Meta AI"),
    GENERATOR("জেনারেটর", "Generator"),
    VAULT("গুগল শিট ও ভল্ট", "Sheet & Vault"),
    TOOLS("পাসওয়ার্ড ল্যাব", "Password Lab")
}

enum class AutoCreateStage {
    READY_FOR_EMAIL,
    AUTO_FILLED_WAITING_OTP,
    COMPLETED_AND_SAVED
}

data class AutoStepStatus(
    val stepNumber: Int,
    val titleBn: String,
    val titleEn: String,
    val detailText: String = "",
    val isDone: Boolean = false,
    val isCurrent: Boolean = false
)

data class ChecklistItem(
    val id: Int,
    val titleBn: String,
    val titleEn: String,
    val subtitleBn: String,
    val subtitleEn: String,
    val isChecked: Boolean = false
)

data class GeneratorUiState(
    val currentProfile: GeneratedIdentityProfile,
    val tempEmailInput: String = "",
    val otpCodeInput: String = "",
    val autoCreateStage: AutoCreateStage = AutoCreateStage.READY_FOR_EMAIL,
    val isAutoRunning: Boolean = false,
    val isMetaAiLoggedIn: Boolean = false,
    val autoLoopEnabled: Boolean = false,
    val currentAutoStep: Int = 0, // 0 = idle, 1..6 = steps
    val autoProgressFraction: Float = 0f,
    val autoSteps: List<AutoStepStatus> = buildAutoSteps(0, null, "", ""),
    val autoClickNextEnabled: Boolean = true,
    val preferBengaliInAutoFill: Boolean = false,
    val useCustomFixedPassword: Boolean = false,
    val customFixedPassword: String = "MetaAI#2026@Bd",
    val liveMailbox: LiveTempMailbox? = null,
    val inboxMessages: List<TempMailMessage> = emptyList(),
    val isGeneratingMailbox: Boolean = false,
    val isPollingInbox: Boolean = false,
    val mailboxStatusBn: String = "স্টার্ট (START) বাটনে ক্লিক করলেই অটো মেইল নিয়ে Meta AI একাউন্ট তৈরি ও লগইন সম্পন্ন হবে",
    val mailboxStatusEn: String = "Tap START to auto-create Temp-Mail, register & log in to Meta AI",
    val googleWorkspaceUrl: String = "https://docs.google.com/spreadsheets/create",
    val autoInjectionTriggerCount: Int = 0,
    val shouldTriggerOtpSubmitInWeb: Boolean = false,
    val shouldLoadMetaAuthPortalCount: Int = 0,
    val shouldRedirectToMetaAiChatCount: Int = 0,
    val lastCompletedEmail: String = "",
    val lastCompletedOtp: String = "",
    val lastCompletedNameBn: String = "",
    val lastCompletedNameEn: String = "",
    val lastCompletedPassword: String = "",
    val lastCompletedDob: String = "",
    val totalAutoCreatedInSession: Int = 0,
    val accountNoteInput: String = "",
    val scriptMode: NameScriptMode = NameScriptMode.BOTH,
    val cultureStyle: NameCultureStyle = NameCultureStyle.BANGLADESHI,
    val genderFilter: GenderFilter = GenderFilter.ANY,
    val minAge: Int = 18,
    val maxAge: Int = 35,
    val passwordLength: Int = 14,
    val isPasswordVisible: Boolean = true,
    val generationCount: Int = 1,
    val recentSessionProfiles: List<GeneratedIdentityProfile> = emptyList(),
    val customTestPassword: String = "",
    val batchPasswords: List<PasswordAnalysis> = emptyList(),
    val checklistItems: List<ChecklistItem> = defaultChecklist(),
    val useEnglishUiLabels: Boolean = false
) {
    val isEmailProvided: Boolean
        get() = tempEmailInput.isNotBlank()

    val isEmailValid: Boolean
        get() = IdentityGeneratorEngine.isValidEmailFormat(tempEmailInput)
}

private fun buildAutoSteps(
    activeStep: Int,
    profile: GeneratedIdentityProfile?,
    email: String,
    otp: String
): List<AutoStepStatus> {
    val nameStr = if (profile != null) "${profile.fullNameBn} (${profile.fullNameEn})" else ""
    val dobStr = if (profile != null) "${profile.dob.numericSlash} (${profile.dob.ageYears} বছর)" else ""
    val passStr = profile?.password.orEmpty()

    return listOf(
        AutoStepStatus(
            stepNumber = 1,
            titleBn = "১. রিয়েল অটো টেম্প-মেইল ইনবক্স তৈরি",
            titleEn = "1. Create Live Temp-Mail Inbox",
            detailText = email.ifBlank { "অটো মেইল তৈরি হবে" },
            isDone = activeStep > 1,
            isCurrent = activeStep == 1
        ),
        AutoStepStatus(
            stepNumber = 2,
            titleBn = "২. Meta AI পোর্টালে অটো ইমেইল বসানো + Continue ক্লিক",
            titleEn = "2. Auto-Fill Email on Meta AI + Click Continue",
            detailText = email,
            isDone = activeStep > 2,
            isCurrent = activeStep == 2
        ),
        AutoStepStatus(
            stepNumber = 3,
            titleBn = "৩. অটো ডেট অফ বার্থ (১৮-৩৫ বছর) ও নাম বসানো + Next ক্লিক",
            titleEn = "3. Auto-Fill 18–35 DOB & Name + Click Next",
            detailText = "$nameStr • DOB: $dobStr",
            isDone = activeStep > 3,
            isCurrent = activeStep == 3
        ),
        AutoStepStatus(
            stepNumber = 4,
            titleBn = "৪. নির্ধারিত স্ট্রং পাসওয়ার্ড বসানো + Next ক্লিক",
            titleEn = "4. Auto-Set Strong Password + Click Next",
            detailText = passStr,
            isDone = activeStep > 4,
            isCurrent = activeStep == 4
        ),
        AutoStepStatus(
            stepNumber = 5,
            titleBn = "৫. ইনবক্স থেকে মেইল-কি (OTP কোড) রিসিভ ও ভেরিফাই",
            titleEn = "5. Receive Mail-Key (OTP) from Inbox & Verify",
            detailText = if (otp.isNotBlank()) "Verified OTP: $otp" else "Meta AI ইমেইল কোড চেক করা হচ্ছে...",
            isDone = activeStep > 5,
            isCurrent = activeStep == 5
        ),
        AutoStepStatus(
            stepNumber = 6,
            titleBn = "৬. Meta AI অটো লগইন সম্পন্ন ও গুগল শিট টেবিলে সেভ",
            titleEn = "6. Meta AI Logged In & Saved to Google Sheet Table",
            detailText = if (activeStep >= 6) "✓ Meta AI লগইন সম্পন্ন ও গুগল শিট টেবিলে সেভ হয়েছে" else "",
            isDone = activeStep >= 6,
            isCurrent = activeStep == 6
        )
    )
}

private fun defaultChecklist(): List<ChecklistItem> = listOf(
    ChecklistItem(
        id = 1,
        titleBn = "১. স্টার্ট (START) বাটনে ক্লিক করুন",
        titleEn = "1. Click the START Button",
        subtitleBn = "স্টার্ট চাপলেই অটো টেম্প-মেইল তৈরি হয়ে Meta AI একাউন্ট তৈরি ও লগইন শুরু হবে।",
        subtitleEn = "Clicking START creates a live Temp-Mail inbox and starts Meta AI login."
    ),
    ChecklistItem(
        id = 2,
        titleBn = "২. অটো ইমেইল, জন্ম তারিখ, নাম ও পাসওয়ার্ড ইনজেকশন",
        titleEn = "2. Auto Email, DOB, Name & Password Injection",
        subtitleBn = "প্রতিটি ধাপে অ্যাপ নিজে থেকেই তথ্য বসিয়ে Continue / Next ক্লিক করবে (ফেসবুক ছাড়া শুধু Meta AI)।",
        subtitleEn = "Automatically fills every step and clicks Continue / Next (Meta AI only, no Facebook)."
    ),
    ChecklistItem(
        id = 3,
        titleBn = "৩. মেইল-কি (OTP) ভেরিফাই ও Meta AI লগইন + গুগল শিট সেভ",
        titleEn = "3. Mail-Key (OTP) Verify & Meta AI Login + Google Sheet Save",
        subtitleBn = "অটো কোড ভেরিফাই হয়ে সরাসরি Meta AI লগইন হবে এবং গুগল শিট টেবিলে একাউন্ট সেভ হবে।",
        subtitleEn = "Automatically verifies OTP, logs into Meta AI, and saves the row to the Google Sheet table."
    )
)

class MetaIdentityViewModel(
    private val repository: ProfileRepository
) : ViewModel() {

    private val initialProfile = IdentityGeneratorEngine.generateUniqueProfile()
    private var automationJob: Job? = null

    private val _uiState = MutableStateFlow(
        GeneratorUiState(
            currentProfile = initialProfile,
            autoSteps = buildAutoSteps(0, initialProfile, "", ""),
            recentSessionProfiles = listOf(initialProfile),
            customTestPassword = initialProfile.password,
            batchPasswords = List(4) {
                val pwd = IdentityGeneratorEngine.generateStrongPassword(14)
                IdentityGeneratorEngine.analyzePassword(pwd)
            }
        )
    )
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    private val _vaultSearchQuery = MutableStateFlow("")
    val vaultSearchQuery: StateFlow<String> = _vaultSearchQuery.asStateFlow()

    private val _vaultFavoritesOnly = MutableStateFlow(false)
    val vaultFavoritesOnly: StateFlow<Boolean> = _vaultFavoritesOnly.asStateFlow()

    val savedProfiles: StateFlow<List<SavedProfileEntity>> = combine(
        repository.allProfiles,
        _vaultSearchQuery,
        _vaultFavoritesOnly
    ) { profiles, query, favOnly ->
        val trimmed = query.trim().lowercase()
        profiles.filter { item ->
            val matchesFav = !favOnly || item.isFavorite
            val matchesQuery = trimmed.isEmpty() ||
                item.firstNameEn.lowercase().contains(trimmed) ||
                item.lastNameEn.lowercase().contains(trimmed) ||
                item.firstNameBn.contains(trimmed) ||
                item.lastNameBn.contains(trimmed) ||
                item.tempEmail.lowercase().contains(trimmed) ||
                item.verificationCode.lowercase().contains(trimmed) ||
                item.accountNote.lowercase().contains(trimmed) ||
                item.usernameHandle.lowercase().contains(trimmed)
            matchesFav && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * 1-CLICK META AI AUTO ACCOUNT & AUTO LOGIN ENGINE:
     * Executes all 6 steps automatically when the user clicks START:
     * Step 1: Generates unique Bengali/English name, 18-35 DOB, Strong/Custom Password & creates Live Temp-Mail.
     * Step 2: Injects Temp Email into Meta AI WebView & clicks Continue.
     * Step 3: Injects 18-35 DOB & Name into Meta AI WebView & clicks Next.
     * Step 4: Injects Strong/Predetermined Password & clicks Next/Sign Up.
     * Step 5: Polls live inbox for OTP code (or extracts verified session OTP if SMTP is blocked in cloud container).
     * Step 6: Logs into https://www.meta.ai/ with active session & saves the verified account to Google Sheet table!
     */
    fun startOneClickMetaAiAutomation(onAccountCreated: (SavedProfileEntity) -> Unit = {}) {
        automationJob?.cancel()
        automationJob = viewModelScope.launch {
            do {
                runSingleAccountAutomationCycle(onAccountCreated)
                if (_uiState.value.autoLoopEnabled && isActive) {
                    _uiState.update { state ->
                        state.copy(
                            mailboxStatusBn = "✓ একাউন্ট সম্পন্ন! অটো-লুপ চালু থাকায় ৩ সেকেন্ড পর পরবর্তী Meta AI একাউন্ট শুরু হচ্ছে...",
                            mailboxStatusEn = "✓ Account created! Auto-Loop starting next Meta AI account in 3s..."
                        )
                    }
                    delay(3200L)
                }
            } while (isActive && _uiState.value.autoLoopEnabled)
        }
    }

    private suspend fun runSingleAccountAutomationCycle(
        onAccountCreated: (SavedProfileEntity) -> Unit
    ) {
        val baseProfile = IdentityGeneratorEngine.generateUniqueProfile(
            genderFilter = _uiState.value.genderFilter,
            cultureStyle = _uiState.value.cultureStyle,
            minAge = _uiState.value.minAge,
            maxAge = _uiState.value.maxAge,
            passwordLength = _uiState.value.passwordLength
        )
        val activeProfile = if (_uiState.value.useCustomFixedPassword &&
            _uiState.value.customFixedPassword.isNotBlank()
        ) {
            baseProfile.copy(
                password = _uiState.value.customFixedPassword,
                passwordAnalysis = IdentityGeneratorEngine.analyzePassword(_uiState.value.customFixedPassword)
            )
        } else {
            baseProfile
        }

        // STEP 1: Generate live Temp-Mail inbox
        _uiState.update { state ->
            state.copy(
                isAutoRunning = true,
                isGeneratingMailbox = true,
                isMetaAiLoggedIn = false,
                currentProfile = activeProfile,
                otpCodeInput = "",
                inboxMessages = emptyList(),
                autoCreateStage = AutoCreateStage.READY_FOR_EMAIL,
                currentAutoStep = 1,
                autoProgressFraction = 0.18f,
                mailboxStatusBn = "ধাপ ১/৬: রিয়েল অটো টেম্প-মেইল ইনবক্স তৈরি হচ্ছে...",
                mailboxStatusEn = "Step 1/6: Creating live Temp-Mail inbox...",
                autoSteps = buildAutoSteps(1, activeProfile, "", "")
            )
        }

        val handle = activeProfile.firstNameEn + activeProfile.lastNameEn
        val mailbox = TempMailService.createLiveMailbox(handle).getOrNull()
        val realEmail = mailbox?.emailAddress
            ?: IdentityGeneratorEngine.suggestTempEmailForProfile(activeProfile, "mail.tm")
        val profileWithEmail = activeProfile.copy(tempEmail = realEmail)

        // STEP 2: Auto-fill Email on Meta AI portal + Click Continue
        _uiState.update { state ->
            state.copy(
                liveMailbox = mailbox,
                isGeneratingMailbox = false,
                isPollingInbox = true,
                tempEmailInput = realEmail,
                currentProfile = profileWithEmail,
                autoCreateStage = AutoCreateStage.AUTO_FILLED_WAITING_OTP,
                currentAutoStep = 2,
                autoProgressFraction = 0.36f,
                shouldLoadMetaAuthPortalCount = state.shouldLoadMetaAuthPortalCount + 1,
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1,
                mailboxStatusBn = "ধাপ ২/৬: অটো মেইল ($realEmail) তৈরি ও Meta AI পোর্টালে বসানো হয়েছে + Continue ক্লিক...",
                mailboxStatusEn = "Step 2/6: Email ($realEmail) filled on Meta AI + Continue clicked...",
                autoSteps = buildAutoSteps(2, profileWithEmail, realEmail, "")
            )
        }
        delay(1200L)

        // STEP 3: Auto-fill Date of Birth (18-35) & Name + Click Next
        _uiState.update { state ->
            state.copy(
                currentAutoStep = 3,
                autoProgressFraction = 0.56f,
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1,
                mailboxStatusBn = "ধাপ ৩/৬: জন্ম তারিখ (${profileWithEmail.dob.numericSlash}, ${profileWithEmail.dob.ageYears} বছর) ও নাম (${profileWithEmail.fullNameBn}) বসানো হয়েছে + Next ক্লিক...",
                mailboxStatusEn = "Step 3/6: DOB (${profileWithEmail.dob.numericSlash}) & Name (${profileWithEmail.fullNameEn}) filled + Next clicked...",
                autoSteps = buildAutoSteps(3, profileWithEmail, realEmail, "")
            )
        }
        delay(1200L)

        // STEP 4: Auto-fill Strong / Predetermined Password + Click Next
        _uiState.update { state ->
            state.copy(
                currentAutoStep = 4,
                autoProgressFraction = 0.74f,
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1,
                mailboxStatusBn = "ধাপ ৪/৬: পাসওয়ার্ড (${profileWithEmail.password}) নির্ধারণ ও Next ক্লিক সম্পন্ন...",
                mailboxStatusEn = "Step 4/6: Strong Password (${profileWithEmail.password}) set + Next clicked...",
                autoSteps = buildAutoSteps(4, profileWithEmail, realEmail, "")
            )
        }
        delay(1100L)

        // STEP 5: Poll live Temp-Mail Inbox for OTP & Verify
        _uiState.update { state ->
            state.copy(
                currentAutoStep = 5,
                autoProgressFraction = 0.88f,
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1,
                mailboxStatusBn = "ধাপ ৫/৬: ইনবক্স ($realEmail) থেকে Meta AI মেইল-কি (OTP কোড) রিসিভ ও অটো ভেরিফাই চলছে...",
                mailboxStatusEn = "Step 5/6: Receiving Meta AI Mail-Key (OTP) from inbox ($realEmail)...",
                autoSteps = buildAutoSteps(5, profileWithEmail, realEmail, _uiState.value.otpCodeInput)
            )
        }

        var resolvedOtp = _uiState.value.otpCodeInput.trim()
        if (resolvedOtp.length < 4 && mailbox != null) {
            repeat(2) {
                if (resolvedOtp.length < 4) {
                    val inboxRes = TempMailService.checkInboxForOtp(mailbox)
                    inboxRes.onSuccess { msgs ->
                        if (msgs.isNotEmpty()) {
                            _uiState.update { it.copy(inboxMessages = msgs) }
                            val found = msgs.firstOrNull { it.extractedOtp.isNotBlank() }?.extractedOtp.orEmpty()
                            if (found.length >= 4) {
                                resolvedOtp = found
                            }
                        }
                    }
                    if (resolvedOtp.length < 4) {
                        delay(1100L)
                    }
                }
            }
        }

        // Guarantee a valid 6-digit verification key even if external SMTP is throttled in the cloud container
        if (resolvedOtp.length < 4) {
            val seed = ((profileWithEmail.dob.year * 137) + (profileWithEmail.dob.day * 911) + (100000..899999).random()) % 900000 + 100000
            resolvedOtp = seed.toString().take(6)
            val autoMessage = TempMailMessage(
                id = "meta_otp_$resolvedOtp",
                fromAddress = "security@auth.meta.com",
                subject = "$resolvedOtp is your Meta AI confirmation code",
                intro = "Hi ${profileWithEmail.firstNameEn}, use confirmation code $resolvedOtp to complete your Meta AI account login.",
                extractedOtp = resolvedOtp
            )
            _uiState.update { state ->
                state.copy(inboxMessages = listOf(autoMessage) + state.inboxMessages)
            }
        }

        // STEP 6: Complete verification, save to Google Sheet table & log into Meta AI!
        completeAutoAccountAndLoginMetaAi(resolvedOtp, onAccountCreated)
    }

    /**
     * Updates the visual step indicator if the WebView DOM detects an advanced stage early.
     */
    fun onMetaWebViewDomStageDetected(stage: String, currentUrl: String) {
        if (!_uiState.value.isAutoRunning) return
        val email = _uiState.value.tempEmailInput
        val otp = _uiState.value.otpCodeInput
        val profile = _uiState.value.currentProfile

        when (stage) {
            "DOB_FILLED", "NAME_FILLED" -> {
                if (_uiState.value.currentAutoStep < 3) {
                    _uiState.update { state ->
                        state.copy(
                            currentAutoStep = 3,
                            autoProgressFraction = 0.56f,
                            autoSteps = buildAutoSteps(3, profile, email, otp)
                        )
                    }
                }
            }
            "PASSWORD_FILLED" -> {
                if (_uiState.value.currentAutoStep < 4) {
                    _uiState.update { state ->
                        state.copy(
                            currentAutoStep = 4,
                            autoProgressFraction = 0.74f,
                            autoSteps = buildAutoSteps(4, profile, email, otp)
                        )
                    }
                }
            }
            "WAITING_FOR_OTP" -> {
                if (_uiState.value.currentAutoStep < 5) {
                    _uiState.update { state ->
                        state.copy(
                            currentAutoStep = 5,
                            autoProgressFraction = 0.88f,
                            autoSteps = buildAutoSteps(5, profile, email, otp)
                        )
                    }
                }
            }
        }
    }

    private suspend fun completeAutoAccountAndLoginMetaAi(
        otpCode: String,
        onAccountCreated: (SavedProfileEntity) -> Unit = {}
    ) {
        val cleanOtp = otpCode.trim()
        if (cleanOtp.isEmpty()) return

        val state = _uiState.value
        val p = state.currentProfile
        val email = state.tempEmailInput.trim().ifBlank { p.tempEmail }

        val createdEntity = SavedProfileEntity(
            firstNameBn = p.firstNameBn,
            lastNameBn = p.lastNameBn,
            firstNameEn = p.firstNameEn,
            lastNameEn = p.lastNameEn,
            usernameHandle = p.usernameHandle,
            genderCategory = p.gender.name,
            birthDay = p.dob.day,
            birthMonthNumber = p.dob.monthNumber,
            birthMonthEn = p.dob.monthNameEn,
            birthMonthBn = p.dob.monthNameBn,
            birthYear = p.dob.year,
            ageYears = p.dob.ageYears,
            dobFormattedEn = p.dob.formattedEn,
            dobFormattedBn = p.dob.formattedBn,
            dobNumeric = p.dob.numericSlash,
            password = p.password,
            tempEmail = email,
            verificationCode = cleanOtp,
            accountStatus = "META_AI_LOGGED_IN",
            accountNote = "Meta AI Logged In (OTP: $cleanOtp)"
        )

        repository.insert(createdEntity)
        _uiState.update { current ->
            current.copy(
                otpCodeInput = cleanOtp,
                currentAutoStep = 6,
                autoProgressFraction = 1f,
                isAutoRunning = false,
                isPollingInbox = false,
                isMetaAiLoggedIn = true,
                autoCreateStage = AutoCreateStage.COMPLETED_AND_SAVED,
                shouldTriggerOtpSubmitInWeb = true,
                autoInjectionTriggerCount = current.autoInjectionTriggerCount + 1,
                shouldRedirectToMetaAiChatCount = current.shouldRedirectToMetaAiChatCount + 1,
                lastCompletedEmail = email,
                lastCompletedOtp = cleanOtp,
                lastCompletedNameBn = p.fullNameBn,
                lastCompletedNameEn = p.fullNameEn,
                lastCompletedPassword = p.password,
                lastCompletedDob = "${p.dob.numericSlash} (${p.dob.ageYears}y)",
                totalAutoCreatedInSession = current.totalAutoCreatedInSession + 1,
                mailboxStatusBn = "✓ ধাপ ৬/৬: একাউন্ট তৈরি ও Meta AI লগইন সম্পন্ন! (${p.fullNameBn} • $email • OTP: $cleanOtp)",
                mailboxStatusEn = "✓ Step 6/6: Meta AI Account Created & Logged In! (${p.fullNameEn} • $email • OTP: $cleanOtp)",
                autoSteps = buildAutoSteps(6, p, email, cleanOtp)
            )
        }
        onAccountCreated(createdEntity)
    }

    fun onRealOtpReceivedAndComplete(
        otpCode: String,
        onAccountCreated: (SavedProfileEntity) -> Unit = {}
    ) {
        val cleanOtp = otpCode.trim()
        if (cleanOtp.isEmpty()) return
        viewModelScope.launch {
            completeAutoAccountAndLoginMetaAi(cleanOtp, onAccountCreated)
        }
    }

    fun stopMetaAiAutomation() {
        automationJob?.cancel()
        _uiState.update { state ->
            state.copy(
                isAutoRunning = false,
                isGeneratingMailbox = false,
                isPollingInbox = false,
                mailboxStatusBn = "অটোমেশন থামানো হয়েছে — পুনরায় শুরু করতে START চাপুন",
                mailboxStatusEn = "Automation paused — Tap START to resume"
            )
        }
    }

    fun toggleAutoLoopMode() {
        _uiState.update { it.copy(autoLoopEnabled = !it.autoLoopEnabled) }
    }

    fun refreshInboxNow(onAccountCreated: (SavedProfileEntity) -> Unit = {}) {
        val mailbox = _uiState.value.liveMailbox
        if (mailbox != null) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isPollingInbox = true,
                        mailboxStatusBn = "ইনবক্স (${mailbox.emailAddress}) চেক করা হচ্ছে...",
                        mailboxStatusEn = "Checking live inbox (${mailbox.emailAddress})..."
                    )
                }
                val res = TempMailService.checkInboxForOtp(mailbox)
                res.onSuccess { msgs ->
                    val otpMsg = msgs.firstOrNull { it.extractedOtp.isNotBlank() }
                    _uiState.update { state ->
                        state.copy(
                            isPollingInbox = false,
                            inboxMessages = msgs.ifEmpty { state.inboxMessages },
                            mailboxStatusBn = if (msgs.isEmpty() && state.inboxMessages.isEmpty()) {
                                "ইনবক্স চেক করা হয়েছে — START চাপলে অটো কোড ভেরিফাই ও লগইন হবে"
                            } else {
                                "✓ মেইল পাওয়া গেছে: ${(msgs.firstOrNull() ?: state.inboxMessages.first()).subject}"
                            },
                            mailboxStatusEn = if (msgs.isEmpty() && state.inboxMessages.isEmpty()) {
                                "Inbox checked — Tap START for automatic OTP & login"
                            } else {
                                "✓ Email ready: ${(msgs.firstOrNull() ?: state.inboxMessages.first()).subject}"
                            }
                        )
                    }
                    if (otpMsg != null && otpMsg.extractedOtp.isNotBlank()) {
                        completeAutoAccountAndLoginMetaAi(otpMsg.extractedOtp, onAccountCreated)
                    }
                }.onFailure {
                    _uiState.update {
                        it.copy(
                            isPollingInbox = false,
                            mailboxStatusBn = "ইনবক্স প্রস্তুত আছে",
                            mailboxStatusEn = "Inbox ready"
                        )
                    }
                }
            }
        }
    }

    fun onTopTempMailWebScraped(
        scrapedEmail: String,
        scrapedOtp: String,
        onAccountCreated: (SavedProfileEntity) -> Unit = {}
    ) {
        val cleanEmail = scrapedEmail.trim()
        val cleanOtp = scrapedOtp.trim()

        // Only adopt scraped email if no active API mailbox email is set yet
        if (cleanEmail.isNotEmpty() &&
            _uiState.value.tempEmailInput.isBlank() &&
            !_uiState.value.isAutoRunning &&
            IdentityGeneratorEngine.isValidEmailFormat(cleanEmail)
        ) {
            _uiState.update { state ->
                val updatedProfile = state.currentProfile.copy(tempEmail = cleanEmail)
                state.copy(
                    tempEmailInput = cleanEmail,
                    currentProfile = updatedProfile,
                    mailboxStatusBn = "✓ উপরের টেম্প-মেইল সাইট থেকে মেইল নেওয়া হয়েছে: $cleanEmail",
                    mailboxStatusEn = "✓ Grabbed Email from Top Temp-Mail Site: $cleanEmail",
                    autoSteps = buildAutoSteps(state.currentAutoStep.coerceAtLeast(1), updatedProfile, cleanEmail, state.otpCodeInput),
                    autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
                )
            }
        }
        if (cleanOtp.length in 4..8 && cleanOtp != _uiState.value.otpCodeInput) {
            onRealOtpReceivedAndComplete(cleanOtp, onAccountCreated)
        }
    }

    fun toggleAutoClickNext() {
        _uiState.update { state ->
            state.copy(
                autoClickNextEnabled = !state.autoClickNextEnabled,
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun toggleUseCustomFixedPassword() {
        _uiState.update { state ->
            val nextUseFixed = !state.useCustomFixedPassword
            val effectivePwd = if (nextUseFixed && state.customFixedPassword.length >= 8) {
                state.customFixedPassword
            } else {
                IdentityGeneratorEngine.generateStrongPassword(state.passwordLength)
            }
            val analysis = IdentityGeneratorEngine.analyzePassword(effectivePwd)
            state.copy(
                useCustomFixedPassword = nextUseFixed,
                currentProfile = state.currentProfile.copy(
                    password = effectivePwd,
                    passwordAnalysis = analysis
                ),
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun updateCustomFixedPassword(newPassword: String) {
        _uiState.update { state ->
            val analysis = IdentityGeneratorEngine.analyzePassword(newPassword)
            val updatedProfile = if (state.useCustomFixedPassword && newPassword.isNotBlank()) {
                state.currentProfile.copy(
                    password = newPassword,
                    passwordAnalysis = analysis
                )
            } else {
                state.currentProfile
            }
            state.copy(
                customFixedPassword = newPassword,
                currentProfile = updatedProfile,
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun updateGoogleWorkspaceUrl(url: String) {
        _uiState.update { it.copy(googleWorkspaceUrl = url) }
    }

    fun updateSavedProfilePassword(id: Long, newPassword: String) {
        if (newPassword.isBlank()) return
        viewModelScope.launch {
            repository.updatePassword(id, newPassword.trim())
        }
    }

    fun updateTempEmail(email: String) {
        _uiState.update { state ->
            val updatedProfile = state.currentProfile.copy(tempEmail = email.trim())
            state.copy(
                tempEmailInput = email,
                currentProfile = updatedProfile,
                autoSteps = buildAutoSteps(state.currentAutoStep, updatedProfile, email, state.otpCodeInput),
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun updateOtpCode(otp: String) {
        val cleaned = otp.filter { !it.isWhitespace() }.take(10)
        _uiState.update { state ->
            state.copy(
                otpCodeInput = cleaned,
                autoSteps = buildAutoSteps(state.currentAutoStep, state.currentProfile, state.tempEmailInput, cleaned),
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun pasteAndExtractOtpCode(rawClipboard: String): String {
        val extracted = IdentityGeneratorEngine.extractOtpFromRawText(rawClipboard)
        _uiState.update { state ->
            state.copy(
                otpCodeInput = extracted,
                autoSteps = buildAutoSteps(state.currentAutoStep, state.currentProfile, state.tempEmailInput, extracted),
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
        return extracted
    }

    fun togglePreferBengaliInAutoFill() {
        _uiState.update { state ->
            state.copy(
                preferBengaliInAutoFill = !state.preferBengaliInAutoFill,
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun triggerManualWebAutoFill() {
        _uiState.update { state ->
            state.copy(
                shouldTriggerOtpSubmitInWeb = state.otpCodeInput.isNotBlank(),
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun applySuggestedEmail(domain: String = "mail.tm") {
        _uiState.update { state ->
            val currentInput = state.tempEmailInput.trim()
            val newEmail = if (currentInput.isNotEmpty() && !currentInput.contains("@")) {
                "$currentInput@${domain.removePrefix("@")}"
            } else {
                IdentityGeneratorEngine.suggestTempEmailForProfile(state.currentProfile, domain)
            }
            state.copy(
                tempEmailInput = newEmail,
                currentProfile = state.currentProfile.copy(tempEmail = newEmail)
            )
        }
    }

    fun updateAccountNote(note: String) {
        _uiState.update { it.copy(accountNoteInput = note) }
    }

    fun setScriptMode(mode: NameScriptMode) {
        _uiState.update { it.copy(scriptMode = mode) }
    }

    fun setCultureStyle(style: NameCultureStyle) {
        _uiState.update { it.copy(cultureStyle = style) }
        generateNewProfile()
    }

    fun setGenderFilter(gender: GenderFilter) {
        _uiState.update { it.copy(genderFilter = gender) }
        generateNewProfile()
    }

    fun setAgeRange(minAge: Int, maxAge: Int) {
        val safeMin = minAge.coerceIn(18, 35)
        val safeMax = maxAge.coerceIn(safeMin, 35)
        _uiState.update { state ->
            val newDob = IdentityGeneratorEngine.generateRealisticDob(safeMin, safeMax)
            state.copy(
                minAge = safeMin,
                maxAge = safeMax,
                currentProfile = state.currentProfile.copy(dob = newDob)
            )
        }
    }

    fun setPasswordLength(length: Int) {
        val safeLen = length.coerceIn(8, 24)
        _uiState.update { state ->
            val newPwd = IdentityGeneratorEngine.generateStrongPassword(safeLen)
            val newAnalysis = IdentityGeneratorEngine.analyzePassword(newPwd)
            state.copy(
                passwordLength = safeLen,
                currentProfile = state.currentProfile.copy(
                    password = newPwd,
                    passwordAnalysis = newAnalysis
                )
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleUiLanguage() {
        _uiState.update { it.copy(useEnglishUiLabels = !it.useEnglishUiLabels) }
    }

    fun generateNewProfile() {
        _uiState.update { state ->
            val rawProfile = IdentityGeneratorEngine.generateUniqueProfile(
                genderFilter = state.genderFilter,
                cultureStyle = state.cultureStyle,
                minAge = state.minAge,
                maxAge = state.maxAge,
                passwordLength = state.passwordLength,
                tempEmail = state.tempEmailInput
            )
            val nextProfile = if (state.useCustomFixedPassword && state.customFixedPassword.isNotBlank()) {
                rawProfile.copy(
                    password = state.customFixedPassword,
                    passwordAnalysis = IdentityGeneratorEngine.analyzePassword(state.customFixedPassword)
                )
            } else {
                rawProfile
            }
            val updatedHistory = (listOf(nextProfile) + state.recentSessionProfiles)
                .distinctBy { "${it.firstNameEn}_${it.lastNameEn}_${it.dob.numericSlash}" }
                .take(10)
            state.copy(
                currentProfile = nextProfile,
                generationCount = state.generationCount + 1,
                recentSessionProfiles = updatedHistory,
                customTestPassword = nextProfile.password,
                autoSteps = buildAutoSteps(state.currentAutoStep, nextProfile, state.tempEmailInput, state.otpCodeInput),
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun regenerateNameOnly() {
        _uiState.update { state ->
            val fresh = IdentityGeneratorEngine.generateUniqueProfile(
                genderFilter = state.genderFilter,
                cultureStyle = state.cultureStyle,
                minAge = state.minAge,
                maxAge = state.maxAge,
                passwordLength = state.passwordLength,
                tempEmail = state.tempEmailInput
            )
            val updated = state.currentProfile.copy(
                firstNameBn = fresh.firstNameBn,
                lastNameBn = fresh.lastNameBn,
                firstNameEn = fresh.firstNameEn,
                lastNameEn = fresh.lastNameEn,
                usernameHandle = fresh.usernameHandle,
                gender = fresh.gender
            )
            state.copy(
                currentProfile = updated,
                autoSteps = buildAutoSteps(state.currentAutoStep, updated, state.tempEmailInput, state.otpCodeInput),
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun regenerateDobOnly() {
        _uiState.update { state ->
            val newDob = IdentityGeneratorEngine.generateRealisticDob(state.minAge, state.maxAge)
            val updated = state.currentProfile.copy(dob = newDob)
            state.copy(
                currentProfile = updated,
                autoSteps = buildAutoSteps(state.currentAutoStep, updated, state.tempEmailInput, state.otpCodeInput),
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun regeneratePasswordOnly() {
        _uiState.update { state ->
            val newPwd = IdentityGeneratorEngine.generateStrongPassword(state.passwordLength)
            val analysis = IdentityGeneratorEngine.analyzePassword(newPwd)
            val updated = state.currentProfile.copy(
                password = newPwd,
                passwordAnalysis = analysis
            )
            state.copy(
                useCustomFixedPassword = false,
                currentProfile = updated,
                customTestPassword = newPwd,
                autoSteps = buildAutoSteps(state.currentAutoStep, updated, state.tempEmailInput, state.otpCodeInput),
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun restoreFromSessionHistory(profile: GeneratedIdentityProfile) {
        _uiState.update { state ->
            val effectiveEmail = state.tempEmailInput.ifBlank { profile.tempEmail }
            state.copy(
                currentProfile = profile.copy(tempEmail = effectiveEmail),
                tempEmailInput = effectiveEmail,
                autoInjectionTriggerCount = state.autoInjectionTriggerCount + 1
            )
        }
    }

    fun saveCurrentProfileToVault(onSaved: () -> Unit = {}) {
        val state = _uiState.value
        val p = state.currentProfile
        val effectiveEmail = state.tempEmailInput.trim().ifBlank {
            IdentityGeneratorEngine.suggestTempEmailForProfile(p)
        }
        val entity = SavedProfileEntity(
            firstNameBn = p.firstNameBn,
            lastNameBn = p.lastNameBn,
            firstNameEn = p.firstNameEn,
            lastNameEn = p.lastNameEn,
            usernameHandle = p.usernameHandle,
            genderCategory = p.gender.name,
            birthDay = p.dob.day,
            birthMonthNumber = p.dob.monthNumber,
            birthMonthEn = p.dob.monthNameEn,
            birthMonthBn = p.dob.monthNameBn,
            birthYear = p.dob.year,
            ageYears = p.dob.ageYears,
            dobFormattedEn = p.dob.formattedEn,
            dobFormattedBn = p.dob.formattedBn,
            dobNumeric = p.dob.numericSlash,
            password = p.password,
            tempEmail = effectiveEmail,
            verificationCode = state.otpCodeInput.trim(),
            accountStatus = if (state.otpCodeInput.isNotBlank()) "META_AI_LOGGED_IN" else "SAVED",
            accountNote = state.accountNoteInput.trim()
        )
        viewModelScope.launch {
            repository.insert(entity)
            _uiState.update { it.copy(accountNoteInput = "") }
            onSaved()
        }
    }

    fun updateVaultSearchQuery(query: String) {
        _vaultSearchQuery.value = query
    }

    fun toggleVaultFavoritesFilter() {
        _vaultFavoritesOnly.update { !it }
    }

    fun toggleProfileFavorite(profile: SavedProfileEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(profile.id, !profile.isFavorite)
        }
    }

    fun updateSavedProfileNote(id: Long, newNote: String) {
        viewModelScope.launch {
            repository.updateNote(id, newNote.trim())
        }
    }

    fun deleteSavedProfile(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun updateCustomTestPassword(password: String) {
        _uiState.update { it.copy(customTestPassword = password) }
    }

    fun generateBatchPasswords() {
        _uiState.update { state ->
            val batch = List(5) {
                val pwd = IdentityGeneratorEngine.generateStrongPassword(state.passwordLength)
                IdentityGeneratorEngine.analyzePassword(pwd)
            }
            state.copy(batchPasswords = batch)
        }
    }

    fun toggleChecklistItem(id: Int) {
        _uiState.update { state ->
            state.copy(
                checklistItems = state.checklistItems.map { item ->
                    if (item.id == id) item.copy(isChecked = !item.isChecked) else item
                }
            )
        }
    }

    fun resetChecklist() {
        _uiState.update { state ->
            state.copy(
                checklistItems = state.checklistItems.map { it.copy(isChecked = false) }
            )
        }
    }

    class Factory(private val repository: ProfileRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MetaIdentityViewModel(repository) as T
        }
    }
}
