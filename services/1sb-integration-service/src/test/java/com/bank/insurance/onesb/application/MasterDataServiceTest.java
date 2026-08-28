package com.bank.insurance.onesb.application;

import com.bank.insurance.onesb.TestErrors;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.model.LookupValue;
import com.bank.insurance.onesb.domain.port.outbound.OneSbMasterDataPort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbMasterDataPort.MasterLookupResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@Tag("FUNC-001")
@ExtendWith(MockitoExtension.class)
class MasterDataServiceTest {

    @Mock
    private OneSbMasterDataPort port;

    private final AtomicReference<Instant> now =
            new AtomicReference<>(Instant.parse("2026-07-30T12:00:00Z"));

    private MasterDataService service;

    @BeforeEach
    void setUp() {
        Clock clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };
        service = new MasterDataService(port, new MastersCacheProperties(4L), clock, TestErrors.ONESB);
    }

    @Test
    @DisplayName("AC-2 precursor: cache miss calls port and stores result")
    void cacheMiss_callsPortAndReturnsMiss() {
        when(port.lookup(eq("TERM"), eq("quote"), anyList(), isNull()))
                .thenReturn(new MasterLookupResult(Map.of(
                        "GENDER", List.of(LookupValue.of("M"), LookupValue.of("F")))));

        MasterLookupOutcome outcome = service.lookup("TERM", "quote", List.of("GENDER"), null);

        assertThat(outcome.cacheHit()).isFalse();
        assertThat(outcome.stale()).isFalse();
        assertThat(outcome.cacheHeader()).isEqualTo(MasterLookupOutcome.HEADER_MISS);
        assertThat(outcome.lookups().get("GENDER")).extracting(LookupValue::code)
                .containsExactly("M", "F");
        verify(port, times(1)).lookup("TERM", "quote", List.of("GENDER"), null);
    }

    @Test
    @DisplayName("AC-2: cache hit within TTL does not call port")
    void cacheHitWithinTtl_doesNotCallPort() {
        when(port.lookup(anyString(), anyString(), anyList(), any()))
                .thenReturn(new MasterLookupResult(Map.of(
                        "OCC", List.of(LookupValue.of("SALARIED")))));

        service.lookup("TERM", "quote", List.of("OCC"), null);
        MasterLookupOutcome second = service.lookup("TERM", "quote", List.of("OCC"), null);

        assertThat(second.cacheHit()).isTrue();
        assertThat(second.stale()).isFalse();
        assertThat(second.cacheHeader()).isEqualTo(MasterLookupOutcome.HEADER_HIT);
        verify(port, times(1)).lookup(anyString(), anyString(), anyList(), any());
    }

    @Test
    @DisplayName("AC-3: port failure with stale cache returns stale=true")
    void portFailure_withStaleCache_returnsStale() {
        when(port.lookup(anyString(), anyString(), anyList(), any()))
                .thenReturn(new MasterLookupResult(Map.of(
                        "TITLE", List.of(LookupValue.of("MR")))))
                .thenThrow(ServiceException.of(ErrorCodes.UPSTREAM_UNAVAILABLE).reason("1SB down").build());

        service.lookup("TERM", "quote", List.of("TITLE"), null);
        now.set(now.get().plusSeconds(10)); // expire TTL (4s)

        MasterLookupOutcome stale = service.lookup("TERM", "quote", List.of("TITLE"), null);

        assertThat(stale.stale()).isTrue();
        assertThat(stale.cacheHit()).isFalse();
        assertThat(stale.cacheHeader()).isEqualTo(MasterLookupOutcome.HEADER_STALE);
        assertThat(stale.lookups().get("TITLE")).extracting(LookupValue::code).containsExactly("MR");
        verify(port, times(2)).lookup(anyString(), anyString(), anyList(), any());
    }

    @Test
    @DisplayName("AC-3: port failure with no cache throws UPSTREAM_UNAVAILABLE 503")
    void portFailure_noCache_throws503() {
        when(port.lookup(anyString(), anyString(), anyList(), any()))
                .thenThrow(ServiceException.of(ErrorCodes.UPSTREAM_UNAVAILABLE).reason("1SB down").build());

        assertThatThrownBy(() -> service.lookup("TERM", "quote", List.of("GENDER"), null))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getHttpStatus()).isEqualTo(503);
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE);
                });
    }

    @Test
    void cacheKey_sortsEntityIds_andIgnoresManufacturerNull() {
        String a = MasterDataService.cacheKey("TERM", "quote", null, List.of("OCC", "GENDER"));
        String b = MasterDataService.cacheKey("TERM", "quote", null, List.of("GENDER", "OCC"));
        assertThat(a).isEqualTo(b);
        assertThat(a).isEqualTo("TERM|quote||GENDER,OCC");
    }

    @Test
    void afterTtlExpiry_withoutFailure_refreshesViaPort() {
        when(port.lookup(anyString(), anyString(), anyList(), any()))
                .thenReturn(new MasterLookupResult(Map.of("GENDER", List.of(LookupValue.of("M")))))
                .thenReturn(new MasterLookupResult(Map.of("GENDER", List.of(LookupValue.of("F")))));

        service.lookup("TERM", "quote", List.of("GENDER"), null);
        now.set(now.get().plusSeconds(5));
        MasterLookupOutcome refreshed = service.lookup("TERM", "quote", List.of("GENDER"), null);

        assertThat(refreshed.cacheHit()).isFalse();
        assertThat(refreshed.lookups().get("GENDER")).extracting(LookupValue::code).containsExactly("F");
        verify(port, times(2)).lookup(anyString(), anyString(), anyList(), any());
        verify(port, never()).lookup(eq("HEALTH"), anyString(), anyList(), any());
    }
}
