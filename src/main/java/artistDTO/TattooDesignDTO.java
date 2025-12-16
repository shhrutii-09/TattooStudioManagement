/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package artistDTO;

import java.io.Serializable;
import java.math.BigDecimal;


public class TattooDesignDTO implements Serializable {


private Long designId;
private String title;
private String description;
private String style;
private BigDecimal price;
private String imagePath;


// ---------- getters & setters ----------


public Long getDesignId() { return designId; }
public void setDesignId(Long designId) { this.designId = designId; }


public String getTitle() { return title; }
public void setTitle(String title) { this.title = title; }


public String getDescription() { return description; }
public void setDescription(String description) { this.description = description; }


public String getStyle() { return style; }
public void setStyle(String style) { this.style = style; }


public BigDecimal getPrice() { return price; }
public void setPrice(BigDecimal price) { this.price = price; }


public String getImagePath() { return imagePath; }
public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}