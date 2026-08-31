# Baseline section 11.5: expose useful NON-SENSITIVE outputs. Never credentials.

output "group_ids" {
  description = "Group id by name."
  value       = merge({ "bank-insurance" = module.group_root.id }, { for k, m in module.group_sub : k => m.id })
}

output "project_ids" {
  description = "Project id by name."
  value       = { for k, m in module.project : k => m.id }
}

output "project_urls" {
  description = "Web URL by project name."
  value       = { for k, m in module.project : k => m.web_url }
}

output "clone_urls" {
  description = "HTTPS clone URL by project name. No credentials — the URL only."
  value       = { for k, m in module.project : k => m.http_url_to_repo }
}

output "project_count" {
  description = "Projects this configuration manages. Eight until AC-2 lands, nine after."
  value       = length(local.projects)
}

output "platform_governance_created" {
  description = "AC-2 gate state. False until the bank accepts the Appendix C exception in writing."
  value       = var.platform_governance_enabled
}

output "unavailable_controls" {
  description = <<-EOT
    Controls the approved governance model wants that this instance cannot enforce.
    On CE this is non-empty by design (CR-016). Surfaced so the gap is reportable
    rather than silently absent — C-ARC-6.
  EOT
  value       = local.unavailable_controls
}

output "edition" {
  description = "Licence tier the capability flags were derived from."
  value       = var.gitlab_edition
}

output "governance_applied" {
  description = "Whether branch protection and environments have been applied. False until M6.1."
  value       = var.apply_governance
}

output "control_gap" {
  description = <<-EOT
    The C-ARC-6 report: what the approved model declares versus what this instance
    applies. On CE the applied counts are lower by design, and that gap is the
    thing CR-016 has to decide about — visible here rather than absent from the code.
  EOT
  value = var.apply_governance ? {
    approval_rules_declared = try(values(module.branch_governance)[0].approval_rules_declared, 0)
    approval_rules_applied  = try(values(module.branch_governance)[0].approval_rules_applied, 0)
    environments_protected_declared = try(values(module.environments)[0].protected_declared, 0)
    environments_protected_applied  = try(values(module.environments)[0].protected_applied, 0)
  } : null
}
