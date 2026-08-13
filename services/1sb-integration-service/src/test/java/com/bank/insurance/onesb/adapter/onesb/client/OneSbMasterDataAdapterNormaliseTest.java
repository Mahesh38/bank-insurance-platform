package com.bank.insurance.onesb.adapter.onesb.client;

import com.bank.insurance.onesb.domain.model.LookupValue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA-001 — shape coverage for {@code OneSbMasterDataAdapter.normalise}.
 * <p>
 * 1SB returns master-data enums in several shapes across LOBs (flat string arrays, arrays of
 * objects with varying key names, a single object, a scalar, or nested under {@code data}).
 * {@link OneSbMasterDataAdapterTest} covers the wire path for the common shapes; this class
 * exercises the normalisation branches directly so an unusual upstream shape is a known
 * outcome rather than a surprise.
 */
@Tag("unit")
@Tag("FUNC-001")
class OneSbMasterDataAdapterNormaliseTest {

    private static final List<String> ENTITY_IDS = List.of("gender");

    private static Map<String, List<LookupValue>> normaliseGender(Object value) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("gender", value);
        return OneSbMasterDataAdapter.normalise(raw, ENTITY_IDS);
    }

    @Test
    void nullBody_yieldsEmptyListPerRequestedEntity() {
        Map<String, List<LookupValue>> out =
                OneSbMasterDataAdapter.normalise(null, List.of("gender", "maritalStatus"));

        assertThat(out).containsOnlyKeys("gender", "maritalStatus");
        assertThat(out.get("gender")).isEmpty();
        assertThat(out.get("maritalStatus")).isEmpty();
    }

    @Test
    void entityMissingAtRoot_isResolvedFromDataSection() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gender", List.of("M", "F"));
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("data", data);

        Map<String, List<LookupValue>> out = OneSbMasterDataAdapter.normalise(raw, ENTITY_IDS);

        assertThat(out.get("gender"))
                .containsExactly(new LookupValue("M", "M"), new LookupValue("F", "F"));
    }

    @Test
    void entityMissingEverywhere_yieldsEmptyList() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("somethingElse", List.of("X"));

        assertThat(OneSbMasterDataAdapter.normalise(raw, ENTITY_IDS).get("gender")).isEmpty();
    }

    /** {@code data} present but not an object — must not be treated as a lookup section. */
    @Test
    void nonMapDataSection_isIgnored() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("data", "not-an-object");

        assertThat(OneSbMasterDataAdapter.normalise(raw, ENTITY_IDS).get("gender")).isEmpty();
    }

    @Test
    void listOfObjects_prefersCodeThenValueThenId() {
        List<Object> items = new ArrayList<>();
        items.add(Map.of("code", "M", "label", "Male"));
        items.add(Map.of("value", "F", "name", "Female"));
        items.add(Map.of("id", "O", "description", "Other"));

        assertThat(normaliseGender(items).get("gender")).containsExactly(
                new LookupValue("M", "Male"),
                new LookupValue("F", "Female"),
                new LookupValue("O", "Other"));
    }

    /** An entry with neither code nor label carries no information — it is dropped, not mapped. */
    @Test
    void listEntriesWithoutCodeOrLabel_areDropped() {
        List<Object> items = new ArrayList<>();
        items.add(Map.of("code", "M", "label", "Male"));
        items.add(Map.of("unrelated", "noise"));
        items.add(null);

        assertThat(normaliseGender(items).get("gender"))
                .containsExactly(new LookupValue("M", "Male"));
    }

    @Test
    void labelOnlyEntry_usesLabelAsCode() {
        assertThat(normaliseGender(List.of(Map.of("label", "Male"))).get("gender"))
                .containsExactly(new LookupValue("Male", "Male"));
    }

    @Test
    void codeOnlyEntry_usesCodeAsLabel() {
        assertThat(normaliseGender(List.of(Map.of("code", "M"))).get("gender"))
                .containsExactly(new LookupValue("M", "M"));
    }

    @Test
    void listOfNonStringScalars_isStringified() {
        assertThat(normaliseGender(List.of(1, 2)).get("gender"))
                .containsExactly(new LookupValue("1", "1"), new LookupValue("2", "2"));
    }

    @Test
    void singleCodeLabelObject_becomesSingletonList() {
        assertThat(normaliseGender(Map.of("code", "M", "label", "Male")).get("gender"))
                .containsExactly(new LookupValue("M", "Male"));
    }

    /** A map keyed by neither code/label/value is not a lookup entry — stringified, not parsed. */
    @Test
    void objectWithoutRecognisedKeys_isStringifiedNotParsed() {
        List<LookupValue> values = normaliseGender(Map.of("unrelated", "noise")).get("gender");

        assertThat(values).hasSize(1);
        assertThat(values.get(0).code()).contains("unrelated");
    }

    @Test
    void emptyObject_isStringifiedNotParsed() {
        List<LookupValue> values = normaliseGender(Map.of()).get("gender");

        assertThat(values).hasSize(1);
        assertThat(values.get(0).code()).isEqualTo("{}");
    }

    @Test
    void bareString_becomesCodeEqualLabel() {
        assertThat(normaliseGender("M").get("gender")).containsExactly(new LookupValue("M", "M"));
    }

    @Test
    void bareScalar_isStringified() {
        assertThat(normaliseGender(42).get("gender"))
                .containsExactly(new LookupValue("42", "42"));
    }
}
