# Web Developer Fix Plan: Camera Capture Data URL Upload

## Summary

Android no longer returns a `content://` URI for camera capture. The camera callback now returns a web-readable image data URL:

```js
window.checkTheCameraPermission(true, "data:image/jpeg;base64,<base64-image>", null);
```

The web app must stop trying to `fetch()` or XHR the captured source. Instead, convert the returned data URL into a `File` or `Blob` and pass that into the existing upload flow.

## Native Contract

Web still starts camera capture with:

```js
Android.requestPermission("CAMERA");
```

Android responds with:

```js
window.checkTheCameraPermission(granted, source, error);
```

Success:

```js
window.checkTheCameraPermission(
  true,
  "data:image/jpeg;base64,<base64-image>",
  null
);
```

Failure:

```js
window.checkTheCameraPermission(false, null, "PERMISSION_DENIED");
```

Possible `error` values:

```txt
UNSUPPORTED_TYPE
PERMISSION_DENIED
CAMERA_UNAVAILABLE
CAPTURE_CANCELLED
CAPTURE_FAILED
REQUEST_IN_PROGRESS
```

## Required Web Changes

Remove the old logic that does this:

```js
await fetch(capturedUri);
```

Do not fetch `content://` or `data:image/...` camera values. The camera success value is already the image payload.

Implement or update the global callback:

```js
let nativeCameraRequestInProgress = false;

window.checkTheCameraPermission = async function (granted, source, error) {
  nativeCameraRequestInProgress = false;

  if (!granted) {
    handleCameraFailure(error);
    return;
  }

  if (!source || !source.startsWith("data:image/")) {
    handleCameraFailure("CAPTURE_FAILED");
    return;
  }

  const file = dataUrlToFile(source, "camera_capture.jpg");
  await uploadCapturedImage(file);
};
```

Keep the camera button guarded:

```js
function openNativeCamera() {
  if (nativeCameraRequestInProgress) {
    handleCameraFailure("REQUEST_IN_PROGRESS");
    return;
  }

  if (
    !window.Android ||
    typeof window.Android.requestPermission !== "function"
  ) {
    handleCameraFailure("ANDROID_BRIDGE_UNAVAILABLE");
    return;
  }

  nativeCameraRequestInProgress = true;
  window.Android.requestPermission("CAMERA");
}
```

Convert the data URL to a file:

```js
function dataUrlToFile(dataUrl, fileName) {
  const [header, base64] = dataUrl.split(",");
  const mimeType = header.match(/data:(.*);base64/)?.[1] || "image/jpeg";
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);

  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }

  return new File([bytes], fileName, { type: mimeType });
}
```

Map failure states to the existing UI:

```js
function handleCameraFailure(error) {
  nativeCameraRequestInProgress = false;

  switch (error) {
    case "PERMISSION_DENIED":
      showCameraError("Camera permission was denied.");
      break;
    case "CAPTURE_CANCELLED":
      showCameraError("Camera capture was cancelled.");
      break;
    case "REQUEST_IN_PROGRESS":
      showCameraError("Camera is already opening.");
      break;
    case "CAMERA_UNAVAILABLE":
      showCameraError("Camera is unavailable on this device.");
      break;
    case "ANDROID_BRIDGE_UNAVAILABLE":
      showCameraError("Camera is unavailable in this browser.");
      break;
    default:
      showCameraError("Could not capture image. Please try again.");
      break;
  }
}
```

## CSP Notes

- `connect-src content:` is no longer required for this camera upload flow.
- If the web app previews the returned data URL in an image element, ensure CSP allows data images:

```txt
img-src 'self' https: data:;
```

- If the app uses `default-src` as fallback for images, ensure the effective image policy allows `data:`.

## Test Checklist

- Capture image and confirm callback receives `source` starting with `data:image/jpeg;base64,`.
- Confirm web does not call `fetch()` or XHR for the camera source.
- Convert the data URL to `File` and verify upload succeeds.
- Confirm image preview works if preview is shown.
- Deny camera permission and confirm `PERMISSION_DENIED` handling.
- Cancel camera and confirm `CAPTURE_CANCELLED` handling.
- Double tap camera and confirm duplicate requests are blocked or handled as `REQUEST_IN_PROGRESS`.
- Test a large captured image on Android WebView; if upload is slow or memory-heavy, report it so Android can add compression.

## Ownership

- Android owns permission, camera launch, file capture, Base64 encoding, and callback invocation.
- Web owns callback handling, data URL to `File` conversion, preview, upload, loading state, and error UI.
- Location flow is unchanged.
