# ---------------------------------------------------------------------------
# Root composition. Modules take DATA from config/*.yaml; adding a project is a
# YAML change reviewed through an MR, then plan, then approved apply.
# Baseline section 16.1 — the future-change test.
# ---------------------------------------------------------------------------

# --- Groups -----------------------------------------------------------------
# Two passes, because a subgroup needs its parent's id. bank-insurance hangs off
# the imported `insurance` parent; everything else hangs off bank-insurance.

module "group_root" {
  source = "./modules/gitlab-group"

  name        = "bank-insurance"
  path        = local.groups["bank-insurance"].path
  parent_id   = data.gitlab_group.insurance.group_id
  description = local.groups["bank-insurance"].description
  visibility  = local.groups["bank-insurance"].visibility
}

module "group_sub" {
  source   = "./modules/gitlab-group"
  for_each = { for k, v in local.groups : k => v if v.parent == "bank-insurance" }

  name        = each.key
  path        = each.value.path
  parent_id   = module.group_root.id
  description = each.value.description
  visibility  = each.value.visibility
}

# --- Projects ---------------------------------------------------------------

module "project" {
  source   = "./modules/gitlab-project"
  for_each = local.projects

  name         = each.key
  path         = each.key
  namespace_id = module.group_sub[each.value.group].id
  description  = each.value.description

  # IMP-11 #1 and #2 — both driven from data, both false for migrated history.
  initialize_with_readme = each.value.initialize_with_readme
  set_default_branch     = each.value.set_default_branch
  default_branch         = var.default_branch

  container_registry = each.value.container_registry
  packages           = each.value.packages

  # S08-G2 — CE-available, never gated behind an edition flag.
  pipeline_must_succeed = local.capabilities.pipeline_must_succeed
}

# --- Labels -----------------------------------------------------------------
# Applied once at bank-insurance; every project below inherits them.

module "labels" {
  source = "./modules/labels"

  group_id = module.group_root.id
  labels   = local.labels
}

# --- Job-token allowlists ---------------------------------------------------
# IMP-9. Modelled here; applied at M6.8 before the first consuming pipeline.

module "job_token_scope" {
  source   = "./modules/job-token-scope"
  for_each = { for k, v in local.job_tokens : k => v if contains(keys(local.projects), v.target) }

  target_project_id   = module.project[each.value.target].id
  allowed_project_ids = [
    for p in each.value.allowed : module.project[p].id
    if contains(keys(local.projects), p)
  ]
}

# --- Branch governance ------------------------------------------------------
# M6.1 ONLY. var.apply_governance is false through M4 because baseline section 7
# forbids protecting a branch that does not exist yet (IMP-11 #2).

module "branch_governance" {
  source   = "./modules/branch-governance"
  for_each = var.apply_governance ? local.projects : {}

  project_id = module.project[each.key].id

  protection = {
    branch                       = local.branch_gov.main_protection.branch
    allow_force_push             = local.branch_gov.main_protection.allow_force_push
    push_access_level            = local.branch_gov.main_protection.push_access_level
    merge_access_level           = local.branch_gov.main_protection.merge_access_level
    code_owner_approval_required = local.branch_gov.main_protection.code_owner_approval_required
  }

  # Rules that name this project, plus the ones that apply to all of them.
  approval_rules = [
    for r in local.branch_gov.approval_rules : {
      name               = r.name
      approvals_required = r.approvals_required
    }
    if r.applies_to == "all" || (can(tolist(r.applies_to)) && contains(tolist(r.applies_to), each.key))
  ]

  capabilities = {
    merge_request_approval_rules = local.capabilities.merge_request_approval_rules
    code_owner_approval          = local.capabilities.code_owner_approval
  }
}

# --- Environments -----------------------------------------------------------
# Only the projects that actually deploy. Baseline section 9.3.

module "environments" {
  source   = "./modules/environments"
  for_each = var.apply_governance ? { for k, v in local.projects : k => v if contains(["backend", "frontend", "gitops"], k) } : {}

  project_id   = module.project[each.key].id
  environments = local.environments

  capabilities = {
    protected_environments = local.capabilities.protected_environments
  }
}
