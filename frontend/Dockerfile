# Frontend Dockerfile
# 1. Use Node.js 22 Alpine image as the build stage
# 2. Set the working directory to /app
# 3. Copy package.json and package-lock.json and install dependencies
# 4. Copy the source code and build the application
# 5. Use Nginx 1.27 Alpine image as the runtime stage, copy the Nginx configuration and built files,
# expose port 80, and set the entry point to run Nginx

FROM node:22-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
