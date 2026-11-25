# AbyssDynasty — Fabric 26.3 — V1 TEST

Mod server-side experimental pentru AbyssMC. Java 25 / Fabric Loader 0.19.5 / Fabric API 0.166.2+26.3. Surse, nu JAR compilat.

## Instalare
1. Extrage arhiva si incarca continutul (inclusiv `.github`) in radacina unui repository GitHub nou.
2. Actions -> Build AbyssDynasty 26.3 -> Run workflow (sau push in main).
3. Descarca artefactul AbyssDynasty-Fabric-26.3, extrage JAR-ul si instaleaza-l in mods pe serverul de test.
4. Nu este necesara instalarea pe client. Foloseste o copie de test si backup.

## Comenzi pentru jucatori
- /dynasty sau /dy — GUI principal.
- /dynasty create <nume> — creeaza dinastie (3-20 caractere, fara spatii).
- /dynasty invite <player> — invita un jucator online.
- /dynasty accept — accepta ultima invitatie valida.
- /dynasty kick <player> — elimina membru.
- /dynasty promote <player> — co-lider (doar liderul).
- /dynasty demote <player> — membru (doar liderul).
- /dynasty transfer <player> — transfera conducerea.
- /dynasty leave — paraseste dinastia (nu liderul).
- /dynasty disband confirm — sterge dinastia si punctele (doar liderul).
- /dynasty top — clasament global, click pentru lista de membri.
- /dynasty missions — categorii si challenge-uri.
- /dynasty members — lista membrilor.
- /dynasty history — challenge-uri finalizate, click pentru contributii.

## Administratie
OP/consola sau `/lp group admin permission set abyssdynasty.admin true`.
- /dynasty editor — GUI; click pe categorie, apoi stanga +100 XP / dreapta -100 XP; emerald adauga un prag mai mare.
- /dynasty editor add <category> <goal> <xp> — adauga challenge sau categorie noua.
- /dynasty reload — reincarca configuratiile (nu datele de joc).
- /dynasty credit <player> <category> <amount> — progres manual sau integrare din consola pentru jucatori online. Nu oferi acces normal.
- /dynasty scoreboard on — activeaza scoreboard GLOBAL top 10; poate inlocui sidebar-ul altui mod. Nu activa daca ai deja un scoreboard pe server.
- /dynasty scoreboard off — elimina obiectivul abyssdynasty; nu restaureaza sidebar-ul anterior.
- /dynasty hologram set — holograma TOP 10 la pozitia ta; se actualizeaza la 30 secunde cand sunt jucatori aproape.
- /dynasty hologram remove — elimina holograma (in chunk-uri descarcate, curatarea are loc cand se incarca din nou).

## Configuratii
Se genereaza config/abyssdynasty/config.yml, message.yml si missions.yml. YAML plat, cate o cheie si valoare intre ghilimele duble, fara liste, indentare sau comentarii inline. `data.json` se genereaza la prima modificare persistenta; nu il edita cu serverul pornit.

message.yml: prefixul HEX cerut, mesaje, nume categorii, texte GUI, holograma. Exemplu `category-build: "Constructii"`.
config.yml: max-members (10), xp-per-level (10000), invite-seconds (120), track-placed-positions (true), save-ticks (600). Datele se salveaza periodic si la oprire normala; crash-ul poate pierde progresul de la ultima salvare.
missions.yml: `build.1000000: "50000"` (categorie.prag: recompensa). 15 praguri fiecare pentru build/mine/hunt, 5 pentru fiecare alta categorie. Cheile sunt ID-uri permanente; nu schimba ID-uri deja finalizate. Daca modifici recompensa unui challenge finalizat, nu se recalculeaza XP-ul castigat. Daca adaugi un prag sub progresul actual, se acorda la urmatorul eveniment eligibil (snapshot-ul reprezinta progresul disponibil, nu istoricul exact anterior editarii).

## Progres
- build: plasari BlockItem reusite; creative/spectator nu castiga XP. Mixin experimental, unele blocuri speciale pot sa nu fie numarate corect.
- mine: blocuri sparte cu succes, exclude pozitiile cu plasari inregistrate de mod (nu poate identifica blocurile plasate inaintea instalarii).
- hunt: monstri omorati de jucator (include spawner in V1).
- farm: culturi mature CropBlock recoltate.
- fish: statistica vanilla fish_caught, esantionare la o secunda.
- activity: minute acumulate din secunde in care pozitia de bloc se schimba; anti-AFK simplu, nu detecteaza macro-uri.
- vote, crate, explore si categorii noi: credit prin comanda/integrari, fara hook automat in V1. De exemplu consola `dynasty credit Zixtyoff vote 1`.
- Progres cumulativ. XP este separat de vanilla si se acorda o singura data per prag. Contributia membrilor care pleaca ramane. Snapshot la finalizare, afisat din istoric. Nivel = 1 + XP / xp-per-level.
- Anti-farm partial: cu track-placed-positions=true, o pozitie este creditata pentru constructii cel mult o data la nivel global si nu mai da progres minerit. Evidenta poate creste mult pe servere mari. Nu este protectie completa anti-abuz. Cu false se dezactiveaza aceasta verificare.

## Test rapid
1. Cu 2 conturi: creeaza dinastie, invita, accepta; verifica rolurile.
2. Admin: `/dynasty editor add build 3 100` si `/dynasty editor add mine 3 100`.
3. In survival, plaseaza 3 blocuri distincte cu ambele conturi; verifica XP, topul, contributiile. Sparge aceleasi blocuri; nu trebuie sa dea progres minerit. Sparge 3 blocuri naturale.
4. Testeaza un mob, o cultura matura, pescuit, si `/dynasty credit <player> vote 50`.
5. Verifica top, history, restart si reload. Testeaza ca GUI nu permite scoaterea itemelor.
6. Scoreboard/holograma sunt optionale; verifica separat, pe server de test.

## Limite V1
Necompilat aici (Java 21, fara dependintele Fabric 26.3); verificare de sintaxa preliminara, build si teste reale prin GitHub Actions/server. Sezoanele, Hall of Fame, recompensele de sezon, home si hook-urile directe de vot/crate nu sunt implementate. GUI editor adauga urmatorul prag si editeaza XP; pentru categorii noi si valori exacte foloseste comanda sau fisierul. Capetele sunt iteme player_head generice, nu skin-uri personalizate. Hologramele si scoreboard-ul sunt experimentale.
