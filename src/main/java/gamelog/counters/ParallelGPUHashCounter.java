package gamelog.counters;

import org.jocl.*;
import static org.jocl.CL.*;

/**
 * GPU/OpenCL counter using integer hashes instead of string/byte comparison.
 *
 * It keeps the original string-based GPU strategy for comparison, but this
 * variant sends int hashes + token lengths to the device. This is much closer
 * to what GPUs handle efficiently: flat numeric arrays.
 *
 * Note: hash + length collisions are theoretically possible. For the official
 * samples this is acceptable as an experimental benchmark variant, and the
 * original exact string strategy remains available for correctness comparison.
 */
public class ParallelGPUHashCounter implements WordCounter {

    private static final String KERNEL_SRC =
        "__kernel void countHashes(\n" +
        "    __global const int* hashes,\n" +
        "    __global const int* lengths,\n" +
        "    const int targetHash,\n" +
        "    const int targetLen,\n" +
        "    const int total,\n" +
        "    __global int* results)\n" +
        "{\n" +
        "    int gid = get_global_id(0);\n" +
        "    if (gid >= total) return;\n" +
        "    results[gid] = (hashes[gid] == targetHash && lengths[gid] == targetLen) ? 1 : 0;\n" +
        "}\n";

    private boolean fallbackMode;
    private boolean gpuDevice;
    private cl_context context;
    private cl_command_queue queue;
    private cl_program program;
    private cl_kernel kernel;
    private double lastPreparationMs;
    private double lastKernelMs;

    public ParallelGPUHashCounter() {
        this.fallbackMode = !tryInit();
    }

    private boolean tryInit() {
        try {
            setExceptionsEnabled(false);
            int[] nPlatforms = new int[1];
            if (clGetPlatformIDs(0, null, nPlatforms) != CL_SUCCESS || nPlatforms[0] == 0)
                return fail("Nenhuma plataforma OpenCL encontrada");
            cl_platform_id[] platforms = new cl_platform_id[nPlatforms[0]];
            clGetPlatformIDs(platforms.length, platforms, null);

            int[] nDevices = new int[1];
            long devType = CL_DEVICE_TYPE_GPU;
            if (clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_GPU, 0, null, nDevices) != CL_SUCCESS || nDevices[0] == 0) {
                if (clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_CPU, 0, null, nDevices) != CL_SUCCESS || nDevices[0] == 0)
                    return fail("Nenhum dispositivo OpenCL encontrado");
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
            clBuildProgram(program, 0, null, null, null, null);
            kernel = clCreateKernel(program, "countHashes", null);
            System.out.println("[ParallelGPUHash] OpenCL inicializado. Dispositivo: " + (gpuDevice ? "GPU" : "CPU OpenCL"));
            return true;
        } catch (Throwable e) {
            return fail("Falha na inicialização OpenCL: " + e.getMessage());
        }
    }

    private boolean fail(String reason) {
        System.out.println("[ParallelGPUHash] Usando fallback CPU — " + reason);
        return false;
    }

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
            System.err.println("[ParallelGPUHash] Erro OpenCL, usando fallback: " + e.getMessage());
            return fallback(lines, word);
        }
    }

    private long gpuCount(String[] lines, String word) {
        int total = lines.length;
        long prepStart = System.nanoTime();
        int[] hashes = new int[total];
        int[] lengths = new int[total];
        for (int i = 0; i < total; i++) {
            hashes[i] = lines[i].hashCode();
            lengths[i] = lines[i].length();
        }
        int targetHash = word.hashCode();
        int targetLen = word.length();

        cl_mem bufHashes = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, (long) total * Integer.BYTES, Pointer.to(hashes), null);
        cl_mem bufLengths = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, (long) total * Integer.BYTES, Pointer.to(lengths), null);
        cl_mem bufResult = clCreateBuffer(context, CL_MEM_WRITE_ONLY, (long) total * Integer.BYTES, null, null);
        lastPreparationMs = (System.nanoTime() - prepStart) / 1_000_000.0;

        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(bufHashes));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(bufLengths));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{targetHash}));
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{targetLen}));
        clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{total}));
        clSetKernelArg(kernel, 5, Sizeof.cl_mem, Pointer.to(bufResult));

        long kernelStart = System.nanoTime();
        clEnqueueNDRangeKernel(queue, kernel, 1, null, new long[]{total}, null, 0, null, null);
        int[] resultArr = new int[total];
        clEnqueueReadBuffer(queue, bufResult, CL_TRUE, 0, (long) total * Integer.BYTES, Pointer.to(resultArr), 0, null, null);
        lastKernelMs = (System.nanoTime() - kernelStart) / 1_000_000.0;

        long count = 0;
        for (int v : resultArr) count += v;

        clReleaseMemObject(bufHashes);
        clReleaseMemObject(bufLengths);
        clReleaseMemObject(bufResult);
        return count;
    }

    private long fallback(String[] lines, String word) {
        long prepStart = System.nanoTime();
        int targetHash = word.hashCode();
        int targetLen = word.length();
        int[] hashes = new int[lines.length];
        int[] lengths = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            hashes[i] = lines[i].hashCode();
            lengths[i] = lines[i].length();
        }
        lastPreparationMs = (System.nanoTime() - prepStart) / 1_000_000.0;

        long kernelStart = System.nanoTime();
        long count = java.util.stream.IntStream.range(0, lines.length).parallel()
                .filter(i -> hashes[i] == targetHash && lengths[i] == targetLen)
                .count();
        lastKernelMs = (System.nanoTime() - kernelStart) / 1_000_000.0;
        return count;
    }

    public void release() {
        if (kernel != null) clReleaseKernel(kernel);
        if (program != null) clReleaseProgram(program);
        if (queue != null) clReleaseCommandQueue(queue);
        if (context != null) clReleaseContext(context);
    }

    @Override public String getName() { return fallbackMode ? "ParallelGPU-Hash-FallbackCPU" : "ParallelGPU-Hash"; }
    @Override public StrategyFamily getFamily() { return StrategyFamily.GPU_OPENCL; }
    @Override public int getParallelism() { return 0; }
    @Override public boolean isRealGpu() { return !fallbackMode && gpuDevice; }
    @Override public double getLastPreparationMs() { return lastPreparationMs; }
    @Override public double getLastKernelMs() { return lastKernelMs; }
}
