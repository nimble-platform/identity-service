FROM java:8
VOLUME /tmp
ENV PORT=20001
ADD app.jar app.jar
ADD entrypoint.sh entrypoint.sh
RUN bash -c 'touch /app.jar'
EXPOSE 20001
ENTRYPOINT ["sh", "/entrypoint.sh"]