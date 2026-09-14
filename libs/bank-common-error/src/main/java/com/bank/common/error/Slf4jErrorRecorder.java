package com.bank.common.error;

import com.bank.common.observability.ErrorMetrics;
import com.bank.common.observability.MdcContext;
import com.bank.common.observability.MdcKeys;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default {@link ErrorRecorder}: one structured log line and one metric increment.
 *
 * <p><strong>Level follows the category, not the status.</strong> A caller's invalid request is a
 * normal outcome of a public API; logging it at {@code ERROR} with a stack trace is what makes an
 * error dashboard unreadable within a week. Client-caused categories log at {@code WARN} without a
 * stack; platform-caused categories log at {@code ERROR} with one.
 *
 * <p>The log line is the six questions from {@code
 * docs/journey-execution/07-PLATFORM-ERROR-CONTRACT.md §6}, and the MDC entries are what let
 * support pull every line of one failure by its incident id.
 *
 * <p>Every free-text fragment that reaches the log backend is passed through {@link LogPiiScrubber}
 * first ({@code S08-G7} / {@code FF-05}). The in-memory diagnostic kept for support search is
 * unchanged; only the emitted line is scrubbed.
 */
public class Slf4jErrorRecorder implements ErrorRecorder {

  private static final Logger log = LoggerFactory.getLogger(Slf4jErrorRecorder.class);

  private static final String MESSAGE =
      "error code={} category={} service={} layer={} originService={} status={} runbook={} reason={}";

  private final PlatformLayer defaultLayer;
  private final ErrorMetrics metrics;

  /**
   * @param defaultLayer used when a failure carries no diagnostic naming its own layer
   * @param metrics may be null; a service must not lose its error responses because it cannot count
   *     them
   */
  public Slf4jErrorRecorder(PlatformLayer defaultLayer, ErrorMetrics metrics) {
    this.defaultLayer = defaultLayer;
    this.metrics = metrics;
  }

  @Override
  public void record(ServiceErrorResponse response, Throwable cause) {
    ErrorDiagnostic d = response.getDiagnostic();
    ErrorCategory category = response.getCategory();
    PlatformLayer layer = d != null && d.getLayer() != null ? d.getLayer() : defaultLayer;
    String originService = d != null ? d.effectiveOriginService() : response.getService();
    String runbook =
        d != null
            ? d.getRunbook()
            : ErrorCatalogue.find(response.getCode()).map(ErrorDefinition::runbook).orElse(null);

    MdcContext.with(
        diagnosticContext(response, category, layer, originService),
        () -> {
          Object[] args = {
            response.getCode(),
            category,
            response.getService(),
            layer,
            originService,
            response.getStatus(),
            runbook,
            d != null ? LogPiiScrubber.scrub(d.getReason()) : null
          };
          if (category != null && category.clientCaused()) {
            log.warn(MESSAGE, args);
          } else if (cause != null) {
            log.error(MESSAGE, append(args, forLogging(cause)));
          } else {
            log.error(MESSAGE, args);
          }
        });

    if (metrics != null) {
      metrics.record(
          response.getService(),
          response.getCode(),
          category != null ? category.name() : null,
          layer != null ? layer.name() : null,
          originService,
          response.isRetryable(),
          response.getStatus());
    }
  }

  private static Map<String, String> diagnosticContext(
      ServiceErrorResponse response,
      ErrorCategory category,
      PlatformLayer layer,
      String originService) {
    Map<String, String> context = new LinkedHashMap<>();
    context.put(MdcKeys.INCIDENT_ID, response.getIncidentId());
    context.put(MdcKeys.ERROR_CODE, response.getCode());
    context.put(MdcKeys.ERROR_CATEGORY, category != null ? category.name() : null);
    context.put(MdcKeys.SERVICE, response.getService());
    context.put(MdcKeys.ORIGIN_SERVICE, originService);
    context.put(MdcKeys.LAYER, layer != null ? layer.name() : null);
    if (response.getCorrelationId() != null) {
      context.put(MdcKeys.CORRELATION_ID, response.getCorrelationId());
    }
    return context;
  }

  private static Object[] append(Object[] args, Throwable cause) {
    Object[] withCause = new Object[args.length + 1];
    System.arraycopy(args, 0, withCause, 0, args.length);
    withCause[args.length] = cause;
    return withCause;
  }

  /**
   * Preserves the original stack <em>and</em> cause chain for operators while ensuring every
   * exception message printed by the logging backend cannot carry regulated attributes.
   *
   * <p>A shallow copy that drops {@link Throwable#getCause()} was rejected in review: operators
   * lose the root failure. Suppressed exceptions are scrubbed the same way. Cycles are broken with
   * an identity map so a malicious / broken cause graph cannot recurse forever.
   */
  static Throwable forLogging(Throwable cause) {
    return scrubChain(cause, new IdentityHashMap<>());
  }

  private static Throwable scrubChain(Throwable cause, IdentityHashMap<Throwable, Throwable> seen) {
    if (cause == null) {
      return null;
    }
    Throwable existing = seen.get(cause);
    if (existing != null) {
      return existing;
    }
    String type = cause.getClass().getName();
    String scrubbedMessage = LogPiiScrubber.scrub(cause.getMessage());
    Throwable logged =
        new Throwable(
            scrubbedMessage == null || scrubbedMessage.isBlank()
                ? type
                : type + ": " + scrubbedMessage);
    seen.put(cause, logged);
    logged.setStackTrace(cause.getStackTrace());
    Throwable scrubbedCause = scrubChain(cause.getCause(), seen);
    if (scrubbedCause != null) {
      logged.initCause(scrubbedCause);
    }
    for (Throwable suppressed : cause.getSuppressed()) {
      logged.addSuppressed(scrubChain(suppressed, seen));
    }
    return logged;
  }
}
