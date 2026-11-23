# Guide XAMPP (MySQL/MariaDB) – AutoTuning AI

## 1. Démarrer la base
1. Installe XAMPP et ouvre le **XAMPP Control Panel**.
2. Clique sur **Start** à côté de **MySQL** (Apache n'est utile que pour ouvrir phpMyAdmin).
3. Ouvre http://localhost/phpmyadmin.

## 2. Créer la base (recommandé : encodage UTF-8)
Dans phpMyAdmin → onglet **SQL**, colle et exécute :
```sql
CREATE DATABASE IF NOT EXISTS autotuning_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
(ou importe le fichier `sql/schema.sql`, qui crée aussi toutes les tables). L'application peut aussi créer les tables
toute seule au premier lancement (`spring.jpa.hibernate.ddl-auto=update`), mais créer la base en utf8mb4 évite les
problèmes d'accents.

## 3. Lancer l'application avec XAMPP
XAMPP utilise par défaut l'utilisateur `root` **sans mot de passe** : c'est la configuration par défaut du projet.
```powershell
$env:Path += ";C:\apache-maven-3.9.16\bin"
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```
Si tu as mis un mot de passe à root : `$env:DB_PASSWORD="ton_mot_de_passe"` avant la commande.
Si MySQL écoute sur un autre port (ex. 3307) : `$env:DB_URL="jdbc:mysql://localhost:3307/autotuning_ai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"`.

## 4. Vérifier
Dans phpMyAdmin → base `autotuning_ai`, tu dois voir : utilisateurs, categories, produits, commandes, lignes_commande,
lignes_panier, vehicules, consultations_ia, personnalisations, modeles_voiture, moteurs_reference.
Le compte admin (`admin@autotuning.tn` / `Admin@12345`), 6 catégories, 8 produits et 2 moteurs d'exemple sont créés au premier démarrage.
Le fichier `sql/schema.sql` contient aussi les champs d'authentification (`fournisseur`, `google_id`, `actif`, verrouillage)
et les tables de réinitialisation du mot de passe.

## 5. Configuration optionnelle de l'authentification

Les secrets ne doivent jamais être ajoutés aux fichiers du projet. Définis-les dans la session PowerShell avant le lancement :
```powershell
$env:GOOGLE_CLIENT_ID="identifiant-oauth"
$env:GOOGLE_CLIENT_SECRET="secret-oauth"
$env:REMEMBER_ME_KEY="cle-secrete-longue-et-stable"
$env:MAIL_HOST="smtp.example.com"
$env:MAIL_PORT="587"
$env:MAIL_USER="compte-smtp"
$env:MAIL_PASSWORD="mot-de-passe-smtp"
```
Google reste désactivé si `GOOGLE_CLIENT_ID` ou `GOOGLE_CLIENT_SECRET` manque. Sans SMTP configuré, une demande de
réinitialisation conserve une réponse générique et le lien de développement apparaît dans la console. La durée du lien
est de 30 minutes ; la limite est de 3 demandes par heure et par adresse.

## Problèmes fréquents
| Message | Cause / solution |
|---|---|
| `Communications link failure` / `Connection refused` | MySQL n'est pas démarré dans XAMPP (bouton Start). |
| `Access denied for user 'root'` | Mot de passe root défini : `$env:DB_PASSWORD=...` |
| Accents illisibles (Ã©) | Base créée en latin1 : supprime-la et recrée-la avec l'étape 2. |
| Port 3306 déjà utilisé | Un autre MySQL tourne ; arrête-le ou change le port dans XAMPP et dans `DB_URL`. |
