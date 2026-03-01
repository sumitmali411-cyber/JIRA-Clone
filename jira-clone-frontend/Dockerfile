# Stage 1 — build Angular app
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
