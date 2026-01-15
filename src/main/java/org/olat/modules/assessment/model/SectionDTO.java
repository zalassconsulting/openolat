package org.olat.modules.assessment.model;

import java.util.List;

public class SectionDTO {

    private final String title;
    private final Integer diff;
    private final List<QuestionDTO> qsts;

    public SectionDTO(String title, List<QuestionDTO> qsts) {
        if(qsts == null || qsts.isEmpty()) throw new IllegalArgumentException();
        this.qsts = qsts;
        this.title = title;
        this.diff = getQsts().get(0).getDifficulty();
    }

    public List<QuestionDTO> getQsts() {
        return qsts;
    }

    public String getTitle() {
        return title;
    }

    public Integer getDiff() {
        return diff;
    }
}
