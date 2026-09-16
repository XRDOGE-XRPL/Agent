package de.xrdoge.agent

import android.app.Application
import de.xrdoge.agent.laufzeit.AgentRuntime

class AgentAnwendung : Application() {
    val runtime by lazy { AgentRuntime() }

    override fun onCreate() {
        super.onCreate()
    }
}
