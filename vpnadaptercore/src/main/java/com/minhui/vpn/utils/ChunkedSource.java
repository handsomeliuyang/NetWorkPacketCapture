package com.minhui.vpn.utils;

import java.io.IOException;
import java.net.ProtocolException;

import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import okio.Timeout;

public class ChunkedSource implements Source {
    private static final long NO_CHUNK_YET = -1L;
    private long bytesRemainingInChunk = NO_CHUNK_YET;
    private boolean hasMoreChunks = true;

    private final BufferedSource source;

    public ChunkedSource(Source source) {
        this.source = Okio.buffer(source);
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        if (byteCount < 0) throw new IllegalArgumentException("byteCount < 0: " + byteCount);

        if (!hasMoreChunks) return -1;

        if (bytesRemainingInChunk == 0 || bytesRemainingInChunk == NO_CHUNK_YET) {
            readChunkSize();
            if (!hasMoreChunks) return -1;
        }

        long read = source.read(sink, Math.min(byteCount, bytesRemainingInChunk));
        if (read == -1) {
            throw new ProtocolException("unexpected end of stream");
        }
        bytesRemainingInChunk -= read;
        return read;
    }

    private void readChunkSize() throws IOException {
        // Read the suffix of the previous chunk.
        if (bytesRemainingInChunk != NO_CHUNK_YET) {
            source.readUtf8LineStrict();
        }
        try {
            bytesRemainingInChunk = source.readHexadecimalUnsignedLong();
            String extensions = source.readUtf8LineStrict().trim();
            if (bytesRemainingInChunk < 0 || (!extensions.isEmpty() && !extensions.startsWith(";"))) {
                throw new ProtocolException("expected chunk size and optional extensions but was \""
                        + bytesRemainingInChunk + extensions + "\"");
            }
        } catch (NumberFormatException e) {
            throw new ProtocolException(e.getMessage());
        }
        if (bytesRemainingInChunk == 0L) {
            hasMoreChunks = false;
        }
    }

    @Override
    public Timeout timeout() {
        return source.timeout();
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}
