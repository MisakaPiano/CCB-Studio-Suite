package io.github.misakapiano.ccbstudio.assets;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Child processes touch only the exact synthetic root passed by the tests. */
public final class StoreProcess {
    static final byte[] PAYLOAD = syntheticPng();
    private static byte[] syntheticPng() {
        var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x12345678);
        var bytes = new ByteArrayOutputStream();
        try {
            if (!javax.imageio.ImageIO.write(image, "png", bytes)) throw new AssertionError("PNG fixture encoder absent");
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return bytes.toByteArray();
    }
    static AssetStore.Request request(UUID id) {
        return new AssetStore.Request(id, "synthetic.png", AssetStore.Kind.ORIGINAL, null,
                "{\"syntheticRecipe\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "future project bytes\u0000".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[1]);
        switch (args[0]) {
            case "owner" -> {
                try (var store = AssetStore.open(root)) {
                    Files.writeString(root.resolve("test-owner-ready"), "ready");
                    System.in.read();
                }
            }
            case "crash" -> {
                var point = AssetStore.Point.valueOf(args[3]);
                try (var store = AssetStore.open(root, new AssetStore.Faults() {
                    @Override public void after(AssetStore.Point reached, Path path) {
                        if (reached == point) Runtime.getRuntime().halt(71);
                    }
                })) {
                    store.put(request(UUID.fromString(args[2])), new ByteArrayInputStream(PAYLOAD));
                }
                throw new AssertionError("Requested crash point was not reached");
            }
            case "verify" -> {
                try (var store = AssetStore.open(root)) {
                    store.recover();
                    var out = new ByteArrayOutputStream(); store.export(UUID.fromString(args[2]), out);
                    if (!Arrays.equals(PAYLOAD, out.toByteArray())) throw new AssertionError("Bytes differ in new process");
                }
            }
            default -> throw new IllegalArgumentException("Unknown test mode");
        }
    }
}
