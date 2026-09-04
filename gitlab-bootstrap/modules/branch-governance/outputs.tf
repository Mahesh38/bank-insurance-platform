output "protected_branch" {
  description = "The branch actually protected."
  value       = gitlab_branch_protection.main.branch
}

output "approval_rules_applied" {
  description = "Approval rules applied. Zero on CE by design — declared and skipped, not deleted."
  value       = var.capabilities.merge_request_approval_rules ? length(var.approval_rules) : 0
}

output "approval_rules_declared" {
  description = "Approval rules the approved model defines, regardless of tier."
  value       = length(var.approval_rules)
}
