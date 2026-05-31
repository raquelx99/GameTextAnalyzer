# 🎮 GameText Analyzer
### Análise Comparativa de Algoritmos com Uso de Paralelismo em Textos Literários para Jogos Narrativos

---

## Resumo

O **GameText Analyzer** é uma ferramenta Java para fazer benchmark modular de estratégias de busca/contagem de palavras em arquivos `.txt`. Além das abordagens base **SerialCPU**, **ParallelCPU** e **ParallelGPU/OpenCL**, a versão atual inclui estratégias adicionais para enriquecer a comparação: **SerialStream**, **ParallelStream**, **ForkJoinCPU** e **VirtualThreads**. A versão final do projeto utiliza obrigatoriamente as amostras fornecidas na atividade: **Don Quixote**, **Dracula** e **Moby Dick**.

Para manter a proposta relacionada a jogos digitais, cada obra foi interpretada como um **mundo narrativo** que poderia servir de base para uma adaptação em jogo: *Don Quixote* como um RPG de cavalaria, *Dracula* como um survival horror gótico e *Moby Dick* como uma aventura naval/boss hunt. A contagem de palavras representa uma análise simples de recorrência de elementos narrativos importantes, enquanto os métodos seriais e paralelos são comparados em desempenho.

---

## Introdução

Jogos narrativos frequentemente se inspiram em obras literárias para criar mundos, personagens, missões, atmosferas e conflitos. Antes de adaptar uma obra para um jogo, uma equipe poderia analisar termos recorrentes para entender quais elementos aparecem com mais força no texto.

Neste trabalho, essa análise textual é usada como contexto para o benchmark de paralelismo. O objetivo técnico continua sendo comparar diferentes formas de contar palavras em arquivos de texto, mas a interpretação dos dados foi gamificada.

### Estratégias escolhidas

A aplicação foi refatorada para funcionar como um **framework modular de benchmark**. Todas as estratégias implementam a interface `WordCounter` e são registradas em `StrategyRegistry`, permitindo adicionar novos métodos sem reescrever o benchmark, o CSV ou os gráficos.

| Família | Método | Descrição |
|---|---|---|
| Serial | `SerialCPU` | Percorre todas as palavras sequencialmente usando um loop simples. |
| Serial | `SerialStream` | Usa `Arrays.stream(...)` em modo sequencial, servindo para comparar loop manual com Stream API. |
| CPU paralela | `ParallelCPU-2t` | Divide o vetor de palavras entre 2 platform threads usando `ExecutorService`. |
| CPU paralela | `ParallelCPU-4t` | Divide o vetor de palavras entre 4 platform threads usando `ExecutorService`. |
| CPU paralela | `ParallelCPU-8t` | Divide o vetor de palavras entre 8 platform threads usando `ExecutorService`. |
| CPU paralela | `ParallelCPU-Nt` | Usa a quantidade de processadores disponíveis na máquina. |
| CPU paralela | `ForkJoinCPU` | Usa `ForkJoinPool` e divisão recursiva do vetor em subtarefas. |
| Stream paralela | `ParallelStream` | Usa `Arrays.stream(...).parallel()`, executando sobre o ForkJoinPool comum. |
| Virtual threads | `VirtualThreads-50chunks` | Divide o vetor em 50 partes e executa cada parte em uma virtual thread. |
| Virtual threads | `VirtualThreads-100chunks` | Testa uma granularidade maior, com 100 chunks em virtual threads. |
| GPU/OpenCL | `ParallelGPU` | Tenta executar a comparação na GPU com OpenCL/JOCL. Se JOCL/OpenCL não estiver disponível, usa fallback em CPU identificado como `ParallelGPU-FallbackCPU`. |

---

## Metodologia

### Amostras obrigatórias

As amostras oficiais ficam em:

```text
data/samples/
```

| Arquivo | Obra | Interpretação gamificada | Palavra padrão |
|---|---|---|---|
| `DonQuixote-388208.txt` | Don Quixote | RPG de Cavalaria e Aventura | `quijote` |
| `Dracula-165307.txt` | Dracula | Survival Horror Gótico | `blood` |
| `MobyDick-217452.txt` | Moby Dick | Aventura Naval / Boss Hunt | `whale` |

Também foram adicionadas sugestões de palavras por obra:

```text
Don Quixote: quijote, sancho, caballero, aventura, castillo, dulcinea
Dracula: dracula, vampire, blood, night, fear, castle, helsing
Moby Dick: whale, sea, ship, captain, ahab, harpoon, moby
```

> Observação: a versão atual não usa logs sintéticos no fluxo principal. Todos os benchmarks oficiais são feitos sobre as amostras em `data/samples/`.

### Funcionalidades da interface

A interface gráfica foi organizada em uma experiência mais moderna e próxima de um painel de análise:

