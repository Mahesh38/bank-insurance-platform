package com.bank.insurance.onesb.lob.life.ulip;

import com.bank.common.domain.Lob;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.insurance.onesb.lob.LobProposalHandler;
import com.bank.insurance.onesb.lob.life.LifeProposalSupport;
import com.bank.insurance.onesb.lob.life.payload.LifeProposalSubmitBody;
import org.springframework.stereotype.Component;

/**
 * Life ULIP proposal handler ({@code FUNC-019}).
 * Uses Saving proposal paths — ULIP is a Saving subtype on the 1SB portal, not a separate prefix.
 */
@Component
public class UlipProposalHandler implements LobProposalHandler {

    static final String SCHEMA_PATH = "/insurance/lifesave/v1/proposal";
    static final String SUBMIT_PATH = "/insurance/lifesave/v1/proposal";
    static final String POLL_PATH_PREFIX = "/insurance/lifesave/v1/proposal/poll/";

    private final SecretProvider secretProvider;

    public UlipProposalHandler(SecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    @Override
    public Lob supportedLob() {
        return Lob.ULIP;
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
