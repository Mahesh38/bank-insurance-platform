package com.bank.platform.quotation.adapter.envelope;

import com.bank.platform.quotation.domain.QuoteEnvelope;
import com.bank.platform.quotation.domain.QuoteMember;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * JSON form of the envelope before encryption. A private mapper, not the web one, so a change to
 * HTTP serialisation settings can never change the stored envelope shape.
 */
@Component
public class QuoteEnvelopeCodec {

  private final ObjectMapper mapper =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  public byte[] encode(QuoteEnvelope envelope) {
    return write(envelope);
  }

  public QuoteEnvelope decode(byte[] json) {
    try {
      return mapper.readValue(json, QuoteEnvelope.class);
    } catch (IOException e) {
      throw new IllegalStateException("working quote envelope is unreadable", e);
    }
  }

  /** Members alone, for {@code quotation.quote.members_enc}. */
  public byte[] encodeMembers(List<QuoteMember> members) {
    return write(members);
  }

  public List<QuoteMember> decodeMembers(byte[] json) {
    try {
      return mapper.readValue(json, new TypeReference<List<QuoteMember>>() {});
    } catch (IOException e) {
      throw new IllegalStateException("members payload is unreadable", e);
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
