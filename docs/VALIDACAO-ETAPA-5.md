# Validação da etapa 5

Atualizado em 12/11/2026. **Entrega encerrada com validação parcial, aceita pelo usuário para seguir o planejamento.** A conexão básica com novo PIN, simulação e espera diante de instrução incompleta foram confirmadas. O envio de uma conversão real do Maps permanece sem confirmação física inequívoca. Os registros abaixo preservam o histórico desde 10/11.

## Implementação

- Ativação explícita por **Usar Maps no Tripper**, após conexão com dispositivo previamente salvo por confirmação visual. Não inicia navegação nem coleta textual automaticamente.
- Fonte independente da captura de diagnóstico: lê somente notificações do pacote exato do Maps enquanto a sessão estiver ativada. Não persiste títulos, destinos, nomes de ruas ou chaves de notificações. O registro Bluetooth contém somente estados, manobras, distâncias e contagens.
- Cada atualização substitui a instrução inteira. A fila descarta telas aguardando envio quando uma instrução mais atual chega. Um envio já aceito pelo Android não pode ser revogado; a tela seguinte o substitui.
- Prazo monotônico de 30 segundos para instruções. Snapshots preservam a idade original; snapshots antigos ou com horário futuro são rejeitados. Notificações de outro tipo ou resumos não viram instruções. Remoção de uma chave antiga não apaga a fonte atual; remover a fonte atual encerra a sessão e não restaura outra amostra.
- Manobras completas: esquerda/direita, suaves, em frente, manter à esquerda/direita, entrada em via e rotatórias com saída explícita de 1 a 9 (circulação pela direita).
- Dados incompletos, desconhecidos, sem correspondência ou expirados usam o símbolo não direcional de espera/recálculo. Não presumir distância zero, saída ou lado ausentes.
- `Arriving` permanece aproximação. Chegada explícita, remoção da fonte, perda do listener ou ação do usuário encerram a sessão. Antes de desconectar, o app tenta substituir a seta por espera, com envio limitado por timeout; aceitação pelo Android não confirma a exibição física. Em falha do link, o apagamento não é garantido.
- Mantidos limite de sessão de 10 minutos, serviço em primeiro plano, encerramento pela notificação, wakelock limitado e ausência de retomada automática. A captura opcional de 20 minutos continua separada.

## Verificações concluídas

- Compilação, lint e **36 testes** aprovados: 13 de interpretação de texto, 12 de estado/frescor da rota, 7 do protocolo e 4 da política de captura.
- Cobertura de substituição de direção/distância, ausência de dados, recálculo, chegada, chaves antigas, resumos, troca de categoria, reinício de sessão, limite de 30 segundos, snapshots antigos/futuros, novos símbolos e limites das saídas de rotatória.
- APK assinado e verificado; instalação no Pixel retornou sucesso. Abertura da `MapsActivity` confirmou `Status: ok`.
- APK: `artifacts/Tripper-Lab-0.4.0.apk`.
- SHA-256: `8A9ACF759219C64EF76F6C0E3E8E38A8DEF09F737794045DDBFA3F1D05F314C1`.

## Teste físico parcial

- Usuário confirmou que o símbolo de espera apareceu ao ativar o Maps sem rota, sem seta de conversão. Registro do app: reconexão ao dispositivo salvo às 12:39:55, link pronto às 12:39:56, Maps ativado e espera enviada às 12:40:09 (horários de Brasília). Não foi solicitado novo PIN.
- O usuário relatou coincidência de seta e distância com o Maps. Contudo, o registro disponível da sessão iniciada às 12:43:26 contém `Simulação: Direita • 200 m` às 12:43:43, ativação do Maps/espera às 12:43:46 e retirada da orientação/encerramento por remoção da notificação às 12:44:52, sem registro de envio de manobra pelo Maps. Portanto, a origem da seta ainda precisa ser esclarecida antes de considerar a integração física validada. Solicitado repetir sem simulações e informar o estado `Maps:` na tela do app.
- Encerramento por remoção da notificação comprovado no registro do app; confirmação visual do resultado final e desaparecimento da notificação de sessão ainda pendentes.
- Ainda pendentes: instrução real inequivocamente enviada pelo Maps, bloqueio e cobertura física dos símbolos novos. O pedido inicial de bloqueio foi precedido por uma nova orientação para conferir a origem da seta primeiro.

