package com.kpyruy.takt.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kpyruy.takt.core.database.*
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataIntegrityTest {
    private lateinit var db: TaktDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaktDatabase::class.java).build() }
    @After fun close() { db.close() }

    @Test fun importMigratesLocalCourseAndEveryPersonalDependency() = runBlocking {
        db.courseDao().insertAll(listOf(CourseEntity("local", "CODE", "Local", 1, 1, "enrolled", "COMPULSORY", null, iconKey = "Science")))
        db.scheduleDao().upsertRule(ScheduleRuleEntity("rule", "local", "Lesson", 1, 60, 120, "WEEKLY", null))
        db.scheduleDao().upsertOneOffEvent(OneOffScheduleEventEntity("event", "local", "Event", 1, 60, 120, null, "BLOCK_ACTION"))
        db.studyContentDao().upsertTask(StudyTaskEntity("task", "local", "Task", "personal", null, false))
        db.studyContentDao().upsertNote(CourseNoteEntity("note", "local", "Note", "content", 1, "[]"))
        db.gradeDao().upsertItem(GradeItemEntity("grade", "local", "Grade", "TEST", 3.0, 5.0, 1))
        db.gradeDao().upsertScale(GradeScaleEntity("local", 90.0, 80.0, 70.0, 60.0, 50.0))
        db.gradeDao().upsertOverride(GradeOverrideEntity("local", "B"))
        db.examDao().upsertExamInfo(ExamInfoEntity("local", "grade", 1, 60, 120, "Room", 1, 3, "notes"))
        db.examDao().upsertMaterial(ExamMaterialEntity("material", "local", "Material", "content://material"))
        val settings = SharedPreferencesAppSettingsRepository(ApplicationProvider.getApplicationContext())
        val backup = RoomBackupRepository(db, settings)
        val legacyBackup = BackupPayloadCodec.encode(BackupPayloadCodec.decode(backup.exportJson()).copy(version = 7))
        val repository = RoomStudyPlanRepository(db)
        val remote = Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED, CourseRequirementType.COMPULSORY, null)
        repository.upsertImportedCourses(listOf(remote))
        assertEquals(listOf(remote.id), db.courseDao().getAllSnapshot().map { it.id })
        assertEquals("Science", db.courseDao().getAllSnapshot().single().iconKey)
        assertEquals(remote.id, repository.observeCourse("local").first()?.id)
        assertEquals(remote.id, db.scheduleDao().getRulesSnapshot().single().courseId)
        assertEquals(remote.id, db.scheduleDao().getOneOffEventsSnapshot().single().courseId)
        assertEquals(remote.id, db.studyContentDao().getTasksSnapshot().single().courseId)
        assertEquals(remote.id, db.studyContentDao().getNotesSnapshot().single().courseId)
        assertEquals(remote.id, db.gradeDao().getItemsSnapshot().single().courseId)
        assertEquals(remote.id, db.gradeDao().getScalesSnapshot().single().courseId)
        assertEquals(remote.id, db.gradeDao().getOverridesSnapshot().single().courseId)
        assertEquals(remote.id, db.examDao().getExamInfoSnapshot().single().courseId)
        assertEquals(remote.id, db.examDao().getMaterialsSnapshot().single().courseId)
        repository.upsertImportedCourses(listOf(remote))
        assertEquals(1, db.courseDao().count())
        val exported = backup.exportJson()
        db.courseDao().deleteAllAliases()
        db.courseDao().deleteAll()
        backup.importJson(exported)
        assertEquals(remote.id, repository.observeCourse("local").first()?.id)
        assertEquals(remote.id, db.studyContentDao().getNotesSnapshot().single().courseId)
        backup.importJson(legacyBackup)
        assertEquals("local", db.studyContentDao().getNotesSnapshot().single().courseId)
        repository.upsertImportedCourses(listOf(remote))
        assertEquals(remote.id, db.studyContentDao().getNotesSnapshot().single().courseId)
        assertEquals(remote.id, repository.observeCourse("local").first()?.id)
    }

    @Test fun shippedSchema16MigratesToLatestWithPersonalDataAndIdentityTables() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "takt-migration-test-${System.nanoTime()}.db"
        val schema = org.json.JSONObject(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("com.kpyruy.takt.core.database.TaktDatabase/16.json").bufferedReader().use { it.readText() })
            .getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) old.execSQL(setup.getString(index))
            old.execSQL("INSERT INTO courses(id, code, title, credits, semester, status, requirementType) VALUES('local', 'CODE', 'Local', 1, 1, 'enrolled', 'COMPULSORY')")
            old.execSQL("INSERT INTO course_notes(id, courseId, title, content, updatedAtEpochMillis, attachmentsJson) VALUES('note', 'local', 'Note', 'Personal', 1, '[]')")
            old.version = 16
        }
        val migrated = Room.databaseBuilder(context, TaktDatabase::class.java, name)
            .addMigrations(TaktDatabase.MIGRATION_16_17, TaktDatabase.MIGRATION_17_18, TaktDatabase.MIGRATION_18_19, TaktDatabase.MIGRATION_19_20, TaktDatabase.MIGRATION_20_21).build()
        try {
            assertEquals("local", migrated.courseDao().getAllSnapshot().single().id)
            assertEquals("Personal", migrated.studyContentDao().getNotesSnapshot().single().content)
            assertTrue(migrated.courseDao().getAliasesSnapshot().isEmpty())
            assertTrue(migrated.courseDao().getImportDecisionsSnapshot().isEmpty())
            RoomStudyPlanRepository(migrated).upsertImportedCourses(listOf(Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED)), mapOf("uis:1:101" to "local"))
            assertEquals("uis:1:101", migrated.studyContentDao().getNotesSnapshot().single().courseId)
            assertEquals(listOf("uis:1:101"), migrated.courseDao().getImportDecisionsSnapshot().map { it.courseId })
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun staleEditorWritesResolveAliasAtEveryDependencyBoundary() = runBlocking {
        db.courseDao().insertAll(listOf(CourseEntity("local", "CODE", "Local", 1, 1, "enrolled", "COMPULSORY", null)))
        RoomStudyPlanRepository(db).upsertImportedCourses(listOf(Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED)))
        val content = RoomStudyContentRepository(db)
        val grades = RoomGradeRepository(db)
        val exams = RoomExamRepository(db)
        val schedule = RoomScheduleRepository(db)
        content.upsertTask(StudyTask("task", "local", "Task", "Draft", null, false))
        content.upsertNote(CourseNote("note", "local", "Note", "Draft", 1))
        grades.upsertItem(GradeItem("grade", "local", "Grade", GradeItemType.TEST, 3.0, 5.0))
        grades.setScale("local", GradeScale.default())
        grades.setManualGrade("local", GradeLetter.B)
        exams.upsertExamInfo(ExamInfo("local", "grade", null, null, null, null, notes = "Draft"))
        exams.upsertMaterial(ExamMaterial("material", "local", "Material", "content://material"))
        schedule.upsertRule(ScheduleRule("rule", "local", "Lesson", java.time.DayOfWeek.TUESDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0), ScheduleRecurrence.WEEKLY))
        schedule.upsertOneOffEvent(OneOffScheduleEvent("event", "local", "Event", java.time.LocalDate.of(2026, 10, 12), java.time.LocalTime.of(11, 0), java.time.LocalTime.of(12, 0), type = OneOffScheduleEventType.BLOCK_ACTION))
        assertEquals("uis:1:101", db.studyContentDao().getTasksSnapshot().single().courseId)
        assertEquals("uis:1:101", db.studyContentDao().getNotesSnapshot().single().courseId)
        assertEquals("uis:1:101", db.gradeDao().getItemsSnapshot().single().courseId)
        assertEquals("uis:1:101", db.gradeDao().getScalesSnapshot().single().courseId)
        assertEquals("uis:1:101", db.gradeDao().getOverridesSnapshot().single().courseId)
        assertEquals("uis:1:101", db.examDao().getExamInfoSnapshot().single().courseId)
        assertEquals("uis:1:101", db.examDao().getMaterialsSnapshot().single().courseId)
        assertEquals("uis:1:101", db.scheduleDao().getRulesSnapshot().single().courseId)
        assertEquals("uis:1:101", db.scheduleDao().getOneOffEventsSnapshot().single().courseId)
        assertEquals("note", content.observeNotes("local").first().single().id)
        assertEquals("grade", grades.observeItems("local").first().single().id)
        assertEquals("material", exams.observeMaterials("local").first().single().id)
        grades.setManualGrade("local", null)
        assertTrue(db.gradeDao().getOverridesSnapshot().isEmpty())
        schedule.replaceSchedule(listOf(ScheduleRule("replacement", "local", "Lesson", java.time.DayOfWeek.TUESDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0), ScheduleRecurrence.WEEKLY)), emptyList())
        assertEquals("uis:1:101", db.scheduleDao().getRulesSnapshot().single().courseId)
    }

    @Test fun unknownCourseWriteCannotCreateAnOrphan() = runBlocking {
        val content = RoomStudyContentRepository(db)
        assertNotNull(runCatching { content.upsertNote(CourseNote("note", "missing", "Note", "Draft", 1)) }.exceptionOrNull())
        assertTrue(db.studyContentDao().getNotesSnapshot().isEmpty())
    }

    @Test fun automaticMetadataImportDoesNotApplyUnapprovedProgress() = runBlocking {
        db.courseDao().insertAll(listOf(CourseEntity("local", "CODE", "Local", 1, 1, "fulfilled", "COMPULSORY", null,
            gradingType = "PASS_FAIL", passFailResult = "PASSED", officialGrade = "A", fulfilledOnEpochDay = 5)))
        val repository = RoomStudyPlanRepository(db)
        val local = repository.observeCourses().first()
        val remote = Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED,
            gradingType = CourseGradingType.PASS_FAIL, passFailResult = PassFailResult.FAILED, officialGrade = GradeLetter.C)
        repository.upsertImportedCourses(prepareAutomaticCourseImports(listOf(remote), local, importMetadata = true))
        val stored = db.courseDao().getAllSnapshot().single()
        assertEquals("uis:1:101", stored.id)
        assertEquals("UIS", stored.title)
        assertEquals(5, stored.credits)
        assertEquals("fulfilled", stored.status)
        assertEquals("PASSED", stored.passFailResult)
        assertEquals("A", stored.officialGrade)
        assertEquals(5L, stored.fulfilledOnEpochDay)
    }

    @Test fun automaticProgressCanonicalizationKeepsMetadataOutsideSelectedSection() = runBlocking {
        db.courseDao().insertAll(listOf(CourseEntity("local", "CODE", "Local", 1, 1, "fulfilled", "COMPULSORY", null)))
        val repository = RoomStudyPlanRepository(db)
        val remote = Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED)
        repository.upsertImportedCourses(prepareAutomaticCourseImports(listOf(remote), repository.observeCourses().first(), importMetadata = false))
        assertEquals("uis:1:101", db.courseDao().getAllSnapshot().single().id)
        assertEquals("Local", db.courseDao().getAllSnapshot().single().title)
        assertEquals(1, db.courseDao().getAllSnapshot().single().credits)
        assertEquals("fulfilled", db.courseDao().getAllSnapshot().single().status)
    }

    @Test fun changedSubjectIdInSameStudyMigratesDependenciesWithoutDuplicates() = runBlocking {
        db.courseDao().insertAll(listOf(CourseEntity("uis:1:100", "CODE", "Old UIS", 1, 1, "enrolled", "COMPULSORY", null)))
        db.studyContentDao().upsertNote(CourseNoteEntity("note", "uis:1:100", "Note", "personal", 1))
        val repository = RoomStudyPlanRepository(db)
        val remote = Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED)
        repository.upsertImportedCourses(listOf(remote))
        assertEquals(1, db.courseDao().count())
        assertEquals("uis:1:101", db.studyContentDao().getNotesSnapshot().single().courseId)
        assertEquals("uis:1:101", repository.observeCourse("uis:1:100").first()?.id)
    }

    @Test fun sameCodeInDifferentStudyNeverMigratesPersonalData() = runBlocking {
        db.courseDao().insertAll(listOf(CourseEntity("uis:2:100", "CODE", "Other study", 1, 1, "enrolled", "COMPULSORY", null)))
        db.studyContentDao().upsertNote(CourseNoteEntity("note", "uis:2:100", "Note", "personal", 1))
        val repository = RoomStudyPlanRepository(db)
        val remote = Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED)
        assertNull(repository.observeCourses().first().matchingUisCourse(remote))
        repository.upsertImportedCourses(listOf(remote))
        assertEquals(2, db.courseDao().count())
        assertEquals("uis:2:100", db.studyContentDao().getNotesSnapshot().single().courseId)
    }

    @Test fun migrationFailureRollsBackAllEarlierDependencyChanges() = runBlocking {
        val original = CourseEntity("local", "CODE", "Local", 1, 1, "enrolled", "COMPULSORY", null)
        db.courseDao().insertAll(listOf(original))
        db.scheduleDao().upsertRule(ScheduleRuleEntity("rule", "local", "Lesson", 1, 60, 120, "WEEKLY", null))
        db.studyContentDao().upsertNote(CourseNoteEntity("note", "local", "Note", "personal", 1))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_note_remap BEFORE UPDATE ON course_notes BEGIN SELECT RAISE(ABORT, 'Injected remap failure'); END")
        val failure = runCatching { RoomStudyPlanRepository(db).upsertImportedCourses(listOf(Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED))) }.exceptionOrNull()
        assertNotNull(failure)
        assertEquals(listOf(original), db.courseDao().getAllSnapshot())
        assertEquals("local", db.scheduleDao().getRulesSnapshot().single().courseId)
        assertEquals("local", db.studyContentDao().getNotesSnapshot().single().courseId)
        assertTrue(db.courseDao().getAliasesSnapshot().isEmpty())
    }

    @Test fun explicitSelectionMigratesOnlyChosenDuplicate() = runBlocking {
        val first = CourseEntity("first", "CODE", "First", 1, 1, "enrolled", "COMPULSORY", null)
        db.courseDao().insertAll(listOf(first, first.copy(id = "second", title = "Second")))
        db.studyContentDao().upsertNote(CourseNoteEntity("first-note", "first", "First note", "personal", 1))
        db.studyContentDao().upsertNote(CourseNoteEntity("second-note", "second", "Second note", "personal", 1))
        val remote = Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED)
        val repository = RoomStudyPlanRepository(db)
        repository.upsertImportedCourses(listOf(remote), mapOf(remote.id to "first"))
        assertEquals(setOf("second", "uis:1:101"), db.courseDao().getAllSnapshot().map { it.id }.toSet())
        assertEquals("uis:1:101", db.studyContentDao().getNotesSnapshot().first { it.id == "first-note" }.courseId)
        assertEquals("second", db.studyContentDao().getNotesSnapshot().first { it.id == "second-note" }.courseId)
        repository.upsertImportedCourses(listOf(remote))
        assertEquals(2, db.courseDao().count())
    }

    @Test fun choosingCanonicalCoursePersistsWithoutAdoptingRejectedOfflineDuplicate() = runBlocking {
        val original = CourseEntity("local", "CODE", "Local", 1, 1, "enrolled", "COMPULSORY", null)
        db.courseDao().insertAll(listOf(original, original.copy(id = "uis:1:101", title = "UIS")))
        db.studyContentDao().upsertNote(CourseNoteEntity("local-note", "local", "Local note", "personal", 1))
        val repository = RoomStudyPlanRepository(db)
        val remote = Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED)
        repository.upsertImportedCourses(listOf(remote), mapOf(remote.id to remote.id))
        assertTrue(db.courseDao().getAliasesSnapshot().isEmpty())
        assertEquals(listOf("uis:1:101"), db.courseDao().getImportDecisionsSnapshot().map { it.courseId })
        val backup = RoomBackupRepository(db, SharedPreferencesAppSettingsRepository(ApplicationProvider.getApplicationContext()))
        val exported = backup.exportJson()
        backup.importJson(exported)
        repository.upsertImportedCourses(listOf(remote))
        assertEquals(2, db.courseDao().count())
        assertEquals("local", db.studyContentDao().getNotesSnapshot().single().courseId)
        assertTrue(db.courseDao().getAliasesSnapshot().isEmpty())
    }

    @Test fun existingCanonicalCourseRetainsBothSetsOfPersonalNotes() = runBlocking {
        val original = CourseEntity("local", "CODE", "Local", 1, 1, "enrolled", "COMPULSORY", null)
        db.courseDao().insertAll(listOf(original, original.copy(id = "uis:1:101", title = "UIS")))
        db.studyContentDao().upsertNote(CourseNoteEntity("local-note", "local", "Local note", "personal", 1))
        db.studyContentDao().upsertNote(CourseNoteEntity("uis-note", "uis:1:101", "UIS note", "other personal", 1))
        RoomStudyPlanRepository(db).upsertImportedCourses(listOf(Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED)), mapOf("uis:1:101" to "local"))
        assertEquals(1, db.courseDao().count())
        assertEquals(setOf("local-note", "uis-note"), db.studyContentDao().getNotesSnapshot().map { it.id }.toSet())
        assertEquals(setOf("uis:1:101"), db.studyContentDao().getNotesSnapshot().map { it.courseId }.toSet())
    }

    @Test fun conflictingCanonicalScaleKeepsBothRecordsUnchanged() = runBlocking {
        val original = CourseEntity("local", "CODE", "Local", 1, 1, "enrolled", "COMPULSORY", null)
        db.courseDao().insertAll(listOf(original, original.copy(id = "uis:1:101", title = "UIS")))
        db.gradeDao().upsertScale(GradeScaleEntity("local", 90.0, 80.0, 70.0, 60.0, 50.0))
        db.gradeDao().upsertScale(GradeScaleEntity("uis:1:101", 95.0, 85.0, 75.0, 65.0, 55.0))
        val before = db.gradeDao().getScalesSnapshot()
        assertNotNull(runCatching { RoomStudyPlanRepository(db).upsertImportedCourses(listOf(Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED)), mapOf("uis:1:101" to "local")) }.exceptionOrNull())
        assertEquals(before, db.gradeDao().getScalesSnapshot())
        assertEquals(2, db.courseDao().count())
    }

    @Test fun successfulReplacementClearsOldExceptionsAndAbsences() = runBlocking {
        db.scheduleDao().upsertRule(ScheduleRuleEntity("old", null, "Old", 1, 60, 120, "WEEKLY", null))
        db.scheduleDao().upsertOneOffEvent(OneOffScheduleEventEntity("old-event", null, "Old event", 1, 60, 120, null, "BLOCK_ACTION"))
        db.scheduleDao().upsertException(ScheduleExceptionEntity("exception", "old", 1, "CANCELLED", null, null, null, null))
        db.scheduleDao().upsertAbsences(listOf(LessonAbsenceEntity("old", 1, false), LessonAbsenceEntity("old-event", 1, true)))
        val rule = ScheduleRule("new", null, "New", java.time.DayOfWeek.TUESDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0), ScheduleRecurrence.WEEKLY)
        val event = OneOffScheduleEvent("new-event", null, "New event", java.time.LocalDate.of(2026, 10, 12), java.time.LocalTime.of(11, 0), java.time.LocalTime.of(12, 0), type = OneOffScheduleEventType.BLOCK_ACTION)
        RoomScheduleRepository(db).replaceSchedule(listOf(rule), listOf(event))
        assertEquals(listOf("new"), db.scheduleDao().getRulesSnapshot().map { it.id })
        assertEquals(listOf("new-event"), db.scheduleDao().getOneOffEventsSnapshot().map { it.id })
        assertTrue(db.scheduleDao().getExceptionsSnapshot().isEmpty())
        assertTrue(db.scheduleDao().getAbsencesSnapshot().isEmpty())
    }

    @Test fun invalidReplacementLinksKeepOldSchedule() = runBlocking {
        db.scheduleDao().upsertRule(ScheduleRuleEntity("old", null, "Old", 1, 60, 120, "WEEKLY", null))
        val rule = ScheduleRule("new", "missing", "New", java.time.DayOfWeek.TUESDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0), ScheduleRecurrence.WEEKLY)
        assertNotNull(runCatching { RoomScheduleRepository(db).replaceSchedule(listOf(rule), emptyList()) }.exceptionOrNull())
        assertEquals(listOf("old"), db.scheduleDao().getRulesSnapshot().map { it.id })
    }

    @Test fun missingOfficialResultFieldsKeepKnownGradeAndDate() = runBlocking {
        db.courseDao().insertAll(listOf(CourseEntity("local", "CODE", "Local", 1, 1, "fulfilled", "COMPULSORY", null, officialGrade = "A", fulfilledOnEpochDay = 5)))
        val repository = RoomStudyPlanRepository(db)
        repository.setOfficialResult("local", null, null)
        assertEquals("A", db.courseDao().getAllSnapshot().single().officialGrade)
        assertEquals(5L, db.courseDao().getAllSnapshot().single().fulfilledOnEpochDay)
        repository.setOfficialResult("local", GradeLetter.C, null)
        assertEquals("C", db.courseDao().getAllSnapshot().single().officialGrade)
        assertEquals(5L, db.courseDao().getAllSnapshot().single().fulfilledOnEpochDay)
    }

    @Test fun invalidUisIdLeavesExistingCourseUntouched() = runBlocking {
        val original = CourseEntity("local", "CODE", "Local", 1, 1, "enrolled", "COMPULSORY", null)
        db.courseDao().insertAll(listOf(original))
        val remote = Course("uis:1:", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED)
        val failure = runCatching { RoomStudyPlanRepository(db).upsertImportedCourses(listOf(remote)) }.exceptionOrNull()
        assertNotNull("Missing UIS identifier must be rejected", failure)
        assertEquals(listOf(original), db.courseDao().getAllSnapshot())
    }

    @Test fun ambiguousCodeNeverChoosesAnArbitraryLocalCourse() = runBlocking {
        val first = CourseEntity("first", "CODE", "First", 1, 1, "enrolled", "COMPULSORY", null)
        val second = first.copy(id = "second", title = "Second")
        db.courseDao().insertAll(listOf(first, second))
        val remote = Course("uis:1:101", "CODE", "UIS", 5, 2, CourseStatus.ENROLLED)
        val failure = runCatching { RoomStudyPlanRepository(db).upsertImportedCourses(listOf(remote)) }.exceptionOrNull()
        assertNotNull("Ambiguous code requires explicit selection", failure)
        assertEquals(setOf(first, second), db.courseDao().getAllSnapshot().toSet())
    }

    @Test fun cancelledReplacementKeepsOldSchedule() = runBlocking {
        db.scheduleDao().upsertRule(ScheduleRuleEntity("old", null, "Old", 1, 60, 120, "WEEKLY", null))
        db.scheduleDao().upsertException(ScheduleExceptionEntity("exception", "old", 1, "CANCELLED", null, null, null, null))
        db.scheduleDao().upsertAbsences(listOf(LessonAbsenceEntity("old", 1, false)))
        val actual = db.scheduleDao()
        val cancelled = object : ScheduleDao by actual {
            override suspend fun upsertRules(items: List<ScheduleRuleEntity>) { throw kotlinx.coroutines.CancellationException("Cancelled during replacement") }
        }
        val schedule = RoomScheduleRepository(db, cancelled)
        val account = UniversityAccountRepository(ApplicationProvider.getApplicationContext<Context>(), scheduleRepository = schedule)
        val preview = UisSyncPreview(emptyList(), emptyList(), 1, null, null, emptyList(), emptyList(), emptyList(), emptyList(), null, null, null, null)
        val field = UniversityAccountRepository::class.java.getDeclaredField("mutablePreview").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (field.get(account) as kotlinx.coroutines.flow.MutableStateFlow<UisSyncPreview?>).value = preview
        try { account.replaceSchedule() } catch (_: kotlinx.coroutines.CancellationException) { }
        assertEquals(listOf("old"), actual.getRulesSnapshot().map { it.id })
        assertEquals(listOf("exception"), actual.getExceptionsSnapshot().map { it.id })
        assertEquals(listOf("old"), actual.getAbsencesSnapshot().map { it.eventId })
    }

    @Test fun failedScheduleReplacementKeepsOldLessonsExceptionsAndAbsences() = runBlocking {
        db.scheduleDao().upsertRule(ScheduleRuleEntity("old", null, "Old", 1, 60, 120, "WEEKLY", null))
        db.scheduleDao().upsertException(ScheduleExceptionEntity("exception", "old", 1, "CANCELLED", null, null, null, null))
        db.scheduleDao().upsertAbsences(listOf(LessonAbsenceEntity("old", 1, false)))
        val actual = db.scheduleDao()
        val failing = object : ScheduleDao by actual {
            override suspend fun upsertRules(items: List<ScheduleRuleEntity>) { error("Injected insert failure") }
        }
        val schedule = RoomScheduleRepository(db, failing)
        val account = UniversityAccountRepository(ApplicationProvider.getApplicationContext<Context>(), scheduleRepository = schedule)
        val preview = UisSyncPreview(emptyList(), emptyList(), 1, null, null, emptyList(), emptyList(), emptyList(), emptyList(), null, null, null, null)
        val field = UniversityAccountRepository::class.java.getDeclaredField("mutablePreview").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (field.get(account) as kotlinx.coroutines.flow.MutableStateFlow<UisSyncPreview?>).value = preview
        try { account.replaceSchedule() } catch (_: IllegalStateException) { }
        assertEquals(listOf("old"), actual.getRulesSnapshot().map { it.id })
        assertEquals(listOf("exception"), actual.getExceptionsSnapshot().map { it.id })
        assertEquals(listOf("old"), actual.getAbsencesSnapshot().map { it.eventId })
    }
}
