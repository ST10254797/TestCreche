# Use .NET SDK for building
FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /src

# Copy the csproj file and restore dependencies
COPY ["TestPaymentGateway/TestPaymentGateway.csproj", "TestPaymentGateway/"]
RUN dotnet restore "TestPaymentGateway/TestPaymentGateway.csproj"

# Copy the rest of the project files
COPY . .

# Build the project
WORKDIR "/src/TestPaymentGateway"
RUN dotnet build "TestPaymentGateway.csproj" -c Release -o /app/build

# Publish the project
FROM build AS publish
RUN dotnet publish "TestPaymentGateway.csproj" -c Release -o /app/publish

# Use ASP.NET runtime image to run the app
FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS base
WORKDIR /app
EXPOSE 80

COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "TestPaymentGateway.dll"]
