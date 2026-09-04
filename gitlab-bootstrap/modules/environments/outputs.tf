output "environments" {
  description = "Environments created."
  value       = sort([for e in var.environments : e.name])
}

output "protected_applied" {
  description = "Environments actually protected. Zero on CE by design — CR-016 decides the compensating control."
  value       = var.capabilities.protected_environments ? length([for e in var.environments : e if e.protected]) : 0
}

output "protected_declared" {
  description = "Environments the approved model says must be protected, regardless of tier."
  value       = length([for e in var.environments : e if e.protected])
}
