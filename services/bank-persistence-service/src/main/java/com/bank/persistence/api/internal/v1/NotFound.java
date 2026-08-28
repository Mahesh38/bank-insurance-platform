package com.bank.persistence.api.internal.v1;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;

/**
 * The one "not found" this service produces.
 *
 * <p>Takes the injected {@link ServiceErrors} rather than naming the service itself: an API class
 * should not be the place a service id is written down.
 */
final class NotFound {

    private NotFound() {}

    static ServiceException of(ServiceErrors errors, String resource, String id) {
        return errors.error(ErrorCodes.RESOURCE_NOT_FOUND)
                .component(resource)
                .reason(resource + " not found: " + id)
                .build();
    }
}
