package com.bank.insurance.onesb.application;

import com.bank.common.domain.Lob;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrors;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.UlipFundResult;
import com.bank.insurance.onesb.domain.port.inbound.UlipFundUseCase;
import com.bank.insurance.onesb.domain.port.outbound.OneSbUlipFundPort;
import com.bank.insurance.onesb.lob.LobQuoteHandler;
import com.bank.insurance.onesb.lob.LobQuoteHandlerRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ULIP fund list / performance orchestration — FUNC-027.
 * Payload reuses the ULIP quote envelope ({@code LifeSave} + {@code savingsProductType=["ULIP"]}).
 */
@Service
public class UlipFundService implements UlipFundUseCase {

    private final LobQuoteHandlerRegistry handlerRegistry;
    private final OneSbUlipFundPort ulipFundPort;
    private final ServiceErrors serviceErrors;

    public UlipFundService(
            LobQuoteHandlerRegistry handlerRegistry,
            OneSbUlipFundPort ulipFundPort,
            ServiceErrors serviceErrors) {
        this.handlerRegistry = handlerRegistry;
        this.ulipFundPort = ulipFundPort;
        this.serviceErrors = serviceErrors;
    }

    @Override
    public UlipFundResult listFunds(CreateQuoteCommand command) {
        Object payload = payload(command, false, "listFunds");
        return ulipFundPort.listFunds(payload);
    }

    @Override
    public UlipFundResult fundPerformance(CreateQuoteCommand command) {
        Object payload = payload(command, true, "fundPerformance");
        return ulipFundPort.fundPerformance(payload);
    }

    private Object payload(CreateQuoteCommand command, boolean requirePin, String operation) {
        validate(command, requirePin, operation);
        LobQuoteHandler handler = handlerRegistry.get(Lob.ULIP);
        return handler.buildSubmitPayload(command);
    }

    private void validate(CreateQuoteCommand command, boolean requirePin, String operation) {
        List<ServiceError> errors = new ArrayList<>();
        if (command.lob() == null || command.lob() != Lob.ULIP) {
            errors.add(ServiceError.ofField(
                    ErrorCodes.UNSUPPORTED_LOB,
                    "ULIP fund APIs require lob=ULIP",
                    "lob"));
        }
        if (command.sumAssured() == null) {
            errors.add(ServiceError.ofField(
                    ErrorCodes.MISSING_REQUIRED_FIELD, "sumAssured is required", "sumAssured"));
        }
        if (command.members() == null || command.members().isEmpty()) {
            errors.add(ServiceError.ofField(
                    ErrorCodes.MISSING_REQUIRED_FIELD, "members must be non-empty", "members"));
        }
        if (requirePin) {
            CreateQuoteCommand.ProductSelection selection = command.selection();
            if (selection == null || selection.insurerCode() == null || selection.insurerCode().isBlank()) {
                errors.add(ServiceError.ofField(
                        ErrorCodes.MISSING_REQUIRED_FIELD,
                        "selection.insurerCode is required for ULIP fund performance",
                        "selection.insurerCode"));
            }
            if (selection == null
                    || selection.productCodes() == null
                    || selection.productCodes().stream().noneMatch(c -> c != null && !c.isBlank())) {
                errors.add(ServiceError.ofField(
                        ErrorCodes.MISSING_REQUIRED_FIELD,
                        "selection.productCodes is required for ULIP fund performance",
                        "selection.productCodes"));
            }
        }
        if (!errors.isEmpty()) {
            String code = errors.stream()
                    .anyMatch(e -> ErrorCodes.UNSUPPORTED_LOB.equals(e.code()))
                    ? ErrorCodes.UNSUPPORTED_LOB
                    : ErrorCodes.VALIDATION_ERROR;
            throw serviceErrors.error(code)
                    .component("UlipFundService")
                    .operation(operation)
                    .reason("ULIP fund request validation failed: " + errors.size() + " field error(s)")
                    .errors(errors)
                    .build();
        }
    }
}
