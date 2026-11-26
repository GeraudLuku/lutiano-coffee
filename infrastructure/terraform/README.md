# LUTIANO infrastructure on Oracle Cloud (Terraform)

This folder describes the LUTIANO servers and network on Oracle Cloud Infrastructure (OCI). Terraform reads these files, compares them with what exists in Oracle, and shows what it would create or change. Nothing changes until you run `apply`.

## What it builds

```
Internet ──► Network load balancer (public IP, ports 80 and 443)
                     │            public subnet 10.0.0.0/24
─────────────────────┼──────────────────────────────────────
                     ▼            private subnet 10.0.1.0/24 (no public IPs)
            lutiano-1  10.0.1.10        lutiano-2  10.0.1.11
                     │
                     └──► NAT gateway ──► Internet (updates, images; outbound only)

You ──► Oracle Bastion ──► SSH to the private VMs
```

| Resource | Purpose |
|---|---|
| VCN `lutiano-vcn` (`10.0.0.0/16`) | The private network |
| Public subnet | Holds only the load balancer |
| Private subnet | Holds both VMs; Oracle refuses public IPs there |
| Network load balancer + reserved public IP | The only entry from the internet (TCP 80 and 443, passed straight to the VMs) |
| NAT gateway | Lets the VMs reach the internet; nothing can come in through it |
| 2 VMs (ARM, `VM.Standard.A1.Flex`) | `lutiano-1` and `lutiano-2`; sizes are set in `variables.tf` |
| Bastion | Managed SSH access to the private VMs |

### Files

| File | Content | Committed? |
|---|---|---|
| `providers.tf` | Terraform and provider versions, where the state is stored, Oracle login | yes |
| `variables.tf` | Inputs, and the VM sizes and addresses (`vms`) | yes |
| `network.tf` | VCN, gateways, route tables, firewalls (security lists), subnets | yes |
| `compute.tf` | The two VMs | yes |
| `loadbalancer.tf` | Reserved IP, load balancer, listeners, backends | yes |
| `bastion.tf` | The Bastion service | yes |
| `outputs.tf` | Load balancer IP, bastion id, VM private IPs | yes |
| `terraform.tfvars.example` | Template for your own settings | yes |
| `backend.hcl.example` | Template for the state bucket keys | yes |
| `.terraform.lock.hcl` | Pins the provider version | yes |
| **`terraform.tfvars`** | Your Oracle ids and region | **never** |
| **`backend.hcl`** | The keys that open the state bucket | **never** |
| **`*.tfstate`, `tfplan`, `.terraform/`** | Terraform's working data | **never** |

## Before you start

You need:

- **Terraform** 1.6 or later: `brew tap hashicorp/tap && brew install hashicorp/tap/terraform`
- **OCI command line**: `brew install oci-cli`
- An **Oracle Cloud account** and its console access
- An **SSH key** on your computer (default: `~/.ssh/id_ed25519`, public half `~/.ssh/id_ed25519.pub`). Create one with `ssh-keygen -t ed25519` if needed.

### Two network rules that cause most problems

1. **Raw SSH is blocked on many company networks.** The Bastion tunnel (below) needs a network that allows outgoing SSH, such as mobile data. Terraform and the `oci` command only use HTTPS and work on most networks.
2. **Company proxy variables.** If your shell exports `HTTP_PROXY` or `HTTPS_PROXY` (check with `env | grep -i proxy`), Terraform sends its requests through that proxy. Away from the company network the proxy refuses them with `connection reset by peer`. Clear them first:
   ```bash
   unset HTTP_PROXY HTTPS_PROXY NO_PROXY http_proxy https_proxy no_proxy
   ```
   This only affects the current terminal window.

## One-time setup

### 1. Log in to Oracle and create a profile

Terraform and the `oci` command log in with a temporary browser session, saved as a **profile** in `~/.oci/config`. This folder uses a profile named `lutiano-terraform` (see `providers.tf`).

```bash
oci session authenticate --region <your-region> --profile lutiano-terraform
```

- It opens your browser: sign in to Oracle and approve.
- It saves the session and adds the `[lutiano-terraform]` profile to `~/.oci/config`.
- Use a profile name of your own if you prefer, but then change `config_file_profile` in `providers.tf` and `PROFILE` in `../bastion.sh` to the same name. Avoid the name `DEFAULT`, which may already hold other credentials.

Check that it works:

