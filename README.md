# drogue-ditto-converter project

This is a simple [Knative](https://knative.dev/) event payload converter from Drogue Cloud to Eclipse Ditto.

## Build

```shell script
./mvnw package
docker build -f src/main/docker/Dockerfile.jvm -t quay.io/dejanb/drogue-ditto-converter-jvm .
docker push quay.io/dejanb/drogue-ditto-converter-jvm
```
## Deploy

```shell script
kubectl -n drogue-iot apply -f src/main/k8s/ditto-converter.yaml
```

### Use

```shell script
# Create Drogue Cloud resources
drg create app app_id
drg create device --app app_id simple-thing --data '{"credentials": {"credentials":[{ "pass": "foobar" }]}}'

# Create Ditto resources
cat src/main/ditto/simple-thing.json | http --auth ditto:ditto PUT http://$TWIN_API/api/2/things/app_id:simple-thing

# Send telemetry
http --auth simple-thing@app_id:foobar --verify build/certs/endpoints/ca-bundle.pem POST https://$HTTP_ENDPOINT/v1/foo data_schema==ditto:test temp:=23

# Check device state
http --auth ditto:ditto http://$TWIN_API/api/2/things/app_id:simple-thing
```