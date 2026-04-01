# 🚀 Spring Boot Deployment Guide: DigitalOcean

This document outlines the end-to-end process for deploying the `stream-spring-backend` on a DigitalOcean Droplet, including SSH setup, environment configuration, and performance tuning for video streaming.

---

## 1️⃣ Initial Access & SSH Connectivity

If you encounter a timeout, ensure the SSH port is open and your keys are authorized.

### 🔧 On the Droplet (via DigitalOcean Console)

#### Fix Firewall

```bash
ufw allow 22/tcp
ufw reload
````

#### Authorize Your Mac's Public Key

On your **Mac**:

```bash
cat ~/.ssh/id_rsa.pub
```

Copy the output.

On the **Droplet (DO Console)**:

```bash
nano ~/.ssh/authorized_keys
```

* Paste your public key
* Save and exit

Set proper permissions:

```bash
chmod 600 ~/.ssh/authorized_keys
```

---

### 💻 From Your Mac

Connect using:

```bash
ssh root@206.189.173.185
```

---

## 2️⃣ System Dependencies

Install Java runtime, build tools, FFmpeg (for HLS transcoding), and PostgreSQL.

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven ffmpeg postgresql postgresql-contrib
```

---

## 3️⃣ Database Setup (PostgreSQL)

The application is configured to run on a custom port (`5433`) to avoid conflicts.

### 📝 Edit Configuration

> Adjust version number (e.g., `16`) as needed.

```bash
sudo nano /etc/postgresql/16/main/postgresql.conf
```

Change:

```
port = 5432
```

To:

```
port = 5433
```

---

### 🗄 Initialize Database

```bash
sudo -u postgres psql
```

Inside the SQL prompt:

```sql
CREATE DATABASE videodb;
ALTER USER postgres WITH PASSWORD '1710';
\q
```

---

### 🔄 Restart PostgreSQL

```bash
sudo systemctl restart postgresql
```

---

## 4️⃣ Environment Secrets

Securely inject your DigitalOcean Spaces (S3) credentials without hardcoding them.

Open your profile:

```bash
nano ~/.bashrc
```

Add:

```bash
export CLOUD_AWS_CREDENTIALS_ACCESS_KEY="YOUR_ACCESS_KEY"
export CLOUD_AWS_CREDENTIALS_SECRET_KEY="YOUR_SECRET_KEY"
```

Refresh environment:

```bash
source ~/.bashrc
```

Verify:

```bash
echo $CLOUD_AWS_CREDENTIALS_ACCESS_KEY
```

---

## 5️⃣ Performance Tuning (Swap Space)

Video streaming apps handle large file buffers (1GB+). Adding swap prevents **Out Of Memory (OOM)** crashes on smaller Droplets.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

Persist after reboot:

```bash
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

---

## 6️⃣ Build and Run

### 📥 Clone & Package

```bash
git clone https://github.com/santrupt29/stream-spring-backend.git
cd stream-spring-backend
mvn clean package -DskipTests
```

---

### 🚀 Deploy to Background

```bash
nohup java -jar target/spring-stream-backend-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

---

### 🛠 Manage the App

View logs:

```bash
tail -f app.log
```

Check running process:

```bash
ps -ef | grep java
```

Stop application:

```bash
kill -9 <PID>
```

---

## 7️⃣ Networking & Frontend Integration

### 🔥 Open API Port

```bash
sudo ufw allow 8080/tcp
```

### 🌐 API URL

```
http://206.189.173.185:8080
```

### 🔄 CORS Configuration

Ensure your backend `WebConfig` allows your frontend domain (e.g., Vercel).

---

## 🛠 Troubleshooting Tips

### 🔐 Git Authentication

Use a **GitHub Personal Access Token (PAT)** instead of your password when cloning private repositories.

---

### 🎥 FFmpeg Errors

Check installation:

```bash
ffmpeg -version
```

---

### 🔑 Credential Check

Verify environment variables:

```bash
echo $CLOUD_AWS_CREDENTIALS_ACCESS_KEY
```

---

# ✅ Deployment Complete

Your Spring Boot video streaming backend should now be live and accessible via:

```
http://206.189.173.185:8080
```

For production deployments, consider:

* Using `systemd` instead of `nohup`
* Setting up a reverse proxy (e.g., Nginx)
* Enabling HTTPS with Let's Encrypt
* Configuring proper logging & monitoring

