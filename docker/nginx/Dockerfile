FROM nginx:alpine

# Copy custom nginx config
COPY nginx.conf /etc/nginx/nginx.conf

# Create non-root user for security (optional, nginx alpine already runs as nginx)
# Run as default nginx user

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]