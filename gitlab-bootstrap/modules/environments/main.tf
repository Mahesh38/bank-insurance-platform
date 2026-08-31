# Baseline section 9.3.
#
# C-ARC-6: environments are CE-available and always created. Protection is
# Premium+ and is declared here, applied only where the tier allows. On CE, PROD
# and DR protection falls to whatever compensating control CR-016 decides — a
# manual job on a restricted runner. That is NOT equivalent and must never be
# recorded as if it were.

resource "gitlab_project_environment" "this" {
  for_each = { for e in var.environments : e.name => e }

  project = var.project_id
  name    = each.value.name

  # Stopping an environment before its deployments are gone loses the deployment
  # record, and the deployment record is audit evidence.
  stop_before_destroy = false
}

resource "gitlab_project_protected_environment" "this" {
  for_each = var.capabilities.protected_environments ? { for e in var.environments : e.name => e if e.protected } : {}

  project     = var.project_id
  environment = gitlab_project_environment.this[each.key].name

  deploy_access_levels {
    access_level = "maintainer"
  }

  required_approval_count = each.value.approvals_required
}
