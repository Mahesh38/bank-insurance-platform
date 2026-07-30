package com.bank.insurance.onesb.lob;

import com.bank.insurance.onesb.domain.model.Lob;

/**
 * Strategy interface for LOB-specific quote request translation.
 * Each implementation knows how to map a bank canonical command to the
 * insurer-specific payload for its LOB.
 */
public interface LobQuoteHandler {

    Lob supportedLob();
}
