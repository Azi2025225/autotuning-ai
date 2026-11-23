# AutoTuning AI

Plateforme intelligente de vente et de personnalisation automobile — Projet Java G2E.
Java 21 · Spring Boot 3 · Thymeleaf · Bootstrap 5 · Spring Data JPA · MySQL · Spring Security (BCrypt) · API IA (Gemini / OpenAI).

Les documents de référence sont dans `docs/` (cahier des charges, UML, maquette 8 écrans).

## Démarrage rapide

Prérequis : JDK 21, Maven 3.9+, MySQL 8 (ou le profil H2 sans installation).

### Option A — avec MySQL
```bash
# 1) (facultatif) créer la base à la main : mysql -u root -p < sql/schema.sql
#    sinon elle est créée automatiquement au premier lancement
export DB_USER=root
export DB_PASSWORD=votre_mot_de_passe
mvn spring-boot:run
```
Windows (PowerShell) : `$env:DB_USER="root"; $env:DB_PASSWORD="..."; mvn spring-boot:run`

### Option A bis — avec XAMPP
Voir `docs/GUIDE_XAMPP.md` (MySQL de XAMPP, root sans mot de passe).

### Option B — démonstration sans MySQL (base H2 en mémoire)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Application : http://localhost:8080

## Authentification et réinitialisation

La connexion locale utilise l'email comme identifiant, avec mots de passe BCrypt, verrouillage de 10 minutes après
5 échecs consécutifs et une option « Se souvenir de moi » valable 14 jours. Pour activer cette dernière, définir
`REMEMBER_ME_KEY` avec une clé secrète stable et propre à l'environnement (elle n'est jamais enregistrée dans le dépôt).
Google est facultatif : le bouton n'apparaît que lorsque les deux identifiants sont présents. Les comptes Google
nécessitent un email vérifié ; un compte existant de même adresse est associé sans modifier son rôle.

Variables d'environnement facultatives :

| Variable | Usage |
|---|---|
| `GOOGLE_CLIENT_ID` | Identifiant OAuth2 Google |
| `GOOGLE_CLIENT_SECRET` | Secret OAuth2 Google |
| `REMEMBER_ME_KEY` | Clé secrète stable des cookies « Se souvenir de moi » |
| `MAIL_HOST` | Hôte SMTP ; sans configuration SMTP, le lien de réinitialisation est journalisé en mode développement |
| `MAIL_PORT` | Port SMTP (défaut : `25`) |
| `MAIL_USER` | Utilisateur SMTP |
| `MAIL_PASSWORD` | Mot de passe SMTP |
| `APP_URL` | URL publique de l'application pour construire le lien (défaut : `http://localhost:8080`) |

Les demandes de réinitialisation ne révèlent pas si l'adresse email existe et sont limitées à 3 par heure et par adresse.
Les liens expirent au bout de 30 minutes et ne peuvent être utilisés qu'une fois.
`sql/schema.sql` comprend les colonnes de fournisseur Google, d'activité et de verrouillage ainsi que les tables
`password_reset_tokens` et `password_reset_requests`. Avec `ddl-auto=update`, Hibernate met également à jour la base existante.

### Comptes
| Rôle | Email | Mot de passe |
|---|---|---|
| Administrateur | `admin@autotuning.tn` | `Admin@12345` (modifiable via `ADMIN_EMAIL` / `ADMIN_PASSWORD`) |
| Client | à créer via « S'inscrire » | — |

Au premier démarrage, 6 catégories et 8 produits de démonstration sont insérés.

## Conseiller IA moteur
Par défaut `IA_PROVIDER=mock` : réponse **générique de démonstration**, sans aucun chiffre de gain inventé.
Pour une vraie analyse, fournir la clé **uniquement par variable d'environnement** (jamais dans le code) :
```bash
export IA_PROVIDER=gemini      # ou openai
export IA_API_KEY=votre_cle
export IA_MODEL=               # facultatif (défaut : gemini-2.0-flash / gpt-4o-mini)
```
Le service construit un prompt qui interdit d'inventer des gains, exige une réponse JSON en 3 stages, vérifie le format
reçu, enregistre la consultation et affiche toujours l'avertissement « informatif, ne remplace pas un professionnel ».
En cas de timeout, d'erreur ou de clé absente, un message clair s'affiche et le reste du site reste utilisable.

