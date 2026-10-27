package com.orgteacher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class TutorService : AccessibilityService() {

    companion object { var instance: TutorService? = null }

    private lateinit var wm: WindowManager
    private val h = Handler(Looper.getMainLooper())

    private var overlay: OverlayView? = null
    private lateinit var overlayLp: WindowManager.LayoutParams
    private var panel: LinearLayout? = null
    private lateinit var panelLp: WindowManager.LayoutParams
    private lateinit var body: LinearLayout
    private lateinit var tTitle: TextView
    private lateinit var tInstr: TextView
    private lateinit var tHint: TextView
    private lateinit var tFb: TextView
    private lateinit var btnNext: Button

    private var li = 0
    private var si = 0
    private var mistakes = 0
    private var aiAsked = false
    private var solved = false
    private var minimized = false

    // معايرة: بكسل الشاشة = o + بكسل الصورة × s
    private var sx = 1f; private var sy = 1f; private var ox = 0f; private var oy = 0f
    private var calStage = 0
    private var calA: PointF? = null
    // نقطتين المعايرة بإحداثيات الصورة: كنوب VOLUME، وزرار CHORD/MINI KBD
    private val calPtA = PointF(155f, 178f)
    private val calPtB = PointF(2190f, 683f)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun prefs() = getSharedPreferences("cfg", MODE_PRIVATE)

    private fun screen(): Point {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm)
        return Point(dm.widthPixels, dm.heightPixels)
    }

    override fun onServiceConnected() {
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        loadCalib()
        buildOverlay()
        buildPanel()
        render()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        overlay?.let { runCatching { wm.removeView(it) } }
        panel?.let { runCatching { wm.removeView(it) } }
        instance = null
        super.onDestroy()
    }

    // ---------- المعايرة ----------
    private fun loadCalib() {
        val s = screen(); val p = prefs()
        if (p.getInt("cw", 0) == s.x && p.getInt("ch", 0) == s.y && p.contains("sx")) {
            sx = p.getFloat("sx", 1f); sy = p.getFloat("sy", 1f); ox = p.getFloat("ox", 0f); oy = p.getFloat("oy", 0f)
        } else {
            sx = s.x / IW; sy = s.y / IH; ox = 0f; oy = 0f
        }
    }

    private fun saveCalib() {
        val s = screen()
        prefs().edit().putInt("cw", s.x).putInt("ch", s.y)
            .putFloat("sx", sx).putFloat("sy", sy).putFloat("ox", ox).putFloat("oy", oy).apply()
    }

    private fun map(r: RectF) = RectF(ox + r.left * sx, oy + r.top * sy, ox + r.right * sx, oy + r.bottom * sy)

    private fun startCalib() {
        calStage = 1; calA = null
        overlay?.apply { target = null; strong = false }
        setTouchable(true)
        showCalibStage()
    }

    private fun showCalibStage() {
        val pt = if (calStage == 1) calPtA else calPtB
        overlay?.apply {
            hint = PointF(ox + pt.x * sx, oy + pt.y * sy)
            message = if (calStage == 1) "اضغط في مركز كنوب VOLUME (فوق شمال)" else "اضغط في مركز زرار CHORD/MINI KBD (تحت يمين)"
            invalidate()
        }
        tInstr.text = "معايرة ${calStage} من 2"
        tFb.text = "دوس بالظبط على مركز الزرار المطلوب في تطبيق الأورج."
    }

    private fun calibTap(x: Float, y: Float) {
        if (calStage == 1) { calA = PointF(x, y); calStage = 2; showCalibStage(); return }
        val a = calA ?: return
        val nsx = (x - a.x) / (calPtB.x - calPtA.x)
        val nsy = (y - a.y) / (calPtB.y - calPtA.y)
        if (nsx <= 0.05f || nsy <= 0.05f) { tFb.text = "النقط مش منطقية. جرّب تاني."; startCalib(); return }
        sx = nsx; sy = nsy
        ox = a.x - calPtA.x * sx; oy = a.y - calPtA.y * sy
        saveCalib()
        calStage = 0
        overlay?.apply { hint = null; message = null }
        render()
        tFb.text = "اتعايرت. كمّل الدرس."
    }

    // ---------- الطبقة ----------
    private fun buildOverlay() {
        val v = OverlayView(this)
        v.onTap = { x, y -> handleTap(x, y) }
        overlayLp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        if (android.os.Build.VERSION.SDK_INT >= 28)
            overlayLp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        wm.addView(v, overlayLp)
        overlay = v
    }

    private fun setTouchable(on: Boolean) {
        val v = overlay ?: return
        overlayLp.flags = if (on) overlayLp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        else overlayLp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        runCatching { wm.updateViewLayout(v, overlayLp) }
    }

    private fun stepTouchable() {
        val on = calStage > 0 || (li in Lessons.all.indices && si < Lessons.all[li].steps.size &&
            Lessons.all[li].steps[si].kind == Kind.PRESS && !minimized)
        setTouchable(on)
    }

    // ---------- اللوحة ----------
    private fun buildPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(8), dp(12), dp(10))
            background = GradientDrawable().apply { setColor(0xF2171C24.toInt()); cornerRadius = dp(14).toFloat(); setStroke(dp(1), 0xFF2A313C.toInt()) }
        }
        tTitle = TextView(this).apply { setTextColor(0xFFD8B36A.toInt()); textSize = 13f; text = "مدرّس الأورج (اسحب من هنا)"; setPadding(0, dp(2), 0, dp(4)) }
        root.addView(tTitle)

        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tInstr = TextView(this).apply { setTextColor(Color.WHITE); textSize = 17f; typeface = Typeface.DEFAULT_BOLD }
        tHint = TextView(this).apply { setTextColor(0xFFB8C0CC.toInt()); textSize = 12f; setPadding(0, dp(4), 0, dp(4)) }
        tFb = TextView(this).apply { setTextColor(0xFF55D6A2.toInt()); textSize = 13f }
        body.addView(tInstr); body.addView(tHint); body.addView(tFb)

        val lessons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        Lessons.all.forEachIndexed { idx, _ ->
            lessons.addView(smallBtn("درس ${idx + 1}") { li = idx; si = 0; calStage = 0; overlay?.apply { hint = null; message = null }; render() })
        }
        body.addView(lessons)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        btnNext = smallBtn("فهمت، كمّل") { advance() }
        row.addView(smallBtn("السابق") { if (si > 0) { si--; render() } })
        row.addView(btnNext)
        row.addView(smallBtn("اسأل") { askTeacher("اشرحلي الخطوة دي أكتر وقولي أعمل إيه بالظبط.") })
        row.addView(smallBtn("معايرة") { startCalib() })
        body.addView(row)
        root.addView(body)

        val minBtn = smallBtn("تصغير") {
            minimized = !minimized
            body.visibility = if (minimized) View.GONE else View.VISIBLE
            (it as Button).text = if (minimized) "فتح" else "تصغير"
            stepTouchable()
        }
        root.addView(minBtn)

        panelLp = WindowManager.LayoutParams(
            dp(310), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        panelLp.gravity = Gravity.TOP or Gravity.START
        val s = screen()
        panelLp.x = dp(12); panelLp.y = (s.y - dp(300)).coerceAtLeast(0)

        var sx0 = 0f; var sy0 = 0f; var px0 = 0; var py0 = 0
        tTitle.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { sx0 = e.rawX; sy0 = e.rawY; px0 = panelLp.x; py0 = panelLp.y }
                MotionEvent.ACTION_MOVE -> {
                    panelLp.x = px0 + (e.rawX - sx0).toInt(); panelLp.y = py0 + (e.rawY - sy0).toInt()
                    wm.updateViewLayout(root, panelLp)
                }
            }
            true
        }
        wm.addView(root, panelLp)
        panel = root
    }

    private fun smallBtn(t: String, onClick: (View) -> Unit) = Button(this).apply {
        text = t; textSize = 11f; isAllCaps = false
        minHeight = 0; minimumHeight = 0; minWidth = 0; minimumWidth = 0
        setPadding(dp(8), dp(4), dp(8), dp(4))
        setOnClickListener(onClick)
    }

    // ---------- الدرس ----------
    private fun cur(): Step? = Lessons.all[li].steps.getOrNull(si)

    private fun render() {
        val L = Lessons.all[li]
        mistakes = 0; aiAsked = false; solved = false
        val st = cur()
        if (st == null) {
            tTitle.text = "${L.title}  •  خلّصت"
            tInstr.text = "خلّصت الدرس! اختار الدرس اللي بعده."
            tHint.text = ""; tFb.text = ""
            overlay?.apply { target = null; invalidate() }
            btnNext.visibility = View.GONE
            stepTouchable(); return
        }
        tTitle.text = "${L.title}  •  ${si + 1}/${L.steps.size}  (اسحب من هنا)"
        tInstr.text = st.title
        tHint.text = "${st.hand}\nالفايدة: ${st.why}\nنصيحة: ${st.tip}"
        tFb.text = ""; tFb.setTextColor(0xFF55D6A2.toInt())
        btnNext.visibility = if (st.kind == Kind.INFO) View.VISIBLE else View.GONE
        overlay?.apply {
            target = st.rect()?.let { map(it) }
            label = if (st.kind == Kind.PRESS) "اضغط هنا" else "بص هنا"
            strong = false; invalidate()
        }
        stepTouchable()
    }

    private fun advance() { si++; render() }

    private fun hitTest(x: Float, y: Float): Btn? =
        Buttons.all.values.filter { map(it.r).contains(x, y) && it.id != "screen" }
            .minByOrNull { it.r.width() * it.r.height() }

    private fun handleTap(x: Float, y: Float) {
        if (calStage > 0) { calibTap(x, y); return }
        val st = cur() ?: run { forward(x, y); return }
        if (st.kind != Kind.PRESS || solved) { forward(x, y); return }
        val hit = hitTest(x, y)
        if (hit == null) { forward(x, y); return }
        if (hit.id == st.target) {
            solved = true
            forward(x, y)
            tFb.setTextColor(0xFF55D6A2.toInt())
            tFb.text = "تمام. «${hit.name}»: ${hit.desc}"
            h.postDelayed({ advance() }, 1100)
        } else {
            // غلط: منمرّرش الضغطة للأورج، ونصحّح
            val T = Buttons.all[st.target]!!
            val msg = st.wrong[hit.id] ?: "ده زرار «${hit.name}». ${hit.desc} المطلوب دلوقتي: «${T.name}»."
            mistakes++
            tFb.setTextColor(0xFFFF7A66.toInt())
            tFb.text = msg
            if (mistakes >= 2) overlay?.apply { strong = true; label = "هنا بالظبط"; invalidate() }
            if (mistakes >= 3 && !aiAsked) {
                aiAsked = true
                askTeacher("غلطت $mistakes مرات. آخر زرار ضغطته غلط هو «${hit.name}». ساعدني.")
            }
        }
    }

    /** بيمرّر الضغطة للتطبيق اللي تحت (الأورج) عن طريق إيماءة. */
    private fun forward(x: Float, y: Float) {
        setTouchable(false)
        h.postDelayed({
            val path = Path().apply { moveTo(x, y) }
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build()
            val ok = dispatchGesture(g, object : GestureResultCallback() {
                override fun onCompleted(d: GestureDescription?) { stepTouchable() }
                override fun onCancelled(d: GestureDescription?) { stepTouchable() }
            }, null)
            if (!ok) stepTouchable()
        }, 40)
    }

    private fun askTeacher(question: String) {
        val st = cur() ?: return
        tFb.setTextColor(0xFFFFFFFF.toInt()); tFb.text = "المدرّس بيفكر..."
        val ctx = "الدرس: ${Lessons.all[li].title}. الخطوة: ${st.title}. الفايدة: ${st.why}. الأخطاء: $mistakes."
        Teacher.ask(prefs().getString("apikey", "") ?: "", ctx, question) { ans ->
            h.post { tFb.text = ans ?: "${st.why}\nنصيحة: ${st.tip}\n(للردود الذكية حط مفتاح Claude في الإعدادات.)" }
        }
    }
}
