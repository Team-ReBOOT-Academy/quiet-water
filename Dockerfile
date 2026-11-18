# Запуск уже собранного дистрибутива Gradle (installDist).
# Компиляция внутри образа здесь не выполняется: сеть движка Docker не открывает
# Maven Central, а auth.docker.io не отвечает (TLS handshake timeout).
# Базовый JRE должен уже лежать локально под именем eclipse-temurin:25-jre,
# тогда сборка не обращается к реестру.
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY build/install/quiet-water /app

# Скрипт installDist на Unix делает exec java, поэтому PID 1 ловит SIGTERM.
RUN chmod +x /app/bin/quiet-water

EXPOSE 8888

ENTRYPOINT ["/app/bin/quiet-water"]
