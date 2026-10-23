package com.bank.platform.proposal.adapter.envelope;

import com.bank.platform.proposal.domain.CounterOffer;
import com.bank.platform.proposal.domain.DocumentRef;
import com.bank.platform.proposal.domain.Nominee;
import com.bank.platform.proposal.domain.ProposalAnswer;
import com.bank.platform.proposal.domain.ProposalEnvelope;
import com.bank.platform.proposal.domain.UwRequirement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/**
 * JSON form of the envelope before encryption. A private mapper, so web serialisation settings can
 * never change the stored shape. Writes envelope v2 ({@link ProposalEnvelope#SCHEMA_VERSION}) and
 * still reads v1 items written before D8 (item attribute {@code payloadSchemaVersion}).
 */
@Component
public class ProposalEnvelopeCodec {

  private final ObjectMapper mapper =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  public byte[] encode(ProposalEnvelope envelope) {
    return write(envelope);
  }

  /** Envelope v1: answers were a bare {@code questionId → value} map, without question text. */
  record EnvelopeV1(
      Map<String, String> formValues,
      Nominee nominee,
      List<UwRequirement> requirements,
      List<DocumentRef> documents) {}

  public ProposalEnvelope decode(byte[] json) {
    return decode(json, ProposalEnvelope.SCHEMA_VERSION, null);
  }

  /**
   * @param payloadSchemaVersion the item's {@code payloadSchemaVersion}
   * @param formSchemaVersion the proposal's form {@code schemaVersion}, recorded on v1 answers
   */
  public ProposalEnvelope decode(byte[] json, int payloadSchemaVersion, String formSchemaVersion) {
    try {
      return switch (payloadSchemaVersion) {
        case 1 -> upgrade(mapper.readValue(json, EnvelopeV1.class), formSchemaVersion);
        case ProposalEnvelope.SCHEMA_VERSION -> mapper.readValue(json, ProposalEnvelope.class);
        default ->
            throw new IllegalStateException(
                "unknown proposal envelope version " + payloadSchemaVersion);
      };
    } catch (IOException e) {
      throw new IllegalStateException("working proposal envelope is unreadable", e);
    }
  }

  // The question text shown was never stored in v1, so it stays null: materialise refuses such
  // an answer rather than inventing the text (D8 "as shown").
  private static ProposalEnvelope upgrade(EnvelopeV1 v1, String formSchemaVersion) {
    List<ProposalAnswer> answers = new ArrayList<>();
    if (v1.formValues() != null) {
      new TreeMap<>(v1.formValues())
          .forEach(
              (id, value) ->
                  answers.add(
                      new ProposalAnswer(id, null, null, formSchemaVersion, value, null, null)));
    }
    return new ProposalEnvelope(answers, v1.nominee(), v1.requirements(), v1.documents(), null);
  }

  /** The counter-offer as {@code proposal.counter_offer} JSON; null if none. */
  public String encodeCounterOffer(CounterOffer counterOffer) {
    return counterOffer == null ? null : new String(write(counterOffer), StandardCharsets.UTF_8);
  }

  /** The nominee alone, for {@code proposal.proposal.nominee_enc}; {@code null} if none. */
  public byte[] encodeNominee(Nominee nominee) {
    return write(nominee);
  }

  public Nominee decodeNominee(byte[] json) {
    try {
      return mapper.readValue(json, Nominee.class);
    } catch (IOException e) {
      throw new IllegalStateException("nominee payload is unreadable", e);
    }
  }

  private byte[] write(Object value) {
    try {
      return mapper.writeValueAsBytes(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("envelope could not be serialised", e);
    }
  }
}
