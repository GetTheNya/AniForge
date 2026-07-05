package moe.GetTheNya.AniForge.ui.profile

object UsernameValidator {
    sealed interface ValidationResult {
        object Valid : ValidationResult
        data class Invalid(val errorType: ErrorType) : ValidationResult
    }

    enum class ErrorType {
        TOO_SHORT,
        TOO_LONG,
        INVALID_CHARACTERS
    }

    fun validate(username: String): ValidationResult {
        if (username.length < 3) {
            return ValidationResult.Invalid(ErrorType.TOO_SHORT)
        }
        if (username.length > 15) {
            return ValidationResult.Invalid(ErrorType.TOO_LONG)
        }
        // Characters: Latin letters, digits, period, underscore, hyphen
        val allowedPattern = Regex("^[a-zA-Z0-9._-]+$")
        if (!allowedPattern.matches(username)) {
            return ValidationResult.Invalid(ErrorType.INVALID_CHARACTERS)
        }
        return ValidationResult.Valid
    }

    /**
     * Filters input to block spaces dynamically.
     */
    fun filterInput(input: String): String {
        return input.replace(" ", "")
    }
}
