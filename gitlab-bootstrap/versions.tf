# ---------------------------------------------------------------------------
# Terraform and provider version constraints.
#
# Baseline section 4.3: "Pin the GitLab provider to an explicitly tested version
# range; do not float on latest. Commit the dependency lock file."
#
# !! THE CONSTRAINT BELOW IS PROVISIONAL AND HAS NOT BEEN TESTED. !!
#
# It cannot be tested from the authoring environment: no Terraform binary is
# installed and registry.terraform.io is not reachable through the egress proxy.
# Writing a number here and calling it "tested" would be false.
#
# GLM-001 M3.11 is the point at which this becomes a real pin:
#   1. terraform init against https://gitlab-ce.au.bank.in/
#   2. record the resolved provider version
#   3. narrow the constraint to that version
#   4. commit .terraform.lock.hcl  <- the lock file is the actual control
#
# Until M3.11 has run, treat this file as a placeholder that happens to parse.
# ---------------------------------------------------------------------------

terraform {
  required_version = ">= 1.6.0"

  required_providers {
    gitlab = {
      source = "gitlabhq/gitlab"

      # PROVISIONAL — see the header. Narrow at M3.11.
      version = ">= 17.0.0, < 19.0.0"
    }
  }
}
