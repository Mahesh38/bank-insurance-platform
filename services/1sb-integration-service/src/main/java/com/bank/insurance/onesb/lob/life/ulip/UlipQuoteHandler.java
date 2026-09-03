package com.bank.insurance.onesb.lob.life.ulip;

import com.bank.common.domain.Lob;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.lob.LobQuoteHandler;
import com.bank.insurance.onesb.lob.life.LifeQuotePayloadFactory;
import com.bank.insurance.onesb.lob.life.payload.LifeQuoteRequest;
import org.springframework.stereotype.Component;

/**
 * Life ULIP quote handler ({@code FUNC-019} / {@code EPIC-002}).
 * Paths follow {@code docs/.../field-guides/ulip-quote.md} — confirm against 1SB sandbox.
 */
@Component
public class UlipQuoteHandler implements LobQuoteHandler {

    static final String SUBMIT_PATH = "/insurance/lifeulip/v1/quote";
    static final String POLL_PATH_PREFIX = "/insurance/lifeulip/v1/quote/poll/";
    static final String PRODUCT_TOKEN = "LifeUlip";

    private final SecretProvider secretProvider;

    public UlipQuoteHandler(SecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    @Override
    public Lob supportedLob() {
        return Lob.ULIP;
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
