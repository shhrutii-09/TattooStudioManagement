package mbclient;

import clientDTO.ArtistCardDTO;
import ejb.ClientEJBLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class ArtistsBrowseBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ClientEJBLocal clientEJB;

    private List<ArtistCardDTO> artists;

    @PostConstruct
    public void init() {
        artists = clientEJB.getAllArtistsForBrowse();
    }

    public List<ArtistCardDTO> getArtists() {
        return artists;
    }
}
