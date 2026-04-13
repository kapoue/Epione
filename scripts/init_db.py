#!/usr/bin/env python3
"""
init_db.py — Génère la base de données SQLite de test pour Épione (Lot 1).

Usage :
    python scripts/init_db.py

Résultat :
    Crée app/src/main/assets/epione.db avec ~25 établissements factices.

Prérequis :
    Python 3.8+ (aucune dépendance externe requise — sqlite3 est natif).
"""

import sqlite3
import os
import shutil

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
ASSETS_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets")
DB_PATH = os.path.join(ASSETS_DIR, "epione.db")

# ---------------------------------------------------------------------------
# Données factices (25 établissements fictifs inspirés de villes réelles)
# ---------------------------------------------------------------------------
ETABLISSEMENTS = [
    # (finess_et, nom, type, adresse, code_postal, ville, telephone, site_web, lat, lon)
    ("750000001", "Hôpital Lariboisière (test)",       "Hôpital public",  "2 Rue Ambroise Paré",         "75010", "Paris",     "0149956768", "https://lariboisiere.aphp.fr",    48.8763, 2.3572),
    ("750000002", "Hôpital Cochin (test)",             "Hôpital public",  "27 Rue du Faubourg Saint-Jacques", "75014", "Paris", "0158412111", "https://cochin.aphp.fr",          48.8394, 2.3392),
    ("750000003", "Hôpital Necker (test)",             "Hôpital public",  "149 Rue de Sèvres",           "75015", "Paris",     "0144494000", "https://necker.aphp.fr",          48.8459, 2.3159),
    ("750000004", "Hôpital Saint-Louis (test)",        "Hôpital public",  "1 Avenue Claude Vellefaux",   "75010", "Paris",     "0142499494", "https://saint-louis.aphp.fr",     48.8736, 2.3638),
    ("750000005", "Clinique de la Muette (test)",      "Clinique privée", "46 Rue Nicolo",               "75016", "Paris",     "0156003000", "https://clinique-muette.fr",      48.8601, 2.2743),
    ("690000001", "Hôpital Édouard Herriot (test)",   "Hôpital public",  "5 Place d'Arsonval",          "69003", "Lyon",      "0472110000", "https://chu-lyon.fr",             45.7496, 4.8629),
    ("690000002", "Clinique du Tonkin (test)",         "Clinique privée", "22 Rue du Tonkin",            "69100", "Villeurbanne", "0478940000", None,                           45.7660, 4.8791),
    ("690000003", "Hôpital Femme Mère Enfant (test)", "Hôpital public",  "59 Boulevard Pinel",          "69500", "Bron",      "0427856000", "https://chu-lyon.fr",             45.7352, 4.9017),
    ("690000004", "Hôpital de la Croix-Rousse (test)","Hôpital public",  "103 Grande Rue de la Croix-Rousse", "69004", "Lyon", "0426109210", "https://chu-lyon.fr",            45.7726, 4.8278),
    ("690000005", "Clinique Sainte-Marie (test)",      "Clinique privée", "4 Rue du Port du Temple",     "69002", "Lyon",      "0478375000", None,                             45.7578, 4.8266),
    ("130000001", "Hôpital de la Timone (test)",       "Hôpital public",  "264 Rue Saint-Pierre",        "13385", "Marseille", "0491386500", "https://ap-hm.fr",               43.2892, 5.4034),
    ("130000002", "Hôpital de la Conception (test)",   "Hôpital public",  "147 Boulevard Baille",        "13005", "Marseille", "0491383000", "https://ap-hm.fr",               43.2881, 5.3951),
    ("130000003", "Hôpital Nord Marseille (test)",     "Hôpital public",  "Chemin des Bourrely",         "13015", "Marseille", "0491968000", "https://ap-hm.fr",               43.3478, 5.3624),
    ("130000004", "Clinique Bouchard (test)",           "Clinique privée", "77 Rue du Docteur Escat",     "13006", "Marseille", "0491175050", "https://clinique-bouchard.fr",   43.2855, 5.3737),
    ("130000005", "Clinique Monticelli (test)",         "Clinique privée", "6 Rue Cdt Rolland",           "13008", "Marseille", "0491770100", None,                             43.2540, 5.3790),
    ("310000001", "CHU de Toulouse Purpan (test)",      "Hôpital public",  "Place du Docteur Baylac",     "31059", "Toulouse",  "0561779999", "https://chu-toulouse.fr",        43.6168, 1.4031),
    ("310000002", "Hôpital Rangueil (test)",            "Hôpital public",  "1 Avenue Jean Poulhès",       "31059", "Toulouse",  "0561320000", "https://chu-toulouse.fr",        43.5731, 1.4561),
    ("310000003", "Clinique Pasteur (test)",            "Clinique privée", "45 Avenue de Lombez",         "31300", "Toulouse",  "0534259000", "https://clinique-pasteur.com",   43.6042, 1.4124),
    ("330000001", "CHU de Bordeaux Pellegrin (test)",  "Hôpital public",  "Place Amélie Raba-Léon",      "33076", "Bordeaux",  "0556795500", "https://chu-bordeaux.fr",        44.8395, -0.5943),
    ("330000002", "Hôpital Saint-André (test)",         "Hôpital public",  "1 Rue Jean Burguet",          "33075", "Bordeaux",  "0556796000", "https://chu-bordeaux.fr",        44.8370, -0.5764),
    ("330000003", "Clinique Bordeaux Nord (test)",      "Clinique privée", "15 Rue Claude Boucher",       "33300", "Bordeaux",  "0556438484", None,                             44.8681, -0.5713),
    ("590000001", "CHU de Lille (test)",                "Hôpital public",  "2 Avenue Oscar Lambret",      "59037", "Lille",     "0320445962", "https://chu-lille.fr",           50.6136, 3.0349),
    ("590000002", "Hôpital Salengro (test)",            "Hôpital public",  "Rue Émile Laine",             "59037", "Lille",     "0320444444", "https://chu-lille.fr",           50.6119, 3.0453),
    ("440000001", "CHU de Nantes (test)",               "Hôpital public",  "5 Allée de l'Île Gloriette",  "44093", "Nantes",    "0240083333", "https://chu-nantes.fr",          47.2104, -1.5416),
    ("670000001", "CHU de Strasbourg (test)",           "Hôpital public",  "1 Place de l'Hôpital",        "67091", "Strasbourg","0388116768", "https://chru-strasbourg.fr",     48.5835, 7.7574),
]

