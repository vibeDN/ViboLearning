package com.vibo.vibolearning

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibo.vibolearning.ui.VliNavHost
import com.vibo.vibolearning.ui.theme.VliTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as VliApp).container
        val incoming: Uri? = intent?.takeIf { it.action == android.content.Intent.ACTION_VIEW }?.data

        setContent {
            val repo = container.repository
            val activeId by repo.observeActiveCourseId().collectAsStateWithLifecycle(initialValue = null)
            val summaries by repo.observeCourseSummaries().collectAsStateWithLifecycle(initialValue = emptyList())
            val accent = summaries.firstOrNull { it.id == activeId }?.accent

            val scope = rememberCoroutineScope()
            var handledIntent by remember { mutableStateOf(false) }
            LaunchedEffect(incoming) {
                if (incoming != null && !handledIntent) {
                    handledIntent = true
                    scope.launch { runCatching { repo.importFromFile(incoming) } }
                }
            }

            VliTheme(accentHex = accent) {
                Surface(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    VliNavHost(repo, container.codeRunner, activeId, summaries)
                }
            }
        }
    }
}
