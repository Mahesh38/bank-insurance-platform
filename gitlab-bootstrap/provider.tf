# ---------------------------------------------------------------------------
# GitLab provider.
#
# Authentication (baseline section 5.1, condition C-SEC-5):
#   - a DEDICATED least-privileged automation service account, never an
#     individual employee personal access token;
#   - DISTINCT from the application deployment identity — two identities, two
#     blast radii;
#   - injected as GITLAB_TOKEN in the environment, never committed, never
#     written to a tfvars file, never logged.
#
# The token is deliberately NOT a Terraform variable. A variable can be set in
# a .tfvars file, and a .tfvars file can be committed by accident. An
# environment variable cannot be committed by accident.
# ---------------------------------------------------------------------------

provider "gitlab" {
  base_url = var.gitlab_base_url

  # token is read from the GITLAB_TOKEN environment variable.
  # Do not add `token = ...` here, and do not add a token variable.
}
