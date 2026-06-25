package com.fitplan.app.navigation

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val iconLabel: String
) {
    Home("home", "首页", "⌂"),
    Plans("plans", "计划", "▤"),
    History("history", "记录", "◷"),
    Ai("ai", "AI", "AI"),
    Settings("settings", "设置", "⚙")
}

object FitPlanRoutes {
    const val HOME = "home"
    const val PLANS = "plans"
    const val HISTORY = "history"
    const val AI = "ai"
    const val SETTINGS = "settings"
    const val PLAN_DETAIL = "planDetail/{planId}"
    const val EDIT_PLAN = "editPlan?planId={planId}"
    const val WORKOUT = "workout/{planId}/{dayId}"
    const val RECORD_DETAIL = "recordDetail/{recordId}"

    fun planDetail(planId: Long) = "planDetail/$planId"
    fun newPlan() = "editPlan"
    fun editPlan(planId: Long) = "editPlan?planId=$planId"
    fun workout(planId: Long, dayId: Long) = "workout/$planId/$dayId"
    fun recordDetail(recordId: Long) = "recordDetail/$recordId"
}
