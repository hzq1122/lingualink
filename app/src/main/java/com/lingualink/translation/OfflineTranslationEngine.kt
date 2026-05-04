package com.lingualink.translation

import com.lingualink.domain.model.TranslationMode
import com.lingualink.domain.model.TranslationRequest
import com.lingualink.domain.model.TranslationResult

class OfflineTranslationEngine : TranslationEngine {

    override val mode = TranslationMode.OFFLINE

    override suspend fun translate(request: TranslationRequest): TranslationResult {
        val startTime = System.currentTimeMillis()
        val key = "${request.sourceLang}:${request.targetLang}"
        val dict = dictionaries[key] ?: throw UnsupportedOperationException(
            "离线翻译不支持 ${request.sourceLang} -> ${request.targetLang}"
        )
        val text = request.text.trim()
        val translated = dict[text]
            ?: dict[text.lowercase()]
            ?: translateWordByWord(text, dict)
            ?: throw UnsupportedOperationException("离线词典中未找到匹配内容")
        val latency = System.currentTimeMillis() - startTime
        return TranslationResult(
            translatedText = translated,
            sourceLang = request.sourceLang,
            targetLang = request.targetLang,
            latencyMs = latency,
            mode = TranslationMode.OFFLINE
        )
    }

    private fun translateWordByWord(text: String, dict: Map<String, String>): String? {
        val words = text.split(Regex("\\s+"))
        if (words.isEmpty()) return null
        val translatedParts = words.map { word ->
            dict[word.lowercase()] ?: dict[word] ?: return null
        }
        return translatedParts.joinToString(" ")
    }

    override suspend fun detectLanguage(text: String): String = "auto"

    override fun isAvailable(): Boolean = true

