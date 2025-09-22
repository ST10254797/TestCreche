package com.example.myapplication

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class PaymentActivity : AppCompatActivity() {

    private lateinit var spinnerChild: Spinner
    private lateinit var editAmount: EditText
    private lateinit var editDescription: EditText
    private lateinit var btnCreateFee: Button

    private var childId = "" // Firestore ID of selected child

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Bind UI elements
        spinnerChild = findViewById(R.id.spinnerChild)
        editAmount = findViewById(R.id.editAmount)
        editDescription = findViewById(R.id.editDescription)
        btnCreateFee = findViewById(R.id.btnCreateFee)

        // Load children into spinner
        loadChildren()

        // Create fee when button clicked
        btnCreateFee.setOnClickListener {
            val amount = editAmount.text.toString().toDoubleOrNull()
            val description = editDescription.text.toString().ifEmpty { "School Fee" }

            // Validate input
            if (childId.isEmpty() || amount == null || amount <= 0) {
                Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Use Firestore ID directly
            createNewFeeForChild(childId, amount, description)
        }
    }

    private fun loadChildren() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Child")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "No children found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Map each child to Pair(Firestore ID, Full Name)
                val childList = snapshot.documents.map { doc ->
                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    val fullName = "$firstName $lastName".trim().ifEmpty { "Unnamed Child" }
                    Pair(doc.id, fullName)
                }

                // Populate spinner with full names
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    childList.map { it.second } // Only show names
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerChild.adapter = adapter

                // Track selected child by Firestore ID
                spinnerChild.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: android.view.View?,
                        position: Int,
                        id: Long
                    ) {
                        childId = childList[position].first // Firestore document ID
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        childId = ""
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load children: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNewFeeForChild(childId: String, amount: Double, description: String) {
        val db = FirebaseFirestore.getInstance()
        val newFee = hashMapOf(
            "amount" to amount,
            "description" to description,
            "createdAt" to Timestamp.now(),
            "amountPaid" to 0.0,
            "paymentStatus" to "PENDING" // Parent will handle payment type
        )

        db.collection("Child")
            .document(childId)
            .collection("Fees")
            .add(newFee)
            .addOnSuccessListener {
                Toast.makeText(this, "Fee created successfully", Toast.LENGTH_SHORT).show()
                finish() // close activity
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
