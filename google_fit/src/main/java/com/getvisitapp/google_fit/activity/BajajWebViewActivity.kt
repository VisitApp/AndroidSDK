package com.getvisitapp.google_fit.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.getvisitapp.google_fit.BuildConfig
import com.getvisitapp.google_fit.R
import com.getvisitapp.google_fit.databinding.BajajFinservFragmentBinding
import com.getvisitapp.google_fit.util.makeStatusBarTransparent
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class BajajWebViewActivity : AppCompatActivity() {

    private var mainUrl: String? = null
    private val TAG = "BajajWebViewActivity"


    lateinit var binding: BajajFinservFragmentBinding

    private var mUploadMessage: ValueCallback<Uri>? = null
    var uploadMessage: ValueCallback<Array<Uri>>? = null
    val REQUEST_SELECT_FILE = 100
    private val mCapturedImageURI: Uri? = null
    private val FILECHOOSER_RESULTCODE = 2888
    private var mCameraPhotoPath: String? = null


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_web_view_bajaj)
        this.makeStatusBarTransparent()

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }




        mainUrl =
            "https://fitpass.getvisitapp.com/sso?userParams=eyJkb2IiOiIxOTg3LTA1LTA5VDAwOjAwOjAwKzA1OjMwIiwiZW1haWwiOiIxMjUyMDU3MDcyLmZpdHBhc3NAZ21haWwuY29tIiwiZ2VuZGVyIjoiTWFsZSIsIm1lbWJlcklkIjoxMjUyMDU3MDcyLCJtb2R1bGVOYW1lIjoicGhhcm1hY3kiLCJuYW1lIjoiSm5hbmVuZHJhIFZlZXIifQ==&clientId=fit-pass-3a9c3"


        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)


        binding.webView.settings.loadsImagesAutomatically = true
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.setGeolocationEnabled(true)


        binding.webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        binding.webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        binding.webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY


        binding.webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Timber.d("onPageFinished: " + view.title)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Timber.d("onPageStarted: $url ")
            }


            // If you will not use this method url links are opeen in new brower
            // not in webview
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) {
                    return false
                }
                if (URLUtil.isNetworkUrl(url)) {
                    return false
                }
                try {
                    val uri = Uri.parse(url)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                return true
            }


        }

