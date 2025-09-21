using FirebaseAdmin.Auth;
using Google.Cloud.Firestore;

public class FirebaseRoleMiddleware
{
    private readonly RequestDelegate _next;
    private readonly FirestoreDb _firestore;

    public FirebaseRoleMiddleware(RequestDelegate next, FirestoreDb firestore)
    {
        _next = next;
        _firestore = firestore;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        string authHeader = context.Request.Headers["Authorization"];
        if (!string.IsNullOrEmpty(authHeader) && authHeader.StartsWith("Bearer "))
        {
            string idToken = authHeader.Substring("Bearer ".Length).Trim();
            try
            {
                var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(idToken);
                string uid = decodedToken.Uid;

                var userDoc = await _firestore.Collection("users").Document(uid).GetSnapshotAsync();
                if (userDoc.Exists)
                {
                    string role = userDoc.GetValue<string>("role")?.ToLower() ?? "user";
                    context.Items["Uid"] = uid;
                    context.Items["Role"] = role;
                }
            }
            catch
            {
                // Token invalid; skip
            }
        }

        await _next(context);
    }
}
