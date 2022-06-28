# AdAid - Design

## Klassendiagramme
Im folgenden werden die implementierten Java Klassen in mehreren Klassendiagrammen dargestellt. Die Unterteilung des Klassendiagramms erfolgt auf Package Level und soll die Übersichtlichkeit verbessern. Vererbungen von Android-Framework spezifischen Klassen werden nicht angegeben. Dies aus dem Grund, dass die solchen Klassen sehr komplex im Aufbau sind aber dabei nicht viel zum Klassenverständnis beitragen. 


### Rule & Database

Package: ch.bfh.adaid.db.*

```mermaid
classDiagram

    class Rule {
        +long id
        +String name
        +String appId
        +String viewId
        +String viewText
        +ActionType actionType
        +int coolDown

        +Rule()
        +isMatchingAppId(appId) boolean
        +isMatchingViewText(text) boolean
    }
    Rule "1" *-- ActionType

    class RuleDao {
        <<interface>>
        +getAll()* List<Rule>
        +getById(ruleId)* Rule
        +findByName(name)* Rule
        +insert(rule)* long
        +update(rule)* int
        +delete(rule)* int
        +deleteAll()*
    }
    RuleDao -- Rule

    class RuleDatabase {
        <<abstract>>
        -RuleDatabase INSTANCE$

        +RuleDatabase()
        +ruleDao()* RuleDao
        +getDatabase(context)$ RuleDatabase
    }
    RuleDao --* "1" RuleDatabase

    class RuleDataSource {
        -RuleDao ruleDao
        -ArrayList<RuleObserver> observers$
        +ExecutorService executor$

        +RuleDataSource(context)
        +addObserver(observer)
        +removeObserver(observer)
        +add(observer, rule)
        +change(observer, rule)
        +remove(observer, rule)
    }
    RuleDatabase --o "1" RuleDataSource
    Rule -- RuleDataSource

    class RuleObserver {
        <<interface>>
        +onRuleLoad(rules)*
        +onRuleAdded(rule)*
        +onRuleChanged(rule)*
        +onRuleRemoved(rule)*
        +onRuleError(error, rule)*
    }
    RuleObserver -- Rule
```


### Action

Package: ch.bfh.adaid.action.*

```mermaid
classDiagram

    Rule "1" *-- ActionType

    class ActionType {
        <<enumeration>>
        ACTION_SWIPE_LEFT
        ACTION_SWIPE_RIGHT
        ACTION_SWIPE_UP
        ACTION_SWIPE_DOWN
        ACTION_CLICK
        ACTION_MUTE
    }

    class Action {
        <<abstract>>
        #AccessibilityService service

        +Action(service)
        +triggerSeen(node)*
        +triggerGone()*
    }

    class ActionFactory {
        +buildAction(type, service)$ Action
    }
    Action -- ActionFactory
    ActionType -- ActionFactory

    class ClickAction {
        +ClickAction(service)
        +triggerSeen(node)
        +triggerGone()
    }
    ClickAction ..|> Action

    class MuteAction {
        -AudioManager audioManager
        +MuteAction(service)
        +triggerSeen(node)
        +triggerGone()
    }
    MuteAction ..|> Action

    class SwipeAction {
        -GestureDescription swipeGesture
        +SwipeAction(service, direction)
        +triggerSeen(node)
        +triggerGone()
    }
    SwipeAction ..|> Action
```


### Service

Package: ch.bfh.adaid.service.*

```mermaid
classDiagram

    class RuleWithExtras {
        +Rule r
        -boolean triggeredByLastEvent
        +Action action

        +RuleWithExtras(rule)
        +wasTriggeredByLastEvent() boolean
        +setTriggeredByCurrentEvent(triggered)
    }
    RuleWithExtras "1" *-- Action
    RuleWithExtras "1" o-- Rule

    class A11yService {
        -ArrayList<RuleWithExtras> rules

        +isA11yServiceEnabled()$ boolean
        +onCreate()
        +onServiceConnected()
        +onAccessibilityEvent(event)
        +onInterrupt()
        +updateListenedPackages()
        +processRulesForEvent(appId, node)
        +processRuleForNode(rule, node)
        +onRuleAdded()
        +onRuleRemoved()
    }
    A11yService ..|> RuleObserver
    A11yService "0...n" *-- RuleWithExtras
```


### GUI

Package: ch.bfh.adaid.gui.*

