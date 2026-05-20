sequenceDiagram
    autonumber
    actor User as OS Environment
    participant Main as Main Class
    participant Factory as ConcreteFactory (Mac/Win)
    participant App as Application Client
    participant Product as ConcreteProduct

    User->>Main: Launches Main Thread
    rect rgb(240, 248, 255)
        note over Main: Step 1: Bootstrap & Detection
        Main->>Main: Detects Platform Profile via System Property
        create participant Factory
        Main->>Factory: Instantiate matching Factory Instance
    end
    
    rect rgb(245, 240, 255)
        note over Main, App: Step 2: Dependency Injection
        create participant App
        Main->>App: new Application(UIFactory)
        App->>Factory: createButton()
        Factory-->>App: returns ConcreteButton
        App->>Factory: createCheckbox()
        Factory-->>App: returns ConcreteCheckbox
    end

    rect rgb(240, 255, 240)
        note over App, Product: Step 3: Execution Runtime
        Main->>App: app.paint()
        App->>Product: render()
        App->>Product: check()
    end