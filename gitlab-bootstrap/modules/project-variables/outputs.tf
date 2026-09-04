output "keys" { value = sort([for v in var.variables : v.key]) }
