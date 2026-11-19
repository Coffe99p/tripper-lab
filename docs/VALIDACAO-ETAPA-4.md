# Validação da etapa 4

Atualização em 10/11/2026: etapa de inspeção concluída, versão 0.3.2 instalada. A seção de 03/11 abaixo é o registro histórico da primeira validação.

## Conclusão após o trajeto de 10/11

- Export encontrado em Downloads do Pixel, copiado para análise sem alterar o original. Contém 1.388 eventos dos dias 03, 05 e 10/11, incluindo 1.377 amostras de notificações; contador de eventos descartados igual a zero.
- Confirmados formatos reais de conversão por ponto de referência, curvas suaves, manter à direita, entrada em via, rotatórias, recálculo e `Arriving`. O usuário confirmou que o Maps encerrou sozinho ao chegar no trajeto de 10/11.
- Leitor corrigido e reprocessamento executado com a própria classe Kotlin compilada do app. No dia 10, 539 amostras contêm manobra e distância reconhecidas (antes: 202); 129 contêm manobra sem distância. As 487 interpretações completas já reconhecidas no export foram preservadas sem alteração de direção, distância ou saída.
- Distância ausente permanece ausente. Rotatória sem número não ganha uma saída inventada; entrada em via sem lado não ganha esquerda/direita. `Arriving` é aproximação, não prova universal de chegada concluída. A remoção continua invalidando a amostra atual, tanto ao chegar quanto ao cancelar.
- 22 testes unitários aprovados (13 de leitura, 4 da política de captura, 5 do protocolo), compilação e lint aprovados.
- APK 0.3.2 assinado e verificado; atualização instalada no Pixel com sucesso, `versionCode=5` / `versionName=0.3.2` confirmados. A abertura da `MapsActivity` retornou `Status: ok`; a consulta de erros AndroidRuntime não retornou erros. O Pixel estava bloqueado na inspeção da tela, portanto não foi feita confirmação visual da nova interface nesta atualização. Não foi iniciada outra captura.
- SHA-256 do APK: `2D32FBC01823F44D789AD74B5ADD772C7415FDDBEBF8741523BC71B3C5975653`.

A conclusão se refere ao diagnóstico dos formatos observados nesta combinação de aparelho/versão/idioma, não a cobertura universal do Maps ou navegação no Tripper. Português, manter à esquerda e outros formatos não presentes ainda têm apenas exemplos sintéticos ou não são suportados. Uma nova rota com captura parada e os limites de descarte de 2.000 eventos/2 MB não foram exercitados integralmente no aparelho; a política de bloqueio de captura tem cobertura unitária. Esses limites não impedem concluir a análise das amostras entregues, mas permanecem como verificações adicionais.

Detalhes e contagens: [ANALISE-TRAJETO-2026-11-10.md](ANALISE-TRAJETO-2026-11-10.md). Nenhuma integração Maps → Tripper foi iniciada. Parar aqui conforme solicitado pelo usuário.

## Registro histórico de 03/11/2026

Coleta real com versão 0.3.0; versão instalada ao final daquele dia: 0.3.1.

## Verificações no computador e no Pixel

- Compilação aprovada.
- 16 testes unitários aprovados: 5 do protocolo Bluetooth existente, 4 da política de captura (ativação explícita, pacote exato, expiração e encerramento) e 7 da leitura experimental de instruções.
- Lint aprovado, sem problemas reportados.
- Instalador atualizado no Pixel 8 Pro / Android 17, com abertura da `MapsActivity` confirmada.
- Nova tela capturada e inspecionada. O Android restaurou inicialmente a tela antiga após atualizar; reabrir o app exibiu corretamente a etapa 4.
- Consulta de erros AndroidRuntime após abrir não retornou erros.

## Validação funcional

O usuário autorizou o listener e confirmou que conteúdo real do Maps apareceu. Foram inspecionados somente os registros do nosso app, sem despejo das notificações de outros aplicativos.

