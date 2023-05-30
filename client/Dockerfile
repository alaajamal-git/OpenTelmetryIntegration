FROM maven:3.8.3-openjdk-17
COPY /client /app
WORKDIR /app
RUN mvn clean
CMD [ "mvn", "spring-boot:run" ]
