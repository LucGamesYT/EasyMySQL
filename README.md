# EasyMySQL

EasyMySQL ist eine kleine Kotlin Bibliothek, die einfache CRUD Operationen auf einer MySQL Datenbank über Reflection ermöglicht. 
Datenklassen werden automatisch in Tabellen übersetzt und Repository-Interfaces bieten Methoden wie `findAll`, `findOneBy...`, `deleteBy...` oder `save`.

## Installation

Binde das Plugin über Gradle ein:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("net.lucgameshd:EasyMySQL:1.0.4")
}
```

Nach dem ersten Start wird eine `mysql-config.json` angelegt. Trage dort die Zugangsdaten für deine Datenbank ein oder erstelle die Datei selbst:

```json
{
  "url": "jdbc:mysql://localhost:3306/database",
  "user": "root",
  "password": "password"
}
```

## Erste Schritte

1. Rufe zu Beginn deiner Anwendung `EasyMySQLAPI.initialize()` auf. Damit wird die Verbindung hergestellt und alle Repositories registriert.
2. Markiere deine Hauptklasse mit `@RegisterRepositories` und gib die Paketnamen an, die deine Repository-Interfaces enthalten.

```kotlin
@RegisterRepositories("de.example.repositories")
class Main {
    fun start() {
        EasyMySQLAPI.initialize()
    }
}
```

## Entities und Repositories

Eine Entity ist eine Kotlin-Datenklasse, die mit `@Table` versehen wird. Das Feld mit `@Id` dient als Primärschlüssel. Mit `@Column` kann der Name oder die Länge angepasst werden.

```kotlin
@Table("users")
data class User(
    @Id val id: Int,
    val name: String,
    val email: String,
    val created: LocalDateTime
)
```

Ein Repository-Interface erweitert `SQLRepository<Entity>` und wird mit `@AutoRepository` markiert. Methoden können anhand ihres Namens automatisch in SQL umgesetzt werden.

```kotlin
@AutoRepository
interface UserRepository : SQLRepository<User> {
    fun findOneById(id: Int): Optional<User>
    fun findAllByName(name: String): List<User>
    fun deleteByEmail(email: String)
}
```

## Nutzung

Nach der Initialisierung kannst du ein Repository über die API beziehen und verwenden:

```kotlin
val userRepository = EasyMySQLAPI.getRepository(UserRepository::class)!!

val user = User(1, "Max", "max@example.com", LocalDateTime.now())
userRepository.save(user)

val result = userRepository.findOneById(1)
println(result)

userRepository.deleteByEmail("max@example.com")
```

Die Tabelle wird beim ersten Speichern automatisch erstellt, falls sie nicht existiert.

## Lizenz

MIT
