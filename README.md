# FamiGuard — Module 1 (démo réelle)

Application Android **réelle** (pas une maquette) qui démontre le module 1 :

1. **Temps d'écran réel** par application, depuis minuit — via `UsageStatsManager`.
2. **Blocage réel** d'une application choisie par le parent — via un service
   d'accessibilité qui détecte l'application au premier plan et affiche un écran
   « application en pause ».

Fonctionne **sans remise à zéro** du téléphone (offre « Simple » de l'étude).
Installable directement (sideload). `minSdk 26` (Android 8.0) → `targetSdk 34`.

---

## 1. Compiler l'APK

### Option A — Android Studio (le plus simple)
1. Ouvrir Android Studio (Hedgehog ou plus récent).
2. **File → Open** → choisir ce dossier.
3. Android Studio télécharge le SDK et régénère le wrapper Gradle tout seul.
4. **Build → Build App Bundle(s)/APK(s) → Build APK(s)**.
5. L'APK est dans `app/build/outputs/apk/debug/app-debug.apk`.

### Option B — Ligne de commande
Pré-requis : JDK 17 et le SDK Android (variable `ANDROID_HOME` ou fichier
`local.properties` contenant `sdk.dir=/chemin/vers/Android/Sdk`).

```bash
# 1) générer le wrapper (une seule fois, nécessite internet)
gradle wrapper --gradle-version 8.11.1

# 2) compiler l'APK de debug
./gradlew assembleDebug

# APK produit :
#   app/build/outputs/apk/debug/app-debug.apk
```

> Note : cet APK de debug est signé automatiquement avec la clé de debug.
> Pour une distribution hors développement, générer un APK de release signé
> avec votre propre clé (`./gradlew assembleRelease` + configuration de signature).

---

## 2. Installer sur un téléphone

1. Copier `app-debug.apk` sur le téléphone (câble, Drive, etc.).
2. Autoriser l'installation d'« applications de sources inconnues » quand il le demande.
3. Ouvrir **FamiGuard**.

Ou par câble USB avec le débogage activé :
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 3. Première utilisation (2 autorisations à donner)

L'app affiche deux cartes de réglage au premier lancement :

1. **Accès aux données d'usage** → bouton « Ouvrir les réglages » → activer
   FamiGuard dans la liste. (Obligatoire pour lire le temps d'écran.)
2. **Blocage d'applications** → bouton « Ouvrir l'accessibilité » → activer le
   service FamiGuard. (Obligatoire pour bloquer une application.)

De retour dans l'app, la liste des applications et leur temps d'écran du jour
s'affiche. Appuyer sur **Bloquer** à côté d'une application : si l'enfant
l'ouvre, l'écran « application en pause » apparaît et renvoie à l'accueil.

---

## 4. Démo en réunion (conseillé)

- Installer sur un **téléphone de test** quelques jours avant, pour avoir de
  vrais chiffres de temps d'écran.
- En direct : ouvrir l'app → montrer les temps réels → **Bloquer** une appli
  (ex. un navigateur ou un jeu installé) → l'ouvrir → l'écran de blocage
  s'affiche vraiment. C'est le moment le plus convaincant.

---

## 5. Ce que ce module NE fait pas encore

- Pas d'application parent séparée : ici le parent règle sur le téléphone même.
  (En production : règles définies à distance depuis l'app parent, signées par
  le serveur — voir l'étude de faisabilité.)
- Pas de scores d'IA (module 2) ni de coach (module 3).
- Le blocage par accessibilité peut être contourné par un enfant averti
  (désactivation du service). L'offre « Complète » (Device Owner) rend le
  blocage inviolable mais demande la remise à zéro du téléphone.

---

## Structure

```
app/src/main/java/tn/famiguard/m1/
  MainActivity.kt                 écran principal (Compose) : liste + boutons
  UsageRepository.kt              lecture du temps d'écran (UsageStatsManager)
  BlockStore.kt                   liste des apps bloquées (local)
  BlockAccessibilityService.kt    détecte l'app au premier plan, applique le blocage
  BlockActivity.kt                écran « application en pause »
app/src/main/res/                 icône, textes, config du service
```
