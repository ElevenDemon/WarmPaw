FROM node:22-bookworm-slim AS build
ENV VITE_API_BASE_URL=/api
ENV NODE_OPTIONS=--max-old-space-size=2048
WORKDIR /build/admin
COPY 2.petVue/package*.json ./
RUN npm ci --no-audit --no-fund
COPY 2.petVue/ ./
RUN npm run build -- --base=/admin/
WORKDIR /build/client
COPY 4.uni-app/package*.json ./
RUN npm ci --no-audit --no-fund
COPY 4.uni-app/ ./
RUN npm run build:h5

FROM nginx:1.28-alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/client/dist/build/h5/ /usr/share/nginx/html/
COPY --from=build /build/admin/dist/ /usr/share/nginx/html/admin/
EXPOSE 80
