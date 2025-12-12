// Centralized API base URL and fetchWithAuthRetry utility

export const isDev = typeof window !== "undefined" && window.location.hostname === "localhost";

// Configure these URLs based on your deployment
// AUTH_API_BASE should point to your external authentication service
// WEATHER_API_BASE should point to your weather service backend
export const AUTH_API_BASE = isDev
  ? "http://localhost:3000"
  : process.env.NEXT_PUBLIC_AUTH_API_URL || "https://your-auth-service.com";
export const WEATHER_API_BASE = isDev
  ? (process.env.NEXT_PUBLIC_WEATHER_API_URL || "http://localhost:13001")
  : process.env.NEXT_PUBLIC_WEATHER_API_URL || "https://your-weather-api.com";

/**
 * Generic fetch utility that retries on 401 by refreshing the token, then retries the original request.
 * If refresh also fails with 401, redirects to auth service login with callback URL.
 */
export async function fetchWithAuthRetry(input: RequestInfo, init?: RequestInit): Promise<Response> {
  let res = await fetch(input, { ...init, credentials: "include" });
  if (res.status !== 401) return res;

  // Try to refresh token from auth service
  const refreshRes = await fetch(`${AUTH_API_BASE}/api/auth/v1/refresh`, { method: "POST", credentials: "include" });
  if (refreshRes.status === 401) {
    // Redirect to auth service login with callback URL
    if (typeof window !== "undefined") {
      const callbackUrl = `${window.location.origin}${window.location.pathname}`;
      window.location.href = `${AUTH_API_BASE}/login?callback=${encodeURIComponent(callbackUrl)}`;
    }
    throw new Error("Session expired. Redirecting to login.");
  }
  // Retry original request
  res = await fetch(input, { ...init, credentials: "include" });
  return res;
}

/**
 * Saved Locations API functions
 */

export async function getSavedLocations() {
  const res = await fetchWithAuthRetry(`${WEATHER_API_BASE}/api/weather/v1/saved-locations`);
  if (!res.ok) {
    throw new Error("Failed to fetch saved locations");
  }
  return res.json();
}

export async function saveLocation(location: any) {
  const res = await fetchWithAuthRetry(`${WEATHER_API_BASE}/api/weather/v1/saved-locations`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(location),
  });
  if (!res.ok) {
    throw new Error("Failed to save location");
  }
  return res.json();
}

export async function deleteSavedLocation(id: number) {
  const res = await fetchWithAuthRetry(`${WEATHER_API_BASE}/api/weather/v1/saved-locations/${id}`, {
    method: "DELETE",
  });
  if (!res.ok && res.status !== 204) {
    throw new Error("Failed to delete saved location");
  }
}

export async function reorderSavedLocations(locationIds: number[]) {
  const res = await fetchWithAuthRetry(`${WEATHER_API_BASE}/api/weather/v1/saved-locations/reorder`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ locationIds }),
  });
  if (!res.ok && res.status !== 204) {
    throw new Error("Failed to reorder saved locations");
  }
}

