package com.bank.insurance.persistence.api.internal.v1;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;

final class NotFound {

    private NotFound() {}

    static ServiceException of(String resource, String id) {
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Not Found")
                .status(404)
                .detail(resource + " not found: " + id)
                .code(ErrorCodes.RESOURCE_NOT_FOUND)
                .retryable(false)
                .build());
    }
}
