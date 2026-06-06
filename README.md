# GameText Analyzer
### Benchmark modular de estratégias de paralelização aplicado à busca de palavras em textos literários

---

## Resumo

O **GameText Analyzer** é uma aplicação em Java desenvolvida para comparar o desempenho de diferentes estratégias de busca/contagem de palavras em arquivos `.txt`. O trabalho utiliza as amostras obrigatórias fornecidas na atividade: **Don Quixote**, **Dracula** e **Moby Dick**, e executa benchmarks entre métodos seriais, paralelos em CPU e paralelos em GPU/OpenCL.

Para manter uma conexão com jogos digitais, as obras foram interpretadas como **mundos narrativos** que poderiam servir de base para adaptações em jogos:

| Obra | Interpretação gamificada | Palavra-tema analisada |
|---|---|---|
| **Don Quixote** | RPG de Cavalaria e Aventura | `quijote` |
| **Dracula** | Survival Horror Gótico | `blood` |
| **Moby Dick** | Aventura Naval / Boss Hunt | `whale` |

A partir dessas amostras, o projeto compara abordagens como `SerialCPU`, `ParallelCPU`, `ParallelStream`, `ForkJoin`, `Virtual Threads`, `GPU String` e `GPU Hash`. Os resultados são registrados em CSV e visualizados em gráficos, permitindo analisar tempo de execução, speedup, eficiência paralela, impacto do número de threads, granularidade dos chunks, thresholds do ForkJoin e overhead da GPU.

---

## Introdução

A busca por eficiência computacional é essencial em aplicações que processam grandes volumes de dados. Em jogos digitais, textos e logs podem ser analisados para identificar padrões narrativos, frequência de eventos, recorrência de personagens, elementos de mundo e temas centrais. Neste trabalho, essa ideia foi adaptada para as amostras obrigatórias, interpretando cada obra literária como um possível universo de jogo.

O objetivo técnico é comparar diferentes formas de realizar a mesma tarefa: **contar a ocorrência de uma palavra em um texto**. A tarefa é simples o suficiente para permitir comparação clara entre métodos, mas também interessante para observar quando o paralelismo compensa ou quando o overhead de paralelização prejudica o desempenho.

A aplicação foi estruturada como um **framework modular de benchmark**. Todas as estratégias implementam uma interface comum (`WordCounter`) e são registradas em `StrategyRegistry`. Dessa forma, novos métodos podem ser adicionados sem alterar a lógica principal do benchmark, do CSV ou da geração de gráficos.

---

## Métodos implementados

### Estratégias seriais

| Método | Descrição |
|---|---|
| `SerialCPU` | Percorre o vetor de palavras com um loop simples. É o baseline principal. |
| `SerialStream` | Usa `Arrays.stream(...)` em modo sequencial, permitindo comparar loop manual com Stream API. |

### Estratégias paralelas em CPU

| Método | Descrição |
|---|---|
| `ParallelCPU-2t` | Divide o vetor entre 2 threads tradicionais. |
| `ParallelCPU-4t` | Divide o vetor entre 4 threads tradicionais. |
| `ParallelCPU-8t` | Divide o vetor entre 8 threads tradicionais. |
| `ParallelCPU-Nt` | Usa `Runtime.getRuntime().availableProcessors()` para definir o número de threads dinamicamente. |
| `ParallelCPU-8t-32chunks` | Usa 8 threads e 32 chunks fixos para testar granularidade. |
| `ParallelCPU-8t-128chunks` | Usa 8 threads e 128 chunks fixos. |
| `ParallelCPU-8t-dynChunks` | Calcula dinamicamente a quantidade de chunks de acordo com o tamanho do texto. |

### Estratégias com ForkJoin, Stream paralela e Virtual Threads

| Método | Descrição |
|---|---|
| `ForkJoinCPU-th2000` | Usa ForkJoin com threshold fixo de 2.000 palavras. |
| `ForkJoinCPU-th10000` | Usa ForkJoin com threshold fixo de 10.000 palavras. |
| `ForkJoinCPU-dyn` | Calcula threshold dinâmico conforme o tamanho da entrada. |
| `ParallelStream` | Usa `Arrays.stream(...).parallel()`, baseado no ForkJoinPool comum da JVM. |
| `VirtualThreads-50chunks` | Divide o trabalho em 50 chunks executados por virtual threads. |
| `VirtualThreads-100chunks` | Testa granularidade maior com 100 chunks. |

