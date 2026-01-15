package org.olat.modules.assessment.model;

public class UserDTO {

    private String firstName;
    private String lastName;
    private String email;
    private Long lwId;
    private Long organisationId;
    private Integer dealerId;
    private String login;

    public UserDTO(String firstName, String lastName, String email, Long lwId, Long organisationId, Integer dealerId, String login) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.lwId = lwId;
        this.organisationId = organisationId;
        this.login = login;
        this.dealerId = dealerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public Long getLwId() {
        return lwId;
    }

    public Long getOrganisationId() {
        return organisationId;
    }

    public String getLogin() {
        return login;
    }

    public Integer getDealerId() {
        return dealerId;
    }
}
