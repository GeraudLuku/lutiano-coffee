variable "region" { type = string }
variable "tenancy_ocid" { type = string }
variable "compartment_ocid" { type = string }

variable "ssh_public_key_path" {
  type    = string
  default = "~/.ssh/id_ed25519.pub"
}

variable "availability_domain_index" {
  type    = number
  default = 0
}

variable "bastion_allowed_cidr" {
  type    = string
  default = "0.0.0.0/0"
}

variable "vms" {
  type = map(object({
    ocpus               = number
    memory_in_gbs       = number
    boot_volume_size_gb = number
    private_ip          = string
  }))
  default = {
    "lutiano-1" = { ocpus = 1, memory_in_gbs = 6, boot_volume_size_gb = 100, private_ip = "10.0.1.10" }
    "lutiano-2" = { ocpus = 1, memory_in_gbs = 6, boot_volume_size_gb = 100, private_ip = "10.0.1.11" }
  }
}