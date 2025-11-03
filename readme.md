# S-Emulator

S-Emulator is a multi-user, client–server environment for running and debugging programs written in a very small, assembly-like language called **S**. Multiple desktop clients (JavaFX) — and optionally lightweight web clients — can all talk to the same server at the same time. The server is the single source of truth for uploaded programs, execution state, credits, and run history.

The system is built for three things:
- Understand how an S program actually executes step by step.
- Inspect and expand higher-level operations into smaller primitive operations.
- Charge and enforce resource usage in real time (credits / cycles / architecture tier).

---

## 1. The S language (what we run)

The “S language” is intentionally minimal. It looks like a stripped-down ISA with labeled jumps, counters, and mutable registers. The goal is to make control flow and cost completely visible.

Core model:
- **Inputs:** `x1`, `x2`, `x3`, … are the external inputs to the program. The UI asks the user to provide numeric values for all required inputs before runtime.
- **Work registers:** `z1`, `z2`, … are scratch variables used internally (for example, created by expansion).
- **Output:** `y` is the program’s final reported result. After the run ends, the UI shows the final value of `y`.
- **Labels:** `L1`, `L2`, … mark jump targets and define loops / branches.
- **Flow:** execution is linear, except for conditional jumps like `IF x1 = 2 GOTO L1`, which create loops and branches.

Instruction set:
- **Basic instructions** (the “hardware level”) such as incrementing a variable, decrementing a variable (down to zero), copying one variable into another, and conditional/unconditional jumps.
- **Synthetic instructions** (the “macro level”): higher-level operations that internally expand into multiple basic instructions. Expansion may introduce fresh temporary `z*` variables and local labels, guaranteed not to collide with other code.

You can write a higher-level instruction, and the tool can show you exactly what that really means as a sequence of low-level basic steps.

---

## 2. What the S-Emulator does

### 2.1 Load & validate programs
- You upload an S program file (XML).
- The server parses it, validates it (labels, function references, structure), and either accepts it or returns a clear error.
- A valid program becomes available in the shared server catalog. Other logged-in users can immediately see it.

### 2.2 Program view
Before running, you get a structured view of the program:
- the full instruction list with line numbers,
- labels (`L#`) and jumps,
- which input variables (`x1`, `x2`, …) and temporary variables (`z#`) are used,
- which helper functions are defined in this program.

This same view is also used later to show “what actually ran,” after expansion.

### 2.3 Expansion / “degree”
Synthetic instructions can be expanded (inlined) into the primitive instructions they’re made of.
- **Degree 0** = no expansion: show the program exactly as it was written.
- Higher degrees recursively expand more layers of synthetic instructions.
- The UI shows the maximum legal expansion degree for the program, lets the user pick a degree, and regenerates a fully expanded “what will actually run” view.

Alongside the main program table there is also a “chain / lineage” panel: selecting an instruction shows where that instruction originally came from in terms of expansions. This makes it easy to trace a low-level line back to the higher-level source that generated it.

### 2.4 Run mode (full-speed execution)
When you press **Run**, the server:
1. charges an upfront cost based on the chosen architecture generation (see Architecture below),
2. starts executing the program at full speed on the server,
3. deducts credits in real time for every executed cycle (1 cycle = 1 credit).

If you run out of credits mid-run:
- execution stops immediately,
- you’re told you’re out of credits,
- you’re returned to the dashboard.

After a run finishes (either normally or because you ran out of credits), you see:
- the final value of `y`,
- the final values of all variables (`y`, then all `x` inputs in numeric order, then all `z` temps),
- how many cycles the run consumed,
- the exact expanded program that actually ran.

That snapshot is also recorded in run history.

### 2.5 Debug mode (interactive execution)
Instead of running to the end, you can choose **Debug**:
- **Step over**: execute exactly one instruction and update the machine state (`y`, all `x` / `z`, cycle counter, credits, etc.).
- **Resume**: leave step-by-step mode and let the program continue at full speed.
- **Stop**: abort the run immediately.

During Debug:
- after every step you see the updated variable state,
- the UI highlights which instruction just ran,
- the live cycle counter and your remaining credits update after every step.

Important:
- There is **no “step back”**.
- There are **no breakpoints**.
- Execution only moves forward.

### 2.6 Execution history
The system keeps history for each user and each program/function:
- run number,
- expansion degree used,
- inputs that were provided,
- final `y`,
- total cycle count.

You can open any past run and:
- inspect its final snapshot (program that actually ran, final variables, cycles),
- instantly “re-run” with the same setup (same program/function, same degree, same inputs pre-filled).

This makes iteration fast: tweak inputs, tweak expansion degree, compare cost.

---

## 3. Architecture generation (I / II / III / IV)

Before executing or debugging, you must pick an **architecture generation** (I, II, III, or IV).  
This represents the “machine class” you want to execute on.

The architecture generation matters in three ways:

1. **Capability gating**  
   Every instruction is tagged with the minimum generation that can legally execute it.  
   If the expanded program you’re about to run contains an instruction that requires generation III, and you selected generation II, the UI will flag that as unsupported and will not let you start.  
   You cannot run code on a machine that is “too weak” for it.

