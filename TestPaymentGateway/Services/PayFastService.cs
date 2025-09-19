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

        public string GeneratePaymentData(decimal amount, string itemName, string itemDescription, string emailAddress)
        {
            var data = new Dictionary<string, string>
        {
            { "merchant_id", _merchantId },
            { "merchant_key", _merchantKey },
            { "return_url", "https://[your site].onrender.com/api/payment/payment-success" },
            { "cancel_url", "https://[your site].onrender.com/api/payment/payment-cancel" },
            { "notify_url", "https://[your site].onrender.com/api/payment/payment-notify" },
            { "email_address", emailAddress },
            { "amount", amount.ToString("F2", CultureInfo.InvariantCulture) },
            { "item_name", itemName },
            { "item_description", itemDescription },
        };

            var signature = CreateSignature(data);

            data.Add("signature", signature);

            var url = _sandboxUrl;

            var formData = data.Select(kv => $"<input type='hidden' name='{kv.Key}' value='{kv.Value}' />").ToList();

            var htmlForm = $@"
            <!DOCTYPE html>
            <html lang='en'>
            <head>
                <meta charset='UTF-8'>
                <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                <title>Redirecting to PayFast...</title>
                <script type='text/javascript'>
                    window.onload = function() {{
                        document.getElementById('payfast_form').submit(); // Auto-submit form to PayFast
                    }};
                </script>
            </head>
            <body>
                <h2>Redirecting you to PayFast...</h2>
                <p>If you're not redirected, <a href='#' onclick='document.getElementById('payfast_form').submit();'>click here</a>.</p>
                <form id='payfast_form' action='{url}' method='POST'>
                    {string.Join("\n", formData)}
                    <input type='submit' value='Pay Now' style='display:none;' /> <!-- Hidden submit button -->
                </form>
            </body>
            </html>";

            return htmlForm;
        }

        // Portions adapted from Payfast Nuget Package Code
        public string CreateSignature(Dictionary<string, string> data)
        {
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
            new KeyValuePair<string, string>("item_description", data.ContainsKey("item_description") ? data["item_description"] : ""),
            new KeyValuePair<string, string>("passphrase", _passphrase)
        };

            var payload = new StringBuilder();
            foreach (var item in orderedData)
            {
                if (item.Key != orderedData.Last().Key)
                {
                    payload.Append($"{item.Key}={UrlEncode(item.Value)}&");
                }
                else
                {
                    payload.Append($"{item.Key}={UrlEncode(item.Value)}");
                }
            }

            var md5 = MD5.Create();
            var inputBytes = Encoding.ASCII.GetBytes(payload.ToString());
            var hash = md5.ComputeHash(inputBytes);

            var signature = new StringBuilder();
            for (int i = 0; i < hash.Length; i++)
            {
                signature.Append(hash[i].ToString("x2"));
            }
            return signature.ToString();
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
