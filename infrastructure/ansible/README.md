# LUTIANO servers configured with Ansible

Terraform creates the two empty servers. This folder turns them into a small Kubernetes cluster (k3s). Ansible logs in over SSH and makes each server match what these files describe. Running it twice is safe: it only changes what is different.

## What it does

```
Your Mac ──► bastion tunnels ──► lutiano-1  (k3s server, runs the cluster)
             127.0.0.1:2201      10.0.1.10
             127.0.0.1:2202   ──► lutiano-2  (k3s agent, joins lutiano-1)
                                 10.0.1.11
```

The servers have no public IP, so Ansible reaches them through the Oracle Bastion. `infrastructure/bastion.sh` opens two local tunnels, and the inventory points at those local ports.

## Files

| File | Purpose |
|---|---|
| `ansible.cfg` | Defaults: which inventory to use, no host-key prompts (both servers appear as `127.0.0.1`), faster SSH |
| `inventory.yml` | The servers, how to reach them, and shared settings (login user, SSH key, k3s version) |
| `site.yml` | Runs the five playbooks below in order |
| `base.yml` | Both servers: updates, base packages, hostname, timezone |
| `security.yml` | Both servers: no SSH passwords, no root login, automatic security updates |
| `firewall.yml` | Both servers: allows all traffic from the private network `10.0.0.0/16` and saves the rule |
| `k3s.yml` | `lutiano-1`: installs the k3s server and waits until it is Ready |
| `k3s-agent.yml` | `lutiano-2`: reads the join token from `lutiano-1` and joins the cluster |

No secrets are stored here. The k3s join token is read from the server while the playbook runs and is hidden from the output.

## Requirements

- Ansible on your Mac: `brew install ansible`
- The Terraform setup is applied and the VMs exist (see `../terraform/README.md`)
- A network that allows outbound SSH. Some office networks block it; use another network or mobile data
- Your SSH key `~/.ssh/id_ed25519`. Terraform placed its public half on both servers

## Run it

1. Open the tunnels. The script is in the folder above this one and works from anywhere:
   ```bash
   ../bastion.sh up
   ../bastion.sh status
   ```
   Both servers should say the tunnel is open.

2. Make sure you are in this folder (`infrastructure/ansible`). Always run Ansible from here, so it finds `ansible.cfg`.

3. Check that Ansible reaches both servers:
   ```bash
   ansible all -m ansible.builtin.ping
   ```
   You want two `pong` replies. Do not continue until you get them.

4. Check the files for mistakes:
   ```bash
   ansible-playbook site.yml --syntax-check
   ```

5. Run everything:
   ```bash
   ansible-playbook site.yml
   ```
   The first time, you can run the playbooks one by one so a failure is easy to place: `base.yml`, `security.yml`, `firewall.yml`, `k3s.yml`, `k3s-agent.yml`.

A second run of `site.yml` should show `changed=0` almost everywhere. The firewall save step may still report a change.

## Check the cluster

```bash
../bastion.sh ssh 1
```
On the server:
```bash
sudo k3s kubectl get nodes -o wide
sudo k3s kubectl get pods -A
```
You want two nodes, `lutiano-1` and `lutiano-2`, both `Ready`, and the system pods `Running` or `Completed`. The agent can take a minute or two to appear.

## Settings you may change

| What | Where |
|---|---|
| Login user (`ubuntu`) | `ansible_user` in `inventory.yml` |
| SSH private key | `ansible_ssh_private_key_file` in `inventory.yml` |
| k3s version | `k3s_version` in `inventory.yml`. Both servers use it |
| Tunnel ports (2201, 2202) | `ansible_port` in `inventory.yml`; must match `bastion.sh` |
| Server addresses | `node_ip` in `inventory.yml`; must match `vms` in `../terraform/variables.tf` |
| Firewall network | `private_cidr` in `firewall.yml` |

Changing `k3s_version` does not upgrade servers that already run k3s, because the install step is skipped when k3s exists. An upgrade needs its own step.

## Things to know

- **Firewall rule number.** Oracle's Ubuntu images end the firewall chain with a rule that rejects everything. `firewall.yml` inserts the allow rule above it, at position 5. If the position is wrong, check it on a server with `sudo iptables -L INPUT -n --line-numbers` and set `rule_num` in `firewall.yml` to the line number of the `REJECT` rule.
- **Tunnels last 3 hours.** After that, run `../bastion.sh up` again. They also close when you switch networks.
- **SSH hardening.** `security.yml` disables password logins and root login. Your key keeps working. The new SSH config is checked before it is applied, so a typo cannot lock you out.
- **Do not run the playbooks without the tunnels.** You will see `Connection refused` on ports 2201 and 2202. Open the tunnels first.

## Troubleshooting

| Message | Cause and fix |
|---|---|
| `No inventory was parsed` | You are not in this folder. `cd` here, or run `ansible all -i inventory.yml ...` |
| `Connection refused` on 2201 or 2202 | The tunnels are closed. Run `../bastion.sh up` |
| `Permission denied (publickey)` on the server | Wrong login user or key. Check `ansible_user` and `ansible_ssh_private_key_file` |
| `conflicting action statements` | A misspelled key in a playbook, such as `enviroment` instead of `environment` |
| A node is missing from `kubectl get nodes` | Wait two minutes, then check `sudo systemctl status k3s-agent` on `lutiano-2` |
| Pods stuck `Pending` | The servers may be out of memory. Check `free -h` on the server |

## Next steps

With the cluster running, the next layer is Argo CD, which installs the other services from Git. That work is kept apart from this folder.
