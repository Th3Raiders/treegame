# Treegame

Application Java Swing de visualisation et d'expérimentation pédagogique autour des arbres binaires de recherche (ABR) et des arbres AVL, développée dans le cadre d'un stage de recherche au GR2IF (Groupe de Recherche Rouennais en Informatique Fondamentale).

L'objectif du projet est de rendre visibles et manipulables les mécanismes internes des structures de données arborescentes — en particulier les rotations de rééquilibrage AVL, souvent traitées de façon purement théorique — à travers une interface graphique animée, interactive et exportable.

---

## Fonctionnalités

### Visualisation et manipulation
- Insertion, suppression et recherche de valeurs, avec rendu graphique en temps réel
- Deux structures affichées séparément : un ABR classique et un AVL auto-équilibré
- Navigation libre dans l'arbre (glisser pour déplacer la vue, molette pour zoomer)
- Vue côte à côte (split-pane) pour comparer visuellement les deux structures sur les mêmes données
- Menu contextuel (clic droit) sur chaque nœud : suppression directe, informations détaillées (profondeur, hauteur du sous-arbre, facteur d'équilibre pour l'AVL)
- Infobulle au survol de chaque nœud

### Animations pédagogiques
- Mode animation : visualisation pas-à-pas de la descente dans l'arbre lors d'un ajout, d'une suppression ou d'une recherche
- Animation détaillée des rotations AVL : chaque rotation (simple ou double, gauche ou droite) est identifiée, nommée et expliquée avec son facteur d'équilibre exact, puis animée par un glissement fluide des nœuds vers leur nouvelle position
- Mode pas-à-pas manuel : chaque étape attend un clic explicite sur « Suivant », utile pour une explication en direct
- Réglage de la vitesse d'animation via un curseur (de x0,25 à x3)
- Visualisation des quatre parcours classiques (préfixe, infixe, postfixe, en largeur) avec surlignage progressif

### Analyse et pédagogie
- Mode Quiz : génère un arbre aléatoire et pose des questions (localisation d'insertion, recherche de valeur, profondeur, hauteur de sous-arbre, facteur d'équilibre), avec score et retour visuel immédiat
- Analyseur de complexité empirique : compare le nombre de comparaisons (ABR vs AVL) et le nombre de rotations cumulées sur plusieurs milliers d'insertions, avec option « pire cas » (insertion strictement croissante) pour visualiser concrètement la dégénérescence d'un ABR non équilibré
- Comparaison avant/après : capture l'état de l'arbre juste avant et juste après une opération, affiché côte à côte pour un usage pédagogique statique

### Export et interopérabilité
- Export de l'arbre en image (PNG) ou en PDF
- Génération automatique du code LaTeX/TikZ correspondant à l'arbre affiché, prêt à être collé dans un document scientifique
- Sauvegarde et chargement de l'arbre dans un fichier texte (sérialisation par parcours préfixe avec marqueurs), avec sauvegarde automatique à la fermeture et rechargement à l'ouverture
- Historique des opérations, rejouable intégralement à tout moment
- Mini-langage de script pour préparer des scénarios de démonstration complexes (`ADD`, `DELETE`, `SEARCH`, `CLEAR`, `GENERATE`, boucles `REPEAT`), exécuté depuis un éditeur de texte intégré

---

## Architecture

Le projet suit une architecture en couches, séparant strictement la logique des structures de données de leur représentation graphique.

```
src/
├── node/          Interfaces et implémentations des nœuds (Node, ValuedNode, AVLNode...)
├── tree/          Interfaces et implémentations des arbres (Tree, BSTTree, StdBSTTree, StdAVLTree...)
├── binarytree/     Interface graphique Swing
│   ├── AbstractTreePanel   Classe abstraite mutualisant navigation, dessin, export, sérialisation
│   ├── BSTPanel            Panel spécifique à l'ABR
│   ├── AVLPanel            Panel spécifique à l'AVL, avec moteur d'animation des rotations
│   ├── Treegame            Fenêtre principale et câblage de l'interface
│   ├── QuizController / QuizPanel / QuizQuestion(Type)   Mode Quiz
│   ├── ComplexityAnalyzer / ComplexityChartPanel          Analyse de complexité
│   └── ...
├── script/         Mini-langage de script (tokenizer, parseur, exécuteur)
└── utils/          Composants Swing personnalisés (boutons, sliders, onglets stylisés)
```

**Points d'architecture notables :**
- `AbstractTreePanel<N>` est générique sur le type de nœud, ce qui permet à `BSTPanel` et `AVLPanel` de partager l'intégralité de la logique de dessin, de navigation, d'export et de sérialisation sans duplication de code.
- L'animation des rotations AVL ne s'appuie pas sur une simple réanimation a posteriori : le panel réimplémente fidèlement l'algorithme de rééquilibrage (rotation simple/double, mise à jour des hauteurs) afin de pouvoir s'arrêter précisément à chaque étape et en expliquer la cause exacte.
- Les opérations longues (animations, scripts, rejeu d'historique) suivent un modèle d'enchaînement par callbacks (`Runnable onFinished`), permettant de synchroniser les deux arbres et l'interface sans bloquer le thread Swing.

---

## Prérequis

- Java 8 ou supérieur
- [Apache PDFBox](https://pdfbox.apache.org/) (jar à ajouter au classpath) pour l'export PDF

## Lancement

Le point d'entrée est la classe `binarytree.Treegame` :

```bash
javac -d bin -cp <chemin_vers_pdfbox.jar> $(find src -name "*.java")
java -cp bin:<chemin_vers_pdfbox.jar> binarytree.Treegame
```

Sous Eclipse : importer le projet, ajouter le jar PDFBox au Build Path, puis exécuter `Treegame.java` en tant qu'application Java.

---

## Contexte

Ce projet a été développé en parallèle d'un stage de recherche portant sur l'implémentation de la méthode DSV pour l'énumération de polyominos via grammaires algébriques (OCaml), au sein du GR2IF. Il constitue un projet personnel indépendant, motivé par la volonté de rendre concrets et visibles des concepts d'algorithmique classique.

## Licence

Projet personnel à but pédagogique. Libre de réutilisation pour un usage non commercial.
