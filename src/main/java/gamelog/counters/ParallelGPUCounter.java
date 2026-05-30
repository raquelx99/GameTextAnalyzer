package gamelog.counters;

import org.jocl.*;
import static org.jocl.CL.*;

/**
 * Parallel GPU counter using OpenCL via JOCL 2.0.4.
 *
 * Falls back to a parallel CPU stream if JOCL's native library cannot be
 * loaded or if no OpenCL platform/device is available on the machine.
 */
public class ParallelGPUCounter implements WordCounter {

    private static final boolean JOCL_AVAILABLE = checkJocl();
    private boolean fallbackMode = !JOCL_AVAILABLE;

    /** OpenCL kernel: each work-item checks one line against the target word. */
    private static final String KERNEL_SOURCE =
        "__kernel void countEvent(\n" +
        "    __global const char* text,\n" +
        "    __global const int*  lineOffsets,\n" +
        "    __global const int*  lineLengths,\n" +
        "    __global const char* target,\n" +
        "    const int targetLen,\n" +
        "    const int totalLines,\n" +
        "    __global int* results)\n" +
        "{\n" +
        "    int gid = get_global_id(0);\n" +
        "    if (gid >= totalLines) return;\n" +
        "    int len = lineLengths[gid];\n" +
        "    if (len != targetLen) { results[gid] = 0; return; }\n" +
        "    int offset = lineOffsets[gid];\n" +
        "    for (int i = 0; i < len; i++) {\n" +
        "        if (text[offset + i] != target[i]) { results[gid] = 0; return; }\n" +
        "    }\n" +
        "    results[gid] = 1;\n" +
        "}\n";

    @Override
    public long count(String[] lines, String word) {
        if (!JOCL_AVAILABLE) {
            fallbackMode = true;
            return fallbackCount(lines, word);
        }
        try {
            return gpuCount(lines, word);
        } catch (Exception e) {
            fallbackMode = true;
            System.err.println("[ParallelGPU] OpenCL error, using CPU fallback: " + e.getMessage());
            return fallbackCount(lines, word);
        }
    }

    private long gpuCount(String[] lines, String word) {
        if (lines.length == 0) return 0;

        byte[] wordBytes  = word.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int    totalLines = lines.length;

        // ── Build flat byte buffer of all lines ─────────────────────────────
        int[] offsets = new int[totalLines];
        int[] lengths = new int[totalLines];
        int   total   = 0;
        for (int i = 0; i < totalLines; i++) {
            offsets[i] = total;
            byte[] b = lines[i].getBytes(java.nio.charset.StandardCharsets.UTF_8);
            lengths[i] = b.length;
            total += b.length;
        }
        byte[] textBuf = new byte[Math.max(total, 1)];
        int pos = 0;
        for (String line : lines) {
            byte[] b = line.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            System.arraycopy(b, 0, textBuf, pos, b.length);
            pos += b.length;
        }
        byte[] wordBuf = wordBytes.length > 0 ? wordBytes : new byte[1];

        // ── Platform / device discovery ──────────────────────────────────────
        setExceptionsEnabled(false);

        int[] numPlatforms = new int[1];
        if (clGetPlatformIDs(0, null, numPlatforms) != CL_SUCCESS || numPlatforms[0] == 0)
            throw new RuntimeException("No OpenCL platform found");
        cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
        clGetPlatformIDs(platforms.length, platforms, null);

        // Prefer GPU; fall back to CPU device if not available
        int[]  numDevices = new int[1];
        long   deviceType = CL_DEVICE_TYPE_GPU;
        if (clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_GPU, 0, null, numDevices) != CL_SUCCESS
                || numDevices[0] == 0) {
            if (clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_CPU, 0, null, numDevices) != CL_SUCCESS
                    || numDevices[0] == 0)
                throw new RuntimeException("No OpenCL device found");
            deviceType = CL_DEVICE_TYPE_CPU;
        }
        cl_device_id[] devices = new cl_device_id[numDevices[0]];
        clGetDeviceIDs(platforms[0], deviceType, devices.length, devices, null);

        setExceptionsEnabled(true);

        // ── Context / queue ──────────────────────────────────────────────────
        int[]           err     = new int[1];
        cl_context      context = clCreateContext(null, 1, devices, null, null, err);
        cl_command_queue queue  = clCreateCommandQueueWithProperties(context, devices[0], null, err);

        // ── Device buffers ───────────────────────────────────────────────────
        cl_mem bufText   = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                textBuf.length, Pointer.to(textBuf), null);
        cl_mem bufWord   = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                wordBuf.length, Pointer.to(wordBuf), null);
        cl_mem bufOff    = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                (long) totalLines * Integer.BYTES, Pointer.to(offsets), null);
        cl_mem bufLen    = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                (long) totalLines * Integer.BYTES, Pointer.to(lengths), null);
        cl_mem bufResult = clCreateBuffer(context,
                CL_MEM_WRITE_ONLY,
                (long) totalLines * Integer.BYTES, null, null);

        // ── Build program / kernel ───────────────────────────────────────────
        cl_program program = clCreateProgramWithSource(
                context, 1, new String[]{KERNEL_SOURCE}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel kernel = clCreateKernel(program, "countEvent", null);

        // ── Set kernel arguments ─────────────────────────────────────────────
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(bufText));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(bufOff));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(bufLen));
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(bufWord));
        clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{wordBytes.length}));
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{totalLines}));
        clSetKernelArg(kernel, 6, Sizeof.cl_mem, Pointer.to(bufResult));

        // ── Execute kernel ───────────────────────────────────────────────────
        clEnqueueNDRangeKernel(queue, kernel, 1,
                null, new long[]{totalLines}, null, 0, null, null);

        // ── Read results back ────────────────────────────────────────────────
        int[] resultArr = new int[totalLines];
        clEnqueueReadBuffer(queue, bufResult, true, 0,
                (long) totalLines * Integer.BYTES, Pointer.to(resultArr), 0, null, null);

        long count = 0;
        for (int r : resultArr) count += r;

        // ── Release OpenCL resources ─────────────────────────────────────────
        clReleaseMemObject(bufText);
        clReleaseMemObject(bufWord);
        clReleaseMemObject(bufOff);
        clReleaseMemObject(bufLen);
        clReleaseMemObject(bufResult);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(queue);
        clReleaseContext(context);

        return count;
    }

    /** CPU parallel stream fallback. */
    private long fallbackCount(String[] lines, String word) {
        return java.util.Arrays.stream(lines).parallel().filter(word::equals).count();
    }

    /**
     * Attempts to trigger the JOCL native library load.
     * Returns false (and prints a message) if the native lib is missing or
     * if no OpenCL runtime is present on the machine.
     */
    private static boolean checkJocl() {
        try {
            setExceptionsEnabled(false);
            return true;
        } catch (Throwable e) {
            System.out.println("[ParallelGPU] JOCL native library not available — "
                    + "GPU mode will use CPU fallback: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getName() {
        return fallbackMode ? "ParallelGPU-FallbackCPU" : "ParallelGPU";
    }
}
