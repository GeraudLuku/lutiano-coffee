output "load_balancer_public_ip" {
  value = oci_core_public_ip.nlb.ip_address
}

output "bastion_id" {
  value = oci_bastion_bastion.main.id
}

output "vm_private_ips" {
  value = { for name, vm in oci_core_instance.vm : name => vm.private_ip }
}

output "storage_endpoint" {
  value = "https://${data.oci_objectstorage_namespace.ns.namespace}.compat.objectstorage.${var.region}.oraclecloud.com"
}

output "storage_buckets" {
  value = sort(keys(oci_objectstorage_bucket.files))
}