package com.example.ussd_mmibruteforcer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.UssdResponseCallback
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var responder: Responder

    private lateinit var progress: ProgressBar
    private lateinit var progressText : TextView
    private lateinit var textView: TextView
    private lateinit var current: TextView
    private lateinit var button: Button
    private lateinit var mmi : MMI

    private var allowed = false
    private var started = AtomicBoolean()

    companion object {
        const val PERMISSION_CALL_PHONE_CODE = 1
        const val TAG = "MAIN_ACTIVITY"
        const val MAX_DIGITS = 6
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mmi = MMI(MAX_DIGITS)
        progress = findViewById(R.id.progress)
        progress.visibility = View.INVISIBLE
        progressText = findViewById(R.id.progressText)
        progressText.visibility = View.INVISIBLE
        current = findViewById(R.id.current)
        current.visibility = View.INVISIBLE
        textView = findViewById(R.id.text)
        responder = Responder(this, textView, mmi.total, progress, progressText, current)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        handlerThread = HandlerThread("Responder handler thread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        button = findViewById(R.id.start)
        button.setOnClickListener {
            startStop()
        }
    }

    private fun startStop() {
        if (!started.get()) {
            checkPermission()
            started.set(allowed)
        } else
            started.set(false)
        if (started.get()) {
            progress.visibility = View.VISIBLE
            progressText.visibility = View.VISIBLE
            current.visibility = View.VISIBLE
            button.text = "Stop"
            BruteforceThread(started, telephonyManager, responder, handler, mmi).start()
        } else {
            progress.visibility = View.INVISIBLE
            progressText.visibility = View.INVISIBLE
            current.visibility = View.INVISIBLE
            button.text = "Start"
            responder.reset()
            textView.text = "Press start to bruteforce"
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), PERMISSION_CALL_PHONE_CODE)
        else
            allowed = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CALL_PHONE_CODE) {
            allowed = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            val print = if (allowed) "Permission granted" else "Permission denied"
            Toast.makeText(this, print, Toast.LENGTH_SHORT).show()
        }
    }

    class BruteforceThread(
        private val started: AtomicBoolean,
        private val telephonyManager: TelephonyManager,
        private val responder: Responder,
        private val handler: Handler,
        private val mmi : MMI
    ): Thread() {

        @SuppressLint("MissingPermission")
        override fun run() {
            val iterator = mmi.sequence.iterator()
            while (iterator.hasNext() && started.get())
                telephonyManager.sendUssdRequest(iterator.next(), responder, handler)
        }
    }

    class Responder(
        private val activity: Activity,
        private val textView: TextView,
        private val total: BigDecimal,
        private val progressBar: ProgressBar,
        private val progressText: TextView,
        private val current: TextView,
        private val sb: StringBuilder = StringBuilder()
    ) : UssdResponseCallback() {
        private var i = 0.toBigDecimal()

        companion object {
            const val ERROR = -1
        }

        override fun onReceiveUssdResponse(telephonyManager: TelephonyManager?, request: String?, response: CharSequence?) {
            super.onReceiveUssdResponse(telephonyManager, request, response)
            Log.e(TAG, "SUCCESS $request with response: $response")
            sb.append(request)
                .append("\t:\t")
                .append(response)
                .appendLine()
            if (request != null)
                update(request)
        }

        override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager?, request: String?, failureCode: Int) {
            super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode)
            if (failureCode != ERROR)
                Log.e(TAG, "FAILED $request with code: $failureCode")
            if (request != null)
                update(request)
        }

        private fun update(code: String) = activity.runOnUiThread {
            i = i.inc()
            val percent = i.divide(total, 2, RoundingMode.HALF_UP).times(100.toBigDecimal())
            progressText.text = "$i/$total\n ($percent %)"
            progressBar.progress = percent.toInt()
            current.text = code
            textView.text = sb.toString()
        }

        fun reset() = sb.clear()
    }
}