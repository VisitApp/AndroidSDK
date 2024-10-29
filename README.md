
# Navi SDK:

## Gradle setup:  

``` 
dependencies {  
   implementation 'com.github.VisitApp:AndroidSDK:abhi-2.0'
 }  
 
```

## Add plugin
```
  id 'com.google.gms.google-services'
```


#### Some Additional dependencies are required for proper functioning of the library:

```
   implementation 'androidx.core:core-ktx:1.13.1'
    implementation 'androidx.core:core-ktx:1.13.1'
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'


    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'

    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    def retrofit_version="2.11.0"
    implementation "com.squareup.retrofit2:retrofit:$retrofit_version"
    implementation "com.squareup.retrofit2:converter-gson:$retrofit_version"
    implementation 'de.hdodenhof:circleimageview:3.1.0'

    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation 'com.github.delight-im:Android-AdvancedWebView:v3.0.0'


    //Chucker
    debugApi "com.github.chuckerteam.chucker:library:4.0.0"
    releaseApi "com.github.chuckerteam.chucker:library-no-op:4.0.0"


    implementation "androidx.browser:browser:1.8.0"

```

### To initialize the SDK: 
```
IntiateSdk.s(
            context,
            false, 
            ssoLink
        )
```

### Example

```
IntiateSdk.s(
            this,
            false,
            "https://abhi.getvisitapp.net/sso?userParams=t1Q8E-aBzmU2_Y7xi17gjrMzTGsvH32N9GeePukIc-KjG6bmMrfsLpmrzpqNiKFkgfSB3NMF0SPeQMXITQ6QXBLpUzc7fLrgsdyF8MOw46_02_YH7ogZH_oekVYByGaQ-qSIyP3P8aCxGq5tIZ7QRxImqGtQxeV4pJXJCddGihL7-eIbe5ivM-cNQMM3iHkoNGR7ximmaCOK6iXVQVfxLMyuSFL5O7VCild5iphHX1s&clientId=abhi-58fd14",
            "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"
        )
```
 
#### Proguard rule
For progaurd rule copy the rules from `google-fit/progaurd-rules.pro` file

##### Use ` mytag ` to see the logs of the SDK.

##
Document to configure Google Fit on your Google Cloud Console:  
https://drive.google.com/file/d/1uqhlTLWzYlfcDlJa6tfAbhMOS6QNJ94h/view?usp=sharing