```bash
oci iam region list --profile lutiano-terraform --auth security_token --output table
```

**Sessions expire after about an hour.** Extend one with:

```bash
oci session refresh --profile lutiano-terraform
```

A session can only be refreshed for a limited time (about a day). After that, run `oci session authenticate` again. A `401 NotAuthenticated` error from Terraform or `oci` almost always means the session has expired.

Check how much time is left, without contacting Oracle:

```bash
oci session validate --profile lutiano-terraform --local
```

### 2. The state bucket

Terraform keeps a record of what it manages (the **state**). It is stored in an Oracle Object Storage bucket, so it survives a lost laptop and can be shared. Terraform cannot create this bucket for itself, so you create it once by hand.

In the Oracle console:

1. Storage, Buckets, **Create bucket**: private, Standard storage, with **versioning enabled** (keeps older copies of the state). Note its name.
2. Your profile (top right), **My profile**, **Customer secret keys**, **Generate**. Copy the **secret** immediately (it is shown once), and the access key id.
3. Find your **namespace**: `oci os ns get --profile lutiano-terraform --auth security_token`

Then edit `providers.tf`, in the `backend "s3"` block, with your own values:

- `bucket`: the bucket name
- `region`: your region, for example `eu-frankfurt-1`
- `endpoints.s3`: `https://<namespace>.compat.objectstorage.<region>.oraclecloud.com`

A backend cannot use variables, so these must be written out. None of them is secret.

Create `backend.hcl` (from `backend.hcl.example`) with the two keys:

```hcl
access_key = "<customer secret key id>"
secret_key = "<customer secret key>"
```

### 3. Your settings

Copy the template and fill it in:

```bash
cp terraform.tfvars.example terraform.tfvars
```

```hcl
region           = "<your-region>"
tenancy_ocid     = "<your tenancy ocid>"
compartment_ocid = "<the compartment ocid to use>"
```

