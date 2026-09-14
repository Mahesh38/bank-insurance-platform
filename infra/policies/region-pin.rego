# S09-G9 / S09-E01-S06 — refuse any Terraform plan that creates resources outside India regions.
package terraform.region_pin

deny[msg] {
  rc := input.resource_changes[_]
  rc.change.actions[_] != "delete"
  region := rc.change.after.region
  not india_region(region)
  msg := sprintf("S09 residency: %s region %v is not ap-south-1/ap-south-2", [rc.address, region])
}

india_region(r) = true { r == "ap-south-1" }
india_region(r) = true { r == "ap-south-2" }
india_region(r) = true { r == null } # regionless resources (IAM, etc.) ok at this check
