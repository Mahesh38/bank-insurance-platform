package com.bank.insurance.onesb.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.domain.JobStatus;
import com.bank.common.domain.Lob;
import com.bank.common.domain.ProposalSchema;
import com.bank.common.domain.ProposalSubmitResult;
import com.bank.common.domain.QuoteJob;
import com.bank.common.domain.QuoteOffer;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.TestErrors;
import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.insurance.onesb.domain.model.OneSbProposalSubmitResult;
import com.bank.insurance.onesb.domain.port.outbound.JobPollSchedulerPort;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbProposalPort;
import com.bank.insurance.onesb.lob.LobProposalHandler;
import com.bank.insurance.onesb.lob.LobProposalHandlerRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

  @Mock JobStorePort jobStore;
  @Mock LobProposalHandlerRegistry handlerRegistry;
  @Mock OneSbProposalPort proposalPort;
  @Mock JobPollSchedulerPort pollScheduler;
  @Mock AuditEventPublisher auditEventPublisher;
  @Mock SecretProvider secretProvider;
  @Mock LobProposalHandler handler;

  private ProposalService proposalService;

  @BeforeEach
  void setUp() {
    proposalService =
        new ProposalService(
            jobStore,
            handlerRegistry,
            proposalPort,
            pollScheduler,
            auditEventPublisher,
            secretProvider,
            TestErrors.ONESB);
  }

  @Test
  @Tag("FUNC-028")
  void getSchema_nullLob_throwsValidation() {
    assertThatThrownBy(() -> proposalService.getSchema(null, "T1", "HDFC", "1", null))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.VALIDATION_ERROR));
  }

  @Test
  @Tag("FUNC-028")
  void submit_nullLob_throwsValidation() {
    SubmitProposalCommand command =
        new SubmitProposalCommand(
            null,
            "scm-1",
            "off-1",
            "T1",
            "HDFC",
            "1",
            Map.of("ok", true),
            "consent-1",
            "109337",
            null,
            "j-1",
            null,
            "idem-1",
            "actor-1");

    assertThatThrownBy(() -> proposalService.submit(command))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.VALIDATION_ERROR));
    verify(proposalPort, never()).submit(any(), any(), any());
  }

  @Test
  @Tag("FUNC-028")
  void submit_missingProductIdentity_throwsWithoutCallingOneSb() {
    SubmitProposalCommand command =
        new SubmitProposalCommand(
            Lob.TERM,
            "scm-1",
            "off-1",
            "  ",
            null,
            "1",
            Map.of("ok", true),
            "consent-1",
            "109337",
            new SubmitProposalCommand.DistributionContext("E1", null, "B2B"),
            "j-1",
            null,
            "idem-1",
            "actor-1");

    assertThatThrownBy(() -> proposalService.submit(command))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.VALIDATION_ERROR));
    verify(proposalPort, never()).submit(any(), any(), any());
  }

  @Test
  @Tag("FUNC-004")
  void getSchema_delegatesToHandlerAndPort() {
    when(handlerRegistry.get(Lob.TERM)).thenReturn(handler);
    when(handler.schemaPath("T1", "HDFC", "1"))
        .thenReturn("/insurance/lifeterm/v1/proposal?productId=T1&manufacturerId=HDFC&version=1");
    ProposalSchema expected = new ProposalSchema(Lob.TERM, "T1", "HDFC", "1", Map.of("a", 1));
    when(proposalPort.getSchema(eq(Lob.TERM), eq("T1"), eq("HDFC"), eq("1"), any()))
        .thenReturn(expected);

    ProposalSchema result = proposalService.getSchema(Lob.TERM, "T1", "HDFC", "1", null);

    assertThat(result).isSameAs(expected);
    verify(jobStore, never()).findQuoteJob(any());
  }

  @Test
  @Tag("FUNC-028")
  void getSchema_emptyFields_throwsSchemaInvalid() {
    when(handlerRegistry.get(Lob.TERM)).thenReturn(handler);
    when(handler.schemaPath("T1", "HDFC", "1")).thenReturn("/insurance/lifeterm/v1/proposal");
    when(proposalPort.getSchema(eq(Lob.TERM), eq("T1"), eq("HDFC"), eq("1"), any()))
        .thenReturn(new ProposalSchema(Lob.TERM, "T1", "HDFC", "1", Map.of()));

    assertThatThrownBy(() -> proposalService.getSchema(Lob.TERM, "T1", "HDFC", "1", null))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.SCHEMA_INVALID));
  }

  @Test
  @Tag("FUNC-004")
  void getSchema_missingJob_throwsQuoteExpired() {
    when(jobStore.findQuoteJob("gone")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> proposalService.getSchema(Lob.TERM, "T1", "H", "1", "gone"))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex -> {
              ServiceException se = (ServiceException) ex;
              assertThat(se.getHttpStatus()).isEqualTo(410);
              assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.QUOTE_EXPIRED);
            });
    verify(proposalPort, never()).getSchema(any(), any(), any(), any(), any());
  }

  @Test
  @Tag("FUNC-004")
  void getSchema_timeoutJob_throwsQuoteExpired() {
    when(jobStore.findQuoteJob("t"))
        .thenReturn(
            Optional.of(
                new QuoteJob(
                    "t",
                    JobStatus.TIMEOUT,
                    "POLL_TIMEOUT",
                    Lob.TERM,
                    null,
                    List.of(),
                    List.of(),
                    Instant.now(),
                    Instant.now(),
                    null)));

    assertThatThrownBy(() -> proposalService.getSchema(Lob.TERM, null, null, null, "t"))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.QUOTE_EXPIRED));
  }

  @Test
  @Tag("FUNC-005")
  void submit_missingAgentId_throwsWithoutCallingOneSb() {
    SubmitProposalCommand command = baseCommand(null, null, "consent-1");

    assertThatThrownBy(() -> proposalService.submit(command))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex -> {
              ServiceException se = (ServiceException) ex;
              assertThat(se.getHttpStatus()).isEqualTo(422);
              assertThat(se.getErrorResponse().getCode())
                  .isEqualTo(ErrorCodes.AGENT_ATTRIBUTION_MISSING);
            });

    verify(proposalPort, never()).submit(any(), any(), any());
    verify(jobStore, never()).createJob(any(), any(), any(), any(), any());
  }

  @Test
  @Tag("FUNC-025")
  void submit_emptyValues_throwsWithoutCallingOneSb() {
    SubmitProposalCommand command =
        new SubmitProposalCommand(
            Lob.TERM,
            "scm-1",
            "off-1",
            "T1",
            "HDFC",
            "1",
            Map.of(),
            "consent-1",
            "109337",
            new SubmitProposalCommand.DistributionContext("E1", null, "B2B"),
            "j-1",
            null,
            "idem-1",
            "actor-1");

    assertThatThrownBy(() -> proposalService.submit(command))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.VALIDATION_ERROR));

    verify(proposalPort, never()).submit(any(), any(), any());
    verify(jobStore, never()).createJob(any(), any(), any(), any(), any());
    verify(proposalPort, never()).getSchema(any(), any(), any(), any(), any());
  }

  @Test
  @Tag("FUNC-025")
  void submit_missingMandatoryFromSchema_throwsWithoutCallingOneSb() {
    when(secretProvider.getDistributorId()).thenReturn("BCIBL");
    when(handlerRegistry.get(Lob.TERM)).thenReturn(handler);
    when(handler.schemaPath("T1", "HDFC", "1")).thenReturn("/insurance/lifeterm/v1/proposal");
    when(proposalPort.getSchema(eq(Lob.TERM), eq("T1"), eq("HDFC"), eq("1"), any()))
        .thenReturn(
            new ProposalSchema(
                Lob.TERM,
                "T1",
                "HDFC",
                "1",
                Map.of(
                    "fields",
                    List.of(
                        Map.of("id", "proposer.panNumber", "mandatory", true),
                        Map.of("id", "nominee.name", "mandatory", true)))));

    assertThatThrownBy(() -> proposalService.submit(baseCommand("109337", null, "c-1")))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex -> {
              ServiceException se = (ServiceException) ex;
              assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
              assertThat(se.getErrorResponse().getErrors())
                  .anyMatch(e -> e.field() != null && e.field().contains("nominee.name"));
            });

    verify(proposalPort, never()).submit(any(), any(), any());
    verify(jobStore, never()).createJob(any(), any(), any(), any(), any());
  }

  @Test
  @Tag("FUNC-028")
  void submit_hiddenMandatoryNotRequired_callsOneSb() {
    when(secretProvider.getDistributorId()).thenReturn("BCIBL");
    when(handlerRegistry.get(Lob.TERM)).thenReturn(handler);
    when(handler.buildSubmitPayload(any())).thenReturn(Map.of("ok", true));
    when(handler.submitPath()).thenReturn("/insurance/lifeterm/v1/proposal");
    when(jobStore.createJob(any(), any(), any(), any(), any())).thenReturn("job-hid");
    when(handler.schemaPath(any(), any(), any())).thenReturn("/insurance/lifeterm/v1/proposal");
    when(proposalPort.getSchema(any(), any(), any(), any(), any()))
        .thenReturn(
            new ProposalSchema(
                Lob.TERM,
                "T1",
                "HDFC",
                "1",
                Map.of(
                    "fields",
                    List.of(
                        Map.of("id", "parentQ", "mandatory", false, "type", "string"),
                        Map.of(
                            "id",
                            "childQ",
                            "mandatory",
                            true,
                            "type",
                            "string",
                            "parent",
                            "parentQ")))));
    when(proposalPort.submit(eq("job-hid"), any(), any()))
        .thenReturn(new OneSbProposalSubmitResult(null, "APP-HID", true));

    ProposalSubmitResult result = proposalService.submit(baseCommand("109337", null, "c-1"));

    assertThat(result.status()).isEqualTo(JobStatus.COMPLETED);
    verify(proposalPort).submit(eq("job-hid"), any(), any());
  }

  @Test
  @Tag("FUNC-028")
  void submit_schemaFetchFailure_doesNotCallOneSb() {
    when(secretProvider.getDistributorId()).thenReturn("BCIBL");
    when(handlerRegistry.get(Lob.TERM)).thenReturn(handler);
    when(handler.schemaPath(any(), any(), any())).thenReturn("/insurance/lifeterm/v1/proposal");
    when(proposalPort.getSchema(any(), any(), any(), any(), any()))
        .thenThrow(
            new ServiceException(
                ServiceErrorResponse.builder()
                    .title("Upstream unavailable")
                    .status(502)
                    .detail("1SB schema GET failed")
                    .code(ErrorCodes.UPSTREAM_UNAVAILABLE)
                    .retryable(true)
                    .build()));

    assertThatThrownBy(() -> proposalService.submit(baseCommand("109337", null, "c-1")))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE));

    verify(proposalPort, never()).submit(any(), any(), any());
    verify(jobStore, never()).createJob(any(), any(), any(), any(), any());
  }

  @Test
  @Tag("FUNC-028")
  void submit_quoteJobProductMismatch_throwsWithoutCallingOneSb() {
    when(secretProvider.getDistributorId()).thenReturn("BCIBL");
    when(handlerRegistry.get(Lob.TERM)).thenReturn(handler);
    when(handler.schemaPath(any(), any(), any())).thenReturn("/insurance/lifeterm/v1/proposal");
    when(proposalPort.getSchema(any(), any(), any(), any(), any()))
        .thenReturn(new ProposalSchema(Lob.TERM, "T1", "HDFC", "1", Map.of("ok", true)));
    when(jobStore.findQuoteJob("job-q"))
        .thenReturn(
            Optional.of(
                new QuoteJob(
                    "job-q",
                    JobStatus.COMPLETED,
                    null,
                    Lob.TERM,
                    "j-1",
                    List.of(
                        new QuoteOffer(
                            "off-1",
                            "BALIC",
                            "Bajaj",
                            "345",
                            "Prod",
                            new java.math.BigDecimal("12000"),
                            "M",
                            new java.math.BigDecimal("5000000"),
                            false,
                            "AVAILABLE",
                            null)),
                    List.of(),
                    Instant.now(),
                    Instant.now(),
                    null)));

    SubmitProposalCommand command =
        new SubmitProposalCommand(
            Lob.TERM,
            "scm-1",
            "off-1",
            "T1",
            "HDFC",
            "1",
            Map.of("proposer.panNumber", "ABCDE1234F"),
            "c-1",
            "109337",
            new SubmitProposalCommand.DistributionContext("E1", null, "B2B"),
            "j-1",
            null,
            "idem-1",
            "actor-1",
            "job-q");

    assertThatThrownBy(() -> proposalService.submit(command))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.VALIDATION_ERROR));

    verify(proposalPort, never()).submit(any(), any(), any());
    verify(jobStore, never()).createJob(any(), any(), any(), any(), any());
  }

  @Test
  @Tag("FUNC-005")
  void submit_missingConsentRef_auditsAndBlocks() {
    when(secretProvider.getDistributorId()).thenReturn("BCIBL");

    assertThatThrownBy(() -> proposalService.submit(baseCommand("109337", null, null)))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.CONSENT_REQUIRED));

    verify(proposalPort, never()).submit(any(), any(), any());
    verify(jobStore, never()).createJob(any(), any(), any(), any(), any());
    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditEventPublisher).publish(captor.capture());
    assertThat(captor.getValue().getAction()).isEqualTo(AuditActions.CONSENT_REF_MISSING);
  }

  @Test
  @Tag("FUNC-005")
  void submit_immediateApplicationNumber_completesJob() {
    when(secretProvider.getDistributorId()).thenReturn("BCIBL");
    when(handlerRegistry.get(Lob.TERM)).thenReturn(handler);
    when(handler.buildSubmitPayload(any())).thenReturn(Map.of("ok", true));
    when(handler.submitPath()).thenReturn("/insurance/lifeterm/v1/proposal");
    when(jobStore.createJob(any(), any(), any(), any(), any())).thenReturn("job-done");
    when(handler.schemaPath(any(), any(), any())).thenReturn("/insurance/lifeterm/v1/proposal");
    when(proposalPort.getSchema(any(), any(), any(), any(), any()))
        .thenReturn(new ProposalSchema(Lob.TERM, "T1", "HDFC", "1", Map.of("ok", true)));
    when(proposalPort.submit(eq("job-done"), any(), any()))
        .thenReturn(new OneSbProposalSubmitResult(null, "APP-99", true));

    ProposalSubmitResult result = proposalService.submit(baseCommand("109337", null, "c-1"));

    assertThat(result.status()).isEqualTo(JobStatus.COMPLETED);
    verify(jobStore).completeJob("job-done", List.of(), "APP-99");
    verify(pollScheduler, never()).schedulePoll(any(), any());
  }

  @Test
  @Tag("FUNC-006")
  void getProposalResult_unknown_throwsResourceNotFound() {
    when(jobStore.findQuoteJob("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> proposalService.getProposalResult("missing"))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex -> {
              ServiceException se = (ServiceException) ex;
              assertThat(se.getHttpStatus()).isEqualTo(404);
              assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
            });
  }

  @Test
  @Tag("FUNC-006")
  void getProposalResult_completed_returnsStoredJob() {
    QuoteJob job =
        new QuoteJob(
            "job-ok",
            JobStatus.COMPLETED,
            null,
            Lob.TERM,
            "j-1",
            List.of(),
            List.of(),
            Instant.parse("2026-07-30T12:00:00Z"),
            Instant.parse("2026-07-30T12:01:00Z"),
            "APP-OK");
    when(jobStore.findQuoteJob("job-ok")).thenReturn(Optional.of(job));

    assertThat(proposalService.getProposalResult("job-ok")).isEqualTo(job);
  }

  @Test
  @Tag("FUNC-005")
  void submit_businessReject_auditsAndRethrows() {
    when(secretProvider.getDistributorId()).thenReturn("BCIBL");
    when(handlerRegistry.get(Lob.TERM)).thenReturn(handler);
    when(handler.buildSubmitPayload(any())).thenReturn(Map.of("ok", true));
    when(handler.submitPath()).thenReturn("/insurance/lifeterm/v1/proposal");
    when(jobStore.createJob(any(), any(), any(), any(), any())).thenReturn("job-rej");
    when(handler.schemaPath(any(), any(), any())).thenReturn("/insurance/lifeterm/v1/proposal");
    when(proposalPort.getSchema(any(), any(), any(), any(), any()))
        .thenReturn(new ProposalSchema(Lob.TERM, "T1", "HDFC", "1", Map.of("ok", true)));
    when(proposalPort.submit(any(), any(), any()))
        .thenThrow(
            new ServiceException(
                ServiceErrorResponse.builder()
                    .title("Proposal Rejected")
                    .status(422)
                    .detail("UW declined")
                    .code(ErrorCodes.PROPOSAL_REJECTED)
                    .retryable(false)
                    .build()));

    assertThatThrownBy(() -> proposalService.submit(baseCommand("109337", null, "c-1")))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.PROPOSAL_REJECTED));

    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditEventPublisher).publish(captor.capture());
    assertThat(captor.getValue().getAction()).isEqualTo(AuditActions.PROPOSAL_SUBMITTED);
    assertThat(captor.getValue().getOutcome()).isEqualTo(AuditOutcomes.REJECTED);
  }

  private static SubmitProposalCommand baseCommand(
      String topAgentId, String distAgentId, String consentRef) {
    SubmitProposalCommand.DistributionContext distribution = null;
    if (distAgentId != null) {
      distribution = new SubmitProposalCommand.DistributionContext("E1", distAgentId, "B2B");
    } else if (topAgentId != null) {
      distribution = new SubmitProposalCommand.DistributionContext("E1", null, "B2B");
    }
    return new SubmitProposalCommand(
        Lob.TERM,
        "scm-1",
        "off-1",
        "T1",
        "HDFC",
        "1",
        Map.of("proposer.panNumber", "ABCDE1234F"),
        consentRef,
        topAgentId,
        distribution,
        "j-1",
        null,
        "idem-1",
        "actor-1");
  }
}
