package com.example

import com.example.domain.IdentityGeneratorEngine
import com.example.domain.NameScriptMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun generatedProfile_meetsAllFourConditions() {
        val customTempEmail = "my.user99@temp-mail.org"
        repeat(25) {
            val profile = IdentityGeneratorEngine.generateUniqueProfile(
                minAge = 18,
                maxAge = 35,
                passwordLength = 12,
                tempEmail = customTempEmail
            )

            // 1. Bengali & English First Name & Last Name
            assertTrue(profile.firstNameBn.isNotBlank())
            assertTrue(profile.lastNameBn.isNotBlank())
            assertTrue(profile.firstNameEn.isNotBlank())
            assertTrue(profile.lastNameEn.isNotBlank())

            // 2. Realistic DOB strictly between 18 and 35 years old
            assertTrue(
                "Expected age 18..35 but got ${profile.dob.ageYears}",
                profile.dob.ageYears in 18..35
            )
            assertTrue(profile.dob.day in 1..31)
            assertTrue(profile.dob.monthNumber in 1..12)

            // 3. Strong Password (min 8 chars, Upper, Lower, Digit, Special)
            assertTrue(profile.password.length >= 8)
            assertTrue(profile.passwordAnalysis.hasMinLength)
            assertTrue(profile.passwordAnalysis.hasUppercase)
            assertTrue(profile.passwordAnalysis.hasLowercase)
            assertTrue(profile.passwordAnalysis.hasDigit)
            assertTrue(profile.passwordAnalysis.hasSpecial)
            assertTrue(profile.passwordAnalysis.meetsAllRules)

            // 4. Temp / Custom Email integration
            assertEquals(customTempEmail, profile.tempEmail)
            val clipboardSummary = profile.formatFullClipboardText(NameScriptMode.BOTH)
            assertTrue(clipboardSummary.contains(customTempEmail))
            assertTrue(clipboardSummary.contains(profile.password))
        }
    }

    @Test
    fun autoCreateHelpers_extractOtpAndBuildAutoFillJs() {
        val extracted = IdentityGeneratorEngine.extractOtpFromRawText("FB-58392 is your Facebook confirmation code")
        assertEquals("58392", extracted)

        val profile = IdentityGeneratorEngine.generateUniqueProfile(tempEmail = "auto@temp-mail.org")
        val js = IdentityGeneratorEngine.buildMetaWebViewAutoFillJs(
            profile = profile,
            email = "auto@temp-mail.org",
            otpCode = "58392",
            preferBengaliName = false,
            triggerSubmitOtp = true
        )
        assertTrue(js.contains(profile.firstNameEn))
        assertTrue(js.contains(profile.lastNameEn))
        assertTrue(js.contains("auto@temp-mail.org"))
        assertTrue(js.contains("58392"))
    }
}
