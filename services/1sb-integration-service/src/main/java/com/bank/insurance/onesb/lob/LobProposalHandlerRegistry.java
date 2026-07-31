package com.bank.insurance.onesb.lob;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.model.Lob;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Spring-collected registry of {@link LobProposalHandler} beans keyed by {@link Lob}.
 */
@Component
public class LobProposalHandlerRegistry {

    private final Map<Lob, LobProposalHandler> handlers;

    public LobProposalHandlerRegistry(List<LobProposalHandler> handlerList) {
        Map<Lob, LobProposalHandler> map = new EnumMap<>(Lob.class);
        for (LobProposalHandler handler : handlerList) {
            Lob lob = handler.supportedLob();
            if (map.containsKey(lob)) {
                throw new IllegalStateException("Duplicate LobProposalHandler for " + lob);
            }
            map.put(lob, handler);
        }
        this.handlers = Map.copyOf(map);
    }

    /**
     * @throws ServiceException 422 {@link ErrorCodes#UNSUPPORTED_LOB} when no handler is registered
     */
    public LobProposalHandler get(Lob lob) {
        if (lob == null) {
            throw unsupported(null);
        }
        LobProposalHandler handler = handlers.get(lob);
        if (handler == null) {
            throw unsupported(lob);
        }
        return handler;
    }

    private static ServiceException unsupported(Lob lob) {
        String detail = lob == null ? "lob is required" : "Unsupported lob: " + lob;
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Unsupported LOB")
                .status(422)
                .detail(detail)
                .code(ErrorCodes.UNSUPPORTED_LOB)
                .retryable(false)
                .addError(ServiceError.ofField(ErrorCodes.UNSUPPORTED_LOB, detail, "lob"))
                .build());
    }
}
