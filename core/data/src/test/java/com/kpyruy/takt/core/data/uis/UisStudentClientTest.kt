package com.kpyruy.takt.core.data.uis

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class UisStudentClientTest {
    private val auth="""<a href="/auth/system/logout.pl">Logout</a>"""
    private fun plan(id:String,period:String)="""<form name=formular><input name=obdobi value=$period><select name=studium><option value=$id selected>Study [term 1, year 2]</option></select></form><table id=tmtab_1><tr><td colspan=6>3rd semester</td></tr><tr><td><a href="/auth/katalog/syllabus.pl?predmet=500">PHYS</a></td><td>Physics</td><td>exam</td><td>5</td><td>1x</td><td>FULFILLED</td></tr></table>"""
    @Test fun overviewCannotReplaceAnEstablishedLiveImportStudy() {
        MockWebServer().use { server -> server.start()
            val client=UisClient(server.url("/"))
            fun enqueue(html:String){server.enqueue(MockResponse().setBody(auth+html))}
            server.enqueue(MockResponse().setBody("""<form method=post action=/system/login.pl><input name=credential_0><input name=credential_1 type=password></form>"""))
            server.enqueue(MockResponse().setBody("""{"need2FA":"false"}"""))
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location","/auth/").addHeader("Set-Cookie","UISAuth=fake; Path=/"))
            server.enqueue(MockResponse().setBody(auth))
            assertEquals(UisResult.CONNECTED,client.login("fixture","fixture"))
            enqueue(plan("101","741"));enqueue(plan("101","741"))
            assertEquals("101",client.readStudyPlan().studyId)
            enqueue("""<table><tr><th>Choose</th><th>Faculty</th><th>Study</th><th>Progress of study</th></tr>
                <tr><td><span aria-label=Active></span></td><td>FEI</td><td>Other</td><td>enrolled<a href="studium.pl?studium=303;obdobi=700">Details</a></td></tr>
                <tr><td><a href="moje_studium.pl?studium=101">Choose</a></td><td>MTF</td><td>Current</td><td>enrolled<a href="studium.pl?studium=101;obdobi=741">Details</a></td></tr></table>""")
            for(title in listOf("Other","Current"))enqueue("""<table><tr><td>Programme:</td><td>$title</td></tr><tr><td>Beginning of study:</td><td>09/01/2025</td></tr></table>""")
            val overview=client.readStudentHubOverview()
            assertEquals("101",overview.currentStudyId)
            enqueue(plan("101","741"));enqueue(plan("101","741"))
            client.readStudyPlan()
            val requests=(0 until server.requestCount).map{server.takeRequest()}
            assertTrue(requests.takeLast(2).all { it.path!!.contains("studium=101;obdobi=741") })
            client.close()
        }
    }
    @Test fun scholarshipFormKeepsRepeatedFieldsAndArchiveReadCannotChangePrimaryImport() {
        MockWebServer().use { server -> server.start()
            val client=UisClient(server.url("/"))
            fun enqueue(html:String){server.enqueue(MockResponse().setBody(auth+html))}
            server.enqueue(MockResponse().setBody("""<form method=post action=/system/login.pl><input name=credential_0><input name=credential_1 type=password></form>"""))
            server.enqueue(MockResponse().setBody("""{"need2FA":"false"}"""))
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location","/auth/").addHeader("Set-Cookie","UISAuth=fake; Path=/"))
            server.enqueue(MockResponse().setBody(auth))
            assertEquals(UisResult.CONNECTED,client.login("fixture","fixture"))
            enqueue("""<div id=log>Logged in: Alex Novak</div><table><tr><th>Choose</th><th>Faculty</th><th>Study</th><th>Progress of study</th></tr><tr><td>Active</td><td>MTF</td><td>Current<br>full-time 3</td><td>enrolled<a href="studium.pl?studium=101;obdobi=741">Details</a></td></tr><tr><td><a href="moje_studium.pl?studium=202">Choose</a></td><td>MTF</td><td>Old</td><td>ended<a href="studium.pl?studium=202;obdobi=722">Details</a></td></tr></table>""")
            for(title in listOf("Current","Old"))enqueue("""<table><tr><td>Programme:</td><td>$title</td></tr><tr><td>Beginning of study:</td><td>09/01/2025</td></tr></table>""")
            val overview=client.readStudentHubOverview();assertEquals("101",overview.currentStudyId)
            enqueue(plan("202","722"))
            enqueue(plan("202","722").replace("Physics","Fyzika"))
            enqueue("""<table><tr><th>Code</th><th>Course name</th><th>Result</th><th>Entered</th></tr><tr><td>PHYS</td><td>Physics</td><td>(A)</td><td></td></tr></table>""")
            enqueue("""<table><tr><th>Status</th><th>Type</th><th>Sum</th><th>Settled</th></tr><tr><td><span aria-label=paid></span></td><td>ISIC</td><td>€ 15</td><td>€ 15</td></tr></table>""")
            enqueue("""<form action=/auth/student/vypl_stip_student.pl><input name=obdobi type=hidden value=722><select name=studium><option value=202 selected>Old</option></select><select name=od_mesic><option value=1>January</option></select><select name=od_rok><option value=2025>2025</option></select><select name=do_mesic><option value=12>December</option></select><select name=do_rok><option value=2026>2026</option></select><input name=zobraz type=submit value=Display><input name=obdobi type=hidden value=741><input name=studium type=hidden value=202></form>""")
            enqueue(javaClass.getResource("/students/scholarships-paid.html")!!.readText())
            val archived=client.readStudentStudy(overview.studies.last())
            assertEquals("uis:202:500",archived.courses.single().id);assertEquals(2,archived.scholarships.size)
            assertEquals("Fyzika",archived.courses.single().titleSk)
            enqueue(plan("101","741"));enqueue(plan("101","741"))
            assertEquals("101",client.readStudyPlan().studyId)
            val requests=(0 until server.requestCount).map{server.takeRequest()}
            val post=requests.single{it.method=="POST"&&it.path!!.contains("vypl_stip_student.pl")}
            val body=post.body.readUtf8()
            assertEquals(2,Regex("obdobi=").findAll(body).count());assertEquals(2,Regex("studium=202").findAll(body).count())
            assertTrue(body.contains("od_rok=2025"));assertTrue(body.contains("do_rok=2026"))
            assertTrue(requests.takeLast(2).all { it.path!!.contains("studium=101;obdobi=741") })
            assertTrue(post.getHeader("Cookie")!!.contains("UISAuth=fake"))
            client.close()
        }
    }
}
