#!/usr/bin/env python3
"""
make_release.py — Script de production Épione.

Télécharge les données réelles depuis data.gouv.fr (FINESS),
filtre les établissements sanitaires, génère la base SQLite,
met à jour manifest.json et prépare la release GitHub.

Usage :
    python scripts/make_release.py

Prérequis :
    pip install requests pyproj

Flux :
    1. Récupère l'URL du dernier fichier FINESS via l'API data.gouv.fr
    2. Télécharge et parse le CSV (filtrage sanitaire)
    3. Optionnel : fusionne les données qualité HAS (si disponibles)
    4. Génère app/src/main/assets/epione.db
    5. Calcule le SHA-256 de la DB
    6. Met à jour manifest.json avec la nouvelle version + URL de release
"""

import csv
import hashlib
import io
import json
import os
import sqlite3
import sys
import zipfile
from pathlib import Path

try:
    import requests
except ImportError:
    sys.exit("[ERREUR] 'requests' manquant. Lancez : pip install requests")

# Pyproj pour convertir coordonnées Lambert 93 → WGS84 (facultatif)
try:
    from pyproj import Transformer
    _transformer = Transformer.from_crs("EPSG:2154", "EPSG:4326", always_xy=True)
    HAS_PYPROJ = True
    print("[INFO] pyproj disponible — coordonnées Lambert 93 converties en WGS84.")
except ImportError:
    HAS_PYPROJ = False
    print("[WARNING] pyproj absent — latitude/longitude seront null. pip install pyproj")

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
SCRIPT_DIR   = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent
ASSETS_DIR   = PROJECT_ROOT / "app" / "src" / "main" / "assets"
MANIFEST_PATH = PROJECT_ROOT / "manifest.json"
DB_NAME      = "epione.db"

# API data.gouv.fr — dataset FINESS établissements
FINESS_DATASET_ID = "53699fe4a3a729239d206227"
FINESS_API_URL    = f"https://www.data.gouv.fr/api/1/datasets/{FINESS_DATASET_ID}/"

# Catégorie de regroupement sanitaire dans FINESS
SANITAIRE_CATEGRETAB = "1100"

# Catégories HAS à inclure (codes categetab) — sanitaire MCO, SSR, PSY, Urgences, HAD
SANITAIRE_CATEGETAB_CODES = {
    "101",  # CHR / CHU
    "106",  # CH (Centre Hospitalier)
    "109",  # Hôpital Local
    "114",  # Hôpital des Armées
    "122",  # Clinique polyvalente (OQN)
    "124",  # Clinique spécialisée (OQN)
    "128",  # Clinique MCO privée
    "129",  # Clinique SSR privée
    "131",  # Clinique PSY privée
    "132",  # Maison de santé mentale
    "148",  # HAD (Hospitalisation A Domicile)
    "160",  # Centre de soins de suite et de réadaptation public
    "162",  # Centre de soins psychiatriques public
}

# ---------------------------------------------------------------------------
# Étape 1 : Découverte du fichier FINESS via l'API data.gouv.fr
# ---------------------------------------------------------------------------

def find_finess_csv_url() -> str:
    """Retourne l'URL de téléchargement du dernier fichier établissements FINESS."""
    print("[1/5] Interrogation de l'API data.gouv.fr…")
    response = requests.get(FINESS_API_URL, timeout=30)
    response.raise_for_status()
    dataset = response.json()

    # Cherche la ressource "etalab-cs1100507" (établissements)
    for resource in dataset.get("resources", []):
        title = resource.get("title", "").lower()
        url   = resource.get("url", "")
        fmt   = resource.get("format", "").lower()
        if "cs1100507" in title and "stock" in title and fmt in ("csv", "zip", ""):
            print(f"[1/5] Ressource trouvée : {resource['title']}")
            return url

    # Fallback : prendre la première ressource de type CSV
    for resource in dataset.get("resources", []):
        if resource.get("format", "").lower() in ("csv", "zip"):
            print(f"[1/5] Fallback ressource : {resource['title']}")
            return resource["url"]

    raise RuntimeError(
        "Aucun fichier FINESS trouvé via l'API. "
        "Vérifiez https://www.data.gouv.fr/fr/datasets/finess-extraction-du-fichier-des-etablissements/"
    )


