package com.bank.insurance.onesb.lob;

import com.bank.insurance.onesb.domain.model.Lob;

/**
 * Strategy interface for LOB-specific proposal request translation.
 */
public interface LobProposalHandler {

    Lob supportedLob();
}
