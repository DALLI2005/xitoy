#!/bin/bash
# Serverga bir marta ishga tushiriladigan skript
set -e

echo "=== 1. Bot fayllarini ko'chirish ==="
scp bot.py requirements.txt root@176.101.56.158:/opt/xitoy_bot/

echo "=== 2. Server sozlamalari ==="
ssh root@176.101.56.158 << 'REMOTE'
  set -e

  # Python va venv o'rnatish
  apt-get update -q
  apt-get install -y python3 python3-venv python3-pip -q

  # Bot papkasi
  mkdir -p /opt/xitoy_bot
  cd /opt/xitoy_bot

  # Virtual muhit
  python3 -m venv venv
  venv/bin/pip install --quiet --upgrade pip
  venv/bin/pip install --quiet -r requirements.txt

  echo "Python va kutubxonalar tayyor"
REMOTE

echo "=== 3. Systemd service o'rnatish ==="
scp xitoy_bot.service root@176.101.56.158:/etc/systemd/system/

ssh root@176.101.56.158 << 'REMOTE'
  systemctl daemon-reload
  systemctl enable xitoy_bot
  systemctl restart xitoy_bot
  sleep 2
  systemctl status xitoy_bot --no-pager
REMOTE

echo ""
echo "✅ Bot 24/7 ishga tushdi!"
echo "Log ko'rish:   ssh root@176.101.56.158 'journalctl -u xitoy_bot -f'"
echo "To'xtatish:    ssh root@176.101.56.158 'systemctl stop xitoy_bot'"
echo "Qayta ishga:   ssh root@176.101.56.158 'systemctl restart xitoy_bot'"
