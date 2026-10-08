package com.fjuul.sdk.activitysources.entities

import android.net.Uri
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ExternalAuthenticationFlowHandlerKotlinTest {
    @Test
    fun existingKotlinUsageAndNewPropertyCompile() {
        val handle: (Uri) -> ExternalAuthenticationFlowHandler.ConnectionStatus? =
            ExternalAuthenticationFlowHandler::handle
        val status = handle(Uri.parse("fjuulsdk://external_connect?service=googlehealth&success=false"))!!
        assertEquals("googlehealth", status.service)
        assertFalse(status.isSuccess)
        assertNull(status.errorCode)
        assertNotNull(handle(Uri.parse("fjuulsdk://external_connect?service=googlehealth&success=true")))

        val classified = handle(Uri.parse("fjuulsdk://external_connect?service=googlehealth&success=false&errorCode=google_health_account_not_linked"))!!
        assertFalse(classified.isSuccess)
        assertEquals(ExternalAuthenticationFlowHandler.ErrorCode.GOOGLE_HEALTH_ACCOUNT_NOT_LINKED, classified.errorCode)
    }
}
