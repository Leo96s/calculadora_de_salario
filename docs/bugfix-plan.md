# Plano de correção — Calculadora de Salário

Este plano deriva da análise + testes em emulador feitos em 05/08/2026 (ver
resumo no fim de cada item). Está organizado por fases, da mais bloqueante
para a mais cosmética, para se ir resolvendo aos poucos sem ter de fazer
tudo de uma vez.

**Como usar:** cada item é uma unidade de trabalho pequena e testável
isoladamente. Ao resolver um item, marca a checkbox, e faz um commit
próprio para esse item (não misturar vários itens no mesmo commit). Não
avançar para a Fase 2 antes da Fase 1 estar fechada — a Fase 1 é o que
impede a app de ser usável.

---

## Fase 0 — Segurança crítica

### [x] 0.1 Firestore sem nenhuma regra de acesso — RESOLVIDO

**Problema:** descoberto ao investigar a causa raiz do item 1.2. As regras
de segurança do Firestore do projeto `calculadora-salario-6c4b8` estavam
completamente abertas — confirmado com um pedido HTTP sem autenticação à
coleção `users`, que devolveu `200 OK` com documentos reais (email,
preço/hora de contas reais). Qualquer pessoa na internet conseguia ler (e
muito provavelmente escrever) todos os perfis e registos salariais de
todos os utilizadores.

**Correção aplicada:** criado `firestore.rules` (+ `firebase.json` mínimo)
restringindo `users/{userId}` e `users/{userId}/salary_records/{recordId}`
a `request.auth != null && request.auth.uid == userId`, e negando tudo o
resto por omissão. Deploy feito via `firebase deploy --only
firestore:rules --project calculadora-salario-6c4b8`.

**Validado:** pedido anónimo à coleção `users` passou a devolver `403
PERMISSION_DENIED`. Testado no emulador com conta nova: signup, leitura do
perfil e gravação local de um registo continuam a funcionar normalmente
com utilizador autenticado.

**Efeito colateral esperado (não é regressão):** a sincronização cloud dos
registos salariais (`FirebaseManager.saveRecordToCloud`) passou a falhar
com `PERMISSION_DENIED` — porque grava em `users/{username}/...` em vez
de `users/{uid}/...` (o bug já descrito no item 2.1). Antes, isto
"funcionava" só porque não havia nenhuma regra a impedir escrever no sítio
errado; a base de dados local não é afetada. Resolver o item 2.1 restaura
a sincronização, agora da forma correta.

---

## Fase 1 — Bloqueantes (a app não funciona sem isto)

### [x] 1.1 Guardar registo mensal não faz nada — RESOLVIDO

**Problema:** `hourlyRateInput` (`app/src/main/java/com/example/ui/MainViewModel.kt:40`)
nunca era preenchido por nenhum fluxo real. `saveCurrentCalculation()`
lia esse valor, caía em `0.0`, e recusava gravar — e `RegisterScreen.kt`
nunca observava `uiMessage`, por isso a falha era completamente silenciosa
(sem navegação, sem erro visível).

**Correção aplicada:**
- `saveCurrentCalculation()` passou a usar `user.hourlyRate` (o perfil já
  carregado) em vez de `hourlyRateInput.value`.
- Adicionado `SnackbarHost` + observer de `viewModel.uiMessage` em
  `RegisterScreen.kt`, igual ao padrão já existente em `DashboardScreen.kt`.

**Bug secundário descoberto e corrigido durante a validação:** ao
desbloquear o caminho de gravação, apareceu um `SQLiteConstraintException:
FOREIGN KEY constraint failed` — a tabela local `users` (Room) nunca era
populada para utilizadores autenticados via Firebase (só o Firestore tinha
o perfil), e `SalaryRecord.userId` tem uma FK para `User.uid`. Corrigido
adicionando `AppDao.upsertUser`/`AppRepository.upsertUser`, chamado a
partir de `MainViewModel.refreshCurrentUser()` (usado por `initSession()`
e `loadCurrentUser()`) para manter a linha local sincronizada com o perfil
Firebase.

