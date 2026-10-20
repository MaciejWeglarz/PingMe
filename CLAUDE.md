# PingMe — instrukcje dla Claude

## O projekcie
PingMe to prywatna aplikacja na Androida, która przypomina o codziennych czynnościach (mycie zębów, suplementy, leki itp.).
Korzysta z niej jedna osoba (autor) na własnym telefonie. Aplikacja nie będzie publikowana w Sklepie Play.
Nazwa nawiązuje do „Penguin”. Motyw przewodni to pingwinek trzymający mózg.

## Twarde założenia
- Działa w 100% lokalnie: bez serwera, konta, internetu i analityki. Nie dodawaj uprawnienia INTERNET.
- Interfejs po polsku.
- Przypomnienia muszą przychodzić o czasie, także w trybie Doze i po restarcie telefonu.

## Stack
- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- MVVM: ViewModel + StateFlow
- Room (baza danych), Hilt (DI)
- AlarmManager do przypomnień. Nie używaj WorkManagera, bo nie gwarantuje dokładnej godziny.
- minSdk 26, targetSdk: najnowszy stabilny
- Jeden moduł Gradle, pakiety: `data`, `domain`, `ui`
- Nie dodawaj innych bibliotek bez pytania.

## Funkcje

### Zadanie
Każde zadanie ma:
- nazwę, np. „Mycie zębów rano”
- godzinę przypomnienia
- opcjonalny deadline (godzinę). Jeśli do tej godziny zadanie nie zostanie odhaczone, włącza się alarm.
- datę startu, czyli od kiedy obowiązuje
- harmonogram, jeden z wariantów:
  - jednorazowo: konkretna data, np. 25.10.2026
  - codziennie
  - co tydzień: w ten sam dzień tygodnia co data startu
  - co miesiąc: w ten sam dzień miesiąca. Jeśli miesiąc jest krótszy, w ostatni dzień miesiąca.
  - własny: wybrane dni tygodnia, np. wtorek i środa

### Przypomnienia
- O godzinie przypomnienia wyślij powiadomienie push z dwoma przyciskami:
  - „Zrobione”: odhacza zadanie bez otwierania aplikacji
  - „Przypomnij za 10 min”: ponawia powiadomienie po 10 minutach
- Jeśli po deadline zadanie nie jest odhaczone, włącz alarm: dźwięk i ekran pełnoekranowy z przyciskami „Zrobione” i „Wyłącz”.
- Po restarcie telefonu zaplanuj wszystkie alarmy od nowa (BOOT_COMPLETED).

### Uprawnienia
Proś tylko o uprawnienia, których aplikacja potrzebuje. Zanim o nie poprosisz, wyjaśnij w UI, po co są:
- POST_NOTIFICATIONS (Android 13+)
- dokładne alarmy: USE_EXACT_ALARM (Android 13+) oraz SCHEDULE_EXACT_ALARM z maxSdkVersion 32 dla starszych wersji
- RECEIVE_BOOT_COMPLETED
- USE_FULL_SCREEN_INTENT dla ekranu alarmu. Na Androidzie 14+ może wymagać ręcznego włączenia w ustawieniach. Sprawdź to i pokaż użytkownikowi, gdzie je włączyć.
- prośba o wyłączenie optymalizacji baterii dla aplikacji, żeby system nie blokował alarmów

### Ekrany
1. **Dziś**: zadania zaplanowane na dzisiaj, z możliwością odhaczenia. Zadanie z przyszłą datą pojawia się dopiero w swoim dniu.
2. **Dodaj / edytuj zadanie**: formularz ze wszystkimi polami zadania.
3. **Wszystkie zadania**: lista do edycji i usuwania.
4. **Statystyki**: widok miesięczny. Dla każdego zadania pokazuje, ile razy w wybranym miesiącu było pominięte, a ile razy zrobione.

### Seria (streak)
- Dla każdego zadania cyklicznego licz serię: ile kolejnych zaplanowanych wystąpień zostało zrobionych z rzędu. Licz wystąpienia, nie dni kalendarzowe, bo zadanie tygodniowe też ma mieć serię.
- Pominięcie zeruje serię. Zadania jednorazowe nie mają serii.
- Pokazuj serię przy zadaniu na ekranie „Dziś” i w statystykach.
- Maskotka reaguje na stan dnia: cieszy się, gdy wszystko jest zrobione, a smuci przy pominięciach.

### Kopia zapasowa
- Eksport całej bazy (zadania i historia) do pliku oraz import z takiego pliku.
- Użytkownik wybiera miejsce zapisu i odczytu przez systemowy wybór plików (Storage Access Framework). Nie proś o uprawnienia do pamięci.
- Przed importem ostrzeż, że obecne dane zostaną zastąpione.

### Model danych
- Przechowuj osobno definicję zadania i historię wystąpień. Wystąpienie to zadanie w danym dniu ze statusem „zrobione” albo „pominięte”.
- Wystąpienie jest pominięte, jeśli nie zostało odhaczone do końca swojego dnia.
- Statystyki muszą być poprawne także za dni, w których aplikacja nie była otwierana.

### Wygląd
- Maskotka to pingwinek trzymający mózg. Ikonę aplikacji i maskotkę (np. na pustym ekranie „Dziś”) zrób jako proste grafiki wektorowe (VectorDrawable).
- Maskotka ma być oryginalna. Nie wzoruj się na znanych postaciach, np. Tux.
- Kolory nawiązują do pingwina (granat, biel), z różowym akcentem (mózg).
- Material 3, tryb jasny i ciemny.

## Kolejność prac
Rób jeden etap naraz. Po każdym etapie aplikacja ma się budować i działać.
1. Model danych, Room i logika harmonogramu, z testami jednostkowymi
2. Ekran „Dziś” i dodawanie zadań
3. Powiadomienia z przyciskami „Zrobione” i „Przypomnij za 10 min”, alarmy, uprawnienia, obsługa restartu telefonu
4. Lista wszystkich zadań, edycja, usuwanie
5. Statystyki miesięczne i serie
6. Eksport i import bazy
7. Wygląd: maskotka (z reakcjami), ikona, kolory

## Testy
Logika „czy zadanie wypada w danym dniu” musi mieć testy jednostkowe. Szczególnie przetestuj:
- koniec miesiąca (dni 29–31)
- rok przestępny
- zadania jednorazowe w przyszłości
- własne dni tygodnia

## Komendy
- Budowanie: `./gradlew assembleDebug`
- Testy: `./gradlew test`
- Instalacja na podłączonym telefonie: `./gradlew installDebug`
- W PowerShellu zamiast `./gradlew` użyj `.\gradlew.bat`.

## Jak ze mną pracować
- Odpowiadaj po polsku.
- Jestem junior Android developerem. Po każdej zmianie wyjaśnij w 2–3 zdaniach, co zrobiłeś i dlaczego. Nieoczywiste pojęcia tłumacz jednym zdaniem.
- Przed większą funkcją pokaż plan i poczekaj na akceptację.
- Po każdym działającym etapie zaproponuj commit w git (bez push).

---

# Zasady ogólne

<!-- Źródło: https://github.com/multica-ai/andrej-karpathy-skills (licencja MIT) -->

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.
