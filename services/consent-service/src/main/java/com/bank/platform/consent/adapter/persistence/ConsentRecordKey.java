package com.bank.platform.consent.adapter.persistence;

import java.io.Serializable;
import java.util.Objects;

public class ConsentRecordKey implements Serializable {

  private String consentId;
  private int recordVersion;

  public ConsentRecordKey() {}

  public ConsentRecordKey(String consentId, int recordVersion) {
    this.consentId = consentId;
    this.recordVersion = recordVersion;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ConsentRecordKey key)) {
      return false;
    }
    return recordVersion == key.recordVersion && Objects.equals(consentId, key.consentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(consentId, recordVersion);
  }
}
