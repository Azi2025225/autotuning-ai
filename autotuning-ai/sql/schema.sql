-- ============================================================================
-- AutoTuning TN — Schéma MySQL 8 (équivalent de ce que génère Hibernate)
-- Facultatif : l'application crée/met à jour les tables seule (ddl-auto=update).
-- Utilisation :  mysql -u root -p < sql/schema.sql
-- ============================================================================
CREATE DATABASE IF NOT EXISTS autotuning_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE autotuning_ai;

CREATE TABLE IF NOT EXISTS utilisateurs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom          VARCHAR(80)  NOT NULL,
    prenom       VARCHAR(80)  NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,            -- haché BCrypt
    telephone    VARCHAR(30),
    role         VARCHAR(20)  NOT NULL,            -- CLIENT | ADMIN
    fournisseur  VARCHAR(20)  NOT NULL DEFAULT 'LOCAL', -- LOCAL | GOOGLE
    google_id    VARCHAR(255) UNIQUE,
    actif        BOOLEAN      NOT NULL DEFAULT TRUE,
    echecs_connexion INT      NOT NULL DEFAULT 0,
    verrouille_jusqua DATETIME(6)
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id BIGINT       NOT NULL,
    token_hash     CHAR(64)      NOT NULL UNIQUE,
    expiration     DATETIME(6)   NOT NULL,
    utilise        BOOLEAN      NOT NULL DEFAULT FALSE,
    date_creation  DATETIME(6)  NOT NULL,
    CONSTRAINT fk_reset_token_user FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
);

CREATE TABLE IF NOT EXISTS password_reset_requests (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    email_hash   CHAR(64)    NOT NULL,
    date_demande DATETIME(6) NOT NULL,
    INDEX idx_reset_request_email_date (email_hash, date_demande)
);

CREATE TABLE IF NOT EXISTS avis_clients (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id BIGINT       NOT NULL UNIQUE,
    note           INT          NOT NULL,
    commentaire    VARCHAR(1000) NOT NULL,
    date_creation  DATETIME(6)  NOT NULL,
    CONSTRAINT fk_avis_client_user FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id),
    CONSTRAINT chk_avis_client_note CHECK (note BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS produits (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom           VARCHAR(150)   NOT NULL,
    description   VARCHAR(1500),
    prix          DECIMAL(10,2)  NOT NULL,
    stock         INT            NOT NULL,
    image_url     VARCHAR(255),
    compatibilite VARCHAR(300),
    marque        VARCHAR(255),
    categorie_id  BIGINT         NOT NULL,
    CONSTRAINT fk_produit_categorie FOREIGN KEY (categorie_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS lignes_panier (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL,
    produit_id     BIGINT NOT NULL,
    quantite       INT    NOT NULL,
    UNIQUE KEY uk_panier (utilisateur_id, produit_id),
    CONSTRAINT fk_panier_user    FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id),
    CONSTRAINT fk_panier_produit FOREIGN KEY (produit_id)     REFERENCES produits(id)
);

CREATE TABLE IF NOT EXISTS commandes (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id BIGINT        NOT NULL,
    date_commande  DATETIME(6)   NOT NULL,
    total          DECIMAL(12,2) NOT NULL,
    statut         VARCHAR(20)   NOT NULL,         -- EN_ATTENTE | CONFIRMEE | EXPEDIEE | ANNULEE
    CONSTRAINT fk_commande_user FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
);

CREATE TABLE IF NOT EXISTS lignes_commande (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    commande_id   BIGINT        NOT NULL,
    produit_id    BIGINT        NOT NULL,
    quantite      INT           NOT NULL,
    prix_unitaire DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_lc_commande FOREIGN KEY (commande_id) REFERENCES commandes(id),
    CONSTRAINT fk_lc_produit  FOREIGN KEY (produit_id)  REFERENCES produits(id)
);

CREATE TABLE IF NOT EXISTS vehicules (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id BIGINT      NOT NULL,
    marque         VARCHAR(60) NOT NULL,
    modele         VARCHAR(80) NOT NULL,
    annee          INT         NOT NULL,
    motorisation   VARCHAR(80) NOT NULL,
    carburant      VARCHAR(30) NOT NULL,
    puissance_ch   INT         NOT NULL,
    CONSTRAINT fk_vehicule_user FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
);

CREATE TABLE IF NOT EXISTS consultations_ia (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id    BIGINT      NOT NULL,
    vehicule_id       BIGINT      NOT NULL,
    demande           TEXT        NOT NULL,
    reponse           TEXT        NOT NULL,
    date_consultation DATETIME(6) NOT NULL,
    CONSTRAINT fk_ci_user     FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id),
    CONSTRAINT fk_ci_vehicule FOREIGN KEY (vehicule_id)    REFERENCES vehicules(id)
);

CREATE TABLE IF NOT EXISTS moteurs_reference (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    marque            VARCHAR(60)  NOT NULL,
    modele            VARCHAR(100) NOT NULL,
    motorisation      VARCHAR(120) NOT NULL,
    carburant         VARCHAR(30)  NOT NULL,
    annee_debut       INT NOT NULL,
    annee_fin         INT NOT NULL,
    puissance_origine INT NOT NULL,
    couple_origine    INT NOT NULL,
    regime_couple     INT NOT NULL,
    regime_puissance  INT NOT NULL,
    regime_max        INT NOT NULL,
    stage1_ch INT NULL, stage1_nm INT NULL,
    stage2_ch INT NULL, stage2_nm INT NULL,
    stage3_ch INT NULL, stage3_nm INT NULL,
    source            VARCHAR(500),
    verifie           BIT NOT NULL,                 -- 1 = valeurs vérifiées, 0 = à confirmer
    remarque          VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS modeles_voiture (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    marque    VARCHAR(60)  NOT NULL,
    nom       VARCHAR(100) NOT NULL,
    image_url VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS personnalisations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id  BIGINT       NOT NULL,
    image_originale VARCHAR(255) NOT NULL,
    image_resultat  VARCHAR(255) NOT NULL,
    modification    VARCHAR(60)  NOT NULL,
    date_creation   DATETIME(6)  NOT NULL,
    libelle         VARCHAR(255),
    produit_id      BIGINT,                         -- produit appliqué (sans clé étrangère)
    ia              BIT          NOT NULL,          -- 1 = image générée par l'IA, 0 = simulation graphique
    avertissement   VARCHAR(300),
    CONSTRAINT fk_perso_user FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
);
-- Le compte administrateur et les produits de démonstration sont créés au premier démarrage (DataInitializer).
