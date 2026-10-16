package com.kpyruy.takt.core.model
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
class MultiCourseWorkFilterTest {
 @Test fun selectedCoursesMatchAcrossWorkAndAllResetsTheSpecificSelection() {
  val today=LocalDate.of(2026,10,16)
  val tasks=listOf("a","b","c").map { StudyTask(it,it,it,null,today,false) }
  val filter=HomeWorkFilter(courseIds=setOf("a","b"))
  assertEquals(setOf("a","b"),HomeWorkPlanner.visible(tasks,emptyList(),today,filter).map { it.courseId }.toSet())
  assertEquals(3,HomeWorkPlanner.visible(tasks,emptyList(),today,filter.copy(courseIds=emptySet(),courseId=null)).size)
  assertEquals(setOf("a"),HomeWorkFilter(courseId="a").selectedCourseIds)
 }
}
