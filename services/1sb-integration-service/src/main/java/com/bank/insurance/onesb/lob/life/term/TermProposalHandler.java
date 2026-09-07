package com.bank.insurance.onesb.lob.life.term;

import com.bank.common.domain.Lob;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.insurance.onesb.lob.LobProposalHandler;
import com.bank.insurance.onesb.lob.life.LifeProposalSupport;
import com.bank.insurance.onesb.lob.life.payload.LifeProposalSubmitBody;
import org.springframework.stereotype.Component;

/**
 * Term LOB proposal handler.
 * <p>
 * Schema: {@code GET /insurance/lifeterm/v1/proposal?productId=&manufacturerId=&version=}
 * Submit: {@code POST /insurance/lifeterm/v1/proposal}
 * Poll: {@code GET /insurance/lifeterm/v1/proposal/poll/{reqId}}
 */
@Component
public class TermProposalHandler implements LobProposalHandler {

    static final String SCHEMA_PATH = "/insurance/lifeterm/v1/proposal";
    static final String SUBMIT_PATH = "/insurance/lifeterm/v1/proposal";
    static final String POLL_PATH_PREFIX = "/insurance/lifeterm/v1/proposal/poll/";

    private final SecretProvider secretProvider;

    public TermProposalHandler(SecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    @Override
    public Lob supportedLob() {
        return Lob.TERM;
    }

    @Override
    public String schemaPath(String productCode, String manufacturerId, String version) {
        return LifeProposalSupport.schemaPath(SCHEMA_PATH, productCode, manufacturerId, version);
    }

    @Override
    public String submitPath() {
        return SUBMIT_PATH;
    }

    @Override
    public String pollPath(String externalReqId) {
        return LifeProposalSupport.pollPath(POLL_PATH_PREFIX, externalReqId);
    }

    @Override
    public LifeProposalSubmitBody buildSubmitPayload(SubmitProposalCommand command) {
        return LifeProposalSupport.buildSubmitBody(command, secretProvider);
    }
}
