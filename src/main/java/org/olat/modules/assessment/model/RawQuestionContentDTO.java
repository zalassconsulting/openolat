package org.olat.modules.assessment.model;

public class RawQuestionContentDTO {

    private final String group;
    private final String question;
    private final String answers;
    private final String correct;
    private final String type;
    private final Integer difficulty;

    public RawQuestionContentDTO(String group, String question, String answers, String correct, String type, Integer difficulty) {
        this.group = group;
        this.question = question;
        this.answers = answers;
        this.correct = correct;
        this.type = type;
        this.difficulty = difficulty;
    }

    public String getGroup() {
        return group;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswers() {
        return answers;
    }

    public String getCorrect() {
        return correct;
    }

    public String getType() {
        return type;
    }

    public Integer getDifficulty() {
        return difficulty;
    };
}
