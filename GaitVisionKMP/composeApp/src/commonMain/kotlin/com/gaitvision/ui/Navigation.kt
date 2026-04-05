package com.gaitvision.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gaitvision.data.AppDatabase
import com.gaitvision.platform.PoseDetector
import com.gaitvision.platform.VideoProcessor
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

object Screen {
    const val Dashboard = "dashboard"
    const val AiDisclosure = "ai_disclosure/{patientId}"
    const val Camera = "camera/{patientId}"
    const val Analysis = "analysis"
    const val PatientList = "patient_list"
    const val PatientCreate = "patient_create"
    const val PatientProfile = "patient_profile/{patientId}"
    const val Results = "results/{scoreId}"
    const val SignalsDashboard = "signals_dashboard/{scoreId}"
    const val Settings = "settings"
    const val Help = "help"
    const val Info = "info"
    const val Csv = "csv"

    fun createAiDisclosureRoute(patientId: Long) = "ai_disclosure/$patientId"
    fun createCameraRoute(patientId: Long) = "camera/$patientId"
    fun createPatientProfileRoute(patientId: Long) = "patient_profile/$patientId"
    fun createResultsRoute(scoreId: Long) = "results/$scoreId"
    fun createSignalsDashboardRoute(scoreId: Long) = "signals_dashboard/$scoreId"
}

@Composable
fun AppNavigation(
    poseDetector: PoseDetector,
    videoProcessor: VideoProcessor,
    database: AppDatabase,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()

    val navigateToCameraOrConsent: (Long) -> Unit = { patientId ->
        scope.launch {
            val consent = database.aiConsentDao().getConsentForPatient(patientId)
            if (consent != null && consent.consentGiven) {
                navController.navigate(Screen.createCameraRoute(patientId))
            } else {
                navController.navigate(Screen.createAiDisclosureRoute(patientId))
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard
    ) {
        composable(Screen.Dashboard) {
            DashboardScreen(
                onNavigateToCamera = { patientId -> navigateToCameraOrConsent(patientId) },
                onNavigateToAnalysis = { navController.navigate(Screen.Analysis) },
                onNavigateToPatientList = { navController.navigate(Screen.PatientList) },
                onNavigateToSettings = { navController.navigate(Screen.Settings) },
                onNavigateToPatientProfile = { patientId ->
                    navController.navigate(Screen.createPatientProfileRoute(patientId))
                },
                onNavigateToResults = { scoreId ->
                    navController.navigate(Screen.createResultsRoute(scoreId))
                },
                database = database,
                videoProcessor = videoProcessor,
                onNavigateToHelp = { navController.navigate(Screen.Help) },
                onNavigateToInfo = { navController.navigate(Screen.Info) },
                onNavigateToCreatePatient = { navController.navigate(Screen.PatientCreate) }
            )
        }

        composable(
            route = Screen.AiDisclosure,
            arguments = listOf(navArgument("patientId") { type = NavType.LongType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getLong("patientId") ?: 0L
            AiDisclosureScreen(
                patientId = patientId,
                database = database,
                onConsentGranted = { 
                    navController.popBackStack()
                    navController.navigate(Screen.createCameraRoute(patientId))
                },
                onDecline = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Camera,
            arguments = listOf(navArgument("patientId") { type = NavType.LongType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getLong("patientId") ?: 0L
            CameraScreen(
                patientId = patientId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResults = { scoreId ->
                    navController.navigate(Screen.createResultsRoute(scoreId))
                },
                poseDetector = poseDetector,
                videoProcessor = videoProcessor,
                database = database
            )
        }

        composable(Screen.Analysis) {
            AnalysisScreen(
                onNavigateBack = { navController.popBackStack() },
                database = database
            )
        }

        composable(Screen.PatientList) {
            PatientListScreen(
                database = database,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreatePatient = { navController.navigate(Screen.PatientCreate) },
                onNavigateToPatientProfile = { patientId ->
                    navController.navigate(Screen.createPatientProfileRoute(patientId))
                }
            )
        }

        composable(Screen.PatientCreate) {
            PatientCreateScreen(
                database = database,
                onNavigateBack = { navController.popBackStack() },
                onPatientCreated = { navController.popBackStack() },
                onNavigateToCamera = { patientId -> navigateToCameraOrConsent(patientId) }
            )
        }

        composable(
            route = Screen.PatientProfile,
            arguments = listOf(navArgument("patientId") { type = NavType.LongType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getLong("patientId") ?: 0L
            PatientProfileScreen(
                patientId = patientId,
                database = database,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResults = { scoreId ->
                    navController.navigate(Screen.createResultsRoute(scoreId))
                },
                onNavigateToCamera = { navigateToCameraOrConsent(patientId) }
            )
        }

        composable(
            route = Screen.Results,
            arguments = listOf(navArgument("scoreId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scoreId = backStackEntry.arguments?.getLong("scoreId") ?: 0L
            ResultsScreen(
                scoreId = scoreId,
                database = database,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSignals = { sId ->
                    navController.navigate(Screen.createSignalsDashboardRoute(sId))
                }
            )
        }

        composable(
            route = Screen.SignalsDashboard,
            arguments = listOf(navArgument("scoreId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scoreId = backStackEntry.arguments?.getLong("scoreId") ?: 0L
            SignalsDashboardScreen(
                scoreId = scoreId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHelp = { navController.navigate(Screen.Help) },
                onNavigateToInfo = { navController.navigate(Screen.Info) },
                onNavigateToCsv = { navController.navigate(Screen.Csv) }
            )
        }

        composable(Screen.Help) {
            HelpScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Info) {
            InfoScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Csv) {
            CsvScreen(
                database = database,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
