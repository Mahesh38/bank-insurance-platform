resource "gitlab_group" "this" {
  name             = var.name
  path             = var.path
  parent_id        = var.parent_id
  description      = var.description
  visibility_level = var.visibility

  # Baseline section 11.2 — destructive-change protection.
  # A normal refactor must never silently delete a group. Removing this guard is
  # a deliberate, reviewed act; that is the point of it being here.
  lifecycle {
    prevent_destroy = true
  }
}
