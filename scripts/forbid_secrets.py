#!/usr/bin/env python3
import re
import sys
from pathlib import Path

# Padrões de segredo a bloquear
BLOCK_PATTERNS = [
    re.compile(r"MAILGUN_API_KEY\s*=", re.IGNORECASE),
    re.compile(r"GNEWS_API_KEY\s*=", re.IGNORECASE),
    re.compile(r"GMAIL_APP_PASSWORD\s*=", re.IGNORECASE),
    re.compile(r"SECRET_KEY\s*=", re.IGNORECASE),
    re.compile(r"key-[0-9A-Za-z]{10,}"),
]

# Arquivos a ignorar (documentação/exemplos)
IGNORE_SUFFIXES = (".md", ".MD", ".markdown", ".rst", ".txt")
IGNORE_BASENAMES = {".env.example", "instrucoes.md"}

# Arquivos que nunca devem ser commitados
FORBIDDEN_BASENAMES = {".env"}
FORBIDDEN_SUFFIXES = (".env", ".local.properties")


def should_skip(path: Path) -> bool:
    name = path.name
    if name in IGNORE_BASENAMES:
        return True
    if name.startswith("README"):
        return True
    if any(name.endswith(suf) for suf in IGNORE_SUFFIXES):
        return True
    return False


def is_forbidden(path: Path) -> bool:
    name = path.name
    if name in FORBIDDEN_BASENAMES:
        return True
    if any(name.endswith(suf) for suf in FORBIDDEN_SUFFIXES):
        return True
    return False


def scan_file(path: Path) -> list[tuple[int, str]]:
    """Retorna lista de (linha, conteúdo) com suspeitas encontradas."""
    try:
        text = path.read_text(encoding="utf-8", errors="ignore")
    except Exception:
        return []
    findings: list[tuple[int, str]] = []
    for i, line in enumerate(text.splitlines(), start=1):
        for pat in BLOCK_PATTERNS:
            if pat.search(line):
                findings.append((i, line.strip()))
                break
    return findings


def main(argv: list[str]) -> int:
    if len(argv) == 0:
        return 0
    has_error = False
    for fname in argv:
        p = Path(fname)
        # Arquivos proibidos de serem commitados
        if is_forbidden(p):
            print(f"ERROR: Arquivo de segredo '{p}' não deve ser commitado. Remova do staging: git rm --cached '{p}'")
            has_error = True
            continue
        # Ignora documentação
        if should_skip(p):
            continue
        findings = scan_file(p)
        if findings:
            print(f"ERROR: Possíveis segredos detectados em '{p}':")
            for line_no, content in findings[:10]:
                print(f"  L{line_no}: {content}")
            has_error = True
    return 1 if has_error else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))