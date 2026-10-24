package com.kpyruy.takt.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.RoomStudyPlanRepository
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class CourseColorPersistenceTest {
    @Test fun colorSurvivesUisAdoptionRefreshReopenAndCanReset() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "course-color-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, TaktDatabase::class.java, name).build()
        try {
            var repo = RoomStudyPlanRepository(db)
            val localId = repo.addCourse("Physics", "PHYS", 5, 1)
            repo.setColor(localId, "TEAL")
            val remote = Course("uis:101:10", "PHYS", "Refreshed Physics", 6, 1, CourseStatus.ENROLLED)
            repo.upsertImportedCourses(listOf(remote))
            assertEquals("TEAL", repo.observeCourse(remote.id).first()!!.colorKey)
            repo.upsertImportedCourses(listOf(remote.copy(title = "Updated")))
            db.close()
            db = Room.databaseBuilder(context, TaktDatabase::class.java, name).build()
            repo = RoomStudyPlanRepository(db)
            assertEquals("TEAL", repo.observeCourse(localId).first()!!.colorKey)
            repo.setColor(localId, null)
            assertNull(repo.observeCourse(remote.id).first()!!.colorKey)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun schema20UpgradePreservesCourseAndDefaultsToAutomatic() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "course-color-migration-${UUID.randomUUID()}.db"
        val schema = org.json.JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open(
            "com.kpyruy.takt.core.database.TaktDatabase/20.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(name, 0, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO courses(id,code,title,credits,semester,status,requirementType) VALUES('own','OWN','Existing',5,1,'enrolled','COMPULSORY')")
            old.version = 20
        }
        val db = Room.databaseBuilder(context, TaktDatabase::class.java, name)
            .addMigrations(TaktDatabase.MIGRATION_20_21, TaktDatabase.MIGRATION_21_22).build()
        try {
            val course = RoomStudyPlanRepository(db).observeCourse("own").first()!!
            assertEquals("Existing", course.title)
            assertNull(course.colorKey)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