**Segundo bug secundário:** a primeira versão do upsert usava
`@Insert(onConflict = OnConflictStrategy.REPLACE)`, que o Room implementa
como DELETE+INSERT — como `SalaryRecord` tem `onDelete = CASCADE` na FK,
isto apagava em catadupa os registos salariais do utilizador sempre que o
perfil recarregava (o que acontece a cada entrada no Dashboard, incluindo
logo a seguir a gravar um registo). Corrigido trocando para `@Upsert`
(update in-place, sem apagar a linha).

**Validado no emulador:** conta nova → gravar turnos → snackbar de
sucesso → aparece no histórico e no gráfico → sobrevive a fechar e reabrir
a app.

**Nota sobre testes automatizados:** não foi possível escrever um teste
Robolectric para este fluxo porque instanciar `MainViewModel` sob
Robolectric ainda crasha com `FirebaseApp not initialized` — depende de
resolver o item 3.1 primeiro.

---

### [x] 1.2 Login por nome de utilizador nunca funciona — RESOLVIDO

**Problema:** `AuthManager.loginWithEmail` (`AuthManager.kt:129-162`) fazia
uma query Firestore a `users` por `username` antes de autenticar. Com as
regras do Firestore agora corretamente fechadas (item 0.1), esta query
nunca pode funcionar — é um pedido não autenticado, e as regras exigem
`request.auth != null`. Antes de fechar as regras já falhava por outra
razão não totalmente esclarecida; com as regras fechadas a causa fica
definitiva e sem solução sem infraestrutura nova.

**Decisão tomada:** remover a opção de login por username, aceitar só
email. Alternativa de mover a resolução para uma Cloud Function foi
recusada — exigiria infraestrutura nova (pasta `functions/`, Node.js,
deploy separado) e mudar o projeto para o plano Blaze (pay-as-you-go).

**Correção aplicada:**
- `AuthManager.loginWithEmail(email, password)` — removida a resolução
  username→email e a query Firestore associada.
- `MainViewModel.login(email, password)` — validação de formato de email
  igual à já existente em `register()`; mensagens de erro atualizadas.
- `LoginScreen.kt` — campo passa a chamar-se só "Email"
  (`testTag("login_email_input")`), com `KeyboardType.Email`.

**Validado no emulador:** login com email e password corretos → entra no
dashboard sem erro.

**Teste:** criar conta, logout, login com o username escolhido no signup.

---

### [x] 1.3 Mensagens de erro ficam coladas ao trocar de ecrã — RESOLVIDO

**Problema:** `authError`/`authSuccess` em `MainViewModel.kt` são estado
partilhado a nível de app, nunca limpo ao entrar num ecrã novo. Um erro do
Login ficava visível ao navegar para o SignUp (e vice-versa).

**Correção aplicada:** em `LoginScreen.kt` e `SignUpScreen.kt`, adicionado
`LaunchedEffect(Unit) { viewModel.clearAuthStates() }` à entrada do
composable.

**Validado no emulador:** no Login, submeti formulário vazio (erro
aparece) → naveguei para "Criar Conta" → confirmado que o erro do Login
já não aparece lá.

---

### [x] 1.4 Perfil mostra "Utilizador" genérico depois de login — RESOLVIDO

**Problema:** depois de um login (não signup) bem-sucedido, o dashboard
mostrava "Olá, Utilizador!" em vez do nome real, sem erro no logcat.
Confirmado: não era corrida nem bug de rede — `currentUser == null`
significava tanto "ainda a carregar" como "pedido concluído, sem perfil",
e a UI usava sempre o fallback "Utilizador" para os dois casos.

**Correção aplicada:**
- Novo `MainViewModel.isProfileLoading: StateFlow<Boolean>`, `true` até
  `refreshCurrentUser()` terminar (sucesso ou não).
- `DashboardScreen` passou a ter 3 estados em vez de 2: a carregar
  (`ProfileLoadingCard`, spinner) → perfil encontrado (`ProfileWelcomeCard`
  com os dados reais) → perfil não encontrado (`ProfileWelcomeCard` com o
  fallback "Utilizador", só depois do pedido terminar).

**Bug descoberto ao validar:** a primeira versão só olhava para
`currentUser == null` para mostrar o loading, sem um terceiro estado —
isso fazia com que uma conta cujo perfil Firestore não existisse (ex.
apagado manualmente) ficasse **presa no spinner para sempre**, porque o
pedido terminava com sucesso mas `user` continuava `null`. Cheguei a
suspeitar de um hang genuíno do Firebase/Play Services (cheguei a reiniciar
o emulador todo a investigar) antes de perceber que a conta de teste usada
não tinha documento Firestore (apagado numa limpeza anterior desta
sessão). Corrigido com o `isProfileLoading` acima.

