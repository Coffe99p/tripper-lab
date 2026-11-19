# Tripper Pod: referência de interoperabilidade

Consulta em 03/11/2026. Esta é uma implementação própria em Kotlin do formato de comunicação documentado pela comunidade, para testes no Tripper Pod pequeno da Meteor. Não se aplica ao Tripper Dash.

Referências:

- [Especificação comunitária WazeTripper](https://github.com/jnsdias/wazetripper/blob/main/docs/PROTOCOL.md).
- [Implementação de referência consultada para sequência de conexão e canal de retorno](https://github.com/jnsdias/wazetripper/blob/main/src/com/waze/wazetripper/TripperBridge.java), blob `ae3f6c6c6353706ada373b422ded47f42cd49d94`.
- [Android: comunicação Bluetooth em segundo plano](https://developer.android.com/develop/connectivity/bluetooth/ble/background).
- [Android: serviço de dispositivo conectado](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device).

## Implementado nesta etapa

- Busca BLE limitada a 20 segundos por nome `RE_DISP` ou UUID do serviço. O usuário escolhe o dispositivo encontrado; nenhuma conexão automática a desconhecidos.
- Cliente GATT com transporte LE explícito; validação do serviço e característica antes de escrever.
- Servidor GATT local para receber respostas do mostrador, sem advertising. Escritas de outros dispositivos, fragmentadas ou preparadas são rejeitadas.
- Pausa de 250 ms após descoberta, fila de escritas serializada e limite de 4 segundos para o callback de cada envio.
- Mensagens com 18 bytes de conteúdo e CRC-16/CCITT-FALSE de 2 bytes, big-endian.
- Solicitação de PIN para dispositivo novo. PIN de 1 a 6 dígitos, incluindo zeros iniciais; não persistido, copiado nem registrado no diagnóstico.
- Retomada para endereço salvo somente após confirmação visual explícita do usuário. Falta de PIN ou pareamento incorreto pode ser corrigida com “Esquecer Tripper / pedir novo PIN”.
- Sincronização do relógio em 24 h, setas simuladas e distâncias em metros / décimos de km. Rotatória é marcada como experimental.
- Reenvio da tela a aproximadamente cada 2 segundos; pings quando nenhuma tela foi selecionada.
- Sessão iniciada pelo usuário em serviço foreground `connectedDevice`, com notificação de encerramento e wakelock parcial limitado. Limite total de 10 minutos para este diagnóstico; não é uma estratégia final de economia de energia.
- Cancelamento fecha cliente e servidor, cancela tarefas, limpa a fila e solta o wakelock. Callbacks de sessões antigas são ignorados. Nenhuma reconexão automática nesta etapa.

## Limites conhecidos

### Ampliação na etapa 5 (10/11/2026)

Reconsultada a [referência comunitária](https://github.com/jnsdias/wazetripper/blob/main/docs/PROTOCOL.md). A 0.4.0 acrescenta curvas suaves (`18/19`), manter à esquerda/direita (`28/27`), entrada em via (`1B`) e saída numerada da rotatória (`31 + saída`, circulação pela direita). Espera usa `10 11 1C` com o restante do corpo zero e CRC calculado. Os novos símbolos precisam de confirmação no Tripper do usuário; a referência comunitária não substitui esse teste.

Quando a fonte fica incompleta ou expira, o app envia espera em vez de reaproveitar a última seta. Ao encerrar Maps → Tripper, tenta enviar espera e depois fecha a conexão; não promete retorno direto ao relógio. Mantém a sessão limitada a 10 minutos e não implementa reconexão automática. Cancelamento de simulações mantém o comportamento da etapa 3.

O formato de confirmação de autenticação das respostas do Tripper não está documentado nesta referência. Não interpretamos qualquer resposta ou callback de escrita como “PIN aceito” ou “seta exibida”. A interface pede confirmação física antes de salvar o dispositivo.

O Pod pode anunciar por pouco tempo depois de ligar a ignição. Após encerrar o teste pelo celular, pode ser necessário desligar e ligar a ignição para encontrá-lo novamente. A volta automática ao relógio e a reconexão após falhas não fazem parte desta versão de diagnóstico.

Nenhum payload de resposta é salvo, pois pode conter informações do PIN. O registro local contém horários, transições, contagem de respostas e erros; o endereço exibido é abreviado. O endereço completo do dispositivo só é persistido depois da confirmação do usuário, para reconexão manual.
