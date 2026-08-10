FROM node:20.16-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps --no-audit --no-fund
COPY public ./public
COPY src ./src
RUN npm run build

FROM nginx:1.31.3-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
