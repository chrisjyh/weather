package com.example.weather.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoorCapabilityTest {
    @Test
    fun controlRequiresWriteAccessAndZeroToOneRange() {
        assertTrue(DoorCapability("READ_WRITE", 0, 1).canControl)
        assertFalse(DoorCapability("READ", 0, 1).canControl)
        assertFalse(DoorCapability("READ_WRITE", null, null).canControl)
    }
}