# ---------------------------------------------------------------------------
# Étape 2 : Téléchargement et parsing du CSV FINESS
# ---------------------------------------------------------------------------

def download_and_parse_finess(url: str) -> list[dict]:
    """Télécharge le fichier FINESS et retourne les établissements sanitaires."""
    print(f"[2/5] Téléchargement FINESS depuis {url} …")

    response = requests.get(url, stream=True, timeout=120)
    response.raise_for_status()

    content_type = response.headers.get("content-type", "")
    raw_bytes = b""
    total = 0
    for chunk in response.iter_content(chunk_size=65536):
        raw_bytes += chunk
        total += len(chunk)
        print(f"\r  {total / 1_048_576:.1f} Mo téléchargés…", end="", flush=True)
    print()

    # Décompression ZIP si nécessaire
    if url.endswith(".zip") or "zip" in content_type:
        print("[2/5] Décompression ZIP…")
        with zipfile.ZipFile(io.BytesIO(raw_bytes)) as zf:
            csv_name = next((n for n in zf.namelist() if n.endswith(".csv")), None)
            if not csv_name:
                raise RuntimeError("Aucun CSV trouvé dans l'archive ZIP.")
            raw_bytes = zf.read(csv_name)

    # Décodage (UTF-8 avec BOM possible, sinon Latin-1)
    try:
        text = raw_bytes.decode("utf-8-sig")
    except UnicodeDecodeError:
        text = raw_bytes.decode("latin-1")

    return _parse_csv(text)


def _parse_csv(text: str) -> list[dict]:
    """Parse le CSV FINESS et filtre les établissements sanitaires."""
    reader = csv.DictReader(io.StringIO(text), delimiter=";")
    rows   = list(reader)

    if not rows:
        raise RuntimeError("CSV FINESS vide ou mal formé.")

    # Affiche les colonnes disponibles pour le débogage
    colonnes = list(rows[0].keys())
    print(f"[2/5] {len(rows)} lignes lues. Colonnes : {colonnes[:8]}…")

    etablissements = []
    skipped = 0

    for row in rows:
        # Normalisation des clés (espaces, casse variable selon les versions)
        row = {k.strip().lower(): v.strip() for k, v in row.items()}

        # Filtre : sanitaire uniquement
        categretab = row.get("categretab", "")
        if categretab != SANITAIRE_CATEGRETAB:
            skipped += 1
            continue

        # Établissements fermés exclus
        if row.get("dateferme", ""):
            skipped += 1
            continue

        etab = _extract_etablissement(row)
        if etab:
            etablissements.append(etab)

    print(f"[2/5] {len(etablissements)} établissements sanitaires retenus "
          f"({skipped} exclus).")
    return etablissements


def _extract_etablissement(row: dict) -> dict | None:
    """Extrait et nettoie les champs d'une ligne FINESS."""
    finess_et = row.get("nofinesset", "").strip()
    nom = (row.get("rslongue") or row.get("rs", "")).strip()

    if not finess_et or not nom:
        return None

    # Adresse : numvoie + typvoie + voie
    num   = row.get("numvoie", "")
    typ   = row.get("typvoie", "")
    voie  = row.get("voie", "")
    comp  = row.get("compvoie", "") or row.get("lieuditbp", "")
    adresse = " ".join(filter(None, [num, typ, voie, comp])).strip()

    # CP + Ville depuis ligneacheminement (ex. "75010 PARIS")
    ligne = row.get("ligneacheminement", "")
    if ligne and len(ligne) >= 5:
        code_postal = ligne[:5]
        ville = ligne[5:].strip().title()
    else:
        code_postal = row.get("cp", "")
        ville = row.get("commune", "").title()

    # Type d'établissement
    type_etab = row.get("libcategetab") or row.get("libcategretab", "Établissement sanitaire")
    type_etab = type_etab.strip()

    # Coordonnées
    lat, lon = _get_coordinates(row)

    return {
        "finess_et":   finess_et,
        "nom":         _clean_name(nom),
        "type":        type_etab,
        "adresse":     adresse or "Adresse non renseignée",
        "code_postal": code_postal,
        "ville":       ville or "Ville inconnue",
        "telephone":   _clean_phone(row.get("telephone", "")),
        "site_web":    None,
        "latitude":    lat,
        "longitude":   lon,
    }


