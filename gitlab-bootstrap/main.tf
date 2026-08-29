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