- **Dashboard**: visão geral do projeto, atalhos e indicadores rápidos;
- **Benchmark**: execução oficial com estratégias seriais, CPU paralela, streams, virtual threads e GPU/OpenCL;
- **Explorar Textos**: tabela de frequência, distribuição das palavras e visualização dos tokens;
- **Comparador Narrativo**: comparação de palavras entre as três obras, com densidade por 10 mil palavras;
- **Gráficos**: visualização dos gráficos gerados a partir do CSV;
- **Amostras Oficiais**: documentação visual dos textos obrigatórios e da interpretação gamificada.

O **Comparador Narrativo** é uma funcionalidade adicional para reforçar o lado de jogos: ele permite comparar termos como `blood`, `whale`, `quijote`, `castle`, `ship` e `night` entre as obras, mostrando em qual “mundo narrativo” cada termo é mais representativo.

### Tokenização dos textos

Como as amostras são textos literários completos, e não logs com uma palavra por linha, o projeto agora faz uma etapa de tokenização antes da contagem.

A leitura do arquivo transforma o texto em um vetor de palavras normalizadas:

- converte tudo para minúsculas;
- remove pontuação;
- remove acentos;
- preserva letras e números relevantes para a busca;
- ignora espaços e linhas vazias.

Assim, uma frase como:

```text
The vampire's blood was cold.
```

passa a ser analisada como palavras individuais:

```text
the vampire s blood was cold
```

### Medição do tempo

O tempo é medido usando:

```java
System.nanoTime()
```

O valor é convertido para milissegundos com casas decimais. Essa escolha é mais adequada para benchmark do que `System.currentTimeMillis()`, porque permite medir intervalos pequenos com mais precisão.

A leitura/tokenização do arquivo acontece antes da medição principal. Assim, os tempos registrados representam principalmente o custo da contagem das palavras em memória.

### Configurações testadas

A versão atual testa múltiplas vertentes de paralelização:

- `SerialCPU` e `SerialStream`, para comparar duas formas seriais;
- `ParallelCPU-2t`, `ParallelCPU-4t`, `ParallelCPU-8t` e `ParallelCPU-Nt`, para testar o impacto do número de threads tradicionais;
- `ForkJoinCPU`, para avaliar uma abordagem divide-and-conquer;
- `ParallelStream`, para comparar com uma API paralela de alto nível do Java;
- `VirtualThreads-50chunks` e `VirtualThreads-100chunks`, para investigar se virtual threads ajudam ou atrapalham em uma tarefa CPU-bound;
- `ParallelGPU`, com OpenCL/JOCL, ou `ParallelGPU-FallbackCPU` quando o ambiente não possui GPU/OpenCL configurado.

Cada configuração é executada **3 vezes** após um warmup descartado. A mediana dos tempos é usada nos gráficos para reduzir o impacto de outliers.

> Observação: virtual threads exigem **Java 21 ou superior**. Elas foram incluídas justamente para enriquecer o benchmark, pois são muito relevantes no Java moderno, mas nem sempre são melhores em tarefas puramente CPU-bound como contagem em memória.

### Métricas coletadas

| Métrica | Fórmula / Descrição |
|---|---|
| Ocorrências | Quantidade de vezes que a palavra buscada aparece no texto. |
| Tempo de execução | Tempo da contagem em milissegundos. |
| Speedup | `Tempo Serial / Tempo Método` |
| Eficiência | `Speedup / paralelismo` para estratégias de CPU paralela e virtual threads. |
| Throughput | `Total de palavras / Tempo (ms)` |

---

## Resultados e Discussão

Os resultados são salvos em:

```text
results/resultados.csv
```

O CSV possui as seguintes colunas:

```csv
file,world,total_words,word_searched,strategy_id,method,family,parallelism,run,occurrences,time_ms,speedup,efficiency,words_per_ms,is_real_gpu
```

### Gráficos gerados

A aplicação gera um conjunto ampliado de gráficos para cobrir diretamente os pontos pedidos no enunciado: comparação serial/paralela, variação das amostras, análise estatística, impacto do número de threads e interpretação gamificada dos textos.

Na interface gráfica, os gráficos foram organizados por abas de categoria para facilitar a leitura:

- **Visão geral**: gráficos principais e estatísticos;
- **Paralelismo CPU**: impacto de threads, eficiência e estratégias de CPU;
- **Speedup e vazão**: speedup, throughput e melhor família;
- **GPU e virtual threads**: comparação com GPU/OpenCL, fallback e virtual threads;
- **Análise narrativa**: ranking dos mundos narrativos e densidade das palavras-tema.

Dentro de cada aba, os gráficos podem ser navegados pelos botões **Anterior** e **Próximo**, pelo seletor no canto superior ou pelas setas do teclado. Isso evita que a aba fique visualmente poluída mesmo com muitos gráficos.

