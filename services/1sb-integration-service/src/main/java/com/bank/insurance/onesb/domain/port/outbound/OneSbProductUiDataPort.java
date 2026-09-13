package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.model.ProductUiData;

/**
 * Outbound port for 1SB Get Product UI Data.
 */
public interface OneSbProductUiDataPort {

    ProductUiData getProductUiData(String productId, String manufacturerId);
}
