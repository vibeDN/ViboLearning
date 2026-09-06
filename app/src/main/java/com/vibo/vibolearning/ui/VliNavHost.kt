package com.vibo.vibolearning.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vibo.vibolearning.data.CourseRepository
import com.vibo.vibolearning.data.CourseSummary
import com.vibo.vibolearning.data.run.CodeRunner
import com.vibo.vibolearning.ui.home.HomeRoute
import com.vibo.vibolearning.ui.session.PlayRoute
import com.vibo.vibolearning.ui.session.SessionMode

@Composable
fun VliNavHost(
    repo: CourseRepository,
    runner: CodeRunner,
    activeCourseId: String?,
    summaries: List<CourseSummary>,
) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeRoute(
                repo = repo,
                activeCourseId = activeCourseId,
                summaries = summaries,
                onOpenLesson = { c, l -> nav.navigate("play/$c/lesson/$l") },
                onExam = { c, m -> nav.navigate("play/$c/exam/$m") },
                onPractice = { c, ref -> nav.navigate("play/$c/practice/$ref") },
            )
        }
        composable(
            route = "play/{courseId}/{mode}/{ref}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType },
                navArgument("ref") { type = NavType.StringType },
            ),
        ) { entry ->
            val args = entry.arguments!!
            val mode = when (args.getString("mode")) {
                "exam" -> SessionMode.Exam
                "practice" -> SessionMode.Practice
                else -> SessionMode.Lesson
            }
            PlayRoute(
                repo = repo,
                runner = runner,
                courseId = args.getString("courseId")!!,
                mode = mode,
                ref = args.getString("ref")!!,
                onExit = { nav.popBackStack(route = "home", inclusive = false) },
            )
        }
    }
}
