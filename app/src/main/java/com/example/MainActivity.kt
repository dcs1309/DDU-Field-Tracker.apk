package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.DduPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FieldIntelligenceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FieldIntelligenceViewModel = viewModel()
            val isNightMode by viewModel.isNightMode.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isNightMode) {
                DduApp(viewModel = viewModel)
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun DduApp(viewModel: FieldIntelligenceViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val stats by viewModel.statistics.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            title = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            route = Screen.Records.route,
            title = "Records",
            selectedIcon = Icons.Filled.Folder,
            unselectedIcon = Icons.Outlined.Folder,
            badgeCount = stats.pendingSyncCount
        ),
        BottomNavItem(
            route = Screen.SurveyWizard.route,
            title = "New Survey",
            selectedIcon = Icons.Filled.AddCircle,
            unselectedIcon = Icons.Outlined.AddCircle
        ),
        BottomNavItem(
            route = Screen.Map.route,
            title = "Field Map",
            selectedIcon = Icons.Filled.Map,
            unselectedIcon = Icons.Outlined.Map
        ),
        BottomNavItem(
            route = Screen.Analytics.route,
            title = "Analytics",
            selectedIcon = Icons.Filled.Analytics,
            unselectedIcon = Icons.Outlined.Analytics,
            badgeCount = stats.opportunitiesCount
        )
    )

    // Show bottom bar only on primary tabs when user is logged in
    val showBottomBar = isLoggedIn && currentRoute in listOf(
        Screen.Home.route,
        Screen.Records.route,
        Screen.SurveyWizard.route,
        Screen.Map.route,
        Screen.Analytics.route,
        Screen.Insights.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (item.badgeCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = if (item.route == Screen.Records.route) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            ) {
                                                Text("${item.badgeCount}")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.title
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("bottom_nav_${item.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToNewSurvey = { navController.navigate(Screen.SurveyWizard.route) },
                    onNavigateToRecordDetail = { dduId ->
                        navController.navigate(Screen.RecordDetail.createRoute(dduId))
                    },
                    onNavigateToRecords = { filter ->
                        viewModel.setStatusFilter(filter)
                        navController.navigate(Screen.Records.route)
                    },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) },
                    onNavigateToInsights = { navController.navigate(Screen.Analytics.route) },
                    onNavigateToQuickObservation = { navController.navigate(Screen.QuickObservation.route) },
                    onNavigateToOpportunityGraph = { navController.navigate(Screen.OpportunityGraph.route) },
                    onNavigateToPhotoCapture = { navController.navigate(Screen.PhotoCapture.createRoute()) },
                    onNavigateToSakhyaList = { navController.navigate(Screen.SakhyaScreeningList.route) },
                    onNavigateToNewSakhyaScreening = { screeningId ->
                        navController.navigate(Screen.SakhyaScreeningForm.createRoute(screeningId = screeningId))
                    },
                    onNavigateToStage2Assessment = { assessmentId, oppId ->
                        navController.navigate(Screen.Stage2AssessmentForm.createRoute(assessmentId, oppId))
                    },
                    onNavigateToStage2DduSelection = {
                        navController.navigate(Screen.Stage2DduSelection.route)
                    },
                    onNavigateToWebPortal = { navController.navigate(Screen.WebPortal.route) },
                    onSignOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Records.route) {
                RecordsListScreen(
                    viewModel = viewModel,
                    onNavigateToRecordDetail = { dduId ->
                        navController.navigate(Screen.RecordDetail.createRoute(dduId))
                    },
                    onNavigateToNewSurvey = { navController.navigate(Screen.SurveyWizard.route) }
                )
            }

            composable(Screen.SurveyWizard.route) {
                SurveyWizardScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSurveySubmitted = {
                        navController.navigate(Screen.Records.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateToRecordDetail = { dduId ->
                        navController.navigate(Screen.RecordDetail.createRoute(dduId))
                    }
                )
            }

            composable(Screen.Map.route) {
                FieldMapScreen(
                    viewModel = viewModel,
                    onNavigateToRecordDetail = { dduId ->
                        navController.navigate(Screen.RecordDetail.createRoute(dduId))
                    }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    viewModel = viewModel,
                    onNavigateToRecordDetail = { dduId ->
                        navController.navigate(Screen.RecordDetail.createRoute(dduId))
                    },
                    onNavigateToOpportunityGraph = { navController.navigate(Screen.OpportunityGraph.route) },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) },
                    onNavigateToWebPortal = { navController.navigate(Screen.WebPortal.route) }
                )
            }

            composable(Screen.WebPortal.route) {
                MasterWebPortalScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Insights.route) {
                InsightsScreen(
                    viewModel = viewModel,
                    onNavigateToRecordDetail = { dduId ->
                        navController.navigate(Screen.RecordDetail.createRoute(dduId))
                    },
                    onNavigateToOpportunityGraph = { navController.navigate(Screen.OpportunityGraph.route) },
                    onNavigateToStage2Assessment = { assessmentId, oppId ->
                        navController.navigate(Screen.Stage2AssessmentForm.createRoute(assessmentId, oppId))
                    },
                    onNavigateToStage2DduSelection = {
                        navController.navigate(Screen.Stage2DduSelection.route)
                    }
                )
            }

            composable(Screen.OpportunityGraph.route) {
                OpportunityNetworkGraphScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToOpportunity = { oppId ->
                        navController.navigate(Screen.OpportunityDetail.createRoute(oppId))
                    },
                    onNavigateToRecordDetail = { dduId ->
                        navController.navigate(Screen.RecordDetail.createRoute(dduId))
                    }
                )
            }

            composable(
                route = Screen.RecordDetail.route,
                arguments = listOf(navArgument("dduId") { type = NavType.StringType })
            ) { backStackEntry ->
                val dduId = backStackEntry.arguments?.getString("dduId") ?: ""
                RecordDetailScreen(
                    dduId = dduId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToOpportunity = { oppId ->
                        navController.navigate(Screen.OpportunityDetail.createRoute(oppId))
                    },
                    onNavigateToPhotoCapture = { targetDduId ->
                        navController.navigate(Screen.PhotoCapture.createRoute(dduId = targetDduId))
                    },
                    onNavigateToSakhyaScreening = { targetDduId ->
                        navController.navigate(Screen.SakhyaScreeningForm.createRoute(dduId = targetDduId))
                    }
                )
            }

            composable(
                route = Screen.OpportunityDetail.route,
                arguments = listOf(navArgument("oppId") { type = NavType.StringType })
            ) { backStackEntry ->
                val oppId = backStackEntry.arguments?.getString("oppId") ?: ""
                OpportunityDetailScreen(
                    oppId = oppId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRecord = { dduId ->
                        navController.navigate(Screen.RecordDetail.createRoute(dduId))
                    },
                    onNavigateToStage2Assessment = { targetOppId ->
                        navController.navigate(Screen.Stage2AssessmentForm.createRoute(oppId = targetOppId))
                    }
                )
            }

            composable(Screen.QuickObservation.route) {
                QuickObservationScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.EvidenceLibrary.route) {
                EvidenceLibraryScreen(
                    viewModel = viewModel,
                    onNavigateToRecord = { dduId ->
                        navController.navigate(Screen.RecordDetail.createRoute(dduId))
                    }
                )
            }

            composable(
                route = Screen.PhotoCapture.route,
                arguments = listOf(
                    navArgument("dduId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("oppId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val dduId = backStackEntry.arguments?.getString("dduId")
                val oppId = backStackEntry.arguments?.getString("oppId")
                FieldPhotoCaptureScreen(
                    viewModel = viewModel,
                    dduId = dduId,
                    oppId = oppId,
                    onNavigateBack = { navController.popBackStack() },
                    onPhotoCaptured = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.SakhyaScreeningList.route) {
                SakhyaScreeningListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNewScreening = { screeningId ->
                        navController.navigate(Screen.SakhyaScreeningForm.createRoute(screeningId = screeningId))
                    },
                    onNavigateToDetail = { screeningId ->
                        navController.navigate(Screen.SakhyaDetail.createRoute(screeningId))
                    }
                )
            }

            composable(
                route = Screen.SakhyaScreeningForm.route,
                arguments = listOf(
                    navArgument("screeningId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("dduId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val screeningId = backStackEntry.arguments?.getString("screeningId")
                val dduId = backStackEntry.arguments?.getString("dduId")
                SakhyaScreeningFormScreen(
                    viewModel = viewModel,
                    screeningId = screeningId,
                    dduId = dduId,
                    onNavigateBack = { navController.popBackStack() },
                    onSavedSuccessfully = { savedId ->
                        navController.navigate(Screen.SakhyaDetail.createRoute(savedId)) {
                            popUpTo(Screen.SakhyaScreeningList.route) {
                                inclusive = false
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.SakhyaDetail.route,
                arguments = listOf(navArgument("screeningId") { type = NavType.StringType })
            ) { backStackEntry ->
                val screeningId = backStackEntry.arguments?.getString("screeningId") ?: ""
                SakhyaDetailScreen(
                    viewModel = viewModel,
                    screeningId = screeningId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { editId ->
                        navController.navigate(Screen.SakhyaScreeningForm.createRoute(screeningId = editId))
                    }
                )
            }

            composable(
                route = Screen.Stage2AssessmentForm.route,
                arguments = listOf(
                    navArgument("assessmentId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("oppId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val assessmentId = backStackEntry.arguments?.getString("assessmentId")
                val oppId = backStackEntry.arguments?.getString("oppId")
                Stage2AssessmentFormScreen(
                    viewModel = viewModel,
                    assessmentId = assessmentId,
                    oppId = oppId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDduSelection = {
                        navController.navigate(Screen.Stage2DduSelection.route) {
                            popUpTo(Screen.Stage2AssessmentForm.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Stage2DduSelection.route) {
                Stage2DduSelectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNewAssessment = { targetOppId ->
                        navController.navigate(Screen.Stage2AssessmentForm.createRoute(oppId = targetOppId))
                    },
                    onNavigateToEditAssessment = { editAssessmentId ->
                        navController.navigate(Screen.Stage2AssessmentForm.createRoute(assessmentId = editAssessmentId))
                    }
                )
            }
        }
    }
}
