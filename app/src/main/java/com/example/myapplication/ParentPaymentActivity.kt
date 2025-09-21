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
import com.google.firebase.firestore.ListenerRegistration
import java.net.URLEncoder

class ParentPaymentActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var childId: String
    private lateinit var feeId: String
    private lateinit var parentEmail: String
    private var feeListener: ListenerRegistration? = null
    private var currentAmount: Double = 0.0 // Admin-set fee amount
    private var selectedPaymentType: String = "FULL" // Track parent selection

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

        // Retrieve info passed from previous activity
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
                            Toast.makeText(this@ParentPaymentActivity, "Waiting for confirmation...", Toast.LENGTH_SHORT).show()
                            return true
                        }
                        it.startsWith("myapp://payment-cancel") -> {
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
        val childRef = db.collection("Child").document(childId)

        childRef.get().addOnSuccessListener { childSnapshot ->
            val firstName = childSnapshot.getString("firstName") ?: ""
            val lastName = childSnapshot.getString("lastName") ?: ""
            findViewById<TextView>(R.id.tvChildName).text = "$firstName $lastName"

            // Fetch the latest fee document
            val feesRef = childRef.collection("Fees")
            feesRef.orderBy("createdAt").limitToLast(1)
                .get()
                .addOnSuccessListener { feeSnapshot ->
                    val feeDoc = feeSnapshot.documents.firstOrNull()
                    if (feeDoc != null) {
                        feeId = feeDoc.id
                        val description = feeDoc.getString("description") ?: ""
                        currentAmount = feeDoc.getDouble("amount") ?: 0.0

                        findViewById<TextView>(R.id.tvFeeDescription).text = description

                        val rgPaymentType = findViewById<RadioGroup>(R.id.rgPaymentType)
                        val tvAmount = findViewById<TextView>(R.id.tvAmount)

                        // Function to update amount display based on selection
                        fun updateAmountDisplay(): Double {
                            selectedPaymentType = when (rgPaymentType.checkedRadioButtonId) {
                                R.id.rbFullPayment -> "FULL"
                                R.id.rbMonthlyPayment -> "MONTHLY"
                                else -> "FULL"
                            }

                            val displayAmount = if (selectedPaymentType == "MONTHLY") {
                                Math.round(currentAmount / 10 * 100.0) / 100.0
                            } else currentAmount

                            tvAmount.text = "R %.2f".format(displayAmount)
                            return displayAmount
                        }

                        // Initial display and WebView load
                        val initialAmount = updateAmountDisplay()
                        initiateFeePayment(initialAmount)

                        // Update when radio changes
                        rgPaymentType.setOnCheckedChangeListener { _, _ ->
                            val amount = updateAmountDisplay()
                            initiateFeePayment(amount)
                        }

                    } else {
                        Log.e("ParentPaymentActivity", "No fees found for child $childId")
                        Toast.makeText(this, "No fees available", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Log.e("ParentPaymentActivity", "Failed to fetch fee: ${e.message}")
                    Toast.makeText(this, "Failed to load fee info", Toast.LENGTH_SHORT).show()
                }

        }.addOnFailureListener {
            Log.e("ParentPaymentActivity", "Failed to fetch child data: ${it.message}")
            Toast.makeText(this, "Failed to load child info", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initiateFeePayment(amount: Double) {
        val url = "https://testcrecheapp.onrender.com/api/payment/initiate-school-fee-payment" +
                "?childId=${URLEncoder.encode(childId, "UTF-8")}" +
                "&feeId=${URLEncoder.encode(feeId, "UTF-8")}" +
                "&email=${URLEncoder.encode(parentEmail, "UTF-8")}" +
                "&amount=${URLEncoder.encode(amount.toString(), "UTF-8")}" +
                "&paymentType=${URLEncoder.encode(selectedPaymentType, "UTF-8")}" // Pass selected type

        Log.d("ParentPaymentActivity", "Loading payment URL: $url")
        webView.loadUrl(url)

        listenForPaymentStatus()
    }

    private fun listenForPaymentStatus() {
        val db = FirebaseFirestore.getInstance()
        val feeRef = db.collection("Child").document(childId).collection("Fees").document(feeId)

        feeListener?.remove()
        feeListener = feeRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("ParentPaymentActivity", "Error listening for payment updates", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val paymentStatus = snapshot.getString("paymentStatus")
                val amountPaid = snapshot.getDouble("amountPaid")
                val paymentType = snapshot.getString("paymentType")

                Log.d("ParentPaymentActivity", "Payment status from Firestore: $paymentStatus")

                if (paymentStatus == "PAID") {
                    Toast.makeText(
                        this,
                        "Payment successful! Amount: R$amountPaid, Type: $paymentType",
                        Toast.LENGTH_LONG
                    ).show()
                    navigateBack(true)
                }
            }
        }
    }

    private fun navigateBack(paymentSuccess: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        feeListener?.remove()
    }
}
