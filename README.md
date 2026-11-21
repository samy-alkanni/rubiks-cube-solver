## Exécution (JavaFX 25 + Maven Wrapper)

Ce projet utilise **Maven** (avec wrapper) et **ne dépend d'aucun JAR local** dans `lib/`.

### Prérequis

- **JDK 21+** (JavaFX 25 nécessite Java 21 ou plus)
- Sur Windows, si `JAVA_HOME` n'est pas défini, il faut au minimum que `java` soit disponible dans le `PATH`.

### Lancer l'application (le plus simple)

- Double-cliquez sur `run.cmd`

### Lancer via ligne de commande

- Exécuter JavaFX : `mvnw.cmd javafx:run`
- Compiler/packager : `mvnw.cmd -DskipTests package`

Les artefacts de build sont générés dans `target/`.
