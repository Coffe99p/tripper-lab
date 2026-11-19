# Análise do trajeto — etapa 4

Analisado em 10/11/2026. Export gerado no Tripper Lab 0.3.1, Google Maps 26.43.00.1002020710, Pixel 8 Pro / Android 17. Instruções observadas em inglês. Correções incluídas na versão 0.3.2.

## Origem e escopo

O arquivo `Tripper-Maps-diagnostico.json` foi encontrado em Downloads do Pixel e copiado para `artifacts/maps-trip-2026-11-10.json`. O original não foi alterado. O export reúne sessões dos dias 03, 05 e 10/11; não representa um único percurso contínuo.

- 1.388 eventos: 1.377 amostras POSTED/SNAPSHOT e 11 eventos de início, parada, remoção ou limite.
- Contador `omitted: 0`; isso informa que o coletor não descartou eventos por capacidade, não garante que o Maps publicou cada mudança de navegação.
- Arquivo exportado: 2.230.539 bytes, com indentação. O limite de 2.000.000 bytes do app se aplica ao arquivo privado compacto.
- SHA-256 do export: `6C6C09948591CE22223CE3702CCC0135BFB2621DD6497B7408D21753C2F766CD`.

Os registros brutos e arquivos intermediários ficam em `artifacts/`, excluída do versionamento, porque contêm informações de percurso. Este relatório e os testes usam descrições anonimizadas.

## O que o Maps informou

O título trouxe conversões comuns e por ponto de referência, curvas suaves, manter à direita, entrada em via e três tipos de instrução de rotatória: saída numerada, virar à direita sem número da saída e sair da rotatória. Também surgiram `Rerouting...`, `Arriving` e atualizações sem distância.

O leitor passou a distinguir esses formatos. Exemplos anonimizados:

| Título | Leitura |
|---|---|
| `200 m · Turn right after Example Shop (on the left) onto Test Road` | Direita, 200 m; a posição do estabelecimento não altera a manobra |
| `1.2 km · Slight right to stay on Test Road` | Curva suave à direita, 1.200 m |
| `1.0 km · At the roundabout, take the 1st exit and stay on Test Road` | Rotatória, saída 1, 1.000 m |
| `90 m · At the roundabout, turn right onto Test Road` | Direita na rotatória, 90 m, saída desconhecida |
| `100 m · Exit the roundabout` | Sair da rotatória, 100 m |
| `100 m · Merge onto Test Road` | Entrar na via, 100 m, lado não informado |
| `Turn right` | Direita, distância ausente, estado incompleto |
| `Arriving` | Aproximando-se do destino, sem manobra/distância |

Os exemplos ilustram formatos observados; valores e referências foram escolhidos para testes anônimos. A conversão decimal de quilômetros usa aritmética exata. Distância ausente nunca reutiliza a amostra anterior nem vira zero; zero explícito continua sendo zero.

## Reprocessamento com o código do app

Todas as 1.377 amostras foram passadas à própria classe Kotlin compilada `MapsInstructionParser`, com título, categoria, indicador de notificação contínua e indicador de resumo originais. As 487 interpretações completas gravadas pela versão anterior permaneceram idênticas em estado, direção, distância e saída. As 73 amostras de 03/11 não tinham interpretação persistida e não entram nessa comparação.

| Data | Amostras | Manobra + distância antes | Manobra + distância em 0.3.2 | Manobra com distância ausente em 0.3.2 |
|---|---:|---:|---:|---:|
| 03/11 | 73 | Não registrada | 51 | 0 |
| 05/11 | 625 | 285 | 519 | 80 |
| 10/11 | 679 | 202 | 539 | 129 |

São contagens de atualizações, com repetições da mesma manobra; não são números de curvas diferentes ou uma medida de precisão universal.

Nas 679 amostras de 10/11, o resultado foi:

- 539 manobras com distância reconhecida;
- 129 manobras reconhecidas sem distância;
- 4 títulos de orientação inicial (`Head toward...`) sem manobra inequívoca;
- 1 recálculo;
- 6 mensagens de aproximação (`Arriving`).

Nas amostras de 05/11, 10 títulos que traziam apenas referência de destino com distância permanecem desconhecidos; não foram transformados em direção. Os demais casos sem manobra correspondem a aproximação ou orientação inicial.

## Tela bloqueada e fim do percurso

Horários abaixo em Brasília (UTC−3).

- Em 05/11, 623 amostras vieram com `locked: true`, entre 10:44:20 e 11:01:39.
- Em 10/11, a sessão começou às 11:19:56 e terminou o registro da rota às 11:33:16. Das 679 amostras, 678 vieram com `locked: true`, entre 11:20:23 e 11:33:16, abrangendo cerca de 12 minutos e 53 segundos entre a primeira e a última atualização bloqueada.
- As seis mensagens finais `Arriving` de 10/11 precederam uma remoção às 11:33:16.930, motivo 8. O usuário confirmou: **o Maps encerrou sozinho ao chegar**.

Esse encadeamento comprova o cenário relatado nesta coleta. Não transforma `Arriving` em confirmação universal de chegada concluída nem faz o motivo de remoção distinguir cancelamento de chegada. A implementação mantém aproximação separada e invalida a amostra atual quando sua notificação é removida. Não há medição de latência ponta a ponta nem de consumo de bateria.

O export também contém um evento `LIMIT` em 05/11. A gravação desse evento ocorreu cerca de 20 min 39 s após o início daquela sessão; nenhum POSTED/SNAPSHOT daquela sessão ultrapassou os 20 minutos. A política usa prazo monotônico para rejeitar entradas expiradas, enquanto a execução do callback de parada pode atrasar. Esta evidência não exercita todos os limites de capacidade.

## Resultado e próximo passo

Etapa 4 concluída como inspeção dos dados reais disponíveis, com limites explícitos. Compilação, 22 testes e lint passaram. A versão 0.3.2 foi instalada e sua abertura confirmada pelo Android; a tela estava bloqueada, sem nova conferência visual da interface. Nenhuma captura ou navegação foi iniciada nesta instalação.

A etapa 5 permanece não iniciada. Ela deverá ligar as instruções atuais ao Tripper, definir o comportamento quando faltarem dados, validar os símbolos das novas manobras no mostrador e limpar orientações ao encerrar ou perder a fonte. O teste inicial deverá ser feito com a moto parada. Português e formatos ausentes desta coleta ainda precisam de validação real; isso não impede usar estes resultados para preparar a integração dos formatos observados quando o usuário autorizar a próxima etapa.

Evidências de reprocessamento: `artifacts/ReplayMaps.java`, `artifacts/replay_maps_trip.py`, `artifacts/maps-trip-replay-summary.json` e `artifacts/maps-trip-replay-results.json`. Executar o script após compilar o app refaz a análise local sem modificar o export.
