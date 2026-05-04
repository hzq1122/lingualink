package com.lingualink.translation

enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val localName: String,
    val offlineSupported: Boolean
) {
    AUTO("auto", "自动检测", "Auto", false),
    ZH("zh", "中文", "中文", true),
    EN("en", "English", "English", true),
    JA("ja", "日本語", "日本語", true),
    KO("ko", "한국어", "한국어", false),
    FR("fr", "法语", "Français", false),
    DE("de", "德语", "Deutsch", false),
    ES("es", "西班牙语", "Español", false),
    RU("ru", "俄语", "Русский", false),
    PT("pt", "葡萄牙语", "Português", false),
    IT("it", "意大利语", "Italiano", false),
    AR("ar", "阿拉伯语", "العربية", false),
    TH("th", "泰语", "ไทย", false),
    VI("vi", "越南语", "Tiếng Việt", false),
    NL("nl", "荷兰语", "Nederlands", false),
    PL("pl", "波兰语", "Polski", false),
    TR("tr", "土耳其语", "Türkçe", false),
    ID("id", "印尼语", "Indonesia", false),
    UK("uk", "乌克兰语", "Українська", false),
    HI("hi", "印地语", "हिन्दी", false);

    companion object {
        private val codeMap = entries.associateBy { it.code }
        fun fromCode(code: String): SupportedLanguage? = codeMap[code]
        fun onlineLanguages(): List<SupportedLanguage> = entries.filter { it != AUTO }
        fun offlineLanguages(): List<SupportedLanguage> = entries.filter { it.offlineSupported && it != AUTO }
        val priorityCodes = listOf("zh", "en", "ja", "ko")
    }
}
