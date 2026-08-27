#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
bin_dir="$root/scripts/bin"
release_tag_file="$root/scripts/harness-cli-release-tag"
base_url=${HARNESS_CLI_BASE_URL:-}

fail() {
  printf 'install-harness-cli failed: %s\n' "$*" >&2
  exit 1
}

[[ -f "$release_tag_file" ]] || fail "pinned release file is missing: $release_tag_file"
release_tag=$(awk 'NF && $1 !~ /^#/ { print $1; exit }' "$release_tag_file")
[[ "$release_tag" == harness-cli-v* ]] || fail "invalid pinned release tag: $release_tag"
[[ -n "$base_url" ]] || base_url="https://github.com/hoangnb24/repository-harness/releases/download/$release_tag"

os=$(uname -s)
case "$os" in
  Darwin) asset="harness-cli-macos-arm64" ;;
  Linux)  asset="harness-cli-linux-x64" ;;
  *)      fail "unsupported platform: $os" ;;
esac
dest="$bin_dir/harness-cli"
sum_file="$asset.sha256"

mkdir -p "$bin_dir"
tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT

curl -fsSL "$base_url/$asset" -o "$tmp_dir/$asset"
curl -fsSL "$base_url/$sum_file" -o "$tmp_dir/$sum_file"

expected=$(awk '{ print $1 }' "$tmp_dir/$sum_file")
actual=$(shasum -a 256 "$tmp_dir/$asset" | awk '{ print $1 }')
[[ "$actual" == "$expected" ]] || fail "checksum mismatch for $asset (expected $expected, got $actual)"

install -m 755 "$tmp_dir/$asset" "$dest"
printf 'Harness CLI installed: %s (release %s)\n' "$dest" "$release_tag"