FROM java:8
VOLUME /tmp
ADD sample-client-0.0.1.jar app.jar
ADD entry.sh entry.sh
RUN bash -c 'touch /app.jar'
EXPOSE 80
ENTRYPOINT ["sh", "/entry.sh"]