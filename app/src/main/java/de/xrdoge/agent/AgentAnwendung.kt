package de.xrdoge.agent

import android.app.Application
import de.xrdoge.agent.laufzeit.AgentRuntime
import java.io.File

class AgentAnwendung : Application() {
    val runtime by lazy { AgentRuntime(File(filesDir, "sandbox")) }

    override fun onCreate() {
        super.onCreate()
    }
}
