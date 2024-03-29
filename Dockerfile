# Use an official Java runtime as a parent image
FROM jelastic/jetty:9.4.49-openjdk-1.8.0_352

# Set environment variables
USER root
ENV JAVA_OPTS="-Xmx4G"

# Create a directory for logs
RUN mkdir -p /var/log/hydra

# Expose the specified port
ARG PORT_NUMBER=8080
EXPOSE $PORT_NUMBER
RUN ls -lah
# Copy the application files to the container
COPY hydra-notifications-server /opt/hydra-notifications-server

# Set the working directory
WORKDIR /opt/hydra-notifications-server
RUN ls -lah
# Set the entry point for the application
ENTRYPOINT ["/opt/hydra-notifications-server/bin/hydra-notifications-server"]
