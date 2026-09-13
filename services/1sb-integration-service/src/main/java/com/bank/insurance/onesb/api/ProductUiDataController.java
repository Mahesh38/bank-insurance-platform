package com.bank.insurance.onesb.api;

import com.bank.insurance.onesb.api.dto.ProductUiDataResponse;
import com.bank.insurance.onesb.domain.model.ProductUiData;
import com.bank.insurance.onesb.domain.port.inbound.ProductUiDataUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bank Product UI Data — {@code GET /v1/products/ui-data} (FUNC-027).
 * Maps to documented Term {@code GET /insurance/lifeterm/v1/master/getproductuidata}.
 */
@RestController
@RequestMapping("/v1/products")
public class ProductUiDataController {

    private final ProductUiDataUseCase productUiDataUseCase;

    public ProductUiDataController(ProductUiDataUseCase productUiDataUseCase) {
        this.productUiDataUseCase = productUiDataUseCase;
    }

    @GetMapping("/ui-data")
    public ResponseEntity<ProductUiDataResponse> getProductUiData(
            @RequestParam String productId,
            @RequestParam String manufacturerId) {
        ProductUiData result = productUiDataUseCase.getProductUiData(productId, manufacturerId);
        return ResponseEntity.ok(toResponse(result));
    }

    static ProductUiDataResponse toResponse(ProductUiData result) {
        return new ProductUiDataResponse(
                result.productId(),
                result.manufacturerId(),
                result.reqId(),
                result.data());
    }
}
