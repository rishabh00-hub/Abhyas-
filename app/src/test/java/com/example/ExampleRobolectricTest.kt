package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Abhyas Study Planner", appName)
  }

  @Test
  fun `database entry operations test`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val dao = db.targetDao()

    val target = DailyTarget(
      id = "test-123",
      title = "Test Electrostatics",
      subject = "Physics",
      type = "Lecture",
      batch = "Lakshya",
      status = "not-started",
      durationProposed = 90,
      durationLogged = 0,
      createdAt = "2026-05-23T11:00:00Z",
      targetDate = "2026-05-23"
    )
    dao.insertTarget(target)

    val list = dao.getAllTargets().first()
    assertEquals(1, list.size)
    assertEquals("test-123", list[0].id)
    assertEquals("Test Electrostatics", list[0].title)

    db.close()
  }
}
