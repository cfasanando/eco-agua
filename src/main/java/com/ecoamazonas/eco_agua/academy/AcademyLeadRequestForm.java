package com.ecoamazonas.eco_agua.academy;

public class AcademyLeadRequestForm {

    private String fullName;
    private String phone;
    private String email;
    private AcademyLead.Source source = AcademyLead.Source.CATALOG;
    private String publicMessage;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AcademyLead.Source getSource() {
        return source;
    }

    public void setSource(AcademyLead.Source source) {
        this.source = source;
    }

    public String getPublicMessage() {
        return publicMessage;
    }

    public void setPublicMessage(String publicMessage) {
        this.publicMessage = publicMessage;
    }
}
