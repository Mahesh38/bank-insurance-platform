package com.bank.insurance.onesb;

import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;

/**
 * The service identity unit tests construct their collaborators with.
 *
 * <p>Production gets this from {@code bank.error.*}; a unit test that builds a service directly
 * needs the same thing without a Spring context. One constant, so the identity is written down
 * once in the test tree too.
 */
public final class TestErrors {

    public static final ServiceErrors ONESB = ServiceErrors.of("onesb", PlatformLayer.L5);

    private TestErrors() {}
}
