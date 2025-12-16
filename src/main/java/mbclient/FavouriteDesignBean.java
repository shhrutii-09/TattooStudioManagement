package beans;

import clientDTO.FavouriteDesignDTO;
import ejb.ClientEJBLocal;
import entities.DesignFavourite;
import entities.TattooDesign;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("favouriteDesignBean")
@ViewScoped
public class FavouriteDesignBean implements Serializable {

    @EJB
    private ClientEJBLocal clientEJB;

    @Inject
    private UserSessionBean userSession;

    private List<FavouriteDesignDTO> favourites;

    @PostConstruct
    public void init() {
        loadFavourites();
    }

    private void loadFavourites() {
        favourites = new ArrayList<>();

        Long clientId = userSession.getUserId();
        if (clientId == null) {
            return; // not logged in
        }

        List<DesignFavourite> favEntities =
                clientEJB.listFavourites(clientId, 0, 100);

        for (DesignFavourite fav : favEntities) {
            TattooDesign d = fav.getDesign();

            FavouriteDesignDTO dto = new FavouriteDesignDTO();
            dto.setDesignId(d.getDesignId());
            dto.setTitle(d.getTitle());
            dto.setImagePath(d.getImagePath());
            dto.setStyle(d.getStyle());
            dto.setPrice(d.getPrice());

            dto.setArtistId(d.getArtist().getUserId());
            dto.setArtistName(d.getArtist().getFullName());

            favourites.add(dto);
        }
    }

    // -------- navigation helpers --------

    public String viewDesign(Long designId) {
        return "/web/client/design.xhtml?faces-redirect=true&designId=" + designId;
    }

    public String bookAppointment(Long artistId, Long designId) {
        return "/web/client/book.xhtml?faces-redirect=true"
                + "&artistId=" + artistId
                + "&designId=" + designId;
    }

    // -------- getters --------

    public List<FavouriteDesignDTO> getFavourites() {
        return favourites;
    }
}
