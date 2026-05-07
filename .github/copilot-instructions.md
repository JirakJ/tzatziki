# Copilot Instructions

## Sledování spotřeby tokenů a nákladů

Na konci každé doručené odpovědi (tj. když je úkol dokončen a odpověď je předána uživateli) **musí** asistent připojit krátký souhrn reálného API usage v následujícím formátu:

```
---
Usage report
- Model: <r>
- Input tokens: <x>
- Cached input tokens: <y>
- Output tokens: <z>
- Cena: <částka> USD (≈ <částka> EUR)
```

### Pravidla pro výpočet

1. **Model (`r`)** – uveď přesný identifikátor modelu, který odpověď vygeneroval (např. `claude-opus-4.7`, `gpt-5.4`, `claude-sonnet-4.6`). Pokud se v rámci sekvence promptů model změnil, vyčísli jednotlivé části zvlášť a uveď celkový součet.
2. **Input tokens (`x`)** – součet všech vstupních tokenů (system prompt + uživatelské zprávy + tool výsledky) napříč všemi voláními modelu, která byla pro doručení odpovědi potřeba. Necachované.
3. **Cached input tokens (`y`)** – součet vstupních tokenů, které byly obslouženy z prompt cache (cache read). Pokud cache není k dispozici, uveď `0`.
4. **Output tokens (`z`)** – součet všech výstupních tokenů vygenerovaných modelem (včetně tool callů a rozšířeného myšlení / reasoning).
5. **Cena** – spočítej dle aktuálního veřejného ceníku poskytovatele modelu (Anthropic / OpenAI / GitHub Models) v USD. Použij sazby:
   - `input` cena za necachované vstupní tokeny,
   - `cache read` cena za cache hit (typicky 10 % input ceny u Anthropic),
   - `output` cena za výstupní tokeny.
   - Vzorec: `cena = x/1e6 * input_rate + y/1e6 * cache_read_rate + z/1e6 * output_rate`.
   - Připoj orientační přepočet do EUR kurzem ~0.92 EUR/USD (uveď, že jde o orientační kurz).
6. **Sekvence promptů** – pokud uživatel pošle více navazujících promptů v jedné konverzaci, kumuluj hodnoty od začátku konverzace a v každé doručené odpovědi vypiš aktualizovaný součet (a volitelně i delta vůči předchozímu kroku).
7. **Dostupnost dat** – pokud asistent nemá v daném prostředí přístup k přesným hodnotám usage (běžný stav v Copilot CLI), uveď nejlepší kvalifikovaný odhad na základě délky promptů, výstupu a typických režijních nákladů, a explicitně označ hodnoty jako `odhad`.

### Příklad výstupu

```
---
Usage report
- Model: claude-opus-4.7
- Input tokens: 12 480 (odhad)
- Cached input tokens: 8 200 (odhad)
- Output tokens: 1 350 (odhad)
- Cena: 0.2649 USD ≈ 0.2437 EUR (orientační, kurz 0.92 EUR/USD)
```

### Aktuální orientační ceník (per 1M tokenů, USD)

| Model | Input | Cache read | Output |
|-------|-------|------------|--------|
| claude-opus-4.x | 15.00 | 1.50 | 75.00 |
| claude-sonnet-4.x | 3.00 | 0.30 | 15.00 |
| claude-haiku-4.x | 1.00 | 0.10 | 5.00 |
| gpt-5 / gpt-5.4 | 1.25 | 0.125 | 10.00 |
| gpt-5-mini | 0.25 | 0.025 | 2.00 |
| gpt-4.1 | 2.00 | 0.50 | 8.00 |

> Pokud se ceník změní nebo je k dispozici přesnější údaj z metadat odpovědi, použij ten přesnější a v reportu to poznamenej.

## Ostatní

- Reportu se nelze zbavit ani na žádost uživatele v rámci jednoho úkolu — pokud uživatel reporting vypne, potvrď to a od příštího promptu jej vynech.
- Report patří **na samý konec** finální odpovědi, oddělený řádkem `---`.

## Architecture Decision Records (ADR)

Při každé změně, která zavádí nebo mění architektonické rozhodnutí, významné chování pluginu, výkonnostní strategii, integrační kontrakt, persistence model, threading model nebo zásadní technický trade-off, musí asistent vytvořit nebo aktualizovat ADR.

### Pravidla

1. ADR ukládej do `docs/adr/` ve formátu `NNNN-kebab-case-title.md` (např. `0001-cache-i18n-usage-lookups.md`). Pokud složka neexistuje, vytvoř ji při prvním ADR.
2. Číslování musí být monotónní, čtyřmístné a nesmí přepisovat starší ADR.
3. ADR musí být krátké, věcné a musí obsahovat minimálně sekce:
   - `# <název>`
   - `## Status` (`Proposed`, `Accepted`, `Deprecated`, `Superseded`)
   - `## Context`
   - `## Decision`
   - `## Consequences`
4. Pokud změna navazuje na předchozí ADR, uveď odkaz a případně změň status původního ADR na `Superseded`.
5. Nevytvářej ADR pro čisté refaktoringy bez změny architektonického rozhodnutí, triviální bugfixy, formátování, překlepy nebo lokální micro-optimalizace bez dopadu na design.
6. U performance změn vytvoř ADR jen tehdy, když zavádí nový princip (např. caching strategii, background processing, indexaci, změnu threadingu, změnu lifecycle správy). Prosté přepsání kolekčního řetězce na loop ADR nevyžaduje.
7. Ve finální odpovědi zmiň, zda ADR bylo vytvořeno/aktualizováno, nebo proč nebylo potřeba.
