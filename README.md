# catan-placement-optimizer

Inteligentni softverski agent za optimizaciju faze početnog postavljanja u društvenoj igri **Settlers of Catan**, razvijen kao deo diplomskog rada na Fakultetu tehničkih nauka u Novom Sadu.

---

## O projektu

Settlers of Catan je kompleksna društvena igra zasnovana na heksagonalnoj mreži od 19 polja, gde svako polje proizvodi jedan od pet resursa (drvo, cigla, ovca, pšenica, ruda). Ovaj projekat se fokusira isključivo na **fazu početnog postavljanja (Placement Phase)**, u kojoj tri igrača postavljaju po dva naselja prema Snake Draft redosledu (`1 → 2 → 3 → 3 → 2 → 1`).

Cilj je razvoj agenta koji primenjuje principe teorije igara i napredne heurističke metode kako bi optimizovao odluke u ovoj fazi.

---

## Tehnološki stack

| Sloj | Tehnologija |
|------|-------------|
| Backend | Java Spring Boot |
| Frontend | Angular |
| Komunikacija | REST API / JSON |

---

## Arhitektura sistema

```
catan-placement-optimizer/
├── backend/        # Spring Boot — algoritmi, simulacije, heuristika
└── frontend/       # Angular — vizuelizacija table, heatmapa, analitika
```

---

## Algoritamski pristup

Pošto igru igraju tri igrača, klasični Minimax nije primenljiv. Koristi se:

- **MAXN algoritam** — generalizacija Minimaxа za višekorisničke igre
- **Backtracking** sa ograničenjem dubine na 3 nivoa
- Striktno poštovanje **Distance Rule-a** (naselja moraju biti udaljena najmanje 2 ivice)
- **Prediktivno planiranje** — agent P1 analizira buduća stanja nakon poteza P2 i P3

---

## Heuristički model evaluacije

```
H(s) = w₁ · Pips + w₂ · Diversity + w₃ · Scarcity
```

| Parametar | Opis |
|-----------|------|
| **Pips** | Očekivana produkcija resursa (6 i 8 imaju najveću težinu) |
| **Diversity** | Nagrađuje pokrivenost svih 5 tipova resursa |
| **Scarcity** | Dodatna vrednost za retke resurse na mapi |

---

## Tipovi botova

| Bot | Strategija |
|-----|-----------|
| **Greedy Bot** | Maksimizacija početne produkcije |
| **Scarcity Bot** | Fokus na monopol nad retkim resursima |
| **Balanced Bot** | Balansiranje produkcije, diversifikacije i retkosti |

---

## Testiranje

Svaki bot prolazi kroz **300 simulacija** faze postavljanja:

- 100 partija kao P1 (prvi igrač)
- 100 partija kao P2 (drugi igrač)
- 100 partija kao P3 (treći igrač)

### Hipoteze

- **H1:** Balanced Bot ostvaruje prosečno veću vrednost H(s) u odnosu na Greedy i Scarcity botove.
- **H2:** Balanced Bot pokazuje manju standardnu devijaciju u pokrivenosti resursa.

---

## Vizuelni modul (Angular)

- Grafički prikaz stanja Catan table
- Dinamički generisana **toplotna mapa** heurističkih vrednosti čvorova
- Tabelarni prikaz evaluacionih metrika po igraču

### Headless (Analitički) režim

Masovno pokretanje simulacija i prikupljanje statističkih podataka, bez UI sloja.

---

## Pokretanje projekta

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
ng serve
```

---

## Autor

**Veljko** — Softversko inženjerstvo i informacione tehnologije, FTN Novi Sad
