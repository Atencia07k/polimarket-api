"""Segundo cliente PoliMarket, Python 3, sin dependencias externas."""
import argparse
import getpass
import json
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import Request, urlopen


def request(base, path, token=None, body=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = Request(base.rstrip("/") + path, headers=headers,
                  data=json.dumps(body).encode() if body is not None else None)
    with urlopen(req, timeout=20) as response:
        raw = response.read()
        return json.loads(raw) if raw else None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default="http://127.0.0.1:8080/api/v1")
    parser.add_argument("--username", required=True)
    parser.add_argument("--bodega", type=int, default=1)
    args = parser.parse_args()
    if urlparse(args.url).scheme not in ("http", "https") or args.bodega <= 0:
        parser.error("URL HTTP(S) y bodega positiva requeridas")
    refresh = None
    try:
        tokens = request(args.url, "/auth/login", body={
            "username": args.username, "password": getpass.getpass("Contraseña: ")})
        refresh = tokens["refreshToken"]
        for title, path in [("Clientes", "/clientes"), ("Productos", "/productos"),
                            ("Inventario", f"/inventarios/bodegas/{args.bodega}")]:
            print(f"\n{title}")
            print(json.dumps(request(args.url, path, tokens["accessToken"]),
                             ensure_ascii=False, indent=2))
        return 0
    except HTTPError as exc:
        try:
            detail = json.load(exc).get("detail", str(exc))
        except (ValueError, AttributeError):
            detail = str(exc)
        print(f"Error HTTP {exc.code}: {detail}", file=sys.stderr)
        return 1
    except (URLError, TimeoutError, ValueError) as exc:
        print(f"No se pudo completar la consulta: {exc}", file=sys.stderr)
        return 1
    finally:
        if refresh:
            try:
                request(args.url, "/auth/logout", body={"refreshToken": refresh})
            except (URLError, TimeoutError, ValueError):
                print("No se pudo revocar la sesión remota.", file=sys.stderr)


if __name__ == "__main__":
    sys.exit(main())

