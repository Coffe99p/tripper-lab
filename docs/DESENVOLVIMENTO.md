# Guia de desenvolvimento do Tripper Lab

Este guia acompanha a versão **0.7.0**. Para preparar o SDK, compilar e instalar o aplicativo, comece pelo [README](../README.md). Para operar o Pixel e o Tripper Pod, consulte o [guia de uso](GUIA-DE-USO.md).

## Entrada do aplicativo e navegação

O [manifesto](../app/src/main/AndroidManifest.xml) define `MainActivity` como a tela aberta pelo ícone. Ela carrega o [layout principal](../app/src/main/res/layout/activity_main.xml), que inclui os [controles de conexão](../app/src/main/res/layout/tripper_controls.xml). Em `onStart`, a tela se vincula a `TripperService` pelo `LocalBinder`. O serviço fornece um `Snapshot` com estado, candidatos, contadores e mensagem do Maps. A tela usa `renderTripper` para atualizar os controles. Em `onStop`, remove o observador e desfaz o vínculo. As telas herdam de `TripperActivity`. Enquanto alguma delas está visível, o serviço pode procurar o endereço BLE salvo. A notificação de sessão e o bloqueio parcial de suspensão começam quando o app encontra o mostrador ou quando o usuário inicia a conexão manual. [MainActivity](../app/src/main/java/com/joaofabijack/tripper/MainActivity.kt) · [TripperService](../app/src/main/java/com/joaofabijack/tripper/TripperService.kt).

| Ação na tela inicial | Destino e responsabilidade |
|---|---|
| Procurar ou reconectar | `requestSession` verifica as permissões e solicita ao serviço a ação `SCAN` ou `RECONNECT`. O serviço gerencia busca, conexão, PIN, prazo e encerramento. |
| Usar Maps no Tripper | O botão chama `startMaps` no serviço. **Abrir Google Maps** abre o aplicativo externo. |
| Como usar e resolver problemas | [HelpActivity](../app/src/main/java/com/joaofabijack/tripper/HelpActivity.kt) apresenta a ajuda local. |
| Diagnóstico do Maps | [MapsActivity](../app/src/main/java/com/joaofabijack/tripper/MapsActivity.kt) permite iniciar, parar, consultar e exportar a captura opcional de notificações. |
| Medir bateria | [BatteryActivity](../app/src/main/java/com/joaofabijack/tripper/BatteryActivity.kt) apresenta as sessões de medição e o histórico mantido por [BatteryLab](../app/src/main/java/com/joaofabijack/tripper/BatteryLab.kt). |

## Exemplo completo — conversão à direita a 190 metros

O título `190 m · Turn right onto Rua de teste` vem de um teste com nome de rua fictício. O resultado esperado no software é uma manobra à direita a 190 metros. A confirmação de que o mostrador exibiu essa manobra exige observar o equipamento.

