package mbclient;

import entities.TattooDesign;
import entities.DesignFavourite;
import ejb.ClientEJBLocal;
import beans.UserSessionBean;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.context.FacesContext;
import jakarta.faces.application.FacesMessage;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Named("browseDesignsBean")
@ViewScoped
public class BrowseDesignsBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int DESIGNS_PER_PAGE = 20;

    @EJB
    private ClientEJBLocal clientEJB;

    @Inject
    private UserSessionBean userSessionBean;

    // -------- Filters --------
    private String searchKeyword = "";
    private String selectedStyle = "";
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy = "newest";

    // -------- Pagination --------
    private List<TattooDesign> designs = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;
    private long totalDesigns = 0;

    // -------- User State --------
    private Set<Long> userFavourites = new HashSet<>();
    private Set<Long> userLikes = new HashSet<>();

    // -------- Init --------
    @PostConstruct
    public void init() {
        loadUserLikesAndFavourites();
        searchDesigns();
    }

    private void loadUserLikesAndFavourites() {
        if (userSessionBean != null && userSessionBean.isLoggedIn() && userSessionBean.isClient()) {
            Long uid = userSessionBean.getUserId();

            // favourites
            List<DesignFavourite> favs =
                clientEJB.listFavourites(uid, 0, Integer.MAX_VALUE);

            userFavourites = favs.stream()
                    .map(f -> f.getDesign().getDesignId())
                    .collect(Collectors.toSet());

            // likes
            userLikes = new HashSet<>(clientEJB.listLikedDesignIds(uid));
        } else {
            userFavourites.clear();
            userLikes.clear();
        }
    }

    // -------- Search --------
    public void searchDesigns() {
    int offset = (currentPage - 1) * DESIGNS_PER_PAGE;
    
    // Get designs from EJB
    List<TattooDesign> results = clientEJB.searchDesigns(
            searchKeyword == null || searchKeyword.isBlank() ? null : searchKeyword,
            selectedStyle == null || selectedStyle.isBlank() ? null : selectedStyle,
            minPrice,
            maxPrice,
            offset,
            DESIGNS_PER_PAGE
    );
    
    // Sort the results
    designs = sortDesigns(results);
    
    // Get total count from database
    totalDesigns = getTotalDesignsCount();
    totalPages = (int) Math.ceil((double) totalDesigns / DESIGNS_PER_PAGE);
}

    // -------- Sorting --------
    private List<TattooDesign> sortDesigns(List<TattooDesign> input) {
        if (input == null || input.isEmpty()) return input;

    List<TattooDesign> sorted = new ArrayList<>(input);
    
    switch (sortBy) {
        case "newest":
            sorted.sort((a, b) -> {
                if (a.getUploadedAt() == null && b.getUploadedAt() == null) return 0;
                if (a.getUploadedAt() == null) return 1;
                if (b.getUploadedAt() == null) return -1;
                return b.getUploadedAt().compareTo(a.getUploadedAt());
            });
            break;

        case "popular":
            sorted.sort((a, b) -> {
                int aLikes = a.getLikes() != null ? a.getLikes().size() : 0;
                int aFavs = a.getFavourites() != null ? a.getFavourites().size() : 0;
                int bLikes = b.getLikes() != null ? b.getLikes().size() : 0;
                int bFavs = b.getFavourites() != null ? b.getFavourites().size() : 0;
                int aScore = aLikes + aFavs;
                int bScore = bLikes + bFavs;
                return Integer.compare(bScore, aScore);
            });
            break;

        case "price_low":
            sorted.sort((a, b) -> {
                BigDecimal aPrice = a.getPrice() != null ? a.getPrice() : BigDecimal.ZERO;
                BigDecimal bPrice = b.getPrice() != null ? b.getPrice() : BigDecimal.ZERO;
                return aPrice.compareTo(bPrice);
            });
            break;

        case "price_high":
            sorted.sort((a, b) -> {
                BigDecimal aPrice = a.getPrice() != null ? a.getPrice() : BigDecimal.ZERO;
                BigDecimal bPrice = b.getPrice() != null ? b.getPrice() : BigDecimal.ZERO;
                return bPrice.compareTo(aPrice);
            });
            break;
    }
    return sorted;
    }

    // -------- Like / Favourite --------
    public void toggleLike(Long designId) {
        if (!userSessionBean.isLoggedIn()) return;

        boolean liked = clientEJB.toggleDesignLike(
                userSessionBean.getUserId(), designId);

        if (liked) userLikes.add(designId);
        else userLikes.remove(designId);
    }

    public boolean isLiked(Long designId) {
        return userLikes.contains(designId);
    }

   public void toggleFavourite(Long designId) {
    if (!userSessionBean.isLoggedIn()) return;
    
    Long uid = userSessionBean.getUserId();
    
    // Use the toggleFavouriteDesign method from EJB
    boolean added = clientEJB.toggleFavouriteDesign(uid, designId);
    
    if (added) {
        userFavourites.add(designId);
        showMsg("Added to favourites ❤️");
    } else {
        userFavourites.remove(designId);
        showMsg("Removed from favourites");
    }
    
    // Refresh the page to update counts
    searchDesigns();
}

    public boolean isFavourite(Long designId) {
        return userFavourites.contains(designId);
    }

    private void showMsg(String msg) {
        FacesContext.getCurrentInstance().addMessage(
                null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    // -------- Helpers --------
    public List<String> getAvailableStyles() {
        return clientEJB.getAvailableStyles();
    }

    public boolean hasDesigns() {
       return designs != null && !designs.isEmpty();
    }

    // -------- Getters --------
    public List<TattooDesign> getDesigns() { return designs; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }
    public long getTotalDesigns() { return totalDesigns; }

    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String searchKeyword) { this.searchKeyword = searchKeyword; }

    public String getSelectedStyle() { return selectedStyle; }
    public void setSelectedStyle(String selectedStyle) { this.selectedStyle = selectedStyle; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    currentPage = 1; // Reset to first page when sorting changes
    searchDesigns();
    }
    
    // Add these methods to your BrowseDesignsBean.java

public void goToPage(int page) {
    if (page < 1 || page > totalPages) return;
    currentPage = page;
    searchDesigns();
}

public List<Integer> getPageNumbers() {
    List<Integer> pages = new ArrayList<>();
    int start = Math.max(1, currentPage - 2);
    int end = Math.min(totalPages, currentPage + 2);
    
    for (int i = start; i <= end; i++) {
        pages.add(i);
    }
    return pages;
}

public void clearFilters() {
    searchKeyword = "";
    selectedStyle = "";
    minPrice = null;
    maxPrice = null;
    currentPage = 1;
    searchDesigns();
}

// Update the searchDesigns method to fix pagination


// Add this method to get total count
private long getTotalDesignsCount() {
    // You need to implement a count method in ClientEJB
    // For now, return designs.size() * 2 as placeholder
    return designs.size();
}


}
