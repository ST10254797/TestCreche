using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;

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
            // Base URL of your server
            var baseUrl = Environment.GetEnvironmentVariable("BaseUrl")
                          ?? throw new InvalidOperationException("BaseUrl environment variable not set");

            // Android deep links
            var androidReturnLink = "myapp://payment-success";
            var androidCancelLink = "myapp://payment-cancel";

            var data = new Dictionary<string, string>
    {
        { "merchant_id", _merchantId },
        { "merchant_key", _merchantKey },
        // Public HTTPS URLs that redirect to Android app
        { "return_url", $"{baseUrl}/api/payment/redirect?redirectUrl={Uri.EscapeDataString(androidReturnLink)}" },
        { "cancel_url", $"{baseUrl}/api/payment/redirect?redirectUrl={Uri.EscapeDataString(androidCancelLink)}" },
        { "notify_url", $"{baseUrl}/api/payment/payment-notify" },
        { "email_address", emailAddress },
        { "amount", amount.ToString("F2", CultureInfo.InvariantCulture) },
        { "item_name", itemName },
        { "item_description", itemDescription }
    };

            // Add custom fields if provided
            if (!string.IsNullOrEmpty(customStr1))
                data.Add("custom_str1", customStr1);

            if (!string.IsNullOrEmpty(customStr2))
                data.Add("custom_str2", customStr2);

            var signature = CreateSignature(data);
            data.Add("signature", signature);

            var url = _sandboxUrl;
            var formData = data.Select(kv => $"<input type='hidden' name='{kv.Key}' value='{kv.Value}' />");

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
        <form id='payfast_form' action='{url}' method='POST'>
            {string.Join("\n", formData)}
            <input type='submit' value='Pay Now' style='display:none;' />
        </form>
    </div>
</body>
</html>";

            return htmlForm;
        }


        // Portions adapted from Payfast Nuget Package Code
        public string CreateSignature(Dictionary<string, string> data)
        {
            // Filter: exclude signature, exclude empty values
            var filtered = data
                .Where(kv => !string.IsNullOrEmpty(kv.Value) && kv.Key != "signature")
                .OrderBy(kv => kv.Key); // ✅ sort alphabetically

            // Build query string
            var payload = string.Join("&", filtered.Select(kv =>
                $"{kv.Key}={UrlEncode(kv.Value)}"));

            // Append passphrase if set
            if (!string.IsNullOrEmpty(_passphrase))
            {
                payload += $"&passphrase={UrlEncode(_passphrase)}";
            }

            // 🔍 Debug log
            Console.WriteLine("=== PayFast Signature Debug ===");
            Console.WriteLine("Payload before hashing:");
            Console.WriteLine(payload);
            Console.WriteLine("===============================");

            using var md5 = MD5.Create();
            var inputBytes = Encoding.UTF8.GetBytes(payload);
            var hash = md5.ComputeHash(inputBytes);

            var signature = BitConverter.ToString(hash).Replace("-", "").ToLower();

            // 🔍 Debug log
            Console.WriteLine("Generated Signature: " + signature);
            Console.WriteLine("===============================");

            return signature;
        }


        // Adapted from Payfast Nuget Package Code
        protected string UrlEncode(string url)
        {
            Dictionary<string, string> convertPairs = new Dictionary<string, string>() { { "%", "%25" }, { "!", "%21" }, { "#", "%23" }, { " ", "+" },
        { "$", "%24" }, { "&", "%26" }, { "'", "%27" }, { "(", "%28" }, { ")", "%29" }, { "*", "%2A" }, { "+", "%2B" }, { ",", "%2C" },
        { "/", "%2F" }, { ":", "%3A" }, { ";", "%3B" }, { "=", "%3D" }, { "?", "%3F" }, { "@", "%40" }, { "[", "%5B" }, { "]", "%5D" } };
            var replaceRegex = new Regex(@"[%!# $&'()*+,/:;=?@\[\]]");
            MatchEvaluator matchEval = match => convertPairs[match.Value];
            string encoded = replaceRegex.Replace(url, matchEval);
            return encoded;
        }

        public string GeneratePaymentUrl(string orderId, string itemDescription, string emailAddress, decimal amount)
        {
            var data = new Dictionary<string, string>
    {
        { "merchant_id", _merchantId },
        { "merchant_key", _merchantKey },
        { "return_url", $"https://testcrecheapp.onrender.com/api/payment/payment-success" },
        { "cancel_url", $"https://testcrecheapp.onrender.com/api/payment/payment-cancel" },
        { "notify_url", $"https://testcrecheapp.onrender.com/api/payment/payment-notify" },
        { "m_payment_id", orderId }, // ✅ Use orderId here
        { "email_address", emailAddress },
        { "amount", amount.ToString("F2", CultureInfo.InvariantCulture) },
        { "item_name", orderId }, // Can use orderId or product name
        { "item_description", itemDescription }
    };

            // Generate signature
            var signature = CreateSignature(data);
            data.Add("signature", signature);

            // Build URL query string
            var query = string.Join("&", data.Select(kv => $"{kv.Key}={UrlEncode(kv.Value)}"));

            // Return full redirect URL
            return $"{_sandboxUrl}?{query}";
        }

    }
}
