resource "gitlab_project" "this" {
  name             = var.name
  path             = var.path
  namespace_id     = var.namespace_id
  description      = var.description
  visibility_level = "private" # baseline section 14 — all projects private

  # IMP-11 #1 — MANDATORY false for projects receiving migrated history.
  # A generated README commit forks the history before the mirror lands.
  initialize_with_readme = var.initialize_with_readme

  # IMP-11 #2 — do not set a default branch before the branch exists.
  # Terraform will protect a non-existent branch and then drift on every plan.
  default_branch = var.set_default_branch ? var.default_branch : null

  container_registry_enabled = var.container_registry
  packages_enabled           = var.packages

  # S08-G2 mechanism. IMP-4: CE has no named-required-check analogue, so the gate
  # is one required pipeline with every gating job allow_failure:false. This
  # setting is the CE-available half and it is what makes the gate real.
  only_allow_merge_if_pipeline_succeeds = var.pipeline_must_succeed

  # Unresolved discussions block merge — baseline section 6.2.
  only_allow_merge_if_all_discussions_are_resolved = true

  remove_source_branch_after_merge = true
  squash_option                    = "default_off"

  lifecycle {
    prevent_destroy = true
  }
}
