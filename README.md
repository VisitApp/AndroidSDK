

## Gradle setup:  

``` 
dependencies {  
	      implementation 'com.github.VisitApp:AndroidSDK:v1.70'
 }  
 
```

## Add plugin
```
  id 'kotlin-kapt'
  id 'com.google.gms.google-services'
```


#### Some Additional dependencies are required for proper functioning of the library:

```
    implementation "com.google.android.gms:play-services-gcm:17.0.0"
    implementation "com.google.android.gms:play-services-fitness:20.0.0"
    implementation "com.google.android.gms:play-services-auth:19.0.0"


    implementation 'io.reactivex:rxjava:1.3.0'
    implementation 'io.reactivex:rxandroid:1.2.1'


    implementation 'com.twilio:video-android:7.1.2'
    implementation 'com.twilio:audioswitch:1.1.5'

    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2'

    implementation 'com.github.bumptech.glide:glide:4.11.0'
    kapt 'com.github.bumptech.glide:compiler:4.11.0'


    def retrofit_version = "2.9.0"
    implementation "com.squareup.retrofit2:retrofit:$retrofit_version"
    implementation "com.squareup.retrofit2:converter-gson:$retrofit_version"
    implementation 'de.hdodenhof:circleimageview:3.1.0'
    implementation 'com.squareup.okhttp3:okhttp:3.8.1'

    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    implementation 'com.github.delight-im:Android-AdvancedWebView:v3.0.0'

    implementation('com.getkeepsafe.relinker:relinker:1.4.4') {
        version {
            strictly '1.4.4'
        }
    }
    
    implementation("org.greenrobot:eventbus:3.3.1")
    
    implementation  "androidx.browser:browser:1.3.0"


```

We use EventBus to explose some event from the library that your application can consume. 

Add the following code in the activity where you want to consume the events broadcasted by this library.

```

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        event?.let { eventType ->
            Log.d(TAG, "event:${event.eventType}")


            when (eventType.eventType) {
                VisitEventType.AskForFitnessPermission -> {

                }
                VisitEventType.AskForLocationPermission -> {


                }
                VisitEventType.FitnessPermissionGranted -> {


                }
                is VisitEventType.RequestHealthDataForDetailedGraph -> {

                    val graphEvent =
                        event.eventType as VisitEventType.RequestHealthDataForDetailedGraph

                }
                is VisitEventType.StartVideoCall -> {
                    val callEvent =
                        event.eventType as VisitEventType.StartVideoCall


                }
                is VisitEventType.HRA_Completed -> {

                }
                is VisitEventType.GoogleFitConnectedAndSavedInPWA -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        //passing event to Visit PWA to close itself
                        EventBus.getDefault().post(ClosePWAEvent())


                    }, 200)
                }
                is VisitEventType.HRAQuestionAnswered -> {
                     // can be used for analytics events
                    val hraQuestionEvent = event.eventType as VisitEventType.HRAQuestionAnswered
                    Log.d(
                        "mytag",
                        "current:${hraQuestionEvent.current} total:${hraQuestionEvent.total}"
                    )
                }
            }

        }

    }


    override fun onStart() {
        super.onStart()

        //unregister any previously registered event listener first before registering.
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        EventBus.getDefault().register(this)
    }


    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

```

Use `GoogleFitAccessChecker.java` to check for the state of Google Fit Permission. Check `MainActivity.kt` to see how it is implemented 

### To initialize the SDK: 
```
IntiateSdk.s(
            context,
            false, 
            magicLink,
            base_url,
            auth_token,
            google_default_client_id
        )
 ```
 
 
 base_url examples - 
 
https://web.getvisitapp.xyz/sso?userParams=XHymLn6AQGWjniLhaFGfdMRTtogNkB-3ub6LajcEO9M8IoVqqHq9lWFIXPAGZGdMh0CXGII5hf66lIYf2gDCBZELtckIK-COGoIuzDqATv_qeXcl3g12RTvS8vnOShIy&clientId=visit-web-app-3498hjkfd832 -- Online consults
 
https://web.getvisitapp.xyz/sso?userParams=XHymLn6AQGWjniLhaFGfdMRTtogNkB-3ub6LajcEO9M8IoVqqHq9lWFIXPAGZGdMh0CXGII5hf66lIYf2gDCBZELtckIK-COGoIuzDqATv9fyVyuxcrqSwfX3Nv-t22N&clientId=visit-web-app-3498hjkfd832 -- HRA
 
https://web.getvisitapp.xyz/sso?userParams=XHymLn6AQGWjniLhaFGfdMRTtogNkB-3ub6LajcEO9M8IoVqqHq9lWFIXPAGZGdMh0CXGII5hf66lIYf2gDCBZELtckIK-COGoIuzDqATv8t9-s4toFaKHOkzFKnriRU&clientId=visit-web-app-3498hjkfd832  -- OPD

https://web.getvisitapp.xyz/sso?userParams=XHymLn6AQGWjniLhaFGfdMRTtogNkB-3ub6LajcEO9M8IoVqqHq9lWFIXPAGZGdMh0CXGII5hf66lIYf2gDCBZELtckIK-COGoIuzDqATv9XqLPagqnIMhr6XGktuMkN&clientId=visit-web-app-3498hjkfd832 -- LAB TEST

https://web.getvisitapp.xyz/sso?userParams=XHymLn6AQGWjniLhaFGfdMRTtogNkB-3ub6LajcEO9M8IoVqqHq9lWFIXPAGZGdMh0CXGII5hf66lIYf2gDCBZELtckIK-COGoIuzDqATv8S8HabSqbWql3MKly6H82e&clientId=visit-web-app-3498hjkfd832 -- PHARMACY
 
#### Use `VisitStepSyncHelper` to sync steps manually (this will only work if the google fit is connected).
```
 val syncStepHelper = VisitStepSyncHelper(context = this, default_client_id)
 syncStepHelper.syncSteps(tataAIG_base_url, tataAIG_auth_token)
```        
#### Proguard rule
For progaurd rule copy the rules from `google-fit/progaurd-rules.pro` file

##### Use ` mytag ` to see the logs of the SDK.

##### Use `openGoogleFit()` to open Google Fit app. It returns false if the google fit app is not installed.

##### Use this to handle HRA retry logic from your end
```
val syncStepHelper = VisitStepSyncHelper(context = this, default_client_id)
syncStepHelper.sendHRAInComplete(tataAIG_base_url, tataAIG_auth_token)
```

##
Document to configure Google Fit on your Google Cloud Console:  
https://drive.google.com/file/d/1uqhlTLWzYlfcDlJa6tfAbhMOS6QNJ94h/view?usp=sharing
