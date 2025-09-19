package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class PaymentActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val client = OkHttpClient()
    private var hasNavigated = false // Flag to prevent double navigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true

        // Intercept URL loading
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    // Check for deep link from PayFast
                    if (it.startsWith("myapp://payment-success")) {
                        navigateToDashboard(true)
                        return true
                    } else if (it.startsWith("myapp://payment-cancel")) {
                        navigateToDashboard(false)
                        return true
                    }
                }
                return false // load other URLs normally
            }
        }

        // Example payment parameters
        val orderId = "123"
        val orderDescription = "Test Payment"
        val email = "test@example.com"
        val amount = 100.0

        initiatePayment(orderId, orderDescription, email, amount)
    }

    private fun initiatePayment(orderId: String, orderDescription: String, email: String, amount: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://testcrecheapp.onrender.com/api/payment/initiate-payment" +
                        "?orderId=${URLEncoder.encode(orderId, "UTF-8")}" +
                        "&orderDescription=${URLEncoder.encode(orderDescription, "UTF-8")}" +
                        "&email=${URLEncoder.encode(email, "UTF-8")}" +
                        "&amount=$amount"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                runOnUiThread {
                    webView.loadDataWithBaseURL(null, body, "text/html", "UTF-8", null)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun navigateToDashboard(paymentSuccess: Boolean) {
        if (hasNavigated) return // prevent double navigation
        hasNavigated = true

        // Hide the WebView to remove flicker
        webView.visibility = View.GONE

        val intent = Intent(this, AdminDashboardActivity::class.java) // or whichever dashboard
        intent.putExtra("paymentStatus", if (paymentSuccess) "SUCCESS" else "CANCELLED")
        startActivity(intent)
        finish() // close PaymentActivity
    }
}