2. **Upfront cost**  
   When you start a run or debug session, the system immediately deducts a one-time credit cost for the chosen generation.

3. **Runtime cost**  
   While the program is executing under that generation, every executed cycle consumes one credit in real time.  
   If you hit zero credits, the run is forcibly stopped.

This makes architecture selection both:
- a correctness requirement (the chosen generation must support all required instructions), and
- an economic decision (newer / stronger architectures cost more to start and consume credits while running).

---

## 4. Screens

The UI is split into two main screens: **Dashboard** and **Execution**.  
(The web client mirrors the same concepts with a reduced feature set.)

### 4.1 Dashboard
The Dashboard is the control surface / lobby.

It shows and lets you do:

- **User & credits panel**  
  - Your username  
  - Your current credit balance  
  - A way to add more credits  
  The balance auto-refreshes so you can watch it change live.

- **Program list**  
  All programs uploaded to the server: who uploaded them, their size/complexity, maximum expansion degree, etc.  
  Multiple users can upload different programs; the server keeps all of them available. Nothing gets overwritten automatically.

- **Function list**  
  Individual helper functions exposed by those programs. You can choose to run or debug just a function, not only the main program.

- **Run history table**  
  A per-user timeline of executions:
  - run #,
  - chosen expansion degree,
  - final `y`,
  - total cycles.  
  You can open any past run, inspect its snapshot (final variables, cost), and quickly re-run with the same setup.

From the Dashboard you pick what you want to execute (program or function) and then move into the Execution screen for that target.  
If you don’t currently have enough credits to start a run at the chosen architecture generation, you’ll get an “insufficient credits” warning instead of entering execution.

### 4.2 Execution
The Execution screen is where the run (or debug session) actually happens. It brings together:

- **Header bar**
  - who is running,
  - your live credits (updates every second),
  - which program/function is being executed,
  - the selected expansion degree,
  - optional highlight modes (e.g. “show only instructions that touch `y`” or “show only jumps to EXIT”) to filter the instruction table visually.

- **Architecture selector + summary panel**
  - choose generation I / II / III / IV,
  - see how many instructions in the expanded program require each generation,
  - immediately see if your chosen generation is too weak (that case is highlighted and the Start button is disabled),
  - see counts of basic vs synthetic instructions.  
  This panel acts like a pre-flight check: “Will this program legally run on the machine I’m about to pay for?”

- **Inputs panel**
  - lists the required inputs (`x1`, `x2`, …) for this run,
  - you enter concrete numeric values.

- **Run / Debug controls**
  - toggle between **Run** (full-speed) and **Debug** (step-driven),
  - buttons for `Start`, `Step Over`, `Resume`, and `Stop`,
  - live cycle counter and live credit counter as the program runs.

- **Program table + lineage table**
  - top table: the instruction list that is actually executing (after expansion, based on your chosen degree),
  - bottom table: for the currently selected instruction, the “created by” chain — how that low-level instruction originated from higher-level synthetic code.

- **Outputs / snapshot**
  - while running (and especially in Debug) you see the live machine state:
    - `y`
    - all `x` inputs in ascending order
    - all `z` temps in ascending order
  - after Run finishes or you Stop, that final snapshot (variables, final `y`, cycles) is what gets written into history so you can revisit or re-run it later.

The web client variant exposes a lighter version of both screens: upload may be disabled, and debug is reduced (no per-step controls), but the same core flow applies — pick a target, run it on the shared server, see the result and the cost.

---

## 5. Credits and fairness

Credits are how the server enforces fairness across concurrent users.

- Each session belongs to a specific username.
- Starting a run / debug session immediately deducts an up-front cost based on the selected architecture generation.
- While running, every executed cycle costs one more credit.
- If credits drop to zero, the run is immediately stopped and control returns to the Dashboard.
- You can add credits from the Dashboard at any time, and the UI refreshes your balance continuously.

This prevents any single user from hogging compute. It also makes performance and cost visible: heavy programs cost more credits because they burn more cycles.

---

## 6. Technology

- **Client (desktop):** JavaFX 22 UI on Java 21. Renders Dashboard and Execution screens, polls the server over HTTP/JSON (OkHttp + Gson), and visualizes program structure, execution state, live credits, and history.

- **Client (web):** A lighter web front end hitting the same HTTP API. It shows program catalog, run results, credits, and history, with a reduced debugging feature set.

- **Server:** Java (servlets on Tomcat 10.x).  
  Responsibilities:
  - store uploaded programs,
  - perform validation,
  - expand synthetic instructions,
  - execute and debug programs,
  - track credits,
  - track connected users,
  - record and serve execution history.

- **DTO layer:** All communication is via DTOs (JSON). The engine never exposes its internal classes directly to the UI. That keeps the runtime logic clean and lets multiple different front ends talk to the same backend.

---

**In short:**  
S-Emulator is a multi-tenant execution & debugging environment for S programs.  
It gives full visibility into what each instruction did, what it cost, which architecture tier it required, and who ran it — in real time.
