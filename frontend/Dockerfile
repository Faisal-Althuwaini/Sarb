# Vite env vars are baked in at build time. Defaults point at the ports
# docker-compose publishes to the HOST (localhost:8080 gateway, localhost:8083
# telemetry WS) - the browser runs on the host, not inside the compose
# network, so it reaches everything the same way whether the backend is
# running via mvnw or via docker compose.
ARG VITE_GATEWAY_URL=http://localhost:8080
ARG VITE_WS_URL=http://localhost:8083/ws

FROM node:22-alpine AS build
ARG VITE_GATEWAY_URL
ARG VITE_WS_URL
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ENV VITE_GATEWAY_URL=${VITE_GATEWAY_URL}
ENV VITE_WS_URL=${VITE_WS_URL}
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
