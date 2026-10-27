package com.orgteacher

import android.graphics.RectF

/** أبعاد صورة الأورج المرجعية اللي اتحددت عليها الإحداثيات */
const val IW = 2340f
const val IH = 1080f

data class Btn(val id: String, val name: String, val desc: String, val r: RectF)

object Buttons {
    val all = LinkedHashMap<String, Btn>()

    private fun d(id: String, n: String, x: Float, y: Float, w: Float, h: Float, desc: String) {
        all[id] = Btn(id, n, desc, RectF(x, y, x + w, y + h))
    }

    init {
        d("styleSelect", "STYLE SELECT", 1995f, 652f, 128f, 50f, "بتختار منه الإيقاع (مصري، شامي، خليجي...). كل ستايل معاه إيقاع وآلات جاهزة.")
        d("startstop", "START/STOP", 1115f, 425f, 135f, 58f, "بيشغّل الإيقاع ويوقفه.")
        d("tempo", "TEMPO", 1848f, 243f, 128f, 42f, "بتتحكم في سرعة الإيقاع.")
        d("lock", "LOCK", 1707f, 243f, 128f, 42f, "بيثبّت السرعة لما تبدّل الستايل.")
        d("octave", "OCTAVE", 1990f, 243f, 128f, 42f, "بيرفع أو ينزّل الكيبورد أوكتاف.")
        d("transpose", "TRANSPOSE", 2130f, 243f, 128f, 42f, "بيغيّر المقام كله لفوق أو لتحت.")
        d("splitmix", "SPLIT/MIX", 1550f, 243f, 130f, 42f, "بيقسم الكيبورد لنصين بصوتين مختلفين.")
        d("chordLower", "CHORD SCAN: LOWER", 1707f, 340f, 128f, 40f, "بيخلي الأورج يقرا الكورد من الإيد الشمال بس.")
        d("chordUpper", "CHORD SCAN: UPPER", 1848f, 340f, 128f, 40f, "بيخلي الأورج يقرا الكورد من الجزء اليمين.")
        d("chordMute", "CHORD MUTE", 1990f, 340f, 128f, 40f, "بيسكّت آلات الكورد وتفضل الدرمز.")
        d("drumMute", "DRUM MUTE", 2130f, 340f, 128f, 40f, "بيسكّت الدرمز وتفضل الآلات.")
        d("chordMemory", "CHORD MEMORY", 1550f, 340f, 130f, 40f, "بيفضّل يشغّل آخر كورد بعد ما ترفع إيدك.")
        d("autoFill", "AUTO FILL", 90f, 438f, 128f, 42f, "بيعمل Fill تلقائي كل ما تغيّر Variation.")
        d("volume", "VOLUME", 95f, 128f, 122f, 98f, "مستوى الصوت العام.")
        d("balance", "BALANCE", 95f, 272f, 122f, 98f, "التوازن بين صوت الإيقاع وصوت الكيبورد.")
        d("fade", "FADE IN/OUT", 1380f, 438f, 66f, 50f, "بيطلّع أو بيخفّت الصوت تدريجي.")
        d("tap", "TAP TEMPO", 1285f, 438f, 66f, 50f, "بتدوس عليه بسرعة الإيقاع اللي عايزه.")
        d("screen", "شاشة الستايل", 800f, 25f, 745f, 360f, "بتعرض الستايل اللي شغال دلوقتي.")
        for (i in 0 until 3) d("intro${i + 1}", "INTRO ${i + 1}", 90f + i * 140f, 530f, 128f, 70f, "مقدمة قبل ما الإيقاع يدخل.")
        for (i in 0 until 4) d("var${i + 1}", "VARIATION ${i + 1}", 537f + i * 140f, 530f, 128f, 70f, "الإيقاع الأساسي. 1 هادي و4 أقوى.")
        for (i in 0 until 4) d("fill${i + 1}", "FILL ${i + 1}", if (i == 3) 1532f else 1118f + i * 140f, 530f, 128f, 70f, "جملة درمز سريعة بتنقلك بين الأجزاء.")
        d("brk", "BREAK", 1690f, 530f, 128f, 70f, "وقفة قصيرة في الإيقاع.")
        for (i in 0 until 3) d("end${i + 1}", "ENDING ${i + 1}", 1852f + i * 140f, 530f, 128f, 70f, "نهاية الأغنية. الإيقاع بيقف لوحده بعدها.")
        for (i in 0 until 4) d("kbd${i + 1}", "KEYBOARD SET ${i + 1}", 1465f + i * 138f, 438f, 130f, 42f, "مجموعة أصوات كاملة بتبدّلها بضغطة.")
        for (i in 0 until 4) d("pad${i + 1}", "PAD ${i + 1}", 343f + i * 139f, 438f, 128f, 42f, "جمل وأصوات جاهزة.")
    }
}

enum class Kind { PRESS, INFO }

