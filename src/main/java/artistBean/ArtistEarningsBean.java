package artistBean;

import beans.UserSessionBean;
import ejb.ArtistEJBLocal;
import entities.EarningLog;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Named("artistEarningsBeanA")

@ViewScoped
public class ArtistEarningsBean implements Serializable {

    @Inject
    private ArtistEJBLocal artistEJB;

    @Inject
    private UserSessionBean userSession;

    private List<EarningLog> earnings;
    private BigDecimal pendingTotal;

    @PostConstruct
    public void init() {
        Long artistId = userSession.getUserId(); // 🔐 ONLY logged-in artist
        earnings = artistEJB.listEarningLogsForArtist(artistId, 0, 50);
        pendingTotal = artistEJB.calculateArtistPendingEarnings(artistId);
    }

    public List<EarningLog> getEarnings() {
        return earnings;
    }

    public BigDecimal getPendingTotal() {
        return pendingTotal;
    }
}
