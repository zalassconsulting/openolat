package org.olat.course.assessment.ui.tool;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.olat.core.gui.media.MediaResource;
import jakarta.servlet.http.HttpServletResponse;

public class RemoteFileMediaResource implements MediaResource {

    private byte[] data;
    private String contentType;
    private String downloadName;

    public RemoteFileMediaResource(String urlString, String downloadName, String contentType) throws Exception {
        this.downloadName = downloadName;
        this.contentType = contentType;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (InputStream is = conn.getInputStream()) {
            this.data = is.readAllBytes();
        }
    }

    @Override
    public long getCacheControlDuration() {
        return 0;
    }

    @Override
    public boolean acceptRanges() {
        return false;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public Long getLastModified() {
        return null;
    }

    @Override
    public Long getSize() {
        return (long) data.length;
    }

    @Override
    public void prepare(HttpServletResponse hres) {
        hres.setHeader("Content-Disposition", "attachment; filename=\"" + downloadName + "\"");
    }

    @Override
    public void release() {
        data = null;
    }
}