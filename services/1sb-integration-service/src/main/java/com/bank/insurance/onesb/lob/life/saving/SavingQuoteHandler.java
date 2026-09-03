package com.bank.insurance.onesb.lob.life.saving;

import com.bank.common.domain.Lob;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.lob.LobQuoteHandler;
import com.bank.insurance.onesb.lob.life.LifeQuotePayloadFactory;
import com.bank.insurance.onesb.lob.life.payload.LifeQuoteRequest;
import org.springframework.stereotype.Component;

/**
 * Life Savings quote handler ({@code FUNC-015} / {@code EPIC-002}).
 * Paths follow {@code docs/.../field-guides/savings-quote.md} — confirm against 1SB sandbox.
 */
@Component
public class SavingQuoteHandler implements LobQuoteHandler {

    static final String SUBMIT_PATH = "/insurance/lifesaving/v1/quote";
    static final String POLL_PATH_PREFIX = "/insurance/lifesaving/v1/quote/poll/";
    static final String PRODUCT_TOKEN = "LifeSaving";

    private final SecretProvider secretProvider;

    public SavingQuoteHandler(SecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    @Override
    public Lob supportedLob() {
        return Lob.SAVING;
    }

    @Override
    public LifeQuoteRequest buildSubmitPayload(CreateQuoteCommand command) {
        return LifeQuotePayloadFactory.build(command, secretProvider, PRODUCT_TOKEN);
    }

    @Override
    public String submitPath() {
        return SUBMIT_PATH;
    }

    @Override
    public String pollPath(String externalReqId) {
        return POLL_PATH_PREFIX + externalReqId;
    }
}
