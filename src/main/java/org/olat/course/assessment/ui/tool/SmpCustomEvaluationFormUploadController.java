package org.olat.course.assessment.ui.tool;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.logging.Tracing;

import java.io.File;

public class SmpCustomEvaluationFormUploadController {

    private static final Logger log = Tracing.createLoggerFor(SmpCustomEvaluationFormUploadController.class);

    private static final String BASE_URL = "http://localhost:8080/evaluation-form";

    public SmpCustomEvaluationFormUploadResult sendEvaluationFile(File file, String fileName, Long fkSession) {

        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(BASE_URL+"/upload");

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

    public void downloadTemplate(UserRequest ureq) {
        String url = BASE_URL+"/template";
        try {
            MediaResource mr = new RemoteFileMediaResource(
                    url,
                    "evaluation-form-template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
            ureq.getDispatchResult().setResultingMediaResource(mr);
            log.info("Template download initiated");
        } catch (Exception e) {
            log.error("Failed to download template", e);
        }
    }

    public void downloadForm(UserRequest ureq, Long fkSession, String name) {
        String url = BASE_URL+"/download?fkSession=" + fkSession;
        try {
            String fileName = "evaluation-form-session-" + name + "-fk-" + fkSession + ".xlsx";
            MediaResource mr = new RemoteFileMediaResource(
                    url,
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
            ureq.getDispatchResult().setResultingMediaResource(mr);
            log.info("Form download initiated for fkSession={}, user={}", fkSession, name);
        } catch (Exception e) {
            log.error("Failed to download form for fkSession={}, user={}", fkSession, name, e);
        }
    }
}