package com.kpyruy.takt.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.kpyruy.takt.core.model.normalizeWallpaperReference
import java.io.File
import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Copies picker bytes immediately, validates before decode, and publishes an immutable bounded JPEG. */
class WallpaperStore(context: Context) {
    private val app = context.applicationContext
    private val directory get() = File(app.filesDir, "wallpapers").also { check(it.isDirectory || it.mkdirs()) }

    suspend fun importImage(uri: Uri, keepReference: String?): String = imports.withLock { withContext(Dispatchers.IO) {
        val directory = directory
        val temporary = File.createTempFile("import-", ".tmp", directory)
        var decoded: Bitmap? = null
        var oriented: Bitmap? = null
        var bounded: Bitmap? = null
        var published: File? = null
        try {
            val source = requireNotNull(app.contentResolver.openInputStream(uri))
            source.use { input -> temporary.outputStream().use { output ->
                val buffer = ByteArray(32 * 1024)
                var total = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= WallpaperImageLimits.MAX_BYTES) { "Image exceeds import limit" }
                    output.write(buffer, 0, count)
                }
            } }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporary.path, options)
            WallpaperImageLimits.outputSize(options.outWidth, options.outHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inMutable = true
            options.inSampleSize = 1
            while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > WallpaperImageLimits.MAX_EDGE) {
                options.inSampleSize *= 2
            }
            val image = requireNotNull(BitmapFactory.decodeFile(temporary.path, options))
            decoded = image
            val orientation = runCatching { ExifInterface(temporary.path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
                }
            }
            val rotated = if (matrix.isIdentity) image else Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
            oriented = rotated
            val (width, height) = WallpaperImageLimits.outputSize(rotated.width, rotated.height)
            val result = if (width == rotated.width && height == rotated.height) rotated
                else Bitmap.createScaledBitmap(rotated, width, height, true)
            bounded = result
            // Flatten transparency in place; do not allocate a third full-size pixel buffer.
            android.graphics.Canvas(result).drawColor(android.graphics.Color.rgb(96, 104, 112),
                android.graphics.PorterDuff.Mode.DST_OVER)
            result.setHasAlpha(false)
            temporary.outputStream().use { output ->
                check(result.compress(Bitmap.CompressFormat.JPEG, 90, output))
                output.fd.sync()
            }
            val reference = "wallpaper-${UUID.randomUUID()}.jpg"
            val destination = File(directory, reference)
            check(temporary.renameTo(destination))
            published = destination
            // One applied image plus one draft, even after repeated selections or abandoned drafts.
            directory.listFiles()?.filter { it.name != reference && it.name != normalizeWallpaperReference(keepReference) && it.name !in restoreReferences }
                ?.forEach { it.delete() }
            reference
        } catch (failure: Throwable) {
            published?.delete()
            throw failure
        } finally {
            temporary.delete()
            if (bounded !== oriented) bounded?.recycle()
            if (oriented !== decoded) oriented?.recycle()
            decoded?.recycle()
        }
    } }

    suspend fun exportWallpaper(reference: String?): BackupWallpaper? = withContext(Dispatchers.IO) {
        val safe = normalizeWallpaperReference(reference) ?: return@withContext null
        val file = File(directory, safe)
        if (!file.isFile) return@withContext null
        BackupWallpaper.fromBytes(file.readBytes())
    }

    /** Restore already-normalized owned pixels byte-for-byte, without repeated JPEG degradation. */
    suspend fun restoreWallpaper(photo: BackupWallpaper, keepReference: String?, preserveExisting: Boolean = false): String = imports.withLock {
        withContext(Dispatchers.IO) {
            val bytes = photo.decodedBytes()
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            require(options.outWidth in 1..2048 && options.outHeight in 1..2048) { "Invalid wallpaper dimensions" }
            requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)).recycle()
            val temporary = File.createTempFile("restore-", ".tmp", directory)
            var published: File? = null
            try {
                temporary.outputStream().use { it.write(bytes); it.fd.sync() }
                currentCoroutineContext().ensureActive()
                val reference = "wallpaper-${UUID.randomUUID()}.jpg"
                val destination = File(directory, reference)
                check(temporary.renameTo(destination))
                published = destination
                if (preserveExisting) {
                    syncDirectory(directory)
                    restoreReferences.add(reference)
                    normalizeWallpaperReference(keepReference)?.let(restoreReferences::add)
                }
                if (!preserveExisting) directory.listFiles()?.filter { it.name != reference && it.name != normalizeWallpaperReference(keepReference) && it.name !in restoreReferences }
                    ?.forEach { it.delete() }
                reference
            } catch (failure: Throwable) {
                published?.delete()
                throw failure
            } finally { temporary.delete() }
        }
    }

    /** Deletes only the immutable image staged by the failed restore, never the active/previous photo. */
    suspend fun discardPrepared(reference: String?) = imports.withLock { withContext(Dispatchers.IO) {
        val safe = normalizeWallpaperReference(reference) ?: return@withContext
        restoreReferences.remove(safe)
        val file = File(directory, safe)
        if (file.exists()) { check(file.delete()) { "Could not remove staged wallpaper" }; syncDirectory(directory) }
    } }

    /** Run only after a durable commit/rollback decision. Keep one applied image and one draft. */
    suspend fun finishRestore(active: String?, previous: String?, prepared: String?) = imports.withLock {
        withContext(Dispatchers.IO) {
            previous?.let(restoreReferences::remove)
            prepared?.let(restoreReferences::remove)
            val keep = normalizeWallpaperReference(active)
            val candidates = directory.listFiles().orEmpty().filter {
                it.name.startsWith("wallpaper-") && it.name.endsWith(".jpg") &&
                    it.name != keep && it.name !in restoreReferences
            }.sortedByDescending { it.lastModified() }
            candidates.drop(1).forEach { it.delete() }
        }
    }

    private companion object { val imports = Mutex(); val restoreReferences = mutableSetOf<String>() }
}
