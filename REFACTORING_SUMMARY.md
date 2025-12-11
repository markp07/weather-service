# Weather Service Frontend Refactoring Summary

## Overview
Refactored the weather service frontend to be a standalone weather application that delegates all authentication and profile management to an external auth service (auth.markpost.dev locally: localhost:3000).

## Changes Made

### 1. Port Configuration (Changed from 3000 to 3030)

#### `frontend/package.json`
- Updated dev script: `next dev -p 3030`
- Updated start script: `next start -p 3030`
- Changed package name from `demo-authentication` to `weather-service-frontend`

#### `frontend/Dockerfile`
- Changed `EXPOSE 3000` to `EXPOSE 3030`

#### `docker-compose.yml`
- Updated port mapping: `13002:3030` (was `13002:3000`)
- Changed environment variable from `NEXT_PUBLIC_API_URL` to `NEXT_PUBLIC_WEATHER_API_URL`

### 2. API Configuration Updates

#### `frontend/src/utils/api.ts`
- **AUTH_API_BASE**: Changed from `demo.markpost.dev` to `auth.markpost.dev` (production) and `localhost:3000` (dev)
- **WEATHER_API_BASE**: Changed from `demo.markpost.dev` to `weather.markpost.dev` (production)
- **fetchWithAuthRetry**: Updated to redirect to external auth service on 401:
  - Tries to refresh token using `auth.markpost.dev/api/auth/v1/refresh`
  - On failure, redirects to `auth.markpost.dev/login?callback=<weather-service-url>`
  - Callback URL is `localhost:3030` (dev) or `weather.markpost.dev` (production)

### 3. Removed Authentication Pages

Deleted the following page directories:
- `frontend/src/app/login/`
- `frontend/src/app/register/`
- `frontend/src/app/profile/`
- `frontend/src/app/security/`
- `frontend/src/app/forgot-password/`
- `frontend/src/app/reset-password/`
- `frontend/src/app/verify-email/`

### 4. Removed Authentication Components

Deleted the following component files:
- `frontend/src/components/Login.tsx`
- `frontend/src/components/Register.tsx`
- `frontend/src/components/ProfilePage.tsx`
- `frontend/src/components/SecurityPage.tsx`
- `frontend/src/components/ForgotPassword.tsx`
- `frontend/src/components/ResetPassword.tsx`
- `frontend/src/components/Setup2FA.tsx`
- `frontend/src/components/PasskeyModal.tsx`
- `frontend/src/components/BackupCodesModal.tsx`
- `frontend/src/components/ChangePassword.tsx`
- `frontend/src/components/DeleteAccountModal.tsx`
- `frontend/src/components/PublicLanguageSelector.tsx`

### 5. Removed Utility Files

Deleted the following utility files:
- `frontend/src/utils/callbackValidation.ts`
- `frontend/src/utils/profilePicture.ts`

### 6. Removed Test Files

Deleted the following test files:
- `frontend/src/__tests__/utils/callbackValidation.test.ts`
- `frontend/src/__tests__/utils/profilePicture.test.ts`
- `frontend/src/__tests__/components/Register.test.tsx`
- `frontend/src/__tests__/components/ChangePassword.test.tsx`
- `frontend/src/__tests__/app/login-page.test.tsx`

### 7. Updated Sidebar Component

#### `frontend/src/components/Sidebar.tsx`
- Updated interface: Changed `activePage` from `"dashboard" | "profile" | "security"` to `"dashboard"`
- Updated `onNavigate` to only accept `"dashboard"`
- Added external navigation handler: `handleExternalNavigation(path: string)`
- Profile and Security menu items now redirect to external auth service with callback:
  - Profile: `auth.markpost.dev/profile?callback=<weather-service-url>`
  - Security: `auth.markpost.dev/security?callback=<weather-service-url>`

### 8. Updated Main Page

#### `frontend/src/app/page.tsx`
- Updated `AUTH_API_BASE` to use `auth.markpost.dev` (production) or `localhost:3000` (dev)
- Updated `WEATHER_API_BASE` to use `weather.markpost.dev` (production)
- **checkLogin effect**: On 401, redirects to external auth service login instead of local `/login`
- **fetchWeatherWithAuth**: On 401 after refresh attempt, redirects to external auth service
- **handleLogout**: Now redirects to external auth service after logout
- **handleNavigate**: Simplified to only handle dashboard navigation

### 9. Cleaned Translation Files

Removed authentication-related translations from all language files:
- `frontend/messages/en.json`
- `frontend/messages/nl.json`
- `frontend/messages/de.json`
- `frontend/messages/fr.json`

Removed sections:
- `login`
- `register`
- `forgotPassword`
- `resetPassword`
- `profile`
- `security`

Cleaned `pageTitle` to only include:
- `dashboard`

Kept sidebar translations for Profile and Security (used for menu items that redirect externally).

## Authentication Flow

### New Authentication Flow:
1. User visits `weather.markpost.dev` (or `localhost:3030`)
2. Frontend checks authentication with `auth.markpost.dev/api/auth/v1/user`
3. If 401:
   - Attempts token refresh via `auth.markpost.dev/api/auth/v1/refresh`
   - If refresh fails, redirects to `auth.markpost.dev/login?callback=<weather-url>`
4. After successful login on auth service, user is redirected back to weather service
5. Subsequent API calls use `fetchWithAuthRetry` which handles 401s automatically

### Profile/Security Access:
- Clicking "Profile" or "Security" in sidebar redirects to:
  - `auth.markpost.dev/profile?callback=<weather-url>`
  - `auth.markpost.dev/security?callback=<weather-url>`
- After making changes, user can return to weather service via the callback

## Testing

All tests pass (71 tests):
- ✅ Integration tests (weather-retry)
- ✅ API utility tests
- ✅ Retry utility tests
- ✅ Component tests (LocationEditModal, Modal, LocationBar, Sidebar)

## Environment Variables

Required environment variables:
- `NEXT_PUBLIC_WEATHER_API_URL`: Weather API base URL (default: `http://localhost:12001` for dev)

Note: Auth API URL is determined automatically based on hostname (localhost vs production).

## Next Steps

1. Deploy the updated frontend to `weather.markpost.dev`
2. Ensure auth service is running at `auth.markpost.dev` (or `localhost:3000` for dev)
3. Test the authentication flow end-to-end
4. Verify callback redirects work correctly

## Benefits

1. **Separation of Concerns**: Weather service focuses only on weather functionality
2. **Centralized Authentication**: Single auth service can serve multiple applications
3. **Reduced Codebase**: Removed ~150 lines of authentication code and components
4. **Better Maintainability**: Auth changes only need to be made in one place
5. **Consistent Auth Experience**: All markpost.dev services use the same auth UI/flow

