# Kubernetes Deployment Guide — JIRA Clone

## Overview

This guide walks through containerising all four services of the JIRA Clone app
and deploying them to a Kubernetes cluster, either locally (Minikube) or on a
cloud provider (AWS EKS, GCP GKE, Azure AKS).

Services deployed to Kubernetes:

    1. MySQL 8          — StatefulSet with PersistentVolumeClaim
    2. Keycloak 24      — Deployment (replaces Docker Compose)
    3. Spring Boot      — Deployment (JAR inside a custom Docker image)
    4. Angular frontend — Deployment (static files served by nginx)

All services communicate inside the cluster via ClusterIP Services.
External traffic enters through an Ingress (or NodePort for local dev).

---

## Prerequisites

Install these tools before starting:

    kubectl       Kubernetes CLI          https://kubernetes.io/docs/tasks/tools/
    minikube      Local K8s cluster       https://minikube.sigs.k8s.io/docs/start/
    Docker        Build + push images     https://docs.docker.com/get-docker/
    Docker Hub    Image registry (free)   https://hub.docker.com/  (create account)

Check versions:

    kubectl version --client
    minikube version
    docker --version

---

## Part 1 — Containerise Each Service

### 1a. Dockerise the Spring Boot Backend

Create `jira-clone-backend/Dockerfile`:

    FROM eclipse-temurin:17-jre-alpine

    WORKDIR /app

    # Copy the built JAR (run: mvn package -DskipTests first)
    COPY target/jira-clone-backend-1.0.0.jar app.jar

    EXPOSE 8080

    ENTRYPOINT ["java", "-jar", "app.jar"]

Build and push to Docker Hub:

    cd jira-clone-backend

    # Build the JAR first
    mvn package -DskipTests

    # Build Docker image
    docker build -t yourdockerhubusername/jira-clone-backend:1.0.0 .

    # Push to Docker Hub
    docker login
    docker push yourdockerhubusername/jira-clone-backend:1.0.0

Replace `yourdockerhubusername` with your actual Docker Hub username throughout this guide.

---

### 1b. Dockerise the Angular Frontend (served by nginx)

Create `jira-clone-frontend/Dockerfile`:

    # Stage 1 — build the Angular app
    FROM node:20-alpine AS build

    WORKDIR /app
    COPY package*.json ./
    RUN npm install --legacy-peer-deps
    COPY . .
    RUN npm run build -- --configuration=production

    # Stage 2 — serve with nginx
    FROM nginx:alpine

    COPY --from=build /app/dist/jira-clone-frontend/browser /usr/share/nginx/html
    COPY nginx.conf /etc/nginx/conf.d/default.conf

    EXPOSE 80

Create `jira-clone-frontend/nginx.conf` (needed so Angular routing works — all routes must serve `index.html`):

    server {
        listen 80;
        server_name _;
        root /usr/share/nginx/html;
        index index.html;

        # Serve static files
        location ~* \.(js|css|png|jpg|gif|ico|woff2?)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }

        # All other routes go to index.html (Angular router handles them)
        location / {
            try_files $uri $uri/ /index.html;
        }
    }

Build and push:

    cd jira-clone-frontend
    docker build -t yourdockerhubusername/jira-clone-frontend:1.0.0 .
    docker push yourdockerhubusername/jira-clone-frontend:1.0.0

---

### 1c. Note on environment.ts for production

Before building the frontend image, update `src/environments/environment.prod.ts`
to use the correct Kubernetes internal service names:

    export const environment = {
      production: true,
      apiUrl: 'http://jira-clone-backend:8080/api',   // K8s service name
      keycloak: {
        url: 'http://keycloak:8080',                   // K8s service name
        realm: 'jira-clone',
        clientId: 'jira-clone-app'
      }
    };

If you are using Ingress with a domain, use the external domain instead:

    apiUrl: 'https://yourdomain.com/api',
    keycloak: { url: 'https://auth.yourdomain.com', ... }

---

## Part 2 — Create the Kubernetes Namespace and Secrets

Create a working directory for all manifest files:

    mkdir k8s
    cd k8s

All manifest files below go inside this `k8s/` directory.

---

### 2a. Namespace

File: `k8s/00-namespace.yaml`

    apiVersion: v1
    kind: Namespace
    metadata:
      name: jira-clone

---

### 2b. Secrets

Secrets store sensitive values. Values must be base64-encoded.

