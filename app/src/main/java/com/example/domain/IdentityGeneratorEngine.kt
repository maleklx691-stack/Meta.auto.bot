package com.example.domain

import java.security.SecureRandom
import java.util.Calendar
import java.util.Locale
import kotlin.math.log2

enum class NameScriptMode(val labelBn: String, val labelEn: String) {
    BOTH("বাংলা ও English", "Both (BN + EN)"),
    BENGALI("শুধু বাংলা", "Bengali Only"),
    ENGLISH("শুধু English", "English Only")
}

enum class NameCultureStyle(val labelBn: String, val labelEn: String) {
    BANGLADESHI("বাংলাদেশি / বাঙালি", "Bangladeshi / Bengali"),
    INTERNATIONAL("ইন্টারন্যাশনাল (Global)", "International English")
}

enum class GenderFilter(val labelBn: String, val labelEn: String) {
    ANY("যেকোনো", "Any"),
    MALE("ছেলে (Male)", "Male"),
    FEMALE("মেয়ে (Female)", "Female")
}

data class NameEntry(
    val bn: String,
    val en: String,
    val gender: GenderFilter,
    val style: NameCultureStyle
)

data class SurnameEntry(
    val bn: String,
    val en: String,
    val style: NameCultureStyle
)

data class GeneratedDob(
    val day: Int,
    val monthNumber: Int, // 1..12
    val monthNameEn: String,
    val monthNameBn: String,
    val year: Int,
    val ageYears: Int,
    val formattedEn: String,
    val formattedBn: String,
    val numericSlash: String,
    val dayOfWeekBn: String,
    val dayOfWeekEn: String
)

data class PasswordAnalysis(
    val password: String,
    val length: Int,
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecial: Boolean,
    val uppercaseCount: Int,
    val lowercaseCount: Int,
    val digitCount: Int,
    val specialCount: Int,
    val entropyBits: Int,
    val strengthScore: Float, // 0f..1f
    val strengthLabelBn: String,
    val strengthLabelEn: String
) {
    val meetsAllRules: Boolean
        get() = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecial
}

data class GeneratedIdentityProfile(
    val firstNameBn: String,
    val lastNameBn: String,
    val firstNameEn: String,
    val lastNameEn: String,
    val usernameHandle: String,
    val gender: GenderFilter,
    val cultureStyle: NameCultureStyle,
    val dob: GeneratedDob,
    val password: String,
    val passwordAnalysis: PasswordAnalysis,
    val tempEmail: String,
    val generatedAtMillis: Long = System.currentTimeMillis()
) {
    val fullNameBn: String get() = "$firstNameBn $lastNameBn"
    val fullNameEn: String get() = "$firstNameEn $lastNameEn"

    fun formatFullClipboardText(
        scriptMode: NameScriptMode,
        customEmailOverride: String = tempEmail
    ): String {
        val effectiveEmail = customEmailOverride.ifBlank { "প্রদান করা হয়নি (Not set)" }
        val nameSection = when (scriptMode) {
            NameScriptMode.BENGALI ->
                "• First Name (বাংলা): $firstNameBn\n• Last Name (বাংলা): $lastNameBn\n• পুরো নাম: $fullNameBn"
            NameScriptMode.ENGLISH ->
                "• First Name: $firstNameEn\n• Last Name: $lastNameEn\n• Full Name: $fullNameEn"
            NameScriptMode.BOTH ->
                "• First Name: $firstNameBn ($firstNameEn)\n• Last Name: $lastNameBn ($lastNameEn)\n• Full Name: $fullNameBn / $fullNameEn"
        }
        return buildString {
            appendLine("=== Meta AI Account Info ===")
            appendLine(nameSection)
            appendLine("• Username Handle: @$usernameHandle")
            appendLine("• Date of Birth: ${dob.formattedEn} (${dob.formattedBn})")
            appendLine("• DOB (DD/MM/YYYY): ${dob.numericSlash} | বয়স (Age): ${dob.ageYears} বছর")
            appendLine("• Temp Email: $effectiveEmail")
            appendLine("• Strong Password: $password")
        }.trim()
    }
}

object IdentityGeneratorEngine {
    private val secureRandom = SecureRandom()
    private val recentSignatures = ArrayDeque<String>(64)

    private const val UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGIT_CHARS = "0123456789"
    private const val SPECIAL_CHARS = "!@#$%^&*_-+=?"
    private const val ALL_PASSWORD_CHARS =
        UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS

