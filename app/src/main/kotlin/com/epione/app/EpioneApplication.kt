package com.epione.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Point d'entrée Hilt — déclenche la génération du graphe de dépendances. */
@HiltAndroidApp
class EpioneApplication : Application()
