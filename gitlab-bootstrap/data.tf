# The `insurance` parent group is a PREREQUISITE, never a managed resource.
#
# IMP-11 #3: import the parent, never create it. Reading it as a data source is
# stronger than importing it — a data source CANNOT appear under `create` or
# `destroy` in any plan, so the failure mode is structurally impossible rather
# than merely guarded against.
#
# If this lookup fails, the estate is not safe to provision and the plan should
# stop. That is the intended behaviour.
data "gitlab_group" "insurance" {
  group_id = var.parent_group_id
}
