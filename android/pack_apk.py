"""Repackages aapt2 base APK with classes.dex and selected Android ABI .so libraries."""

import argparse
import os
import zipfile


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-apk", required=True)
    parser.add_argument("--dex", required=True)
    parser.add_argument("--lib-root", required=True)
    parser.add_argument("--abis", nargs="+", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)

    with zipfile.ZipFile(args.base_apk, "r") as zin, zipfile.ZipFile(args.out, "w") as zout:
        for info in zin.infolist():
            data = zin.read(info.filename)
            new_info = zipfile.ZipInfo(info.filename, date_time=info.date_time)
            new_info.compress_type = info.compress_type
            new_info.external_attr = info.external_attr
            zout.writestr(new_info, data)

        zout.write(args.dex, "classes.dex", compress_type=zipfile.ZIP_DEFLATED)

        for abi in args.abis:
            abi_dir = os.path.join(args.lib_root, abi)
            if not os.path.isdir(abi_dir):
                raise FileNotFoundError(f"ABI library directory not found: {abi_dir}")
            so_files = sorted(f for f in os.listdir(abi_dir) if f.endswith(".so"))
            if not so_files:
                raise RuntimeError(f"No .so files found in {abi_dir}")
            for fname in so_files:
                src = os.path.join(abi_dir, fname)
                arcname = f"lib/{abi}/{fname}"
                zout.write(src, arcname, compress_type=zipfile.ZIP_DEFLATED)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
