# MailIt Postal Wrapper API

A TrackingMore wrapper API providing normalized shipment tracking with multi-tenant support, rate limiting, and standardized status responses.

## Features

- **Batch Tracking Creation**: Register up to 40 shipments in a single request
- **Batch Tracking Retrieval**: Fetch status for up to 40 shipments in a single request
- **Normalized Statuses**: Consistent status values across all carriers
- **Multi-Tenant**: Isolated data per API client with Stripe-style API keys
- **Access Control**: Suspend clients or set API key expiration dates
- **Rate Limiting**: Tiered rate limits (FREE, STARTER, PRO, ENTERPRISE)
- **Soft Delete**: Audit-friendly tracking deletion
- **OpenAPI Documentation**: Full Swagger UI at `/swagger-ui.html`

## Documentation

- **[API Guide](API_GUIDE.md)**: For developers integrating with the API (Authentication, Endpoints, Workflows).
- **[Admin Guide](ADMIN_GUIDE.md)**: For operators managing clients and issuing API keys.

## Tech Stack

- **Java 17** (LTS)
- **Spring Boot 3.2.1**
- **H2** (development) / **PostgreSQL** (production)
- **Flyway** for database migrations
- **Bucket4j** for rate limiting
- **SpringDoc OpenAPI** for API documentation

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- (Optional) PostgreSQL 15+ for production mode

### Development Mode

```bash
# Clone and build
git clone <repository-url>
cd mailit-postal-wrapper-api
mvn clean install

# Run with H2 database
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# API available at http://localhost:9000
# Swagger UI at http://localhost:9000/swagger-ui.html
# H2 Console at http://localhost:9000/h2-console
```

### Production Mode

```bash
# Set environment variables
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://localhost:5433/mailit
export DATABASE_USERNAME=mailit
export DATABASE_PASSWORD=your-secure-password
export TRACKINGMORE_API_KEY=your-trackingmore-api-key

# Run
java -jar target/mailit-wrapper-*.jar
```

### Docker Deployment

The easiest way to deploy to production is with Docker Compose.

```bash
# 1. Copy environment template and edit with your values
cp .env.example .env
nano .env  # Set your TRACKINGMORE_API_KEY and DB_PASSWORD

# 2. Build and start containers
docker-compose up -d --build

# 3. Check status
docker-compose ps
docker-compose logs -f api

# API available at http://localhost:8080
```

**Building standalone image**:
```bash
# Build the image
docker build -t mailit-wrapper:latest .

# Run with external PostgreSQL
docker run -d \
  --name mailit-api \
  -p 9000:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/mailit \
  -e SPRING_DATASOURCE_USERNAME=mailit \
  -e SPRING_DATASOURCE_PASSWORD=your-password \
  -e TRACKINGMORE_API_KEY=your-api-key \
  mailit-wrapper:latest
```

### Release & Versioning

Current Version: **0.0.1** (See [VERSION](VERSION) file)

To push to Docker Hub:
```bash
# 1. Login
docker login

# 2. Tag the image
docker tag mailit-postal-wrapper-api-api:latest <your-dockerhub-username>/mailit-wrapper:0.0.1
docker tag mailit-postal-wrapper-api-api:latest <your-dockerhub-username>/mailit-wrapper:latest

# 3. Push
docker push <your-dockerhub-username>/mailit-wrapper:0.0.1
docker push <your-dockerhub-username>/mailit-wrapper:latest
```

### Production Deployment (Pre-built Image)

To deploy using the pre-built image from Docker Hub (`macubex/mailit-wrapper`):

1.  **Copy files to server**:
    *   `docker-compose.prod.yml`
    *   `.env.example`
    *   `deploy_prod.sh` (Linux) or `deploy_prod.ps1` (Windows)

2.  **Configure**:
    ```bash
    cp .env.example .env
    # Edit .env with your API keys
    ```

3.  **Run**:
    ```bash
    # Linux/Mac
    ./deploy_prod.sh

    # Windows
    .\deploy_prod.ps1
    ```

## API Overview

### Authentication

All API requests require an `X-API-Key` header:

