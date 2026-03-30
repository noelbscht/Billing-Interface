# <img src="src/main/resources/web-default/static/img/icon.png" style="vertical-align: middle; width: 128px;"> HTML Template Engine - Billing-Interface     
Die leichtgewichtige, implementierte HTML Template Engine ist von Jinja2 inspiriert.

<img src="src/main/resources/documentation/img/templating/defaults.png" style="width: 100%;">

---

## 📌 Überblick

**Die Template Engine** unterstützt:

- Variablen
- Methodenaufrufe
- Method-Chaining
- Bedingungen
- Iteratoren
- Partials (Template-Includes)

## Anwendung

### Variablen

Variablen werden mit '{{ variable }}' eingebunden.
**Standardvariablen:**
- messages
- loggedIn
- uid
- WebPermission

Beispiel:
` <p>User-ID: {{ uid }}</p> `

### Bedingungen

Syntax:

```
{% if condition %}
	...
{% else %}
	...
{% /if %}
```

Unterstützt:
- Boolean-Variablen
- Methoden
- Method-Chaining
- Vergleichsoperatoren (`==`, `!=`, `>`, `<`, `>=`, `<=`)

### Iteratoren

Syntax:

```
{% iterate item : listObj %}
	{% item %}
{% /iterate %}
```
oder bei dictionaries:

```
{% iterate key, value : listObj %}
	{% key %}: {% value %}
{% /iterate %}
```

### Methoden

Methoden können im Backend registriert werden. 
Dazu wird der `Syntax` angegeben, das `Besitzer-Objekt`, der tatsächliche `Methodenname`, sowie die erwarteten `Parametertypen`.

Bsp.:

```
addMethod("isAuthorized", ws.getPermissionLoader(), "isAuthorized", String.class, WebPermission.class);
);
```
im Template:

```
{% if isAuthorized(uid, WebPermission.ARCHIVE_WRITE) %}
	<p>Zugriff erlaubt</p>
{% /if %}
```

### Partials
Partials sind einbindbare Template-Fragmente:
`addPartial(ws, "navigation", "presets/navigation.html");`
Im Template:
`{{ navigation }}`

### Backend-Integration

In WebRouten kann ein Template folgendermaßen gerendert werden:

```
@Override
public Response onRequest(IHTTPSession session, String body, SessionHelper sessionHelper) throws Exception {
    SimpleTemplating template = new SimpleTemplating(this.ws, sessionHelper);

    t.addVariable("condition", false);

    return ws.serveTemplate("not_found.html", session, template);
}
```



