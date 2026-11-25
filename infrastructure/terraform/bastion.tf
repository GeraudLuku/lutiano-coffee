resource "oci_bastion_bastion" "main" {
  bastion_type                 = "STANDARD"
  compartment_id               = var.compartment_ocid
  target_subnet_id             = oci_core_subnet.private.id
  name                         = "lutianobastion"
  client_cidr_block_allow_list = [var.bastion_allowed_cidr]
  max_session_ttl_in_seconds   = 10800
}