Evidências locais: `artifacts/tripper-stage5-first-live.txt`, `artifacts/tripper-stage5-live-confirmed.txt` (nome do arquivo reflete o relato recebido; não é prova de origem Maps) e `artifacts/tripper-stage5-app-logcat.txt`. O logcat disponível retornou apenas as linhas finais de encerramento, sem esclarecer a origem da seta vista anteriormente.

Roteiro: [TESTE-ETAPA-5.md](TESTE-ETAPA-5.md). Não iniciar a etapa 6.

## Ponto de retomada

O usuário informou que não consegue fazer mais testes agora. As solicitações de teste ainda sem resposta ficam adiadas; não solicitar novos testes nesta sessão. Não concluir a etapa 5 com base apenas na coincidência visual relatada, pois o registro disponível não diferencia essa observação da simulação anterior.

Quando houver disponibilidade, retomar com uma conexão nova e **Usar Maps no Tripper**, sem enviar simulações. Conferir primeiro o estado `Maps:` na tela e o registro de uma manobra real com distância; depois conferir o mostrador, bloqueio e cancelamento. A 0.4.0 permanece instalada. A última sessão inspecionada encerrou por remoção da notificação às 12:44:52; isso não comprova o estado atual do aparelho após essa inspeção.

## Retomada em 12/11/2026

O usuário solicitou continuidade. Retomada da etapa 5, sem avançar à etapa 6. O projeto permanece na versão 0.4.0; não houve alteração de código nem necessidade de repetir a compilação já aprovada. Na primeira consulta, nenhum aparelho estava conectado por ADB. Solicitada disponibilidade da moto parada e conexão USB do Pixel para confirmar primeiro uma instrução real sem usar simulações. A validação continua pendente até obter as novas evidências.

O usuário confirmou disponibilidade para testar. O Pixel conectou por USB, a versão instalada foi confirmada (`versionCode=6`, `versionName=0.4.0`) e a abertura da `MapsActivity` retornou `Status: ok`. Solicitada nova conexão ao Tripper e ativação do Maps sem usar comandos simulados, com relato do estado `Maps:` e da exibição física. Na consulta seguinte, o USB estava desconectado; nenhum registro novo da sessão foi obtido. O teste no celular pode prosseguir, com coleta do registro após reconectar o cabo.

## Desconexões em 12/11 e versão 0.4.1

O usuário relatou desconexões repetidas e ausência do texto `Maps:`, e confirmou que o Pixel permaneceu ao lado da moto com a ignição ligada. Após reconectar o USB, foi obtido o registro da última tentativa: início às 07:21:24, link pronto às 07:21:25 e erro Bluetooth 147 às 07:21:56. Não há ativação do Maps nem resposta do Tripper registrada nessa tentativa. A causa da falha de conexão ainda não foi determinada.