QUALITES = [
    # (finess_et, score_global, score_patient, score_securite, annee_evaluation)
    # Seulement 10 établissements évalués sur 25 — simule la réalité HAS
    ("750000001", 4.2, 4.5, 3.9, 2023),
    ("750000002", 3.8, 3.6, 4.0, 2022),
    ("690000001", 4.5, 4.7, 4.3, 2023),
    ("690000003", 4.1, 4.0, 4.2, 2023),
    ("130000001", 3.5, 3.4, 3.6, 2022),
    ("310000001", 4.3, 4.4, 4.1, 2023),
    ("330000001", 4.0, 3.9, 4.2, 2022),
    ("590000001", 4.6, 4.8, 4.5, 2023),
    ("440000001", 4.2, 4.1, 4.4, 2023),
    ("670000001", 3.9, 3.8, 4.0, 2022),
]

# ---------------------------------------------------------------------------
# Création de la base
# ---------------------------------------------------------------------------
def create_database():
    os.makedirs(ASSETS_DIR, exist_ok=True)

    # Supprime l'ancienne DB si elle existe
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
        print(f"[init_db] Ancienne DB supprimée : {DB_PATH}")

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # --- Schéma (doit correspondre EXACTEMENT aux entités Room) ---
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS etablissements (
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

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS qualite (
            finess_et           TEXT    PRIMARY KEY NOT NULL,
            score_global        REAL,
            score_patient       REAL,
            score_securite      REAL,
            annee_evaluation    INTEGER,
            FOREIGN KEY (finess_et) REFERENCES etablissements(finess_et) ON DELETE CASCADE
        )
    """)

    # --- Insertion des données ---
    cursor.executemany("""
        INSERT INTO etablissements
            (finess_et, nom, type, adresse, code_postal, ville, telephone, site_web, latitude, longitude)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, ETABLISSEMENTS)

    cursor.executemany("""
        INSERT INTO qualite
            (finess_et, score_global, score_patient, score_securite, annee_evaluation)
        VALUES (?, ?, ?, ?, ?)
    """, QUALITES)

    conn.commit()
    conn.close()

    size_kb = os.path.getsize(DB_PATH) / 1024
    print(f"[init_db] Base créée : {DB_PATH} ({size_kb:.1f} Ko)")
    print(f"[init_db] {len(ETABLISSEMENTS)} établissements insérés.")
    print(f"[init_db] {len(QUALITES)} évaluations qualité insérées.")
    print("[init_db] Terminé. Lancez './gradlew assembleDebug' pour compiler.")


if __name__ == "__main__":
    create_database()
