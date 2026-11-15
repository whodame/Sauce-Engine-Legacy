"""Strips MethodParameters attributes from compiled .class files to avoid a known R8/D8 8.2.x NPE on JDK 21 mandated unnamed parameters."""

import glob
import os
import struct
import sys


def parse_cp(data: bytes, pos: int):
    (cp_count,) = struct.unpack_from(">H", data, pos)
    pos += 2
    cp = {}
    i = 1
    while i < cp_count:
        tag = data[pos]
        pos += 1
        if tag == 1:  # Utf8
            (length,) = struct.unpack_from(">H", data, pos)
            pos += 2
            cp[i] = data[pos : pos + length]
            pos += length
            i += 1
        elif tag in (3, 4):  # Integer, Float
            pos += 4
            i += 1
        elif tag in (5, 6):  # Long, Double (2 slots)
            pos += 8
            i += 2
        elif tag in (7, 8, 16, 19, 20):
            pos += 2
            i += 1
        elif tag in (9, 10, 11, 12, 17, 18):
            pos += 4
            i += 1
        elif tag == 15:  # MethodHandle
            pos += 3
            i += 1
        else:
            raise ValueError(f"Unsupported constant pool tag {tag} at index {i}")
    return cp, pos


def rewrite_attributes(data: bytes, pos: int, cp: dict, drop_names: set):
    (attr_count,) = struct.unpack_from(">H", data, pos)
    pos += 2
    kept = []
    changed = False
    for _ in range(attr_count):
        (name_idx, attr_len) = struct.unpack_from(">HI", data, pos)
        end = pos + 6 + attr_len
        if cp.get(name_idx) in drop_names:
            changed = True
        else:
            kept.append(data[pos:end])
        pos = end
    out = struct.pack(">H", len(kept)) + b"".join(kept)
    return out, pos, changed


def strip_class_file(path: str) -> bool:
    with open(path, "rb") as f:
        data = f.read()

    if data[:4] != b"\xca\xfe\xba\xbe":
        return False

    cp, pos = parse_cp(data, 8)
    out = bytearray(data[:pos])

    # access_flags, this_class, super_class, interfaces_count
    (ifaces_count,) = struct.unpack_from(">H", data, pos + 6)
    end_ifaces = pos + 8 + 2 * ifaces_count
    out.extend(data[pos:end_ifaces])
    pos = end_ifaces

    # fields
    (fields_count,) = struct.unpack_from(">H", data, pos)
    pos += 2
    out.extend(struct.pack(">H", fields_count))
    for _ in range(fields_count):
        out.extend(data[pos : pos + 6])
        pos += 6
        attr_bytes, pos, _ = rewrite_attributes(data, pos, cp, set())
        out.extend(attr_bytes)

    # methods
    (methods_count,) = struct.unpack_from(">H", data, pos)
    pos += 2
    out.extend(struct.pack(">H", methods_count))
    changed_any = False
    for _ in range(methods_count):
        out.extend(data[pos : pos + 6])
        pos += 6
        attr_bytes, pos, changed = rewrite_attributes(data, pos, cp, {b"MethodParameters"})
        if changed:
            changed_any = True
        out.extend(attr_bytes)

    if not changed_any:
        return False

    # class attributes
    out.extend(data[pos:])
    with open(path, "wb") as f:
        f.write(out)
    return True


def main() -> int:
    root = sys.argv[1]
    stripped = 0
    for path in glob.glob(os.path.join(root, "**", "*.class"), recursive=True):
        if strip_class_file(path):
            stripped += 1
    print(f"Stripped MethodParameters from {stripped} class file(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