- Google Maps instalado: **26.43.00.1002020710**, com instruções de navegação em inglês nesta sessão.
- Notificações de categoria `navigation`, `ongoing: true`. Os canais observados foram `1_2` e `1_foreground_1`; não são usados como identificadores fixos obrigatórios.
- O título (`android.title`) trouxe distância e manobra. Exemplos anonimizados: `190 m · Turn right onto [via]`, `180 m · Turn right onto [via]` e `400 m · Turn left onto [via]`.
- `android.subText` trouxe previsão de chegada, como `Arrive 16:02`; isso não é tratado como mensagem de chegada ao destino.
- Ícones foram extraídos. O ícone da instrução à direita foi inspecionado visualmente e correspondia à seta. A leitura experimental usa texto explícito, sem depender de reconhecimento visual ou IDs internos do Maps.
- Também apareceram `Starting navigation…` e `Head toward [via]`, sem distância. São classificados como informação incompleta, sem reaproveitar uma seta anterior.
- Atualizações ocorreram aproximadamente a cada segundo nesta amostra; isso não mede atraso total entre posição real e orientação.
- O usuário confirmou ter executado o roteiro de bloquear por 60 segundos e cancelar a rota. O registro contém **30 eventos com `locked: true`**, entre **15:45:40 e 15:46:08**. Esses registros comprovam recebimento com o aparelho bloqueado por cerca de 28 segundos observados; não devem ser apresentados como prova de um intervalo maior ou de uso prolongado.
- Cancelamento registrado como `REMOVED` às **15:46:19**, motivo Android 8. A tela do Tripper Lab passou a informar que não havia notificação atual, sem deixar a instrução removida como amostra vigente. Cancelamento não foi confundido com chegada.
- Captura encerrada pelo botão **Parar captura** às **15:50:03**, com `capturing: false` e evento `STOP` persistidos.
- Após atualizar e reabrir a versão 0.3.1, a interface permaneceu em **Captura parada**, com 75 eventos históricos do Maps e sem amostra atual.
- Assinatura do APK 0.3.1 validada. SHA-256: `BA015298CB78F523DBD294B5B621AF31C791DE1406258DC22751D7D976CE1F60`.

## Leitura experimental

Implementada uma interpretação conservadora dos títulos: manobra e distância precisam aparecer em um formato reconhecido e em uma notificação de navegação. Formatos desconhecidos, resumos de grupo, títulos incompletos e frases ambíguas não produzem uma direção. Estados de recálculo e chegada são separados das manobras.

Os testes com exemplos de português, bifurcação, rotatória, recálculo e chegada são **sintéticos**. Não substituem amostras reais desses casos. As amostras reais usadas nos testes de esquerda/direita têm nomes de ruas anonimizados.

## Pendências em 03/11 e decisão do usuário naquele dia

O usuário escolheu **deixar o trajeto para depois**. Naquele momento a etapa 4 estava parcialmente validada: faltavam amostras reais de bifurcações, rotatórias, recálculo e chegada, além de avaliar instruções durante deslocamento. Exportação pelo seletor de arquivos e limites de 20 minutos/2 MB não haviam sido exercitados em um teste completo de duração. A rejeição de eventos com captura parada foi verificada em teste unitário, sem iniciar outra rota real após parar. O trajeto foi posteriormente entregue; ver a conclusão de 10/11 acima.

Não há envio de conteúdo do Maps ao Tripper. A etapa 5 não foi iniciada.

## Evidências locais

Os registros brutos estão em `artifacts/maps-sample-stage4.json`, `artifacts/maps-locked-cancel-stage4.json` e `artifacts/maps-final-capture-stage4.json`. Podem conter nomes de ruas e destinos; a pasta está fora do versionamento. Este documento não reproduz esses nomes. A interface pós-cancelamento foi registrada em `artifacts/tripper-stage4-cancel.xml`; o estado parado da versão final está em `artifacts/tripper-stage4-final.xml`.

O roteiro e os casos pendentes estão em [TESTE-ETAPA-4.md](TESTE-ETAPA-4.md).