data class Step(
    val kind: Kind,
    val target: String?,
    val zone: RectF?,
    val title: String,
    val hand: String,
    val why: String,
    val tip: String,
    val wrong: Map<String, String> = emptyMap()
) {
    fun rect(): RectF? = zone ?: target?.let { Buttons.all[it]?.r }
}

data class Lesson(val title: String, val steps: List<Step>)

private fun p(t: String, title: String, hand: String, why: String, tip: String, wrong: Map<String, String> = emptyMap()) =
    Step(Kind.PRESS, t, null, title, hand, why, tip, wrong)

private fun i(t: String?, zone: RectF?, title: String, hand: String, why: String, tip: String) =
    Step(Kind.INFO, t, zone, title, hand, why, tip)

object Lessons {
    private val pianoLeft = RectF(0f, 718f, 1170f, 1080f)
    private val pianoRight = RectF(1170f, 718f, 2340f, 1080f)
    private val right = "سبّابة إيدك اليمين"

    val all = listOf(
        Lesson("التعرّف على الأورج", listOf(
            i("screen", null, "ابدأ من الشاشة", "بص بس", "الشاشة بتوريك الستايل اللي شغال.", "بص عليها قبل أي حاجة."),
            p("styleSelect", "اضغط STYLE SELECT", right, "بيفتح قايمة الإيقاعات.", "ابدأ بستايل مصري هادي."),
            p("startstop", "اضغط START/STOP", right, "بيشغّل الإيقاع.", "ركّز على إحساس الضربات.",
                mapOf("tempo" to "السرعة نظبطها بعد ما الإيقاع يشتغل. دوس START/STOP الأول.")),
            p("tempo", "غيّر السرعة: TEMPO", right, "بتغيّر سرعة الإيقاع.", "ابدأ بسرعة بطيئة."),
            p("startstop", "اضغط START/STOP تاني", right, "بيوقف الإيقاع.", "في الأغاني الحقيقية بتنهي بـ ENDING."),
            p("kbd2", "جرّب KEYBOARD SET 2", right, "بيبدّل مجموعة الأصوات كلها.", "جهّز Set لكل جزء في الأغنية.")
        )),
        Lesson("أزرار الأداء", listOf(
            p("intro1", "ابدأ بـ INTRO 1", right, "بيدخّل الأغنية بمقدمة.", "اضغطه قبل أول ضربة بوقت قليل.",
                mapOf("var1" to "Variation هو الإيقاع الأساسي، لكن الأغنية بتبدأ بـ Intro الأول.")),
            p("var1", "اضغط VARIATION 1", right, "الإيقاع الأساسي الهادي للكوبليه.", "1 و2 للكوبليه و3 و4 للكورس.",
                mapOf("fill1" to "Fill قبل ما نغيّر Variation. لسه ما غيّرناش، ثبّت على Variation 1.")),
            p("fill1", "اعمل FILL 1 قبل التغيير", right, "بيمهّد للانتقال.", "اضغطه في آخر ميزة قبل التغيير.",
                mapOf("var2" to "استنى! الأول Fill وبعدين Variation 2.")),
            p("var2", "اضغط VARIATION 2", right, "إيقاع أقوى شوية.", "غيّر Variation مع كل جزء جديد."),
            p("brk", "اضغط BREAK", right, "وقفة قصيرة بتدي شد.", "استخدمه مرة واحدة مش كل شوية."),
            p("end1", "اختم بـ ENDING 1", right, "بيقفل الأغنية بنهاية مظبوطة.", "اضغطه قبل آخر ميزة بوقت كافي.",
                mapOf("startstop" to "لو وقفت بـ START/STOP الإيقاع هيقطع فجأة. اختم بـ Ending."))
        )),
        Lesson("إيدك الشمال واليمين", listOf(
            i(null, pianoLeft, "النص الشمال للكورد", "إيدك الشمال", "الإيد الشمال بتعزف الكوردات وبتتحكم في الأرانج.", "دوس «فهمت» لما تخلص قراءة."),
            p("chordLower", "اضغط CHORD SCAN: LOWER", right, "بيقرا الكورد من الإيد الشمال بس.", "من غيره ممكن الأورج يلخبط بين الكورد واللحن.",
                mapOf("chordUpper" to "ده Upper. عايزين Lower.")),
            i(null, pianoLeft, "اعزف كورد دو: دو + مي + صول مع بعض", "إيدك الشمال", "كورد دو ماجور 3 نوتات.", "الإبهام والوسطى والخنصر. اضغط «فهمت» لما تعزف."),
            i(null, pianoRight, "اعزف أي 3 نوتات باللحن", "إيدك اليمين", "النص اليمين للحن.", "نوتة نوتة، بالراحة. اضغط «فهمت» لما تخلص."),
            p("startstop", "شغّل الإيقاع واعزف عليه", right, "كل حاجة جاهزة دلوقتي.", "أول عزف حقيقي ليك.")
        ))
    )
}