```bash
curl -H "X-API-Key: sk_live_abc12345_xxxxxxxxxxx" \
     http://localhost:8080/api/v1/trackings
```

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/trackings` | Create batch trackings (max 40) |
| GET | `/api/v1/trackings` | List trackings with pagination |
| GET | `/api/v1/trackings/{id}` | Get tracking details |
| DELETE | `/api/v1/trackings/{id}` | Soft delete tracking |

### Example: Create Trackings

```bash
curl -X POST http://localhost:8080/api/v1/trackings \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_abc12345_xxxxxxxxxxx" \
  -d '{
    "shipments": [
      {
        "trackingNumber": "9400111899223456789012",
        "courier": "usps",
        "orderId": "ORD-001"
      },
      {
        "trackingNumber": "EE123456789IN",
        "courier": "india-post",
        "originCountry": "IN",
        "destinationCountry": "US"
      }
    ]
  }'
```

### Example: List Trackings

```bash
curl http://localhost:8080/api/v1/trackings?status=IN_TRANSIT&page=0&size=20 \
  -H "X-API-Key: sk_live_abc12345_xxxxxxxxxxx"
```

## Tracking Statuses

| Status | Description |
|--------|-------------|
| `PENDING` | Tracking registered, awaiting carrier scan |
| `NOT_FOUND` | Carrier has no record of this tracking |
| `IN_TRANSIT` | Package is in transit |
| `OUT_FOR_DELIVERY` | Package is out for delivery |
| `DELIVERED` | Package delivered |
| `EXCEPTION` | Delivery exception occurred |
| `EXPIRED` | Tracking expired (no updates for 30+ days) |
| `RETURNED` | Package returned to sender |

## Rate Limits

| Plan | Requests/Minute |
|------|-----------------|
| FREE | 60 |
| STARTER | 300 |
| PRO | 1,000 |
| ENTERPRISE | 10,000 |

Rate limit headers are included in responses:
- `X-RateLimit-Remaining`: Requests remaining in current window
- `Retry-After`: Seconds until rate limit resets (when exceeded)

## Admin Endpoints (Internal)

Admin endpoints are not exposed in Swagger and should be protected by network policy:

```bash
# Create a client
curl -X POST http://localhost:8080/admin/clients \
  -H "Content-Type: application/json" \
  -d '{"name": "New Client", "plan": "PRO"}'

# Response includes the raw API key (shown only once)
# {"id":1,"name":"New Client","apiKeyPrefix":"sk_live_abc123","apiKey":"sk_live_abc123_xxxx...","plan":"PRO"}
```

## Complete Testing Guide

### Step 1: Start the Application

```bash
# Using Maven (development mode)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Step 2: Create an API Client

```bash
# Create a client and get your API key
curl -X POST http://localhost:8080/admin/clients \
  -H "Content-Type: application/json" \
  -d '{"name": "My Test Client", "plan": "PRO"}'
```

**Response:**
```json
{
  "id": 1,
  "name": "My Test Client",
  "apiKeyPrefix": "sk_live_abc123",
  "apiKey": "sk_live_abc123_xxxxxxxxxxxxxxxxxxxxxxxxxx",
  "plan": "PRO"
}
```

⚠️ **SAVE YOUR API KEY** - It won't be shown again!

### Step 3: Create Trackings

```bash
# Set your API key
export API_KEY="sk_live_abc123_xxxxxxxxxxxxxxxxxxxxxxxxxx"

# Create trackings (batch up to 40)
curl -X POST http://localhost:8080/api/v1/trackings \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{
    "shipments": [
      {
        "trackingNumber": "JQ048712813IN",
        "courier": "india-post",
        "originCountry": "IN",
        "destinationCountry": "US",
        "orderId": "ORDER-001"
      },
      {
        "trackingNumber": "JQ048712827IN",
        "courier": "india-post",
        "originCountry": "IN",
        "destinationCountry": "US",
        "orderId": "ORDER-002"
      }
    ]
  }'
```

**Response:**
```json
{
  "success": true,
  "created": [
    {
      "trackingId": "trk_xl37qpcczg1u",
      "trackingNumber": "JQ048712813IN",
      "status": "created"
    },
    {
      "trackingId": "trk_gp5q78mkedtw",
      "trackingNumber": "JQ048712827IN",
      "status": "created"
    }
  ],
  "failed": []
}
```

