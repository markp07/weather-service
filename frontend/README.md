# Weather Service Frontend - Quick Start Guide

## Development Setup

### Prerequisites
- Node.js 25+ installed
- Auth service running at `localhost:3000`
- Weather backend running at `localhost:12001`

### Installation
```bash
cd frontend
npm install
```

### Running Locally
```bash
npm run dev
```

The frontend will start on **http://localhost:3030**

## Architecture

### Service Separation
This weather service frontend is now a standalone application that:
- Focuses solely on weather-related functionality
- Delegates all authentication to the auth service
- Uses shared authentication cookies from `auth.markpost.dev`

### Authentication Flow
```
┌─────────────────┐
│  User visits    │
│  localhost:3030 │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Check auth with │
│ localhost:3000  │
└────────┬────────┘
         │
         ├─── Authenticated ──▶ Show weather dashboard
         │
         └─── Not authenticated ──▶ Redirect to localhost:3000/login?callback=localhost:3030
```

### Profile/Security Navigation
When users click "Profile" or "Security" in the sidebar:
1. They are redirected to the auth service: `localhost:3000/profile` or `localhost:3000/security`
2. A callback parameter is included: `?callback=localhost:3030/`
3. After making changes, users can return to the weather service

## API Endpoints

### Auth Service (localhost:3000 / auth.markpost.dev)
- `GET /api/auth/v1/user` - Get current user info
- `POST /api/auth/v1/refresh` - Refresh authentication token
- `POST /api/auth/v1/logout` - Logout user

### Weather Service (localhost:12001 / weather.markpost.dev)
- `GET /api/weather/v1/forecast?latitude={lat}&longitude={lon}` - Get weather forecast
- `GET /api/weather/v1/saved-locations` - Get saved locations
- `POST /api/weather/v1/saved-locations` - Save a location
- `DELETE /api/weather/v1/saved-locations/{id}` - Delete a saved location
- `PUT /api/weather/v1/saved-locations/reorder` - Reorder saved locations

## Environment Variables

### Development
```bash
NEXT_PUBLIC_WEATHER_API_URL=http://localhost:12001
```

### Production
```bash
NEXT_PUBLIC_WEATHER_API_URL=https://weather.markpost.dev
```

Note: Auth API URL is automatically determined based on hostname:
- `localhost` → `http://localhost:3000`
- Production → `https://auth.markpost.dev`

## Docker

### Build
```bash
docker build -t weather-service-frontend:latest .
```

### Run with Docker Compose
```bash
docker-compose up -d frontend
```

The service will be available at `localhost:13002` (mapped to container port 3030).

## Testing

### Run All Tests
```bash
npm test
```

### Run Tests in Watch Mode
```bash
npm test -- --watch
```

### Test Coverage
```bash
npm test -- --coverage
```

## Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── page.tsx          # Main weather dashboard
│   │   └── demo/             # Demo page (no auth required)
│   ├── components/
│   │   ├── Sidebar.tsx       # Navigation sidebar
│   │   ├── LocationBar.tsx   # Location search/display
│   │   ├── SavedLocations.tsx
│   │   └── ...
│   ├── utils/
│   │   ├── api.ts            # API utilities with auth retry
│   │   ├── retry.ts          # Retry logic
│   │   └── weatherTranslations.ts
│   └── types/
│       └── ...
├── messages/                  # i18n translations
│   ├── en.json
│   ├── nl.json
│   ├── de.json
│   └── fr.json
└── ...
```

## Key Features

### 1. Automatic Token Refresh
The `fetchWithAuthRetry` utility automatically:
- Detects 401 responses
- Attempts to refresh the token
- Retries the original request
- Redirects to login if refresh fails

### 2. Saved Locations
Users can:
- Save multiple weather locations
- Reorder locations via drag-and-drop
- Edit location names
- Delete locations
- View weather for all saved locations

### 3. Multi-language Support
Supported languages:
- English (en)
- Dutch (nl)
- German (de)
- French (fr)

### 4. Responsive Design
- Mobile-first design
- Sidebar collapses on mobile
- Touch-friendly interface

## Deployment

### Production Build
```bash
npm run build
npm start
```

### Environment Setup
Ensure the following are configured:
1. Auth service is accessible at `auth.markpost.dev`
2. Weather backend is accessible at `weather.markpost.dev`
3. CORS is configured to allow cookies from auth service
4. Domain cookies are set up properly for `markpost.dev`

## Troubleshooting

### "Session expired" errors
- Check that the auth service is running
- Verify cookies are being set correctly
- Check CORS configuration

### Weather data not loading
- Verify weather backend is running at `localhost:12001`
- Check browser console for API errors
- Ensure geolocation permissions are granted

### Profile/Security links not working
- Verify auth service is running at `localhost:3000`
- Check that callback URLs are properly encoded
- Ensure auth service has the weather service in its allowed callbacks

## Support

For issues or questions:
1. Check the logs in the browser console
2. Check the backend logs
3. Review the REFACTORING_SUMMARY.md for architecture details

