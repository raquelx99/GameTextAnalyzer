package gamelog.counters;

import org.jocl.*;
import static org.jocl.CL.*;

/**
 * Parallel GPU counter via OpenCL (JOCL 2.0.4).
 *
 * Fallback: se JOCL não estiver disponível ou não houver dispositivo OpenCL,
 * usa parallel stream automaticamente e registra o motivo.
 */
public class ParallelGPUCounter implements WordCounter {

    /** Kernel OpenCL: cada work-item verifica uma linha contra a palavra-alvo. */
    private static final String KERNEL_SRC =
        "__kernel void countWords(\n"                                    +
        "    __global const char* text,\n"                              +
        "    __global const int*  offsets,\n"                           +
        "    __global const int*  lengths,\n"                           +
        "    __global const char* target,\n"                            +
        "    const int targetLen,\n"                                    +
        "    const int totalLines,\n"                                   +
        "    __global int* results)\n"                                  +
        "{\n"                                                           +
        "    int gid = get_global_id(0);\n"                            +
        "    if (gid >= totalLines) { return; }\n"                     +
        "    int len = lengths[gid];\n"                                +
        "    if (len != targetLen) { results[gid] = 0; return; }\n"   +
        "    int off = offsets[gid];\n"                                +
        "    for (int i = 0; i < len; i++) {\n"                       +
        "        if (text[off+i] != target[i]) { results[gid]=0; return; }\n" +
        "    }\n"                                                       +
        "    results[gid] = 1;\n"                                      +
        "}\n";

    // ── Estado OpenCL (inicializado uma vez, reutilizado) ─────────────────
    private boolean       fallbackMode;
    private cl_context    context;
    private cl_command_queue queue;
    private cl_program    program;
    private cl_kernel     kernel;
    private boolean       gpuDevice; // true = GPU real, false = CPU OpenCL
    private double        lastPreparationMs;
    private double        lastKernelMs;

    public ParallelGPUCounter() {
        this.fallbackMode = !tryInit();
    }

    /**
     * Inicializa contexto, fila, programa e kernel uma única vez.
     * Retorna false se qualquer etapa falhar - o caller ativa o fallback.
     */
    private boolean tryInit() {
        try {
            setExceptionsEnabled(false);

            // Descoberta de plataforma
            int[] nPlatforms = new int[1];
            if (clGetPlatformIDs(0, null, nPlatforms) != CL_SUCCESS || nPlatforms[0] == 0)
                return fail("Nenhuma plataforma OpenCL encontrada");
            cl_platform_id[] platforms = new cl_platform_id[nPlatforms[0]];
            clGetPlatformIDs(platforms.length, platforms, null);

            // Prefere GPU; aceita CPU como fallback OpenCL
            int[] nDevices = new int[1];
            long devType = CL_DEVICE_TYPE_GPU;
            if (clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_GPU, 0, null, nDevices) != CL_SUCCESS
                    || nDevices[0] == 0) {
                if (clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_CPU, 0, null, nDevices) != CL_SUCCESS
                        || nDevices[0] == 0)
                    return fail("Nenhum dispositivo OpenCL encontrado");
                devType = CL_DEVICE_TYPE_CPU;
            }
            gpuDevice = (devType == CL_DEVICE_TYPE_GPU);

            cl_device_id[] devices = new cl_device_id[nDevices[0]];
            clGetDeviceIDs(platforms[0], devType, devices.length, devices, null);
            setExceptionsEnabled(true);

            // Contexto e fila de comandos - criados uma vez
            int[] err = new int[1];
            context = clCreateContext(null, 1, devices, null, null, err);
            queue   = clCreateCommandQueueWithProperties(context, devices[0], null, err);

            // Programa e kernel - compilados uma vez
            program = clCreateProgramWithSource(context, 1, new String[]{KERNEL_SRC}, null, null);
            clBuildProgram(program, 0, null, null, null, null);
            kernel  = clCreateKernel(program, "countWords", null);

