# Application Guide: Database Layer & Lambda Handlers

### Core Database Layer (4 Java Files) 

1. **DatabaseConnection.java** (3.0 KB)
   - Connection management
   - AWS Secrets Manager integration

2. **BaseDAO.java** (2.2 KB)
   - Generic template for all DAOs
   - Automatic transaction management

3. **UserDAO.java** (3.7 KB)
   - User table operations
   - Insert, query, and exists methods

4. **WardrobeItemDAO.java** (5.6 KB)
   - Complete example with all CRUD patterns

### Lambda Handler (Refactored) 

5. **PreAuthorizerLambdaHandler.java**
   - Uses UserDAO

6. **WardrobeLambdaHandler.java**
    - Uses UserDAO


## Cold Start
   The first TCP+TLS handshake to any remote endpoint on a cold start JVM is unavoidable. It's ~2.5–3s because:

   - JVM loads SSL/TLS classes for the first time (~500ms)

   - javax.net.ssl.SSLContext initializes the secure random seed (~500ms)

   - TCP handshake to DynamoDB endpoint (~few ms, negligible)

   - TLS 1.3 handshake (~100–200ms)
