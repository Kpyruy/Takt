package com.kpyruy.takt.app

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.database.*
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class StudentHubStorageTest {
    @Test fun shippedSchema19UpgradeRetainsCoursesAndCreatesEmptyStudentCache()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val name="student-upgrade-${UUID.randomUUID()}.db"
        val schema=org.json.JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open(
            "com.kpyruy.takt.core.database.TaktDatabase/19.json").bufferedReader().use{it.readText()}).getJSONObject("database")
        context.openOrCreateDatabase(name,0,null).use { old ->
            val entities=schema.getJSONArray("entities")
            for(i in 0 until entities.length()){val e=entities.getJSONObject(i);old.execSQL(e.getString("createSql").replace("\${TABLE_NAME}",e.getString("tableName")))}
            val setup=schema.getJSONArray("setupQueries");for(i in 0 until setup.length())old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO courses(id,code,title,credits,semester,status,requirementType) VALUES('own','OWN','Existing',5,1,'enrolled','COMPULSORY')")
            old.version=19
        }
        val db=Room.databaseBuilder(context,TaktDatabase::class.java,name).addMigrations(TaktDatabase.MIGRATION_19_20, TaktDatabase.MIGRATION_20_21, TaktDatabase.MIGRATION_21_22).build()
        try{assertEquals("own",db.courseDao().getAllSnapshot().single().id);assertNull(RoomStudentHubStore(db).read())}
        finally{db.close();context.deleteDatabase(name)}
    }
    @Test fun failedStudentRestoreRollsBackRowsSettingsAndProfileTogether()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<Context>();val tag="student-restore-${UUID.randomUUID()}"
        val context=object:ContextWrapper(app){override fun getApplicationContext():Context=this
            override fun getSharedPreferences(name:String,mode:Int)=app.getSharedPreferences("$tag-$name",mode)}
        val db=Room.inMemoryDatabaseBuilder(context,TaktDatabase::class.java).build()
        val prefs=SharedPreferencesAppSettingsRepository(context)
        val store=RoomStudentHubStore(db);val backup=RoomBackupRepository(db,prefs)
        val old=StudentHubSnapshot(identity=StudentIdentity(name="Original"),displayName="Display")
        try{
            store.write(old);prefs.setLanguage(AppLanguage.SLOVAK)
            db.courseDao().insertAll(listOf(CourseEntity("old","OLD","Old",5,1,"enrolled","COMPULSORY",null)))
            val before=backup.exportJson()
            val incoming=BackupPayload(studentHub=old.copy(displayName="Replacement").toBackupStudentHub(),settings=BackupSettings(language="ENGLISH"))
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_student BEFORE INSERT ON student_hub BEGIN SELECT RAISE(ABORT,'fixture'); END")
            assertTrue(runCatching{backup.importJson(BackupPayloadCodec.encode(incoming))}.isFailure)
            assertEquals(before,backup.exportJson());assertEquals(old,store.read());assertEquals(AppLanguage.SLOVAK,prefs.settings.value.language)
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_student")
            backup.importJson(BackupPayloadCodec.encode(incoming));assertEquals("Replacement",store.read()!!.displayName)
        }finally{db.close();context.getSharedPreferences("takt_settings",0).edit().clear().commit()}
    }
}
