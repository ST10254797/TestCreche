package com.example.myapplication

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class PaymentActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        // Example parameters
        val orderId = "123"
        val orderDescription = "Test Payment"
        val email = "test@example.com"
        val amount = 100.0

        initiatePayment(orderId, orderDescription, email, amount)
    }

    private fun initiatePayment(orderId: String, orderDescription: String, email: String, amount: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Encode parameters to safely include in URL
                val url = "https://testcrecheapp.onrender.com/api/payment/initiate-payment" +
                        "?orderId=${URLEncoder.encode(orderId, "UTF-8")}" +
                        "&orderDescription=${URLEncoder.encode(orderDescription, "UTF-8")}" +
                        "&email=${URLEncoder.encode(email, "UTF-8")}" +
                        "&amount=$amount"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                // The endpoint returns raw HTML form
                runOnUiThread {
                    webView.loadDataWithBaseURL(null, body, "text/html", "UTF-8", null)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
