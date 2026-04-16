# Guide de mise à jour de la BDD Épione

> **Cas d'usage :** Les données FINESS sont publiées tous les mois sur data.gouv.fr.
> Ce guide permet de régénérer la base avec des données fraîches et de la déployer.
> Prévoir **~45 minutes** (géocodage réseau) + 10 min de build/release.

---

## Pré-requis (à vérifier une seule fois)

- **uv** installé (`uv --version` doit répondre)
- **adb** accessible pour tester sur le téléphone
- Connecté au **dépôt GitHub** (`git remote -v` doit afficher `kapoue/Epione`)
- Avoir les droits pour créer une **GitHub Release** sur https://github.com/kapoue/Epione

---

## Étape 1 — Régénérer la base de données

```powershell
cd C:\Users\kapou\www\Epione
New-Item -ItemType Directory -Force -Path logs | Out-Null
uv run --with requests --with pyproj python scripts/make_release.py *> logs\make_release.log ; Write-Host "DONE exit=$LASTEXITCODE"
```

Le script fait tout automatiquement :
1. Télécharge le fichier FINESS le plus récent depuis data.gouv.fr (~45 Mo)
2. Filtre les établissements sanitaires (hôpitaux, pharmacies, labos, centres de santé, MMG, PMI…)
3. Géocode les adresses via api-adresse.data.gouv.fr (~45 min, plusieurs lots de 5 000)
4. Génère `app/src/main/assets/epione.db`
5. **Incrémente automatiquement `manifest.json`** (version + sha256 + record_count)

Pour suivre l'avancement en direct :

```powershell
Get-Content logs\make_release.log -Tail 10
```

Le script est terminé quand tu vois :

```
[5/5] manifest.json -> version XX ...
RELEASE PRÊTE — Étapes suivantes :
```

> **Si le script échoue :** consulte `logs\make_release.log` pour voir l'erreur.
> Les erreurs réseau (IncompleteRead) sont normales et font l'objet d'un retry automatique.

---

## Étape 2 — Vérifier manifest.json

Ouvre `manifest.json` à la racine du projet. Il doit ressembler à ceci :

```json
{
  "version": 11,
  "db_url": "https://github.com/kapoue/Epione/releases/download/v8.0/epione.db",
  "db_sha256": "abc123...",
  "record_count": 45200
}
```

⚠️ **La version a été incrémentée automatiquement** (ex : 10 → 11).
⚠️ **`db_url` pointe encore sur l'ancienne release** — tu la mettras à jour à l'étape 4.

---

## Étape 3 — Bumper la version de l'app

> Fais ça seulement si tu changes du code en même temps.
> Si tu ne mets à jour que la BDD (pas de code), tu peux sauter cette étape.

Dans `app/build.gradle.kts` :

```kotlin
versionCode = 3        // +1 par rapport à la valeur actuelle
versionName = "1.2.0"
```

Dans `app/src/main/res/values/strings.xml` :

```xml
<string name="app_version">1.2.0</string>
```

---

## Étape 4 — Commit de la nouvelle DB

```powershell
cd C:\Users\kapou\www\Epione
git add app/src/main/assets/epione.db manifest.json
git commit -m "data: DB FINESS juillet 2026 — 45200 établissements"
git push
```

> Adapte le message avec le mois courant et le nombre d'établissements affiché dans le log.

---

## Étape 5 — Créer la GitHub Release et uploader la DB

1. Va sur : https://github.com/kapoue/Epione/releases/new
2. **Tag** : `v11.0` (le numéro de version du manifest, ex: si version=11 → `v11.0`)
3. **Titre** : `DB FINESS juillet 2026`
4. Glisse-dépose `app/src/main/assets/epione.db` dans la zone "Attach files"
5. Clique **Publish release**

---

## Étape 6 — Mettre à jour db_url dans manifest.json

Maintenant que la release existe, mets à jour l'URL de téléchargement dans `manifest.json` :

```json
"db_url": "https://github.com/kapoue/Epione/releases/download/v11.0/epione.db"
```

Puis commit :

```powershell
git add manifest.json
git commit -m "data: mise à jour db_url release v11.0"
git push
```

---

## Étape 7 — Tester sur le téléphone

```powershell
# Connecter le téléphone en WiFi (si pas encore fait)
adb connect 192.168.1.42:42985

# Builder et installer l'APK debug
.\gradlew assembleDebug
adb -s 192.168.1.42:42985 install -r app\build\outputs\apk\debug\app-debug.apk
```

Lance l'app → les nouvelles données doivent s'afficher.

Pour tester la mise à jour OTA (sur un appareil qui avait l'ancienne version) :
- L'app vérifie `manifest.json` au démarrage via WorkManager
- Si `version` dans le manifest > version locale → télécharge automatiquement la nouvelle DB
- Une snackbar "Mise à jour téléchargée — Redémarrer" s'affiche

---

## Récapitulatif rapide

| Étape | Commande / Action |
|---|---|
| 1. Générer DB | `uv run --with requests --with pyproj python scripts/make_release.py *> logs\make_release.log` |
| 2. Vérifier | `manifest.json` : version incrémentée, sha256 mis à jour |
| 3. Commit DB | `git add app/src/main/assets/epione.db manifest.json` puis commit + push |
| 4. GitHub Release | Créer tag `vX.0`, uploader `epione.db` |
| 5. Mettre à jour URL | Éditer `db_url` dans `manifest.json`, commit + push |
| 6. Tester | `.\gradlew assembleDebug` + install ADB |

---

## Versions actuelles (référence)

| Élément | Valeur |
|---|---|
| Version BDD (manifest) | 10 |
| Version app | 1.1.0 (versionCode 2) |
| Dernière release GitHub | v8.0 |
| ADB WiFi téléphone | 192.168.1.42:42985 |
| GitHub | https://github.com/kapoue/Epione |
