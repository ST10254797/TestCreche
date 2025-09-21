using Google.Apis.Auth.OAuth2;
using Google.Cloud.Firestore;
using Google.Cloud.Firestore.V1;
using Grpc.Auth;
using TestPaymentGateway;
using TestPaymentGateway.Services;

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

// -------------------- Swagger / OpenAPI --------------------
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

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

// Commented HTTPS redirection for Render
// app.UseHttpsRedirection();

app.UseStaticFiles();
app.UseRouting();
app.UseMiddleware<FirebaseRoleMiddleware>();
app.UseAuthorization();

// Map MVC routes
app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}"
);

app.MapControllers(); // This enables routes defined in ApiController classes


// -------------------- Bind to Render port --------------------
var port = Environment.GetEnvironmentVariable("PORT") ?? "5000";
app.Urls.Add($"http://0.0.0.0:{port}");

app.Run();
