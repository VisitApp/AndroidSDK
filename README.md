

## Gradle setup:  

``` 
dependencies {  
       implementation 'com.github.VisitApp:AndroidSDK:1.18'     
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


    implementation 'com.twilio:video-android:5.10.1'
    implementation 'com.twilio:audioswitch:1.0.0'

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


```

#### Follow the instructions from the link below to configure Google Fit into your project:  


https://drive.google.com/file/d/1eLbhSd4nDno85L1Dds6i716-NlBZdMnV/view?usp=sharing


We use EventBus to explose some event from the library that your application can consume. 

Add the following code in the activity where you want to consume the events broadcasted by this library.

```

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        event?.let { eventType ->
            Log.d(TAG, "event:${event.eventType}")


            when (eventType.eventType) {
                VisitEventType.AskForFitnessPermission -> {}
                VisitEventType.AskForLocationPermission -> {}
                VisitEventType.FitnessPermissionGranted -> {}
                is VisitEventType.RequestHealthDataForDetailedGraph -> {
                    val graphEvent =
                        event.eventType as VisitEventType.RequestHealthDataForDetailedGraph
                }
                is VisitEventType.StartVideoCall -> {
                    val callEvent =
                        event.eventType as VisitEventType.StartVideoCall
                }
                is VisitEventType.HRA_Completed -> {
                    Handler(Looper.getMainLooper()).postDelayed({

                        //passing event to Visit PWA to close itself
                        EventBus.getDefault().post(ClosePWAEvent())


                    }, 200)
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



