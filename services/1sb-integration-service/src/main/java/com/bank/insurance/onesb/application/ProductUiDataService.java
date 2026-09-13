package com.bank.insurance.onesb.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrors;
import com.bank.insurance.onesb.domain.model.ProductUiData;
import com.bank.insurance.onesb.domain.port.inbound.ProductUiDataUseCase;
import com.bank.insurance.onesb.domain.port.outbound.OneSbProductUiDataPort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Term Product UI Data orchestration — FUNC-027.
 */
@Service
public class ProductUiDataService implements ProductUiDataUseCase {

    private final OneSbProductUiDataPort productUiDataPort;
    private final ServiceErrors serviceErrors;

    public ProductUiDataService(OneSbProductUiDataPort productUiDataPort, ServiceErrors serviceErrors) {
        this.productUiDataPort = productUiDataPort;
        this.serviceErrors = serviceErrors;
    }

    @Override
    public ProductUiData getProductUiData(String productId, String manufacturerId) {
        if (!StringUtils.hasText(productId) || !StringUtils.hasText(manufacturerId)) {
            throw serviceErrors.error(ErrorCodes.VALIDATION_ERROR)
                    .component("ProductUiDataService")
                    .operation("getProductUiData")
                    .reason("productId and manufacturerId are required")
                    .errors(List.of(
                            ServiceError.ofField(ErrorCodes.MISSING_REQUIRED_FIELD,
                                    "productId is required", "productId"),
                            ServiceError.ofField(ErrorCodes.MISSING_REQUIRED_FIELD,
                                    "manufacturerId is required", "manufacturerId")))
                    .build();
        }
        return productUiDataPort.getProductUiData(productId.trim(), manufacturerId.trim());
    }
}
