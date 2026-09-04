rootProject.name = "1sb-insurance-platform"

include(
    "libs:bank-common-error",
    "libs:bank-common-domain",
    "libs:bank-common-security",
    "libs:bank-common-audit",
    "libs:bank-common-observability",
    "services:1sb-integration-service",
    "libs:bank-common-secrets",
    "services:bank-persistence-service",
    "services:identity-provider-adapter-service",
    "services:identity-authorization-service",
    "services:workforce-access-bff"
)

