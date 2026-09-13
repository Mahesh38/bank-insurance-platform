package com.bank.insurance.onesb.domain.port.inbound;

import com.bank.insurance.onesb.domain.model.ProductUiData;

/**
 * Term Product UI Data (portal {@code Get Product UI Data}).
 */
public interface ProductUiDataUseCase {

    ProductUiData getProductUiData(String productId, String manufacturerId);
}
