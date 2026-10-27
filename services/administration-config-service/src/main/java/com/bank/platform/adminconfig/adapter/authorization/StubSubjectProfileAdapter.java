package com.bank.platform.adminconfig.adapter.authorization;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.adminconfig.domain.SubjectProfilePort;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local/test profile store so checker roles are not taken from request headers. Tests seed roles
 * via {@link #seed(String, Set)}.
 */
@Component
@ConditionalOnProperty(
    name = "admin-config.authorization.mode",
    havingValue = "stub",
    matchIfMissing = true)
public class StubSubjectProfileAdapter implements SubjectProfilePort {

  private final Map<String, SubjectProfile> profiles = new ConcurrentHashMap<>();
  private final ServiceErrors errors;

  public StubSubjectProfileAdapter(ServiceErrors errors) {
    this.errors = errors;
  }

  public void seed(String businessUserId, String userType, Set<String> roleCodes) {
    profiles.put(businessUserId, new SubjectProfile(userType, roleCodes));
  }

  public void clear() {
    profiles.clear();
  }

  @Override
  public SubjectProfile load(String businessUserId) {
    SubjectProfile profile = profiles.get(businessUserId);
    if (profile == null) {
      throw errors
          .error(ErrorCodes.FORBIDDEN)
          .component("StubSubjectProfileAdapter")
          .operation("load")
          .reason("no authoritative roles for actor")
          .build();
    }
    return profile;
  }
}
