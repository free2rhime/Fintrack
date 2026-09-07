# FINTRACK — DEVELOPMENT WORKFLOW & AGENT CONTRACT

> Sursa tehnică de adevăr în Android AI Studio este ramura `main` a repository-ului GitHub:
> `https://github.com/free2rhime/Fintrack`

---

## 1. SCOP ȘI MEDIU DE EXECUȚIE

Acest fișier definește workflow-ul de lucru unitar pentru FinTrack în **Android AI Studio**, asigurând alinierea strictă a principiilor și disciplinei de dezvoltare cu cele utilizate în **Antigravity**.

### Relația GitHub vs. Mediu Local Antigravity
1. **Sursa Tehnică de Adevăr:** În Android AI Studio, singura sursă de adevăr disponibilă este ramura `main` din GitHub.
2. **Limitări de Mediu:** Android AI Studio **NU** are acces la filesystem-ul local, uneltele locale, Git-ul local, branch-urile locale, stash-urile sau modificările neîmpinse din Antigravity.
3. **Fără Presupuneri Nejustificate:** Nu se presupune că GitHub conține modificări aflate doar local în Antigravity și nici că o modificare absentă pe GitHub nu există local.
4. **Fără Reconciliere Forțată:** Dacă există posibilitatea unei discrepanțe între medii, limita se declară explicit. Nu se rulează comenzi arbitrare de merge/pull/reset/revert pentru reconciliere automată.

---

## 2. WORKFLOW PRINCIPAL

Fiecare task parcurge secvențial următoarele etape:

```text
ANALYZE
   ↓
DRILL
   ↓
[RESEARCH]        (numai dacă este relevant)
   ↓
[ARCHITECTURE]    (numai dacă este relevant)
   ↓
[DEBUG]           (numai dacă este vorba despre un bug)
   ↓
IMPLEMENT
   ↓
VERIFY
   ↓
LAND
   ↓
[UPDATE PROJECT MEMORY] (numai la modificări de stare durabilă)
```

Skill-urile dintre paranteze drepte `[...]` sunt activate numai atunci când contextul tehnic o impune.

---

## 3. SPECIFICAȚIA ETAPELOR (SKILLS)

### 3.1 ANALYZE & DRILL
**Rol:** Transformă cererea într-o specificație tehnică precisă înainte de a atinge codul.
- Stabilește:
  - Problema exactă și comportamentul actual.
  - Comportamentul dorit.
  - Componentele/fișierele afectate și dependențele directe.
  - Invarianții de sistem (Household scoping, security, outbox FIFO).
  - Criteriile de acceptanță (Acceptance Criteria) și limitele explicite (**Non-goals**).
  - Riscurile identificate și testele necesare.
- **Invarianți FinTrack protejați:**
  - Household scoping strict (fără household sintetic, fără fallback global).
  - Autentificare Firebase & Google Sign-In via Credential Manager.
  - Dual-layer persistence: Room (SQLite offline-first) + Firestore sync.
  - Sincronizare bidirecțională prin `OutboundSyncEngine`, `SyncOutboxDao` și Firestore listeners.
  - RBAC: rolurile OWNER vs MEMBER (administrare membri și categorii rezervate OWNER-ului).
  - Import/Export CSV și conversie automată EUR/RON prin rate BNR.
- *Regulă:* Dacă task-ul este ambiguu sau riscă să schimbe arhitectura, se cer clarificări înainte de implementare.

### 3.2 RESEARCH (La nevoie)
**Rol:** Documentare tehnică atunci când task-ul implică:
- API-uri Android noi sau modificate.
- Firebase / Firestore security rules sau SDK updates.
- Jetpack Compose, Room migrations, Gradle, Kotlin coroutines.
- Biblioteci externe.
- *Prioritate surse:* 1. Documentație oficială Google/Android/Firebase; 2. Repository-ul FinTrack; 3. Surse secundare. Nu se introduce o bibliotecă nouă pe baza unor presupuneri.

### 3.3 ARCHITECTURE (La nevoie)
**Rol:** Evaluarea deciziilor structurale înainte de modificări arhitecturale:
- Păstrează separarea strictă a straturilor:
  ```text
  UI / Jetpack Compose
          ↓
       ViewModel
          ↓
      Repository
          ↓
   Room / Firestore
          ↓
  Sync / Outbox Engine
  ```
- Fără surse duplicate de adevăr (Single Source of Truth).
- Fără rezolvări superficiale în UI pentru defecte din straturile de sincronizare sau date.
- Verificarea compatibilității înapoi (backward compatibility) pentru schemele Room și DTO-urile Firestore.

### 3.4 DEBUG (La rezolvarea de bug-uri)
**Rol:** Debugging bazat pe ipoteze și dovezi, excluzând abordarea prin încercare și eroare (*trial-and-error*).
- Flux:
  `OBSERVE → REPRODUCE → FORMULATE HYPOTHESIS → COLLECT EVIDENCE → ISOLATE ROOT CAUSE → MINIMAL FIX → REGRESSION TEST`
