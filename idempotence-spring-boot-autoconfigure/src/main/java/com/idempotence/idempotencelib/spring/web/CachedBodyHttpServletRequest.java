package com.idempotence.idempotencelib.spring.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override public boolean isFinished() { return byteArrayInputStream.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener readListener) {}
            @Override public int read() { return byteArrayInputStream.read(); }
        };
    }

    @Override
    public BufferedReader getReader() {
        Charset charset = getCharacterEncoding() == null
                ? StandardCharsets.UTF_8 : Charset.forName(getCharacterEncoding());
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    public int getCachedBodySize() {
        return cachedBody.length;
    }

    public String getCachedBodyAsString() {
        Charset charset = getCharacterEncoding() == null
                ? StandardCharsets.UTF_8
                : Charset.forName(getCharacterEncoding());

        return new String(cachedBody, charset);
    }
}