**Validado no emulador:** conta com perfil intacto → loading breve e
depois nome real; conta sem documento Firestore → loading breve e depois
o fallback "Utilizador" (não fica presa).

---

## Fase 2 — Segurança / dados

### [x] 2.1 Sincronização cloud grava no caminho errado do Firestore — RESOLVIDO

**Problema:** o perfil do utilizador vive em `users/{uid}`
(`AuthManager.kt:79-83`), mas os registos salariais eram gravados/lidos em
`users/{username}/salary_records/...`. Com as regras do Firestore
corrigidas (item 0.1), isto passou de "inseguro" a "sempre falha com
PERMISSION_DENIED" — confirmado no emulador antes desta correção.

**Correção aplicada:** trocado `user.username` por `user.uid` nas
chamadas de `saveCurrentCalculation()`, `deleteRecord()` e
`triggerSyncSimulation()`. `syncCloudRecords(userIdStr: String)` foi
renomeado para `syncCloudRecords(uid: String)` e deixou de fazer uma
lookup local por `getUserByUsername(userIdStr)` — usa o `uid` recebido
diretamente (o lookup só servia para obter o `.uid`, que já vinha por
parâmetro).

**Validado no emulador:** conta nova → guardar registo → logcat confirma
`Record saved successfully: <uid>_2026-08` (documento chaveado pelo uid,
sem `PERMISSION_DENIED`) → botão de sincronizar não reporta erros.

---

### [x] 2.2 `.gitignore` não cobre google-services.json nem a keystore — RESOLVIDO

**Problema:** `app/google-services.json` e `calculadora_salario-keystore`
não estavam listados em nenhum `.gitignore` (só `debug.keystore` estava).

**Correção aplicada:** adicionados `*.keystore`, `*-keystore`, `*.jks` e
`google-services.json` ao `.gitignore` da raiz (o nome exato
`calculadora_salario-keystore` não tem extensão `.keystore`, daí o padrão
`*-keystore` à parte).

**Validado:** `git status` já não lista nenhum dos dois ficheiros como
untracked.

---

### [x] 2.3 `registerWithEmail` sem rollback se a escrita do perfil falhar — RESOLVIDO (não testado dinamicamente)

**Problema:** `AuthManager.kt:93-127` criava o utilizador no Firebase Auth
e só depois escrevia o perfil no Firestore. Se a escrita falhasse, a conta
Auth ficava órfã (sem perfil) e o próximo registo com o mesmo email falhava
com "email already in use", sem forma óbvia de recuperar.

**Correção aplicada (opção a):** se a escrita do perfil no Firestore falhar
depois de a conta Auth já ter sido criada, apaga-se a conta Auth
(`user.delete()`) antes de devolver falha — a conta deixa de ficar órfã.

**Não foi possível validar dinamicamente:** tentei simular a falha
temporariamente alterando `firestore.rules` para negar o `create` do
perfil (determinístico, sem depender de timing de rede) — o `firebase
deploy` foi bloqueado pelo classificador de segurança do Claude Code por
alterar um recurso de produção sensível (correto, não tentei contornar).
A alteração nunca chegou a ser publicada; o ficheiro local foi revertido
de imediato. Simular por desligar a rede a meio do registo (a alternativa
sugerida original) também não é fiável de automatizar por timing.
Validado só por leitura de código + compilação; recomenda-se um teste
manual (desligar Wi-Fi do dispositivo mesmo depois do ecrã mostrar
"a registar" mas antes de "sucesso") antes de confiar cegamente nisto.

---

## Fase 3 — Qualidade / manutenção

### [x] 3.1 Testes automatizados não conseguem tocar código Firebase — RESOLVIDO

**Problema:** `./gradlew testDebugUnitTest` — 2 de 4 testes falhavam com
`IllegalStateException: Default FirebaseApp is not initialized`, porque
`FirebaseManager.initialize()` (`FirebaseManager.kt:17-19`) conta com a
auto-inicialização via `ContentProvider`, que não corre no sandbox do
Robolectric.

