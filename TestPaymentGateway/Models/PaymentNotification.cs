using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc;

public class PaymentNotification
{
    [FromForm(Name = "payment_status")]
    [Required]
    public string PaymentStatus { get; set; }

    [FromForm(Name = "pf_payment_id")]
    [Required]
    public string PfPaymentId { get; set; }

    [FromForm(Name = "m_payment_id")]
    public string MPaymentId { get; set; }

    [FromForm(Name = "item_name")]
    [Required]
    public string ItemName { get; set; }

    [FromForm(Name = "item_description")]
    public string ItemDescription { get; set; }

    [FromForm(Name = "amount_gross")]
    public decimal AmountGross { get; set; }

    [FromForm(Name = "amount_fee")]
    public decimal AmountFee { get; set; }

    [FromForm(Name = "amount_net")]
    public decimal AmountNet { get; set; }

    [FromForm(Name = "custom_str1")]
    public string CustomStr1 { get; set; }

    [FromForm(Name = "custom_str2")]
    public string CustomStr2 { get; set; }

    [FromForm(Name = "custom_str3")]
    public string CustomStr3 { get; set; }

    [FromForm(Name = "custom_str4")]
    public string CustomStr4 { get; set; }

    [FromForm(Name = "custom_str5")]
    public string CustomStr5 { get; set; }

    [FromForm(Name = "email_address")]
    public string EmailAddress { get; set; }

    [FromForm(Name = "merchant_id")]
    [Required]
    public string MerchantId { get; set; }

    [FromForm(Name = "signature")]
    public string Signature { get; set; }
}
