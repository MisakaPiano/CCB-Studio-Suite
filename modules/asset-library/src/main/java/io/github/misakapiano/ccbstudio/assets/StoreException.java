package io.github.misakapiano.ccbstudio.assets;

import java.io.IOException;

public final class StoreException extends IOException {
    private final String code;
    public StoreException(String code, String detail) { super(detail); this.code = code; }
    public String code() { return code; }
}
