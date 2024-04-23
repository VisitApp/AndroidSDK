

## Gradle setup:  

``` 
dependencies {  
    implementation 'com.github.VisitApp:AndroidSDK:abhi-1.0'
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

### To initialize the SDK: 
```
IntiateSdk.s(
            context,
            false, 
            magicLink,
            google_default_client_id
        )
```
 
#### Proguard rule
For progaurd rule copy the rules from `google-fit/progaurd-rules.pro` file

##### Use ` mytag ` to see the logs of the SDK.

##
Document to configure Google Fit on your Google Cloud Console:  
https://drive.google.com/file/d/1uqhlTLWzYlfcDlJa6tfAbhMOS6QNJ94h/view?usp=sharing
