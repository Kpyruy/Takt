package com.kpyruy.takt.core.data
import org.junit.Assert.*
import org.junit.Test
class BackupContentCompatibilityTest {
 @Test fun addingPhotoMetadataWithoutChangingAnyPixelsCannotCreateASyncConflict() {
  val old=BackupPayload()
  val current=old.copy(settings=old.settings.copy(wallpaperRecorded=true))
  assertTrue(sameDocumentBackupData(BackupPayloadCodec.encode(old),BackupPayloadCodec.encode(current)))
 }
 @Test fun imageLabelsDoNotCreateConflictsButDifferentPixelsDo() {
  val image=BackupWallpaper.fromBytes(byteArrayOf(0xFF.toByte(),0xD8.toByte(),0xFF.toByte(),0))
  val a=BackupPayload(settings=BackupSettings(wallpaperRecorded=true,wallpaper=image))
  val b=a.copy(settings=a.settings.copy(wallpaper=image.copy(name="Restored photo")))
  assertTrue(sameDocumentBackupData(BackupPayloadCodec.encode(a),BackupPayloadCodec.encode(b)))
  val c=a.copy(settings=a.settings.copy(wallpaper=BackupWallpaper.fromBytes(byteArrayOf(0xFF.toByte(),0xD8.toByte(),0xFF.toByte(),1))))
  assertFalse(sameDocumentBackupData(BackupPayloadCodec.encode(a),BackupPayloadCodec.encode(c)))
 }
}