def _get_coordinates(row: dict) -> tuple[float | None, float | None]:
    """Extrait et convertit les coordonnées (Lambert 93 → WGS84 si pyproj disponible)."""
    x_str = row.get("coordxet", "")
    y_str = row.get("coordyet", "")

    if not x_str or not y_str:
        return None, None

    try:
        x, y = float(x_str.replace(",", ".")), float(y_str.replace(",", "."))
        if HAS_PYPROJ:
            lon, lat = _transformer.transform(x, y)
            # Santé-check : coordonnées dans les limites de la France métropolitaine + DOM
            if -180 <= lon <= 180 and -90 <= lat <= 90:
                return round(lat, 6), round(lon, 6)
        # Sans pyproj : coordonnées Lambert 93 brutes (inutilisables pour affichage)
        return None, None
    except (ValueError, Exception):
        return None, None


def _clean_name(name: str) -> str:
    """Normalise le nom (supprime les guillemets, double espaces)."""
    return " ".join(name.replace('"', "").split())


def _clean_phone(phone: str) -> str | None:
    """Formate le numéro de téléphone français."""
    digits = "".join(c for c in phone if c.isdigit())
    if len(digits) == 10:
        return f"{digits[0:2]}.{digits[2:4]}.{digits[4:6]}.{digits[6:8]}.{digits[8:10]}"
    return phone.strip() if phone.strip() else None


# ---------------------------------------------------------------------------
# Étape 3 (facultatif) : Données qualité HAS
# ---------------------------------------------------------------------------

def fetch_has_quality_data() -> dict[str, dict]:
    """
    Tente de récupérer les indicateurs qualité HAS depuis data.gouv.fr.
    Retourne un dict { finess_et → { score_global, score_patient, ... } }.

    Source : "Résultats des indicateurs IQSS en établissements de santé"
    URL API : https://www.data.gouv.fr/api/1/datasets/ (recherche par mot clé)
    """
    print("[3/5] Tentative de récupération données HAS…")
    # NOTE : L'URL exacte du dataset HAS change selon les années et mises à jour.
    # Implémentez ici selon le dataset disponible au moment de la release.
    # Exemple de structure attendue dans le CSV HAS :
    #   FINESSET;SCORE_GLOBAL;SCORE_PATIENT;SCORE_SECURITE;ANNEE
    # En attendant, retourne un dict vide (tous les établissements = "Non évalué").
    print("[3/5] Données HAS : intégration non implémentée "
          "— tous les scores seront null (Non évalué).")
    return {}


# ---------------------------------------------------------------------------
# Étape 4 : Génération de la base SQLite
# ---------------------------------------------------------------------------

def generate_database(etablissements: list[dict], qualite_data: dict[str, dict]) -> Path:
    """Génère la base SQLite dans assets/epione.db. Retourne le chemin du fichier."""
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    db_path = ASSETS_DIR / DB_NAME

    if db_path.exists():
        db_path.unlink()
        print(f"[4/5] Ancienne DB supprimée.")

    conn = sqlite3.connect(db_path)
    cur  = conn.cursor()

    cur.execute("""
        CREATE TABLE etablissements (
            finess_et   TEXT    PRIMARY KEY NOT NULL,
            nom         TEXT    NOT NULL,
            type        TEXT    NOT NULL,
            adresse     TEXT    NOT NULL,
            code_postal TEXT    NOT NULL,
            ville       TEXT    NOT NULL,
            telephone   TEXT,
            site_web    TEXT,
            latitude    REAL,
            longitude   REAL
        )
    """)

    cur.execute("""
        CREATE TABLE qualite (
            finess_et           TEXT    PRIMARY KEY NOT NULL,
            score_global        REAL,
            score_patient       REAL,
            score_securite      REAL,
            annee_evaluation    INTEGER,
            FOREIGN KEY (finess_et) REFERENCES etablissements(finess_et) ON DELETE CASCADE
        )
    """)

    # Insertion des établissements
    cur.executemany("""
        INSERT OR IGNORE INTO etablissements
            (finess_et, nom, type, adresse, code_postal, ville, telephone, site_web, latitude, longitude)
        VALUES (:finess_et, :nom, :type, :adresse, :code_postal, :ville, :telephone, :site_web, :latitude, :longitude)
    """, etablissements)

    # Insertion des données qualité
    qualite_rows = [
        (
            finess_et,
            q.get("score_global"),
            q.get("score_patient"),
            q.get("score_securite"),
            q.get("annee_evaluation"),
        )
        for finess_et, q in qualite_data.items()
        if any(v is not None for v in [q.get("score_global"), q.get("score_patient")])
    ]
    if qualite_rows:
        cur.executemany("""
            INSERT OR IGNORE INTO qualite
                (finess_et, score_global, score_patient, score_securite, annee_evaluation)
            VALUES (?, ?, ?, ?, ?)
        """, qualite_rows)

    conn.commit()

    # Statistiques
    nb_etab   = cur.execute("SELECT COUNT(*) FROM etablissements").fetchone()[0]
    nb_qualite = cur.execute("SELECT COUNT(*) FROM qualite").fetchone()[0]
    conn.close()

    size_mb = db_path.stat().st_size / 1_048_576
    print(f"[4/5] DB générée : {nb_etab} établissements, {nb_qualite} évaluations "
          f"({size_mb:.1f} Mo).")
    return db_path


