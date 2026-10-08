package com.fjuul.sdk.activitysources.entities;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.net.Uri;
import android.os.Build;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class ExternalAuthenticationFlowHandlerTest {
    @Test
    public void errorCodeReachesAppFacingStatus() {
        String[][] fixtures =
            {{"", null}, {"&errorCode=oauth_cancelled", ExternalAuthenticationFlowHandler.ErrorCode.OAUTH_CANCELLED},
                    {"&errorCode=google_health_account_not_linked",
                            ExternalAuthenticationFlowHandler.ErrorCode.GOOGLE_HEALTH_ACCOUNT_NOT_LINKED},
                    {"&errorCode=future%5Fcode", "future_code"}, {"&errorCode=", null}, {"&errorCode", null},
                    {"&unrelated=value&error=private_provider_message", null}};
        for (String[] fixture : fixtures) {
            for (boolean success : new boolean[] {true, false}) {
                deliverBrowserCallback(
                    Uri.parse("fjuulsdk://external_connect?service=googlehealth&success=" + success + fixture[0]),
                    status -> {
                        assertNotNull(status);
                        assertEquals("googlehealth", status.getService());
                        assertEquals(success, status.isSuccess());
                        assertEquals(success ? null : fixture[1], status.getErrorCode());
                    });
            }
        }
    }

    @Test
    public void existingConstructorAndGettersRemainAvailable() throws Exception {
        ExternalAuthenticationFlowHandler.ConnectionStatus status =
            new ExternalAuthenticationFlowHandler.ConnectionStatus("googlehealth", false);
        assertEquals("googlehealth", status.getService());
        assertFalse(status.isSuccess());
        assertNull(status.getErrorCode());
        assertNotNull(ExternalAuthenticationFlowHandler.ConnectionStatus.class.getDeclaredConstructor(String.class,
            boolean.class));
    }

    @Test
    public void existingRoutingAndValidationRemainUnchanged() {
        assertNull(ExternalAuthenticationFlowHandler
            .handle(Uri.parse("fjuulsdk://other?service=googlehealth&success=false&errorCode=oauth_cancelled")));
        assertNull(ExternalAuthenticationFlowHandler
            .handle(Uri.parse("fjuulsdk://external_connect?service=googlehealth&errorCode=oauth_cancelled")));
        assertNull(ExternalAuthenticationFlowHandler
            .handle(Uri.parse("fjuulsdk://external_connect?success=false&errorCode=oauth_cancelled")));
    }

    @Test
    public void localBrowserDismissalDoesNotReportProviderCancellation() {
        List<ExternalAuthenticationFlowHandler.ConnectionStatus> statuses = new ArrayList<>();
        deliverBrowserCallback(null, statuses::add);
        assertTrue(statuses.isEmpty());

        deliverBrowserCallback(
            Uri.parse("fjuulsdk://external_connect?service=googlehealth&success=false&errorCode=oauth_cancelled"),
            statuses::add);
        assertEquals(1, statuses.size());
        assertFalse(statuses.get(0).isSuccess());
        assertEquals(ExternalAuthenticationFlowHandler.ErrorCode.OAUTH_CANCELLED, statuses.get(0).getErrorCode());
    }

    private void deliverBrowserCallback(Uri uri,
        Consumer<ExternalAuthenticationFlowHandler.ConnectionStatus> completion) {
        if (uri != null) {
            completion.accept(ExternalAuthenticationFlowHandler.handle(uri));
        }
    }

    @Test
    public void additionalQueryParametersPreserveLegacyStatus() {
        for (String suffix : new String[] {"errorCode=oauth_cancelled", "errorCode=google_health_account_not_linked",
                "errorCode=future_code", "errorCode=", "unrelated=value"}) {
            for (boolean success : new boolean[] {true, false}) {
                ExternalAuthenticationFlowHandler.ConnectionStatus status = ExternalAuthenticationFlowHandler.handle(
                    Uri.parse("fjuulsdk://external_connect?service=googlehealth&success=" + success + "&" + suffix));
                assertNotNull(status);
                assertEquals(success, status.isSuccess());
                assertEquals("googlehealth", status.getService());
            }
        }
    }
}
