package artistDTO;

import java.io.Serializable;
import java.time.LocalDate;

public class ArtistAppointmentFilterDTO implements Serializable {

    private String clientName;
    private String status = "ALL";
    private LocalDate startDate;
    private LocalDate endDate;

    private int page = 0;
    private int size = 10;

    public boolean hasFilters() {
        return (clientName != null && !clientName.isBlank())
            || (startDate != null)
            || (endDate != null)
            || (!"ALL".equals(status));
    }

    // getters & setters

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