### Step 4: List All Trackings

```bash
curl -X GET "http://localhost:8080/api/v1/trackings" \
  -H "X-API-Key: $API_KEY"
```

**Response:**
```json
{
  "trackings": [
    {
      "trackingId": "trk_xl37qpcczg1u",
      "trackingNumber": "JQ048712813IN",
      "courierCode": "india-post",
      "status": "PENDING"
    }
  ],
  "pagination": {
    "page": 0,
    "limit": 20,
    "total": 2,
    "totalPages": 1
  }
}
```

### Step 5: Get Tracking Details

```bash
curl -X GET "http://localhost:8080/api/v1/trackings/trk_xl37qpcczg1u" \
  -H "X-API-Key: $API_KEY"
```

**Response:**
```json
{
  "trackingId": "trk_xl37qpcczg1u",
  "trackingNumber": "JQ048712813IN",
  "courierCode": "india-post",
  "status": "PENDING",
  "originCountry": "IN",
  "destinationCountry": "US",
  "orderId": "ORDER-001",
  "createdAt": "2025-12-18T11:44:05.988183Z",
  "events": []
}
```

### Step 6: Delete a Tracking (Soft Delete)

```bash
curl -X DELETE "http://localhost:8080/api/v1/trackings/trk_xl37qpcczg1u" \
  -H "X-API-Key: $API_KEY"
```

**Response:**
```json
{
  "success": true,
  "trackingId": "trk_xl37qpcczg1u",
  "message": "Tracking deleted successfully"
}
```

## Supported Couriers

Common courier codes for India Post:
- `india-post` - India Post

For a full list of courier codes, refer to the TrackingMore documentation.

## Testing with Real Data

To test the API with real tracking numbers (e.g., `JQ048712813IN`), follow these steps:

1. **Configure API Key**: Ensure your `application-dev.yml` has a valid TrackingMore API key.
2. **Create a Client**:
   ```bash
   curl -X POST http://localhost:8080/admin/clients \
     -H "Content-Type: application/json" \
     -d '{"name":"Test Client","contactEmail":"test@example.com"}'
   ```
   Note the returned `apiKey` (e.g., `sk_live_...`).

3. **Create Tracking**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/trackings \
     -H "Content-Type: application/json" \
     -H "X-API-Key: YOUR_API_KEY" \
     -d '{
       "shipments": [
         {
           "trackingNumber": "JQ048712813IN",
           "courier": "india-post"
         }
       ]
     }'
   ```

4. **Get Tracking Details**:
   Use the `trackingId` returned from the previous step (e.g., `trk_...`).
   ```bash
   curl -H "X-API-Key: YOUR_API_KEY" \
     http://localhost:8080/api/v1/trackings/trk_...
   ```
   You should see the full tracking history including events like "Item Delivered".

## Project Structure

```
src/main/java/com/mailit/wrapper/
├── MailItWrapperApplication.java    # Main application
├── client/                          # TrackingMore API client
├── config/                          # Configuration classes
├── controller/                      # REST controllers
├── exception/                       # Exception handling
├── model/                           # Entities, DTOs, enums
├── repository/                      # JPA repositories
├── service/                         # Business logic
└── util/                            # Utility classes

src/main/resources/
├── application.yml                  # Common configuration
├── application-dev.yml              # Development profile (H2)
├── application-prod.yml             # Production profile (PostgreSQL)
├── db/migration/                    # Flyway migrations
└── logback-spring.xml               # Logging configuration
```

## Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `trackingmore.api-key` | TrackingMore API key | (required) |
| `trackingmore.base-url` | TrackingMore base URL | `https://api.trackingmore.com/v4` |
| `trackingmore.timeout` | HTTP client timeout | `30s` |
| `rate-limit.enabled` | Enable rate limiting | `true` |

## Development

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# With coverage
mvn test jacoco:report
```

### Database Migrations

Migrations are automatically applied on startup. To create a new migration:

```bash
# Create a new migration file
touch src/main/resources/db/migration/V3__description.sql
```

## License

MIT

## Support

For issues, please create a GitHub issue or contact the development team.
