package gamelog.counters;

import org.jocl.*;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.jocl.CL.*;

/**
 * Variante otimizada da GPU Hash.
 *
 * Diferenças em relação a ParallelGPUHashCounter:
 *  1. usa hashCode + length como representação numérica dos tokens;
 *  2. reutiliza buffers OpenCL por array de tokens, evitando reenviar o texto em toda execução;
 *  3. faz redução parcial no próprio kernel, retornando apenas somas por workgroup
 *     em vez de retornar um inteiro para cada palavra.
 *
 * A estratégia original por string e a GPU Hash simples foram mantidas para comparação.
 */
public class ParallelGPUHashReductionCounter implements WordCounter {

    private static final int LOCAL_SIZE = 256;

    private static final String KERNEL_SRC =
            "__kernel void countHashReduce(\n" +
            "    __global const int* hashes,\n" +
            "    __global const int* lengths,\n" +
            "    const int targetHash,\n" +
            "    const int targetLen,\n" +
            "    const int total,\n" +
            "    __global int* partials,\n" +
            "    __local int* localSums)\n" +
            "{\n" +
            "    int gid = get_global_id(0);\n" +
            "    int lid = get_local_id(0);\n" +
            "    int group = get_group_id(0);\n" +
            "    int globalSize = get_global_size(0);\n" +
            "    int acc = 0;\n" +
            "    for (int i = gid; i < total; i += globalSize) {\n" +
            "        if (hashes[i] == targetHash && lengths[i] == targetLen) acc++;\n" +
            "    }\n" +
            "    localSums[lid] = acc;\n" +
            "    barrier(CLK_LOCAL_MEM_FENCE);\n" +
            "    for (int stride = get_local_size(0) / 2; stride > 0; stride >>= 1) {\n" +
            "        if (lid < stride) localSums[lid] += localSums[lid + stride];\n" +
            "        barrier(CLK_LOCAL_MEM_FENCE);\n" +
            "    }\n" +
            "    if (lid == 0) partials[group] = localSums[0];\n" +
            "}\n";

    // testar quebrar por linha
    private boolean fallbackMode;
    private boolean gpuDevice;
    private cl_context context;
    private cl_command_queue queue;
    private cl_program program;
    private cl_kernel kernel;

    private PreparedData prepared;
    private double lastPreparationMs;
    private double lastKernelMs;

    public ParallelGPUHashReductionCounter() {
        this.fallbackMode = !tryInit();
    }

    private boolean tryInit() {
        try {
            setExceptionsEnabled(false);
            int[] nPlatforms = new int[1];
            if (clGetPlatformIDs(0, null, nPlatforms) != CL_SUCCESS || nPlatforms[0] == 0) {
                return fail("Nenhuma plataforma OpenCL encontrada");
            }
            cl_platform_id[] platforms = new cl_platform_id[nPlatforms[0]];
            clGetPlatformIDs(platforms.length, platforms, null);

            int[] nDevices = new int[1];
            long devType = CL_DEVICE_TYPE_GPU;
            if (clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_GPU, 0, null, nDevices) != CL_SUCCESS || nDevices[0] == 0) {
                if (clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_CPU, 0, null, nDevices) != CL_SUCCESS || nDevices[0] == 0) {
                    return fail("Nenhum dispositivo OpenCL encontrado");
                }
                devType = CL_DEVICE_TYPE_CPU;
            }
            gpuDevice = (devType == CL_DEVICE_TYPE_GPU);
            cl_device_id[] devices = new cl_device_id[nDevices[0]];
            clGetDeviceIDs(platforms[0], devType, devices.length, devices, null);
            setExceptionsEnabled(true);