## Données moteur réelles, courbes et chatbot
- **Admin → Moteurs de référence** : base de données de moteurs (puissance/couple d'origine, régimes, valeurs Stage 1/2/3, source,
  statut « vérifié »). Le conseiller n'affiche des **chiffres et des courbes que si un moteur de référence correspond**
  (marque, modèle, carburant, année, puissance ±5 ch) ; sinon l'analyse reste qualitative et aucun gain n'est annoncé.
  Les gains annoncés par l'IA sont toujours ignorés.
- **Import CSV** (Admin → Moteurs de référence) : chargez plusieurs moteurs d'un coup. Chaque ligne est validée comme le formulaire et
  doit citer sa **source** ; « Télécharger le CSV » donne le modèle (colonnes `marque;modele;motorisation;carburant;annee_debut;annee_fin;puissance_origine;couple_origine;regime_couple;regime_puissance;regime_max;stage1_ch…stage3_nm;source;verifie;remarque`).
  Les régimes vides reçoivent une valeur par défaut signalée « estimée ».
- Au démarrage, `src/main/resources/data/moteurs_reference.csv` (24 moteurs, sources indiquées) est chargé : seuls les moteurs absents sont ajoutés,
  vos modifications ne sont jamais écrasées (`MOTEURS_SEED=false` pour désactiver). Ce sont des valeurs **commerciales annoncées par des préparateurs**
  (toutes « à confirmer », jamais mesurées) : confirmez-les ou remplacez-les par vos relevés, puis cochez « vérifié ».
- Les courbes sont **schématiques** (tracées depuis les valeurs de pointe), pas des relevés de banc d'essai.
- Quand un moteur de référence correspond, ses chiffres sont aussi donnés au conseiller IA (qui doit les recopier tels quels, avec leur statut).
- **Assistant** (`/chat`, connecté) : il lit la base **à chaque question** — produits, prix, stocks, nouveautés, catégories, moteurs de référence (chiffres Stage avec statut et source)
  et **les commandes du client connecté** (jamais celles des autres). Un produit ajouté ou un stock modifié est donc pris en compte immédiatement, sans mise à jour.
  Avec une clé IA, ces données sont injectées dans la requête ; sans clé (ou en cas de panne), des règles locales répondent à partir des mêmes données.
  Il n'invente aucun chiffre : moteur absent de la base = « pas de valeurs ». Limité à 15 messages / 5 minutes par utilisateur.

## Ollama (modèles locaux ou cloud)
Ollama peut remplacer Gemini/OpenAI pour le **conseiller moteur** et vérifier les **photos importées** (modèle vision).
Il ne sait pas *modifier* une image : l'essai de kit reste fait par Gemini.
```bash
# Local : installer Ollama puis télécharger les modèles
ollama pull llama3.1
ollama pull llama3.2-vision
export IA_PROVIDER=ollama
export OLLAMA_TEXT_MODEL=llama3.1
export OLLAMA_VISION_MODEL=llama3.2-vision     # facultatif : active la vérification « est-ce une voiture ? »
# Cloud : export OLLAMA_URL=https://ollama.com ; export OLLAMA_API_KEY=... ; OLLAMA_TEXT_MODEL=<modèle cloud>
```
Si Ollama est arrêté, le conseiller affiche un message clair et la vérification des photos est simplement ignorée.

## Catalogue photo, modèles de voitures et essai de kit
- **Admin → Produits** : envoi de la photo du produit depuis l'ordinateur (JPG/PNG, 5 Mo, contenu vérifié).
- **Admin → Modèles de voitures** : ajout des voitures (marque, modèle, photo). Page publique : `/modeles`.
- **Essai de kit** (`/ia/personnalisation`) : le client choisit une voiture du catalogue (ou importe sa photo) puis un kit
  du catalogue ; l'IA génère l'image avant/après. Depuis une fiche produit : « Essayer ce produit sur une voiture ».
