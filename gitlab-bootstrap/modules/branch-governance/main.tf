# Baseline sections 6.2 and 6.3.
#
# SEQUENCING (baseline section 7, IMP-11 #2): this module must NOT run before the
# branch exists. It is invoked at M6.1, after M5 has pushed history. Terraform
# will happily protect a non-existent branch and then drift on every plan.
#
# C-ARC-6: the approval model is expressed in full. `capabilities` decides what is
# applied. On CE the EE resources are count=0 — declared and skipped, not deleted.

# --- CE-available: real protection on this instance today --------------------
resource "gitlab_branch_protection" "main" {
  project            = var.project_id
  branch             = var.protection.branch
  allow_force_push   = var.protection.allow_force_push
  push_access_level  = var.protection.push_access_level
  merge_access_level = var.protection.merge_access_level

  # Premium+ only. On CE this must stay false or the API rejects the request.
  code_owner_approval_required = var.capabilities.code_owner_approval ? var.protection.code_owner_approval_required : false
}

# --- Premium+: declared, applied only where the tier allows ------------------
resource "gitlab_project_level_mr_approvals" "this" {
  count = var.capabilities.merge_request_approval_rules ? 1 : 0

  project                                        = var.project_id
  reset_approvals_on_push                        = true
  disable_overriding_approvers_per_merge_request = true
  merge_requests_author_approval                 = false
}

resource "gitlab_project_approval_rule" "this" {
  for_each = var.capabilities.merge_request_approval_rules ? { for r in var.approval_rules : r.name => r } : {}

  project            = var.project_id
  name               = each.value.name
  approvals_required = each.value.approvals_required

  depends_on = [gitlab_project_level_mr_approvals.this]
}
