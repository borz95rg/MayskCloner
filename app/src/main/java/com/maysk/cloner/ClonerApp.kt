package com.maysk.cloner

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.maysk.cloner.data.SettingsStore
import com.maysk.cloner.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Контейнер зависимостей приложения. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings = SettingsStore(context)
    val updates = UpdateManager(context, settings, scope)
    private val _backgroundGeneration = MutableStateFlow(0L)
    val backgroundGeneration = _backgroundGeneration.asStateFlow()

    fun onProcessBackgrounded() { _backgroundGeneration.value += 1L }
}

class ClonerApp : Application(), DefaultLifecycleObserver {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) { container.onProcessBackgrounded() }
}
