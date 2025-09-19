using Microsoft.AspNetCore.Mvc;
using TestPaymentGateway.Models;
using TestPaymentGateway.Services;

namespace TestPaymentGateway.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class PaymentController : ControllerBase
    {
        private readonly TransactionService _transactionService;
        private readonly PayFastService _payFastService;

        public PaymentController(TransactionService transactionService, PayFastService payFastService)
        {
            _transactionService = transactionService;
            _payFastService = payFastService;
        }

        [HttpGet("health")]
        public IActionResult HealthCheck()
        {
            return Ok("App is running!");
        }

        [HttpGet("initiate-payment")]
        public IActionResult InitiatePayment(string orderId, string orderDescription, string email, decimal amount)
        {
            var transaction = new Transaction
            {
                OrderId = orderId,
                OrderDescription = orderDescription,
                Email = email,
                Amount = amount,
                PaymentStatus = "PENDING",
                PaymentDate = DateTime.UtcNow,
            };

            _transactionService.AddTransaction(transaction);
            var paymentUrl = _payFastService.GeneratePaymentUrl(orderId, orderDescription, email, amount);

            return Ok(new
            {
                Status = "Pending",
                OrderId = orderId,
                Amount = amount,
                PaymentUrl = paymentUrl
            });

        }

        [HttpGet("payment-success")]
        public IActionResult PaymentSuccess()
        {
            var html = @"
    <html>
        <head><title>Payment Success</title></head>
        <body style='font-family:sans-serif; text-align:center;'>
            <h1 style='color:green;'>✅ Payment Successful</h1>
            <p>Thank you for your purchase!</p>
            <a href='/' style='color:blue;'>Go back to Home</a>
        </body>
    </html>";
            return Content(html, "text/html");
        }

        [HttpGet("payment-cancel")]
        public IActionResult PaymentCancel()
        {
            var html = @"
    <html>
        <head><title>Payment Cancelled</title></head>
        <body style='font-family:sans-serif; text-align:center;'>
            <h1 style='color:red;'>❌ Payment Cancelled</h1>
            <p>You cancelled the transaction. Please try again.</p>
            <a href='/' style='color:blue;'>Return to Shop</a>
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
    }
}