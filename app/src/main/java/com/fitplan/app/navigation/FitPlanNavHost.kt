package com.fitplan.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitplan.app.ui.common.FitPlanScaffold
import com.fitplan.app.ui.common.rememberAppContainer
import com.fitplan.app.ui.ai.AiPlanScreen
import com.fitplan.app.ui.ai.AiPlanViewModel
import com.fitplan.app.ui.editPlan.EditPlanScreen
import com.fitplan.app.ui.editPlan.EditPlanViewModel
import com.fitplan.app.ui.history.HistoryScreen
import com.fitplan.app.ui.history.HistoryViewModel
import com.fitplan.app.ui.history.RecordDetailScreen
import com.fitplan.app.ui.history.RecordDetailViewModel
import com.fitplan.app.ui.home.HomeScreen
import com.fitplan.app.ui.home.HomeViewModel
import com.fitplan.app.ui.planDetail.PlanDetailScreen
import com.fitplan.app.ui.planDetail.PlanDetailViewModel
import com.fitplan.app.ui.plans.PlansScreen
import com.fitplan.app.ui.plans.PlansViewModel
import com.fitplan.app.ui.settings.SettingsScreen
import com.fitplan.app.ui.settings.SettingsViewModel
import com.fitplan.app.ui.workout.WorkoutSessionScreen
import com.fitplan.app.ui.workout.WorkoutSessionViewModel

@Composable
fun FitPlanNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val appContainer = rememberAppContainer()
    val backStackEntry = navController.currentBackStackEntryAsState()

    FitPlanScaffold(
        currentDestination = backStackEntry.value?.destination,
        onNavigateToTopLevel = { destination ->
            navController.navigate(destination.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(FitPlanRoutes.HOME) {
                    saveState = true
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = FitPlanRoutes.HOME,
            modifier = modifier.padding(paddingValues)
        ) {
            composable(FitPlanRoutes.HOME) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(
                        appContainer.workoutPlanRepository,
                        appContainer.workoutRecordRepository,
                        appContainer.settingsRepository
                    )
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onStartWorkout = { planId, dayId -> navController.navigate(FitPlanRoutes.workout(planId, dayId)) },
                    onNewPlan = { navController.navigate(FitPlanRoutes.newPlan()) },
                    onAiPlan = { navController.navigate(FitPlanRoutes.AI) },
                    onHistory = { navController.navigate(FitPlanRoutes.HISTORY) }
                )
            }
            composable(FitPlanRoutes.PLANS) {
                val plansViewModel: PlansViewModel = viewModel(
                    factory = PlansViewModel.factory(appContainer.workoutPlanRepository)
                )
                PlansScreen(
                    viewModel = plansViewModel,
                    onNewPlan = { navController.navigate(FitPlanRoutes.newPlan()) },
                    onPlanClick = { navController.navigate(FitPlanRoutes.planDetail(it)) },
                    onEditPlan = { navController.navigate(FitPlanRoutes.editPlan(it)) }
                )
            }
            composable(FitPlanRoutes.HISTORY) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.factory(appContainer.workoutRecordRepository)
                )
                HistoryScreen(
                    viewModel = historyViewModel,
                    onRecordClick = { navController.navigate(FitPlanRoutes.recordDetail(it)) }
                )
            }
            composable(FitPlanRoutes.AI) {
                val aiPlanViewModel: AiPlanViewModel = viewModel(
                    factory = AiPlanViewModel.factory(
                        appContainer.aiPlanRepository,
                        appContainer.workoutPlanRepository
                    )
                )
                AiPlanScreen(
                    viewModel = aiPlanViewModel,
                    onSaved = { navController.navigate(FitPlanRoutes.planDetail(it)) }
                )
            }
            composable(FitPlanRoutes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(appContainer.settingsRepository)
                )
                SettingsScreen(viewModel = settingsViewModel)
            }
            composable(
                route = FitPlanRoutes.PLAN_DETAIL,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: 0L
                val detailViewModel: PlanDetailViewModel = viewModel(
                    factory = PlanDetailViewModel.factory(planId, appContainer.workoutPlanRepository)
                )
                PlanDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(FitPlanRoutes.editPlan(it)) },
                    onStartWorkout = { selectedPlanId, dayId -> navController.navigate(FitPlanRoutes.workout(selectedPlanId, dayId)) },
                    onDuplicated = { navController.navigate(FitPlanRoutes.planDetail(it)) }
                )
            }
            composable(
                route = FitPlanRoutes.EDIT_PLAN,
                arguments = listOf(navArgument("planId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { entry ->
                val rawPlanId = entry.arguments?.getLong("planId") ?: -1L
                val planId = rawPlanId.takeIf { it > 0 }
                val editViewModel: EditPlanViewModel = viewModel(
                    factory = EditPlanViewModel.factory(planId, appContainer.workoutPlanRepository)
                )
                EditPlanScreen(
                    viewModel = editViewModel,
                    onSaved = { navController.navigate(FitPlanRoutes.planDetail(it)) }
                )
            }
            composable(
                route = FitPlanRoutes.WORKOUT,
                arguments = listOf(
                    navArgument("planId") { type = NavType.LongType },
                    navArgument("dayId") { type = NavType.LongType }
                )
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: 0L
                val dayId = entry.arguments?.getLong("dayId") ?: 0L
                val workoutViewModel: WorkoutSessionViewModel = viewModel(
                    factory = WorkoutSessionViewModel.factory(
                        planId,
                        dayId,
                        appContainer.workoutPlanRepository,
                        appContainer.workoutRecordRepository
                    )
                )
                WorkoutSessionScreen(
                    viewModel = workoutViewModel,
                    onFinished = { navController.navigate(FitPlanRoutes.recordDetail(it)) }
                )
            }
            composable(
                route = FitPlanRoutes.RECORD_DETAIL,
                arguments = listOf(navArgument("recordId") { type = NavType.LongType })
            ) { entry ->
                val recordId = entry.arguments?.getLong("recordId") ?: 0L
                val recordDetailViewModel: RecordDetailViewModel = viewModel(
                    factory = RecordDetailViewModel.factory(recordId, appContainer.workoutRecordRepository)
                )
                RecordDetailScreen(
                    viewModel = recordDetailViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