Encode your values:

    # Encode mysql root password
    echo -n "sam9311" | base64
    # Output: c2FtOTMxMQ==

    # Encode keycloak admin password
    echo -n "admin" | base64
    # Output: YWRtaW4=

    # Encode github webhook secret
    echo -n "your-secret-here" | base64

File: `k8s/01-secrets.yaml`

    apiVersion: v1
    kind: Secret
    metadata:
      name: mysql-secret
      namespace: jira-clone
    type: Opaque
    data:
      root-password: c2FtOTMxMQ==      # sam9311
      database: amlyYWNsb25l          # jiraclone

    ---

    apiVersion: v1
    kind: Secret
    metadata:
      name: keycloak-secret
      namespace: jira-clone
    type: Opaque
    data:
      admin-password: YWRtaW4=        # admin

    ---

    apiVersion: v1
    kind: Secret
    metadata:
      name: backend-secret
      namespace: jira-clone
    type: Opaque
    data:
      db-password: c2FtOTMxMQ==       # sam9311
      webhook-secret: <base64 of your webhook secret>

---

### 2c. ConfigMap for backend application.properties

File: `k8s/02-backend-config.yaml`

    apiVersion: v1
    kind: ConfigMap
    metadata:
      name: backend-config
      namespace: jira-clone
    data:
      application.properties: |
        server.port=8080
        spring.application.name=jira-clone-backend

        spring.datasource.url=jdbc:mysql://mysql:3306/jiraclone?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
        spring.datasource.username=root
        spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

        spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
        spring.jpa.hibernate.ddl-auto=update
        spring.jpa.show-sql=false
        spring.jpa.properties.hibernate.jdbc.time_zone=UTC

        spring.jackson.serialization.indent-output=false
        spring.jackson.default-property-inclusion=non_null

        spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8080/realms/jira-clone

        logging.level.com.jiraclone=INFO

Notice: `mysql` and `keycloak` are Kubernetes Service names defined in later manifests.
Inside the cluster, pods reach each other via their Service name.

---

## Part 3 — MySQL StatefulSet

A StatefulSet is used (not a Deployment) because MySQL is stateful — it needs a stable
network identity and persistent storage that survives pod restarts.

File: `k8s/03-mysql.yaml`

    apiVersion: v1
    kind: PersistentVolumeClaim
    metadata:
      name: mysql-pvc
      namespace: jira-clone
    spec:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 5Gi

    ---

    apiVersion: apps/v1
    kind: StatefulSet
    metadata:
      name: mysql
      namespace: jira-clone
    spec:
      serviceName: mysql
      replicas: 1
      selector:
        matchLabels:
          app: mysql
      template:
        metadata:
          labels:
            app: mysql
        spec:
          containers:
            - name: mysql
              image: mysql:8.0
              ports:
                - containerPort: 3306
              env:
                - name: MYSQL_ROOT_PASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: mysql-secret
                      key: root-password
                - name: MYSQL_DATABASE
                  valueFrom:
                    secretKeyRef:
                      name: mysql-secret
                      key: database
              volumeMounts:
                - name: mysql-data
                  mountPath: /var/lib/mysql
              readinessProbe:
                exec:
                  command: ["mysqladmin", "ping", "-h", "localhost"]
                initialDelaySeconds: 20
                periodSeconds: 10
          volumes:
            - name: mysql-data
              persistentVolumeClaim:
                claimName: mysql-pvc

    ---

    apiVersion: v1
    kind: Service
    metadata:
      name: mysql
      namespace: jira-clone
    spec:
      selector:
        app: mysql
      ports:
        - port: 3306
          targetPort: 3306
      clusterIP: None     # Headless service — required for StatefulSet

---

## Part 4 — Keycloak Deployment

Keycloak in `start-dev` mode stores realm data in an embedded H2 database inside the pod.
This means realm configuration is lost when the pod restarts.

There are two approaches:

**Option A (simple, local dev):** Keep using `start-dev`. On each restart you must
re-create the realm and client manually via the Admin Console. Good for testing.

**Option B (production-grade):** Point Keycloak to the MySQL database so realm data persists.
This is shown below.

