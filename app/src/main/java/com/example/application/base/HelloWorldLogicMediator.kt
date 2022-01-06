package com.example.application.base

import android.content.Context
import com.example.application.MCU.MCUHelloWorldLogic
import com.example.application.SFU.SFUHelloWorldLogic
import fm.liveswitch.LocalMedia

/**
 *  Created by paulbisioc on 05.01.2022
 */

class HelloWorldLogicMediator {
    companion object {
        fun getInstance(isSFU: Boolean, context: Context): BaseHelloWorldLogic {
            return if (isSFU)
                SFUHelloWorldLogic.getInstance(context)
            else
                MCUHelloWorldLogic.getInstance(context)
        }

        fun openConnection(isSFU: Boolean, localMedia: LocalMedia) {
            if(isSFU)
                SFUHelloWorldLogic.openSfuUpstreamConnection(localMedia)
            else
                //MCUHelloWorldLogic.openMcuConnection()
        }
    }
}