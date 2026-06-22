# Build stage via Gradle
FROM gradle:8.14.3-jdk17 AS builder

USER root
RUN mkdir -p /app && chown -R gradle:gradle /app

USER gradle

WORKDIR /app

COPY --chown=gradle:gradle . /app

# Build the distribution zip
RUN gradle --no-daemon distZip

# For testing purposes, replace the Gradle build with the following to reduce delays.
# RUN mkdir -p build/distributions
# RUN curl -L -o build/distributions/quarkdown.zip https://github.com/iamgio/quarkdown/releases/download/latest/quarkdown.zip

WORKDIR build/distributions
RUN unzip quarkdown.zip && rm quarkdown.zip

# Run stage
FROM mcr.microsoft.com/playwright:v1.60.0-noble AS runner

# Install fonts present in the Puppeteer image but missing from Playwright.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        fonts-dejavu-core \
        fonts-kacst \
        fonts-khmeros \
        fonts-thai-tlwg \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/build/distributions/quarkdown quarkdown
ENV PATH="/app/quarkdown/bin:${PATH}"

ENTRYPOINT ["quarkdown"]

LABEL org.opencontainers.image.vendor="Quarkdown"
LABEL org.opencontainers.image.title="Quarkdown Docker image"
LABEL org.opencontainers.image.description="Versatile Markdown-based typesetting system."
LABEL org.opencontainers.image.authors="Giorgio Garofalo (iamgio) <info@quarkdown.com>"
LABEL org.opencontainers.image.url="https://quarkdown.com"
LABEL org.opencontainers.image.source="https://github.com/iamgio/quarkdown"
LABEL org.opencontainers.image.documentation="https://quarkdown.com/wiki"
LABEL org.opencontainers.image.licenses="GPL-3.0-only AND AGPL-3.0-only"