File: `k8s/04-keycloak.yaml`

    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: keycloak
      namespace: jira-clone
    spec:
      replicas: 1
      selector:
        matchLabels:
          app: keycloak
      template:
        metadata:
          labels:
            app: keycloak
        spec:
          containers:
            - name: keycloak
              image: quay.io/keycloak/keycloak:24.0.1
              # Option A: dev mode (realm resets on restart)
              # command: ["start-dev"]
              # Option B: dev mode backed by MySQL (realm persists)
              command: ["start-dev", "--db=mysql", "--db-url=jdbc:mysql://mysql:3306/keycloak", "--db-username=root"]
              ports:
                - containerPort: 8080
              env:
                - name: KEYCLOAK_ADMIN
                  value: admin
                - name: KEYCLOAK_ADMIN_PASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: keycloak-secret
                      key: admin-password
                - name: KC_DB_PASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: mysql-secret
                      key: root-password
              readinessProbe:
                httpGet:
                  path: /realms/master
                  port: 8080
                initialDelaySeconds: 60
                periodSeconds: 15
                failureThreshold: 10

    ---

    apiVersion: v1
    kind: Service
    metadata:
      name: keycloak
      namespace: jira-clone
    spec:
      selector:
        app: keycloak
      ports:
        - name: http
          port: 8080
          targetPort: 8080

Note: Keycloak uses the MySQL Service name `mysql:3306`. The `keycloak` database must
exist in MySQL. You can create it by exec-ing into the MySQL pod after startup:

    kubectl exec -n jira-clone -it <mysql-pod-name> -- mysql -u root -psam9311 -e "CREATE DATABASE IF NOT EXISTS keycloak;"

---

## Part 5 — Spring Boot Backend Deployment

File: `k8s/05-backend.yaml`

    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: jira-clone-backend
      namespace: jira-clone
    spec:
      replicas: 1
      selector:
        matchLabels:
          app: jira-clone-backend
      template:
        metadata:
          labels:
            app: jira-clone-backend
        spec:
          initContainers:
            # Wait for MySQL to be ready before starting the backend
            - name: wait-for-mysql
              image: busybox:1.36
              command:
                - sh
                - -c
                - |
                  until nc -z mysql 3306; do
                    echo "Waiting for MySQL...";
                    sleep 3;
                  done
            # Wait for Keycloak to be ready
            - name: wait-for-keycloak
              image: busybox:1.36
              command:
                - sh
                - -c
                - |
                  until nc -z keycloak 8080; do
                    echo "Waiting for Keycloak...";
                    sleep 5;
                  done
          containers:
            - name: jira-clone-backend
              image: yourdockerhubusername/jira-clone-backend:1.0.0
              ports:
                - containerPort: 8080
              env:
                - name: SPRING_DATASOURCE_PASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: backend-secret
                      key: db-password
                - name: GITHUB_WEBHOOK_SECRET
                  valueFrom:
                    secretKeyRef:
                      name: backend-secret
                      key: webhook-secret
              volumeMounts:
                - name: backend-config
                  mountPath: /app/config
              # Spring Boot loads application.properties from /app/config/application.properties
              # when SPRING_CONFIG_LOCATION is set
              envFrom: []
              command:
                - java
                - -jar
                - app.jar
                - --spring.config.location=/app/config/application.properties
              readinessProbe:
                httpGet:
                  path: /actuator/health
                  port: 8080
                initialDelaySeconds: 30
                periodSeconds: 10
              livenessProbe:
                httpGet:
                  path: /actuator/health
                  port: 8080
                initialDelaySeconds: 60
                periodSeconds: 20
          volumes:
            - name: backend-config
              configMap:
                name: backend-config

    ---

    apiVersion: v1
    kind: Service
    metadata:
      name: jira-clone-backend
      namespace: jira-clone
    spec:
      selector:
        app: jira-clone-backend
      ports:
        - name: http
          port: 8080
          targetPort: 8080

Note: If you do not have `spring-boot-starter-actuator` in `pom.xml`, remove the
readinessProbe and livenessProbe sections, or add the actuator dependency:

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

---

## Part 6 — Angular Frontend Deployment

File: `k8s/06-frontend.yaml`

    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: jira-clone-frontend
      namespace: jira-clone
    spec:
      replicas: 1
      selector:
        matchLabels:
          app: jira-clone-frontend
      template:
        metadata:
          labels:
            app: jira-clone-frontend
        spec:
          containers:
            - name: jira-clone-frontend
              image: yourdockerhubusername/jira-clone-frontend:1.0.0
              ports:
                - containerPort: 80
              readinessProbe:
                httpGet:
                  path: /
                  port: 80
                initialDelaySeconds: 5
                periodSeconds: 10

    ---

    apiVersion: v1
    kind: Service
    metadata:
      name: jira-clone-frontend
      namespace: jira-clone
    spec:
      selector:
        app: jira-clone-frontend
      ports:
        - name: http
          port: 80
          targetPort: 80