### Estratégias em GPU/OpenCL

| Método | Descrição |
|---|---|
| `ParallelGPU-String` | Envia o texto como bytes, offsets e tamanhos, e compara a palavra caractere por caractere no kernel OpenCL. |
| `ParallelGPU-Hash` | Converte cada palavra para `hashCode + length`, tornando a comparação na GPU uma operação numérica mais simples. |

A versão `ParallelGPU-String` foi mantida por ser mais próxima da comparação textual exata. A versão `ParallelGPU-Hash` foi adicionada como otimização experimental, reduzindo a complexidade da comparação no kernel. Como hashes podem ter colisões teóricas, o tamanho da palavra também é comparado para reduzir o risco de falso positivo.

---

## Metodologia

### Amostras obrigatórias

As amostras oficiais utilizadas no benchmark ficam em:

```text
data/samples/
```

| Arquivo | Obra | Mundo narrativo | Palavra buscada |
|---|---|---|---|
| `DonQuixote-388208.txt` | Don Quixote | RPG de Cavalaria e Aventura | `quijote` |
| `Dracula-165307.txt` | Dracula | Survival Horror Gótico | `blood` |
| `MobyDick-217452.txt` | Moby Dick | Aventura Naval / Boss Hunt | `whale` |

Essas obras atendem aos requisitos de variar o **tamanho** e a **natureza** das entradas: são textos literários diferentes, com volumes diferentes e temáticas distintas.

### Tokenização

Como as amostras são textos literários completos, o programa realiza uma etapa de tokenização antes do benchmark. Essa etapa:

- converte o texto para minúsculas;
- remove pontuação;
- remove acentos;
- divide o texto em palavras;
- ignora espaços e linhas vazias.

A tokenização acontece antes da medição principal. Assim, os tempos registrados nos gráficos representam principalmente o custo da **contagem em memória**, e não o tempo de leitura do disco.

### Medição de tempo

O tempo é medido com:

```java
System.nanoTime()
```

O valor é convertido para milissegundos com casas decimais. Essa escolha é mais adequada para benchmark do que `System.currentTimeMillis()`, pois permite medir intervalos muito pequenos com maior precisão.

Cada estratégia é executada **3 vezes**, após um warmup descartado. Os gráficos usam principalmente a **mediana**, pois ela reduz o impacto de variações pontuais.

### Métricas coletadas

| Métrica | Descrição |
|---|---|
| Ocorrências | Quantidade de vezes que a palavra buscada apareceu no texto. |
| Tempo de execução | Tempo da contagem em milissegundos. |
| Speedup | `tempo_serial / tempo_metodo`. Indica quantas vezes o método foi mais rápido que o serial. |
| Eficiência paralela | `speedup / número_de_threads`. Mede o aproveitamento do paralelismo. |
| Throughput | `total_de_palavras / tempo_ms`. Mede palavras processadas por milissegundo. |
| Preparation time | Tempo de preparação de dados, usado principalmente na GPU. |
| Kernel time | Tempo de execução/leitura associado ao kernel OpenCL. |

O CSV gerado fica em:

```text
results/resultados.csv
```

E possui colunas como:

```csv
file,world,total_words,word_searched,strategy_id,method,family,parallelism,run,occurrences,time_ms,preparation_ms,kernel_ms,speedup,efficiency,words_per_ms,is_real_gpu
```

---

## Resultados e discussão

Os resultados apresentados nesta seção foram gerados a partir do arquivo `results/resultados.csv`, produzido automaticamente pelo benchmark da aplicação. Como o desempenho pode variar de acordo com o hardware, número de núcleos disponíveis, estado da JVM, driver OpenCL, disponibilidade de GPU e carga do sistema operacional, os valores devem ser interpretados como resultados obtidos no ambiente de teste utilizado.

A análise foi organizada em torno dos principais objetivos do trabalho: comparar métodos seriais, paralelos em CPU e paralelos em GPU; investigar o impacto do número de threads; observar a influência do tamanho das entradas; avaliar a estabilidade estatística das execuções; discutir a granularidade das tarefas; e analisar o custo de uso da GPU em diferentes representações de dados.

---

### 1. Comparação direta entre métodos

![Tempo mediano por método](charts/chart_01_tempo_mediano.png)

O gráfico de tempo mediano apresenta uma comparação direta entre as principais estratégias implementadas. O `SerialCPU` foi utilizado como baseline, pois representa a abordagem mais simples: percorrer o vetor de palavras sequencialmente e contar as ocorrências da palavra-alvo.

