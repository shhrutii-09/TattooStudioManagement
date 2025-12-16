package clientDTO;

import java.io.Serializable;

public class BookAppointmentDTO implements Serializable {
    private Long designId;
    private Long artistId;
    private String designTitle;
    private String designImage;
    private String artistName;
    private Double price;

    private String allergyDetails; 
    private Boolean diabetes = false;
    private Boolean heartCondition = false;
    private Boolean isPregnant = false;
    private boolean above18 = false; 
    private boolean consentGiven = false; 

//    private Integer slotId;
    private String clientNote;

    // --- GETTERS AND SETTERS (MUST BE PRESENT FOR JSF) ---
    public Long getDesignId() { return designId; }
    public void setDesignId(Long designId) { this.designId = designId; }

    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }

    public String getDesignTitle() { return designTitle; }
    public void setDesignTitle(String designTitle) { this.designTitle = designTitle; }

    public String getDesignImage() { return designImage; }
    public void setDesignImage(String designImage) { this.designImage = designImage; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getAllergyDetails() { return allergyDetails; }
    public void setAllergyDetails(String allergyDetails) { this.allergyDetails = allergyDetails; }

    public Boolean getDiabetes() { return diabetes; }
    public void setDiabetes(Boolean diabetes) { this.diabetes = diabetes; }

    public Boolean getHeartCondition() { return heartCondition; }
    public void setHeartCondition(Boolean heartCondition) { this.heartCondition = heartCondition; }

    public Boolean getIsPregnant() { return isPregnant; }
    public void setIsPregnant(Boolean isPregnant) { this.isPregnant = isPregnant; }

    public boolean isAbove18() { return above18; }
    public void setAbove18(boolean above18) { this.above18 = above18; }

    public boolean isConsentGiven() { return consentGiven; }
    public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }

//    public Integer getSlotId() { return slotId; }
//    public void setSlotId(Integer slotId) { this.slotId = slotId; }

    public String getClientNote() { return clientNote; }
    public void setClientNote(String clientNote) { this.clientNote = clientNote; }
}