package io.github.misakapiano.ccbstudio.assets;

/** Missing is reserved for a genuinely absent path, never an unreadable or unknown file. */
public record ReadResult<T>(Status status, T value, String detail) {
    public enum Status { VALID, MISSING, CORRUPT, READ_ERROR, UNSUPPORTED_VERSION }
    public static <T> ReadResult<T> valid(T value) { return new ReadResult<>(Status.VALID, value, ""); }
    public static <T> ReadResult<T> error(Status status, String detail) {
        return new ReadResult<>(status, null, detail);
    }
    public T require() throws StoreException {
        if (status != Status.VALID) throw new StoreException(status.name(), detail);
        return value;
    }
}
