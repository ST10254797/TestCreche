using Google.Apis.Auth.OAuth2;
using Google.Cloud.Firestore;
using Google.Cloud.Firestore.V1;
using Grpc.Auth;
using TestPaymentGateway;
using TestPaymentGateway.Services;
using System;
using System.Threading.Tasks;

var builder = WebApplication.CreateBuilder(args);

// -------------------- Services --------------------

// Add MVC controllers
builder.Services.AddControllersWithViews();

// Add TransactionService as singleton
builder.Services.AddSingleton<TransactionService>();

// Add PayFastService using environment variables
builder.Services.AddScoped<PayFastService>(serviceProvider =>
{
    var configuration = builder.Configuration;
    var merchantId = configuration["PayFast_MerchantId"];
    var merchantKey = configuration["PayFast_MerchantKey"];
    var passphrase = configuration["PayFast_Passphrase"];
    var sandboxUrl = configuration["PayFast_SandboxUrl"];
    return new PayFastService(merchantId, merchantKey, passphrase, sandboxUrl);
});

// -------------------- Firestore Initialization --------------------

// Read Firebase service account JSON from environment variable
var firebaseJson = Environment.GetEnvironmentVariable("GOOGLE_APPLICATION_CREDENTIALS_JSON");
if (string.IsNullOrEmpty(firebaseJson))
{
    throw new InvalidOperationException("Environment variable GOOGLE_APPLICATION_CREDENTIALS_JSON is not set.");
}

// Create GoogleCredential from JSON
var credential = GoogleCredential.FromJson(firebaseJson);

// Create FirestoreClient with the credential
var firestoreClient = new FirestoreClientBuilder
{
    ChannelCredentials = credential.ToChannelCredentials()
}.Build();

// Register FirestoreDb as singleton
builder.Services.AddSingleton(provider =>
{
    return FirestoreDb.Create("testcreche", firestoreClient); // replace with your Firebase Project ID
});

var app = builder.Build();

// -------------------- One-time Firestore Cleanup in Background --------------------
try
{
    var firestoreDb = app.Services.GetRequiredService<FirestoreDb>();
    // Run cleanup in a background task
    _ = Task.Run(async () =>
    {
        try
        {
            await RemoveTypeFieldFromFees(firestoreDb);
        }
        catch (Exception ex)
        {
            Console.WriteLine("Firestore cleanup failed: " + ex.Message);
        }
    });
}
catch (Exception ex)
{
    Console.WriteLine("Failed to start Firestore cleanup task: " + ex.Message);
}

// -------------------- Middleware --------------------

// Swagger UI
app.UseSwagger();
app.UseSwaggerUI(c =>
{
    c.SwaggerEndpoint("/swagger/v1/swagger.json", "PayFast API V1");
});

// HTTP request pipeline
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
    app.UseHsts();
}

// app.UseHttpsRedirection(); // Commented for Render
app.UseStaticFiles();
app.UseRouting();
app.UseAuthorization();

// Map MVC routes
app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}"
);

app.MapControllers();

// -------------------- Bind to Render port --------------------
var port = Environment.GetEnvironmentVariable("PORT") ?? "5000";
app.Urls.Add($"http://0.0.0.0:{port}");

app.Run();

// -------------------- Firestore Cleanup Method --------------------
async Task RemoveTypeFieldFromFees(FirestoreDb db)
{
    Console.WriteLine("Starting Firestore cleanup: removing 'type' field...");

    var childrenSnapshot = await db.Collection("Child").GetSnapshotAsync();

    foreach (var childDoc in childrenSnapshot.Documents)
    {
        var feesSnapshot = await childDoc.Reference.Collection("Fees").GetSnapshotAsync();

        foreach (var feeDoc in feesSnapshot.Documents)
        {
            if (feeDoc.ContainsField("type"))
            {
                Console.WriteLine($"Removing 'type' from {childDoc.Id}/{feeDoc.Id}");
                await feeDoc.Reference.UpdateAsync("type", FieldValue.Delete);
            }
        }
    }

    Console.WriteLine("Firestore cleanup completed!");
}
