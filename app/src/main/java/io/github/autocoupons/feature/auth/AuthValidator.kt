package io.github.autocoupons.feature.auth

enum class PasswordStrength(val label: String, val score: Int) {
    EMPTY("", 0),
    WEAK("Weak", 1),
    FAIR("Fair", 2),
    STRONG("Strong", 3),
}

/** Form validation for the demo screens. Pure Kotlin so it is unit-tested on the JVM. */
object AuthValidator {

    // Practical check (local@domain.tld); full RFC 5322 validation is out of scope for a demo.
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    const val MIN_PASSWORD_LENGTH = 8

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Enter your email address"
        !EMAIL.matches(email.trim()) -> "Enter a valid email address, like name@example.com"
        else -> null
    }

    fun loginPasswordError(password: String): String? =
        if (password.isEmpty()) "Enter your password" else null

    fun nameError(name: String): String? = when {
        name.isBlank() -> "Enter your full name"
        name.trim().length < 2 -> "Name is too short"
        else -> null
    }

    fun registrationPasswordError(password: String): String? = when {
        password.isEmpty() -> "Create a password"
        password.length < MIN_PASSWORD_LENGTH -> "Use at least $MIN_PASSWORD_LENGTH characters"
        strength(password) == PasswordStrength.WEAK -> "Add a mix of letters, numbers or symbols"
        else -> null
    }

    fun confirmPasswordError(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Re-enter your password"
        confirm != password -> "Passwords do not match"
        else -> null
    }

    fun strength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.EMPTY
        val classes = listOf(
            password.any { it.isLowerCase() },
            password.any { it.isUpperCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() },
        ).count { it }
        return when {
            password.length < MIN_PASSWORD_LENGTH || classes <= 1 -> PasswordStrength.WEAK
            classes == 4 || (password.length >= 12 && classes >= 3) -> PasswordStrength.STRONG
            else -> PasswordStrength.FAIR
        }
    }
}
