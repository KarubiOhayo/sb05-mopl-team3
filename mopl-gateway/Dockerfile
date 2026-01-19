FROM nginx:alpine

COPY mopl-gateway/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY mopl-gateway/frontend/dist/ /usr/share/nginx/html/

EXPOSE 80
