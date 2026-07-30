package com.bank.insurance.onesb.observability;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PiiMaskerTest {

    private static final String PAN = "ABCDE1234F";
    private static final String MOBILE = "9876543210";
    private static final String NAME = "Priya Sharma";
    private static final String EMAIL = "priya.sharma@example.com";
    private static final String DOB = "1990-05-15";

    @Test
    void maskPan_hidesContiguousPanPattern() {
        String masked = PiiMasker.maskPan(PAN);

        assertThat(masked).isEqualTo("*****1234F");
        assertThat(masked).doesNotContain(PAN);
        assertThat(masked).doesNotMatch("(?i).*[A-Z]{5}[0-9]{4}[A-Z].*");
    }

    @Test
    void maskMobile_showsAtMostLastFourDigits() {
        String masked = PiiMasker.maskMobile(MOBILE);

        assertThat(masked).endsWith("3210");
        assertThat(masked).doesNotContain("987654");
        assertThat(masked.replaceAll("\\D", "").length()).isLessThanOrEqualTo(4);
    }

    @Test
    void maskName_email_dob_removeOriginals() {
        assertThat(PiiMasker.maskName(NAME)).doesNotContain(NAME);
        assertThat(PiiMasker.maskEmail(EMAIL)).doesNotContain(EMAIL);
        assertThat(PiiMasker.maskDob(DOB)).doesNotContain(DOB);
        assertThat(PiiMasker.maskDob(DOB)).isEqualTo("****-**-**");
    }

    @Test
    void nullAndBlank_areSafe() {
        assertThat(PiiMasker.maskName(null)).isNull();
        assertThat(PiiMasker.maskMobile("")).isEmpty();
        assertThat(PiiMasker.maskEmail("   ")).isEqualTo("   ");
        assertThat(PiiMasker.maskPan(null)).isNull();
        assertThat(PiiMasker.maskDob("")).isEmpty();
        assertThat(PiiMasker.maskJson(null)).isNull();
        assertThat(PiiMasker.maskText(null)).isNull();
        assertThat(PiiMasker.maskMap(null)).isNull();
    }

    @Test
    void maskJson_scrubsKnownFieldsAndPatterns() {
        String json = """
                {"name":"%s","mobile":"%s","email":"%s","pan":"%s","dob":"%s","product":"TERM"}
                """.formatted(NAME, MOBILE, EMAIL, PAN, DOB).trim();

        String masked = PiiMasker.maskJson(json);

        assertThat(masked).doesNotContain(NAME);
        assertThat(masked).doesNotContain(MOBILE);
        assertThat(masked).doesNotContain(EMAIL);
        assertThat(masked).doesNotContain(PAN);
        assertThat(masked).doesNotContain(DOB);
        assertThat(masked).contains("TERM");
        assertThat(masked).doesNotMatch("(?i).*[A-Z]{5}[0-9]{4}[A-Z].*");
    }

    @Test
    void maskMap_masksNestedPii() {
        Map<String, Object> masked = PiiMasker.maskMap(Map.of(
                "customerName", NAME,
                "mobileNumber", MOBILE,
                "email", EMAIL,
                "pan", PAN,
                "dateOfBirth", DOB,
                "lob", "TERM"
        ));

        assertThat(masked.get("customerName").toString()).doesNotContain(NAME);
        assertThat(masked.get("mobileNumber").toString()).endsWith("3210");
        assertThat(masked.get("email").toString()).doesNotContain(EMAIL);
        assertThat(masked.get("pan").toString()).isEqualTo("*****1234F");
        assertThat(masked.get("dateOfBirth").toString()).isEqualTo("****-**-**");
        assertThat(masked.get("lob")).isEqualTo("TERM");
    }

    @Test
    void maskText_scrubsEmbeddedPanAndMobile() {
        String text = "Customer " + NAME + " PAN " + PAN + " mobile " + MOBILE + " dob " + DOB;

        String masked = PiiMasker.maskText(text);

        assertThat(masked).doesNotContain(PAN);
        assertThat(masked).doesNotContain(MOBILE);
        assertThat(masked).doesNotContain(DOB);
        assertThat(masked).doesNotMatch("(?i).*[A-Z]{5}[0-9]{4}[A-Z].*");
    }
}
