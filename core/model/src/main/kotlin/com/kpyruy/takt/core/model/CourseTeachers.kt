package com.kpyruy.takt.core.model

data class CourseTeacher(
    val personId: String,
    val name: String,
    val roles: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
) {
    val profileUrl: String get() = "https://is.stuba.sk/auth/lide/clovek.pl?id=$personId"
}

data class TeacherProfile(
    val personId: String,
    val name: String,
    val position: String? = null,
    val universityEmail: String? = null,
    val systemEmail: String? = null,
    val officeAddress: String? = null,
    val officeNumber: String? = null,
    val officePhone: String? = null,
    val consultationHours: String? = null,
    val mailboxLastChecked: String? = null,
)
