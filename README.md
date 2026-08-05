# Calculadora de Salário

App Android nativo (Kotlin + Jetpack Compose) para calcular o salário
mensal com base nas horas normais, domingos a dobrar e feriados a
triplicar, com histórico de meses e conta de utilizador (Firebase Auth +
Firestore).

## Pré-requisitos

- [Android Studio](https://developer.android.com/studio) (inclui o JDK
  necessário)
- Acesso ao projeto Firebase da app (ou um projeto Firebase próprio para
  desenvolvimento — ver abaixo)

## Configurar o Firebase

A app depende de um projeto Firebase real com **Authentication**
(fornecedores Email/Password e Google) e **Cloud Firestore** ativados.

1. Cria ou abre o projeto na [consola Firebase](https://console.firebase.google.com/).
2. Regista uma app Android com o `applicationId`
   `com.salariocalculator.pwtqzs` e descarrega o `google-services.json`
   gerado.
3. Coloca esse ficheiro em `app/google-services.json` (está no
   `.gitignore` — nunca o commites).
4. Em **Authentication → Sign-in method**, ativa **Email/Password** e
   **Google**.
5. Em **Firestore Database**, cria a base de dados (modo produção) e
   publica as regras deste repositório:

   ```
   firebase deploy --only firestore:rules --project <o-teu-project-id>
   ```

   (`firestore.rules` na raiz do repo restringe cada utilizador aos seus
   próprios dados — nunca deixar o Firestore sem regras, mesmo em
   desenvolvimento.)

## Correr localmente

1. Abre o Android Studio, **Open** e escolhe a pasta deste projeto.
2. Deixa o Android Studio sincronizar o Gradle (pode pedir para corrigir
   incompatibilidades — aceita).
3. Confirma que `app/google-services.json` existe (passo acima).
4. Corre a app num emulador ou dispositivo físico (build de debug, não
   precisa de mais nada).

Não é preciso criar um ficheiro `.env`: o `.env.example` na raiz é um
resquício do template original ("AI Studio") para uma `GEMINI_API_KEY`
que esta app não usa (a dependência `firebase-ai` está comentada em
`app/build.gradle.kts`) — o build usa os valores por omissão desse
ficheiro sem precisar de nenhuma chave real.

## Build de release assinado

O `signingConfig` de release lê o keystore e as passwords de variáveis de
ambiente, nunca de valores no código:

```
KEYSTORE_PATH=/caminho/para/a/tua.jks
STORE_PASSWORD=...
KEY_PASSWORD=...
```

Sem estas variáveis definidas, o Gradle tenta usar
`<raiz-do-repo>/my-upload-key.jks` por omissão. Um build de debug normal
(`./gradlew assembleDebug` ou correr a partir do Android Studio) não
precisa de nenhuma destas variáveis — usa a assinatura de debug padrão do
Android.

## Testes

```
./gradlew testDebugUnitTest
```

Testes unitários/Robolectric em `app/src/test`; testes instrumentados em
`app/src/androidTest`.

## Plano de correções

O trabalho de correção de bugs em curso está documentado em
[`docs/bugfix-plan.md`](docs/bugfix-plan.md).
