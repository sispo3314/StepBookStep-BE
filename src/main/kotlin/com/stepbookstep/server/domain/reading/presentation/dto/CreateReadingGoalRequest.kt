package com.stepbookstep.server.domain.reading.presentation.dto

import com.stepbookstep.server.domain.reading.domain.GoalMetric
import com.stepbookstep.server.domain.reading.domain.GoalPeriod

data class CreateReadingGoalRequest(
    val period: GoalPeriod,
    val metric: GoalMetric,
    val targetAmount: Int
)
