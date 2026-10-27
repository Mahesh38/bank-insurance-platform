package com.bank.platform.suitability.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "mapping_product", schema = "suitability")
@IdClass(MappingProductEntity.Key.class)
public class MappingProductEntity {

  @Id
  @Column(name = "run_id", length = 26)
  private String runId;

  @Id
  @Column(name = "product_id", length = 64)
  private String productId;

  @Column(name = "insurer_code", nullable = false, length = 32)
  private String insurerCode;

  @Column(name = "product_name", nullable = false, length = 160)
  private String productName;

  @Column(name = "product_type", nullable = false, length = 32)
  private String productType;

  public String getRunId() {
    return runId;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public void setInsurerCode(String insurerCode) {
    this.insurerCode = insurerCode;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getProductType() {
    return productType;
  }

  public void setProductType(String productType) {
    this.productType = productType;
  }

  public static final class Key implements Serializable {
    private String runId;
    private String productId;

    public Key() {}

    public Key(String runId, String productId) {
      this.runId = runId;
      this.productId = productId;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof Key key)) {
        return false;
      }
      return Objects.equals(runId, key.runId) && Objects.equals(productId, key.productId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(runId, productId);
    }
  }
}
