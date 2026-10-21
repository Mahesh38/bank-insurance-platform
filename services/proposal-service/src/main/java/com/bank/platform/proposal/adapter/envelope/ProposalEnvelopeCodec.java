package com.bank.platform.proposal.adapter.envelope;

import com.bank.platform.proposal.domain.Nominee;
import com.bank.platform.proposal.domain.ProposalEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import org.springframework.stereotype.Component;

/**
 * JSON form of the envelope before encryption. A private mapper, so web serialisation settings can
 * never change the stored shape.
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

  public ProposalEnvelope decode(byte[] json) {
    try {
      return mapper.readValue(json, ProposalEnvelope.class);
    } catch (IOException e) {
      throw new IllegalStateException("working proposal envelope is unreadable", e);
    }
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
