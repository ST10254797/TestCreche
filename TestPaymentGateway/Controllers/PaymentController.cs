using Microsoft.AspNetCore.Mvc;
using TestPaymentGateway.Models;
using TestPaymentGateway.Services;
using Google.Cloud.Firestore;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

using AppTransaction = TestPaymentGateway.Models.Transaction;

namespace TestPaymentGateway.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class PaymentController : ControllerBase
    {
        private readonly TransactionService _transactionService;
        private readonly PayFastService _payFastService;
        private readonly FirestoreDb _firestore;

        public PaymentController(TransactionService transactionService, PayFastService payFastService, FirestoreDb firestore)
        {
            _transactionService = transactionService;
            _payFastService = payFastService;
            _firestore = firestore;
        }

        // -------------------- Health --------------------
        [HttpGet("health")]
        public IActionResult HealthCheck()
        {
            return Ok("App is running!");
        }

        // -------------------- PayFast Payment --------------------
        [HttpGet("initiate-payment")]
        public IActionResult InitiatePayment(string orderId, string orderDescription, string email, decimal amount)
        {
            var transaction = new AppTransaction
            {
                OrderId = orderId,
                OrderDescription = orderDescription,
                Email = email,
                Amount = amount,
                PaymentStatus = "PENDING",
                PaymentDate = DateTime.UtcNow
            };

            _transactionService.AddTransaction(transaction);
            string htmlForm = _payFastService.GeneratePaymentData(amount, orderId, orderDescription, email);

            return Content(htmlForm, "text/html");
        }

        [HttpGet("payment-success")]
        public IActionResult PaymentSuccess()
        {
            string html = @"
<!DOCTYPE html>
<html>
<head>
    <title>Payment Successful</title>
    <style>
        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
        .card { display: inline-block; padding: 20px; border: 1px solid #ddd; border-radius: 8px; background: #f9f9f9; }
        h1 { color: #28a745; }
        a { display: inline-block; margin-top: 20px; padding: 10px 15px; background: #28a745; color: white; text-decoration: none; border-radius: 5px; }
    </style>
</head>
<body>
    <div class='card'>
        <h1>✅ Payment Successful</h1>
        <p>Thank you for your purchase!</p>
        <a href='/'>Go Back Home</a>
    </div>
</body>
</html>";
            return Content(html, "text/html");
        }

        [HttpGet("payment-cancel")]
        public IActionResult PaymentCancel()
        {
            string html = @"
<!DOCTYPE html>
<html>
<head>
    <title>Payment Cancelled</title>
    <style>
        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
        .card { display: inline-block; padding: 20px; border: 1px solid #ddd; border-radius: 8px; background: #f9f9f9; }
        h1 { color: #dc3545; }
        a { display: inline-block; margin-top: 20px; padding: 10px 15px; background: #dc3545; color: white; text-decoration: none; border-radius: 5px; }
    </style>
</head>
<body>
    <div class='card'>
        <h1>❌ Payment Cancelled</h1>
        <p>Your payment was cancelled. Please try again.</p>
        <a href='/'>Try Again</a>
    </div>
</body>
</html>";
            return Content(html, "text/html");
        }

        [HttpPost("payment-notify")]
        public async Task<IActionResult> Notify()
        {
            var form = await Request.ReadFormAsync();
            var notification = new PaymentNotification
            {
                PaymentStatus = form["payment_status"],
                PfPaymentId = form["pf_payment_id"],
                MPaymentId = form["m_payment_id"],
                ItemName = form["item_name"],
                ItemDescription = form["item_description"],
                AmountGross = decimal.TryParse(form["amount_gross"], out var gross) ? gross : 0,
                AmountFee = decimal.TryParse(form["amount_fee"], out var fee) ? fee : 0,
                AmountNet = decimal.TryParse(form["amount_net"], out var net) ? net : 0,
                CustomStr1 = form["custom_str1"],
                CustomStr2 = form["custom_str2"],
                CustomStr3 = form["custom_str3"],
                CustomStr4 = form["custom_str4"],
                CustomStr5 = form["custom_str5"],
                EmailAddress = form["email_address"],
                MerchantId = form["merchant_id"],
                Signature = form["signature"]
            };

            if (notification.PaymentStatus == "COMPLETE")
            {
                var transactions = _transactionService.GetTransactions();
                var transaction = transactions.FirstOrDefault(t => t.OrderId == notification.ItemName);

                if (transaction != null)
                {
                    transaction.PaymentId = notification.PfPaymentId;
                    transaction.PaymentStatus = notification.PaymentStatus;
                    transaction.AmountPaid = notification.AmountGross;

                    _transactionService.SaveTransactions(transactions);

                    // -------------------- Update Firestore School Fee --------------------
                    if (!string.IsNullOrEmpty(notification.CustomStr1) && !string.IsNullOrEmpty(notification.CustomStr2))
                    {
                        var childId = notification.CustomStr1;
                        var feeId = notification.CustomStr2;
                        var feeRef = _firestore.Collection("Child").Document(childId).Collection("Fees").Document(feeId);

                        var update = new Dictionary<string, object>
                        {
                            { "paymentStatus", "PAID" },
                            { "transactionId", transaction.PaymentId },
                            { "paidAt", DateTime.UtcNow }
                        };
                        await feeRef.UpdateAsync(update);
                    }
                }
            }

            return Ok();
        }

        [HttpGet("all-transactions")]
        public IActionResult GetAllTransactions()
        {
            var transactions = _transactionService.GetTransactions();
            return Ok(transactions);
        }

        [HttpGet("clear-transactions")]
        public IActionResult ClearTransactions()
        {
            _transactionService.ClearTransactions();
            return Ok();
        }

        [HttpGet("redirect")]
        public IActionResult RedirectToApp(string redirectUrl)
        {
            return Redirect(redirectUrl);
        }

        // -------------------- School Fees --------------------
        [HttpPost("create-school-fee")]
        public async Task<IActionResult> CreateSchoolFee([FromBody] SchoolFeeRequest request)
        {
            if (string.IsNullOrEmpty(request.ChildId))
                return BadRequest("ChildId is required.");

            var feeId = Guid.NewGuid().ToString();
            var feeRef = _firestore.Collection("Child")
                                    .Document(request.ChildId)
                                    .Collection("Fees")
                                    .Document(feeId);

            var feeData = new
            {
                type = request.Type, // "ONE_TIME" or "MONTHLY"
                description = request.Description,
                amount = request.Amount,
                dueDate = request.DueDate,
                paymentStatus = "PENDING",
                transactionId = (string)null,
                createdAt = DateTime.UtcNow
            };

            await feeRef.SetAsync(feeData);

            return Ok(new { feeId, message = "School fee created successfully." });
        }

        [HttpGet("school-fee-payment-page")]
        public IActionResult SchoolFeePaymentPage(string childId, string feeId, string email)
        {
            // Fetch the fee document
            var feeRef = _firestore.Collection("Child")
                                    .Document(childId)
                                    .Collection("Fees")
                                    .Document(feeId);
            var feeSnapshot = feeRef.GetSnapshotAsync().Result;

            if (!feeSnapshot.Exists)
                return NotFound("Fee not found.");

            var fee = feeSnapshot.ToDictionary();
            decimal amount = Convert.ToDecimal(fee["amount"]);
            string description = fee["description"].ToString();

            // Fetch child document safely
            var childRef = feeSnapshot.Reference.Parent.Parent;
            var childSnapshot = childRef.GetSnapshotAsync().Result;
            string childName = childSnapshot.Exists
                ? $"{childSnapshot.GetValue<string>("firstName")} {childSnapshot.GetValue<string>("lastName")}"
                : "Unknown";

            // Clean strings
            childName = childName.Trim();
            description = description.Trim();

            // Create transaction
            var transaction = new AppTransaction
            {
                OrderId = feeId,
                OrderDescription = description,
                Email = email,
                Amount = amount,
                PaymentStatus = "PENDING",
                PaymentDate = DateTime.UtcNow
            };
            _transactionService.AddTransaction(transaction);

            // Generate PayFast HTML form
            string htmlForm = _payFastService.GeneratePaymentData(
                amount: amount,
                itemName: childName,
                itemDescription: description,
                emailAddress: email,
                customStr1: childId,
                customStr2: feeId
            );

            return Content(htmlForm, "text/html");
        }


        [HttpGet("initiate-school-fee-payment")]
        public IActionResult InitiateSchoolFeePayment(string childId, string feeId, string email)
        {
            // Redirect to the dedicated school fee payment page
            return Redirect($"/api/payment/school-fee-payment-page?childId={childId}&feeId={feeId}&email={email}");
        }




        public class SchoolFeeRequest
        {
            public string ChildId { get; set; }
            public string Type { get; set; } // "ONE_TIME" or "MONTHLY"
            public string Description { get; set; }
            public decimal Amount { get; set; }
            public DateTime DueDate { get; set; }
        }
    }
}