            int[] err = new int[1];
            context = clCreateContext(null, 1, devices, null, null, err);
            queue = clCreateCommandQueueWithProperties(context, devices[0], null, err);
            program = clCreateProgramWithSource(context, 1, new String[]{KERNEL_SRC}, null, null);
            int build = clBuildProgram(program, 0, null, null, null, null);
            if (build != CL_SUCCESS) {
                return fail("Falha ao compilar kernel OpenCL: " + build);
            }
            kernel = clCreateKernel(program, "countHashReduce", null);
            System.out.println("[ParallelGPUHashReduction] OpenCL inicializado. Dispositivo: " + (gpuDevice ? "GPU" : "CPU OpenCL"));
            return true;
        } catch (Throwable e) {
            return fail("Falha na inicialização OpenCL: " + e.getMessage());
        }
    }

    private boolean fail(String reason) {
        System.out.println("[ParallelGPUHashReduction] Usando fallback CPU - " + reason);
        return false;
    }

    @Override
    public long count(String[] lines, String word) {
        if (lines.length == 0) {
            lastPreparationMs = 0;
            lastKernelMs = 0;
            return 0;
        }
        if (fallbackMode) {
            return fallback(lines, word);
        }
        try {
            return gpuCount(lines, word);
        } catch (Throwable e) {
            fallbackMode = true;
            System.err.println("[ParallelGPUHashReduction] Erro OpenCL, usando fallback: " + e.getMessage());
            releasePrepared();
            return fallback(lines, word);
        }
    }

    private long gpuCount(String[] lines, String word) {
        PreparedData data = prepare(lines, true);
        int targetHash = word.hashCode();
        int targetLen = word.length();

        int groups = Math.max(1, (int) Math.ceil(data.total / (double) LOCAL_SIZE));
        long globalSize = (long) groups * LOCAL_SIZE;
        int[] partials = new int[groups];
        cl_mem bufPartials = clCreateBuffer(context, CL_MEM_WRITE_ONLY, (long) groups * Integer.BYTES, null, null);

        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(data.bufHashes));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(data.bufLengths));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{targetHash}));
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{targetLen}));
        clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{data.total}));
        clSetKernelArg(kernel, 5, Sizeof.cl_mem, Pointer.to(bufPartials));
        clSetKernelArg(kernel, 6, (long) LOCAL_SIZE * Integer.BYTES, null);

        long kernelStart = System.nanoTime();
        clEnqueueNDRangeKernel(queue, kernel, 1, null, new long[]{globalSize}, new long[]{LOCAL_SIZE}, 0, null, null);
        clEnqueueReadBuffer(queue, bufPartials, CL_TRUE, 0, (long) groups * Integer.BYTES, Pointer.to(partials), 0, null, null);
        lastKernelMs = (System.nanoTime() - kernelStart) / 1_000_000.0;

        long count = 0;
        for (int p : partials) count += p;
        clReleaseMemObject(bufPartials);
        return count;
    }

    private long fallback(String[] lines, String word) {
        PreparedData data = prepare(lines, false);
        int targetHash = word.hashCode();
        int targetLen = word.length();

        long kernelStart = System.nanoTime();
        long count = IntStream.range(0, data.total).parallel()
                .filter(i -> data.hashes[i] == targetHash && data.lengths[i] == targetLen)
                .count();
        lastKernelMs = (System.nanoTime() - kernelStart) / 1_000_000.0;
        return count;
    }

    private PreparedData prepare(String[] lines, boolean deviceBuffers) {
        if (prepared != null && prepared.source == lines && prepared.deviceBuffers == deviceBuffers) {
            lastPreparationMs = 0.0;
            return prepared;
        }

        long start = System.nanoTime();
        releasePrepared();

        int[] hashes = new int[lines.length];
        int[] lengths = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            hashes[i] = lines[i].hashCode();
            lengths[i] = lines[i].length();
        }

        PreparedData data = new PreparedData();
        data.source = lines;
        data.total = lines.length;
        data.hashes = hashes;
        data.lengths = lengths;
        data.deviceBuffers = deviceBuffers;

        if (deviceBuffers) {
            data.bufHashes = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    (long) lines.length * Integer.BYTES, Pointer.to(hashes), null);
            data.bufLengths = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    (long) lines.length * Integer.BYTES, Pointer.to(lengths), null);
        }

        prepared = data;
        lastPreparationMs = (System.nanoTime() - start) / 1_000_000.0;
        return prepared;
    }

    private void releasePrepared() {
        if (prepared != null) {
            if (prepared.bufHashes != null) clReleaseMemObject(prepared.bufHashes);
            if (prepared.bufLengths != null) clReleaseMemObject(prepared.bufLengths);
            prepared = null;
        }
    }

    public void release() {
        releasePrepared();
        if (kernel != null) clReleaseKernel(kernel);
        if (program != null) clReleaseProgram(program);
        if (queue != null) clReleaseCommandQueue(queue);
        if (context != null) clReleaseContext(context);
    }

    @Override public String getName() { return fallbackMode ? "ParallelGPU-HashReduction-FallbackCPU" : "ParallelGPU-HashReduction"; }
    @Override public StrategyFamily getFamily() { return StrategyFamily.GPU_OPENCL; }
    @Override public int getParallelism() { return 0; }
    @Override public boolean isRealGpu() { return !fallbackMode && gpuDevice; }
    @Override public double getLastPreparationMs() { return lastPreparationMs; }
    @Override public double getLastKernelMs() { return lastKernelMs; }

    private static final class PreparedData {
        String[] source;
        int total;
        int[] hashes;
        int[] lengths;
        boolean deviceBuffers;
        cl_mem bufHashes;
        cl_mem bufLengths;
    }
}
