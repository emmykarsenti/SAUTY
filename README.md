# SAUTY - La Corde à Sauter Connectée

**SAUTY** est une application mobile Android couplée à un microcontrôleur STM32, conçue pour transformer une corde à sauter classique en un équipement sportif intelligent. L'application suit vos performances en temps réel, enregistre votre historique et analyse vos tendances sportives.

Projet réalisé par **Emmy Karsenti** et **Nicolas Betoin**.

---

## Fonctionnalités Principales

* **Connexion Bluetooth (BLE) :** Appairage rapide et automatique (ou manuel) avec le module STM32 de la corde à sauter.
* **Suivi en Temps Réel :** Affichage en direct du chronomètre, du nombre de sauts effectués et des calories brûlées.
* **Tableau de Bord & Tendances :** Visualisation de la progression quotidienne/hebdomadaire par rapport aux objectifs fixés.
* **Historique des Sessions :** Sauvegarde détaillée des entraînements passés dans le cloud.
* **Authentification :** Création de compte et connexion sécurisées pour des données personnalisées.

---

## Architecture Technique

### Application Android
* **Langage :** Kotlin
* **Interface Graphique :** Jetpack Compose (Material Design 3)
* **Architecture :** MVVM (Model-View-ViewModel)
* **Communication :** Bluetooth Low Energy (BLE) via `BleManager` personnalisé.
* **Navigation :** Jetpack Navigation Compose.

### Backend (Firebase)
* **Base de données :** Firebase Realtime Database
    * *URL du projet :* [https://sauty-ekarsenti-nbetoin-default-rtdb.europe-west1.firebasedatabase.app/](https://sauty-ekarsenti-nbetoin-default-rtdb.europe-west1.firebasedatabase.app/)
* **Authentification :** Firebase Authentication (Email/Mot de passe). *La configuration client est intégrée nativement dans le code source de l'application via le fichier `google-services.json`.*

### Hardware (Microcontrôleur)
* **Carte :** STM32 (avec module Bluetooth)
* **Rôle :** Détection des sauts (accéléromètre/capteurs) et transmission des données au smartphone via des caractéristiques BLE.
* **Code source STM32 :** Le fichier binaire/code lié au fonctionnement du STM32 est disponible dans la section **[Releases](../../releases)** de ce dépôt GitHub.

---

## Installation & Utilisation (Pour les correcteurs)

### Prérequis
1.  Un smartphone Android (Android 8.0+ minimum recommandé).
2.  Le Bluetooth et la localisation activés sur le téléphone (requis par Android pour le scan BLE).
3.  Le module STM32 SAUTY allumé et prêt à l'appairage.

### Étapes pour tester
1.  **Cloner le dépôt :**
    ```bash
    git clone [https://github.com/VOTRE_NOM_D_UTILISATEUR/Sauty.git](https://github.com/VOTRE_NOM_D_UTILISATEUR/Sauty.git)
    ```
2.  **Ouvrir avec Android Studio :** Laissez Gradle synchroniser les dépendances.
3.  **Lancer l'application :** Compilez et installez l'application sur un appareil physique (les émulateurs ne gèrent pas bien le Bluetooth).
4.  **Inscription/Connexion :** Créez un compte test sur l'écran d'accueil.
5.  **Connexion BLE :** Allez dans l'onglet "Scanner", trouvez l'appareil "SAUTY", et connectez-vous.
6.  **Entraînement :** Allez dans l'onglet "Exercice", lancez le timer et commencez à sauter !

---

## Structure du Projet (Android)
* `ui/` : Composants graphiques Jetpack Compose (Dashboard, Login, Workout, etc.).
* `viewmodel/` : Logique métier et gestion d'état (`SautyViewModel`).
* `ble/` : Gestionnaire de connexion Bluetooth et réception des trames (`BleManager`).
* `data/` : Classes de données (ex: `WorkoutSession`).
