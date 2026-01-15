package org.olat.modules.assessment.model;

import java.util.List;

public class QuestionDTO {

    private final String type;
    private final String qContent;
    private final List<String> aOpts;
    private final List<Integer> correct;
    private final Integer difficulty;

    public QuestionDTO(String type, String qContent, List<String> aOpts, List<Integer> correct, Integer difficulty) {
        this.type = type;
        this.qContent = qContent;
        this.aOpts = aOpts;
        this.correct = correct;
        this.difficulty = difficulty;
    }

    public String getqContent() {
        return qContent;
    }

    public List<String> getaOpts() {
        return aOpts;
    }

    public List<Integer> getCorrect() {
        return correct;
    }

    public String getType() {
        return type;
    }

    public Integer getDifficulty() {
        return difficulty;
    }
}
