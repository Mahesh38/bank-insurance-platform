package com.bank.persistence.api.internal.v1;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceException;

final class NotFound {

    private NotFound() {}

    static ServiceException of(String resource, String id) {
        return ServiceException.of(ErrorCodes.RESOURCE_NOT_FOUND)
                .service("persistence")
                .layer(PlatformLayer.L7)
                .component(resource)
                .reason(resource + " not found: " + id)
                .build();
    }
}
