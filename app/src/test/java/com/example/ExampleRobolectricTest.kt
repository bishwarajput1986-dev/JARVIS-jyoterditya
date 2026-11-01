package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.JarvisPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals("J.A.R.V.I.S.", appName)
    }

    @Test
    fun `verify default preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = JarvisPreferences(context)
        assertEquals("Sir Jyoterditya", prefs.userTitle.value)
        assertEquals("Jyoterditya Singh", prefs.userName.value)
        assertEquals("Karate and Coding", prefs.userHobbies.value)
        assertEquals("J.A.R.V.I.S.", prefs.assistantName.value)
        assertTrue(prefs.autoSpeak.value)
        assertTrue(prefs.darkTheme.value)
    }
}
