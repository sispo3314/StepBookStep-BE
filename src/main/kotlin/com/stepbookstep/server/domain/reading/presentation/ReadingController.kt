package com.stepbookstep.server.domain.reading.presentation

import com.stepbookstep.server.domain.book.domain.BookRepository
import com.stepbookstep.server.domain.reading.application.ReadingGoalService
import com.stepbookstep.server.domain.reading.application.ReadingLogService
import com.stepbookstep.server.domain.reading.presentation.dto.ActiveReadingGoalResponse
import com.stepbookstep.server.domain.reading.presentation.dto.CreateReadingGoalRequest
import com.stepbookstep.server.domain.reading.presentation.dto.CreateReadingLogRequest
import com.stepbookstep.server.domain.reading.presentation.dto.CreateReadingLogResponse
import com.stepbookstep.server.domain.reading.presentation.dto.ReadingGoalResponse
import com.stepbookstep.server.global.response.ApiResponse
import com.stepbookstep.server.global.response.CustomException
import com.stepbookstep.server.global.response.ErrorCode
import com.stepbookstep.server.security.jwt.AuthenticatedUserResolver
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Reading", description = "독서 목표/기록 API")
@RestController
@RequestMapping("/api/v1")
class ReadingController(
    private val readingGoalService: ReadingGoalService,
    private val readingLogService: ReadingLogService,
    private val bookRepository: BookRepository,
    private val authenticatedUserResolver: AuthenticatedUserResolver
) {

    @Operation(summary = "독서 목표 생성", description = "특정 책에 대한 독서 목표를 생성합니다.")
    @PostMapping("/books/{bookId}/goals")
    fun createGoal(
        @Parameter(description = "도서 ID") @PathVariable bookId: Long,
        @RequestHeader("Authorization", required = false) authorization: String?,
        @RequestBody request: CreateReadingGoalRequest
    ): ResponseEntity<ApiResponse<ReadingGoalResponse>> {
        val userId = authenticatedUserResolver.getUserId(authorization)
        val goal = readingGoalService.createGoal(
            userId = userId,
            bookId = bookId,
            period = request.period,
            metric = request.metric,
            targetAmount = request.targetAmount
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(ReadingGoalResponse.from(goal)))
    }

    @Operation(summary = "책 목표 조회", description = "특정 책의 현재 활성화된 독서 목표를 조회합니다.")
    @GetMapping("/books/{bookId}/goals")
    fun getGoal(
        @Parameter(description = "도서 ID") @PathVariable bookId: Long,
        @RequestHeader("Authorization", required = false) authorization: String?
    ): ResponseEntity<ApiResponse<ReadingGoalResponse?>> {
        val userId = authenticatedUserResolver.getUserId(authorization)
        val goal = readingGoalService.getActiveGoal(userId, bookId)
        val response = goal?.let { ReadingGoalResponse.from(it) }
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @Operation(summary = "활성 목표 목록", description = "사용자의 활성화된 독서 목표를 조회합니다.")
    @GetMapping("/goals/active")
    fun getActiveGoals(
        @RequestHeader("Authorization", required = false) authorization: String?
    ): ResponseEntity<ApiResponse<List<ActiveReadingGoalResponse>>> {
        val userId = authenticatedUserResolver.getUserId(authorization)
        val goals = readingGoalService.getActiveGoals(userId)
        val bookIds = goals.map { it.bookId }.distinct()
        val books = bookRepository.findAllById(bookIds).associateBy { it.id }

        val responses = goals.map { goal ->
            val book = books[goal.bookId] ?: throw CustomException(ErrorCode.BOOK_NOT_FOUND)
            ActiveReadingGoalResponse(
                goalId = goal.id,
                bookId = goal.bookId,
                bookTitle = book.title,
                bookAuthor = book.author,
                period = goal.period,
                metric = goal.metric,
                targetAmount = goal.targetAmount,
                currentProgress = 0,
                achievementRate = 0.0
            )
        }
        return ResponseEntity.ok(ApiResponse.ok(responses))
    }

    @Operation(summary = "독서 기록 생성", description = "독서 기록을 생성합니다.")
    @PostMapping("/books/{bookId}/reading-logs")
    fun createReadingLog(
        @Parameter(description = "도서 ID") @PathVariable bookId: Long,
        @RequestHeader("Authorization", required = false) authorization: String?,
        @RequestBody request: CreateReadingLogRequest
    ): ResponseEntity<ApiResponse<CreateReadingLogResponse>> {
        val userId = authenticatedUserResolver.getUserId(authorization)
        val log = readingLogService.createLog(
            userId = userId,
            bookId = bookId,
            bookStatus = request.bookStatus,
            recordDate = request.recordDate,
            readQuantity = request.readQuantity,
            durationSeconds = request.durationSeconds,
            difficulty = request.difficulty
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(CreateReadingLogResponse(log.id)))
    }
}
