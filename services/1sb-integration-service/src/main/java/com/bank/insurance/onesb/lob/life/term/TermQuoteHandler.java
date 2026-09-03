package com.bank.insurance.onesb.lob.life.term;

import com.bank.common.domain.Lob;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.lob.LobQuoteHandler;
import com.bank.insurance.onesb.lob.life.LifeQuotePayloadFactory;
import com.bank.insurance.onesb.lob.life.payload.LifeQuoteRequest;
import org.springframework.stereotype.Component;

/**
 * Term LOB quote handler — maps bank {@link CreateQuoteCommand} to typed
 * {@link LifeQuoteRequest} for {@code POST /insurance/lifeterm/v1/quote}.
 * <p>
 * Poll path: {@code GET /insurance/lifeterm/v1/quote/poll/{reqId}}.
 */
@Component
public class TermQuoteHandler implements LobQuoteHandler {

    static final String SUBMIT_PATH = "/insurance/lifeterm/v1/quote";
    static final String POLL_PATH_PREFIX = "/insurance/lifeterm/v1/quote/poll/";
    static final String PRODUCT_TOKEN = "LifeTerm";

    private final SecretProvider secretProvider;

    public TermQuoteHandler(SecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    @Override
    public Lob supportedLob() {
        return Lob.TERM;
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
