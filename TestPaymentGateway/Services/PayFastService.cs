using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using System.Web;

namespace TestPaymentGateway.Services
{
    public class PayFastService
    {
        private readonly string _merchantId;
        private readonly string _merchantKey;
        private readonly string _passphrase;
        private readonly string _sandboxUrl;

        public PayFastService(string merchantId, string merchantKey, string passphrase, string sandboxUrl)
        {
            _merchantId = merchantId;
            _merchantKey = merchantKey;
            _passphrase = passphrase;
            _sandboxUrl = sandboxUrl;
        }

        public string GeneratePaymentData(decimal amount, string itemName, string itemDescription, string emailAddress,
                                    string customStr1 = null, string customStr2 = null)
        {
            var baseUrl = Environment.GetEnvironmentVariable("BaseUrl")
                          ?? throw new InvalidOperationException("BaseUrl environment variable not set");

            var androidReturnLink = "myapp://payment-success";
            var androidCancelLink = "myapp://payment-cancel";

            var data = new Dictionary<string, string>
            {
                { "merchant_id", _merchantId },
                { "merchant_key", _merchantKey },
                { "return_url", $"{baseUrl}/api/payment/redirect?redirectUrl={androidReturnLink}" },
                { "cancel_url", $"{baseUrl}/api/payment/redirect?redirectUrl={androidCancelLink}" },
                { "notify_url", $"{baseUrl}/api/payment/payment-notify" },
                { "email_address", emailAddress },
                { "amount", amount.ToString("F2", CultureInfo.InvariantCulture) },
                { "item_name", itemName },
                { "item_description", itemDescription }
            };

            if (!string.IsNullOrEmpty(customStr1))
                data.Add("custom_str1", customStr1);
            if (!string.IsNullOrEmpty(customStr2))
                data.Add("custom_str2", customStr2);

            var signature = CreateSignature(data);
            data.Add("signature", signature);

            var formData = data.Select(kv => $"<input type='hidden' name='{kv.Key}' value='{kv.Value}' />");

            // Full CSS restored
            var htmlForm = $@"
<!DOCTYPE html>
<html lang='en'>
<head>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title>Redirecting to PayFast...</title>
    <style>
        body {{
            font-family: Arial, sans-serif;
            text-align: center;
            background: #f5f6fa;
            margin: 0;
            padding: 50px;
        }}
        .container {{
            display: inline-block;
            padding: 30px 40px;
            border-radius: 10px;
            background: #ffffff;
            box-shadow: 0 4px 10px rgba(0,0,0,0.1);
        }}
        h2 {{
            color: #2ecc71;
        }}
        p {{
            color: #555;
            font-size: 16px;
        }}
        a {{
            color: #3498db;
            text-decoration: none;
            font-weight: bold;
        }}
        a:hover {{
            text-decoration: underline;
        }}
    </style>
    <script type='text/javascript'>
        window.onload = function() {{
            document.getElementById('payfast_form').submit();
        }};
    </script>
</head>
<body>
    <div class='container'>
        <h2>Redirecting to PayFast...</h2>
        <p>Please wait while we redirect you to complete your payment.</p>
        <p>If you are not redirected automatically, <a href='#' onclick='document.getElementById(""payfast_form"").submit();'>click here</a>.</p>
        <form id='payfast_form' action='{_sandboxUrl}' method='POST'>
            {string.Join("\n", formData)}
            <input type='submit' value='Pay Now' style='display:none;' />
        </form>
    </div>
</body>
</html>";

            return htmlForm;
        }

        public string CreateSignature(Dictionary<string, string> data)
        {
            var filtered = data
                .Where(kv => !string.IsNullOrEmpty(kv.Value) && kv.Key != "signature")
                .OrderBy(kv => kv.Key);

            var payload = string.Join("&", filtered.Select(kv =>
                $"{kv.Key}={UrlEncode(kv.Value)}"));

            // CRITICAL: Passphrase should NOT be URL encoded in the payload
            if (!string.IsNullOrEmpty(_passphrase))
                payload += $"&passphrase={_passphrase}";  // Remove UrlEncode here!

            Console.WriteLine("=== PayFast Signature Debug ===");
            Console.WriteLine("Payload before hashing:");
            Console.WriteLine(payload);
            Console.WriteLine("===============================");

            using var md5 = MD5.Create();
            var inputBytes = Encoding.UTF8.GetBytes(payload);
            var hash = md5.ComputeHash(inputBytes);
            var signature = BitConverter.ToString(hash).Replace("-", "").ToLower();

            Console.WriteLine("Generated Signature: " + signature);
            Console.WriteLine("===============================");

            return signature;
        }

        protected string UrlEncode(string url)
        {
            return Uri.EscapeDataString(url);
        }
    }
}