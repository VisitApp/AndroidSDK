package com.getvisitapp.google_fit.util

import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch

class AudioHelper {

    companion object {
        fun selectDevice(audioSwitch: AudioSwitch, devices: List<AudioDevice>) {
            devices.find { it is AudioDevice.Speakerphone }?.let { audioSwitch.selectDevice(it) }
        }
    }
}