Os resultados mostram que o método serial é estável e previsível, mas não foi o mais rápido. As estratégias paralelas em CPU apresentaram redução significativa no tempo de execução, especialmente as variações baseadas em `ParallelCPU`, `ForkJoin`, `ParallelStream` e `Virtual Threads`. Isso indica que a tarefa de contagem de palavras possui uma parte altamente paralelizável: diferentes trechos do texto podem ser processados independentemente e seus resultados parciais podem ser somados ao final.

Entretanto, o melhor método não foi sempre o mesmo para todas as obras. Isso mostra que o desempenho não depende apenas do algoritmo, mas também do tamanho da entrada, da distribuição dos dados, da granularidade das tarefas e do overhead de cada estratégia.

![Speedup em relação ao SerialCPU](charts/chart_02_speedup_vs_serial.png)

O gráfico de speedup reforça essa interpretação. O speedup mede quantas vezes um método foi mais rápido que o `SerialCPU`, usando a fórmula:

```text
speedup = tempo_serial / tempo_metodo
```

Valores maiores que 1 indicam ganho de desempenho em relação ao serial. Em algumas amostras, as estratégias paralelas em CPU alcançaram speedups superiores a 3x, demonstrando que a paralelização foi vantajosa.

Na execução registrada no CSV atual, os melhores resultados observados por obra foram:

| Obra        |              Melhor método | Tempo mediano | Speedup aproximado |
| ----------- | -------------------------: | ------------: | -----------------: |
| Don Quixote |      `ForkJoinCPU-th10000` |     0,3891 ms |              3,48x |
| Dracula     |          `ParallelCPU-20t` |     0,1825 ms |              4,83x |
| Moby Dick   | `ParallelCPU-8t-dynChunks` |     0,2016 ms |              4,04x |

Esses resultados indicam que o paralelismo em CPU foi eficiente para as amostras analisadas. Porém, a variação do método vencedor reforça que não existe uma estratégia universalmente superior. O melhor desempenho depende do equilíbrio entre quantidade de trabalho, divisão das tarefas e custo de coordenação.

---

### 2. Impacto do número de threads

![Impacto do número de threads](charts/chart_03_threads_tempo.png)

O gráfico de impacto do número de threads mostra o tempo mediano do `ParallelCPU` ao variar a quantidade de threads. Essa análise atende diretamente ao requisito do trabalho de investigar o comportamento dos algoritmos sob diferentes configurações de processamento paralelo.

Em geral, aumentar o número de threads reduziu o tempo de execução, especialmente quando comparado ao baseline de 1 thread. Isso ocorre porque o texto é dividido em partes menores, permitindo que múltiplos núcleos da CPU trabalhem simultaneamente.

No entanto, a redução do tempo não acontece de forma perfeitamente linear. Isso é esperado, pois o paralelismo introduz custos adicionais, como:

* divisão do vetor de palavras em chunks;
* criação ou gerenciamento de tarefas;
* escalonamento das threads pelo sistema operacional;
* acesso concorrente à memória;
* soma dos resultados parciais;
* overhead da própria infraestrutura de execução.

Portanto, mesmo que a contagem seja uma tarefa paralelizável, o ganho real depende do custo de coordenar o trabalho paralelo.

![Speedup do ParallelCPU por número de threads](charts/chart_04_threads_speedup.png)

O gráfico de speedup por número de threads mostra que o ganho cresce conforme mais threads são usadas, mas fica abaixo da linha ideal. A linha ideal representaria um crescimento perfeitamente linear: por exemplo, 2 threads gerariam 2x de speedup, 4 threads gerariam 4x, e assim por diante.

Na prática, o speedup real fica abaixo desse limite porque parte da execução não é paralelizável ou sofre overhead. Esse comportamento é comum em benchmarks paralelos e evidencia que adicionar mais threads não garante ganho proporcional.

![Eficiência paralela](charts/chart_05_amdahl_efficiency.png)

A eficiência paralela foi calculada como:

```text
eficiência = speedup / número_de_threads
```

Esse gráfico mostra que a eficiência tende a diminuir conforme o número de threads aumenta. Isso significa que, embora o tempo total possa melhorar, cada thread adicional contribui proporcionalmente menos para o desempenho.

