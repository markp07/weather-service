import { fetchWithRetry } from "./retry";

// Centralized API base URL and fetchWithAuthRetry utility

export const isDev = typeof window !== "undefined" && window.location.hostname === "localhost";

// Configure these URLs based on your deployment
// NEXT_PUBLIC_AUTH_API_URL should point to your external authentication service
// NEXT_PUBLIC_WEATHER_API_URL should point to your weather service backend
export const AUTH_API_BASE = isDev ? "http://localhost:3000" : (process.env.NEXT_PUBLIC_AUTH_API_URL || "https://auth.yourdomain.tld");
export const WEATHER_API_BASE = isDev ? "http://localhost:13001" : (process.env.NEXT_PUBLIC_WEATHER_API_URL || "https://weather.yourdomain.tld");

export function getCurrentCallbackUrl() {
  if (typeof window === "undefined") {
    return "/";
  }

  return `${window.location.origin}${window.location.pathname}${window.location.search}`;
}

export function getAppHomeCallbackUrl() {
  if (typeof window === "undefined") {
    return "/";
  }

  return `${window.location.origin}/`;
}

export function buildAuthLoginUrl(callbackUrl = getCurrentCallbackUrl()) {
  return `${AUTH_API_BASE}/login?callback=${encodeURIComponent(callbackUrl)}`;
}

export function redirectToLogin(callbackUrl = getCurrentCallbackUrl()) {
  if (typeof window === "undefined") {
    return;
  }

  window.location.href = buildAuthLoginUrl(callbackUrl);
}

export async function refreshAuthToken(): Promise<boolean> {
  const refreshRes = await fetch(`${AUTH_API_BASE}/api/auth/v1/refresh`, { method: "POST", credentials: "include" });
  return refreshRes.ok;
}

async function fetchRequest(input: RequestInfo, init?: RequestInit, retry5xx = false): Promise<Response> {
  if (retry5xx) {
    return fetchWithRetry(input, init);
  }
  return fetch(input, { ...init, credentials: "include" });
}

/**
 * Generic fetch utility that retries on 401 by refreshing the token, then retries the original request.
 * If refresh also fails with 401, redirects to auth service login with callback URL.
 */
export async function fetchWithAuthRetry(
  input: RequestInfo,
  init?: RequestInit,
  options: { retry5xx?: boolean } = {}
): Promise<Response> {
  const { retry5xx = false } = options;
  let res = await fetchRequest(input, init, retry5xx);
  if (res.status !== 401) return res;

  const refreshSucceeded = await refreshAuthToken();
  if (!refreshSucceeded) {
    redirectToLogin();
    throw new Error("Session expired. Redirecting to login.");
  }

  res = await fetchRequest(input, init, retry5xx);
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
