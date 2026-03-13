FROM gradle:8-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install GStreamer and required plugins
# Note: native GStreamer libraries must be present for gst1-java-core JNA bindings to work.
RUN apk add --no-cache \
    gstreamer \
    gst-plugins-base \
    gst-plugins-good \
    gst-plugins-bad \
    gst-plugins-ugly \
    gstreamer-dev \
    ffmpeg

COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