1. **Ativação pela interface.** Ao tocar em **Usar Maps no Tripper**, `TripperService.startMaps` verifica se a conexão está em `READY`, se o endereço selecionado é o dispositivo já confirmado e se o leitor de notificações está autorizado e conectado. O serviço cria o estado da rota, registra a função que receberá os eventos e solicita as notificações já ativas. [Serviço da sessão](../app/src/main/java/com/joaofabijack/tripper/TripperService.kt).
2. **Entrada do Android.** Quando o Maps publica a notificação, `onNotificationPosted` filtra o pacote do Google Maps. O fluxo de navegação lê o título, a categoria e os indicadores de notificação contínua e de resumo de grupo. A captura para diagnóstico é opcional e tem controle próprio. [MapsNotificationListener](../app/src/main/java/com/joaofabijack/tripper/MapsNotificationListener.kt) · [MapsNavigationFeed](../app/src/main/java/com/joaofabijack/tripper/MapsNavigationFeed.kt).
3. **Interpretação.** `MapsInstructionParser.parse` reconhece a conversão, normaliza a distância e devolve a direção `RIGHT` e o valor `190` em metros. É aqui que se altera o reconhecimento de um novo formato de título. [MapsInstructionParser](../app/src/main/java/com/joaofabijack/tripper/MapsInstructionParser.kt) · [MapsInstructionParserTest](../app/src/test/java/com/joaofabijack/tripper/MapsInstructionParserTest.kt).
4. **Decisão sobre a rota.** `MapsRouteState.posted` seleciona a notificação atual e traduz o resultado em uma `Guidance` com `TripperProtocol.Maneuver.RIGHT`. Se o Maps repetir a mesma instrução da mesma notificação sem a distância, o painel conserva a última metragem por até oito segundos desde a medição. A próxima manobra completa tem efeito imediato. Ao ativar o envio, uma notificação já existente com 30 segundos ou mais é descartada. Durante a sessão, 30 segundos sem atualização também levam à espera. [MapsRouteState](../app/src/main/java/com/joaofabijack/tripper/MapsRouteState.kt) · [MapsRouteStateTest](../app/src/test/java/com/joaofabijack/tripper/MapsRouteStateTest.kt).
5. **Saída e retorno ao usuário.** `applyGuidance` pede a `TripperProtocol.maneuver` os bytes da tela ou usa `waiting` para espera. Quando os bytes mudam, o serviço substitui envios pendentes de orientação e `pump` escreve na característica GATT. Em paralelo, `publish` envia o novo estado à interface. Assim, o texto **Maps** no celular mostra a decisão do software. Para verificar o símbolo e a distância recebidos, observe também o Tripper Pod. [TripperProtocol](../app/src/main/java/com/joaofabijack/tripper/TripperProtocol.kt) · [Protocolo e símbolos](PROTOCOLO.md).

**Se a seta estiver errada**, compare primeiro o título anonimizado com o resultado do parser. Se direção e metros estiverem corretos, confira a tradução em `MapsRouteState`, depois os bytes em `TripperProtocol`. Se o celular mostrar a indicação esperada e o painel continuar em espera, consulte o registro da sessão para distinguir falha de conexão, escrita recusada ou falta de confirmação da transmissão.

## Integrações e ciclo da sessão

- **Google Maps.** O Android entrega as notificações via `NotificationListenerService`. O usuário concede o acesso nas configurações do aparelho. O APK não exige chave de API do Maps nem configuração de servidor. A remoção da notificação selecionada, a chegada explícita e a perda do leitor retiram a seta e preservam a conexão. Uma mensagem de aproximação aguarda a próxima atualização.
- **Tripper Pod.** O serviço usa BLE/GATT. O cliente escreve no mostrador e o servidor GATT local recebe respostas. No primeiro pareamento, o usuário informa o PIN e confirma uma simulação no painel para salvar o dispositivo. As escritas passam por uma fila, com até quatro segundos de espera pelo callback. Ao encerrar uma rota, o serviço envia e mantém a tela de espera até receber outra instrução. [Primeira conexão](GUIA-DE-USO.md) · [Protocolo](PROTOCOLO.md).
- **Duração da conexão.** O serviço permanece em primeiro plano durante a sessão, inclusive com o Maps aberto e a tela bloqueada. O usuário pode encerrar pela notificação. Uma queda do enlace já confirmado inicia uma tentativa de reconexão ao mesmo mostrador, inclusive em segundo plano. Se funcionar, o Maps é retomado quando já estava ativo. O serviço usa `START_NOT_STICKY` após encerramento do processo.

## Dados no aparelho

