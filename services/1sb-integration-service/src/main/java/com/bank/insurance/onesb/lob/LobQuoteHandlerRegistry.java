package com.bank.insurance.onesb.lob;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.common.domain.Lob;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Spring-collected registry of {@link LobQuoteHandler} beans keyed by {@link Lob}.
 */
@Component
public class LobQuoteHandlerRegistry {

    private final Map<Lob, LobQuoteHandler> handlers;
    private final ServiceErrors serviceErrors;


    public LobQuoteHandlerRegistry(List<LobQuoteHandler> handlerList, ServiceErrors serviceErrors) {
        this.serviceErrors = serviceErrors;
        Map<Lob, LobQuoteHandler> map = new EnumMap<>(Lob.class);
        for (LobQuoteHandler handler : handlerList) {
            Lob lob = handler.supportedLob();
            if (map.containsKey(lob)) {
                throw new IllegalStateException("Duplicate LobQuoteHandler for " + lob);
            }
            map.put(lob, handler);
        }
        this.handlers = Map.copyOf(map);
    }

    /**
     * @throws ServiceException 422 {@link ErrorCodes#UNSUPPORTED_LOB} when no handler is registered
     */
    public LobQuoteHandler get(Lob lob) {
        if (lob == null) {
            throw unsupported(null);
        }
        LobQuoteHandler handler = handlers.get(lob);
        if (handler == null) {
            throw unsupported(lob);
        }
        return handler;
    }

    private ServiceException unsupported(Lob lob) {
        String detail = lob == null ? "lob is required" : "Unsupported lob: " + lob;
        return serviceErrors.error(ErrorCodes.UNSUPPORTED_LOB)
                .component("LobQuoteHandlerRegistry")
                .operation("get")
                .reason(detail)
                .errors(java.util.List.of(ServiceError.ofField(ErrorCodes.UNSUPPORTED_LOB, detail, "lob")))
                .build();
    }
}
