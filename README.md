# Red Sox Intelligence Dashboard Starter

This is a beginner-friendly starter project for a Red Sox portfolio app.

It already includes:

- Java 21
- Spring Boot
- Thymeleaf pages
- Spring Data JPA
- Sample Red Sox game data
- Game list page
- Game detail page
- Game notes feature
- Basic filtering by opponent and result
- H2 demo database by default
- PostgreSQL config for the real portfolio version

## What this starter does right now

When you run it, you can open:

```text
http://localhost:8080
```

You will see a basic Red Sox dashboard.

You can also open:

```text
http://localhost:8080/games
```

That page shows sample Red Sox games from the database.

Click a game opponent name to open the game detail page. From there, you can add and delete personal game notes.

## Apps you need installed

Install these first:

1. Visual Studio Code
2. JDK 21
3. Git
4. PostgreSQL and pgAdmin, for later
5. Postman, for later API testing

For VS Code, install these extensions:

- Extension Pack for Java
- Spring Boot Extension Pack

## How to open this project in VS Code

1. Download and unzip this project.
2. Open VS Code.
3. Click **File**.
4. Click **Open Folder**.
5. Select the unzipped `red-sox-tracker-starter` folder.
6. Wait for the Java extensions to finish loading.

## How to run it in VS Code

Open the VS Code terminal and run:

```bash
mvn spring-boot:run
```

On Windows, you can also double-click `run-app.bat`, or open PowerShell in the project folder and run:

```powershell
.\run-app.ps1
```

Then open:

```text
http://localhost:8080
```

## Maven requirement

This starter does not include the Maven wrapper, so install Apache Maven if `mvn` is not recognized in your terminal. After installing Maven, close and reopen VS Code.

## Default database mode

The app starts with an H2 in-memory database.

That means:

- You do not need PostgreSQL immediately.
- Sample data loads from `src/main/resources/data.sql`.
- Data resets every time you restart the app.

This is intentional so you can see the app working before dealing with database setup.

## H2 console

You can view the demo database here:

```text
http://localhost:8080/h2-console
```

Use:

```text
JDBC URL: jdbc:h2:mem:redsoxtracker
User Name: sa
Password: leave blank
```

## PostgreSQL setup later

When you are ready to use PostgreSQL:

1. Open pgAdmin.
2. Create a database named:

```text
redsox_tracker
```

3. Open this file:

```text
src/main/resources/application-postgres.properties
```

4. Replace this line:

```text
spring.datasource.password=CHANGE_ME_TO_YOUR_POSTGRES_PASSWORD
```

with your real PostgreSQL password.

5. Run the app with the postgres profile:

Windows PowerShell:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Mac/Linux/Git Bash:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Suggested next features

Build these in order:

1. Add a manual Add Game form.
2. Add Edit Game and Delete Game buttons.
3. Add filters for home/away and scheduled/final games.
4. Add a real Panic Meter service instead of the hardcoded 38/100.
5. Add a Player entity and Players page.
6. Add a Fenway Planner table.
7. Add an MLB API sync service.
8. Add tests for the Panic Meter and GameService.
9. Deploy the app.

## Resume description once improved

Red Sox Intelligence Dashboard is a Java Spring Boot and PostgreSQL web application that tracks Boston Red Sox games, notes, trends, and fan planning data using searchable dashboards and custom analytics such as opponent records and a Red Sox Panic Meter.
