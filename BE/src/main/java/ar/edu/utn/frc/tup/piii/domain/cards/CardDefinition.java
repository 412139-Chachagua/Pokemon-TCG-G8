package ar.edu.utn.frc.tup.piii.domain.cards;

import java.util.List;

public abstract class CardDefinition {
    protected String id;
    protected String name;
    protected String supertype;
    protected List<String> subtypes;
    protected String setCode;
    protected String number;
    protected String imageSmallUrl;
    protected String imageLargeUrl;
    protected List<String> rulesText;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSupertype() { return supertype; }
    public void setSupertype(String supertype) { this.supertype = supertype; }
    public List<String> getSubtypes() { return subtypes; }
    public void setSubtypes(List<String> subtypes) { this.subtypes = subtypes; }
    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getImageSmallUrl() { return imageSmallUrl; }
    public void setImageSmallUrl(String imageSmallUrl) { this.imageSmallUrl = imageSmallUrl; }
    public String getImageLargeUrl() { return imageLargeUrl; }
    public void setImageLargeUrl(String imageLargeUrl) { this.imageLargeUrl = imageLargeUrl; }
    public List<String> getRulesText() { return rulesText; }
    public void setRulesText(List<String> rulesText) { this.rulesText = rulesText; }
}