Find them in the console: your profile, **Tenancy** (the tenancy OCID); Identity, **Compartments** (a compartment's OCID). The root compartment uses the same OCID as the tenancy.

### 4. The SSH key for the VMs

Terraform installs the **public** key from `~/.ssh/id_ed25519.pub` on both VMs (change `ssh_public_key_path` in `variables.tf` to use another file). If you only have a private key, create its public half with:

```bash
ssh-keygen -y -f ~/.ssh/id_ed25519 > ~/.ssh/id_ed25519.pub
```

## Running Terraform

Always work from this folder, with a valid Oracle session.

```bash
cd infrastructure/terraform
oci session refresh --profile lutiano-terraform

terraform init -backend-config=backend.hcl   # first time, or after changing the backend
terraform fmt                                # tidy the formatting
terraform validate                           # check the files for mistakes
terraform plan -out=tfplan                   # preview, and save exactly that plan
terraform apply tfplan                       # make the changes in Oracle
```

- `init` downloads the Oracle provider and connects to the state bucket.
- `plan` creates nothing. Read it: `+` is a create, `~` a change in place, `-/+` a **destroy and recreate**, `-` a destroy. Stop and ask before applying anything with `-` or `-/+` that you did not expect.
- `apply tfplan` carries out exactly the plan you reviewed. If the real situation changed since, Terraform refuses to apply it.
- Keep `tfplan` out of Git. It can contain sensitive values.
- If Oracle's load balancer reports a "409 conflict", run `terraform apply -parallelism=1 tfplan` to create things one at a time.

### Useful commands

```bash
terraform output                  # load balancer IP, bastion id, VM private IPs
terraform state list              # everything Terraform manages
terraform plan                    # "No changes" means reality matches the files
```

### Changing things

- **VM sizes, disks and private IPs**: the `vms` map in `variables.tf`. Adding an entry creates another VM. Check Oracle's free-tier limits first (the account this was built for allows 2 CPUs, 12 GB of memory and 200 GB of block storage in total).
- **Firewall rules**: `network.tf` (security lists).
- **Deleting**: the VMs and the reserved IP are the resources that matter. Review a destroy plan carefully, and never run `terraform destroy` unless you mean it.

## Connecting to a VM through the Bastion

The VMs have **no public IP**. You reach them through Oracle's Bastion service: it creates a temporary session (up to 3 hours), and an SSH tunnel on your computer carries your connection through it.

```
your computer :2201 ──tunnel──► Bastion ──► lutiano-1 (10.0.1.10) port 22
your computer :2202 ──tunnel──► Bastion ──► lutiano-2 (10.0.1.11) port 22
```

Requirements: a valid Oracle session (step 1), a network that allows outgoing SSH (mobile data), and the proxy variables cleared (see above).

### With the script (recommended)

The script is `../bastion.sh`, one folder up, because Ansible uses it too. From this folder:

```bash
../bastion.sh up        # create the sessions and open all tunnels
../bastion.sh status    # which tunnels are open
../bastion.sh ssh 1     # log in to lutiano-1   (use 2 for lutiano-2)
../bastion.sh down      # close all tunnels
```

- It opens three tunnels: SSH to `lutiano-1` (local port 2201), SSH to `lutiano-2` (2202), and the Kubernetes API of `lutiano-1` (6443, for `kubectl` and `helm` from your computer).
- It works from any folder. It reads `terraform/terraform.tfvars` and the Terraform state itself.
- The script refreshes your Oracle session, creates one bastion session per VM, and opens a tunnel for each in the background. Tunnels that are already open are skipped.
- Sessions last **3 hours**. When they expire, run `../bastion.sh down` and then `../bastion.sh up`.
- The ports, names and addresses are listed at the top of the script (`VMS=...`).
- Running `../bastion.sh` with no argument prints a short help.

### By hand

Create a session for a VM (here `lutiano-1`):

```bash
oci bastion session create-port-forwarding \
  --bastion-id "$(terraform output -raw bastion_id)" \
  --target-private-ip 10.0.1.10 \
  --target-port 22 \
  --ssh-public-key-file ~/.ssh/id_ed25519.pub \
  --session-ttl 10800 \
  --display-name lutiano-1 \
  --wait-for-state SUCCEEDED \
  --profile lutiano-terraform --auth security_token
```

The session id is under `data.resources[0].identifier` in the output (it starts with `ocid1.bastionsession`). The `data.id` field at the top is a different thing (the work request).

Open the tunnel in the background, then log in:

```bash
ssh -f -N -L 2201:10.0.1.10:22 -p 22 <session id>@host.bastion.<your-region>.oci.oraclecloud.com
ssh -p 2201 ubuntu@127.0.0.1
```

For `lutiano-2`, use `10.0.1.11` and local port `2202`. Close a tunnel with `lsof -ti :2201 | xargs kill`.

## Troubleshooting

| What you see | Cause | What to do |
|---|---|---|
| `401 NotAuthenticated` | The Oracle session expired, or the wrong profile | `oci session refresh --profile lutiano-terraform`, or authenticate again; check `config_file_profile` in `providers.tf` |
| `connection reset by peer`, with a `146.112...` address | Company proxy variables are set and you are off the company network | `unset HTTP_PROXY HTTPS_PROXY NO_PROXY http_proxy https_proxy no_proxy` |
| `No valid credential sources found` (on `init`) | `init` was run without the keys file | `terraform init -backend-config=backend.hcl` |
| `NoSuchBucket` | Wrong bucket name, region or namespace in `providers.tf`, or no access to the bucket | Check the three values; list buckets with `oci os bucket list` |
| `Variables not allowed` in `providers.tf` | A variable used in the backend block | Write the real value; a backend cannot read variables |
| `Duplicate resource` | The same resource is defined twice in a file | Remove the duplicate; one resource per name |
| `Out of host capacity` when creating a VM | Oracle has no free ARM capacity in that availability domain | Set `availability_domain_index` (1 or 2) in `terraform.tfvars`, plan and apply again, or retry later |
| `LimitExceeded` | A free-tier limit is used up, or an old VM or boot volume still exists | Console, Limits, Quotas and Usage; delete leftovers |
| Tunnel command hangs | The network blocks outgoing SSH | Use mobile data, and turn off the office connection |
| `Address already in use` (port 2201 or 2202) | A tunnel is already open | `../bastion.sh status`, or `../bastion.sh down` first |
| `Connection refused` on `127.0.0.1:2201` | No tunnel is open | `../bastion.sh up` |
| `Permission denied (publickey)` on the tunnel | The bastion session was created with a different key | Use the same public key for the session and for the VMs |

## Costs and limits

The design stays within Oracle's Always Free tier for compute, one network load balancer, the NAT gateway, the Bastion and Object Storage within limits, but limits and prices change. Check **Console, Limits, Quotas and Usage** before applying, and keep a budget alert on the account.
