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