Esse resultado se relaciona com a Lei de Amdahl, que afirma que o ganho máximo de um programa paralelo é limitado pela fração do programa que permanece sequencial. No caso deste trabalho, mesmo que a contagem em si seja paralelizável, ainda existem etapas de coordenação, divisão e agregação que limitam o ganho.

Assim, os resultados indicam que o paralelismo em CPU é vantajoso, mas precisa ser bem configurado. Usar mais threads pode melhorar o desempenho até certo ponto, mas também pode gerar overhead e perda de eficiência.

---

### 3. Impacto do tamanho do texto

![Escalabilidade por tamanho do texto](charts/chart_06_escala_tamanho.png)

O gráfico de escalabilidade mostra o comportamento dos métodos conforme o tamanho do texto aumenta. As obras utilizadas possuem quantidades diferentes de palavras, o que permite observar como cada estratégia responde ao aumento do volume de dados.

Esse gráfico é importante porque um método pode ser rápido em textos menores, mas escalar mal em textos maiores. Estratégias mais eficientes tendem a apresentar uma curva com inclinação menor, indicando que o tempo cresce de forma mais controlada conforme o tamanho da entrada aumenta.

Os resultados mostram que as estratégias paralelas em CPU escalaram melhor que o método serial. Isso ocorre porque, à medida que o volume de palavras cresce, há mais trabalho disponível para ser distribuído entre threads. Dessa forma, o overhead da paralelização tende a ser mais compensado em entradas maiores.

![Tempo normalizado por 100 mil palavras](charts/chart_07_tempo_normalizado.png)

O gráfico de tempo normalizado por 100 mil palavras complementa a análise anterior. Como as obras têm tamanhos diferentes, comparar apenas o tempo bruto pode gerar interpretações injustas: textos maiores naturalmente tendem a demorar mais.

A métrica normalizada calcula aproximadamente quanto tempo cada método levaria para processar um mesmo volume de palavras:

```text
tempo_normalizado = tempo_ms / total_palavras * 100000
```

Com isso, é possível comparar a eficiência relativa dos métodos independentemente do tamanho absoluto de cada obra. Os resultados mostram que as estratégias paralelas mais ajustadas mantiveram melhor desempenho relativo, enquanto métodos com maior overhead, como algumas versões de GPU, apresentaram custo proporcional mais alto.

---

### 4. Estabilidade das execuções

![Tempo das 3 execuções individuais](charts/chart_08_tres_execucoes.png)

O enunciado exige pelo menos três execuções por método. Este gráfico apresenta as três medições individuais, permitindo avaliar a reprodutibilidade dos resultados.

Quando as barras de um mesmo método ficam próximas, isso indica estabilidade. Quando há variação significativa, pode haver influência de fatores como:

* aquecimento da JVM;
* otimizações do JIT Compiler;
* escalonamento de threads;
* carga do sistema operacional;
* variação no acesso à memória;
* overhead variável em métodos mais complexos.

A presença de três execuções permite reduzir a chance de tirar conclusões a partir de uma medição isolada.

![Média e desvio padrão](charts/chart_09_media_desvio.png)

O gráfico de média com desvio padrão complementa a análise de estabilidade. A média mostra o comportamento geral de cada método, enquanto o desvio padrão indica o quanto os tempos oscilaram entre as execuções.

Métodos com menor desvio padrão são mais previsíveis. Métodos com maior variação podem ser mais sensíveis ao ambiente de execução ou ao overhead interno da estratégia. Isso é especialmente relevante em métodos que envolvem maior complexidade, como GPU/OpenCL, virtual threads ou estratégias com muitas tarefas.

---

### 5. Granularidade do paralelismo em CPU

![Granularidade de chunks](charts/chart_11_chunks.png)

Além de variar o número de threads, o projeto também testou diferentes formas de dividir o trabalho. Essa decisão é importante porque o desempenho paralelo não depende apenas de quantas threads são usadas, mas também de como o texto é particionado.

No caso do `ParallelCPU`, foram testadas estratégias com chunks fixos e dinâmicos:

* `ParallelCPU-8t-32chunks`;
* `ParallelCPU-8t-128chunks`;
* `ParallelCPU-8t-dynChunks`.

Chunks maiores reduzem overhead, pois geram menos tarefas, mas podem distribuir mal o trabalho. Chunks menores podem melhorar o balanceamento, mas aumentam o custo de gerenciamento das tarefas. A estratégia dinâmica tenta equilibrar esses dois fatores calculando a quantidade de chunks de acordo com o tamanho da entrada.

