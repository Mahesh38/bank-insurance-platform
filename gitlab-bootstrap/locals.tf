# ---------------------------------------------------------------------------
# Configuration is DATA. Adding a project is a YAML change, not new HCL.
# Baseline section 4.2; the future-change test is section 16.1.
# ---------------------------------------------------------------------------

locals {
  groups       = yamldecode(file("${path.module}/config/groups.yaml")).groups
  projects_all = yamldecode(file("${path.module}/config/projects.yaml")).projects
  labels       = yamldecode(file("${path.module}/config/labels.yaml")).labels
  job_tokens   = yamldecode(file("${path.module}/config/job-token-scope.yaml")).allowlists
  branch_gov   = yamldecode(file("${path.module}/config/branch-governance.yaml"))
  environments = yamldecode(file("${path.module}/config/environments.yaml")).environments

  # AC-2 gate. platform-governance is filtered out unless the bank exception is in.
  projects = {
    for name, cfg in local.projects_all : name => cfg
    if name != "platform-governance" || var.platform_governance_enabled
  }

  # -------------------------------------------------------------------------
  # C-ARC-6 — capability flags.
  #
  # The modules express the APPROVED governance model regardless of tier. These
  # flags decide what is actually applied against this instance, so the gap is
  # visible in code rather than absent from it, and a licence upgrade is a flag
  # change rather than a redesign.
  #
  # Verified against GitLab documentation 2026-08-29; MUST be re-verified against
  # the instance itself (DEC-20260829-02 section 3, checks 1-5). v19.1.2 is newer
  # than the reachable documentation and the instance is authoritative.
  # -------------------------------------------------------------------------
  ee = contains(["premium", "ultimate"], var.gitlab_edition)

  capabilities = {
    # Free / CE — the model survives on these.
    protected_branches         = true
    pipeline_must_succeed      = true # S08-G2 depends on this one
    container_registry         = true # confirmed by the bank 2026-08-29
    job_token_scope            = true
    cicd_variables             = true

    # Premium and above — absent on CE. CR-016 decides the compensating control.
    merge_request_approval_rules = local.ee
    code_owner_approval          = local.ee
    protected_environments       = local.ee
    push_rules                   = local.ee
    group_protected_branches     = local.ee

    # Ultimate only.
    security_policies          = var.gitlab_edition == "ultimate"
    vulnerability_management   = var.gitlab_edition == "ultimate"
  }

  # Capabilities the approved model wants but this instance cannot enforce.
  # Surfaced as an output so the gap is reportable rather than silent.
  unavailable_controls = [
    for k, v in local.capabilities : k if v == false
  ]

  common_tags = {
    managed_by = "gitlab-bootstrap"
    workstream = "WS-3"
    change     = "CR-014"
  }
}
