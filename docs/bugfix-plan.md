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

### [ ] 2.1 Sincronização cloud grava no caminho errado do Firestore

**Problema:** o perfil do utilizador vive em `users/{uid}`
(`AuthManager.kt:79-83`), mas os registos salariais são gravados/lidos em
`users/{username}/salary_records/...`
(`MainViewModel.kt:481,496,596` chamam `FirebaseManager.saveRecordToCloud`
/`deleteRecordFromCloud`/`fetchRecordsFromCloud` com `user.username`).
Isto ou parte a sincronização (se as regras exigirem `auth.uid ==
userId`), ou permite a dois utilizadores com o mesmo username lerem/
escreverem os registos um do outro.

**Correção:** trocar `user.username` por `user.uid` nas três chamadas em
`MainViewModel.kt`.

**Teste:** só depois de 1.1 estar resolvido — guardar um registo, verificar
na consola Firestore que aparece em `users/{uid}/salary_records/...`, e
que sincroniza corretamente noutro dispositivo/sessão com o mesmo login.

**Depende de:** 1.1 (sem isso não há registos para testar a sincronização).

---

### [ ] 2.2 `.gitignore` não cobre google-services.json nem a keystore

**Problema:** `app/google-services.json` e `calculadora_salario-keystore`
não estão listados em nenhum `.gitignore` (só `debug.keystore` está).

**Correção:** adicionar ambos os nomes ao `.gitignore` da raiz.

**Teste:** `git status` depois de os ficheiros existirem no disco — não
devem aparecer como untracked/stageable.

---

### [ ] 2.3 `registerWithEmail` sem rollback se a escrita do perfil falhar

**Problema:** `AuthManager.kt:93-127` cria o utilizador no Firebase Auth e
só depois escreve o perfil no Firestore. Se a escrita falhar, a conta Auth
fica órfã (sem perfil) e o próximo registo com o mesmo email falha com
"email already in use", sem forma óbvia de recuperar.

**Correção:** decidir uma estratégia — (a) apagar a conta Auth recém-criada
se a escrita do perfil falhar (`result.user?.delete()`), ou (b) detetar
"conta Auth existe mas sem perfil" no login e reoferecer completar o
registo. (a) é mais simples de implementar primeiro.

**Teste:** simular falha na escrita do Firestore (ex.: desligar a rede a
meio do registo) e confirmar que a conta não fica órfã.

---

## Fase 3 — Qualidade / manutenção

### [ ] 3.1 Testes automatizados não conseguem tocar código Firebase

**Problema:** `./gradlew testDebugUnitTest` — 2 de 4 testes falham com
`IllegalStateException: Default FirebaseApp is not initialized`, porque
`FirebaseManager.initialize()` (`FirebaseManager.kt:17-19`) conta com a
auto-inicialização via `ContentProvider`, que não corre no sandbox do
Robolectric.

**Correção:** inicializar o `FirebaseApp` explicitamente num `@Before` dos
testes que tocam `AuthManager`/`FirebaseManager` (com `FirebaseOptions` de
teste), ou isolar `AuthManager`/`FirebaseManager` atrás de uma interface
injetável para poder substituir por um fake nos testes unitários.

**Teste:** `./gradlew testDebugUnitTest` → 0 falhas.

---

### [ ] 3.2 Remover código morto da migração para Firebase

**Problema:** `AppRepository`/`AppDao` (`registerUser`, `authenticateUser`,
`getUserById`) e `MainViewModel.loginWithGoogle(email, displayName)` nunca
são chamados pela UI (confirmado por grep) — remanescentes do sistema de
auth local anterior ao Firebase. `AppDao.getUserById(Int)` compara com uma
coluna `uid` que é `String` — nunca poderia funcionar mesmo se fosse
usado.

**Correção:** apagar essas funções/métodos não usados. Confirmar por grep
antes de apagar que continuam mesmo sem chamadores.

**Teste:** projeto continua a compilar depois da remoção; testes existentes
continuam a passar.

---

### [ ] 3.3 README.md e metadata.json desatualizados

**Problema:** ainda é o boilerplate genérico "AI Studio" — fala de
`GEMINI_API_KEY`, de uma linha `signingConfig = signingConfigs.getByName
("debugConfig")` que já não existe no `build.gradle.kts`, e não documenta
o setup real (Firebase: onde pôr `google-services.json`, como ativar
Auth/Firestore; keystore de release: variáveis de ambiente
`KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD`).

**Correção:** reescrever o README com os passos reais de setup deste
projeto. Rever `metadata.json` e remover/corrigir referências à capacidade
Gemini que não se aplicam.

**Teste:** seguir o README do zero num ambiente limpo e confirmar que a
app corre.

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
