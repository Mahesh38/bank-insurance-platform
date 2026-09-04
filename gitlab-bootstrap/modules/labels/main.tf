resource "gitlab_group_label" "this" {
  for_each = { for l in var.labels : l.name => l }

  group       = var.group_id
  name        = each.value.name
  color       = each.value.color
  description = try(each.value.description, "")
}
