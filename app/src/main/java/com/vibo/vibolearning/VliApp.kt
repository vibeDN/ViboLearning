package com.vibo.vibolearning

import android.app.Application
import com.vibo.vibolearning.data.CourseRepository
import com.vibo.vibolearning.data.local.AppDatabase
import com.vibo.vibolearning.data.remote.CourseDownloader
import com.vibo.vibolearning.data.run.CodeRunner
import com.vibo.vibolearning.data.run.WandboxRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual dependency container — small enough that Hilt would be overkill. */
class AppContainer(app: Application) {
    private val database = AppDatabase.build(app)
    val repository = CourseRepository(app, database, CourseDownloader())
    val codeRunner: CodeRunner = WandboxRunner()
}

class VliApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.repository.syncBuiltins()
        }
    }
}
