package com.example.googlefitsdk

import android.app.Application
import com.zomato.chatsdk.chatcorekit.init.ChatSdkAccessTokenData
import com.zomato.chatsdk.chatcorekit.network.request.BusinessContext
import com.zomato.chatsdk.init.ChatSDKInitCommunicator
import com.zomato.chatsdk.init.ChatSdk

class SdkApp : Application() {

    object ChatSDKInitCommunicatorImpl : ChatSDKInitCommunicator {
        override fun getAccessTokenData(): ChatSdkAccessTokenData {
            return ChatSdkAccessTokenData(accessToken = "", httpCode = 200)
        }

        override fun getBusinessContext(): BusinessContext {
            return BusinessContext(
                channelHandle = "",
                ticketGroupingId = "",
                ticketProperties = HashMap(),
                botProperties = HashMap()
            )
        }

        override fun getRefreshToken(): String {
            return ""
        }

    }


    override fun onCreate() {
        super.onCreate()

        TimberUtils.configTimber()

        ChatSdk.initialize(applicationContext = this, initInterface = ChatSDKInitCommunicatorImpl)
    }
}