Os resultados mostram que a granularidade tem impacto real no desempenho. Em algumas amostras, chunks dinâmicos foram mais competitivos, sugerindo que adaptar a divisão ao tamanho do texto pode ser melhor do que usar uma configuração fixa.

![Thresholds do ForkJoin](charts/chart_12_forkjoin_thresholds.png)

O mesmo princípio aparece no `ForkJoin`. Nessa estratégia, o texto é dividido recursivamente até que cada subtarefa atinja um tamanho mínimo definido pelo threshold.

Thresholds menores criam mais tarefas, aumentando o potencial de paralelismo, mas também aumentam overhead. Thresholds maiores reduzem a quantidade de tarefas, mas podem limitar a distribuição do trabalho entre os núcleos.

Foram testadas versões com thresholds fixos e dinâmico:

* `ForkJoinCPU-th2000`;
* `ForkJoinCPU-th10000`;
* `ForkJoinCPU-dyn`.

Os resultados indicam que não existe um threshold ideal para todos os textos. Isso reforça uma conclusão importante do benchmark: a granularidade é uma variável crítica em métodos paralelos e precisa ser ajustada conforme o tamanho e o comportamento da entrada.

---

### 6. GPU/OpenCL: comparação entre String, Hash e CPU

![CPU vs GPU String, GPU Hash e GPU HashReduction](charts/chart_10_gpu_vs_cpu.png)

A execução em GPU foi uma das partes mais complexas do projeto. Foram implementadas três abordagens principais:

* `ParallelGPU-String`;
* `ParallelGPU-Hash`;
* `ParallelGPU-HashReduction`.

A versão `GPU String` representa a abordagem mais direta. Nela, o texto é convertido para bytes, e o kernel OpenCL recebe:

* um buffer com os caracteres das palavras;
* um vetor de offsets;
* um vetor com os tamanhos das palavras;
* a palavra-alvo em bytes;
* um vetor de resultados.

Cada work-item da GPU verifica uma palavra comparando seus caracteres com a palavra buscada. Essa abordagem é mais fiel à comparação textual, mas é menos eficiente para GPU, pois envolve:

* palavras com tamanhos diferentes;
* acessos indiretos por offset;
* comparação caractere por caractere;
* divergência entre work-items;
* maior volume de dados textuais transferidos.

A GPU é mais eficiente quando executa operações simples, uniformes e repetidas em grande escala. A comparação direta de strings não se encaixa perfeitamente nesse perfil, pois cada palavra pode exigir um número diferente de comparações.

Já a versão `GPU Hash` transforma cada palavra em uma representação numérica:

```text
hashCode + length
```

Nesse caso, o kernel não compara caracteres diretamente. Ele apenas verifica se o hash e o tamanho da palavra coincidem com o hash e o tamanho da palavra buscada. Isso torna a operação muito mais simples para a GPU, pois cada work-item executa comparações inteiras e uniformes.

Essa mudança reduz o custo do kernel e diminui a complexidade da tarefa enviada à GPU. Por isso, a `GPU Hash` tende a apresentar desempenho melhor que a `GPU String`.

A versão `GPU HashReduction` acrescenta uma otimização adicional. Em vez de retornar um resultado individual para cada palavra, a GPU realiza uma redução parcial no próprio kernel OpenCL. Assim, cada grupo de trabalho soma parte das ocorrências e retorna apenas resultados parciais para a CPU. Isso reduz o volume de dados transferido da GPU de volta para a CPU.

Em resumo:

| Estratégia          | Vantagem                                       | Desvantagem                                          |
| ------------------- | ---------------------------------------------- | ---------------------------------------------------- |
| `GPU String`        | Comparação textual direta e mais fiel          | Kernel mais pesado, acesso irregular, maior overhead |
| `GPU Hash`          | Comparação numérica simples e mais rápida      | Possibilidade teórica de colisão                     |
| `GPU Hash + length` | Reduz risco de colisão e mantém kernel simples | Ainda não é uma verificação textual completa         |
| `GPU HashReduction` | Reduz o volume de dados retornado à CPU        | Exige kernel mais elaborado e etapa de redução       |

Apesar das otimizações por hash e redução parcial, a GPU não superou as melhores estratégias paralelas em CPU nas amostras analisadas. Isso sugere que, para textos desse tamanho e para uma tarefa simples de contagem, o overhead da GPU ainda pesa mais do que o ganho obtido com paralelismo massivo.

