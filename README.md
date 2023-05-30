# openTelemetry

### Run with scenario 1
```
docker-compose --profile=scenario1 up -d --build
```
### Run with scenario 2
```
docker-compose --profile=scenario2 up -d --build
```
### Send request
```
curl --location 'http://localhost:8080/call' \
--header 'Content-Type: text/plain' \
--data 'test'
```
### Note
please specify the app env variable (EXTERNAL_SERVICE) to setup the observable endpoint before build the docker environment.
