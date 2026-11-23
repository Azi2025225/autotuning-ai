package com.autotuning.model;

public enum StatutCommande {
    EN_ATTENTE("En attente", "warning"),
    CONFIRMEE("Confirmée", "success"),
    EXPEDIEE("Expédiée", "info"),
    ANNULEE("Annulée", "danger");

    private final String libelle;
    private final String couleur;

    StatutCommande(String libelle, String couleur) { this.libelle = libelle; this.couleur = couleur; }
    public String getLibelle() { return libelle; }
    public String getCouleur() { return couleur; }
}
