package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ChallengeRow;
import com.bank.platform.consent.domain.ConsentRow;
import java.util.Arrays;
import java.util.List;

final class RowMapping {

  private RowMapping() {}

  static ConsentRecordEntity toEntity(ConsentRow row) {
    ConsentRecordEntity entity = new ConsentRecordEntity();
    entity.setConsentId(row.consentId());
    entity.setRecordVersion(row.recordVersion());
    entity.setCustomerId(row.customerId());
    entity.setLeadId(row.leadId());
    entity.setJourneyId(row.journeyId());
    entity.setLob(row.lob());
    entity.setPurpose(row.purpose());
    entity.setInsurerCode(row.insurerCode());
    entity.setStatementId(row.statementId());
    entity.setStatementVersion(row.statementVersion());
    entity.setStatementText(row.statementText());
    entity.setStatementTextHash(row.statementTextHash());
    entity.setLanguage(row.language());
    entity.setChannel(row.channel());
    entity.setAcknowledgementMethod(row.acknowledgementMethod());
    entity.setCheckpoint(row.checkpoint());
    entity.setState(row.state());
    entity.setCapturedAt(row.capturedAt());
    entity.setOtpVerifiedAt(row.otpVerifiedAt());
    entity.setOtpTxnId(row.otpTxnId());
    entity.setSourceIp(row.sourceIp());
    entity.setContactUsedEnc(row.contactUsedEnc());
    entity.setEncryptionKeyId(row.encryptionKeyId());
    entity.setCapturedByActorId(row.capturedByActorId());
    entity.setValidFrom(row.validFrom());
    entity.setValidUntil(row.validUntil());
    entity.setSupersedesConsentId(row.supersedesConsentId());
    entity.setWithdrawnAt(row.withdrawnAt());
    entity.setWithdrawnBy(row.withdrawnBy());
    entity.setWithdrawalChannel(row.withdrawalChannel());
    entity.setWithdrawalReasonCode(row.withdrawalReasonCode());
    entity.setWithdrawalOtpTxnId(row.withdrawalOtpTxnId());
    entity.setConfigVersionRef(row.configVersionRef());
    entity.setCreatedAt(row.createdAt());
    return entity;
  }

  static ConsentRow toRow(ConsentRecordEntity entity) {
    return new ConsentRow(
        entity.getConsentId(),
        entity.getRecordVersion(),
        entity.getCustomerId(),
        entity.getLeadId(),
        entity.getJourneyId(),
        entity.getLob(),
        entity.getPurpose(),
        entity.getInsurerCode(),
        entity.getStatementId(),
        entity.getStatementVersion(),
        entity.getStatementText(),
        entity.getStatementTextHash(),
        entity.getLanguage(),
        entity.getChannel(),
        entity.getAcknowledgementMethod(),
        entity.getCheckpoint(),
        entity.getState(),
        entity.getCapturedAt(),
        entity.getOtpVerifiedAt(),
        entity.getOtpTxnId(),
        entity.getSourceIp(),
        entity.getContactUsedEnc(),
        entity.getEncryptionKeyId(),
        entity.getCapturedByActorId(),
        entity.getValidFrom(),
        entity.getValidUntil(),
        entity.getSupersedesConsentId(),
        entity.getWithdrawnAt(),
        entity.getWithdrawnBy(),
        entity.getWithdrawalChannel(),
        entity.getWithdrawalReasonCode(),
        entity.getWithdrawalOtpTxnId(),
        entity.getConfigVersionRef(),
        entity.getCreatedAt());
  }

  static ChallengeEntity toEntity(ChallengeRow row) {
    ChallengeEntity entity = new ChallengeEntity();
    entity.setChallengeId(row.challengeId());
    entity.setKind(row.kind());
    entity.setState(row.state());
    entity.setAttempts(row.attempts());
    entity.setOtpIssueCount(row.otpIssueCount());
    entity.setOtpExpiresAt(row.otpExpiresAt());
    entity.setOtpTxnId(row.otpTxnId());
    entity.setOtpHash(row.otpHash());
    entity.setPurposes(String.join(",", row.purposes()));
    entity.setStatementRefs(row.statementRefs());
    entity.setContactUsedEnc(row.contactUsedEnc());
    entity.setEncryptionKeyId(row.encryptionKeyId());
    entity.setCustomerId(row.customerId());
    entity.setJourneyId(row.journeyId());
    entity.setLeadId(row.leadId());
    entity.setLob(row.lob());
    entity.setCheckpoint(row.checkpoint());
    entity.setInsurerCode(row.insurerCode());
    entity.setChannel(row.channel());
    entity.setLanguage(row.language());
    entity.setTargetConsentId(row.targetConsentId());
    entity.setActorId(row.actorId());
    entity.setConfigVersionRef(row.configVersionRef());
    entity.setCreatedAt(row.createdAt());
    return entity;
  }

  static ChallengeRow toRow(ChallengeEntity entity) {
    List<String> purposes =
        entity.getPurposes() == null || entity.getPurposes().isBlank()
            ? List.of()
            : Arrays.asList(entity.getPurposes().split(","));
    return new ChallengeRow(
        entity.getChallengeId(),
        entity.getKind(),
        entity.getState(),
        entity.getAttempts(),
        entity.getOtpIssueCount(),
        entity.getOtpExpiresAt(),
        entity.getOtpTxnId(),
        entity.getOtpHash(),
        purposes,
        entity.getStatementRefs(),
        entity.getContactUsedEnc(),
        entity.getEncryptionKeyId(),
        entity.getCustomerId(),
        entity.getJourneyId(),
        entity.getLeadId(),
        entity.getLob(),
        entity.getCheckpoint(),
        entity.getInsurerCode(),
        entity.getChannel(),
        entity.getLanguage(),
        entity.getTargetConsentId(),
        entity.getActorId(),
        entity.getConfigVersionRef(),
        entity.getCreatedAt());
  }
}
