# Use an official Java runtime as a parent image
FROM java

# Set environment variables
USER root
ENV JAVA_OPTS="-Xmx4G"

# Create a directory for logs
RUN mkdir -p /var/log/hydra

# Expose the specified port
ARG PORT_NUMBER=8080
EXPOSE $PORT_NUMBER

# Copy the application files to the container
COPY . /opt/hydra-notifications-server

# Set the working directory
WORKDIR /opt/hydra-notifications-server

# Set the entry point for the application
ENTRYPOINT ["/opt/hydra-notifications-server/bin/hydra-notifications-server"]
