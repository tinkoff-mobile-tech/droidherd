LOCAL_MANIFEST_FILE=testops.tinkoff.ru_droidherdsessions.yaml
out_dir="$(pwd)/../../.."

docker stop kind-control-plane
docker container rm kind-control-plane

docker run \
  --rm \
  -v "$(pwd)/$LOCAL_MANIFEST_FILE":"/$LOCAL_MANIFEST_FILE" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$out_dir":"$out_dir" \
  -ti \
  --network host \
  ghcr.io/kubernetes-client/java/crd-model-gen:v1.0.6 \
  /generate.sh \
  -u /$LOCAL_MANIFEST_FILE \
  -n ru.tinkoff.testops \
  -p ru.tinkoff.testops.droidherd \
  -o "$out_dir"

