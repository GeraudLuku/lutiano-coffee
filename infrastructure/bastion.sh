#!/bin/bash
# Opens or closes SSH tunnels to the two private VMs through the Oracle Bastion.
#   ./bastion.sh up        create the sessions and open both tunnels
#   ./bastion.sh down      close both tunnels
#   ./bastion.sh status    show which tunnels are open
#   ./bastion.sh ssh 1     log in to lutiano-1 (use 2 for lutiano-2)
# Needs a network that allows outgoing SSH (mobile data). Sessions last 3 hours.
set -euo pipefail

SCRIPT="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"
TF_DIR="$(dirname "$SCRIPT")/terraform"

PROFILE="lutiano-terraform"
SSH_KEY_PUB="$HOME/.ssh/id_ed25519.pub"
VM_USER="ubuntu"
VMS=("lutiano-1 10.0.1.10 2201" "lutiano-2 10.0.1.11 2202")
REGION="$(sed -nE 's/^region *= *"(.*)"/\1/p' "$TF_DIR/terraform.tfvars")"
BASTION_HOST="host.bastion.${REGION}.oci.oraclecloud.com"

listening() { lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }

up() {
  oci session refresh --profile "$PROFILE" >/dev/null 2>&1 || true
  if ! oci session validate --profile "$PROFILE" --local >/dev/null 2>&1; then
    echo "The Oracle login has expired. Run: oci session authenticate --region $REGION --profile $PROFILE"
    exit 1
  fi
  local bastion_id
  bastion_id="$(terraform -chdir="$TF_DIR" output -raw bastion_id)"
  for vm in "${VMS[@]}"; do
    read -r name ip port <<<"$vm"
    if listening "$port"; then
      echo "$name: tunnel already open on port $port"
      continue
    fi
    echo "$name: creating a bastion session..."
    session_id="$(oci bastion session create-port-forwarding \
      --bastion-id "$bastion_id" --target-private-ip "$ip" --target-port 22 \
      --ssh-public-key-file "$SSH_KEY_PUB" --session-ttl 10800 --display-name "$name" \
      --wait-for-state SUCCEEDED --profile "$PROFILE" --auth security_token \
      --query 'data.resources[0].identifier' --raw-output)"
    echo "$name: opening the tunnel on port $port..."
    # A new session can take a few seconds to accept keys, so retry a few times.
    attempt=1
    until ssh -f -N -i "${SSH_KEY_PUB%.pub}" -o IdentitiesOnly=yes -o ExitOnForwardFailure=yes -o StrictHostKeyChecking=accept-new -o ServerAliveInterval=30 \
      -L "$port:$ip:22" -p 22 "$session_id@$BASTION_HOST"; do
      if [ "$attempt" -ge 6 ]; then
        echo "$name: could not open the tunnel after $attempt tries"
        exit 1
      fi
      attempt=$((attempt + 1))
      echo "$name: session not ready yet, retrying in 5 seconds (try $attempt of 6)..."
      sleep 5
    done
    echo "$name: ready. Log in with: $SCRIPT ssh ${name##*-}"
  done
}

down() {
  for vm in "${VMS[@]}"; do
    read -r name ip port <<<"$vm"
    if listening "$port"; then
      lsof -ti tcp:"$port" -sTCP:LISTEN | xargs kill
      echo "$name: tunnel closed"
    else
      echo "$name: no tunnel open"
    fi
  done
}

status() {
  for vm in "${VMS[@]}"; do
    read -r name ip port <<<"$vm"
    if listening "$port"; then
      echo "$name: tunnel OPEN on port $port, log in with: $SCRIPT ssh ${name##*-}"
    else
      echo "$name: closed. Open it with: $SCRIPT up"
    fi
  done
}

login() {
  case "${1:-}" in 1|2) ;; *) echo "Usage: $SCRIPT ssh 1|2"; exit 1 ;; esac
  read -r name ip port <<<"${VMS[$(($1 - 1))]}"
  listening "$port" || { echo "No tunnel on port $port. Run: $SCRIPT up"; exit 1; }
  exec ssh -p "$port" "$VM_USER@127.0.0.1"
}

case "${1:-}" in
  up) up ;;
  down) down ;;
  status) status ;;
  ssh) login "${2:-}" ;;
  *) sed -n '2,7p' "$SCRIPT"; exit 1 ;;
esac