---

### 7. Decomposição do tempo da GPU

![Decomposição do tempo da GPU](charts/chart_13_gpu_prep_kernel.png)

A decomposição do tempo da GPU ajuda a entender por que ela não foi a estratégia mais rápida.

O tempo da GPU pode ser dividido em duas partes principais:

1. **Preparação dos dados**
   Inclui conversão das palavras, cálculo de hashes ou bytes, criação de buffers e envio dos dados para a GPU.

2. **Kernel/leitura**
   Inclui a execução do kernel OpenCL e a leitura dos resultados de volta para a CPU.

Na versão `GPU String`, a preparação é mais custosa porque é necessário converter e enviar uma representação textual mais complexa: caracteres, offsets e tamanhos. Além disso, o kernel precisa comparar caracteres individualmente.

Na versão `GPU Hash`, a preparação ainda existe, mas a representação enviada é mais simples: arrays de inteiros com hashes e tamanhos. O kernel também se torna menor e mais uniforme.

Na versão `GPU HashReduction`, além da comparação numérica, parte da soma dos resultados é feita dentro da própria GPU. Essa abordagem reduz a quantidade de dados retornados para a CPU, atacando um dos gargalos observados nas versões anteriores.

Mesmo assim, o gráfico mostra que parte significativa do tempo total da GPU ainda está relacionada à preparação e transferência de dados. Isso explica por que a GPU não venceu a CPU: embora a GPU tenha grande capacidade de paralelismo, o custo de preparar e movimentar os dados pode ser alto demais para uma tarefa simples e para amostras de tamanho limitado.

Essa análise mostra um ponto importante sobre paralelismo: acelerar uma tarefa não depende apenas da quantidade de núcleos disponíveis, mas também do custo de comunicação entre as unidades de processamento.

---

### 8. Resumo dos melhores métodos

![Resumo do melhor método por amostra](charts/chart_14_best_method_summary.png)

O gráfico-resumo apresenta o melhor método encontrado para cada obra, considerando o tempo mediano das execuções. Ele é útil para visualizar que o vencedor não foi sempre o mesmo.

Isso reforça a principal conclusão do benchmark: a escolha da melhor estratégia depende do contexto. O tamanho do texto, a granularidade dos chunks, a quantidade de threads, o threshold do ForkJoin e o overhead da GPU influenciam diretamente os resultados.

Assim, o trabalho não apenas compara métodos, mas demonstra que a otimização de desempenho exige experimentação. Diferentes estratégias podem ser melhores em diferentes cenários, e o benchmark é necessário para identificar qual abordagem se adapta melhor a cada caso.

---

## Conclusão

O **GameText Analyzer** cumpriu o objetivo do trabalho ao implementar e comparar diferentes estratégias de busca de palavras em textos, incluindo abordagens seriais, paralelas em CPU e paralelas em GPU/OpenCL. A aplicação utilizou as amostras obrigatórias da atividade, executou múltiplas medições por método, registrou os resultados em CSV e gerou gráficos para análise comparativa.

Os resultados mostraram que o paralelismo em CPU foi a abordagem mais vantajosa para as amostras analisadas. Estratégias como `ParallelCPU`, `ForkJoin`, `ParallelStream` e `Virtual Threads` conseguiram reduzir o tempo de execução em relação ao `SerialCPU`, alcançando speedups significativos em várias obras. No entanto, o ganho não foi linear: a eficiência paralela diminuiu conforme o número de threads aumentou, evidenciando overhead de coordenação e limites descritos pela Lei de Amdahl.

A análise também mostrou que a granularidade das tarefas influencia diretamente o desempenho. Estratégias com chunks fixos, chunks dinâmicos e diferentes thresholds de ForkJoin apresentaram resultados distintos, demonstrando que a forma como o trabalho é dividido pode ser tão importante quanto a quantidade de threads utilizada.

A GPU/OpenCL foi implementada em diferentes versões para investigar seu comportamento. A versão `GPU String`, baseada em comparação textual direta, apresentou maior custo por exigir offsets, tamanhos variáveis e comparação caractere por caractere. A versão `GPU Hash` reduziu esse custo ao transformar a busca em uma comparação numérica baseada em `hashCode + length`, tornando o kernel mais simples e uniforme. A versão `GPU HashReduction` acrescentou redução parcial na GPU, diminuindo o volume de dados retornado para a CPU.

