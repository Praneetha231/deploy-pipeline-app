# Automated Application Deployment Pipeline

Java + Jenkins + Docker + Kubernetes + AWS pipeline for automated build, containerization, and deployment.

## What this project does
1. Spring Boot Java backend (REST API: `/`, `/health`, `/api/version`)
2. Jenkins pipeline: pulls code from GitHub → builds JAR → builds Docker image → pushes to Docker Hub → deploys to Kubernetes
3. Multi-stage Dockerfile (Maven build stage + slim JRE Alpine runtime stage)
4. Kubernetes Deployment (3 replicas, rolling updates, health probes) + Service (NodePort)
5. Postman collection to validate live endpoints

---

## STEP 1 — Push this code to GitHub
```bash
cd deploy-pipeline
git init
git add .
git commit -m "Initial commit - deployment pipeline project"
git branch -M main
git remote add origin https://github.com/<your-username>/deploy-pipeline-app.git
git push -u origin main
```

## STEP 2 — Launch AWS EC2 instance
- Go to AWS Console → EC2 → Launch Instance
- AMI: Ubuntu 22.04 LTS
- Type: t2.medium (t2.micro is too small for Jenkins+Docker+K8s together)
- Security Group: allow inbound ports **22** (SSH), **8080** (Jenkins), **30080** (App NodePort)
- Download the `.pem` key

SSH in:
```bash
chmod 400 your-key.pem
ssh -i your-key.pem ubuntu@<EC2_PUBLIC_IP>
```

## STEP 3 — Install Docker
```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
newgrp docker
```

## STEP 4 — Install Jenkins
```bash
sudo apt install -y openjdk-17-jdk
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt update
sudo apt install -y jenkins
sudo usermod -aG docker jenkins
sudo systemctl enable --now jenkins
```
Visit `http://<EC2_PUBLIC_IP>:8080`, unlock using:
```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```
Install suggested plugins. Also install via Plugin Manager: **Docker Pipeline**, **Kubernetes CLI**.

## STEP 5 — Install kubectl + a local Kubernetes cluster (kind or minikube)
For a quick single-node cluster on the same EC2 box:
```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl && sudo mv kubectl /usr/local/bin/

curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.22.0/kind-linux-amd64
chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind

kind create cluster --name pipeline-cluster
```
(Alternative: use Amazon EKS if you want a "real" managed cluster — more setup, costs more.)

## STEP 6 — Add Jenkins credentials
In Jenkins → Manage Jenkins → Credentials → Add:
- `dockerhub-creds` (Username/Password) — your Docker Hub login
- `kubeconfig` (Secret file) — upload `~/.kube/config` from the EC2 box

## STEP 7 — Create the Jenkins Pipeline job
- New Item → Pipeline → name it `deploy-pipeline-app`
- Pipeline script from SCM → Git → your GitHub repo URL → Script Path: `Jenkinsfile`
- Update `IMAGE_NAME` in the Jenkinsfile to your actual Docker Hub username
- Save → Build Now

## STEP 8 — Verify deployment
```bash
kubectl get pods
kubectl get svc
```
Visit: `http://<EC2_PUBLIC_IP>:30080/health`

## STEP 9 — Validate with Postman
- Import `postman/deploy-pipeline-collection.json`
- Set `base_url` variable to `http://<EC2_PUBLIC_IP>:30080`
- Run all 3 requests, confirm 200 OK responses

---

## Local testing (before touching AWS)
```bash
mvn clean package
docker build -t deploy-pipeline-app .
docker run -p 8080:8080 deploy-pipeline-app
curl http://localhost:8080/health
```
