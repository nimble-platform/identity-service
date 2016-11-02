FROM java:8
VOLUME /tmp
ADD app.jar app.jar
ADD entry.sh entry.sh
RUN bash -c 'touch /app.jar'
ENTRYPOINT ["sh", "/entry.sh"]