package com.raku.smsrelay.sms

data class SmsPresentation(
    val displaySender: String,
    val sourceAddress: String,
    val compactBody: String,
    val verificationCode: String?,
) {
    val notificationPreview: String
        get() = verificationCode?.let { "验证码 $it · $compactBody" } ?: compactBody

    val mailSubject: String
        get() = verificationCode?.let { "[验证码] $displaySender · $it" }
            ?: "[短信] $displaySender · ${compactBody.take(SUBJECT_PREVIEW_LENGTH)}"

    private companion object {
        const val SUBJECT_PREVIEW_LENGTH = 42
    }
}

object SmsPresentationFactory {
    private val chineseSignaturePattern = Regex("【([^】\\r\\n]{1,24})】")
    private val asciiSignaturePattern = Regex("\\[([^\\[\\]\\r\\n]{1,24})\\]")
    private val verificationCodePattern = Regex(
        pattern = "(?:验证码|校验码|动态码|确认码|安全码|一次性密码|OTP|CODE)[^\\d\\r\\n]{0,8}(\\d{4,8})",
        option = RegexOption.IGNORE_CASE,
    )
    private val whitespacePattern = Regex("\\s+")

    fun from(sender: String, body: String): SmsPresentation {
        val normalizedSender = sender.replace(whitespacePattern, " ").trim().ifBlank { "未知来源" }
        val compactBody = body.replace(whitespacePattern, " ").trim().ifBlank { "（无正文）" }
        val signature = sequenceOf(chineseSignaturePattern, asciiSignaturePattern)
            .mapNotNull { pattern -> pattern.find(compactBody)?.groups?.get(1)?.value }
            .map(String::trim)
            .firstOrNull(::isBusinessSignature)
        val code = verificationCodePattern.find(compactBody)?.groupValues?.getOrNull(1)

        return SmsPresentation(
            displaySender = signature ?: normalizedSender,
            sourceAddress = normalizedSender,
            compactBody = compactBody,
            verificationCode = code,
        )
    }

    private fun isBusinessSignature(value: String): Boolean =
        value.isNotBlank() && value.any { !it.isDigit() } && value.none { it == '\r' || it == '\n' }
}