```mermaid
classDiagram

    %% ToDo
    class MainActivity {
        -RuleDataSource data

        #onCreate(savedInstanceState)
        +onStart()
        +onRuleLoad(rules)
        +onRuleAdded(rule)
        +onRuleChanged(rule)
        +onRuleRemoved(rule)
        +onRuleError(error, rule)
    }
    MainActivity ..|> RuleObserver
    MainActivity "1" o-- RuleDataSource

    class RuleActivity {
        <<abstract>>
        #RuleDataSource data
        #Rule rule
        #boolean formValid
        
        #onCreate(savedInstanceState)
        +finish()
        +onOptionsItemSelected(item) boolean
        #initFormFromRule()
        #setRuleFromView()
        +onRuleError(error, rule)
        #saveRule()*
        #deleteRule()*
    }
    RuleActivity ..|> RuleObserver
    RuleActivity "1" o-- RuleDataSource
    RuleActivity "1" *-- AppAdapter

    class AppAdapter {
        -PackageManager packageManager
        +AppAdapter(context)
        +getView(position, convertView, parent) View
    }

    class EditRuleActivity {
        +String EXTRA_RULE_ID$
        -long ruleId

        +getStartActivityIntent(callingActivity, ruleId)$ Intent
        #onCreate(savedInstanceState)
        +onRuleLoad(rules)
        +onRuleAdded(rule)
        +onRuleChanged(rule)
        +onRuleRemoved(rule)
        #saveRule()
        #deleteRule()
    }
    EditRuleActivity ..|> RuleActivity

    class NewRuleActivity {

        +getStartActivityIntent(callingActivity)$ Intent
        #onCreate(savedInstanceState)
        +onRuleLoad(rules)
        +onRuleAdded(rule)
        +onRuleChanged(rule)
        +onRuleRemoved(rule)
        #saveRule()
        #deleteRule()
    }
    NewRuleActivity ..|> RuleActivity
```


## Sequenzdiagramme
Die folgenden Sequenzdiagramme dokumentieren den groben Ablauf der Applikation resp. deren Verhalten. Dabei wird nicht zu sehr ins Detail beschrieben und nur einen konzeptuellen Überblick geschaffen.

### Gesamt
Die abstrakte Gesamtsequenz nach der die Applikation entworfen wurde.

```mermaid
sequenceDiagram
    participant Android
    participant App
    participant db as RulesDatabase
    participant bg as BackgroundService

    Android ->> App: Öffnen
    App ->> db: Regeln laden
    activate db
    db ->> App: Regeln senden
    deactivate db
    App ->> db: Regeln bearbeiten / erstellen
    activate db
    db ->> bg: Aktualisierung
    deactivate db

    Android ->> bg: Start
    bg ->> db: Regeln laden
    activate db
    db ->> bg: Regeln senden
    deactivate db

    Android ->> bg: UI Events
    activate bg
    bg ->> bg: Regel verarbeiten
    bg ->> Android: Aktionen ausführen
    deactivate bg
    
```


### Benutzerinteraktion
Wie der Benutzer die Applikation bedienen kann.

```mermaid
sequenceDiagram
    actor user as User
    participant main as MainActivity
    participant edit as EditRuleActivity
    participant new as NewRuleActivity

    user ->> main: App starten
    activate main
    main ->> main: Regeln aus Datenbank laden
    main ->> user: Auflistung konfigurierter Regeln

    alt User möchte bestehende Regel bearbeiten

        user ->> main: Klick auf Regel
        main ->> edit: Aktivität starten
        deactivate main
        activate edit

        edit ->> user: Formular mit Daten aus existierender Regel anzeigen
        opt
            user ->> edit: Daten im Formular verändern.
        end

        alt User möchte Änderungen speichern
            user ->> edit: Klick auf "Änderungen speichern"
            edit ->> edit: Regel in Datenbank verändern
        else User möchte Regel löschen
            user ->> edit: Klick auf "Regel löschen"
            edit ->> edit: Regel aus Datenbank löschen
        else User möchte Änderungen verwerfen
            user ->> edit: Zurück-Geste / Zurück-Button / Zurück Pfeil oben links
        end

        edit ->> main: Zurück zu MainActivity
        deactivate edit

    else User möchte neue Regel erstellen

        activate main
        user ->> main: Klick auf "+" Button
        main ->> new: Aktivität starten
        deactivate main
        activate new

        new ->> user: Leeres Formular anzeigen
        opt
            user ->> new: Daten im Formular verändern.
        end

        alt User möchte Regel speichern
            user ->> new: Klick auf "Änderungen speichern"
            new ->> new: Regel in Datenbank erstellen
        else User möchte Regel verwerfen
            user ->> new: Zurück-Geste / Zurück-Button / Zurück Pfeil oben links
        end

        new ->> main: Zurück zu MainActivity
        deactivate new

    end
```


### Accessibility Service
Wie der A11yService ausgeführt wird.

```mermaid
sequenceDiagram
    participant a as Android
    participant s as A11yService
    participant act as Action

    a ->> s: Service starten
    activate s
    s ->> s: Regeln aus Datenbank laden

    loop über alle Regeln
        s ->> act: Aktion für Regel erstellen
        activate act
        act ->> s: Aktion
        deactivate act
    end

    deactivate s

    a ->> s: Window Content Changed Event
    activate s

    loop über alle aktivierten Regeln iterieren
        s ->> s: Regel verarbeiten

        alt View der die Regel auslöst wird zu ersten mal gesehen
            s ->> act: "Seen" Aktion auslösen
            activate act
            act ->> a: Aktion ausführen
            deactivate act
        else View der die Regel ausgelöst hatte wurde zu letzten mal gesehen
            s ->> act: "Gone" Aktion auslösen
            activate act
            act ->> a: Aktion ausführen
            deactivate act
        end
    end

    deactivate s
```
