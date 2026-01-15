package org.olat.modules.assessment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity(name = "mapping")
@Table(name="lw_risors_mapping")
@NamedQuery(name = "risorsesByTestIds", query = "select mp from mapping as mp where mp.testId in :testIds")
public class Mapping implements Serializable {

    @Column(name = "test_id")
    private Long testId;

    @Column(name = "risors_id")
    private Long risorsId;

    public Long getTestId() {
        return testId;
    }

    public Long getRisorsId() {
        return risorsId;
    }
}
