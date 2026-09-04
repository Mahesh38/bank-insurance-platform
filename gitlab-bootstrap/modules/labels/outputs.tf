output "names" { value = sort([for l in var.labels : l.name]) }
output "count" { value = length(var.labels) }
