package com.bank.platform.customer.adapter.apigee;

import com.bank.common.apigee.ApigeeAccessTokenHolder;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.customer.domain.AccessTokenPort;
import java.util.Objects;

/**
 * Process-local Apigee token port. Mint failures become catalogue {@code UPSTREAM_UNAVAILABLE} so
 * the HTTP boundary records them (OBS-1 / OBS-9) instead of a bare {@code IllegalStateException}
 * 500. Messages never include the token, secret, or URI (OBS-2, OBS-4).
 */
public final class HolderAccessTokenPort implements AccessTokenPort {

  private final ApigeeAccessTokenHolder holder;
  private final ServiceErrors errors;

  public HolderAccessTokenPort(ApigeeAccessTokenHolder holder, ServiceErrors errors) {
    this.holder = Objects.requireNonNull(holder, "holder");
    this.errors = Objects.requireNonNull(errors, "errors");
  }

  @Override
  public String currentAccessToken() {
    try {
      return holder.currentAccessToken();
    } catch (ServiceException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw errors
          .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
          .component("HolderAccessTokenPort")
          .operation("currentAccessToken")
          .reason("Apigee token mint failed")
          .cause(ex)
          .build();
    }
  }

  @Override
  public void invalidate() {
    holder.invalidate();
  }
}
