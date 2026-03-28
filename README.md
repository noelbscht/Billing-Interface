# <img src="src/main/resources/web-default/static/img/icon.png" style="vertical-align: middle; width: 128px;"> BillingInterface (GoDB-Archive)   
Ein GoBD-orientiertes Archivsystem  
*(in Entwicklung)*

<img src="src/main/resources/documentation/img/web_interface/index.png" style="width: 100%;">

---

## 📌 Überblick

**BillingInterface** ist ein Archivsystem, das langfristig vollständig **GoBD-konform** arbeiten soll.  
Das Projekt dient als **technische Demonstration im Rahmen einer Ausbildungsbewerbung** und befindet sich aktiv im Ausbau.

Das System bietet:

- ein **CLI** zur direkten Steuerung  
- einen **ServiceHandler** mit **TUI**, um Webdienste wie Webserver & API zu verwalten  
- ein **Webinterface** zur Archivverwaltung, sowie als Zugang für Betriebsprüfer 
- eine **REST-API** mit API-Key Authentifizierung  
- eine **MySQL-basierte, anpassbare Benutzerverwaltung** für das Webinterface  
- flexible **Konfiguration über `.env` und weitere Konfigurationsdateien**  
- ein **GoBD-orientiertes Archivierungsmodell** mit Audit-Log und Metadaten
- eine einfache [Templating-Language](/TEMPLATING_README.md) inspiriert von Jinja2

> ⚠️ **Wichtig:**  
> Das Projekt ist **ausschließlich unter Linux lauffähig**, da Terminal-Steuerung, Dateirechte und Systembefehle Linux-spezifisch sind.

---

## 🧩 Architektur

### 1. **CLI**

<img src="src/main/resources/documentation/img/cli/example.png" style="width: 45%;">

CLI Anwendungsbeispiel:

```bash
java -jar archive.jar ws full --serve
```

oder je nach Konfiguration:

```bash
billing-interface ws full --serve
```

### 2. **ServiceHandler (mit TUI)**

<img src="src/main/resources/documentation/img/tui/overview.png" style="width: 45%;">

Der ServiceHandler:
- startet und "überwacht" Webdienste (Webserver, API, etc.)
- bietet ein TUI, das über die Tastatur steuerbar ist (A/D Navigation)
- zeigt Live-Logs der einzelnen Services an
- besitzt eine Statusübersicht der Dienste (Index -1)
Die Logs werden nicht gespeichert, sondern nur live angezeigt — GoBD-relevant ist ausschließlich das Audit-Log des Archivsystems.

### 3. Webserver & Webinterface

<img src="src/main/resources/documentation/img/web_interface/dashboard/sidebar.png" style="height: 49%">
<img src="src/main/resources/documentation/img/tui/http.png" style="width: 45%;">

Das Webinterface:
- kommuniziert ausschließlich über Web-Routen, nicht direkt mit der API
- nutzt eine MySQL-basierte Benutzerverwaltung
- ist vollständig konfigurierbar
- unterstützt zukünftig Datei-Uploads, Queries, Exporte, Verifizierungen und mehr.

### 4. REST-API
Die API ist nicht direkt vom Frontend erreichbar, sondern wird über interne API-Routen angesprochen.
Sie soll später u.a. den Austausch zwischen dem Backup-Archiv und dem Hauptarchiv ermöglichen.

### 5. Authentifizierung & Sicherheit
**API-Key**
- Wird in der .env konfiguriert

Webinterface-Login
- Benutzer werden aus einer MySQL-Datenbank geladen
- Sessions werden serverseitig verwaltet
- Anpassungsfähig auf verschiedene MySQL- Tabellen.


## **Installation & Start**
### 1. JAR erstellen
```bash
mvn clean package
```

### 2. `.env` erstellen

```bash
cp .env.example .env
```

### 3. Programmstart (Webinterface & API)
```bash
cd /pfad/zur/jar
java -jar archive.jar ws full --serve
```


## Optionaler Alias

```bash
alias billing-interface='cd /opt/gobd-archive && java -jar archive.jar ws full --serve'
```

Dann reicht künftig:
```bash
billing-interface
```
, um auf das CLI zuzugreifen.

## GoBD-Konformität (Zielsetzung)

Das Projekt orientiert sich an den Grundsätzen der GoBD:

### Unveränderbarkeit
- Audit-Log  
- Hash-Verifikation  
- Commit-Typen und Metadaten  

### Nachvollziehbarkeit & Nachprüfbarkeit
- klare Trennung von UI, API und Archivlogik  
- strukturierte Metadaten  
- Verifizierungsrouten  

### Dokumentation
- Konfiguration über Dateien  
- klare Start- und Betriebsmodi  
- Eine geplante Verfahrensdokumentation

### Zugriffsschutz
- API-Key im Backend  
- MySQL-basierte Benutzerverwaltung im Webinterface

### Protokollierung
- revisionssichere Audit-Logs  
- TUI-Logs nur als Live-Ansicht 

### 📄 Copyright / Lizenz
[COPYRIGHT](/COPYRIGHT)