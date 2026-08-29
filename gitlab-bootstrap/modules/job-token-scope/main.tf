# CI_JOB_TOKEN allowlist — IMP-9, C-OPS-5.
#
# Allows the target project to be accessed by job tokens originating from each
# project in `allowed_project_ids`. Applied at M6.8, BEFORE the first consuming
# pipeline runs. Applying it reactively means discovering the gap after cutover,
# when the GitHub origin is already read-only.
resource "gitlab_project_job_token_scope" "this" {
  for_each = toset([for id in var.allowed_project_ids : tostring(id)])

  project             = var.target_project_id
  target_project_id   = tonumber(each.value)
}