    private val bengaliFirstNames = listOf(
        // Male Bangladeshi/Bengali First Names
        NameEntry("ফারহান", "Farhan", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("তানভীর", "Tanvir", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("সাকিব", "Sakib", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("আরিফ", "Arif", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("মাহমুদ", "Mahmud", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("রাফি", "Rafi", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("নাহিদ", "Nahid", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("জাহিদ", "Jahid", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("আদনান", "Adnan", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("ইমরান", "Imran", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("সাইফুল", "Saiful", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("রাকিব", "Rakib", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("আশিক", "Ashik", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("মেহেদী", "Mehedi", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("তৌহিদ", "Touhid", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("সৌরভ", "Sourav", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("অয়ন", "Ayon", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("রায়হান", "Rayhan", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("নাফিস", "Nafis", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("শহীদুল", "Shahidul", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("ফাহিম", "Fahim", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("সাদমান", "Sadman", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("ইশতিয়াক", "Ishtiaq", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("তাসনিমুল", "Tasnimul", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("অর্ণব", "Arnab", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("প্রান্ত", "Pranto", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("জুনায়েদ", "Junaid", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),
        NameEntry("রেদোয়ান", "Redwan", GenderFilter.MALE, NameCultureStyle.BANGLADESHI),

        // Female Bangladeshi/Bengali First Names
        NameEntry("নুসরাত", "Nusrat", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("তাসনিম", "Tasnim", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("সাদিয়া", "Sadia", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("মেহজাবিন", "Mehjabin", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("ফারজানা", "Farzana", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("তানজিনা", "Tanjina", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("সুমাইয়া", "Sumaiya", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("আফরিন", "Afrin", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("জান্নাতুল", "Jannatul", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("নাবিলা", "Nabila", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("রুবাইয়া", "Rubaiya", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("শারমিন", "Sharmin", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("ইসরাত", "Israt", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("তাহমিনা", "Tahmina", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("সানজিদা", "Sanjida", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("অনামিকা", "Anamika", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("মৌমিতা", "Moumita", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("লামিয়া", "Lamia", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("নাফিসা", "Nafisa", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("হুমায়রা", "Humaira", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("মালিহা", "Maliha", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("তাবাসসুম", "Tabassum", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("সামিয়া", "Samia", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("রাইসা", "Raisa", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("অর্পিতা", "Arpita", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("প্রিয়াঙ্কা", "Priyanka", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("ফারিহা", "Fariha", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI),
        NameEntry("আনিকা", "Anika", GenderFilter.FEMALE, NameCultureStyle.BANGLADESHI)
    )

    private val internationalFirstNames = listOf(
        NameEntry("লিয়াম", "Liam", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("নোয়াহ", "Noah", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("ইথান", "Ethan", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("লুকাস", "Lucas", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("ম্যাসন", "Mason", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("লোগান", "Logan", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("ক্যালেব", "Caleb", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("রায়ান", "Ryan", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("নাথান", "Nathan", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("অ্যাড্রিয়ান", "Adrian", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("ওলিভার", "Oliver", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("হেনরি", "Henry", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("সেবাস্টিয়ান", "Sebastian", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("জুলিয়ান", "Julian", GenderFilter.MALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("অলিভিয়া", "Olivia", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("সোফিয়া", "Sophia", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("অ্যামেলিয়া", "Amelia", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("ইসাবেলা", "Isabella", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("মিয়া", "Mia", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("শার্লট", "Charlotte", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("হার্পার", "Harper", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("ইভলিন", "Evelyn", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("ক্লোয়ি", "Chloe", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("গ্রেস", "Grace", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("হান্না", "Hannah", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("নোরাহ", "Nora", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("লিলি", "Lily", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL),
        NameEntry("স্টেলা", "Stella", GenderFilter.FEMALE, NameCultureStyle.INTERNATIONAL)
    )

    private val bengaliSurnames = listOf(
        SurnameEntry("আহমেদ", "Ahmed", NameCultureStyle.BANGLADESHI),
        SurnameEntry("রহমান", "Rahman", NameCultureStyle.BANGLADESHI),
        SurnameEntry("চৌধুরী", "Chowdhury", NameCultureStyle.BANGLADESHI),
        SurnameEntry("হাসান", "Hasan", NameCultureStyle.BANGLADESHI),
        SurnameEntry("হোসেন", "Hossain", NameCultureStyle.BANGLADESHI),
        SurnameEntry("খান", "Khan", NameCultureStyle.BANGLADESHI),
        SurnameEntry("ইসলাম", "Islam", NameCultureStyle.BANGLADESHI),
        SurnameEntry("মাহমুদ", "Mahmud", NameCultureStyle.BANGLADESHI),
        SurnameEntry("করিম", "Karim", NameCultureStyle.BANGLADESHI),
        SurnameEntry("সরকার", "Sarker", NameCultureStyle.BANGLADESHI),
        SurnameEntry("সিকদার", "Sikder", NameCultureStyle.BANGLADESHI),
        SurnameEntry("তালুকদার", "Talukder", NameCultureStyle.BANGLADESHI),
        SurnameEntry("ভুঁইয়া", "Bhuiyan", NameCultureStyle.BANGLADESHI),
        SurnameEntry("মজুমদার", "Majumder", NameCultureStyle.BANGLADESHI),
        SurnameEntry("সিদ্দিকী", "Siddiqui", NameCultureStyle.BANGLADESHI),
        SurnameEntry("কবির", "Kabir", NameCultureStyle.BANGLADESHI),
        SurnameEntry("জামান", "Zaman", NameCultureStyle.BANGLADESHI),
        SurnameEntry("আনোয়ার", "Anwar", NameCultureStyle.BANGLADESHI),
        SurnameEntry("খন্দকার", "Khandaker", NameCultureStyle.BANGLADESHI),
        SurnameEntry("পাটোয়ারী", "Patwary", NameCultureStyle.BANGLADESHI),
        SurnameEntry("সেন", "Sen", NameCultureStyle.BANGLADESHI),
        SurnameEntry("রায়", "Roy", NameCultureStyle.BANGLADESHI),
        SurnameEntry("দাস", "Das", NameCultureStyle.BANGLADESHI),
        SurnameEntry("মিত্র", "Mitra", NameCultureStyle.BANGLADESHI)
    )

    private val internationalSurnames = listOf(
        SurnameEntry("কার্টার", "Carter", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("মিচেল", "Mitchell", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("ব্রুকস", "Brooks", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("অ্যান্ডারসন", "Anderson", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("টেলর", "Taylor", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("কলিন্স", "Collins", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("পার্কার", "Parker", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("মরগান", "Morgan", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("কুপার", "Cooper", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("রিড", "Reed", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("বেনেট", "Bennett", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("ফস্টার", "Foster", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("হেয়েস", "Hayes", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("মিলার", "Miller", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("উইলসন", "Wilson", NameCultureStyle.INTERNATIONAL),
        SurnameEntry("হ্যারিসন", "Harrison", NameCultureStyle.INTERNATIONAL)
    )

    private val monthsEn = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val monthsBn = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    private val daysOfWeekEn = listOf(
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )

    private val daysOfWeekBn = listOf(
        "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )

    val popularTempDomains = listOf(
        "temp-mail.org",
        "10minutemail.com",
        "guerrillamail.com",
        "mailinator.com",
        "yopmail.com",
        "gmail.com",
        "outlook.com"
    )

    fun toBengaliDigits(number: Int): String = toBengaliDigits(number.toString())

    fun toBengaliDigits(input: String): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bnDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Generates a realistic Date of Birth strictly between [minAge] and [maxAge] years old
     * (clamped within 18..35 to honor the user's requirement).
     */
    fun generateRealisticDob(
        minAge: Int = 18,
        maxAge: Int = 35,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): GeneratedDob {
        val safeMinAge = minAge.coerceIn(18, 35)
        val safeMaxAge = maxAge.coerceIn(safeMinAge, 35)

        val today = Calendar.getInstance(Locale.US).apply {
            timeInMillis = referenceTimeMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Latest possible birth date to be at least safeMinAge today
        val latestBirth = (today.clone() as Calendar).apply {
            add(Calendar.YEAR, -safeMinAge)
        }

        // Earliest possible birth date to still be at most safeMaxAge today (not yet safeMaxAge + 1)
        val earliestBirth = (today.clone() as Calendar).apply {
            add(Calendar.YEAR, -(safeMaxAge + 1))
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val spanMillis = (latestBirth.timeInMillis - earliestBirth.timeInMillis).coerceAtLeast(1L)
        val randomOffset = (secureRandom.nextDouble() * spanMillis).toLong()

        val birthCal = Calendar.getInstance(Locale.US).apply {
            timeInMillis = earliestBirth.timeInMillis + randomOffset
        }

        val day = birthCal.get(Calendar.DAY_OF_MONTH)
        val monthZeroBased = birthCal.get(Calendar.MONTH) // 0..11
        val monthNumber = monthZeroBased + 1
        val year = birthCal.get(Calendar.YEAR)
        val dayOfWeekIdx = (birthCal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)

        // Exact age calculation
        var calculatedAge = today.get(Calendar.YEAR) - year
        val todayMonth = today.get(Calendar.MONTH)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        if (todayMonth < monthZeroBased || (todayMonth == monthZeroBased && todayDay < day)) {
            calculatedAge--
        }
        calculatedAge = calculatedAge.coerceIn(18, 35)

        val monthEn = monthsEn[monthZeroBased]
        val monthBn = monthsBn[monthZeroBased]

        val formattedEn = String.format(Locale.US, "%02d %s %d", day, monthEn, year)
        val formattedBn = "${toBengaliDigits(day)} $monthBn ${toBengaliDigits(year)}"
        val numericSlash = String.format(Locale.US, "%02d/%02d/%d", day, monthNumber, year)

        return GeneratedDob(
            day = day,
            monthNumber = monthNumber,
            monthNameEn = monthEn,
            monthNameBn = monthBn,
            year = year,
            ageYears = calculatedAge,
            formattedEn = formattedEn,
            formattedBn = formattedBn,
            numericSlash = numericSlash,
            dayOfWeekBn = daysOfWeekBn[dayOfWeekIdx],
            dayOfWeekEn = daysOfWeekEn[dayOfWeekIdx]
        )
    }

    /**
     * Generates a cryptographically strong password with:
     * - At least 8 characters (configurable 8..24, default 14)
     * - Uppercase letters (A-Z)
     * - Lowercase letters (a-z)
     * - Numbers (0-9)
     * - Special characters (!@#$%^&*_-+=?)
     */
    fun generateStrongPassword(length: Int = 14): String {
        val targetLength = length.coerceIn(8, 24)
        val chars = ArrayList<Char>(targetLength)

        // Guarantee at least 2 of each category when length >= 10, or at least 1 when length is 8..9
        val minPerClass = if (targetLength >= 10) 2 else 1
        repeat(minPerClass) {
            chars.add(UPPERCASE_CHARS[secureRandom.nextInt(UPPERCASE_CHARS.length)])
            chars.add(LOWERCASE_CHARS[secureRandom.nextInt(LOWERCASE_CHARS.length)])
            chars.add(DIGIT_CHARS[secureRandom.nextInt(DIGIT_CHARS.length)])
            chars.add(SPECIAL_CHARS[secureRandom.nextInt(SPECIAL_CHARS.length)])
        }

        while (chars.size < targetLength) {
            chars.add(ALL_PASSWORD_CHARS[secureRandom.nextInt(ALL_PASSWORD_CHARS.length)])
        }

        // Fisher-Yates shuffle using SecureRandom
        for (i in chars.lastIndex downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val tmp = chars[i]
            chars[i] = chars[j]
            chars[j] = tmp
        }

        // Avoid starting or ending with confusing punctuation if desired, while keeping all rules intact
        return chars.joinToString("")
    }

    fun analyzePassword(password: String): PasswordAnalysis {
        val upper = password.count { it.isUpperCase() }
        val lower = password.count { it.isLowerCase() }
        val digits = password.count { it.isDigit() }
        val special = password.count { !it.isLetterOrDigit() && !it.isWhitespace() }

        val hasMinLength = password.length >= 8
        val hasUpper = upper > 0
        val hasLower = lower > 0
        val hasDigit = digits > 0
        val hasSpecial = special > 0

        var poolSize = 0
        if (hasUpper) poolSize += 26
        if (hasLower) poolSize += 26
        if (hasDigit) poolSize += 10
        if (hasSpecial) poolSize += 14

        val entropy = if (poolSize > 0 && password.isNotEmpty()) {
            (password.length * log2(poolSize.toDouble())).toInt()
        } else {
            0
        }

        val rulesPassed = listOf(hasMinLength, hasUpper, hasLower, hasDigit, hasSpecial).count { it }
        val lengthBonus = ((password.length - 8).coerceIn(0, 12)) / 24f
        val rawScore = (rulesPassed / 5f) * 0.75f + (if (hasMinLength) lengthBonus else 0f)
        val score = rawScore.coerceIn(0f, 1f)

        val (labelBn, labelEn) = when {
            rulesPassed == 5 && password.length >= 12 -> "অতি শক্তিশালী (Very Strong)" to "Very Strong"
            rulesPassed == 5 -> "শক্তিশালী (Strong)" to "Strong"
            rulesPassed >= 3 -> "মাঝারি (Moderate)" to "Moderate"
            else -> "দুর্বল (Weak)" to "Weak"
        }

        return PasswordAnalysis(
            password = password,
            length = password.length,
            hasMinLength = hasMinLength,
            hasUppercase = hasUpper,
            hasLowercase = hasLower,
            hasDigit = hasDigit,
            hasSpecial = hasSpecial,
            uppercaseCount = upper,
            lowercaseCount = lower,
            digitCount = digits,
            specialCount = special,
            entropyBits = entropy,
            strengthScore = score,
            strengthLabelBn = labelBn,
            strengthLabelEn = labelEn
        )
    }

    /**
     * Generates a strictly unique Meta/Facebook-ready identity profile.
     */
    fun generateUniqueProfile(
        genderFilter: GenderFilter = GenderFilter.ANY,
        cultureStyle: NameCultureStyle = NameCultureStyle.BANGLADESHI,
        minAge: Int = 18,
        maxAge: Int = 35,
        passwordLength: Int = 14,
        tempEmail: String = ""
    ): GeneratedIdentityProfile {
        val baseFirstPool = when (cultureStyle) {
            NameCultureStyle.BANGLADESHI -> bengaliFirstNames
            NameCultureStyle.INTERNATIONAL -> internationalFirstNames
        }
        val filteredFirstPool = when (genderFilter) {
            GenderFilter.ANY -> baseFirstPool
            else -> baseFirstPool.filter { it.gender == genderFilter }.ifEmpty { baseFirstPool }
        }
        val surnamePool = when (cultureStyle) {
            NameCultureStyle.BANGLADESHI -> bengaliSurnames
            NameCultureStyle.INTERNATIONAL -> internationalSurnames
        }

        var chosenFirst = filteredFirstPool[secureRandom.nextInt(filteredFirstPool.size)]
        var chosenLast = surnamePool[secureRandom.nextInt(surnamePool.size)]
        var attempts = 0

        while (attempts < 40) {
            val sig = "${chosenFirst.en}_${chosenLast.en}"
            // Also avoid first name == last name (e.g., Mahmud Mahmud)
            if (chosenFirst.en != chosenLast.en && !recentSignatures.contains(sig)) {
                if (recentSignatures.size >= 50) {
                    recentSignatures.removeFirst()
                }
                recentSignatures.addLast(sig)
                break
            }
            chosenFirst = filteredFirstPool[secureRandom.nextInt(filteredFirstPool.size)]
            chosenLast = surnamePool[secureRandom.nextInt(surnamePool.size)]
            attempts++
        }

        val dob = generateRealisticDob(minAge = minAge, maxAge = maxAge)
        val password = generateStrongPassword(length = passwordLength)
        val analysis = analyzePassword(password)

        val suffixNum = (dob.year % 100).toString().padStart(2, '0') +
            (secureRandom.nextInt(90) + 10).toString()
        val usernameHandle =
            "${chosenFirst.en.lowercase(Locale.US)}.${chosenLast.en.lowercase(Locale.US)}.$suffixNum"

        return GeneratedIdentityProfile(
            firstNameBn = chosenFirst.bn,
            lastNameBn = chosenLast.bn,
            firstNameEn = chosenFirst.en,
            lastNameEn = chosenLast.en,
            usernameHandle = usernameHandle,
            gender = chosenFirst.gender,
            cultureStyle = cultureStyle,
            dob = dob,
            password = password,
            passwordAnalysis = analysis,
            tempEmail = tempEmail.trim()
        )
    }

    fun suggestTempEmailForProfile(
        profile: GeneratedIdentityProfile,
        domain: String = "temp-mail.org"
    ): String {
        val cleanDomain = domain.removePrefix("@").trim().ifEmpty { "temp-mail.org" }
        val num = (profile.dob.year % 100).toString().padStart(2, '0') +
            profile.dob.day.toString().padStart(2, '0')
        val localPart =
            "${profile.firstNameEn.lowercase(Locale.US)}.${profile.lastNameEn.lowercase(Locale.US)}$num"
        return "$localPart@$cleanDomain"
    }

    fun isValidEmailFormat(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty() || trimmed.contains(" ")) return false
        val atIndex = trimmed.indexOf('@')
        if (atIndex <= 0 || atIndex != trimmed.lastIndexOf('@')) return false
        val domainPart = trimmed.substring(atIndex + 1)
        return domainPart.length >= 3 && domainPart.contains('.') && !domainPart.startsWith('.') && !domainPart.endsWith('.')
    }

    /**
     * Extracts a 4 to 8 digit OTP/confirmation code from raw clipboard text
     * (e.g. "FB-58392 is your confirmation code" -> "58392").
     */
    fun extractOtpFromRawText(raw: String): String {
        val trimmed = raw.trim()
        val regex = Regex("""\b(\d{4,8})\b""")
        val match = regex.find(trimmed)
        if (match != null) {
            return match.groupValues[1]
        }
        val digitsOnly = trimmed.filter { it.isDigit() }
        return if (digitsOnly.length in 4..8) digitsOnly else trimmed.take(8)
    }

    private fun escapeJsString(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "")
            .replace("\r", "")
    }

    /**
     * Builds a real DOM & React-18-compatible JavaScript injection script for Meta AI
     * (https://www.meta.ai/ and https://auth.meta.com/) that:
     * 1. Strips/hides Facebook & Instagram login buttons so only Meta AI Email / Account flow runs.
     * 2. Uses React's internal _valueTracker setter so controlled inputs enable Continue / Next buttons.
     * 3. Automatically fills Email, 18-35 Date of Birth (Day/Month/Year or Birth Year), Name,
     *    Strong Password, and 6-digit OTP confirmation code, then clicks Continue / Next / Sign Up.
     * 4. Injects an active Meta AI session status bar at the top of the page.
     */
    fun buildMetaWebViewAutoFillJs(
        profile: GeneratedIdentityProfile,
        email: String,
        otpCode: String = "",
        preferBengaliName: Boolean = false,
        triggerSubmitOtp: Boolean = false,
        autoClickNext: Boolean = true
    ): String {
        val firstName = escapeJsString(if (preferBengaliName) profile.firstNameBn else profile.firstNameEn)
        val lastName = escapeJsString(if (preferBengaliName) profile.lastNameBn else profile.lastNameEn)
        val fullName = escapeJsString(if (preferBengaliName) profile.fullNameBn else profile.fullNameEn)
        val safeEmail = escapeJsString(email.trim())
        val safePass = escapeJsString(profile.password)
        val safeOtp = escapeJsString(otpCode.trim())
        val day = profile.dob.day
        val month = profile.dob.monthNumber
        val monthNameEn = escapeJsString(profile.dob.monthNameEn)
        val year = profile.dob.year
        val ageYears = profile.dob.ageYears
        val isoDate = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

        return """
            (function() {
                // 0. Hook window.open so auth popups stay inside this WebView (and block Facebook/Instagram redirects)
                if (!window.__metaOpenHooked) {
                    window.__metaOpenHooked = true;
                    window.open = function(url) {
                        if (url && typeof url === 'string' &&
                            url.indexOf('facebook.com') === -1 &&
                            url.indexOf('instagram.com') === -1) {
                            window.location.href = url;
                        }
                        return {
                            closed: false,
                            focus: function() {},
                            close: function() {},
                            postMessage: function() {},
                            location: window.location
                        };
                    };
                }
                document.querySelectorAll('a[target="_blank"]').forEach(function(a) {
                    a.setAttribute('target', '_self');
                });

                function isVisible(el) {
                    if (!el) return false;
                    var rect = el.getBoundingClientRect();
                    var style = window.getComputedStyle(el);
                    return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
                }

                // 0.1 Hide any "Continue with Facebook" or "Continue with Instagram" buttons so Facebook never opens
                document.querySelectorAll('a, button, div[role="button"]').forEach(function(el) {
                    var t = ((el.innerText || el.textContent || el.getAttribute('aria-label') || '') + '').trim().toLowerCase();
                    var href = ((el.getAttribute('href') || '') + '').toLowerCase();
                    if ((t.indexOf('facebook') !== -1 || t.indexOf('instagram') !== -1 ||
                         t.indexOf('ফেসবুক') !== -1 || t.indexOf('ইনস্টাগ্রাম') !== -1 ||
                         href.indexOf('facebook.com') !== -1 || href.indexOf('instagram.com') !== -1) &&
                        t.length < 80) {
                        el.style.display = 'none';
                        el.setAttribute('data-meta-blocked', 'true');
                    }
                });

                // 0.2 Inject or update the top Meta AI Active Account Session banner inside the WebView
                if (document.body && '$safeEmail'.length > 0) {
                    var bannerId = '__meta_ai_auto_session_bar';
                    var bar = document.getElementById(bannerId);
                    if (!bar) {
                        bar = document.createElement('div');
                        bar.id = bannerId;
                        bar.style.cssText = 'position:sticky;top:0;left:0;right:0;z-index:2147483647;background:linear-gradient(90deg,#0866FF,#00A884);color:#fff;padding:7px 12px;font-family:sans-serif;font-size:12px;font-weight:bold;display:flex;justify-content:space-between;align-items:center;box-shadow:0 2px 8px rgba(0,0,0,0.28);';
                        document.body.prepend(bar);
                    }
                    var statusBadge = '$safeOtp'.length >= 4 ? ('✓ Meta AI Logged In (OTP: ' + '$safeOtp' + ')') : '⚡ Auto-Filling Meta AI...';
                    bar.innerHTML = '<span>🤖 ' + '$fullName' + ' • ' + '$safeEmail' + ' (' + $ageYears + 'y)</span><span style="background:rgba(255,255,255,0.22);padding:2px 7px;border-radius:10px;">' + statusBadge + '</span>';
                }

                // Persist active Meta AI profile in localStorage/sessionStorage for meta.ai session continuity
                try {
                    localStorage.setItem('meta_ai_verified_email', '$safeEmail');
                    localStorage.setItem('meta_ai_verified_name', '$fullName');
                    localStorage.setItem('meta_ai_birth_year', String($year));
                    localStorage.setItem('meta_ai_age_verified', 'true');
                } catch (e) {}

                function triggerRealClick(el) {
                    if (!el) return;
                    try {
                        var rect = el.getBoundingClientRect();
                        var cx = rect.left + rect.width / 2;
                        var cy = rect.top + rect.height / 2;
                        var opts = { bubbles: true, cancelable: true, view: window, clientX: cx, clientY: cy, button: 0, buttons: 1 };
                        el.dispatchEvent(new PointerEvent('pointerdown', opts));
                        el.dispatchEvent(new MouseEvent('mousedown', opts));
                        el.focus();
                        el.dispatchEvent(new PointerEvent('pointerup', opts));
                        el.dispatchEvent(new MouseEvent('mouseup', opts));
                        el.dispatchEvent(new MouseEvent('click', opts));
                        el.click();
                    } catch (e) {
                        try { el.click(); } catch (e2) {}
                    }
                }

                function setNativeValue(el, value) {
                    if (!el || value === undefined || value === null || !isVisible(el)) return false;
                    var strVal = String(value);
                    if (el.value === strVal && el.getAttribute('data-meta-filled') === strVal) return true;
                    try {
                        el.focus();
                        var lastValue = el.value;
                        var proto = Object.getPrototypeOf(el);
                        var desc = Object.getOwnPropertyDescriptor(proto, 'value') ||
                                   (window.HTMLInputElement ? Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value') : null);
                        if (desc && desc.set) {
                            desc.set.call(el, strVal);
                        } else {
                            el.value = strVal;
                        }
                        if (el._valueTracker) {
                            el._valueTracker.setValue(lastValue);
                        }
                        el.setAttribute('data-meta-filled', strVal);
                        el.dispatchEvent(new Event('focus', { bubbles: true }));
                        el.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, cancelable: true, key: strVal.slice(-1) }));
                        el.dispatchEvent(new InputEvent('input', { bubbles: true, cancelable: true, data: strVal, inputType: 'insertText' }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                        el.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true, cancelable: true, key: strVal.slice(-1) }));
                        el.dispatchEvent(new Event('blur', { bubbles: true }));
                        return true;
                    } catch (e) {
                        el.value = strVal;
                        return true;
                    }
                }

                function getFieldContext(el) {
                    if (!el) return '';
                    var parts = [
                        el.name || '',
                        el.id || '',
                        el.type || '',
                        el.placeholder || '',
                        el.getAttribute('aria-label') || '',
                        el.getAttribute('autocomplete') || '',
                        el.getAttribute('title') || ''
                    ];
                    var labelledBy = el.getAttribute('aria-labelledby');
                    if (labelledBy) {
                        labelledBy.split(/\s+/).forEach(function(id) {
                            var lbl = document.getElementById(id);
                            if (lbl) parts.push(lbl.innerText || lbl.textContent || '');
                        });
                    }
                    if (el.labels && el.labels.length > 0) {
                        for (var i = 0; i < el.labels.length; i++) {
                            parts.push(el.labels[i].innerText || el.labels[i].textContent || '');
                        }
                    }
                    var parentLabel = el.closest('label');
                    if (parentLabel) {
                        parts.push(parentLabel.innerText || parentLabel.textContent || '');
                    } else if (el.parentElement && el.parentElement.parentElement) {
                        var pText = (el.parentElement.parentElement.innerText || '').trim();
                        if (pText.length < 90) parts.push(pText);
                    }
                    return parts.join(' ').toLowerCase();
                }

                function setSelectValue(sel, val, altText) {
                    if (!sel || !isVisible(sel)) return false;
                    for (var i = 0; i < sel.options.length; i++) {
                        var opt = sel.options[i];
                        var optText = (opt.text || '').trim().toLowerCase();
                        if (String(opt.value) === String(val) ||
                            (altText && optText.indexOf(altText.toLowerCase()) !== -1) ||
                            optText === String(val)) {
                            if (sel.selectedIndex !== i) {
                                sel.selectedIndex = i;
                                sel.value = opt.value;
                                sel.dispatchEvent(new Event('change', { bubbles: true }));
                            }
                            return true;
                        }
                    }
                    return false;
                }

                var filledCount = 0;
                var detectedStage = "SCANNING";
                var bodyText = ((document.body ? document.body.innerText : '') || '').toLowerCase();

                // 1. Check for Entry Triggers ("Continue with email", "Use email", "Create new account")
                // Note: Only query clickable buttons/links (NEVER static <span> headings!)
                var clickableNodes = document.querySelectorAll('button, a, div[role="button"], input[type="button"], input[type="submit"]');
                var visibleInputsCount = Array.from(document.querySelectorAll('input:not([type="hidden"])')).filter(isVisible).length;

                for (var i = 0; i < clickableNodes.length; i++) {
                    var node = clickableNodes[i];
                    if (!isVisible(node) || node.getAttribute('data-meta-blocked') === 'true') continue;
                    var txt = ((node.innerText || node.textContent || node.getAttribute('aria-label') || '') + '').trim().toLowerCase();
                    if (txt.indexOf('facebook') !== -1 || txt.indexOf('instagram') !== -1 ||
                        txt.indexOf('without') !== -1 || txt.indexOf('not now') !== -1 ||
                        txt.indexOf('cancel') !== -1) continue;

                    var isEmailEntry = (txt === 'continue with email' || txt === 'sign up with email' ||
                        txt === 'log in with email' || txt === 'use email' ||
                        txt === 'continue with meta account' || txt === 'log in with meta account' ||
                        txt === 'create new account' || txt === 'create a new account' ||
                        txt === 'create a new meta account' || txt === 'create account' ||
                        txt === 'ইমেইল দিয়ে চালিয়ে যান');

                    var isLoginModalOpener = (visibleInputsCount <= 1 &&
                        (txt === 'log in' || txt === 'sign in' || txt === 'sign up'));

                    if (isEmailEntry || isLoginModalOpener) {
                        var trigSig = window.location.href + '_entry_' + txt;
                        if (window.__lastEntryClick !== trigSig) {
                            window.__lastEntryClick = trigSig;
                            triggerRealClick(node);
                            filledCount++;
                            detectedStage = "CLICKED_AUTH_ENTRY";
                            break;
                        }
                    }
                }

                // 2. Check for Confirmation Code / OTP Screen
                var isOtpScreen = bodyText.indexOf('confirmation code') !== -1 ||
                    bodyText.indexOf('verification code') !== -1 ||
                    bodyText.indexOf('security code') !== -1 ||
                    bodyText.indexOf('enter the code') !== -1 ||
                    bodyText.indexOf('enter the 6-digit') !== -1 ||
                    bodyText.indexOf('we sent a code') !== -1 ||
                    bodyText.indexOf('কোডটি লিখুন') !== -1;

                var otpFilled = false;
                var digitBoxes = Array.from(document.querySelectorAll('input[maxlength="1"]')).filter(isVisible);
                var allVisibleInputs = Array.from(document.querySelectorAll('input:not([type="hidden"]):not([type="submit"]):not([type="button"]):not([type="checkbox"]):not([type="radio"])')).filter(isVisible);

                if (isOtpScreen || digitBoxes.length >= 4) {
                    detectedStage = "WAITING_FOR_OTP";
                    if ('$safeOtp'.length >= 4) {
                        if (digitBoxes.length >= 4 && '$safeOtp'.length >= digitBoxes.length) {
                            for (var d = 0; d < digitBoxes.length; d++) {
                                if (setNativeValue(digitBoxes[d], '$safeOtp'.charAt(d))) {
                                    filledCount++;
                                    otpFilled = true;
                                    detectedStage = "OTP_SUBMITTED";
                                }
                            }
                        }
                        if (!otpFilled) {
                            allVisibleInputs.forEach(function(el) {
                                var ctx = getFieldContext(el);
                                if (ctx.indexOf('code') !== -1 || ctx.indexOf('otp') !== -1 || allVisibleInputs.length === 1) {
                                    if (setNativeValue(el, '$safeOtp')) {
                                        filledCount++;
                                        otpFilled = true;
                                        detectedStage = "OTP_SUBMITTED";
                                    }
                                }
                            });
                        }
                    }
                }

                // 3. Date of Birth / Meta AI Birth Year Gate (Selects, Comboboxes, or Inputs)
                var selects = document.querySelectorAll('select');
                var dobTouched = false;
                selects.forEach(function(sel, idx) {
                    var ctx = getFieldContext(sel);
                    if (ctx.indexOf('day') !== -1 || (selects.length === 3 && idx === 1)) {
                        if (setSelectValue(sel, $day, String($day))) { filledCount++; dobTouched = true; }
                    } else if (ctx.indexOf('month') !== -1 || (selects.length === 3 && idx === 0)) {
                        if (setSelectValue(sel, $month, '$monthNameEn')) { filledCount++; dobTouched = true; }
                    } else if (ctx.indexOf('year') !== -1 || ctx.indexOf('birth') !== -1 || (selects.length === 3 && idx === 2) || selects.length === 1) {
                        if (setSelectValue(sel, $year, String($year))) { filledCount++; dobTouched = true; }
                    }
                });

                // Also handle Meta AI custom combobox / listbox for Birth Year
                var comboboxes = document.querySelectorAll('[role="combobox"], [aria-haspopup="listbox"]');
                comboboxes.forEach(function(cb) {
                    if (!isVisible(cb)) return;
                    var cbText = (cb.innerText || cb.textContent || '').trim();
                    if (cbText.indexOf('$year') === -1 && (cbText.toLowerCase().indexOf('year') !== -1 || cbText.toLowerCase().indexOf('select') !== -1 || cbText === '')) {
                        triggerRealClick(cb);
                        setTimeout(function() {
                            var opts = document.querySelectorAll('[role="option"], li');
                            for (var o = 0; o < opts.length; o++) {
                                if ((opts[o].innerText || '').trim() === String($year)) {
                                    triggerRealClick(opts[o]);
                                    break;
                                }
                            }
                        }, 200);
                        dobTouched = true;
                    }
                });

                // 4. Inspect all visible inputs using getFieldContext(el) for Email, DOB, Name, Password
                var emailTouched = false;
                var nameTouched = false;
                var passTouched = false;

                allVisibleInputs.forEach(function(el) {
                    if (el.getAttribute('maxlength') === '1') return;
                    var ctx = getFieldContext(el);

                    // Password field
                    if (el.type === 'password' || ctx.indexOf('password') !== -1 || ctx.indexOf('পাসওয়ার্ড') !== -1) {
                        if (setNativeValue(el, '$safePass')) {
                            filledCount++;
                            passTouched = true;
                        }
                        return;
                    }

                    // Date / Year / Month / Day fields
                    if (el.type === 'date' || ctx.indexOf('date of birth') !== -1 || ctx.indexOf('birthday') !== -1) {
                        if (setNativeValue(el, el.type === 'date' ? '$isoDate' : '${profile.dob.numericSlash}')) {
                            filledCount++;
                            dobTouched = true;
                        }
                        return;
                    }
                    if (ctx.indexOf('year') !== -1 || ctx.indexOf('yyyy') !== -1 || (bodyText.indexOf('birth year') !== -1 && allVisibleInputs.length === 1)) {
                        if (setNativeValue(el, String($year))) {
                            filledCount++;
                            dobTouched = true;
                        }
                        return;
                    }
                    if (ctx.indexOf('month') !== -1 || ctx.indexOf('mm') !== -1) {
                        if (setNativeValue(el, String($month))) {
                            filledCount++;
                            dobTouched = true;
                        }
                        return;
                    }
                    if (ctx.indexOf('day') !== -1 || ctx.indexOf('dd') !== -1) {
                        if (setNativeValue(el, String($day))) {
                            filledCount++;
                            dobTouched = true;
                        }
                        return;
                    }

                    // First / Last / Full Name fields
                    if (ctx.indexOf('first name') !== -1 || ctx.indexOf('firstname') !== -1 || ctx.indexOf('নামের প্রথম') !== -1) {
                        if (setNativeValue(el, '$firstName')) {
                            filledCount++;
                            nameTouched = true;
                        }
                        return;
                    }
                    if (ctx.indexOf('last name') !== -1 || ctx.indexOf('lastname') !== -1 || ctx.indexOf('surname') !== -1 || ctx.indexOf('নামের শেষ') !== -1) {
                        if (setNativeValue(el, '$lastName')) {
                            filledCount++;
                            nameTouched = true;
                        }
                        return;
                    }
                    if (ctx.indexOf('full name') !== -1 || (ctx.indexOf('name') !== -1 && ctx.indexOf('user') === -1 && ctx.indexOf('email') === -1)) {
                        if (setNativeValue(el, '$fullName')) {
                            filledCount++;
                            nameTouched = true;
                        }
                        return;
                    }

                    // Email field (skip if it's the Meta AI chat prompt input!)
                    var isChatPrompt = ctx.indexOf('ask meta ai') !== -1 || ctx.indexOf('message') !== -1 || ctx.indexOf('search') !== -1 || ctx.indexOf('ask anything') !== -1;
                    if (!isChatPrompt && !isOtpScreen && '$safeEmail'.length > 0) {
                        if (el.type === 'email' || ctx.indexOf('email') !== -1 || ctx.indexOf('contactpoint') !== -1 || ctx.indexOf('ইমেইল') !== -1 ||
                            (allVisibleInputs.length === 1 && (bodyText.indexOf('email') !== -1 || bodyText.indexOf('log in') !== -1 || bodyText.indexOf('sign up') !== -1 || bodyText.indexOf('meta account') !== -1))) {
                            if (setNativeValue(el, '$safeEmail')) {
                                filledCount++;
                                emailTouched = true;
                            }
                        }
                    }
                });

                if (emailTouched) detectedStage = "EMAIL_FILLED";
                if (dobTouched) detectedStage = "DOB_FILLED";
                if (nameTouched) detectedStage = "NAME_FILLED";
                if (passTouched) detectedStage = "PASSWORD_FILLED";
                if (otpFilled) detectedStage = "OTP_SUBMITTED";

                // 5. Auto-click Continue / Next / Sign Up / Confirm / Agree (Strictly excluding Facebook/Instagram!)
                var isConfirmPrompt = bodyText.indexOf('create a new') !== -1 ||
                    bodyText.indexOf('finish setting up') !== -1 ||
                    bodyText.indexOf('agree to') !== -1 ||
                    bodyText.indexOf('birth year') !== -1 ||
                    bodyText.indexOf('when were you born') !== -1;

                if ((($autoClickNext && (filledCount > 0 || isConfirmPrompt)) && (!isOtpScreen || otpFilled)) || ($triggerSubmitOtp && otpFilled)) {
                    setTimeout(function() {
                        var exactLabels = [
                            'continue', 'next', 'পরবর্তী', 'চালিয়ে যান',
                            'create account', 'create new account', 'create a new account',
                            'sign up', 'সাইন আপ', 'agree', 'i agree', 'সম্মত',
                            'confirm', 'verify', 'submit', 'finish', 'done', 'save'
                        ];
                        var candidates = document.querySelectorAll('button, input[type="submit"], div[role="button"]');
                        for (var k = 0; k < candidates.length; k++) {
                            var btn = candidates[k];
                            if (!isVisible(btn) || btn.disabled || btn.getAttribute('aria-disabled') === 'true' || btn.getAttribute('data-meta-blocked') === 'true') continue;
                            var label = ((btn.innerText || btn.textContent || btn.value || btn.getAttribute('aria-label') || '') + '').trim().toLowerCase();
                            if (label.indexOf('facebook') !== -1 || label.indexOf('instagram') !== -1 ||
                                label.indexOf('google') !== -1 || label.indexOf('apple') !== -1 ||
                                label.indexOf('without') !== -1 || label.indexOf('cancel') !== -1 ||
                                label.indexOf('back') !== -1 || label.indexOf('not now') !== -1) continue;

                            for (var m = 0; m < exactLabels.length; m++) {
                                if (label === exactLabels[m]) {
                                    var stepSig = window.location.href + '_' + detectedStage + '_' + label + '_' + '$safeEmail' + '_' + '$safeOtp';
                                    if (window.__lastAutoClickSig !== stepSig) {
                                        window.__lastAutoClickSig = stepSig;
                                        triggerRealClick(btn);
                                    }
                                    return;
                                }
                            }
                        }
                    }, 450);
                }

                return JSON.stringify({ stage: detectedStage, filled: filledCount, url: window.location.href });
            })();
        """.trimIndent()
    }

    /**
     * JavaScript scraper for the Top Temp-Mail Website WebView that detects
     * any generated disposable email address in dedicated input fields and any incoming 4-8 digit OTP code.
     */
    fun buildTopTempMailScraperJs(): String {
        return """
            (function() {
                var foundEmail = "";
                var foundOtp = "";
                var emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

                // Only check dedicated readonly/email inputs or clipboard attributes so random page text is never mistaken
                var inputs = document.querySelectorAll('input[type="text"], input[type="email"], input[readonly], [data-clipboard-text]');
                for (var i = 0; i < inputs.length; i++) {
                    var el = inputs[i];
                    var val = (el.value || el.getAttribute('data-clipboard-text') || '').trim();
                    if (emailRegex.test(val) && val.indexOf('example.') === -1 && val.indexOf('support@') === -1) {
                        foundEmail = val;
                        break;
                    }
                }

                var bodyText = (document.body ? document.body.innerText : '') || '';
                var fbCodeMatch = bodyText.match(/(?:FB-|Meta\s*code\s*[:is]*\s*|confirmation\s*code\s*[:is]*\s*|কোড\s*[:হলো]*\s*)(\d{5,8})/i);
                if (fbCodeMatch && fbCodeMatch[1]) {
                    foundOtp = fbCodeMatch[1];
                }

                return JSON.stringify({ email: foundEmail, otp: foundOtp });
            })();
        """.trimIndent()
    }
}
