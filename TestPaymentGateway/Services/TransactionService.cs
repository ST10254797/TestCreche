using System.Xml;
using TestPaymentGateway.Models;
using Newtonsoft.Json;


namespace TestPaymentGateway.Services
{
    public class TransactionService
    {
        private readonly string _filePath = Path.Combine(Directory.GetCurrentDirectory(), "transactions.json");

        public List<Transaction> GetTransactions()
        {
            if (!File.Exists(_filePath))
                return new List<Transaction>();

            var json = File.ReadAllText(_filePath);
            return JsonConvert.DeserializeObject<List<Transaction>>(json) ?? new List<Transaction>();
        }

        public void SaveTransactions(List<Transaction> transactions)
        {
            var json = JsonConvert.SerializeObject(transactions, Newtonsoft.Json.Formatting.Indented);
            File.WriteAllText(_filePath, json);
        }

        public void AddTransaction(Transaction transaction)
        {
            var transactions = GetTransactions();
            transactions.Add(transaction);
            SaveTransactions(transactions);
        }

        public void ClearTransactions()
        {
            var emptyList = new List<Transaction>();
            var json = JsonConvert.SerializeObject(emptyList, Newtonsoft.Json.Formatting.Indented);
            File.WriteAllText(_filePath, json);
        }
    }
}