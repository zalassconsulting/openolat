package org.olat.course.assessment.ui.tool;

import java.io.File;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.Logger;
import org.olat.core.logging.Tracing;

public class SmpCustomEvaluationFormUploadController {

    private static final Logger log = Tracing.createLoggerFor(SmpCustomEvaluationFormUploadController.class);


    public SmpCustomEvaluationFormUploadResult sendEvaluationFile(File file, String fileName, Long fkSession) {

        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost("http://localhost:8080/evaluation-form/upload");

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, fileName);
            builder.addTextBody("fkSession", String.valueOf(fkSession), ContentType.TEXT_PLAIN);
            post.setEntity(builder.build());

            HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            return new SmpCustomEvaluationFormUploadResult(status, body);

        } catch (Exception e) {
            log.error("Upload HTTP error", e);
            return new SmpCustomEvaluationFormUploadResult(500, "Connection error: " + e.getMessage()
            );
        }
    }
}