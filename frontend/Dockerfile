# syntax=docker/dockerfile:1
# Loja Gigaverse3D — build estático servido por nginx sem privilégios (porta 8080, usuário nginx).
# Build:  docker build -t gigaverse3d-web ./frontend

FROM node:24-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
# API na mesma origem (proxy do nginx); não embutir URLs nem segredos no bundle
ENV VITE_API_BASE_URL=
RUN npm run build \
 && rm -f dist/brand/gigaverse-logo-original.png dist/brand/gigaverse-logo-512.png

FROM nginxinc/nginx-unprivileged:1.29-alpine AS runtime
# aplica correções de segurança do Alpine publicadas depois da imagem base (ex.: brotli-libs CVE-2026-111497)
USER root
RUN apk upgrade --no-cache
USER 101
# o template vira /etc/nginx/conf.d/default.conf na inicialização (envsubst da imagem oficial)
COPY --chown=nginx:nginx nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build --chown=nginx:nginx /src/dist /usr/share/nginx/html
ENV API_UPSTREAM=http://api:8080 \
    PUBLIC_BASE_URL=https://example.invalid \
    NGINX_ENVSUBST_FILTER="^(API_UPSTREAM|PUBLIC_BASE_URL)$"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=3 CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