//        https://stackoverflow.com/questions/61495505/android-webcam-error-in-webview-camera-permission-not-working
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                for (i in request.resources.indices) {

                    Timber.d("request!!.resources: " + request.resources[i])

                    if (request.resources[i].equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE));
                        break;
                    }
                }
            }

            override fun onReceivedTitle(view: WebView, titleReceived: String) {

            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.linearProgressBar.progress = newProgress
                if (newProgress == 100) {
                    binding.linearProgressBar.visibility = View.GONE
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String, callback: GeolocationPermissions.Callback
            ) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                Timber.d("onGeolocationPermissionsShowPrompt called.")
                if (ActivityCompat.checkSelfPermission(
                        this@BajajWebViewActivity, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Timber.d("onGeolocationPermissionsShowPrompt callback invoked")
                    callback.invoke(origin, true, true)
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePath: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {

                Timber.d("filePath: $filePath, fileChooserParams: $fileChooserParams")

                try {
                    mUploadMessage?.onReceiveValue(null)

                    uploadMessage = filePath
                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)


                    // Create the File where the photo should go
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                    } catch (ex: IOException) {
                        Log.e("ErrorCreatingFile", "Unable to create Image File", ex)
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        val fileUri = FileProvider.getUriForFile(
                            /* context = */ this@BajajWebViewActivity,
                            /* authority = */
                            this@BajajWebViewActivity.applicationContext.packageName,
                            /* file = */
                            photoFile
                        )
                        takePictureIntent!!.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    Log.i(TAG, "PhotoPath - $mCameraPhotoPath")
                    val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    contentSelectionIntent.type = "*/*"
                    val extraMimeTypes = arrayOf("application/pdf", "application/doc", "image/*")
                    contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes)


                    val intentArray: Array<Intent?> = arrayOf(takePictureIntent) ?: arrayOfNulls(0)
                    val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                    startActivityForResult(
                        chooserIntent, REQUEST_SELECT_FILE
                    )
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                return true
            }
        }

        binding.webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            try {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@BajajWebViewActivity,
                    "Something went wrong while downloading the file.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val resolutionForResult = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { activityResult ->
            //load page of bajaj finserv
            if (activityResult.resultCode == RESULT_OK) {
                Timber.d("GPS is on")
            } else {
                //don't do anything here.
            }
        }


        val multiplePermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            Timber.d("result: $result")
            var allPermissionAllowed = true
            for ((_, value) in result) {
                if (value == false) {
                    allPermissionAllowed = false
                }
            }
            if (allPermissionAllowed) {
                Timber.d("all permission allowed")
                promptUserToTurnOnGPS({
                    Timber.d("all permission are present and gps is on")

                }) { intentSenderRequest: IntentSenderRequest ->
                    resolutionForResult.launch(intentSenderRequest)

                }
            } else {
                Timber.d("all permission not allowed")
                this@BajajWebViewActivity.showSettingsDialogForNBFC()
            }
        }


        val permissions: List<String> = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            listOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        else
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        multiplePermission.launch(
            permissions.toTypedArray()
        )

        binding.webView.loadUrl(mainUrl!!)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finish()
                }
            }
        })


    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (requestCode != REQUEST_SELECT_FILE || uploadMessage == null) {
                    super.onActivityResult(requestCode, resultCode, data)
                    return
                }
                var results: Array<Uri>? = null
                if (resultCode == RESULT_OK) {
                    if (data == null || (data != null && (data.extras == null && data.clipData == null && data.data == null))) {

                        if (mCameraPhotoPath != null) {
                            results = arrayOf(Uri.parse(mCameraPhotoPath))
                        }
                    } else {
                        if (data != null) {
                            if (data.extras != null && data.extras!!.get("data") != null) {
                                results = getImageUri(
                                    this@BajajWebViewActivity, (data.extras!!["data"] as Bitmap?)!!
                                )?.let {
                                    arrayOf(
                                        it
                                    )
                                }!!
                            } else {
                                if (data.clipData != null) {
                                    val count = data.clipData!!.itemCount
                                    results = Array<Uri>(count) { index ->
                                        data.clipData!!.getItemAt(index).uri
                                    }

                                } else if (data.data != null) {
                                    val dataString = data.dataString
                                    if (dataString != null) {
                                        results = arrayOf(Uri.parse(dataString))
                                    }
                                }
                            }
                        }
                    }
                }
                uploadMessage!!.onReceiveValue(results)
                uploadMessage = null
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null) {
                    super.onActivityResult(requestCode, resultCode, data)
                    return
                }
                if (requestCode == FILECHOOSER_RESULTCODE) {
                    if (null == mUploadMessage) {
                        return
                    }
                    var result: Uri? = null
                    try {
                        result = if (resultCode != RESULT_OK) {
                            null
                        } else {

                            // retrieve from the private variable if the intent is null
                            if (data == null) mCapturedImageURI else data.data
                        }
                    } catch (e: java.lang.Exception) {
                        Toast.makeText(
                            applicationContext, "activity :$e", Toast.LENGTH_LONG
                        ).show()
                    }
                    mUploadMessage!!.onReceiveValue(result)
                    mUploadMessage = null
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return
    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }


    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    fun promptUserToTurnOnGPS(
        onSuccessListener: () -> Unit,
        onFailureListener: (intentSenderRequest: IntentSenderRequest) -> Unit
    ) {

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5 * 1000)
            .setWaitForAccurateLocation(true).setMinUpdateIntervalMillis(2 * 1000)
            .setMaxUpdateDelayMillis(5 * 1000).build()

        val locationSettingsRequestBuilder = LocationSettingsRequest.Builder();

        locationSettingsRequestBuilder.addLocationRequest(locationRequest);
        locationSettingsRequestBuilder.setAlwaysShow(true);

        val settingsClient: SettingsClient =
            LocationServices.getSettingsClient(this);


        val task: Task<LocationSettingsResponse> =
            settingsClient.checkLocationSettings(locationSettingsRequestBuilder.build());
        task.addOnSuccessListener(this, OnSuccessListener {
            onSuccessListener() //it gets called immediately if the gps is turn on for the user.
        })

        task.addOnFailureListener { exception ->
            when (exception) {
                is ResolvableApiException -> {
                    try {
                        val intentSenderRequest =
                            IntentSenderRequest.Builder(exception.resolution).build()
                        onFailureListener(intentSenderRequest)
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun showSettingsDialogForNBFC() {
        var builder = AlertDialog.Builder(this)
        builder.setTitle("Permission Required")
        builder.setMessage("You need \"Camera\",\"Files and Media\",\"Microphone\" and \"Location\" permission to avail this feature. Please provide them from settings.")
        builder.setPositiveButton("Settings", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts(
                    "package",
                    applicationContext.packageName,
                    null
                )
                intent.data = uri
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }

        })
        builder.setCancelable(true)
        builder.create().show()
    }


}