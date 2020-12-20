package com.backbase.oss.util;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

import com.backbase.oss.boat.Utils;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.openapitools.codegen.auth.AuthParser;

@RequiredArgsConstructor
public class CachedResource {
    private final File cache;
    @Getter
    private final String location;
    @Setter
    private String auth;

    @Getter(lazy = true)
    private final File file = createLocal();

    @Getter(lazy = true)
    private final String hash = calculateHash();

    public CachedResource(String location) {
        this(new File(System.getProperty("user.home"), ".cache/boat"), location);
    }

    @SneakyThrows
    private String calculateHash() {
        return Files.asByteSource(getFile()).hash(Hashing.sha256()).toString();
    }

    @SneakyThrows
    private File createLocal() {
        if (this.location.matches("^https?://.+")) {
            return tryRemote();
        }

        try {
            return tryLocalFile();
        } catch (final IOException e) {
        }

        final URL resource = fromClasspath();
        final File cached = localName(resource);

        if (localExists(cached)) {
            return cached;
        }

        try (InputStream is = resource.openStream()) {
            copy(is, cached);
        }

        return cached;
    }

    private File tryRemote() throws IOException {
        final URL remote = new URL(this.location);
        final File cached = localName(remote);

        if (localExists(cached)) {
            return cached;
        }

        final URLConnection conn = remote.openConnection();

        AuthParser.parse(this.auth)
            .forEach(a -> conn.addRequestProperty(a.getKeyName(), a.getValue()));

        try (InputStream is = conn.getInputStream()) {
            copy(is, cached);
        }

        return cached;
    }

    private boolean localExists(final File cached) throws IOException {
        return cached.exists() && java.nio.file.Files.size(cached.toPath()) > 0;
    }

    private File tryLocalFile() throws IOException {
        final File file = new File(this.location);
        final File parent = file.getParentFile();
        final String[] files = Utils.selectInputs(parent.toPath(), file.getName());

        switch (files.length) {
            case 0:
                throw new FileNotFoundException(
                    format("Input %s doesn't match any local file", this.location));

            case 1:
                return new File(parent, files[0]);

            default:
                throw new IOException(
                    format("Input %s matches more than one single file", this.location));
        }
    }

    private URL fromClasspath() throws FileNotFoundException {
        final URL resource = currentThread().getContextClassLoader().getResource(this.location);

        if (resource != null) {
            return resource;
        }

        throw new FileNotFoundException(this.location);
    }

    private void copy(InputStream is, File dest) throws IOException {
        final ByteSource bytes = ByteSource.wrap(ByteStreams.toByteArray(is));

        dest.getParentFile().mkdirs();

        try (FileOutputStream os = new FileOutputStream(dest)) {
            bytes.copyTo(os);
        }
    }

    private File localName(URL source) throws IOException {
        final String path = source.getPath();
        final String hash = CharSource
            .wrap(source.toExternalForm())
            .asByteSource(StandardCharsets.UTF_8)
            .hash(Hashing.farmHashFingerprint64())
            .toString();

        return new File(this.cache, format("%s-%s.%s", getBaseName(path), hash, getExtension(path)));
    }
}

