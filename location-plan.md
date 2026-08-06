# Web Integration Plan: Android Location Permission and GPS Callback

## Summary

The web app will trigger the Android SDK location permission flow through the WebView bridge. Android will handle both required native steps:

1. Android runtime location permission.
2. GPS/location settings enablement.

After both steps complete, Android will call the existing web callback:

```js
window.checkTheGpsPermission(granted);
```

This flow only confirms that location permission and GPS/settings are ready. It does not return latitude or longitude directly.

## Native Bridge Contract

### Web to Android

Call this when the user taps "Use Current Location":

```js
Android.requestPermission("LOCATION");
```

The SDK also keeps the older location bridge for backward compatibility:

```js
Android.getLocationPermissions();
```

Both calls use the same Android location permission flow and the same callback.

### Android to Web

Android will respond with:

```js
window.checkTheGpsPermission(true);
```

or:

```js
window.checkTheGpsPermission(false);
```

Callback meaning:

| Value | Meaning |
| --- | --- |
| `true` | Android location permission is granted and GPS/location settings are enabled. |
| `false` | Android permission was denied, GPS/settings were cancelled or unavailable, or native location flow failed. |

No coordinates are included in this callback. After receiving `true`, web should call its existing geolocation/current-location lookup.

## Web Implementation

Register the callback before calling `Android.requestPermission("LOCATION")`.

```js
let nativeLocationRequestInProgress = false;

window.checkTheGpsPermission = async function (granted) {
  nativeLocationRequestInProgress = false;

  if (!granted) {
    handleLocationFailure();
    return;
  }

  await fetchCurrentLocationFromWebGeolocation();
};
```

Trigger native location permission from the "Use Current Location" button:

```js
function useCurrentLocation() {
  if (nativeLocationRequestInProgress) {
    return;
  }

  if (
    !window.Android ||
    typeof window.Android.requestPermission !== "function"
  ) {
    handleLocationFailure("ANDROID_BRIDGE_UNAVAILABLE");
    return;
  }

  nativeLocationRequestInProgress = true;
  window.Android.requestPermission("LOCATION");
}
```

Use the existing web location fetch only after Android returns `true`:

```js
async function fetchCurrentLocationFromWebGeolocation() {
  if (!navigator.geolocation) {
    handleLocationFailure("WEB_GEOLOCATION_UNAVAILABLE");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    function (position) {
      handleCurrentLocation({
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
      });
    },
    function () {
      handleLocationFailure("WEB_GEOLOCATION_FAILED");
    },
    {
      enableHighAccuracy: true,
      timeout: 15000,
      maximumAge: 0,
    }
  );
}
```

Handle failure in the web UI:

```js
function handleLocationFailure(reason) {
  nativeLocationRequestInProgress = false;

  switch (reason) {
    case "ANDROID_BRIDGE_UNAVAILABLE":
      showLocationError("Current location is unavailable in this browser.");
      break;
    case "WEB_GEOLOCATION_UNAVAILABLE":
      showLocationError("Location is not supported in this browser.");
      break;
    case "WEB_GEOLOCATION_FAILED":
      showLocationError("Could not read your current location.");
      break;
    default:
      showLocationError("Please enable location permission and GPS to continue.");
      break;
  }
}
```

## Important Behavior Notes

- `Android.requestPermission("LOCATION")` does not return latitude/longitude.
- `window.checkTheGpsPermission(true)` means Android native permission and GPS/settings are ready.
- Web still owns the actual current-location lookup after the callback returns `true`.
- If native returns `false`, web should show a retry or manual location fallback.
- Existing `Android.getLocationPermissions()` remains supported and behaves the same way.

## Test Checklist

- Happy path: call `Android.requestPermission("LOCATION")`, grant Android permission, enable GPS, receive `checkTheGpsPermission(true)`, then fetch coordinates through web geolocation.
- Permission denied: deny Android location permission and verify `checkTheGpsPermission(false)`.
- GPS cancelled: grant permission but cancel the GPS/settings prompt and verify `checkTheGpsPermission(false)`.
- Already ready: with permission and GPS already enabled, call again and verify `checkTheGpsPermission(true)` without unnecessary permission prompts.
- Duplicate click: tap "Use Current Location" twice quickly and verify web prevents duplicate in-flight requests.
- Unsupported environment: if `window.Android.requestPermission` is missing, web shows fallback error.
- Backward compatibility: `Android.getLocationPermissions()` still triggers the same flow and callback.

## Ownership

- Android owns runtime location permission, GPS/settings prompt, and `window.checkTheGpsPermission(granted)` invocation.
- Web owns button state, callback registration, error UI, retry/manual fallback, and coordinate fetching after `granted === true`.
- Callback name remains fixed as `window.checkTheGpsPermission`.
