#!/bin/bash
# Xitoy WebApp — server deploy skripti
set -e

SERVER="root@176.101.56.158"
DOMAIN="admin.eliboyev.uz"

echo "=== 1. Frontend build ==="
cd "$(dirname "$0")/frontend"
npm install
npm run build
cd ..

echo "=== 2. Fayllarni serverga ko'chirish ==="
ssh $SERVER "mkdir -p /opt/xitoy_webapp/backend /opt/xitoy_webapp/frontend"
scp -r frontend/dist              $SERVER:/opt/xitoy_webapp/frontend/
scp    backend/main.py            $SERVER:/opt/xitoy_webapp/backend/
scp    backend/login_bot.py       $SERVER:/opt/xitoy_webapp/backend/
scp    backend/login_state.py     $SERVER:/opt/xitoy_webapp/backend/
scp    backend/requirements.txt   $SERVER:/opt/xitoy_webapp/backend/
# Eslatma: .env serverda qo'lda boshqariladi (LOGIN_BOT_TOKEN ni o'sha yerda to'ldiring).
# Birinchi marta:  scp backend/.env.example $SERVER:/opt/xitoy_webapp/backend/.env

echo "=== 3. Python muhit o'rnatish ==="
ssh $SERVER << 'REMOTE'
  set -e
  cd /opt/xitoy_webapp/backend
  python3 -m venv venv
  venv/bin/pip install --quiet --upgrade pip
  venv/bin/pip install --quiet -r requirements.txt
  echo "Python tayyor"
REMOTE

echo "=== 4. nginx o'rnatish ==="
scp nginx.conf $SERVER:/etc/nginx/sites-available/xitoy_webapp

ssh $SERVER << 'REMOTE'
  set -e
  apt-get install -y nginx -q
  ln -sf /etc/nginx/sites-available/xitoy_webapp /etc/nginx/sites-enabled/xitoy_webapp
  rm -f /etc/nginx/sites-enabled/default
  nginx -t
  systemctl restart nginx
  systemctl enable nginx
  echo "nginx tayyor"
REMOTE

echo "=== 5. SSL sertifikat (Let's Encrypt) ==="
ssh $SERVER "DOMAIN=$DOMAIN" << 'REMOTE'
  set -e
  apt-get install -y certbot python3-certbot-nginx -q
  certbot --nginx -d $DOMAIN --non-interactive --agree-tos -m dilshodbekeliboyev2005@gmail.com
  echo "SSL tayyor"
REMOTE

echo "=== 6. FastAPI + Login bot systemd service ==="
scp xitoy_webapp.service   $SERVER:/etc/systemd/system/
scp dalli_login_bot.service $SERVER:/etc/systemd/system/

ssh $SERVER << 'REMOTE'
  systemctl daemon-reload
  systemctl enable xitoy_webapp dalli_login_bot
  systemctl restart xitoy_webapp dalli_login_bot
  sleep 2
  systemctl status xitoy_webapp --no-pager
  systemctl status dalli_login_bot --no-pager
REMOTE

echo ""
echo "✅ WebApp + Login bot ishga tushdi!"
echo ""
echo "URL:        https://admin.eliboyev.uz"
echo "WebApp log: ssh $SERVER 'journalctl -u xitoy_webapp -f'"
echo "Bot log:    ssh $SERVER 'journalctl -u dalli_login_bot -f'"
