package org.olat.modules.assessment.model;

public class DealerDTO {

    private String name;
    private Integer id;
    private Integer parent;
    private DealerDTO child;

    public DealerDTO(String name, Integer id, Integer parent) {
        this.name = name;
        this.id = id;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public Integer getParent() {
        return parent;
    }

    public DealerDTO getChild() {
        return child;
    }

    public void setChild(DealerDTO child) {
        this.child = child;
    }
}