            System.out.println("[ParallelGPU] OpenCL inicializado. Dispositivo: "
                    + (gpuDevice ? "GPU" : "CPU OpenCL"));
            return true;

        } catch (Throwable e) {
            return fail("Falha na inicialização OpenCL: " + e.getMessage());
        }
    }

    private boolean fail(String reason) {
        System.out.println("[ParallelGPU] Usando fallback CPU - " + reason);
        return false;
    }

    // ── count() ────────────────────────────────────────────────────────────

    @Override
    public long count(String[] lines, String word) {
        if (fallbackMode || lines.length == 0) {
            if (!fallbackMode) return 0;
            return fallback(lines, word);
        }
        try {
            return gpuCount(lines, word);
        } catch (Exception e) {
            fallbackMode = true;
            System.err.println("[ParallelGPU] Erro OpenCL, usando fallback: " + e.getMessage());
            return fallback(lines, word);
        }
    }

    private long gpuCount(String[] lines, String word) {
        int totalLines = lines.length;
        long prepStart = System.nanoTime();
        byte[] wordBytes = word.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        int[] offsets = new int[totalLines];
        int[] lengths = new int[totalLines];
        int total = 0;
        byte[][] encoded = new byte[totalLines][];
        for (int i = 0; i < totalLines; i++) {
            offsets[i] = total;
            byte[] b = lines[i].getBytes(java.nio.charset.StandardCharsets.UTF_8);
            encoded[i] = b;
            lengths[i] = b.length;
            total += b.length;
        }
        byte[] textBuf = new byte[Math.max(total, 1)];
        int pos = 0;
        for (byte[] b : encoded) {
            System.arraycopy(b, 0, textBuf, pos, b.length);
            pos += b.length;
        }
        byte[] wordBuf = wordBytes.length > 0 ? wordBytes : new byte[1];

        cl_mem bufText   = clCreateBuffer(context, CL_MEM_READ_ONLY  | CL_MEM_COPY_HOST_PTR, textBuf.length, Pointer.to(textBuf), null);
        cl_mem bufWord   = clCreateBuffer(context, CL_MEM_READ_ONLY  | CL_MEM_COPY_HOST_PTR, wordBuf.length, Pointer.to(wordBuf), null);
        cl_mem bufOff    = clCreateBuffer(context, CL_MEM_READ_ONLY  | CL_MEM_COPY_HOST_PTR, (long) totalLines * Integer.BYTES, Pointer.to(offsets), null);
        cl_mem bufLen    = clCreateBuffer(context, CL_MEM_READ_ONLY  | CL_MEM_COPY_HOST_PTR, (long) totalLines * Integer.BYTES, Pointer.to(lengths), null);
        cl_mem bufResult = clCreateBuffer(context, CL_MEM_WRITE_ONLY, (long) totalLines * Integer.BYTES, null, null);
        lastPreparationMs = (System.nanoTime() - prepStart) / 1_000_000.0;

        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(bufText));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(bufOff));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(bufLen));
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(bufWord));
        clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{wordBytes.length}));
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{totalLines}));
        clSetKernelArg(kernel, 6, Sizeof.cl_mem, Pointer.to(bufResult));

        long kernelStart = System.nanoTime();
        clEnqueueNDRangeKernel(queue, kernel, 1, null, new long[]{totalLines}, null, 0, null, null);
        int[] resultArr = new int[totalLines];
        clEnqueueReadBuffer(queue, bufResult, CL_TRUE, 0,
                (long) totalLines * Integer.BYTES, Pointer.to(resultArr), 0, null, null);
        lastKernelMs = (System.nanoTime() - kernelStart) / 1_000_000.0;

        long count = 0;
        for (int r : resultArr) count += r;

        clReleaseMemObject(bufText);
        clReleaseMemObject(bufWord);
        clReleaseMemObject(bufOff);
        clReleaseMemObject(bufLen);
        clReleaseMemObject(bufResult);

        return count;
    }

    /** Libera todos os recursos OpenCL. Chamar ao encerrar o benchmark. */
    public void release() {
        if (kernel  != null) clReleaseKernel(kernel);
        if (program != null) clReleaseProgram(program);
        if (queue   != null) clReleaseCommandQueue(queue);
        if (context != null) clReleaseContext(context);
    }

    private long fallback(String[] lines, String word) {
        lastPreparationMs = 0;
        long t0 = System.nanoTime();
        long count = java.util.Arrays.stream(lines).parallel().filter(word::equals).count();
        lastKernelMs = (System.nanoTime() - t0) / 1_000_000.0;
        return count;
    }

    @Override public String getName()          { return fallbackMode ? "ParallelGPU-String-FallbackCPU" : "ParallelGPU-String"; }
    @Override public StrategyFamily getFamily(){ return StrategyFamily.GPU_OPENCL; }
    @Override public int getParallelism()      { return 0; }
    @Override public boolean isRealGpu()       { return !fallbackMode && gpuDevice; }
    @Override public double getLastPreparationMs() { return lastPreparationMs; }
    @Override public double getLastKernelMs() { return lastKernelMs; }
}
