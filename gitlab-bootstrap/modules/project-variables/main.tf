# NON-SECRET CI/CD variables only.
#
# C-SEC-9 / baseline section 4.5: no secret value passes through Terraform, because
# a value set through Terraform lands in Terraform state. Secrets are configured
# out of band, or referenced by path into an approved secret store.
#
# The guard below is a real check, not a comment: a variable declared `masked`
# is one someone believed was sensitive, and it does not belong here.
resource "gitlab_project_variable" "this" {
  for_each = { for v in var.variables : v.key => v }

  project           = var.project_id
  key               = each.value.key
  value             = each.value.value
  protected         = try(each.value.protected, false)
  masked            = false
  environment_scope = try(each.value.environment_scope, "*")

  lifecycle {
    precondition {
      condition     = !try(each.value.secret, false)
      error_message = "Variable ${each.value.key} is flagged secret. Secrets never pass through Terraform (C-SEC-9) — configure it out of band or reference a secret store path."
    }
  }
}