---

## Part 7 — Ingress (External Access)

An Ingress routes external HTTP requests to the correct internal Service.
The frontend serves the Angular app. The backend serves the REST API.

File: `k8s/07-ingress.yaml`

    apiVersion: networking.k8s.io/v1
    kind: Ingress
    metadata:
      name: jira-clone-ingress
      namespace: jira-clone
      annotations:
        nginx.ingress.kubernetes.io/rewrite-target: /
    spec:
      ingressClassName: nginx
      rules:
        - host: jira-clone.local           # change to your domain in production
          http:
            paths:
              - path: /api
                pathType: Prefix
                backend:
                  service:
                    name: jira-clone-backend
                    port:
                      number: 8080
              - path: /
                pathType: Prefix
                backend:
                  service:
                    name: jira-clone-frontend
                    port:
                      number: 80
        - host: auth.jira-clone.local      # Keycloak admin + OIDC
          http:
            paths:
              - path: /
                pathType: Prefix
                backend:
                  service:
                    name: keycloak
                    port:
                      number: 8080

For local dev with Minikube, enable the ingress addon (see Part 8).

---

## Part 8 — Deploy Locally with Minikube

### Start Minikube

    minikube start --memory=4096 --cpus=2 --driver=docker

Check it is running:

    minikube status
    kubectl cluster-info

### Enable Addons

    minikube addons enable ingress        # nginx ingress controller
    minikube addons enable metrics-server # optional: pod metrics

### Load Local Docker Images (skip Docker Hub for local dev)

Instead of pushing to Docker Hub, load images directly into Minikube's Docker daemon:

    # Point your terminal's Docker client to Minikube's Docker
    eval $(minikube docker-env)           # Linux / macOS
    # On Windows PowerShell: minikube docker-env | Invoke-Expression

    # Build images inside Minikube (they don't need to be pushed)
    cd jira-clone-backend
    mvn package -DskipTests
    docker build -t yourdockerhubusername/jira-clone-backend:1.0.0 .

    cd ../jira-clone-frontend
    docker build -t yourdockerhubusername/jira-clone-frontend:1.0.0 .

In the Deployment manifests, add `imagePullPolicy: Never` to prevent Kubernetes from
trying to pull from Docker Hub:

    containers:
      - name: jira-clone-backend
        image: yourdockerhubusername/jira-clone-backend:1.0.0
        imagePullPolicy: Never    # use the local image loaded above

### Apply All Manifests

    kubectl apply -f k8s/00-namespace.yaml
    kubectl apply -f k8s/01-secrets.yaml
    kubectl apply -f k8s/02-backend-config.yaml
    kubectl apply -f k8s/03-mysql.yaml

    # Wait for MySQL to be Running before proceeding
    kubectl get pods -n jira-clone -w

    kubectl apply -f k8s/04-keycloak.yaml
    kubectl apply -f k8s/05-backend.yaml
    kubectl apply -f k8s/06-frontend.yaml
    kubectl apply -f k8s/07-ingress.yaml

Or apply the entire directory at once:

    kubectl apply -f k8s/

### Get the Minikube IP and Access the App

    minikube ip
    # Example: 192.168.49.2

Add this to your hosts file (`C:\Windows\System32\drivers\etc\hosts` on Windows):

    192.168.49.2  jira-clone.local
    192.168.49.2  auth.jira-clone.local

Now open:

    http://jira-clone.local          → Angular frontend
    http://jira-clone.local/api      → Spring Boot REST API
    http://auth.jira-clone.local     → Keycloak Admin Console

Alternatively, use `minikube tunnel` to expose LoadBalancer services:

    minikube tunnel
    # Then access via http://localhost

---

## Part 9 — kubectl Cheat Sheet for This App

### View everything in the namespace

    kubectl get all -n jira-clone

### Watch pods start up

    kubectl get pods -n jira-clone -w

### View logs

    # Backend logs
    kubectl logs -n jira-clone deployment/jira-clone-backend -f

    # Frontend (nginx) logs
    kubectl logs -n jira-clone deployment/jira-clone-frontend -f

    # Keycloak logs
    kubectl logs -n jira-clone deployment/keycloak -f

    # MySQL logs
    kubectl logs -n jira-clone statefulset/mysql -f

### Describe a pod (events, errors, image pull issues)

    kubectl describe pod -n jira-clone <pod-name>

