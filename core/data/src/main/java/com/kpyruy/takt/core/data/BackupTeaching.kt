package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.data.uis.UisTeachingLinks
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

private val teachingJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** Portable teaching metadata only. Credentials, session cookies and portraits are excluded. */
@Serializable
data class BackupTeaching(
    val teams: List<BackupTeachingTeam> = emptyList(),
    val profiles: List<BackupTeacherProfile> = emptyList(),
) {
    fun forCourses(codes: List<String>): BackupTeaching? {
        val keys = codes.map(::teachingCode).toSet()
        val filtered = teams.filter { teachingCode(it.courseCode) in keys }
        val people = filtered.flatMap { it.teachers }.map { it.personId }.toSet()
        return BackupTeaching(filtered, profiles.filter { it.personId in people }).takeIf { it.teams.isNotEmpty() }
    }
    fun validated(): BackupTeaching {
        require(teams.size <= 128 && profiles.size <= 256)
        require(teams.map { teachingCode(it.courseCode) }.distinct().size == teams.size)
        require(profiles.map { it.personId to it.language }.distinct().size == profiles.size)
        teams.forEach { team ->
            require(team.courseCode.isNotBlank() && team.courseCode.length <= 100 && team.teachers.size <= 100)
            require(team.teachers.map { it.personId }.distinct().size == team.teachers.size)
            team.teachers.forEach { require(UisTeachingLinks.validId(it.personId) && it.name.length <= 1000) }
        }
        profiles.forEach { require(UisTeachingLinks.validId(it.personId) && it.language in AppLanguage.entries.map { l -> l.name }) }
        require(teachingJson.encodeToString(this).toByteArray(Charsets.UTF_8).size <= 4_000_000) { "Teaching data is too large" }
        return this
    }
}

@Serializable
data class BackupTeachingTeam(val courseCode: String, val teachers: List<BackupTeacher>)
@Serializable
data class BackupTeacher(val personId: String, val name: String, val roles: List<String> = emptyList(), val languages: List<String> = emptyList()) {
    fun toModel() = CourseTeacher(personId, name, roles, languages)
}
@Serializable
data class BackupTeacherProfile(
    val personId: String, val language: String, val name: String,
    val position: String? = null, val universityEmail: String? = null, val systemEmail: String? = null,
    val officeAddress: String? = null, val officeNumber: String? = null, val officePhone: String? = null,
    val consultationHours: String? = null, val mailboxLastChecked: String? = null,
) {
    fun toModel() = TeacherProfile(personId, name, position, universityEmail, systemEmail, officeAddress,
        officeNumber, officePhone, consultationHours, mailboxLastChecked)
}
internal fun teachingCode(code: String) = code.trim().uppercase(Locale.ROOT)
internal fun CourseTeacher.toBackup() = BackupTeacher(personId, name, roles, languages)
internal fun TeacherProfile.toBackup(language: AppLanguage) = BackupTeacherProfile(personId, language.name, name,
    position, universityEmail, systemEmail, officeAddress, officeNumber, officePhone, consultationHours, mailboxLastChecked)

/** Atomic, bounded local snapshot shared by UIS and backup import/export. No Room schema migration. */
class TeachingStore(private val file: File? = null) {
    private val json = teachingJson
    private val mutable = MutableStateFlow(runCatching {
        file?.takeIf { it.isFile && it.length() <= 4_000_000 }?.readText()?.let {
            json.decodeFromString<BackupTeaching>(it).validated()
        }
    }.getOrNull() ?: BackupTeaching())
    val snapshot = mutable.asStateFlow()
    private var epoch = 0L
    @Synchronized fun generation(): Long = epoch
    @Synchronized fun replace(value: BackupTeaching) { write(value.validated()); epoch++ }
    @Synchronized internal fun update(expected: Long, transform: (BackupTeaching) -> BackupTeaching) {
        check(epoch == expected) { "Teaching data changed while loading contacts" }
        write(transform(mutable.value).validated())
    }
    private fun write(value: BackupTeaching) {
        if (value == mutable.value) return
        file?.let { target ->
            target.parentFile?.mkdirs()
            val pending = File(target.path + ".pending")
            try {
                FileOutputStream(pending).use { output -> output.write(json.encodeToString(value).toByteArray(Charsets.UTF_8)); output.fd.sync() }
                check(pending.renameTo(target)) { "Could not save teaching data" }
            } finally { pending.delete() }
        }
        mutable.value = value
    }
}
