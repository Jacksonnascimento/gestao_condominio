# 1. Estágio de build
FROM node:20-slim AS build
WORKDIR /app

ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
# Os redirecionamentos para a API são gravados no build; no Docker, a API é o serviço condigtal-api
ARG API_INTERNA_URL=http://condigtal-api:8080
ENV API_INTERNA_URL=$API_INTERNA_URL

COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 2. Estágio de execução
FROM node:20-slim
WORKDIR /app

COPY --from=build /app/public ./public
COPY --from=build /app/.next ./.next
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package.json ./package.json

EXPOSE 3000
CMD ["npm", "start"]