Mesmo com essas otimizações, a GPU não superou as melhores estratégias em CPU nas amostras analisadas, principalmente por causa do overhead de preparação, transferência de dados e leitura dos resultados. Esse resultado não invalida o uso de GPU; pelo contrário, mostra que o ganho da GPU depende fortemente da natureza do problema, do volume de dados e da relação entre custo de transferência e custo computacional.

Portanto, a principal conclusão do trabalho é que **paralelizar não garante automaticamente melhor desempenho**. A eficiência de uma estratégia depende do tamanho da entrada, da natureza da operação, da granularidade das tarefas, da arquitetura utilizada e do custo de comunicação entre CPU e GPU. Para este problema específico, as estratégias paralelas em CPU apresentaram o melhor equilíbrio entre simplicidade, desempenho e baixo overhead, enquanto a GPU se mostrou uma alternativa tecnicamente interessante, mas menos vantajosa para o volume e o tipo de processamento exigidos pelas amostras obrigatórias.

---

## Como executar

### Pré-requisitos

- Java 21 ou superior;
- `javac` no PATH;
- arquivos das amostras em `data/samples/`;
- opcionalmente, driver OpenCL instalado para execução real da GPU;
- `jocl-2.0.4.jar` em `lib/` para suporte JOCL.

### Executar interface gráfica

```bash
chmod +x build.sh
./build.sh
```

### Executar modo console

```bash
./build.sh --console
```

### Compilar manualmente

```bash
find src/main/java -name "*.java" | xargs javac --release 21 -d out -cp "out:lib/*"
java -cp "out:lib/*" gamelog.Main
```

---

## Estrutura do projeto

```text
GameTextAnalyzer/
├── src/main/java/gamelog/
│   ├── Main.java
│   ├── SampleConfig.java
│   ├── CsvReader.java
│   ├── benchmark/
│   │   ├── BenchmarkRunner.java
│   │   ├── BenchmarkResult.java
│   │   └── CsvWriter.java
│   ├── counters/
│   │   ├── WordCounter.java
│   │   ├── StrategyFamily.java
│   │   ├── StrategyRegistry.java
│   │   ├── SerialCPUCounter.java
│   │   ├── SerialStreamCounter.java
│   │   ├── ParallelCPUCounter.java
│   │   ├── ChunkedParallelCPUCounter.java
│   │   ├── ForkJoinCPUCounter.java
│   │   ├── ParallelStreamCounter.java
│   │   ├── VirtualThreadCounter.java
│   │   ├── ParallelGPUCounter.java
│   │   └── ParallelGPUHashCounter.java
│   ├── ui/
│   │   ├── MainWindow.java
│   │   ├── BenchmarkPanel.java
│   │   ├── ChartsPanel.java
│   │   ├── ChartGenerator.java
│   │   └── ...
│   └── utils/
│       └── FileUtils.java
├── data/samples/
├── results/resultados.csv
├── charts/
├── lib/
│   ├── flatlaf-3.5.4.jar
│   └── jocl-2.0.4.jar
└── build.sh
```

---

## Observações sobre GPU/OpenCL

Para que os métodos GPU sejam executados de verdade, é necessário que a máquina possua OpenCL configurado corretamente. O projeto utiliza JOCL como ponte entre Java e OpenCL.

Requisitos para GPU real:

- driver OpenCL instalado;
- dispositivo compatível com OpenCL;
- `jocl-2.0.4.jar` no classpath;
- bibliotecas nativas necessárias disponíveis no sistema.

Caso o ambiente não suporte OpenCL, o projeto pode usar fallback em CPU, identificado no nome do método e no CSV. Apenas resultados com `is_real_gpu = true` devem ser interpretados como execução real em GPU/OpenCL.

---

## Referências

- Java ExecutorService Documentation: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html
- Java Virtual Threads Documentation: https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html
- Java System.nanoTime Documentation: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#nanoTime()
- JOCL - Java bindings for OpenCL: https://github.com/gpu/JOCL
- OpenCL Specification - Khronos Group: https://www.khronos.org/opencl/
- Amdahl, G. M. *Validity of the Single Processor Approach to Achieving Large Scale Computing Capabilities*. AFIPS, 1967.

---

## Anexos

Os códigos-fonte completos estão disponíveis em `src/main/java/gamelog/`.

> **Link do projeto no GitHub:** https://github.com/raquelx99/GameTextAnalyzer  
