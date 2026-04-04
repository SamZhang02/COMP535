#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <num_receivers>"
  exit 1
}

if [[ $# -ne 1 ]]; then
  usage
fi

if ! [[ "$1" =~ ^[0-9]+$ ]] || (( "$1" <= 0 )); then
  echo "Error: <num_receivers> must be a positive integer."
  usage
fi

num_receivers="$1"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
receiver_bin="$repo_root/receiver"

if [[ ! -x "$receiver_bin" ]]; then
  echo "Error: receiver binary not found at $receiver_bin"
  echo "Build it first with: make receiver"
  exit 1
fi

run_root="$script_dir/runs/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$run_root"

declare -a receiver_pids=()

cleanup() {
  echo
  echo "Stopping receivers..."
  for pid in "${receiver_pids[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
  wait || true
}

trap cleanup INT TERM

for ((i = 1; i <= num_receivers; i++)); do
  receiver_id="$(printf "%02d" "$i")"
  receiver_root="$run_root/receiver_${receiver_id}"
  output_dir="$receiver_root/received_files"
  log_file="$receiver_root/receiver.log"

  mkdir -p "$output_dir"

  "$receiver_bin" -d "$output_dir" >"$log_file" 2>&1 &
  pid=$!
  receiver_pids+=("$pid")

  echo "Receiver ${receiver_id} started (pid=$pid)"
  echo "  output: $output_dir"
  echo "  log:    $log_file"
done

echo
echo "Started $num_receivers receiver(s). Run root: $run_root"
echo "Press Ctrl-C to stop all receivers."

wait
