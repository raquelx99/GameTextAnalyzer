package gamelog.counters;

/**
 * Parallel GPU counter using OpenCL (JOCL).
 *
 * If JOCL is not available on the classpath or no OpenCL device is found,
 * the counter automatically falls back to a parallel CPU implementation
 * and records a warning — so the benchmark still runs end-to-end.
 *
 * To enable GPU support, add jocl-2.0.4.jar (and native libs) to the classpath.
 */
public class ParallelGPUCounter implements WordCounter {

    private static final boolean JOCL_AVAILABLE = checkJocl();
    private boolean fallbackMode = !JOCL_AVAILABLE;

    /** OpenCL kernel: each work-item checks one line. */
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

    /**
     * True GPU path. Encodes lines into a flat byte buffer, transfers to GPU,
     * runs the kernel, reads back per-line results, and sums them on the CPU.
     */
    private long gpuCount(String[] lines, String word) throws Exception {
        // ── Build flat buffer ───────────────────────────────────────────────
        byte[] wordBytes = word.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int totalLines   = lines.length;

        int[] offsets = new int[totalLines];
        int[] lengths = new int[totalLines];
        int   total   = 0;
        for (int i = 0; i < totalLines; i++) {
            offsets[i] = total;
            byte[] b = lines[i].getBytes(java.nio.charset.StandardCharsets.UTF_8);
            lengths[i] = b.length;
            total += b.length;
        }

        byte[] textBuf = new byte[total];
        int pos = 0;
        for (String line : lines) {
            byte[] b = line.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            System.arraycopy(b, 0, textBuf, pos, b.length);
            pos += b.length;
        }

        // ── JOCL calls (via reflection to avoid hard compile dependency) ────
        Class<?> cl   = Class.forName("org.jocl.CL");
        Class<?> clm  = Class.forName("org.jocl.cl_mem");

        // setExceptionsEnabled(true)
        cl.getMethod("setExceptionsEnabled", boolean.class).invoke(null, true);

        // Obtain platform/device/context/queue
        Object[] platforms = (Object[]) cl.getMethod("getPlatforms").invoke(null);
        if (platforms == null || platforms.length == 0)
            throw new RuntimeException("No OpenCL platform found");

        long deviceType = 4L; // CL_DEVICE_TYPE_GPU
        Object[] devices;
        try {
            devices = (Object[]) cl.getMethod("getDevices", platforms[0].getClass(), long.class)
                                    .invoke(null, platforms[0], deviceType);
        } catch (Exception ex) {
            deviceType = 2L; // CL_DEVICE_TYPE_CPU fallback
            devices = (Object[]) cl.getMethod("getDevices", platforms[0].getClass(), long.class)
                                    .invoke(null, platforms[0], deviceType);
        }
        if (devices == null || devices.length == 0)
            throw new RuntimeException("No OpenCL device found");

        Object context = cl.getMethod("createContext", null, int.class,
                                       devices.getClass(), Object.class, Object.class, int[].class)
                           .invoke(null, null, 1, devices, null, null, null);
        Object queue   = cl.getMethod("createCommandQueue",
                                       context.getClass(), devices[0].getClass(), long.class)
                           .invoke(null, context, devices[0], 0L);

        // Allocate buffers
        long MEM_READ_ONLY  = 4L;
        long MEM_WRITE_ONLY = 2L;

        java.nio.ByteBuffer textNIO    = java.nio.ByteBuffer.allocateDirect(textBuf.length);
        textNIO.put(textBuf); textNIO.rewind();
        java.nio.ByteBuffer wordNIO    = java.nio.ByteBuffer.allocateDirect(wordBytes.length);
        wordNIO.put(wordBytes); wordNIO.rewind();
        java.nio.IntBuffer  offsetNIO  = java.nio.IntBuffer.allocate(totalLines);
        offsetNIO.put(offsets); offsetNIO.rewind();
        java.nio.IntBuffer  lengthNIO  = java.nio.IntBuffer.allocate(totalLines);
        lengthNIO.put(lengths); lengthNIO.rewind();

        Object bufText   = cl.getMethod("createBuffer", context.getClass(), long.class, long.class,
                                         java.nio.Buffer.class, int[].class)
                             .invoke(null, context, MEM_READ_ONLY, (long) textBuf.length, textNIO, null);
        Object bufWord   = cl.getMethod("createBuffer", context.getClass(), long.class, long.class,
                                         java.nio.Buffer.class, int[].class)
                             .invoke(null, context, MEM_READ_ONLY, (long) wordBytes.length, wordNIO, null);
        Object bufOff    = cl.getMethod("createBuffer", context.getClass(), long.class, long.class,
                                         java.nio.Buffer.class, int[].class)
                             .invoke(null, context, MEM_READ_ONLY, (long) totalLines * 4, offsetNIO, null);
        Object bufLen    = cl.getMethod("createBuffer", context.getClass(), long.class, long.class,
                                         java.nio.Buffer.class, int[].class)
                             .invoke(null, context, MEM_READ_ONLY, (long) totalLines * 4, lengthNIO, null);
        Object bufResult = cl.getMethod("createBuffer", context.getClass(), long.class, long.class,
                                         java.nio.Buffer.class, int[].class)
                             .invoke(null, context, MEM_WRITE_ONLY, (long) totalLines * 4, null, null);

        // Build program/kernel
        Object program = cl.getMethod("createProgramWithSource", context.getClass(), int.class,
                                       String[].class, long[].class, int[].class)
                           .invoke(null, context, 1, new String[]{KERNEL_SOURCE}, null, null);
        cl.getMethod("buildProgram", program.getClass(), int.class, devices.getClass(),
                      String.class, Object.class, Object.class)
          .invoke(null, program, 1, devices, null, null, null);
        Object kernel = cl.getMethod("createKernel", program.getClass(), String.class, int[].class)
                          .invoke(null, program, "countEvent", null);

        // Set kernel args
        cl.getMethod("setKernelArg", kernel.getClass(), int.class, long.class, clm)
          .invoke(null, kernel, 0, (long) clm.getField("SIZE").getLong(null), bufText);
        cl.getMethod("setKernelArg", kernel.getClass(), int.class, long.class, clm)
          .invoke(null, kernel, 1, (long) clm.getField("SIZE").getLong(null), bufOff);
        cl.getMethod("setKernelArg", kernel.getClass(), int.class, long.class, clm)
          .invoke(null, kernel, 2, (long) clm.getField("SIZE").getLong(null), bufLen);
        cl.getMethod("setKernelArg", kernel.getClass(), int.class, long.class, clm)
          .invoke(null, kernel, 3, (long) clm.getField("SIZE").getLong(null), bufWord);

        java.nio.IntBuffer wlenBuf = java.nio.IntBuffer.allocate(1);
        wlenBuf.put(wordBytes.length); wlenBuf.rewind();
        cl.getMethod("setKernelArg", kernel.getClass(), int.class, long.class, java.nio.Buffer.class)
          .invoke(null, kernel, 4, 4L, wlenBuf);

        java.nio.IntBuffer tlinesBuf = java.nio.IntBuffer.allocate(1);
        tlinesBuf.put(totalLines); tlinesBuf.rewind();
        cl.getMethod("setKernelArg", kernel.getClass(), int.class, long.class, java.nio.Buffer.class)
          .invoke(null, kernel, 5, 4L, tlinesBuf);

        cl.getMethod("setKernelArg", kernel.getClass(), int.class, long.class, clm)
          .invoke(null, kernel, 6, (long) clm.getField("SIZE").getLong(null), bufResult);

        // Enqueue kernel
        cl.getMethod("enqueueNDRangeKernel", queue.getClass(), kernel.getClass(), int.class,
                      long[].class, long[].class, long[].class, int.class, Object[].class, Object.class)
          .invoke(null, queue, kernel, 1, null, new long[]{totalLines}, null, 0, null, null);

        // Read results
        java.nio.IntBuffer resultBuf = java.nio.IntBuffer.allocate(totalLines);
        cl.getMethod("enqueueReadBuffer", queue.getClass(), clm, boolean.class,
                      long.class, long.class, java.nio.Buffer.class, int.class, Object[].class, Object.class)
          .invoke(null, queue, bufResult, true, 0L, (long) totalLines * 4, resultBuf, 0, null, null);

        // Sum results
        long count = 0;
        resultBuf.rewind();
        while (resultBuf.hasRemaining()) count += resultBuf.get();

        // Release resources
        for (Object buf : new Object[]{bufText, bufWord, bufOff, bufLen, bufResult}) {
            cl.getMethod("releaseMemObject", clm).invoke(null, buf);
        }
        cl.getMethod("releaseKernel",       kernel.getClass() ).invoke(null, kernel);
        cl.getMethod("releaseProgram",      program.getClass()).invoke(null, program);
        cl.getMethod("releaseCommandQueue", queue.getClass()  ).invoke(null, queue);
        cl.getMethod("releaseContext",      context.getClass()).invoke(null, context);

        return count;
    }

    /** Fallback when JOCL/OpenCL is unavailable: CPU parallel stream. */
    private long fallbackCount(String[] lines, String word) {
        return java.util.Arrays.stream(lines)
                .parallel()
                .filter(word::equals)
                .count();
    }

    private static boolean checkJocl() {
        try {
            Class.forName("org.jocl.CL");
            return true;
        } catch (ClassNotFoundException e) {
            System.out.println("[ParallelGPU] JOCL not found — GPU mode will use CPU fallback.");
            return false;
        }
    }

    @Override
    public String getName() {
        return fallbackMode ? "ParallelGPU-FallbackCPU" : "ParallelGPU";
    }
}