    companion object {
        private val zhEn = mapOf(
            "你好" to "Hello", "早上好" to "Good morning", "晚上好" to "Good evening",
            "再见" to "Goodbye", "谢谢" to "Thank you", "对不起" to "Sorry",
            "请" to "Please", "是" to "Yes", "不是" to "No",
            "我" to "I", "你" to "You", "他" to "He", "她" to "She",
            "我们" to "We", "他们" to "They",
            "什么" to "What", "谁" to "Who", "哪里" to "Where",
            "为什么" to "Why", "怎么样" to "How", "多少" to "How many",
            "好" to "Good", "不好" to "Bad", "大" to "Big", "小" to "Small",
            "水" to "Water", "食物" to "Food", "吃" to "Eat", "喝" to "Drink",
            "去" to "Go", "来" to "Come", "看" to "See", "听" to "Hear",
            "说" to "Speak", "读" to "Read", "写" to "Write",
            "今天" to "Today", "明天" to "Tomorrow", "昨天" to "Yesterday",
            "现在" to "Now", "时间" to "Time",
            "朋友" to "Friend", "家" to "Home", "学校" to "School",
            "工作" to "Work", "钱" to "Money", "价格" to "Price",
            "帮助" to "Help", "问题" to "Problem", "可以" to "Can",
            "不可以" to "Cannot", "想要" to "Want", "需要" to "Need",
            "喜欢" to "Like", "爱" to "Love", "知道" to "Know",
            "理解" to "Understand", "明白" to "Understand",
            "漂亮" to "Beautiful", "快乐" to "Happy", "难过" to "Sad",
            "热" to "Hot", "冷" to "Cold", "快" to "Fast", "慢" to "Slow",
            "新" to "New", "旧" to "Old", "买" to "Buy", "卖" to "Sell",
            "打开" to "Open", "关闭" to "Close", "开始" to "Start",
            "结束" to "End", "等待" to "Wait", "休息" to "Rest",
            "医院" to "Hospital", "医生" to "Doctor", "药" to "Medicine",
            "车" to "Car", "飞机" to "Airplane", "火车" to "Train",
            "电话" to "Phone", "电脑" to "Computer", "互联网" to "Internet"
        )

        private val enZh = zhEn.entries.associate { (k, v) -> v.lowercase() to k }

        private val enJa = mapOf(
            "hello" to "こんにちは", "good morning" to "おはようございます",
            "goodbye" to "さようなら", "thank you" to "ありがとう",
            "sorry" to "すみません", "please" to "お願いします",
            "yes" to "はい", "no" to "いいえ",
            "i" to "私", "you" to "あなた",
            "good" to "良い", "bad" to "悪い",
            "water" to "水", "food" to "食べ物",
            "eat" to "食べる", "drink" to "飲む",
            "go" to "行く", "come" to "来る",
            "see" to "見る", "hear" to "聞く",
            "today" to "今日", "tomorrow" to "明日",
            "friend" to "友達", "home" to "家",
            "help" to "助けて", "want" to "欲しい",
            "like" to "好き", "love" to "愛",
            "beautiful" to "美しい", "happy" to "幸せ",
            "hot" to "暑い", "cold" to "寒い"
        )

        private val jaEn = enJa.entries.associate { (k, v) -> v to k }

        private val zhJa = mapOf(
            "你好" to "こんにちは", "早上好" to "おはようございます",
            "晚上好" to "こんばんは", "再见" to "さようなら",
            "谢谢" to "ありがとう", "对不起" to "すみません",
            "请" to "お願いします", "是" to "はい", "不是" to "いいえ",
            "我" to "私", "你" to "あなた",
            "什么" to "何", "谁" to "誰", "哪里" to "どこ",
            "为什么" to "なぜ", "怎么样" to "どう",
            "好" to "良い", "不好" to "悪い",
            "大" to "大きい", "小" to "小さい",
            "水" to "水", "食物" to "食べ物",
            "吃" to "食べる", "喝" to "飲む",
            "去" to "行く", "来" to "来る",
            "看" to "見る", "听" to "聞く",
            "说" to "話す", "读" to "読む", "写" to "書く",
            "今天" to "今日", "明天" to "明日", "昨天" to "昨日",
            "现在" to "今", "时间" to "時間",
            "朋友" to "友達", "家" to "家", "学校" to "学校",
            "工作" to "仕事", "钱" to "お金", "价格" to "値段",
            "帮助" to "助ける", "问题" to "問題",
            "可以" to "できます", "不可以" to "できません",
            "想要" to "欲しい", "需要" to "必要",
            "喜欢" to "好き", "爱" to "愛",
            "知道" to "知る", "理解" to "理解する",
            "漂亮" to "美しい", "快乐" to "幸せ", "难过" to "悲しい",
            "热" to "暑い", "冷" to "寒い",
            "快" to "速い", "慢" to "遅い",
            "新" to "新しい", "旧" to "古い",
            "买" to "買う", "卖" to "売る",
            "打开" to "開ける", "关闭" to "閉める",
            "开始" to "始める", "结束" to "終わる",
            "等待" to "待つ", "休息" to "休む",
            "医院" to "病院", "医生" to "医者", "药" to "薬",
            "车" to "車", "飞机" to "飛行機", "火车" to "電車",
            "电话" to "電話", "电脑" to "パソコン",
            "谢谢你的帮助" to "助けてくれてありがとう",
            "你好吗" to "お元気ですか",
            "我不明白" to "わかりません",
            "请帮我" to "助けてください",
            "多少钱" to "いくらですか",
            "厕所在哪里" to "トイレはどこですか",
            "我不懂日语" to "日本語がわかりません",
            "请说慢一点" to "もう少しゆっくり話してください"
        )

        private val jaZh = zhJa.entries.associate { (k, v) -> v to k }

        private val dictionaries = mapOf(
            "zh:en" to zhEn,
            "en:zh" to enZh,
            "en:ja" to enJa,
            "ja:en" to jaEn,
            "zh:ja" to zhJa,
            "ja:zh" to jaZh
        )
    }
}