# ---------------------------------------------------------------------------
# Étape 5 : Mise à jour du manifest.json
# ---------------------------------------------------------------------------

def update_manifest(db_path: Path, record_count: int) -> None:
    """Met à jour manifest.json avec la nouvelle version et le SHA-256 de la DB."""
    print("[5/5] Mise à jour de manifest.json…")

    # Calcul SHA-256
    sha256 = hashlib.sha256(db_path.read_bytes()).hexdigest()

    # Lecture de la version actuelle
    manifest = {}
    if MANIFEST_PATH.exists():
        manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))

    old_version = manifest.get("version", 1)
    new_version = old_version + 1

    manifest.update({
        "version":      new_version,
        "db_sha256":    sha256,
        "record_count": record_count,
    })

    # L'URL de téléchargement sera générée manuellement après la release GitHub :
    # https://github.com/kapoue/Epione/releases/download/vX.Y/epione.db
    if not manifest.get("db_url"):
        manifest["db_url"] = (
            f"https://github.com/kapoue/Epione/releases/download/"
            f"v{new_version}.0/{DB_NAME}"
        )
        print(f"  [NOTE] db_url auto-générée. Mettez à jour après avoir créé la Release GitHub.")

    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"[5/5] manifest.json → version {new_version}, sha256 = {sha256[:16]}…")
    print()
    print("=" * 60)
    print("RELEASE PRÊTE — Étapes suivantes :")
    print(f"  1. git add app/src/main/assets/epione.db manifest.json")
    print(f"  2. git commit -m 'data: Release v{new_version} ({record_count} établissements)'")
    print(f"  3. git push origin main")
    print(f"  4. Créer une GitHub Release 'v{new_version}.0' et uploader epione.db")
    print(f"  5. Mettre à jour 'db_url' dans manifest.json si l'URL a changé")
    print("=" * 60)


# ---------------------------------------------------------------------------
# Point d'entrée
# ---------------------------------------------------------------------------

def main():
    try:
        # 1. Découverte URL
        finess_url = find_finess_csv_url()

        # 2. Téléchargement + parsing
        etablissements = download_and_parse_finess(finess_url)

        if not etablissements:
            sys.exit("[ERREUR] Aucun établissement sanitaire trouvé dans le fichier FINESS.")

        # 3. Données qualité HAS (optionnel)
        qualite_data = fetch_has_quality_data()

        # 4. Génération DB
        db_path = generate_database(etablissements, qualite_data)

        # 5. Mise à jour manifest
        update_manifest(db_path, record_count=len(etablissements))

    except requests.exceptions.ConnectionError:
        sys.exit("[ERREUR] Impossible de se connecter à internet.")
    except requests.exceptions.HTTPError as e:
        sys.exit(f"[ERREUR] HTTP {e.response.status_code} : {e}")
    except Exception as e:
        sys.exit(f"[ERREUR] {e}")


if __name__ == "__main__":
    main()
