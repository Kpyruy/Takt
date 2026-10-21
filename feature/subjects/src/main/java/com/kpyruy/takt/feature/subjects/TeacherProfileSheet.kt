package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.model.UiKey
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.CourseTeachingRepository
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherProfileSheet(
    teacher: CourseTeacher,
    repository: CourseTeachingRepository,
    language: AppLanguage,
    authenticate: (String, (DeviceAuthenticationResult) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSettings: () -> Unit = {},
) {
    var profile by remember(teacher.personId, language) { mutableStateOf(repository.savedProfile(teacher, language)) }
    var error by remember(teacher.personId, language) { mutableStateOf<Throwable?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var mailFailure by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(teacher.personId, language, repository) {
        repository.changes.collect { profile = repository.savedProfile(teacher, language) }
    }
    LaunchedEffect(teacher.personId, language, reload) {
        loading = true; error = null
        try { profile = repository.profile(teacher, language, refresh = true) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure }
        finally { loading = false }
    }
    val portrait by produceState<Bitmap?>(null, teacher.personId, profile != null) {
        if (profile == null) return@produceState
        value = try {
            val bytes = repository.photo(teacher.personId)
            if (bytes == null) null else withContext(Dispatchers.Default) { decodeTeacherPortrait(bytes) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
    }
    fun composeMail(address: String) {
        mailFailure = runCatching { context.startActivity(teacherEmailIntent(address, teacher.personId)) }.isFailure
    }
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().testTag("teacher-profile").verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t(UiKey.TEXT_LECTURER_PROFILE), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TaktIconButton(Icons.Outlined.Close, t(UiKey.ACTION_CLOSE), onDismiss)
            }
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val image = portrait
                        if (image != null) Image(image.asImageBitmap(), teacher.name,
                            Modifier.size(width = 80.dp, height = 96.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
                        else TeacherAvatar(teacher.name, Modifier.size(80.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(profile?.name ?: teacher.name, style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text(t(UiKey.BRAND_STU), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        teacher.roles.forEach { role ->
                            GlassSurface(shape = RoundedCornerShape(999.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
                                Text(teacherRoleLabel(role), Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    profile?.position?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { TeacherLoadProblem(it, authenticate, { reload++ }, onSettings) }
            profile?.let { person ->
                SectionCard {
                    Text(t(UiKey.TEXT_CONTACTS), style = MaterialTheme.typography.titleMedium)
                    person.universityEmail?.let { address ->
                        TeacherEmailRow(t(UiKey.TEXT_UNIVERSITY_EMAIL), address, "teacher-email-university") { composeMail(address) }
                    }
                    person.systemEmail?.let { address ->
                        TeacherEmailRow(t(UiKey.TEXT_UIS_EMAIL), address, "teacher-email-uis") { composeMail(address) }
                    }
                    if (person.universityEmail == null && person.systemEmail == null) Text(t(UiKey.TEXT_NO_EMAIL_ADDRESS_IS_LISTED_IN_UIS),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    person.officePhone?.let { TeacherDetail(t(UiKey.TEXT_OFFICE_PHONE), it) }
                    if (mailFailure) Text(t(UiKey.TEXT_NO_EMAIL_APP_WAS_FOUND), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.MeetingRoom, null, tint = MaterialTheme.colorScheme.primary)
                        Text(t(UiKey.TEXT_OFFICE_AND_CONSULTATIONS), style = MaterialTheme.typography.titleMedium)
                    }
                    TeacherDetail(t(UiKey.TEXT_OFFICE), person.officeNumber ?: t(UiKey.TEXT_NOT_LISTED_IN_UIS), "teacher-office")
                    person.officeAddress?.let { TeacherDetail(t(UiKey.TEXT_ADDRESS), it) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TeacherDetail(t(UiKey.TEXT_CONSULTATION_HOURS), person.consultationHours ?: t(UiKey.TEXT_ITEMS_NOT_LISTED_IN_UIS), "teacher-hours")
                }
                person.mailboxLastChecked?.let { timestamp ->
                    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.MarkEmailRead, null, tint = MaterialTheme.colorScheme.primary)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(t(UiKey.TEXT_UNIVERSITY_MAILBOX_LAST_CHECKED), style = MaterialTheme.typography.labelMedium)
                                Text(formatMailboxTimestamp(timestamp), style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.testTag("teacher-mailbox-checked"))
                                Text(t(UiKey.TEXT_AS_REPORTED_BY_UIS), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { runCatching { uriHandler.openUri(teacher.profileUrl) } }, Modifier.weight(1f)) {
                    Text(t(UiKey.TEXT_PROFILE_IN_UIS))
                }
                TextButton(onClick = { if (!loading) reload++ }, enabled = !loading) { Text(t(UiKey.ACTION_UPDATE)) }
            }
        }
    }
}

fun teacherEmailIntent(address: String, personId: String? = null): Intent {
    if (address.substringAfterLast('@').equals("is.stuba.sk", ignoreCase = true) && personId != null) {
        require(personId.matches(Regex("[1-9][0-9]{0,11}")))
        return Intent(Intent.ACTION_VIEW, Uri.parse("https://is.stuba.sk/auth/posta/nova_zprava.pl?uzivatel=$personId"))
    }
    return Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", address, null))
}

@Composable
private fun TeacherEmailRow(label: String, address: String, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.AlternateEmail, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(address, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
        Icon(Icons.Outlined.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TeacherDetail(label: String, value: String, tag: String? = null) {
    Column(Modifier.fillMaxWidth().then(tag?.let { Modifier.testTag(it) } ?: Modifier).padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun decodeTeacherPortrait(bytes: ByteArray): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth !in 1..8192 || options.outHeight !in 1..8192) return null
    var sample = 1
    while (options.outWidth / sample > 512 || options.outHeight / sample > 512) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
}

private fun formatMailboxTimestamp(value: String): String {
    val formats = listOf("M/d/yyyy HH:mm", "d.M.yyyy HH:mm", "d. M. yyyy HH:mm")
    val parsed = formats.firstNotNullOfOrNull { pattern -> runCatching {
        LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern))
    }.getOrNull() }
    return parsed?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) ?: value
}