- **Sub-domenii de diagnosticare în FinTrack:**
  1. *Authentication:* Firebase user, UID, token validity, AuthStateListener.
  2. *Household Resolution:* Household ID activ, membership, roluri și permisiuni.
  3. *Inbound Sync:* Firestore snapshot listeners, suprimare/ecou, scrieri în Room.
  4. *Outbound Sync:* `SyncOutboxDao`, operațiuni `PENDING` / `IN_PROGRESS` / `FAILED`, backoff retry, scrieri Firestore.
  5. *SyncStatus:* Tranziții de stare, propagare erori sanitizate, recovery.
  6. *Coroutine Lifecycle:* `CoroutineScope`, `Job`, `SupervisorJob`, anulare la schimbare context, test dispatchers.
- **Regulă critică pentru coroutines:**
  - Dacă apare `UncompletedCoroutinesError`, se identifică exact coroutine-ul/Job-ul activ și proprietarul său din lifecycle.
  - Nu se folosesc `advanceUntilIdle()`, întârzieri artificiale sau rescrieri de teste ca soluții de mascare.

### 3.5 IMPLEMENT
**Rol:** Modificare minimă, reversibilă și curată a codului:
- Limitare strictă la scope-ul task-ului (fără refactoring oportunist necorelat).
- **Regula contextuală pentru teste:**
  - Pentru orice modificare de comportament, logică, sincronizare, persistență sau reguli de business: se identifică testul relevant înainte de modificare, se adaugă/modifică testul dacă este necesar și se confirmă că reproduce/previne problema.
  - Pentru modificări pur vizuale sau de configurare/documentare care nu necesită teste noi: se explică explicit de ce.
  - Nu se modifică niciodată testele existente doar pentru a forța trecerea unui build.

### 3.6 VERIFY
**Rol:** Validarea riguroasă a soluției.
- **"Compile successful" ≠ "Task verified".**
- Un task este verificat doar când dovezile concrete confirmă absența regresiilor:
  - **Compilation:** Build complet fără erori (`compile_applet` sau `:app:assembleDebug`).
  - **Unit & Logic Tests:** Rularea suitei relevante (ex: `gradle :app:testDebugUnitTest`).
  - **Room:** Dacă se atinge baza de date, se verifică Room schema location, migrarea și testele Room.
  - **UI / Motion:** Rularea testelor Robolectric / Roborazzi relevante dacă au fost atinse ecrane sau componente.
  - **Sync:** Verificarea inbound, outbound, outbox shielding și coroutine termination.

### 3.7 LAND
**Rol:** Încheierea curată și structurată a task-ului:
- Inspectarea diff-ului și eliminarea fișierelor sau modificărilor accidentale.
- Verificarea formatării și a integrității repo-ului.
- **Fără commit/push automat:** Nu se execută comenzi de commit sau push către remote fără instrucțiunea explicită a utilizatorului.
- **Formatul standard de raportare la final de task:**
  ```text
  Problem:        [Descrierea precisă a problemei adresate]
  Root cause:     [Cauza tehnică izolată, dacă a fost un bug]
  Files changed:  [Lista fișierelor create sau modificate]
  Changes made:   [Rezumatul concis al modificărilor aduse]
  Tests executed: [Comenzile de test executate și rezultatele obținute]
  Build result:   [Statusul compilării]
  Known risks:    [Eventualele riscuri reziduale sau limitări identificate]
  Next step:      [Următorul pas recomandat]
  ```

---

## 4. COMUNICARE ȘI INTERACȚIUNE CU UTILIZATORUL

1. **Fără simulare sau expunere a Chain-of-Thought:** Agentul efectuează intern rutarea pe skill-uri și raționamentul tehnic, prezentând utilizatorului doar rezultatele structurate.
2. **Informațiile prezentate utilizatorului:**
   - Problema înțeleasă și limitele scope-ului.
   - Dovezile colectate din repository/teste.
   - Ipoteza și root cause-ul.
   - Planul minimal de intervenție.
   - Modificările efectuate.
   - Rezultatele testelor și dovezi de execuție.
   - Riscurile și recomandările.
3. **Core-Interview:** Se adresează întrebări doar atunci când există o ambiguitate critică de business, model de date sau permisiuni care nu poate fi dedusă din inspecția codului existent.

---

## 5. GUVERNANȚA PROJECT MEMORY

- **Fără actualizare automată la fiecare task:** Nu se modifică `FINTRACK_CURRENT_CONTEXT.md` pentru modificări mici de rutină.
- **Când se actualizează:** Numai atunci când modificarea aduce schimbări durabile asupra:
  - Arhitecturii sau responsabilităților modulelor.
  - Schemelor de date Room sau structurii DTO Firestore.
  - Mecanismelor de sincronizare și lifecycle.
  - Regulilor de securitate sau Firebase Auth.
  - Stării cunoscute a bug-urilor majore sau a milestone-urilor din roadmap.
- **Stil:** Project Memory rămâne concis, axat pe starea curentă a proiectului și decizii arhitecturale, nu ca jurnal de commit-uri.
