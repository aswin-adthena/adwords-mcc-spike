# Google Ads MCC Account Viewer

This is a proof-of-concept application that demonstrates how to authenticate with Google OAuth2 and retrieve Google Ads accounts using the Google Ads API.

## Prerequisites

- Java 21
- Maven
- Node.js and npm
- Google Cloud Project with OAuth2 credentials
- Google Ads API access with a developer token

## Setup

### 1. Google Cloud Project Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - Google Ads API
   - Google People API
4. Create OAuth2 credentials:
   - Go to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "OAuth client ID"
   - Choose "Web application" as the application type
   - Add "http://localhost:3000" to Authorized JavaScript Origins
   - Add "http://localhost:4000/oauth2callback" to Authorized Redirect URIs
   - Download the client ID and client secret

### 2. Configure the Application

1. Open `src/main/resources/application.properties`
2. Replace the following placeholders with your actual values:
   - `spring.security.oauth2.client.registration.google.client-id`: Your OAuth2 client ID
   - `spring.security.oauth2.client.registration.google.client-secret`: Your OAuth2 client secret
   - `google.ads.developer-token`: Your Google Ads API developer token

## Running the Application

### Backend

1. Build the application:
   ```
   mvn clean install
   ```

2. Run the Spring Boot application:
   ```
   mvn spring-boot:run
   ```

The backend server will start on http://localhost:4000.

### Frontend

1. Navigate to the frontend directory:
   ```
   cd frontend
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Start the React development server:
   ```
   npm start
   ```

The frontend will be available at http://localhost:3000.

## Usage

1. Open http://localhost:3000 in your browser
2. Click "Sign in with Google"
3. Authenticate with your Google account that has access to Google Ads accounts
4. After successful authentication, you will be redirected back to the application
5. The application will display a list of Google Ads accounts you have access to

## Notes

- This is a proof-of-concept application and is not intended for production use
- The application does not implement token refresh or persistent storage
- Error handling is minimal and should be improved for a production application
