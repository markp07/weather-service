# Use official Node.js image for building
FROM node:25-alpine AS builder
WORKDIR /app

# Accept build arguments for Next.js public environment variables
ARG NEXT_PUBLIC_AUTH_API_URL
ARG NEXT_PUBLIC_WEATHER_API_URL

# Set as environment variables for the build
ENV NEXT_PUBLIC_AUTH_API_URL=$NEXT_PUBLIC_AUTH_API_URL
ENV NEXT_PUBLIC_WEATHER_API_URL=$NEXT_PUBLIC_WEATHER_API_URL

COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Production image
FROM node:25-alpine
WORKDIR /app
COPY --from=builder /app .
EXPOSE 3030
CMD ["npm", "start"]

