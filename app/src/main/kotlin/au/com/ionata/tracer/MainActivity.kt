package au.com.ionata.tracer

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import au.com.ionata.confighelper.NeedsGMSActivity
import au.com.ionata.tracer.BuildConfig.TRACER_PASS
import au.com.ionata.tracer.BuildConfig.TRACER_USER
import au.com.ionata.tracerlib.TourTracer
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick


interface ViewBinder<in T: Activity> {
  fun bind(t: T): View
  fun unbind(t: T)
}


inline fun ViewManager.heading(text: CharSequence?, init: TextView.() -> Unit): TextView {
  return textView(text) {
    textSize = 30f
    typeface = Typeface.DEFAULT_BOLD
    topPadding = dip(10)
    init()
  }
}
inline fun ViewManager.heading(text: CharSequence?): TextView = heading(text) {}


class MainActivity: NeedsGMSActivity(), AnkoLogger {
  // val username = "chris+tracertest@ionata.com.au"
  val username = TRACER_USER
  val password = TRACER_PASS
  val test_api = true
  val TT get() = TourTracer.init(applicationContext, username, password, test_api)

  var touristLabel: TextView? = null
  var tokenLabel: TextView? = null
  var locLabel: TextView? = null
  var locULabel: TextView? = null
  var registerBtn: Button? = null
  var refreshBtn: Button? = null
  var pushBtn: Button? = null
  var touristInfo: LinearLayout? = null

  fun startTracking() {if (TT.isRegistered) TT.startTracking(this)}

  private val layout = MainActivityUI()

  override fun onCreate(savedInstanceState: Bundle?) {
    try {
      super.onCreate(savedInstanceState)
    } catch(ex: Exception) {}
    setContentView(layout.bind(this))
    updateUI()
    startTracking()
  }

  fun register() {
    TT.registerDevice(2, object: TourTracer.APICallback {
      override fun onResult(error: Throwable?) {
        warn("Attempted registration")
        if (error != null)
          warn("Registration failed: $error")
        updateUI()
        startTracking()
      }
    })
  }

  fun toggleRego() = if (TT.isRegistered) unregister() else register()

  fun push() {
    if (TT.isRegistered) {
      TT.pushLocations(object: TourTracer.APICallback {
        override fun onResult(error: Throwable?) {
          warn("Attempted submission")
          if (error != null)
            warn("Submission failed: $error")
          updateUI()
        }
      })
    }
  }

  fun updateUI() = runOnUiThread {
    touristLabel?.text = TT.touristId
    tokenLabel?.text = TT.submitToken
    registerBtn?.text = if (TT.isRegistered) "Unregister" else "Register"
    touristInfo?.visibility = if (TT.isRegistered) VISIBLE else GONE
    locLabel?.text = if (TT.isRegistered) "${TT.locationCount}" else ""
    locULabel?.text = if (TT.isRegistered) "${TT.locationUnsubmittedCount}" else ""
    pushBtn?.visibility = if (TT.isRegistered) VISIBLE else GONE
  }

  fun unregister() {
    TT.reset()
    updateUI()
  }
}

class MainActivityUI: ViewBinder<MainActivity> {
  override fun bind(t: MainActivity): View = t.UI {
    verticalLayout {
      padding = dip(30)
      t.touristInfo = verticalLayout {
        heading("Tourist ID:")
        t.touristLabel = textView()
        heading("Submission token:")
        t.tokenLabel = textView()
        heading("All locations:")
        t.locLabel = textView()
        heading("Unsubmitted locations:")
        t.locULabel = textView()
      }
      t.pushBtn = button("Push") {onClick {t.push()}}
      t.refreshBtn = button("Refresh") {onClick {t.updateUI()}}
      t.registerBtn = button("Register") {onClick {t.toggleRego()}}
    }
  }.view.applyRecursively {
    if (it is TextView) it.textSize = 24f
    if (it is Button)
      (it.layoutParams as MarginLayoutParams).verticalMargin = it.dip(10)
  }

  override fun unbind(t: MainActivity) {
    t.touristLabel = null
    t.registerBtn = null
  }
}