### Exec into a pod (open a shell)

    # Open shell inside the backend pod
    kubectl exec -n jira-clone -it deployment/jira-clone-backend -- /bin/sh

    # Open MySQL CLI inside the MySQL pod
    kubectl exec -n jira-clone -it statefulset/mysql -- mysql -u root -psam9311 jiraclone

### Restart a deployment (pull new image)

    kubectl rollout restart -n jira-clone deployment/jira-clone-backend
    kubectl rollout restart -n jira-clone deployment/jira-clone-frontend

### Scale replicas

    kubectl scale -n jira-clone deployment/jira-clone-backend --replicas=2

### View ConfigMap content

    kubectl get configmap -n jira-clone backend-config -o yaml

### View Secret (base64 decoded)

    kubectl get secret -n jira-clone mysql-secret -o jsonpath='{.data.root-password}' | base64 -d

### Delete everything in the namespace

    kubectl delete namespace jira-clone

### Port-forward a service directly (bypass Ingress, useful for debugging)

    # Access backend directly on localhost:8080
    kubectl port-forward -n jira-clone service/jira-clone-backend 8080:8080

    # Access Keycloak directly on localhost:8180
    kubectl port-forward -n jira-clone service/keycloak 8180:8080

    # Access frontend directly on localhost:4200
    kubectl port-forward -n jira-clone service/jira-clone-frontend 4200:80

---

## Part 10 — Cloud Deployment (AWS EKS, GCP GKE, Azure AKS)

### Differences from Minikube

| Concern | Minikube | Cloud |
| --------| ---------| ------|
| Image registry | local images loaded directly | Docker Hub or cloud registry (ECR, GCR, ACR) |
| LoadBalancer | minikube tunnel / NodePort | Cloud-provisioned load balancer (external IP auto-assigned) |
| PersistentVolume | hostPath / local | Cloud disk (EBS on AWS, Persistent Disk on GCP) |
| Ingress | minikube addons enable ingress | Install nginx-ingress or use cloud ingress |
| TLS / HTTPS | manual self-signed | cert-manager + Let's Encrypt |

### AWS EKS Quick Steps

    # Install eksctl
    # https://eksctl.io/installation/

    # Create cluster (takes ~15 minutes)
    eksctl create cluster \
      --name jira-clone \
      --region us-east-1 \
      --nodegroup-name workers \
      --node-type t3.medium \
      --nodes 2

    # Configure kubectl to point at EKS
    aws eks update-kubeconfig --region us-east-1 --name jira-clone

    # Push images to AWS ECR instead of Docker Hub
    aws ecr create-repository --repository-name jira-clone-backend --region us-east-1
    aws ecr get-login-password --region us-east-1 | docker login --username AWS \
      --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
    docker tag yourdockerhubusername/jira-clone-backend:1.0.0 \
      <account-id>.dkr.ecr.us-east-1.amazonaws.com/jira-clone-backend:1.0.0
    docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/jira-clone-backend:1.0.0

    # Install nginx ingress controller on EKS
    kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.0/deploy/static/provider/aws/deploy.yaml

    # Apply manifests (same files, update image URLs to ECR)
    kubectl apply -f k8s/

    # Get the external load balancer URL
    kubectl get ingress -n jira-clone

### GCP GKE Quick Steps

    # Create cluster
    gcloud container clusters create jira-clone \
      --zone us-central1-a \
      --num-nodes 2 \
      --machine-type e2-medium

    # Configure kubectl
    gcloud container clusters get-credentials jira-clone --zone us-central1-a

    # Use Google Container Registry (GCR)
    docker tag yourdockerhubusername/jira-clone-backend:1.0.0 gcr.io/YOUR_PROJECT/jira-clone-backend:1.0.0
    docker push gcr.io/YOUR_PROJECT/jira-clone-backend:1.0.0

---

## Part 11 — Keycloak Realm Persistence in Kubernetes

After Keycloak starts in Kubernetes, you must configure the realm once:

1. Port-forward Keycloak:

       kubectl port-forward -n jira-clone service/keycloak 8180:8080

2. Open http://localhost:8180 → admin / admin

3. Create realm `jira-clone`, client `jira-clone-app`, test user — exact same steps as
   local development (see ARCHITECTURE.md Section 6).

4. If you are using MySQL-backed Keycloak (Option B), realm data persists through pod restarts.

5. If you are using `start-dev` (Option A with embedded H2), realm resets on pod restart.
   For production, always use a database-backed Keycloak.

### Export Realm as JSON (backup)