| Gráfico | Arquivo | Para que serve |
|---|---|---|
| Tempo mediano por método | `charts/chart_median_time.png` | Compara SerialCPU, ParallelCPU e GPU por amostra. |
| Impacto do número de threads | `charts/chart_thread_impact.png` | Mostra como 2, 4, 8 e núcleos disponíveis afetam o ParallelCPU. |
| Speedup em relação ao SerialCPU | `charts/chart_speedup.png` | Mede o ganho ou perda de cada método em relação ao serial. |
| Throughput | `charts/chart_throughput.png` | Mostra quantas palavras cada método processa por milissegundo. |
| Variação entre as 3 execuções | `charts/chart_run_variation.png` | Evidencia as três amostras executadas por método. |
| Tempo médio com desvio padrão | `charts/chart_mean_stddev.png` | Reforça a análise estatística e a estabilidade dos tempos. |
| Impacto do tamanho do texto | `charts/chart_size_impact.png` | Relaciona total de palavras com tempo de execução. |
| Tempo normalizado por 100 mil palavras | `charts/chart_normalized_100k.png` | Permite comparar textos de tamanhos diferentes de forma mais justa. |
| Speedup da CPU paralela por threads | `charts/chart_cpu_speedup_threads.png` | Analisa especificamente o ganho do ParallelCPU conforme o número de threads. |
| Eficiência paralela | `charts/chart_parallel_efficiency.png` | Mostra o aproveitamento das threads, usando `speedup / threads`. |
| SerialCPU vs melhor ParallelCPU vs GPU | `charts/chart_cpu_vs_gpu.png` | Resume os principais competidores em cada amostra. |
| Ranking por mundo narrativo | `charts/chart_ranking_world.png` | Apresenta, de forma gamificada, o método vencedor em cada obra. |
| Densidade da palavra-tema | `charts/chart_word_density.png` | Mostra a frequência da palavra buscada a cada 10 mil palavras, conectando desempenho e análise narrativa. |
| Melhor desempenho por família | `charts/chart_best_by_family.png` | Compara a melhor estratégia de cada família: serial, CPU paralela, stream paralela, virtual threads e GPU. |
| Vertente CPU paralela | `charts/chart_cpu_strategy_branch.png` | Compara apenas as estratégias de CPU paralela, incluindo threads manuais, ForkJoin e ParallelStream. |
| Vertente Virtual Threads | `charts/chart_virtual_threads_branch.png` | Compara as granularidades de virtual threads, como 50 e 100 chunks. |
| Vertente GPU/OpenCL | `charts/chart_gpu_branch.png` | Compara GPU/fallback contra SerialCPU, melhor CPU paralela e melhor virtual thread. |

### Discussões esperadas

- Em textos menores, o `SerialCPU` pode ser competitivo porque não possui overhead de criação de threads.
- Em textos maiores, o `ParallelCPU` pode apresentar ganhos por dividir o vetor de palavras entre múltiplas threads.
- Aumentar o número de threads nem sempre melhora o desempenho, pois pode haver overhead de criação, escalonamento e sincronização.
- A GPU pode ter overhead alto por causa da transferência de dados e preparação do kernel OpenCL.
- Caso apareça `ParallelGPU-FallbackCPU`, significa que o ambiente não executou GPU real; nesse caso, os resultados devem ser interpretados como fallback em CPU paralela.

---

## Observação importante sobre GPU

A classe `ParallelGPUCounter` tenta usar OpenCL por meio do JOCL. Para execução real em GPU, é necessário ter:

- `jocl-2.0.4.jar` no classpath;
- bibliotecas nativas do JOCL (`.dll`, `.so` ou equivalente);
- driver OpenCL instalado;
- dispositivo OpenCL disponível.

Se esses requisitos não forem encontrados, o programa utiliza automaticamente um fallback em CPU com `parallelStream`, e o método aparece no CSV como:

```text
ParallelGPU-FallbackCPU
```

Portanto, somente resultados marcados como `ParallelGPU` representam execução real em GPU.

---

## Como executar

### Pré-requisitos

- Java 21 ou superior, necessário para as estratégias com Virtual Threads;
- `javac` no PATH;
- arquivos das amostras dentro de `data/samples/`.

### Compilar e executar no modo console

```bash
chmod +x build.sh
./build.sh
```

O script compila o projeto e abre o menu de console.

### Executar interface gráfica

```bash
find src/main/java -name "*.java" | xargs javac --release 21 -d out -cp "out:lib/*"
java -cp "out:lib/*" gamelog.Main
```

### Executar console manualmente

```bash
java -Djava.awt.headless=true -cp "out:lib/*" gamelog.Main --console
```

---

## Estrutura do projeto

