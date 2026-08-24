#!/usr/bin/env bash
# check-env.sh — Diagnostik konektivitas Nestara di Linux/macOS
# Jalankan:  bash tool/check-env.sh
set -euo pipefail

ok(){ echo -e "\033[32m[OK]\033[0m $*"; }
warn(){ echo -e "\033[33m[WARN]\033[0m $*"; }
fail(){ echo -e "\033[31m[FAIL]\033[0m $*"; }
info(){ echo -e "\033[36m[INFO]\033[0m $*"; }

info "=== Nestara Workspace — Linux/macOS Env Check ==="
echo ""

# Docker
if docker info >/dev/null 2>&1; then ok "Docker running"; else fail "Docker tidak running"; fi

# Ports
for port in 8000 8080 3306 3307; do
  if (echo >/dev/tcp/127.0.0.1/$port) 2>/dev/null; then ok "Port $port LISTEN"; else warn "Port $port tidak listen"; fi
done

# Firewall (ufw/firewalld)
if command -v ufw >/dev/null 2>&1; then
  ufw status | grep -q "8000" && ok "ufw rule 8000 ada" || warn "ufw belum allow 8000 — sudo ufw allow 8000/tcp"
fi

# Interfaces
info "Interfaces IPv4 (filter docker/br-/veth/tailscale):"
ip -4 addr show 2>/dev/null | grep -E "inet " | grep -v "127.0.0.1" || ifconfig 2>/dev/null | grep "inet "

# Tailscale
if command -v tailscale >/dev/null 2>&1; then
  if tailscale ip -4 >/dev/null 2>&1; then ok "Tailscale IP: $(tailscale ip -4)"; else warn "Tailscale tidak aktif"; fi
else warn "tailscale CLI tidak ditemukan"; fi

# ADB
if command -v adb >/dev/null 2>&1; then
  ok "ADB: $(adb version 2>&1 | head -1)"
  adb devices 2>&1 | grep -q "device$" && ok "ADB device terdeteksi" || warn "ADB device tidak ada"
else warn "ADB tidak di PATH"; fi

# Health
for url in "http://localhost:8080/up" "http://localhost:8000/up"; do
  if curl -sf --max-time 3 "$url" >/dev/null 2>&1; then ok "$url OK"; else warn "$url tidak reachable"; fi
done

echo ""
info "Rekomendasi: 1) --reverse  2) --tailscale  3) --device  4) emulator"