**Correção aplicada (opção mais simples do plano):**
- `GreetingScreenshotTest` e `ExampleRobolectricTest`: `@Before` a
  inicializar `FirebaseApp` com `FirebaseOptions` fictícias (não é preciso
  um projeto real — só evita o `IllegalStateException` ao construir
  `FirebaseAuth`/`FirebaseFirestore`).
- `ExampleRobolectricTest.testViewModelAuthAndCalculationFlow` foi
  renomeado para `testViewModelDefaultCalculationState` e deixou de
  chamar `viewModel.register(...)` — essa chamada acaba por tentar uma
  ligação de rede real ao Firebase Auth (não isolado atrás de nenhuma
  interface), o que não é apropriado nem fiável num teste unitário.
  Ficou só a verificar o estado inicial do ViewModel.
- **Não foi feita** a alternativa mais completa (isolar
  `AuthManager`/`FirebaseManager` atrás de uma interface injetável e usar
  um fake nos testes) — ficaria com cobertura real da lógica de
  registo/login sem tocar rede, mas é uma mudança maior; qualquer teste
  futuro que precise de exercitar `register()`/`login()` vai precisar
  disto.

**Validado:** `./gradlew testDebugUnitTest` → 4 de 4 testes passam, 0
falhas (confirmado nos ficheiros XML de resultado).

---

### [x] 3.2 Remover código morto da migração para Firebase — RESOLVIDO

**Problema:** `AppRepository`/`AppDao` (`registerUser`, `authenticateUser`,
`getUserById`, `getUserByUsername`, `insertUser`, `hashPassword`) e
`MainViewModel.loginWithGoogle(email, displayName)` nunca eram chamados
pela UI (confirmado por grep) — remanescentes do sistema de auth local
anterior ao Firebase. `AppDao.getUserById(Int)` comparava com uma coluna
`uid` que é `String` — nunca poderia funcionar mesmo se fosse usado.

**Correção aplicada:** removidas todas as funções acima. Também removido
`MainViewModel.hourlyRateInput` — ficou órfão depois de 1) o fix do item
1.1 (que passou a usar `user.hourlyRate` diretamente) e 2) esta remoção
(era o único outro sítio que ainda o escrevia, dentro do `loginWithGoogle`
agora removido). `User.passwordHash` e o índice único em `username`
ficaram intactos — são campos de entidade/schema, não funções não usadas,
fora do âmbito deste item.

**Validado:** `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` →
sucesso, 4 de 4 testes continuam a passar.

---

### [x] 3.3 README.md e metadata.json desatualizados — RESOLVIDO

**Problema:** era o boilerplate genérico "AI Studio" — falava de
`GEMINI_API_KEY`, de uma linha `signingConfig = signingConfigs.getByName
("debugConfig")` que já não existia no `build.gradle.kts`, e não
documentava o setup real (Firebase: onde pôr `google-services.json`, como
ativar Auth/Firestore; keystore de release: variáveis de ambiente
`KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD`).

**Correção aplicada:** README reescrito com os passos reais — setup do
Firebase (Authentication Email/Password + Google, Firestore, deploy de
`firestore.rules`, onde colocar `google-services.json`), esclarecimento
de que o `.env`/`GEMINI_API_KEY` é um resquício do template original e
não é usado por esta app, e as variáveis de ambiente corretas para um
build de release assinado. `metadata.json`:
`majorCapabilities` deixou de referir `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`
(não se aplica, `firebase-ai` está comentado nas dependências).

**Validado:** conteúdo cruzado com o `build.gradle.kts`, `.gitignore` e
`firestore.rules` reais do repo; `./gradlew assembleDebug` continua a
funcionar depois da alteração. Não foi possível testar "de um ambiente
limpo" à letra (exigiria um checkout novo sem `google-services.json` nem
histórico de sessão) — validação feita por revisão cruzada do conteúdo.

---

## Fase 4 — Encontrados na reteste pós-Fase-3 (2026-08-05)

### [x] 4.1 Editar o preço/hora padrão não persiste — RESOLVIDO

**Problema:** `MainViewModel.updateDefaultRate()` só gravava o novo valor
na tabela local `users` (Room), nunca no Firestore. Como
`refreshCurrentUser()` volta a ler o perfil do Firestore e sobrescreve a
tabela local a cada entrada no Dashboard (incluindo depois de reiniciar a
app), a edição parecia funcionar (mensagem de sucesso, UI atualizada) mas
era sempre silenciosamente desfeita a seguir.

