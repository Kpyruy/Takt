package com.kpyruy.takt.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.database.*
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AssessmentStatePersistenceTest {
    @Test fun schema17DefaultsPreserveOldResultsAndNewFlagsRoundTripThroughBackup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "takt-assessment-migration-${System.nanoTime()}.db"
        val json = org.json.JSONObject(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.kpyruy.takt.core.database.TaktDatabase/17.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val entities = json.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = json.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO courses(id,code,title,credits,semester,status,requirementType) VALUES('course','CODE','Course',5,3,'enrolled','COMPULSORY')")
            old.execSQL("INSERT INTO grade_items(id,courseId,title,type,earnedPoints,maxPoints,recordedAtEpochMillis,completed,requiredForExam,minimumPointsForExam) VALUES('test','course','Test','TEST',6,10,1,1,1,5)")
            old.version = 17
        }
        val db = Room.databaseBuilder(context,TaktDatabase::class.java,name).addMigrations(TaktDatabase.MIGRATION_17_18, TaktDatabase.MIGRATION_18_19, TaktDatabase.MIGRATION_19_20, TaktDatabase.MIGRATION_20_21).build()
        try {
            val grades = RoomGradeRepository(db)
            val original = grades.observeItems("course").first().single()
            assertTrue(original.completed);assertTrue(original.contributesToGrade);assertFalse(original.awaitingResult)
            assertEquals(6.0, original.earnedPoints, 0.0)
            val waiting = original.copy(completed=false, earnedPoints=0.0, contributesToGrade=false, awaitingResult=true)
            grades.upsertItem(waiting)
            val backup = RoomBackupRepository(db,SharedPreferencesAppSettingsRepository(context))
            val exported = backup.exportJson()
            val payload = BackupPayloadCodec.decode(exported)
            assertFalse(payload.gradeItems.single().contributesToGrade)
            assertTrue(payload.gradeItems.single().awaitingResult)
            backup.importJson(exported)
            assertEquals(waiting, grades.observeItems("course").first().single())
            // Real old JSON has neither field; decode defaults keep previous grading behavior.
            val oldPayload = BackupPayloadCodec.decode("""{"version":8,"courses":[{"id":"course","code":"CODE","title":"Course","credits":5,"semester":3,"status":"enrolled","requirementType":"COMPULSORY"}],"gradeItems":[{"id":"old","courseId":"course","title":"Old","type":"TEST","earnedPoints":7.0,"maxPoints":10.0,"recordedAtEpochMillis":1}]}""")
            assertTrue(oldPayload.gradeItems.single().contributesToGrade);assertFalse(oldPayload.gradeItems.single().awaitingResult)
        } finally { db.close();context.deleteDatabase(name) }
    }
}
