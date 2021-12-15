package com.getvisitapp.google_fit.util

import android.content.Context


class GoogleFitExtension @JvmOverloads constructor(context: Context,weClientId:String="") :
    GoogleFitConnector(context,weClientId) {

    companion object :
        SingletonHolderForTwoVariable<GoogleFitExtension, Context, String>(::GoogleFitExtension)

}

