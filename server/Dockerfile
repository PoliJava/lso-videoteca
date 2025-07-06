# server/Dockerfile

# Stage 1: Build the server executable
# Use debian:bookworm-slim and install build-essential to ensure a working GCC
FROM debian:bookworm-slim AS builder

WORKDIR /app

# This copies your source files from the build context (./server) into /app inside the builder container
COPY . .

# Install build dependencies: build-essential (provides gcc), sqlite3, libsqlite3-dev, ca-certificates
# Added 'build-essential' which includes gcc, make, etc.
RUN for i in {1..3}; do \
          apt-get update && \
          apt-get install -y --no-install-recommends \
                  build-essential \
                  sqlite3 \
                  libsqlite3-dev \
                  ca-certificates && \
          rm -rf /var/lib/apt/lists/* && \
          break; \
      done || exit 1

# Now, compile the server.c. Removed -w to ensure all output is shown if there are errors.
RUN gcc -o videoteca-server server.c -lsqlite3 -Wall -Wextra

# Stage 2: Create the lean runtime image
FROM debian:bookworm-slim AS stage-1

# Install runtime dependencies for the final image
RUN for i in {1..3}; do \
        apt-get update && \
        apt-get install -y --no-install-recommends \
            libsqlite3-0 \
            ca-certificates && \
        rm -rf /var/lib/apt/lists/* && \
        break; \
    done || exit 1

# Copy compiled binary from the builder stage
COPY --from=builder /app/videoteca-server /usr/local/bin/

# Create data directory
RUN mkdir -p /data

# Set environment variables
ENV DB_PATH=/data/videoteca.db
ENV PORT=8080

# Verify binary exists and is executable
RUN test -x /usr/local/bin/videoteca-server

# Expose port and run server
EXPOSE $PORT
CMD ["videoteca-server"]