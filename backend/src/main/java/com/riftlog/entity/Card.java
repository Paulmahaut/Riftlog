package com.riftlog.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A Riftbound card printing, imported from Riftcodex. */
@Entity
@Table(name = "cards")
public class Card {

    public static final String TYPE_LEGEND = "LEGEND";
    public static final String TYPE_BATTLEFIELD = "BATTLEFIELD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Riftcodex's own id: the only identifier that is unique per printing. */
    @Column(nullable = false, unique = true)
    private String riftcodexId;

    /** Official collector id, e.g. "ogn-001-298". Not unique: shared by some printings. */
    @Column(nullable = false)
    private String riftboundId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String baseName;

    @Column(nullable = false)
    private String cardType;

    private String supertype;

    @Column(nullable = false)
    private String setId;

    private String domains;

    private String imageUrl;

    @Column(nullable = false)
    private boolean variant;

    @Column(nullable = false)
    private boolean canonical;

    @Column(nullable = false)
    private Instant updatedAt;

    public boolean isType(String type) {
        return type.equals(cardType);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRiftcodexId() {
        return riftcodexId;
    }

    public void setRiftcodexId(String riftcodexId) {
        this.riftcodexId = riftcodexId;
    }

    public String getRiftboundId() {
        return riftboundId;
    }

    public void setRiftboundId(String riftboundId) {
        this.riftboundId = riftboundId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getSupertype() {
        return supertype;
    }

    public void setSupertype(String supertype) {
        this.supertype = supertype;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public String getDomains() {
        return domains;
    }

    public void setDomains(String domains) {
        this.domains = domains;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isVariant() {
        return variant;
    }

    public void setVariant(boolean variant) {
        this.variant = variant;
    }

    public boolean isCanonical() {
        return canonical;
    }

    public void setCanonical(boolean canonical) {
        this.canonical = canonical;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
