# Build context: repository root (see docker-compose.yml)

# ---- Build ----
FROM node:26 AS build
WORKDIR /build
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend .
RUN npm run build

# ---- Runtime ----
FROM nginx:alpine
COPY infrastructure/docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
