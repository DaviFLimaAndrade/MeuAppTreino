# Meu Treino

App Android (Kotlin + Jetpack Compose) para registrar treino e dieta.

## Telas

- **Treinos**: Lower A, Upper A, Lower B e Upper B, já com as cargas de novembro. Toque num exercício para registrar a carga e as reps de hoje (o histórico fica salvo). O botão + adiciona exercícios. O banner mostra qual treino cai hoje no ciclo de 2 dias de treino e 2 de descanso. Se você pular um dia, abra o treino que vai fazer e toque em "Hoje vou fazer o…" para ajustar o ciclo.
- **Corpo**: silhueta de frente e de costas, colorida pela carga média de cada grupo muscular, com a lista de médias e máximos.
- **Dieta**: plano alimentar com caixas para marcar o que já comeu e contador de água (meta de 3 L). Tudo é desmarcado sozinho quando vira o dia. Use "Editar lista" para remover itens e o botão + para adicionar.

Os dados ficam salvos só no celular.

## Como instalar

1. Abra o Android Studio e vá em **File > Open**, escolhendo a pasta `MeuTreino`.
2. Espere o Gradle sincronizar. Na primeira vez ele baixa as dependências e pode demorar alguns minutos.
3. Escolha uma das opções:
   - **Celular no cabo (USB)**: ative a Depuração USB no celular (Configurações > Sobre o telefone > toque 7x em "Número da versão" > Opções do desenvolvedor > Depuração USB), conecte o cabo e clique em **Run ▶**.
   - **Gerar APK**: vá em **Build > Build App Bundle(s) / APK(s) > Build APK(s)**. O arquivo sai em `app/build/outputs/apk/debug/app-debug.apk`. Mande o arquivo para o celular e abra para instalar (o Android vai pedir para permitir "instalar apps desconhecidos").

Requisitos: Android Studio Ladybug (2024.2) ou mais novo. O celular precisa ter Android 8.0 ou mais novo.
