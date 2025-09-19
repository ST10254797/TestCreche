namespace TestPaymentGateway.Models
{
    public class Transaction
    {
        public decimal Amount { get; set; }
        public decimal AmountPaid { get; set; }
        public DateTime PaymentDate { get; set; }
        public string PaymentStatus { get; set; }
        public string OrderId { get; set; }
        public string OrderDescription { get; set; }
        public string Email { get; set; }
        public string PaymentId { get; set; }
    }
}
