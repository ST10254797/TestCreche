package com.example.myapplication

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class PaymentActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val client = OkHttpClient()
    private var hasNavigated = false

    private var childId = ""
    private var description = ""
    private var email = ""
    private var paymentType = "FULL" // Admin default type

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        WebView.setWebContentsDebuggingEnabled(true)
        webView.settings.javaScriptEnabled = true

        // Get data from intent
        childId = intent.getStringExtra("childId") ?: ""
        description = intent.getStringExtra("description") ?: "School Fee"
        email = intent.getStringExtra("email") ?: ""

        if (childId.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Missing required data for payment", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        showFeeInputDialog()

        // WebView URL interception for success/cancel
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    if (it.startsWith("myapp://payment-success")) {
                        navigateToDashboard(true)
                        return true
                    } else if (it.startsWith("myapp://payment-cancel")) {
                        navigateToDashboard(false)
                        return true
                    }
                }
                return false
            }
        }
    }

    private fun showFeeInputDialog() {
        val inputAmount = EditText(this)
        inputAmount.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        inputAmount.hint = "Enter amount"

        val inputDescription = EditText(this)
        inputDescription.inputType = InputType.TYPE_CLASS_TEXT
        inputDescription.hint = "Description (optional)"

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.addView(inputAmount)
        layout.addView(inputDescription)
        layout.setPadding(50, 40, 50, 10)

        AlertDialog.Builder(this)
            .setTitle("Create New Fee")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("Proceed") { _: DialogInterface, _: Int ->
                val amount = inputAmount.text.toString().toDoubleOrNull()
                val desc = inputDescription.text.toString().ifEmpty { description }

                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                    showFeeInputDialog()
                } else {
                    createNewFeeForChild(childId, amount, desc)
                }
            }
            .setNegativeButton("Cancel") { _: DialogInterface, _: Int ->
                finish()
            }
            .show()
    }

    private fun createNewFeeForChild(childId: String, amount: Double, description: String) {
        try {
            val db = FirebaseFirestore.getInstance()

            val newFee = hashMapOf(
                "amount" to amount,
                "description" to description,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "amountPaid" to 0.0,
                "paymentStatus" to "PENDING",
                "paymentType" to paymentType
            )

            db.collection("Child")
                .document(childId)
                .collection("Fees")
                .add(newFee)
                .addOnSuccessListener { docRef ->
                    Toast.makeText(this@PaymentActivity, "New fee created successfully", Toast.LENGTH_SHORT).show()
                    // Initiate payment using the newly created fee ID
                    initiatePayment(amount, docRef.id)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this@PaymentActivity, "Failed to create fee: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Toast.makeText(this@PaymentActivity, "Error creating fee: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun initiatePayment(amount: Double, feeId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val createPaymentUrl =
                    "https://testcrecheapp.onrender.com/api/payment/create-payment-request" +
                            "?childId=${URLEncoder.encode(childId, "UTF-8")}" +
                            "&feeId=${URLEncoder.encode(feeId, "UTF-8")}" +
                            "&email=${URLEncoder.encode(email, "UTF-8")}" +
                            "&amount=$amount" +
                            "&paymentType=${URLEncoder.encode(paymentType, "UTF-8")}"

                val request = Request.Builder()
                    .url(createPaymentUrl)
                    .post(okhttp3.FormBody.Builder().build())
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val payFastUrl =
                        "https://testcrecheapp.onrender.com/api/payment/school-fee-payment-page" +
                                "?childId=${URLEncoder.encode(childId, "UTF-8")}" +
                                "&feeId=${URLEncoder.encode(feeId, "UTF-8")}" +
                                "&amount=$amount" +
                                "&email=${URLEncoder.encode(email, "UTF-8")}"

                    val payFastRequest = Request.Builder().url(payFastUrl).build()
                    val payFastResponse = client.newCall(payFastRequest).execute()
                    val body = payFastResponse.body?.string() ?: ""

                    runOnUiThread {
                        webView.loadDataWithBaseURL(null, body, "text/html", "UTF-8", null)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@PaymentActivity,
                            "Failed to create payment request. Server returned ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@PaymentActivity,
                        "Payment initiation failed",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun navigateToDashboard(paymentSuccess: Boolean) {
        if (hasNavigated) return
        hasNavigated = true
        webView.visibility = View.GONE
        val intent = Intent(this, AdminDashboardActivity::class.java)
        intent.putExtra("paymentStatus", if (paymentSuccess) "SUCCESS" else "CANCELLED")
        startActivity(intent)
        finish()
    }
}
