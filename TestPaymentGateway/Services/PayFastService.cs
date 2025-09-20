using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
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

        // --- HTML form POST for WebView / frontend ---
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

            // HTML encode values to avoid breaking POST
            var formData = data.Select(kv => $"<input type='hidden' name='{kv.Key}' value='{HttpUtility.HtmlEncode(kv.Value)}' />");

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
        h2 {{ color: #2ecc71; }}
        p {{ color: #555; font-size: 16px; }}
        a {{ color: #3498db; text-decoration: none; font-weight: bold; }}
        a:hover {{ text-decoration: underline; }}
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
            // --- Build ordered payload (PayFast NuGet style) ---
            var orderedData = new List<KeyValuePair<string, string>>
    {
        new KeyValuePair<string, string>("merchant_id", data.ContainsKey("merchant_id") ? data["merchant_id"] : ""),
        new KeyValuePair<string, string>("merchant_key", data.ContainsKey("merchant_key") ? data["merchant_key"] : ""),
        new KeyValuePair<string, string>("return_url", data.ContainsKey("return_url") ? data["return_url"] : ""),
        new KeyValuePair<string, string>("cancel_url", data.ContainsKey("cancel_url") ? data["cancel_url"] : ""),
        new KeyValuePair<string, string>("notify_url", data.ContainsKey("notify_url") ? data["notify_url"] : ""),
        new KeyValuePair<string, string>("email_address", data.ContainsKey("email_address") ? data["email_address"] : ""),
        new KeyValuePair<string, string>("amount", data.ContainsKey("amount") ? data["amount"] : ""),
        new KeyValuePair<string, string>("item_name", data.ContainsKey("item_name") ? data["item_name"] : ""),
        new KeyValuePair<string, string>("item_description", data.ContainsKey("item_description") ? data["item_description"] : "")
    };

            // Append passphrase if set
            if (!string.IsNullOrEmpty(_passphrase))
                orderedData.Add(new KeyValuePair<string, string>("passphrase", _passphrase));

            // --- Build payload string for signature (raw values!) ---
            var payload = new StringBuilder();
            for (int i = 0; i < orderedData.Count; i++)
            {
                var item = orderedData[i];
                payload.Append($"{item.Key}={item.Value}"); // raw value here
                if (i < orderedData.Count - 1)
                    payload.Append("&");
            }

            // --- Debugging output ---
            Console.WriteLine("=== PayFast Signature Debug ===");
            foreach (var kv in orderedData)
                Console.WriteLine($"{kv.Key} = {kv.Value}");
            Console.WriteLine("Payload before hashing:");
            Console.WriteLine(payload.ToString());
            Console.WriteLine("===============================");

            // --- MD5 hash ---
            using var md5 = MD5.Create();
            var hash = md5.ComputeHash(Encoding.UTF8.GetBytes(payload.ToString()));
            var signature = BitConverter.ToString(hash).Replace("-", "").ToLower();

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



    }
}