**Correção aplicada:** novo `AuthManager.updateHourlyRate(uid, rate)`, que
escreve o campo no Firestore (`update`, não reescreve o documento
inteiro). `updateDefaultRate()` só atualiza o estado local/UI se essa
escrita tiver sucesso; caso contrário mostra um erro em vez de fingir que
funcionou.

**Validado no emulador:** editar o preço/hora → mensagem de sucesso →
fechar e reabrir a app → valor novo mantido (antes da correção, revertia
para o valor antigo).

### [x] 4.2 `LoginScreen` não mostra a mensagem de recuperação de password — RESOLVIDO

**Problema:** `LoginScreen.kt` nunca observava `viewModel.uiMessage` nem
tinha `SnackbarHost` nenhum — só `DashboardScreen` e (desde o item 1.1)
`RegisterScreen` tinham isto. Qualquer mensagem definida por
`recoverPassword()` (sucesso ou o "pedido enviado" genérico do catch)
nunca chegava a aparecer; o diálogo simplesmente fechava sem feedback
nenhum.

**Correção aplicada:** adicionado `SnackbarHost` + observer de
`uiMessage` ao `LoginScreen`, mesmo padrão do `RegisterScreen`/
`DashboardScreen`.

**Validado no emulador:** "Esqueceu-se da senha?" → inserir email →
"Enviar Link" → snackbar "Email de recuperação enviado com sucesso
para ..." aparece (a chamada real ao Firebase demorou ~4-5s a
responder — se testares isto, espera o suficiente antes de concluir que
não aparece nada).

### [x] 4.3 Teclado tapa campos/botão em "Registar Turnos" e engole toques — RESOLVIDO

**Problema:** `RegisterScreen.kt` nunca chamava `imePadding()` nem definia
`windowSoftInputMode`. Com Dias Normais + Domingos + Feriados preenchidos,
o conteúdo do formulário passava a ser mais alto do que o espaço visível
quando o teclado abre. Como o `Scaffold` não reservava espaço para o
teclado, os campos/botão que ficavam por baixo dele não desapareciam do
ecrã visualmente cobertos — o Android entregava o toque ao teclado, que
por acaso tinha uma tecla numérica naquela posição. O resultado, em vez de
"não dá para tocar", era **reescrever silenciosamente o valor do campo que
ainda tinha foco** (ex.: tentar tocar em "Feriados" com "Domingos" focado
inseria dígitos a mais em "Domingos" sem qualquer erro visível).

**Como foi encontrado:** ao testar dinamicamente Domingos+Feriados em
conjunto, os valores dos campos ficavam corrompidos de forma
aparentemente aleatória entre toques. A confusão inicial (culpa do script
de teste vs. bug real da app) só ficou resolvida ao confirmar, por
`uiautomator dump`, que o foco *nunca* saía do campo "Domingos" mesmo
depois de tocar exatamente nas coordenadas do campo "Feriados" — e ao
confirmar por captura de ecrã que o teclado cobria fisicamente esse campo.

**Correção aplicada:** `Modifier.imePadding()` no `Scaffold` de
`RegisterScreen.kt`, para que o conteúdo encolha e passe a ter scroll
disponível quando o teclado abre, tal como as restantes áreas com forms.

**Validado no emulador:** reconstruído o APK, reinstalado, reproduzido o
mesmo teste (Dias Normais 22×8h + Domingos 2×8h + Feriados 1×8h com o
teclado sempre aberto entre campos) — o formulário agora tem scroll
disponível com o teclado aberto, o toque no campo Feriados foca-o
corretamente, o total calculado (5162.00€) bate certo com a soma manual
(3916 + 712 + 534), e o registo grava e aparece no histórico/gráfico do
Dashboard sem erros no logcat.

---

## Notas

- Cada item marcado como resolvido deve ter passado pelo teste descrito
  antes de ser marcado — não marcar por "deve estar bem".
- 2.1 está formalmente dependente de 1.1; os restantes itens são
  independentes entre si e podem ser feitos pela ordem que fizer mais
  sentido, respeitando a ordem das fases (bloqueante → segurança →
  qualidade).
- Este documento não substitui o report completo (achados extra, contexto
  de cada bug) — serve como lista de trabalho acionável derivada dele.