| Dado | Onde vive | O que ocorre ao encerrar ou reabrir |
|---|---|---|
| Tripper confirmado e último desfecho | Preferências privadas `tripper`, chaves `address` e `lastOutcome` | O endereço permite reconexão automática e manual e pode ser removido em **Esquecer Tripper**. O PIN não é persistido. |
| Orientação atual e fila Bluetooth | Memória do processo, em `MapsNavigationFeed`, `MapsRouteState` e `TripperService` | São recriadas em outra sessão. O envio normal não arquiva o título da notificação ou o nome da rua. |
| Registro do serviço | `filesDir/last-session.txt` e, quando há registro anterior, `filesDir/previous-session.txt` | O serviço mantém até 100 linhas recentes no registro atual. O histórico pode ajudar a investigar uma desconexão. |
| Captura opcional do Maps | `noBackupFilesDir/maps-capture.json`, mantido por [MapsDiagnostics](../app/src/main/java/com/joaofabijack/tripper/MapsDiagnostics.kt) | A captura dura até 20 minutos. Pode incluir texto e ícones do trajeto. O usuário pode consultar, apagar ou exportar o registro. |
| Medições de bateria | `noBackupFilesDir/battery-lab.json`, mantido por `BatteryLab` | O histórico guarda até 20 resultados. Após reinício do processo, uma medição em andamento é registrada como interrompida. [BatteryRun](../app/src/main/java/com/joaofabijack/tripper/BatteryRun.kt). |

Na exportação, as telas usam arquivos privados temporários e o seletor de destino do Android. Uma cópia exportada permanece no destino escolhido pelo usuário. Para reproduzir um caso em teste, substitua ruas e destinos por exemplos fictícios. As capturas originais de desenvolvimento ficam em `artifacts/`, fora do versionamento.

## Como validar uma alteração

Na raiz do projeto, com o SDK configurado conforme o README, rode a suíte mais próxima do comportamento alterado. Por exemplo, para uma nova forma de escrever uma conversão, use no PowerShell com o JDK configurado:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.joaofabijack.tripper.MapsInstructionParserTest"
```

| Área alterada | Verificação |
|---|---|
| Interpretação e validade da orientação | `MapsInstructionParserTest` cobre o texto. `MapsRouteStateTest` cobre substituição, expiração e remoção. Inclua um título ambíguo ou incompleto ao testar um novo formato. |
| Símbolo, distância ou mensagem Bluetooth | [TripperProtocolTest](../app/src/test/java/com/joaofabijack/tripper/TripperProtocolTest.kt) verifica a codificação. Confira a seta simulada no painel e registre separadamente o resultado de uma instrução real. |
| Captura ou bateria | [MapsCaptureGateTest](../app/src/test/java/com/joaofabijack/tripper/MapsCaptureGateTest.kt) e [BatteryRunTest](../app/src/test/java/com/joaofabijack/tripper/BatteryRunTest.kt) cobrem as respectivas regras. Use o roteiro físico para observações de consumo. |
| Interface e ciclo do Android | Abra a tela inicial, navegue até a tela alterada e volte. Para mudanças na sessão, confira conexão, estado dos botões, indicação da notificação e encerramento no aparelho. |

Antes de enviar a alteração, execute `.\scripts\build.ps1` para compilar o APK de depuração e rodar todos os testes e o lint. A referência da versão 0.6.0 é de 50 testes aprovados e lint sem ocorrências. Registre o que foi observado no celular e no mostrador quando a mudança depender de hardware. [Relatório desta versão](VALIDACAO-ETAPA-7.md).

## Pendências que afetam a manutenção

| Questão em aberto | Como retomar |
|---|---|
| Conversão real com distância, integração com a tela bloqueada, encerramento visual e símbolos adicionais | Já houve confirmação de simulações e de espera para instrução incompleta. Em uma rota real, confronte a indicação processada pelo app com o símbolo e a distância vistos no painel. Registre também o que aparece ao encerrar. [Roteiro de integração](TESTE-ETAPA-5.md) · [Resultados anteriores](VALIDACAO-ETAPA-5.md). |
| Desconexão com código 147 | A causa continua indeterminada. Se voltar a ocorrer, registre a fase da sessão, o tempo até a queda e o resultado da reconexão. [Histórico da falha](VALIDACAO-ETAPA-5.md). |
| Consumo de bateria | A coleta comparativa está pendente. O roteiro prevê sessões semelhantes de 20–25 minutos, com Maps sozinho e com Maps mais Tripper, sem carregador e com captura de diagnóstico desativada. [Roteiro de medição](TESTE-ETAPA-6.md) · [Estado do medidor](VALIDACAO-ETAPA-6.md). |
