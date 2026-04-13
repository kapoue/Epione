# Épione — Lot 1 : Fondations & Données Locales

Application Android native (Kotlin + Jetpack Compose) recensant les établissements sanitaires français.

---

## Prérequis

| Outil | Version minimum |
|---|---|
| JDK | 17 |
| Android SDK | API 35 (Build-Tools 35) |
| Python | 3.8+ |
| ADB | inclus dans Android SDK |

> Aucun Android Studio requis. Tout se fait en ligne de commande (VSCode / terminal).

---

## 1. Générer la base de données de test

```bash
# Depuis la racine du projet
python scripts/init_db.py
```

Crée `app/src/main/assets/epione.db` avec 25 établissements fictifs.

---

## 2. Compiler l'APK de debug

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows PowerShell
.\gradlew.bat assembleDebug
```

APK produit : `app/build/outputs/apk/debug/app-debug.apk`

---

## 3. Installer sur un appareil / émulateur

```bash
# Vérifier les appareils connectés
adb devices

# Installer l'APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Lancer directement
adb shell am start -n com.epione.app/.MainActivity
```

---

## 4. Consulter les logs en temps réel

```bash
adb logcat -s "Epione" --pid=$(adb shell pidof com.epione.app)
```

---

## Structure du projet

```
Epione/
├── scripts/init_db.py          ← Génère la DB de test
├── app/src/main/
│   ├── assets/epione.db        ← DB SQLite pré-remplie (généré)
│   ├── kotlin/com/epione/app/
│   │   ├── data/               ← Room (entités, DAO, repository)
│   │   ├── di/                 ← Hilt (injection de dépendances)
│   │   └── ui/                 ← Compose (écrans, navigation, thème)
│   └── res/                    ← Ressources (strings, colors, drawables)
└── README.md
```

---

## Images & ressources graphiques

| Type | Dossier | Format recommandé |
|---|---|---|
| Logo vectoriel (splash, in-app) | `res/drawable/` | `.xml` (Vector Drawable) |
| Icône de lanceur | `res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/` | `.png` ou `.webp` |
| Icône adaptative (API 26+) | `res/mipmap-anydpi-v26/` | `.xml` (adaptive-icon) |

Pour remplacer le logo placeholder :
1. Exportez votre SVG depuis Figma/Inkscape.
2. Convertissez-le en Vector Drawable via [svg2android](https://svg2android.netlify.app/) ou l'outil `avdtool`.
3. Remplacez `res/drawable/ic_epione_logo.xml`.

---

## Lot 1 — Périmètre fonctionnel

- [x] Splash Screen natif (logo Épione)
- [x] Liste des établissements (données locales SQLite)
- [x] Recherche par nom / ville / CP
- [x] Fiche détail (adresse, type, score qualité)
- [x] Thème Material 3 Marbre/Or (clair + sombre)
- [x] Architecture MVVM + Repository + Hilt

## Lots suivants (non codés)

- **Lot 2** : Données réelles data.gouv.fr + mise à jour via GitHub
- **Lot 3** : Géolocalisation native + tri par distance
- **Lot 4** : Finalisation, badges qualité HAS, intents externes
