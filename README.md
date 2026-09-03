# Épione — robot de mise à jour des données

Ce dépôt n'héberge **pas** le code de l'application Épione (celui-ci vit sur
[Codeberg](https://codeberg.org/kapoue/Epione)). Il sert uniquement à faire
tourner, via GitHub Actions, la régénération mensuelle automatique de la base
de données FINESS de l'application.

Le robot ([`.github/workflows/update-finess-data.yml`](.github/workflows/update-finess-data.yml)) :
1. se déclenche automatiquement le 2 de chaque mois (un jour après la
   publication de l'instantané mensuel officiel FINESS par l'État, pour ne
   jamais arriver avant elle) ;
2. clone le dépôt Codeberg, régénère la base à partir des données
   gouvernementales les plus récentes ;
3. republie la nouvelle base sur Codeberg (commit + release), déclenchant la
   mise à jour automatique côté utilisateurs.

Un déclenchement manuel est aussi possible depuis l'onglet **Actions** de ce
dépôt (bouton *Run workflow*).
