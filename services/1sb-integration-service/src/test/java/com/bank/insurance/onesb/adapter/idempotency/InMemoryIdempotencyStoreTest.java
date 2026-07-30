package com.bank.insurance.onesb.adapter.idempotency;

import com.bank.insurance.onesb.domain.port.outbound.IdempotencyPort.CachedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@Tag("QA-006")
class InMemoryIdempotencyStoreTest {

    private InMemoryIdempotencyStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryIdempotencyStore();
    }

    @Test
    void storeThenFind_returnsCachedResponseWithBodyHash() {
        CachedResponse cached = new CachedResponse(
                200,
                "application/json",
                "{\"ok\":true}".getBytes(StandardCharsets.UTF_8),
                "hash-abc"
        );

        store.store("key-1", "hash-abc", cached);

        Optional<CachedResponse> found = store.find("key-1");

        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(200);
        assertThat(found.get().contentType()).isEqualTo("application/json");
        assertThat(found.get().bodyHash()).isEqualTo("hash-abc");
        assertThat(new String(found.get().body(), StandardCharsets.UTF_8)).isEqualTo("{\"ok\":true}");
    }

    @Test
    void find_unknownKey_returnsEmpty() {
        assertThat(store.find("missing-key")).isEmpty();
    }

    @Test
    void store_sameKeyTwice_firstWriterWins() {
        CachedResponse first = new CachedResponse(200, "application/json", "first".getBytes(StandardCharsets.UTF_8), "hash-1");
        CachedResponse second = new CachedResponse(201, "text/plain", "second".getBytes(StandardCharsets.UTF_8), "hash-2");

        store.store("key-dup", "hash-1", first);
        store.store("key-dup", "hash-2", second);

        Optional<CachedResponse> found = store.find("key-dup");

        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(200);
        assertThat(found.get().bodyHash()).isEqualTo("hash-1");
        assertThat(new String(found.get().body(), StandardCharsets.UTF_8)).isEqualTo("first");
    }

    @Test
    void find_afterClear_returnsEmpty() {
        store.store("key-x", "h", new CachedResponse(200, "application/json", "{}".getBytes(StandardCharsets.UTF_8), "h"));
        store.clear();

        assertThat(store.find("key-x")).isEmpty();
    }
}
