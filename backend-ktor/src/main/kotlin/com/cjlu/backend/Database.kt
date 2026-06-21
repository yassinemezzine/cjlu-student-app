package com.cjlu.backend

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object Database {
    private const val STUDENT_API_CONFIG_NAME = "student_api_secret"
    const val ACADEMIC_CALENDAR_CONFIG_NAME = "academic_calendar_json"

    val json = Json { ignoreUnknownKeys = true }

    private object AppConfig : Table("app_config") {
        val name = varchar("config_name", 64)
        val value = text("config_value")

        override val primaryKey = PrimaryKey(name)
    }

    private object StudentRequests : Table("student_requests") {
        val id = varchar("id", 50)
        val serviceId = varchar("service_id", 50)
        val studentId = varchar("student_id", 20)
        val contactInfo = text("contact_info")
        val notes = text("notes")
        val status = varchar("status", 20)
        val createdAtMillis = long("created_at_millis")
        val attachmentUrl = varchar("attachment_url", 255).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    private object Students : Table("students") {
        val studentId = varchar("student_id", 20)
        val displayName = varchar("display_name", 200)
        val classSection = varchar("class_section", 80)
        val passwordHash = text("password_hash")
        val major = varchar("major", 200)
        val school = varchar("school", 200)
        val overallAttendancePercent = integer("overall_attendance_percent").default(96)
        val classUpdateNotice = text("class_update_notice").default("")
        val classUpdateAtMillis = long("class_update_at_millis").default(0L)

        override val primaryKey = PrimaryKey(studentId)
    }

    private object CatalogServices : Table("catalog_services") {
        val id = varchar("id", 50)
        val category = varchar("category", 32)
        val titleEn = text("title_en")
        val titleZh = text("title_zh")
        val descEn = text("desc_en")
        val descZh = text("desc_zh")
        val turnaroundEn = varchar("turnaround_en", 120)
        val turnaroundZh = varchar("turnaround_zh", 120)
        val checklistEn = text("checklist_en")
        val checklistZh = text("checklist_zh")
        val isPopular = bool("is_popular")

        override val primaryKey = PrimaryKey(id)
    }

    private object InboxMessages : Table("inbox_messages") {
        val id = varchar("id", 64)
        /** When null, the row is visible to every student (broadcast). Otherwise only that student. */
        val recipientStudentId = varchar("recipient_student_id", 32).nullable()
        val category = varchar("category", 32)
        val senderEn = text("sender_en")
        val senderZh = text("sender_zh")
        val titleEn = text("title_en")
        val titleZh = text("title_zh")
        val bodyEn = text("body_en")
        val bodyZh = text("body_zh")
        val timeLabelEn = varchar("time_label_en", 80)
        val timeLabelZh = varchar("time_label_zh", 80)
        val relatedServiceId = varchar("related_service_id", 50).nullable()
        val requiresAction = bool("requires_action")
        val startsUnread = bool("starts_unread")
        val sentAtMillis = long("sent_at_millis")

        override val primaryKey = PrimaryKey(id)
    }

    private object StudentMessageReads : Table("student_message_reads") {
        val studentId = varchar("student_id", 20)
        val messageId = varchar("message_id", 64)
        val readAtMillis = long("read_at_millis")

        override val primaryKey = PrimaryKey(studentId, messageId)
    }

    private object StudentFcmTokens : Table("student_fcm_tokens") {
        val studentId = varchar("student_id", 20)
        val token = varchar("token", 512)
        val updatedAtMillis = long("updated_at_millis")

        override val primaryKey = PrimaryKey(token)
    }

    private fun getDbUrl(): String {
        val envUrl = System.getenv("DB_URL")?.trim()?.takeIf { it.isNotEmpty() }
            ?: System.getenv("CJLU_DB_URL")?.trim()?.takeIf { it.isNotEmpty() }

        if (envUrl != null) {
            if (envUrl.startsWith("postgresql://")) {
                val cleanUrl = envUrl.substring("postgresql://".length)
                if (cleanUrl.contains("@")) {
                    val parts = cleanUrl.split("@", limit = 2)
                    val hostDbAndParams = parts[1]
                    return "jdbc:postgresql://$hostDbAndParams"
                }
                return "jdbc:$envUrl"
            }
            return envUrl
        }
        if (System.getProperty("cjlu.test.db") == "mem") {
            return "jdbc:h2:mem:cjlu_test;DB_CLOSE_DELAY=-1;"
        }
        return "jdbc:h2:file:./requests_sql_db;DB_CLOSE_DELAY=-1;"
    }

    private fun getDbDriver(): String {
        val envDriver = System.getenv("DB_DRIVER")?.trim()?.takeIf { it.isNotEmpty() }
        if (envDriver != null) return envDriver
        val url = getDbUrl()
        if (url.startsWith("jdbc:postgresql:") || url.startsWith("postgresql:")) {
            return "org.postgresql.Driver"
        }
        return "org.h2.Driver"
    }

    private fun getDbUser(): String? {
        val envUser = System.getenv("DB_USER")?.trim()?.takeIf { it.isNotEmpty() }
        if (envUser != null) return envUser

        val envUrl = System.getenv("DB_URL")?.trim()?.takeIf { it.isNotEmpty() }
            ?: System.getenv("CJLU_DB_URL")?.trim()?.takeIf { it.isNotEmpty() }

        if (envUrl != null && envUrl.startsWith("postgresql://")) {
            val cleanUrl = envUrl.substring("postgresql://".length)
            if (cleanUrl.contains("@")) {
                val credentials = cleanUrl.split("@", limit = 2)[0]
                if (credentials.contains(":")) {
                    return credentials.split(":", limit = 2)[0]
                }
                return credentials
            }
        }

        val url = getDbUrl()
        if (url.startsWith("jdbc:h2:")) {
            return "sa"
        }
        return null
    }

    private fun getDbPassword(): String? {
        val envPassword = System.getenv("DB_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() }
        if (envPassword != null) return envPassword

        val envUrl = System.getenv("DB_URL")?.trim()?.takeIf { it.isNotEmpty() }
            ?: System.getenv("CJLU_DB_URL")?.trim()?.takeIf { it.isNotEmpty() }

        if (envUrl != null && envUrl.startsWith("postgresql://")) {
            val cleanUrl = envUrl.substring("postgresql://".length)
            if (cleanUrl.contains("@")) {
                val credentials = cleanUrl.split("@", limit = 2)[0]
                if (credentials.contains(":")) {
                    return credentials.split(":", limit = 2)[1]
                }
            }
        }
        return null
    }

    init {
        val dbUrl = getDbUrl()
        val host = when {
            dbUrl.contains("@") -> dbUrl.substringAfter("@").substringBefore("/").substringBefore(":")
            dbUrl.startsWith("jdbc:postgresql://") -> dbUrl.substringAfter("jdbc:postgresql://").substringBefore("/").substringBefore(":")
            else -> dbUrl
        }
        println("[Database] Connecting to database host: $host")

        val user = getDbUser()
        val password = getDbPassword()
        if (user != null && password != null) {
            org.jetbrains.exposed.sql.Database.connect(
                url = dbUrl,
                driver = getDbDriver(),
                user = user,
                password = password
            )
        } else {
            org.jetbrains.exposed.sql.Database.connect(
                url = dbUrl,
                driver = getDbDriver()
            )
        }

        transaction {
            SchemaUtils.create(
                StudentRequests,
                AppConfig,
                Students,
                CatalogServices,
                InboxMessages,
                StudentMessageReads,
                StudentFcmTokens,
            )
            // Exposed schema creation is sufficient here; avoid relying on the optional migration module.
            ensureStudentApiSecretRow()

            val seedDbEnv = System.getenv("SEED_DB")?.trim()?.lowercase() == "true"
            val isDbEmpty = Students.selectAll().empty()

            if (isDbEmpty || seedDbEnv) {
                ensureRosterStudentsSeeded()
                seedCatalogIfEmpty()
                seedMessagesIfEmpty()
                if (StudentRequests.selectAll().empty()) {
                    seedData()
                }
                ensureAcademicCalendarSeeded()
            }
            AcademicRepository.ensureTables()
        }
    }

    private fun ensureStudentApiSecretRow() {
        val exists = !AppConfig.selectAll().where { AppConfig.name eq STUDENT_API_CONFIG_NAME }.empty()
        if (exists) return
        val fromEnv = System.getenv("STUDENT_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
        val secret =
            when {
                fromEnv != null -> fromEnv
                DevDefaults.allowInsecure -> {
                    System.err.println(
                        "CJLU_ALLOW_INSECURE_DEV_DEFAULTS=true: initializing student API key from insecure local fallback. " +
                            "Set STUDENT_API_KEY and unset this flag before any shared or production deployment.",
                    )
                    INSECURE_FALLBACK_STUDENT_API_KEY
                }
                else -> error(
                    "STUDENT_API_KEY must be set in the environment before the database is first created, " +
                        "or set CJLU_ALLOW_INSECURE_DEV_DEFAULTS=true only on a private development machine.",
                )
            }
        AppConfig.insert {
            it[name] = STUDENT_API_CONFIG_NAME
            it[value] = secret
        }
    }

    @kotlinx.serialization.Serializable
    data class AcademicCalendarData(
        val academicYearLabel: String,
        val semesterLabel: String,
        val months: List<AcademicDataSeed.AcademicCalendarMonthSeed>,
        val events: List<AcademicDataSeed.AcademicCalendarEventSeed>
    )

    private fun ensureAcademicCalendarSeeded() {
        val exists = !AppConfig.selectAll().where { AppConfig.name eq ACADEMIC_CALENDAR_CONFIG_NAME }.empty()
        if (exists) return
        val defaultData = AcademicCalendarData(
            academicYearLabel = AcademicDataSeed.CURRENT_ACADEMIC_YEAR,
            semesterLabel = AcademicDataSeed.CURRENT_SEMESTER,
            months = AcademicDataSeed.academicCalendarMonths,
            events = AcademicDataSeed.academicCalendarEvents
        )
        AppConfig.insert {
            it[name] = ACADEMIC_CALENDAR_CONFIG_NAME
            it[value] = json.encodeToString(AcademicCalendarData.serializer(), defaultData)
        }
    }

    /**
     * Inserts any roster student that is not yet in the database (default password = student ID).
     * Runs on every startup so new [RosterSeed] entries become accounts without wiping the DB.
     */
    private fun ensureRosterStudentsSeeded() {
        val hasher = BCrypt.withDefaults()
        val existingIds = Students.selectAll().map { it[Students.studentId] }.toSet()
        var inserted = 0
        for ((id, name, section) in RosterSeed.students) {
            val sid = id.trim()
            if (sid in existingIds) continue
            val initialPassword = sid
            val hash = hasher.hashToString(12, initialPassword.toCharArray())
            Students.insert {
                it[studentId] = sid
                it[displayName] = name
                it[classSection] = section
                it[passwordHash] = hash
                it[major] = "Computer Science"
                it[school] = "School of International Students"
                it[overallAttendancePercent] = 96
                it[classUpdateNotice] = ""
                it[classUpdateAtMillis] = 0L
            }
            inserted++
        }
        if (inserted > 0) {
            println(
                "Seeded $inserted new roster student(s) (default password = student ID). " +
                    "Roster size is ${RosterSeed.students.size}.",
            )
        }
    }

    private fun encodeChecklist(list: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), list)

    private fun seedCatalogIfEmpty() {
        if (!CatalogServices.selectAll().empty()) return
        for (row in ContentSeed.catalogRows) {
            CatalogServices.insert {
                it[id] = row.id
                it[category] = row.category
                it[titleEn] = row.titleEn
                it[titleZh] = row.titleZh
                it[descEn] = row.descEn
                it[descZh] = row.descZh
                it[turnaroundEn] = row.turnaroundEn
                it[turnaroundZh] = row.turnaroundZh
                it[checklistEn] = encodeChecklist(row.checklistEn)
                it[checklistZh] = encodeChecklist(row.checklistZh)
                it[isPopular] = row.isPopular
            }
        }
    }

    private fun seedMessagesIfEmpty() {
        if (!InboxMessages.selectAll().empty()) return
        for (row in ContentSeed.messageRows) {
            InboxMessages.insert {
                it[id] = row.id
                it[recipientStudentId] = null
                it[category] = row.category
                it[senderEn] = row.senderEn
                it[senderZh] = row.senderZh
                it[titleEn] = row.titleEn
                it[titleZh] = row.titleZh
                it[bodyEn] = row.bodyEn
                it[bodyZh] = row.bodyZh
                it[timeLabelEn] = row.timeEn
                it[timeLabelZh] = row.timeZh
                it[relatedServiceId] = row.relatedServiceId
                it[requiresAction] = row.requiresAction
                it[startsUnread] = row.startsUnread
                it[sentAtMillis] = row.sentAtMillis
            }
        }
    }

    fun getStudentApiKey(): String = transaction {
        AppConfig.selectAll()
            .where { AppConfig.name eq STUDENT_API_CONFIG_NAME }
            .map { it[AppConfig.value] }
            .singleOrNull()
            ?: error("Missing app_config row for $STUDENT_API_CONFIG_NAME")
    }

    fun getAcademicCalendarData(): AcademicCalendarData = transaction {
        val jsonStr = AppConfig.selectAll()
            .where { AppConfig.name eq ACADEMIC_CALENDAR_CONFIG_NAME }
            .map { it[AppConfig.value] }
            .singleOrNull()
        if (jsonStr == null) {
            val defaultData = AcademicCalendarData(
                AcademicDataSeed.CURRENT_ACADEMIC_YEAR,
                AcademicDataSeed.CURRENT_SEMESTER,
                AcademicDataSeed.academicCalendarMonths,
                AcademicDataSeed.academicCalendarEvents
            )
            AppConfig.insert {
                it[name] = ACADEMIC_CALENDAR_CONFIG_NAME
                it[value] = json.encodeToString(AcademicCalendarData.serializer(), defaultData)
            }
            return@transaction defaultData
        }
        val data = try {
            json.decodeFromString(AcademicCalendarData.serializer(), jsonStr)
        } catch (e: Exception) {
            null
        }

        if (data == null || data.months.isEmpty() || data.events.isEmpty()) {
            val defaultData = AcademicCalendarData(
                AcademicDataSeed.CURRENT_ACADEMIC_YEAR,
                AcademicDataSeed.CURRENT_SEMESTER,
                AcademicDataSeed.academicCalendarMonths,
                AcademicDataSeed.academicCalendarEvents
            )
            AppConfig.update({ AppConfig.name eq ACADEMIC_CALENDAR_CONFIG_NAME }) {
                it[value] = json.encodeToString(AcademicCalendarData.serializer(), defaultData)
            }
            return@transaction defaultData
        }
        data
    }

    fun saveAcademicCalendarData(data: AcademicCalendarData) = transaction {
        val jsonStr = json.encodeToString(AcademicCalendarData.serializer(), data)
        val exists = !AppConfig.selectAll().where { AppConfig.name eq ACADEMIC_CALENDAR_CONFIG_NAME }.empty()
        if (exists) {
            AppConfig.update({ AppConfig.name eq ACADEMIC_CALENDAR_CONFIG_NAME }) {
                it[value] = jsonStr
            }
        } else {
            AppConfig.insert {
                it[name] = ACADEMIC_CALENDAR_CONFIG_NAME
                it[value] = jsonStr
            }
        }
    }

    fun validateStudentApiKey(headerValue: String?): Boolean {
        val provided = headerValue?.trim().orEmpty()
        if (provided.isEmpty()) return false
        val expected = getStudentApiKey()
        return provided == expected
    }

    private const val INSECURE_FALLBACK_STUDENT_API_KEY: String =
        "cjlu-insecure-local-student-api-key-do-not-use-in-production"

    fun login(studentId: String, password: String): StudentProfileDto? = transaction {
        val row = Students.selectAll().where { Students.studentId eq studentId.trim() }.singleOrNull()
            ?: return@transaction null
        val hash = row[Students.passwordHash]
        if (!BCrypt.verifyer().verify(password.toCharArray(), hash).verified) return@transaction null
        rowToProfile(row)
    }

    fun changePassword(studentId: String, currentPassword: String, newPassword: String): Boolean =
        transaction {
            val row = Students.selectAll().where { Students.studentId eq studentId.trim() }.singleOrNull()
                ?: return@transaction false
            if (!BCrypt.verifyer().verify(currentPassword.toCharArray(), row[Students.passwordHash]).verified) {
                return@transaction false
            }
            val newHash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())
            Students.update({ Students.studentId eq studentId.trim() }) {
                it[passwordHash] = newHash
            } > 0
        }

    data class StudentSummary(
        val studentId: String,
        val displayName: String,
        val classSection: String,
    )

    fun listStudentSummaries(): List<StudentSummary> = transaction {
        Students.selectAll()
            .orderBy(Students.studentId to SortOrder.ASC)
            .map { row ->
                StudentSummary(
                    studentId = row[Students.studentId],
                    displayName = row[Students.displayName],
                    classSection = row[Students.classSection],
                )
            }
    }

    /** Batch lookup for admin request rows (plan name: lookupStudentNames). */
    fun lookupStudentNames(studentIds: Set<String>): Map<String, StudentSummary> =
        lookupStudentSummaries(studentIds)

    fun lookupStudentSummaries(studentIds: Set<String>): Map<String, StudentSummary> {
        if (studentIds.isEmpty()) return emptyMap()
        return transaction {
            Students.selectAll()
                .where { Students.studentId inList studentIds.map { it.trim() } }
                .associate { row ->
                    val id = row[Students.studentId]
                    id to StudentSummary(
                        studentId = id,
                        displayName = row[Students.displayName],
                        classSection = row[Students.classSection],
                    )
                }
        }
    }

    fun getStudentProfile(studentId: String): StudentProfileDto? = transaction {
        Students.selectAll().where { Students.studentId eq studentId.trim() }.singleOrNull()?.let(::rowToProfile)
    }

    fun patchStudentProfile(studentId: String, major: String, school: String): StudentProfileDto? = transaction {
        val id = studentId.trim()
        val row = Students.selectAll().where { Students.studentId eq id }.singleOrNull() ?: return@transaction null
        val newMajor = major.trim().ifBlank { row[Students.major] }
        val newSchool = school.trim().ifBlank { row[Students.school] }
        Students.update({ Students.studentId eq id }) {
            it[Students.major] = newMajor
            it[Students.school] = newSchool
        }
        Students.selectAll().where { Students.studentId eq id }.singleOrNull()?.let(::rowToProfile)
    }

    /**
     * Updates learning-related fields used by the student app for notifications.
     * @param attendancePercent when non-null, sets overall attendance (0–100).
     * @param classNoticeUpdate when non-null, sets notice text (empty string clears) and bumps [Students.classUpdateAtMillis].
     */
    fun patchStudentLearningAlerts(
        studentId: String,
        attendancePercent: Int?,
        classNoticeUpdate: String?,
    ): StudentProfileDto? = transaction {
        val id = studentId.trim()
        if (Students.selectAll().where { Students.studentId eq id }.empty()) return@transaction null
        if (attendancePercent != null) {
            Students.update({ Students.studentId eq id }) {
                it[Students.overallAttendancePercent] = attendancePercent.coerceIn(0, 100)
            }
        }
        if (classNoticeUpdate != null) {
            val now = System.currentTimeMillis()
            Students.update({ Students.studentId eq id }) {
                it[Students.classUpdateNotice] = classNoticeUpdate.trim()
                it[Students.classUpdateAtMillis] = now
            }
        }
        Students.selectAll().where { Students.studentId eq id }.singleOrNull()?.let(::rowToProfile)
    }

    private fun rowToProfile(row: ResultRow): StudentProfileDto {
        val noticeRaw = row[Students.classUpdateNotice].trim()
        return StudentProfileDto(
            studentId = row[Students.studentId],
            displayName = row[Students.displayName],
            classSection = row[Students.classSection],
            major = row[Students.major],
            school = row[Students.school],
            overallAttendancePercent = row[Students.overallAttendancePercent],
            classUpdateNotice = noticeRaw.ifEmpty { null },
            classUpdateAtMillis = row[Students.classUpdateAtMillis],
        )
    }

    fun catalogServiceExists(serviceId: String): Boolean = transaction {
        val id = serviceId.trim()
        !CatalogServices.selectAll().where { CatalogServices.id eq id }.empty()
    }

    fun listCatalogServices(preferChinese: Boolean): List<CatalogServiceDto> = transaction {
        CatalogServices.selectAll().orderBy(CatalogServices.id to SortOrder.ASC).map { row ->
            val zh = preferChinese
            CatalogServiceDto(
                id = row[CatalogServices.id],
                category = row[CatalogServices.category],
                title = if (zh) row[CatalogServices.titleZh] else row[CatalogServices.titleEn],
                description = if (zh) row[CatalogServices.descZh] else row[CatalogServices.descEn],
                turnaround = if (zh) row[CatalogServices.turnaroundZh] else row[CatalogServices.turnaroundEn],
                checklist = decodeChecklist(if (zh) row[CatalogServices.checklistZh] else row[CatalogServices.checklistEn]),
                isPopular = row[CatalogServices.isPopular],
            )
        }
    }

    private fun decodeChecklist(raw: String): List<String> =
        try {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        } catch (_: Exception) {
            emptyList()
        }

    fun listMessagesForStudent(studentId: String, preferChinese: Boolean): List<MessageDto> = transaction {
        val sid = studentId.trim()
        val reads = StudentMessageReads.selectAll().where { StudentMessageReads.studentId eq sid }
            .associate { it[StudentMessageReads.messageId] to it[StudentMessageReads.readAtMillis] }

        InboxMessages.selectAll()
            .orderBy(InboxMessages.sentAtMillis to SortOrder.DESC)
            .filter { row ->
                val to = row[InboxMessages.recipientStudentId]
                to == null || to == sid
            }
            .map { row ->
                val id = row[InboxMessages.id]
                val readAt = reads[id]
                val startsUnread = row[InboxMessages.startsUnread]
                val isRead = if (!startsUnread) true else readAt != null
                val zh = preferChinese
                MessageDto(
                    id = id,
                    category = row[InboxMessages.category],
                    sender = if (zh) row[InboxMessages.senderZh] else row[InboxMessages.senderEn],
                    title = if (zh) row[InboxMessages.titleZh] else row[InboxMessages.titleEn],
                    body = if (zh) row[InboxMessages.bodyZh] else row[InboxMessages.bodyEn],
                    timeLabel = if (zh) row[InboxMessages.timeLabelZh] else row[InboxMessages.timeLabelEn],
                    relatedServiceId = row[InboxMessages.relatedServiceId],
                    requiresAction = row[InboxMessages.requiresAction],
                    isRead = isRead,
                )
            }
    }

    /**
     * Inserts a staff-authored inbox row. [recipientStudentId] null = all students; otherwise only that student sees it.
     * @return new message id
     */
    fun insertAdminInboxMessage(
        recipientStudentId: String?,
        category: String,
        senderEn: String,
        senderZh: String,
        titleEn: String,
        titleZh: String,
        bodyEn: String,
        bodyZh: String,
        relatedServiceId: String?,
        requiresAction: Boolean,
    ): String = transaction {
        val id = "adm_" + java.util.UUID.randomUUID().toString().replace("-", "").take(24)
        val now = System.currentTimeMillis()
        val z = java.time.ZoneId.of("Asia/Shanghai")
        val zdt = java.time.Instant.ofEpochMilli(now).atZone(z)
        val timeEn = zdt.format(java.time.format.DateTimeFormatter.ofPattern("MMM d HH:mm", java.util.Locale.ENGLISH))
        val timeZh = zdt.format(java.time.format.DateTimeFormatter.ofPattern("M月d日 HH:mm", java.util.Locale.CHINA))
        InboxMessages.insert {
            it[InboxMessages.id] = id
            it[InboxMessages.recipientStudentId] = recipientStudentId?.trim()?.takeIf { s -> s.isNotEmpty() }
            it[InboxMessages.category] = category
            it[InboxMessages.senderEn] = senderEn
            it[InboxMessages.senderZh] = senderZh
            it[InboxMessages.titleEn] = titleEn
            it[InboxMessages.titleZh] = titleZh
            it[InboxMessages.bodyEn] = bodyEn
            it[InboxMessages.bodyZh] = bodyZh
            it[InboxMessages.timeLabelEn] = timeEn
            it[InboxMessages.timeLabelZh] = timeZh
            it[InboxMessages.relatedServiceId] = relatedServiceId
            it[InboxMessages.requiresAction] = requiresAction
            it[InboxMessages.startsUnread] = true
            it[InboxMessages.sentAtMillis] = now
        }
        id
    }

    fun studentExists(studentId: String): Boolean = transaction {
        val sid = studentId.trim()
        if (sid.isEmpty()) return@transaction false
        !Students.selectAll().where { Students.studentId eq sid }.empty()
    }

    fun markMessageRead(studentId: String, messageId: String, read: Boolean): Boolean = transaction {
        val sid = studentId.trim()
        val mid = messageId.trim()
        val visible = !InboxMessages.selectAll()
            .where {
                (InboxMessages.id eq mid) and (
                    InboxMessages.recipientStudentId.isNull() or (InboxMessages.recipientStudentId eq sid)
                    )
            }
            .empty()
        if (!visible) return@transaction false
        if (read) {
            val exists = !StudentMessageReads.selectAll()
                .where { (StudentMessageReads.studentId eq sid) and (StudentMessageReads.messageId eq mid) }
                .empty()
            if (exists) {
                StudentMessageReads.update(
                    { (StudentMessageReads.studentId eq sid) and (StudentMessageReads.messageId eq mid) },
                ) {
                    it[StudentMessageReads.readAtMillis] = System.currentTimeMillis()
                } > 0
            } else {
                StudentMessageReads.insert {
                    it[StudentMessageReads.studentId] = sid
                    it[StudentMessageReads.messageId] = mid
                    it[StudentMessageReads.readAtMillis] = System.currentTimeMillis()
                }
                true
            }
        } else {
            StudentMessageReads.deleteWhere {
                (StudentMessageReads.studentId eq sid) and (StudentMessageReads.messageId eq mid)
            }
            true
        }
    }

    fun getAllRequests(): List<StudentRequest> = transaction {
        StudentRequests.selectAll()
            .orderBy(StudentRequests.createdAtMillis to SortOrder.DESC)
            .map { it.toStudentRequest() }
    }

    fun getRequestById(id: String): StudentRequest? = transaction {
        StudentRequests.selectAll().where { StudentRequests.id eq id }
            .map { it.toStudentRequest() }
            .singleOrNull()
    }

    fun getRequestsForStudent(studentId: String): List<StudentRequest> = transaction {
        StudentRequests.selectAll()
            .where { StudentRequests.studentId eq studentId }
            .orderBy(StudentRequests.createdAtMillis to SortOrder.DESC)
            .map { it.toStudentRequest() }
    }

    fun addRequest(request: StudentRequest) = transaction {
        StudentRequests.insert {
            it[id] = request.id
            it[serviceId] = request.serviceId
            it[studentId] = request.studentId
            it[contactInfo] = request.contactInfo
            it[notes] = request.notes
            it[status] = request.status.name
            it[createdAtMillis] = request.createdAtMillis
            it[attachmentUrl] = request.attachmentUrl
        }
    }

    fun updateRequestStatus(requestId: String, newStatus: RequestStatus): Boolean = transaction {
        StudentRequests.update({ StudentRequests.id eq requestId }) {
            it[status] = newStatus.name
        } > 0
    }

    fun updateRequestAttachment(requestId: String, attachmentUrl: String): Boolean = transaction {
        StudentRequests.update({ StudentRequests.id eq requestId }) {
            it[StudentRequests.attachmentUrl] = attachmentUrl
        } > 0
    }

    fun studentOwnsAttachment(studentId: String, relativeUrl: String): Boolean = transaction {
        StudentRequests.selectAll().where {
            (StudentRequests.studentId eq studentId) and (StudentRequests.attachmentUrl eq relativeUrl)
        }.any()
    }

    fun attachmentExists(relativeUrl: String): Boolean = transaction {
        StudentRequests.selectAll().where { StudentRequests.attachmentUrl eq relativeUrl }.any()
    }

    private fun seedData() {
        val now = System.currentTimeMillis()
        val defaultStudentId = "20230950"

        val sampleRequests = listOf(
            StudentRequest(
                id = "CJLU-20260510-001",
                serviceId = "back_to_cjlu",
                studentId = defaultStudentId,
                contactInfo = "13812345678",
                notes = "Scheduled return for Autumn semester. Flight MH388.",
                status = RequestStatus.ActionNeeded,
                createdAtMillis = now - (3 * 24 * 60 * 60 * 1000L),
            ),
            StudentRequest(
                id = "CJLU-20260511-042",
                serviceId = "repair_request",
                studentId = defaultStudentId,
                contactInfo = "Dormitory 5, Room 302",
                notes = "Water leakage in the bathroom ceiling.",
                status = RequestStatus.InReview,
                createdAtMillis = now - (2 * 24 * 60 * 60 * 1000L),
            ),
            StudentRequest(
                id = "CJLU-20260512-105",
                serviceId = "ask_leave",
                studentId = defaultStudentId,
                contactInfo = "Medical Certificate Attached",
                notes = "Sick leave for 2 days due to flu.",
                status = RequestStatus.Completed,
                createdAtMillis = now - (1 * 24 * 60 * 60 * 1000L),
            ),
        )

        sampleRequests.forEach { addRequest(it) }
    }

    private fun ResultRow.toStudentRequest() = StudentRequest(
        id = this[StudentRequests.id],
        serviceId = this[StudentRequests.serviceId],
        studentId = this[StudentRequests.studentId],
        contactInfo = this[StudentRequests.contactInfo],
        notes = this[StudentRequests.notes],
        status = RequestStatus.valueOf(this[StudentRequests.status]),
        createdAtMillis = this[StudentRequests.createdAtMillis],
        attachmentUrl = this[StudentRequests.attachmentUrl],
    )

    fun upsertFcmToken(studentId: String, token: String) {
        val sid = studentId.trim()
        val t = token.trim()
        if (sid.isEmpty() || t.isEmpty()) return
        val now = System.currentTimeMillis()
        transaction {
            val existing = StudentFcmTokens.selectAll().where { StudentFcmTokens.token eq t }.singleOrNull()
            if (existing != null) {
                StudentFcmTokens.update({ StudentFcmTokens.token eq t }) {
                    it[StudentFcmTokens.studentId] = sid
                    it[StudentFcmTokens.updatedAtMillis] = now
                }
            } else {
                StudentFcmTokens.insert {
                    it[StudentFcmTokens.studentId] = sid
                    it[StudentFcmTokens.token] = t
                    it[StudentFcmTokens.updatedAtMillis] = now
                }
            }
            // Keep at most 5 tokens per student (drop oldest).
            val tokensForStudent = StudentFcmTokens
                .selectAll()
                .where { StudentFcmTokens.studentId eq sid }
                .orderBy(StudentFcmTokens.updatedAtMillis to SortOrder.DESC)
                .map { it[StudentFcmTokens.token] }
            if (tokensForStudent.size > 5) {
                tokensForStudent.drop(5).forEach { old ->
                    StudentFcmTokens.deleteWhere { StudentFcmTokens.token eq old }
                }
            }
        }
    }

    fun getFcmTokensForStudent(studentId: String): List<String> = transaction {
        StudentFcmTokens
            .select(StudentFcmTokens.token)
            .where { StudentFcmTokens.studentId eq studentId.trim() }
            .map { it[StudentFcmTokens.token] }
    }

    fun removeFcmToken(token: String) {
        val t = token.trim()
        if (t.isEmpty()) return
        transaction {
            StudentFcmTokens.deleteWhere { StudentFcmTokens.token eq t }
        }
    }
}
