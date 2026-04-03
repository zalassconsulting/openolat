package org.olat.course.assessment.ui.tool;

public class SmpCustomEvaluationFormUploadResult {

    private final int status;
    private final String message;

    public SmpCustomEvaluationFormUploadResult(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}