FROM node:22-alpine AS build

WORKDIR /app

ARG NPM_REGISTRY=https://registry.npmmirror.com/
ARG ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/
ENV npm_config_registry=${NPM_REGISTRY} \
    ELECTRON_MIRROR=${ELECTRON_MIRROR}

COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts
RUN node node_modules/electron/install.js

COPY . .

ARG TRANSLATION_API_URL=http://127.0.0.1:18001/
RUN TRANSLATION_API_URL="$TRANSLATION_API_URL" node -e 'const fs = require("node:fs"); const file = "public/config/index.js"; const source = fs.readFileSync(file, "utf8"); const pattern = /^\s*serverURL:\s*"[^"]+"/m; if (!pattern.test(source)) throw new Error("Could not locate the frontend serverURL setting."); fs.writeFileSync(file, source.replace(pattern, "  serverURL: " + JSON.stringify(process.env.TRANSLATION_API_URL)));'

RUN npm run build

FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ /usr/share/nginx/html/

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