```text
GameTextAnalyzer/
├── src/main/java/gamelog/
│   ├── Main.java                   # Ponto de entrada + menu console/GUI
│   ├── SampleConfig.java           # Configuração das amostras oficiais
│   ├── CsvReader.java              # Leitura do CSV para regenerar gráficos
│   ├── benchmark/
│   │   ├── BenchmarkRunner.java    # Orquestra runs, warmup e métricas
│   │   ├── BenchmarkResult.java    # Modelo de dados de um resultado
│   │   └── CsvWriter.java          # Escrita do CSV
│   ├── counters/
│   │   ├── WordCounter.java        # Interface comum das estratégias
│   │   ├── StrategyFamily.java     # Famílias: serial, CPU, virtual threads, GPU
│   │   ├── StrategyRegistry.java   # Registro central dos métodos testados
│   │   ├── SerialCPUCounter.java   # Loop sequencial
│   │   ├── SerialStreamCounter.java# Stream sequencial
│   │   ├── ParallelCPUCounter.java # ExecutorService + Future
│   │   ├── ForkJoinCPUCounter.java # ForkJoinPool recursivo
│   │   ├── ParallelStreamCounter.java # Stream paralela
│   │   ├── VirtualThreadCounter.java # Java 21 virtual threads
│   │   └── ParallelGPUCounter.java # OpenCL/JOCL com fallback
│   ├── ui/
│   │   └── ...                     # Interface gráfica e geração de gráficos
│   └── utils/
│       └── FileUtils.java          # Leitura e tokenização de arquivos
├── data/
│   └── samples/                    # Amostras obrigatórias da atividade
├── results/resultados.csv          # Resultados do benchmark
├── charts/                         # Gráficos PNG
├── lib/                            # Bibliotecas opcionais, como JOCL
└── build.sh                        # Script de compilação e execução no console
```

---

## Conclusão

O **GameText Analyzer** cumpre a proposta da atividade ao comparar algoritmos de contagem de palavras em ambientes serial, paralelo em CPU e GPU/OpenCL. A adaptação do projeto para usar as amostras obrigatórias fortalece a aderência ao enunciado, enquanto a camada de gamificação permanece na interpretação dos textos como mundos narrativos de possíveis jogos digitais.

A análise permite discutir não apenas qual método foi mais rápido, mas também quando o paralelismo compensa, como o número de threads afeta o desempenho e quais limitações aparecem no uso de GPU com OpenCL.

---

## Referências

- Java ExecutorService documentation: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html
- Java Virtual Threads documentation: https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html
- Java System.nanoTime documentation: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#nanoTime()
- JOCL — Java bindings for OpenCL: https://github.com/gpu/JOCL
- OpenCL Specification — Khronos Group: https://www.khronos.org/opencl/
- Amdahl, G. M. Validity of the Single Processor Approach to Achieving Large Scale Computing Capabilities. AFIPS, 1967.

---

## Anexos

Os códigos-fonte completos estão disponíveis nas pastas `src/main/java/gamelog/`.

> **Link do projeto:** https://github.com/SEU_USUARIO/GameTextAnalyzer *(substituir pelo link real)*

---

## Atualização: benchmark modular avançado

A aplicação foi expandida para funcionar como um benchmark modular de estratégias de paralelização. Além dos métodos principais do enunciado, foram adicionadas variações experimentais para enriquecer a análise:

- `ParallelCPU-8t-32chunks` e `ParallelCPU-8t-128chunks`: versões com chunks fixos para estudar granularidade de tarefas;
- `ParallelCPU-8t-dynChunks`: versão com chunks dinâmicos calculados a partir do tamanho do texto;
- `ForkJoinCPU-th2000`, `ForkJoinCPU-th10000` e `ForkJoinCPU-dyn`: versões com thresholds fixos e dinâmico;
- `ParallelGPU-String`: versão GPU/OpenCL baseada em comparação textual por bytes;
- `ParallelGPU-Hash`: versão GPU/OpenCL baseada em códigos inteiros (`hashCode` + tamanho da palavra), mantendo a versão por string para comparação;
- métricas extras `preparation_ms` e `kernel_ms`, úteis para analisar o custo de preparação/transferência de dados na GPU.

A aba de gráficos também passou a ter filtro por amostra. É possível gerar visualizações para todas as obras juntas ou apenas para `Don Quixote`, `Dracula` ou `Moby Dick`, facilitando a escrita do relatório por cenário.

### Observação sobre a GPU por hash

A versão `ParallelGPU-Hash` é uma estratégia experimental de otimização. Ela transforma cada palavra em um inteiro para reduzir o custo da comparação na GPU. Essa abordagem é mais adequada ao modelo de processamento da GPU, mas pode ter colisões teóricas de hash. Por isso, a versão `ParallelGPU-String` foi mantida como comparação mais fiel à busca textual exata.
