package com.fitplan.app.data.repository

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRepositoryTest {
    @Test
    fun fitPlanBackup_serializesVersionAndExportTime() {
        val json = Gson().toJson(FitPlanBackup(exportedAt = 1760000000000))

        assertTrue(json.contains("\"version\":1"))
        assertTrue(json.contains("\"exportedAt\":1760000000000"))

        val parsed = Gson().fromJson(json, FitPlanBackup::class.java)
        assertEquals(1, parsed.version)
        assertEquals(1760000000000, parsed.exportedAt)
    }
}
