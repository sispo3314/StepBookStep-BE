package com.stepbookstep.server.domain.reading.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ReadingLogRepository : JpaRepository<ReadingLog, Long>
