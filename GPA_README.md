# Spring Batch Dashboard

This repository is a fork of:

https://github.com/making/spring-batch-dashboard

This feature was added for the Groeipakket environment because `data_flow_service` schema provides
`GPA_BATCH_*` views that combine the Spring Batch `BATCH_*` and `BOOT3_BATCH_*` tables.

The implementation is based on the following upstream pull request:

https://github.com/making/spring-batch-dashboard/pull/6

The dashboard can now read Spring Batch metadata from tables with a configurable prefix through
`spring.batch.jdbc.table-prefix`. The default value remains empty to preserve the existing behavior.

Prefixed table names are centralized and applied consistently to all job, execution, parameter, step, and
statistics queries. The configured prefix is validated to prevent unsafe SQL identifiers, and tests cover both
the default and custom-prefix scenarios.

We hope this pull request will be accepted by the project contributors. If it is not accepted, maintaining a
separate repository or dedicated service may be a better long-term solution.

## Releasing a new version to the GPA environment

The following steps are currently required to release and deploy a new version to the GPA environment.

For now, the image is pushed directly to Nexus. Once the upstream pull request is accepted, the release process
can be adapted to use the official upstream repository.

### 1. Log in to Nexus

```bash
docker login prd-nexus.groeipakketapplicatie.be:5000
```

Enter the Nexus username and password when prompted.

### 2. Build the image

Use an AMD64 build because the GPA Kubernetes cluster runs on AMD64 nodes:

```bash
./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.builder=paketobuildpacks/builder-jammy-base:latest \
  -Dspring-boot.build-image.imagePlatform=linux/amd64 \
  -Dspring-boot.build-image.imageName=spring-batch-dashboard:0.0.2
```

Replace `0.0.2` with the version being released.

### 3. Tag the image for Nexus

```bash
docker tag spring-batch-dashboard:0.0.2 \
  prd-nexus.groeipakketapplicatie.be:5000/spring-batch-dashboard:0.0.2
```

### 4. Push the image to Nexus

```bash
docker push \
  prd-nexus.groeipakketapplicatie.be:5000/spring-batch-dashboard:0.0.2
```

### 5. Update the GPA deployment

Update the dashboard image version in:

```text
Projects/groeipakket/continuous-delivery/jobs/deployment/templates/batch-trigger-service/03-deployment.yml
```

Set the image to:

```yaml
image: "prd-nexus.groeipakketapplicatie.be:5000/spring-batch-dashboard:0.0.2"
```

The CI/CD pipeline can then deploy the updated version to the GPA environment.
