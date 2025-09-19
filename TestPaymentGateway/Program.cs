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

// Add Swagger/OpenAPI support
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

// Comment out HTTPS redirection for now on Render
// app.UseHttpsRedirection();

app.UseStaticFiles();
app.UseRouting();
app.UseAuthorization();

// Map MVC routes
app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}"
);

// -------------------- Bind to Render port --------------------
var port = Environment.GetEnvironmentVariable("PORT") ?? "5000";
app.Urls.Add($"http://0.0.0.0:{port}");

app.Run();