O Android define 147 como `GATT_CONNECTION_TIMEOUT` ([referência oficial](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#GATT_CONNECTION_TIMEOUT)). O código, isoladamente, não prova interferência, erro de PIN ou defeito no app. Não atribuir a queda ao limite de 10 minutos.

Foi preparada e instalada a **0.4.1** (`versionCode=7`):

- Estado `Maps:` sempre presente no alto da tela de conexão, indicando desligado, aguardando conexão, ativo ou interrompido. O botão **Usar Maps no Tripper** também foi movido para perto desse estado.
- A mensagem de conexão pronta orienta a ativar o Maps, em vez de direcionar automaticamente às simulações.
- Tentativas de ativação impedidas por dispositivo não confirmado ou acesso ao listener ficam registradas.
- Falhas passam a registrar duração, estado, contagens de envios aceitos pelo Android/respostas do Tripper e tempo desde o último envio. Uma nova tentativa preserva o registro da anterior em `previous-session.txt`. Sem PIN, títulos de rota ou chaves de notificações.
- Nenhuma alteração especulativa de temporização ou reconexão Bluetooth. A atualização melhora a interface e o diagnóstico; **não há evidência de correção da desconexão**.
- Compilação, 36 testes e lint aprovados. Assinatura do APK verificada. Instalação e versão confirmadas no Pixel; após o Android terminar a tela de atualização, a abertura foi confirmada na `MapsActivity`.
- APK: `artifacts/Tripper-Lab-0.4.1.apk`. SHA-256: `9CE0FFA084A8A051B48C58276DD532D30E6424A69D3B8FED2C15A35FB6500056`.

Solicitado teste com novo PIN e simulação explícita de **Esquerda • 50 m**, mantendo o Pixel junto à moto por 45 segundos, antes de ativar o Maps. Esse teste é exclusivamente de conexão e não contará como validação do envio real do Maps. Se funcionar, salvar novamente o dispositivo por confirmação visual antes de retomar a integração.

Evidências: `artifacts/tripper-stage5-2026-11-12-disconnect.txt` e `artifacts/tripper-stage5-2026-11-12-disconnect-logcat.txt` (somente linhas do Tripper Lab).

## Resultado do teste com novo PIN

Em 12/11, o usuário respondeu ao roteiro de novo PIN e **Esquerda • 50 m**, com espera de 45 segundos: **a seta apareceu e a conexão permaneceu ativa**. Isso valida a comunicação básica nessa tentativa, mas não determina a causa da falha anterior nem comprova estabilidade prolongada ou envio real do Maps. Na consulta após o relato, o Pixel não estava conectado por USB; não foi obtido registro novo dessa tentativa.

Próximo passo solicitado: confirmar a exibição para salvar novamente o Tripper, ativar **Usar Maps no Tripper**, iniciar uma rota sem enviar outra simulação e informar o estado `Maps:` junto da exibição no mostrador. Bloqueio e encerramento ficam para depois dessa confirmação. Não avançar à etapa 6.

## Instrução incompleta identificada em 12/11

O usuário informou que o app exibe **instrução incompleta** e o Tripper mostra espera. Ao conferir a notificação expandida do Google Maps, identificou o texto **`Head toward [via]`**, sem distância. Esse é um formato inicial já coberto pelos testes do leitor: não contém conversão inequívoca nem distância, portanto não deve virar seta em frente, conversão ou zero metros. Nenhuma alteração de interpretação foi feita para esse caso.

Com o USB reconectado, foi preservado `artifacts/tripper-stage5-2026-11-12-incomplete.txt`. O registro mostra nova conexão com PIN, simulação efetivamente enviada de **Direita • 200 m** às 07:30:57, confirmação visual/salvamento às 07:30:59, ativação do Maps/espera às 07:31:01 e encerramento por remoção da notificação às 07:32:41. A simulação registrada difere do exemplo Esquerda • 50 m do roteiro; nenhuma delas será contabilizada como instrução real do Maps. Não houve novo erro 147 nesse registro. O arquivo de captura textual continua com última amostra de 10/11, portanto o texto `Head toward` desta tentativa foi identificado pelo relato do usuário, não por uma captura nova.

O fluxo Maps → classificação incompleta → símbolo de espera foi confirmado pelo usuário. A validação de uma conversão real com distância permanece pendente. Os testes parados ficaram na orientação inicial; foi consultada a disponibilidade para um trajeto curto, configurado antes de sair e sem operar o celular em movimento. Manter o limite de 10 minutos e não tratar a ausência de instrução completa como defeito de Bluetooth.

## Aceite parcial para prosseguir — 12/11/2026

O usuário optou por seguir o planejamento sem realizar agora o teste em trajeto, deixando a possibilidade de retomá-lo caso necessário. A entrega da etapa 5 fica encerrada com essa ressalva. Isso substitui as exigências anteriores deste documento de concluir toda a validação física antes da etapa 6; não converte testes não realizados em resultados aprovados.

Permanecem pendentes a confirmação física inequívoca de uma conversão real do Maps com distância, a validação dessa integração com tela bloqueada e a confirmação visual do encerramento. A cobertura dos novos símbolos também é parcial. Os 36 testes automatizados aprovados, os resultados físicos já obtidos e as limitações conhecidas continuam registrados separadamente.

Essas pendências não bloqueiam o início da etapa 6 por decisão do usuário. Não insistir em nova coleta agora. A etapa 6 ainda não foi iniciada nesta entrega; manter a execução de uma etapa por vez.
