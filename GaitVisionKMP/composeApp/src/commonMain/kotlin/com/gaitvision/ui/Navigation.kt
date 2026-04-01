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

object Screen {
    const val Dashboard = "dashboard"
    const val Camera = "camera"
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
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard
    ) {
        composable(Screen.Dashboard) {
            DashboardScreen(
                onNavigateToCamera = { navController.navigate(Screen.Camera) },
                onNavigateToAnalysis = { navController.navigate(Screen.Analysis) },
                onNavigateToPatientList = { navController.navigate(Screen.PatientList) },
                onNavigateToSettings = { navController.navigate(Screen.Settings) },
                onNavigateToPatientProfile = { patientId -> 
                    navController.navigate(Screen.createPatientProfileRoute(patientId))
                },
                database = database,
                videoProcessor = videoProcessor
            )
        }
        
        composable(Screen.Camera) {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAnalysis = { navController.navigate(Screen.Analysis) },
                poseDetector = poseDetector,
                videoProcessor = videoProcessor
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
                onPatientCreated = { navController.popBackStack() }
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
                }
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