- **Édition d'image par IA (Gemini)** : définir la clé dans l'environnement, jamais dans le code :
```bash
export IA_IMAGE_API_KEY=votre_cle_google_ai_studio
export IA_IMAGE_MODEL=gemini-2.5-flash-image   # facultatif
```
  Avec une clé, la **couleur** est elle aussi repeinte par l'IA. Si l'IA échoue (quota, clé refusée, modèle introuvable, panne), le motif exact est affiché
  (codes 401/403/404/429/5xx) et un rendu graphique simplifié remplace l'image pour ne pas laisser le client sans résultat.
  Sans clé, une simulation graphique simplifiée s'affiche (bandeau « mode démonstration »). Le résultat est toujours une
  simulation : il ne garantit ni la compatibilité des pièces ni un rendu identique après installation.
- Seules les photos du catalogue (`/media/**`) sont publiques ; les photos privées des clients ne sont accessibles qu'à leur propriétaire.

## Statistiques (Admin → Statistiques)
Chiffre d'affaires par jour (7/30/90 jours), commandes par statut, CA par catégorie, produits les plus vendus, panier moyen, stock faible.

## Architecture (MVC en couches — structure du cours Spring Boot)
```
src/main
├── java/com/autotuning
│   ├── controller   Home, Auth, Catalogue, Modèles, Panier(+commandes), IA, Chat, Avis, Admin
│   ├── model        entités JPA (Utilisateur, Produit, Commande, …)           (= diagramme de classes)
│   ├── repository   interfaces Spring Data JPA
│   ├── service      INTERFACES du métier (ProduitService, CommandeService, …)
│   │   └── impl     IMPLÉMENTATIONS (ProduitServiceImpl, CommandeServiceImpl, …)
│   ├── config       SecurityConfig, WebConfig, propriétés IA, DataInitializer
│   ├── dto          formulaires validés (Bean Validation) et ReponseIA
│   ├── exception    exceptions métier + GlobalExceptionHandler
│   └── AutoTuningApp.java        (classe principale @SpringBootApplication)
└── resources
    ├── templates
    │   ├── layout.html           (head, navbar, flash, footer partagés)
    │   ├── fragments/            (cards.html, sidebar.html)
    │   ├── produits/ panier/ commandes/ avis/ modeles/ chat/ auth/ ia/ admin/
    │   ├── index.html
    │   └── error.html
    ├── static/ (css, js, img)
    └── application.properties
```
Les contrôleurs dépendent uniquement des **interfaces** `service/*Service` ; Spring injecte les classes
`service/impl/*ServiceImpl` (Inversion de contrôle / injection par constructeur). Le fonctionnement est inchangé.

Routes principales : `/`, `/catalogue`, `/produit/{id}`, `/panier`, `/commandes`, `/profil`, `/ia/moteur`,
`/ia/personnalisation`, `/avis` (avis publics, publication réservée aux comptes connectés; un avis par compte),
`/admin/**` (rôle ADMIN). La table `avis_clients` est créée/mise à jour par Hibernate et incluse dans `sql/schema.sql`.

## Tests (plan de tests du cahier des charges)
`mvn test` couvre aussi l'import/export CSV des moteurs, le chatbot (données en direct, commandes du client uniquement, moteur inconnu sans chiffre) et les erreurs de l'édition d'image. Il couvre : email déjà utilisé, accès client refusé sur `/admin`, ajout au panier (quantité/total),
stock insuffisant, confirmation de commande (lignes + stock + panier vidé), annulation (remise en stock),
réponse IA structurée, API IA indisponible, fichier invalide refusé, protection CSRF, et les flux d'authentification et de réinitialisation du mot de passe.

## Limites connues
- Paiement simulé uniquement (hors périmètre).
- Les produits de démonstration utilisent des illustrations SVG ; l'admin peut renseigner d'autres URLs d'images.
- Le projet a été généré sans accès à Maven Central : lancez `mvn test` après le premier téléchargement des dépendances
  et signalez toute erreur de compilation éventuelle.
