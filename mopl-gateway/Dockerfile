FROM nginx:alpine

ARG NGINX_CONF=default.local.conf

COPY mopl-gateway/nginx/ /tmp/nginx/
RUN cp /tmp/nginx/${NGINX_CONF} /etc/nginx/conf.d/default.conf
COPY mopl-gateway/frontend/dist/ /usr/share/nginx/html/

EXPOSE 80
