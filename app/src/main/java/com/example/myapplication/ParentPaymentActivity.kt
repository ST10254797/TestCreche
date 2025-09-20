package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder

class ParentPaymentActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var childId: String
    private lateinit var feeId: String
    private lateinit var parentEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_parent_payment)

        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve info passed from ParentDashboardActivity
        childId = intent.getStringExtra("childId") ?: ""
        feeId = intent.getStringExtra("feeId") ?: ""
        parentEmail = intent.getStringExtra("parentEmail") ?: ""

        Log.d("ParentPaymentActivity", "Received childId=$childId, feeId=$feeId, parentEmail=$parentEmail")

        // Initialize WebView
        webView = findViewById(R.id.wvPayment)
        webView.settings.javaScriptEnabled = true
        WebView.setWebContentsDebuggingEnabled(true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d("ParentPaymentActivity", "Navigating to: $url")
                url?.let {
                    when {
                        it.startsWith("myapp://payment-success") -> {
                            Log.d("ParentPaymentActivity", "Detected success redirect")
                            navigateBack(true)
                            return true
                        }
                        it.startsWith("myapp://payment-cancel") -> {
                            Log.d("ParentPaymentActivity", "Detected cancel redirect")
                            navigateBack(false)
                            return true
                        }
                    }
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("ParentPaymentActivity", "Finished loading page: $url")
            }
        }

        // Load child and fee info first
        loadChildAndFeeData()

        // Handle back gestures
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("ParentPaymentActivity", "Back pressed, cancelling payment")
                navigateBack(false)
            }
        })
    }

    private fun loadChildAndFeeData() {
        val db = FirebaseFirestore.getInstance()

        // Fetch child info
        val childRef = db.collection("Child").document(childId)
        childRef.get().addOnSuccessListener { childSnapshot ->
            val firstName = childSnapshot.getString("firstName") ?: ""
            val lastName = childSnapshot.getString("lastName") ?: ""
            findViewById<TextView>(R.id.tvChildName).text = "$firstName $lastName"

            // Fetch fee info
            val feeRef = childRef.collection("Fees").document(feeId)
            feeRef.get().addOnSuccessListener { feeSnapshot ->
                if (feeSnapshot.exists()) {
                    val description = feeSnapshot.getString("description") ?: ""
                    val originalAmount = feeSnapshot.getDouble("amount") ?: 0.0

                    findViewById<TextView>(R.id.tvFeeDescription).text = description

                    val rgPaymentType = findViewById<RadioGroup>(R.id.rgPaymentType)
                    val tvAmount = findViewById<TextView>(R.id.tvAmount)

                    fun updateAmountDisplay() {
                        val paymentType = when (rgPaymentType.checkedRadioButtonId) {
                            R.id.rbFullPayment -> "FULL"
                            R.id.rbMonthlyPayment -> "MONTHLY"
                            else -> "FULL"
                        }
                        val displayAmount = if (paymentType == "MONTHLY") {
                            Math.round(originalAmount / 10 * 100.0) / 100.0
                        } else originalAmount

                        // Update TextView
                        tvAmount.text = "R %.2f".format(displayAmount)

                        // Reload WebView with updated amount and payment type
                        initiateFeePayment(displayAmount)
                    }

                    // Initial display and WebView load
                    updateAmountDisplay()

                    // Listen for changes in payment type
                    rgPaymentType.setOnCheckedChangeListener { _, _ ->
                        updateAmountDisplay()
                    }

                } else {
                    Log.e("ParentPaymentActivity", "Fee not found")
                    Toast.makeText(this, "Fee not found", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Log.e("ParentPaymentActivity", "Failed to fetch child data: ${it.message}")
            Toast.makeText(this, "Failed to load child info", Toast.LENGTH_SHORT).show()
        }
    }


    private fun initiateFeePayment(amount: Double) {
        // Get selected payment type
        val rgPaymentType = findViewById<RadioGroup>(R.id.rgPaymentType)
        val selectedTypeId = rgPaymentType.checkedRadioButtonId
        val paymentType = when (selectedTypeId) {
            R.id.rbFullPayment -> "FULL"
            R.id.rbMonthlyPayment -> "MONTHLY"
            else -> "FULL"
        }

        val url = "https://testcrecheapp.onrender.com/api/payment/initiate-school-fee-payment" +
                "?childId=${URLEncoder.encode(childId, "UTF-8")}" +
                "&feeId=${URLEncoder.encode(feeId, "UTF-8")}" +
                "&email=${URLEncoder.encode(parentEmail, "UTF-8")}" +
                "&paymentType=${URLEncoder.encode(paymentType, "UTF-8")}" +
                "&amount=${URLEncoder.encode(amount.toString(), "UTF-8")}"

        Log.d("ParentPaymentActivity", "Loading payment URL: $url")
        webView.loadUrl(url)
    }

    private fun navigateBack(paymentSuccess: Boolean) {
        Log.d("ParentPaymentActivity", "Navigating back with paymentSuccess=$paymentSuccess")

        Toast.makeText(
            this,
            if (paymentSuccess) "Payment successful!" else "Payment cancelled",
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
