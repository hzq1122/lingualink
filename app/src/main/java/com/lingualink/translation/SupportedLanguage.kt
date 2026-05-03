package com.lingualink.translation

enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val localName: String,
    val offlineSupported: Boolean
) {
    AUTO("auto", "自动检测", "Auto", true),
    ZH("zh", "中文", "中文", true),
    EN("en", "English", "English", true),
    JA("ja", "日本語", "日本語", true),
    KO("ko", "한국어", "한국어", true),
    FR("fr", "法语", "Français", true),
    DE("de", "德语", "Deutsch", true),
    ES("es", "西班牙语", "Español", true),
    RU("ru", "俄语", "Русский", true),
    PT("pt", "葡萄牙语", "Português", true),
    IT("it", "意大利语", "Italiano", true),
    AR("ar", "阿拉伯语", "العربية", true),
    TH("th", "泰语", "ไทย", true),
    VI("vi", "越南语", "Tiếng Việt", true),
    NL("nl", "荷兰语", "Nederlands", true),
    PL("pl", "波兰语", "Polski", true),
    TR("tr", "土耳其语", "Türkçe", true),
    ID("id", "印尼语", "Indonesia", true),
    UK("uk", "乌克兰语", "Українська", true),
    HI("hi", "印地语", "हिन्दी", true);

    companion object {
        private val codeMap = entries.associateBy { it.code }
        fun fromCode(code: String): SupportedLanguage? = codeMap[code]
        fun onlineLanguages(): List<SupportedLanguage> = entries.filter { it != AUTO }
        fun offlineLanguages(): List<SupportedLanguage> = entries.filter { it.offlineSupported && it != AUTO }
        val priorityCodes = listOf("zh", "en", "ja", "ko")
    }
}