After configuring the realm, export it so you can re-import if the pod restarts:

    kubectl exec -n jira-clone -it deployment/keycloak -- \
      /opt/keycloak/bin/kc.sh export \
      --dir /tmp/export \
      --realm jira-clone \
      --users realm_file

    kubectl cp jira-clone/<keycloak-pod-name>:/tmp/export/jira-clone-realm.json ./jira-clone-realm.json

To auto-import the realm on startup, mount the JSON file as a ConfigMap and use
the `--import-realm` flag in Keycloak's command:

    command: ["start-dev", "--import-realm"]
    volumeMounts:
      - name: realm-config
        mountPath: /opt/keycloak/data/import
    volumes:
      - name: realm-config
        configMap:
          name: keycloak-realm-config

---

## Part 12 — Startup Order in Kubernetes

Kubernetes does not guarantee pod startup order by default.
The manifests handle this with `initContainers` that block until dependencies are ready:

    MySQL pod starts
         ↓
    Keycloak initContainer waits for MySQL:3306 (TCP check)
    Keycloak pod starts → connects to MySQL for realm storage
         ↓
    Backend initContainer 1 waits for MySQL:3306
    Backend initContainer 2 waits for Keycloak:8080
    Backend pod starts → Spring Boot validates Keycloak issuer-uri
         ↓
    Frontend pod starts (no dependencies — it serves static files)

---

## Part 13 — Troubleshooting

### Pod stuck in "Pending"

    kubectl describe pod -n jira-clone <pod-name>

Look at the "Events" section at the bottom. Common causes:
- Insufficient CPU/memory: `minikube start --memory=4096 --cpus=2`
- PVC cannot bind (no storage class available): check `kubectl get storageclass`

### Pod stuck in "CrashLoopBackOff"

    kubectl logs -n jira-clone <pod-name> --previous

`--previous` shows logs from the crashed container.
Common causes: wrong environment variables, DB connection refused, wrong image tag.

### Backend can't connect to MySQL

    # Check MySQL pod is Running
    kubectl get pods -n jira-clone

    # Check MySQL service resolves inside the cluster
    kubectl exec -n jira-clone deployment/jira-clone-backend -- nslookup mysql

    # Check the DB password in the Secret matches application.properties
    kubectl get secret -n jira-clone mysql-secret -o jsonpath='{.data.root-password}' | base64 -d

### Backend can't connect to Keycloak (startup fails with issuer-uri error)

Keycloak must be Running AND the realm endpoint must return JSON before Spring Boot starts.
The initContainer should handle this, but if it doesn't:

    # Check Keycloak is up
    kubectl port-forward -n jira-clone service/keycloak 8180:8080
    curl http://localhost:8180/realms/jira-clone

    # Restart the backend after Keycloak is ready
    kubectl rollout restart -n jira-clone deployment/jira-clone-backend

### ImagePullBackOff

The image tag doesn't exist in the registry, or you forgot `imagePullPolicy: Never` for local images.

    # For Minikube local images: rebuild inside Minikube's Docker
    eval $(minikube docker-env)
    docker build -t yourdockerhubusername/jira-clone-backend:1.0.0 .

    # Add to deployment spec:
    imagePullPolicy: Never

### Ingress returns 404 for /api routes

Check the ingress is using the correct path and service name:

    kubectl get ingress -n jira-clone -o yaml
    kubectl describe ingress -n jira-clone jira-clone-ingress

### CORS errors in browser after deploying

Update `CorsConfig.java` to include the Kubernetes external URL or Ingress domain in
`allowedOrigins`. The current config likely only allows `http://localhost:4200`.

---

## File Structure Summary

    k8s/
    ├── 00-namespace.yaml         Namespace: jira-clone
    ├── 01-secrets.yaml           mysql-secret, keycloak-secret, backend-secret
    ├── 02-backend-config.yaml    ConfigMap with application.properties
    ├── 03-mysql.yaml             PVC + StatefulSet + Service (headless)
    ├── 04-keycloak.yaml          Deployment + Service
    ├── 05-backend.yaml           Deployment + Service (with initContainers)
    ├── 06-frontend.yaml          Deployment + Service (nginx)
    └── 07-ingress.yaml           Ingress routing frontend + backend + keycloak

    jira-clone-backend/
    └── Dockerfile                eclipse-temurin:17-jre-alpine, runs app.jar

    jira-clone-frontend/
    ├── Dockerfile                multi-stage: node:20 build + nginx:alpine serve
    └── nginx.conf                SPA routing (try_files -> index.html)
